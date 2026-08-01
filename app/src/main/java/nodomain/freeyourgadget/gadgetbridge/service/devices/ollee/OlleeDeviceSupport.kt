/*  Copyright (C) 2026 Ken Blizzard-Caron

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.ollee

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.devices.ollee.OlleeActivitySampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.ollee.OlleeConstants
import nodomain.freeyourgadget.gadgetbridge.entities.OlleeActivitySample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.Alarm
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.webview.CurrentPosition
import org.slf4j.LoggerFactory
import java.util.Calendar
import java.util.TimeZone

class OlleeDeviceSupport : AbstractBTLESingleDeviceSupport(LOG) {
    private val reassembler = OlleeFrameReassembler()
    private val composer = WeekdayRegisterComposer()
    private val activeNotifications = mutableSetOf<Int>()
    private var lastAlarmReadback: ByteArray? = null
    private var lastFacesReadback: ByteArray? = null

    // The 02 34 register carries the World Time offset alongside the badge text; until the
    // 0x55 read-back seeds the composer's offset, any register write would push offset 0 and
    // silently reset the watch's World Time. Gates pushBadge(), not just the init sequence.
    private var weekdaysSeeded = false

    // True while the lockstep init read chain (version -> alarm -> weekdays -> faces) runs.
    private var initReadChain = false

    // Activity records drain (02 27 count -> 02 28 x N -> persist -> 02 2D commit). Reads are
    // non-destructive until the 2D commit (probe-verified), so aborting mid-drain is safe.
    private var draining = false
    private var drainExpected = 0L
    private val drainRecords = mutableListOf<OlleeActivityRecords.Record>()

    init {
        addSupportedService(OlleeConstants.UUID_SERVICE_NUS)
    }

    override fun useAutoConnect(): Boolean = true

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        reassembler.reset()
        activeNotifications.clear()
        composer.badgeCount = 0
        weekdaysSeeded = false
        draining = false
        drainExpected = 0
        drainRecords.clear()
        builder.setDeviceState(GBDevice.State.INITIALIZING)
        builder.notify(OlleeConstants.UUID_CHARACTERISTIC_TX, true)
        // The watch silently drops reads that arrive while a reply is in flight (hardware: of four
        // pipelined reads only the first 2-3 answer). So reads run in lockstep — each readback
        // handler in handleFrame() issues the next init read. Writes are fire-and-forget.
        initReadChain = true
        writeFrame(builder, OlleeProtocol.readRequest(OlleeConstants.TARGET_VERSION))
        if (GBApplication.getPrefs().syncTime()) {
            sendSetClock(builder)
        }
        builder.setDeviceState(GBDevice.State.INITIALIZED)
        return builder
    }

    override fun onSetTime() {
        if (!GBApplication.getPrefs().syncTime()) return
        val builder = createTransactionBuilder("set clock")
        sendSetClock(builder)
        builder.queue()
    }

    private fun sendSetClock(builder: TransactionBuilder) {
        val nowMs = System.currentTimeMillis()
        val location = CurrentPosition().lastKnownLocation
        writeFrame(builder, OlleeProtocol.buildSetClock(
            nowEpochSec = nowMs / 1000L,
            utcOffsetSec = TimeZone.getDefault().getOffset(nowMs) / 1000,
            latE3 = (location.latitude * 1000).toInt(),
            lonE3 = (location.longitude * 1000).toInt()
        ))
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        if (characteristic.uuid != OlleeConstants.UUID_CHARACTERISTIC_TX) return super.onCharacteristicChanged(gatt, characteristic, value)
        // The watch pipelines replies; one fragment can complete more than one frame.
        var frame = reassembler.accept(value)
        while (frame != null) {
            handleFrame(frame)
            frame = reassembler.pending()
        }
        return true
    }

    private fun handleFrame(frame: Frame) {
        when (frame.target) {
            OlleeConstants.TARGET_VERSION + OlleeConstants.RESPONSE_TARGET_OFFSET -> {
                handleVersionReply(frame.payload)
                if (initReadChain) sendRead("alarm readback", OlleeConstants.TARGET_GET_ALARM)
            }
            OlleeConstants.TARGET_GET_ALARM + OlleeConstants.RESPONSE_TARGET_OFFSET -> {
                LOG.debug("Alarm readback ({} bytes)", frame.payload.size)
                lastAlarmReadback = frame.payload
                if (initReadChain) sendRead("weekday register", OlleeConstants.TARGET_GET_WEEKDAYS)
            }
            OlleeConstants.TARGET_GET_WEEKDAYS + OlleeConstants.RESPONSE_TARGET_OFFSET -> {
                LOG.debug("Weekday register readback ({} bytes)", frame.payload.size)
                composer.seedFromReadback(frame.payload)
                weekdaysSeeded = true
                // Offset now seeded: safe to restore the weekday table (clears any stale badge
                // left on the watch from before the reconnect).
                pushBadge()
                if (initReadChain) sendRead("faces table", OlleeConstants.TARGET_GET_FACES)
            }
            OlleeConstants.TARGET_GET_FACES + OlleeConstants.RESPONSE_TARGET_OFFSET -> {
                LOG.debug("Faces table readback ({} bytes): {}", frame.payload.size, GB.hexdump(frame.payload))
                logFacesRecords(frame.payload)
                lastFacesReadback = frame.payload
                initReadChain = false
            }
            OlleeConstants.TARGET_ACTIVITY_COUNT + OlleeConstants.RESPONSE_TARGET_OFFSET ->
                handleActivityCount(frame.payload)
            OlleeConstants.TARGET_ACTIVITY_RECORD + OlleeConstants.RESPONSE_TARGET_OFFSET ->
                handleActivityRecord(frame.payload)
            OlleeConstants.TARGET_ACTIVITY_COMMIT + OlleeConstants.RESPONSE_TARGET_OFFSET ->
                LOG.debug("Activity commit acknowledged") // not sent by current firmware
            else -> LOG.debug("Unhandled Ollee frame 0x{}", Integer.toHexString(frame.target))
        }
    }

    /** Byte 4 is still unidentified; logging it per record is how we find out what it means. */
    private fun logFacesRecords(payload: ByteArray) {
        var offset = 6
        while (offset + 6 <= payload.size) {
            fun at(i: Int) = payload[offset + i].toInt() and 0xFF
            LOG.debug(
                "Face id=0x{} b1={} enabled={} b3={} unknown4={} slot={}",
                Integer.toHexString(at(0)), at(1), at(2), at(3), at(4), at(5)
            )
            offset += 6
        }
    }

    override fun onFetchRecordedData(dataTypes: Int) {
        if (dataTypes and RecordedDataTypes.TYPE_ACTIVITY == 0) return
        if (draining) return
        draining = true
        drainExpected = 0
        drainRecords.clear()
        device.setBusyTask(R.string.busy_task_fetch_activity_data, context)
        device.sendDeviceUpdateIntent(context)
        sendRead("activity count", OlleeConstants.TARGET_ACTIVITY_COUNT)
    }

    private fun handleActivityCount(payload: ByteArray) {
        if (!draining) return
        drainExpected = try {
            OlleeActivityRecords.parseCount(payload)
        } catch (e: IllegalArgumentException) {
            LOG.error("Bad activity count payload", e)
            endDrain()
            return
        }
        LOG.debug("Activity drain: {} pending records", drainExpected)
        if (drainExpected == 0L) {
            endDrain()
        } else {
            sendRead("activity record", OlleeConstants.TARGET_ACTIVITY_RECORD)
        }
    }

    private fun handleActivityRecord(payload: ByteArray) {
        if (!draining) return
        val record = try {
            OlleeActivityRecords.parseRecord(payload)
        } catch (e: IllegalArgumentException) {
            // Abort without the 2D commit: the watch queue is untouched and the next
            // drain re-delivers everything.
            LOG.error("Bad activity record payload", e)
            endDrain()
            return
        }
        if (record.typeFlags != 0L) {
            LOG.info("Activity record with unknown type/flags 0x{}", java.lang.Long.toHexString(record.typeFlags))
        }
        drainRecords.add(record)
        if (drainRecords.size < drainExpected) {
            sendRead("activity record", OlleeConstants.TARGET_ACTIVITY_RECORD)
        } else if (persistActivitySamples(drainRecords)) {
            // Persisted first — only now is it safe to let the watch clear its queue.
            LOG.info("Activity drain complete ({} records persisted)", drainRecords.size)
            sendRead("activity commit", OlleeConstants.TARGET_ACTIVITY_COMMIT)
            // The commit is not acknowledged on current firmware (verified on hardware),
            // so the drain ends once the commit is queued.
            endDrain()
        } else {
            endDrain()
        }
    }

    private fun persistActivitySamples(records: List<OlleeActivityRecords.Record>): Boolean {
        try {
            GBApplication.acquireDB().use { dbHandler ->
                val session = dbHandler.daoSession
                val dbUser = DBHelper.getUser(session)
                val dbDevice = DBHelper.getDevice(device, session)
                val provider = OlleeActivitySampleProvider(device, session)
                val samples = records.map { record ->
                    OlleeActivitySample().apply {
                        timestamp = record.startTs.toInt()
                        steps = record.steps.toInt()
                        // Idle intervals arrive as zero-step records — don't paint them
                        // as activity on the charts.
                        rawKind = if (record.steps > 0) {
                            ActivityKind.ACTIVITY.code
                        } else {
                            ActivityKind.NOT_MEASURED.code
                        }
                        setDevice(dbDevice)
                        setUser(dbUser)
                        setProvider(provider)
                    }
                }
                provider.addGBActivitySamples(samples)
            }
            return true
        } catch (e: Exception) {
            LOG.error("Error saving activity samples", e)
            return false
        }
    }

    private fun endDrain() {
        draining = false
        drainExpected = 0
        drainRecords.clear()
        if (device.isBusy) {
            device.unsetBusyTask()
            device.sendDeviceUpdateIntent(context)
        }
        GB.signalActivityDataFinish(device)
    }

    /** Writes [frame] to the RX characteristic, fragmented to the connection's max ATT chunk. */
    private fun writeFrame(builder: TransactionBuilder, frame: ByteArray) {
        builder.writeChunkedData(
            getCharacteristic(OlleeConstants.UUID_CHARACTERISTIC_RX),
            frame,
            OlleeProtocol.MAX_ATT_CHUNK
        )
    }

    private fun sendRead(taskName: String, target: Int) {
        val builder = createTransactionBuilder(taskName)
        writeFrame(builder, OlleeProtocol.readRequest(target))
        builder.queue()
    }

    override fun onSetAlarms(alarms: ArrayList<out Alarm>) {
        val alarm = alarms.firstOrNull() ?: return
        // A deleted alarm arrives as unused (possibly still flagged enabled) — write it disabled.
        val enabled = alarm.enabled && !alarm.unused
        val record = OlleeAlarm.buildRecord(
            enabled = enabled,
            hour = alarm.hour,
            minute = alarm.minute,
            gbRepetitionMask = alarm.repetition,
            snooze = alarm.snooze,
            existing = lastAlarmReadback
        )
        if (enabled && alarm.repetition == 0) {
            // One-shot: watch mask 0x00 would disable the alarm outright — arm the next-fire
            // weekday instead (rings weekly until changed; documented device limitation).
            val now = Calendar.getInstance()
            val isoDay = ((now.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // SUNDAY=1.. -> ISO 1=Mon..7=Sun
            record[5] = OlleeAlarm.oneShotWatchDayMask(
                alarm.hour, alarm.minute, isoDay,
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)
            ).toByte()
        }
        val builder = createTransactionBuilder("set alarm")
        writeFrame(builder, OlleeProtocol.buildPacket(OlleeConstants.TARGET_ALARM, record))
        writeFrame(builder, OlleeProtocol.readRequest(OlleeConstants.TARGET_GET_ALARM)) // refresh RMW cache
        builder.queue()
    }

    override fun onSetWorldClocks(clocks: ArrayList<out WorldClock>) {
        val clock = clocks.firstOrNull { it.enabled == true }
        val builder = createTransactionBuilder("set world clock")
        if (clock != null) {
            val tz = TimeZone.getTimeZone(clock.timeZoneId)
            composer.worldTimeOffsetSec = tz.getOffset(System.currentTimeMillis()) / 1000
            setWorldTimeFace(builder, enabled = true)
            writeFrame(builder, OlleeProtocol.buildPacket(OlleeConstants.TARGET_WEEKDAYS, composer.composePayload()))
        } else {
            // Zone removed: hide the World Time face. The stale offset in the register is
            // harmless once the face is hidden, so no register write is needed.
            setWorldTimeFace(builder, enabled = false)
        }
        builder.queue()
    }

    /** Flips the World Time face via faces-table RMW; no-op when already in [enabled] state. */
    private fun setWorldTimeFace(builder: TransactionBuilder, enabled: Boolean) {
        val faces = lastFacesReadback ?: return
        val current = OlleeFacesTable.isFaceEnabled(faces, OlleeConstants.FACE_ID_WORLD_TIME) ?: return
        if (current == enabled) return
        val updated = OlleeFacesTable.withFaceEnabled(faces, OlleeConstants.FACE_ID_WORLD_TIME, enabled)
        writeFrame(builder, OlleeProtocol.buildPacket(OlleeConstants.TARGET_SET_FACES, updated))
        lastFacesReadback = updated
    }

    override fun onNotification(notificationSpec: NotificationSpec) {
        if (activeNotifications.add(notificationSpec.id)) pushBadge()
    }

    override fun onDeleteNotification(id: Int) {
        // Dismissals for notifications we never tracked (predating the connection) are no-ops.
        if (activeNotifications.remove(id)) pushBadge()
    }

    private fun pushBadge() {
        if (!weekdaysSeeded) return // seed handler pushes the pending count once the offset is known
        composer.badgeCount = activeNotifications.size
        val builder = createTransactionBuilder("badge ${composer.badgeCount}")
        writeFrame(builder, OlleeProtocol.buildPacket(OlleeConstants.TARGET_WEEKDAYS, composer.composePayload()))
        builder.queue()
    }

    private fun handleVersionReply(payload: ByteArray) {
        val version = GBDeviceEventVersionInfo()
        val fw = OlleeProtocol.parseFirmwareVersion(payload)?.also { version.fwVersion = it }
        val hw = OlleeProtocol.parseHardwareVersion(payload)?.also { version.hwVersion = it }
        if (fw != null || hw != null) evaluateGBDeviceEvent(version)
        OlleeProtocol.parseVoltageMillivolts(payload)?.let { mv ->
            val battery = GBDeviceEventBatteryInfo()
            battery.level = GBDevice.BATTERY_UNKNOWN.toInt()
            battery.voltage = mv / 1000f
            evaluateGBDeviceEvent(battery)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OlleeDeviceSupport::class.java)
    }
}
