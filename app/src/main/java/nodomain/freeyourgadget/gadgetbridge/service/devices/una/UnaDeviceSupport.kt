/*  Copyright (C) 2026 Toby Murray

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.una

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import androidx.core.content.edit
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.devices.BaseActivitySummaryProvider
import nodomain.freeyourgadget.gadgetbridge.devices.una.UnaConstants
import nodomain.freeyourgadget.gadgetbridge.devices.una.UnaDailySampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.UnaDailySample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.NativeFITMessages
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils
import org.slf4j.LoggerFactory
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

/**
 * Connect, battery, firmware/hardware revision (DIS), time sync (CTS), discrete workout sync
 * over the custom File Transfer Service (FTS), and daily steps/active-minutes/heart-rate totals
 * over the custom Command Service (CCS).
 *
 * FTS sync works off `/Apps/latest_activity.txt`, a manifest of pending `.fit` activity paths
 * the firmware itself maintains -- reading it avoids walking every app's Activity directory.
 * Each listed path is skipped if already imported (tracked locally; nothing is deleted or
 * acknowledged on the watch, since the firmware's own post-sync cleanup trigger is a secondary
 * command whose exact behavior hasn't been tested, so it's not sent here).
 *
 * CCS's DailyHealth request/response gives one day's steps/floors/active-minutes/resting-HR/
 * average-HR as a single aggregate -- the firmware has no per-minute daily export. That aggregate
 * is turned into a small burst of synthetic per-minute samples (see `persistDailyHealth`) so
 * Gadgetbridge's sample-timeline-based dashboard totals add up to the same numbers the watch itself
 * reports, not because a real per-minute timeline exists.
 */
class UnaDeviceSupport : AbstractBTLESingleDeviceSupport(LOG) {
    private val deviceInfoProfile: DeviceInfoProfile<UnaDeviceSupport>
    private val batteryInfoProfile: BatteryInfoProfile<UnaDeviceSupport>

    private var draining = false
    private val pendingManifestPaths = ArrayDeque<String>()
    private var currentReadPath: String? = null
    private var currentReadChunks = mutableMapOf<Int, ByteArray>()
    private var readingManifest = false
    private val pendingHealthDays = ArrayDeque<Calendar>()
    private var currentHealthDay: Calendar? = null

    init {
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION)
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE)
        addSupportedService(GattService.UUID_SERVICE_CURRENT_TIME)
        addSupportedService(UnaConstants.UUID_SERVICE_FTS)
        addSupportedService(UnaConstants.UUID_SERVICE_CCS)

        val listener = IntentListener { intent ->
            when (intent.action) {
                DeviceInfoProfile.ACTION_DEVICE_INFO ->
                    handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO)!!)
                BatteryInfoProfile.ACTION_BATTERY_INFO ->
                    handleBatteryInfo(intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO)!!)
            }
        }

        deviceInfoProfile = DeviceInfoProfile(this)
        deviceInfoProfile.addListener(listener)
        addSupportedProfile(deviceInfoProfile)

        batteryInfoProfile = BatteryInfoProfile(this)
        batteryInfoProfile.addListener(listener)
        addSupportedProfile(batteryInfoProfile)
    }

    override fun useAutoConnect(): Boolean = false

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        // A stale in-flight fetch's state must not survive a reconnect: without this reset, a
        // disconnect mid-sync (this firmware's advertising/connection is known to be flaky)
        // leaves `draining` stuck true forever, silently no-oping every future
        // onFetchRecordedData() call for the rest of the app's process lifetime.
        draining = false
        pendingManifestPaths.clear()
        currentReadPath = null
        currentReadChunks = mutableMapOf()
        readingManifest = false
        pendingHealthDays.clear()
        currentHealthDay = null

        builder.setDeviceState(GBDevice.State.INITIALIZING)

        // See UnaFtsProtocol.readChunkSizeFor(); the granted value bounds every read.
        builder.requestMtu(UnaConstants.MTU_REQUEST)

        deviceInfoProfile.requestDeviceInfo(builder)

        batteryInfoProfile.requestBatteryInfo(builder)
        batteryInfoProfile.enableNotify(builder, true)

        if (GBApplication.getPrefs().syncTime()) {
            writeCurrentTime(builder)
        }

        builder.notify(UnaConstants.UUID_CHARACTERISTIC_FTS, true)
        builder.notify(UnaConstants.UUID_CHARACTERISTIC_DAILY_HEALTH, true)

        builder.setDeviceState(GBDevice.State.INITIALIZED)
        return builder
    }

    override fun onSetTime() {
        if (!GBApplication.getPrefs().syncTime()) return
        val builder = createTransactionBuilder("set time")
        writeCurrentTime(builder)
        builder.queue()
    }

    private fun writeCurrentTime(builder: TransactionBuilder) {
        val now = GregorianCalendar()
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME, *BLETypeConversions.calendarToCurrentTime(now, 0))
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_LOCAL_TIME, *BLETypeConversions.calendarToLocalTime(now))
    }

    private fun handleDeviceInfo(info: DeviceInfo) {
        LOG.debug("Device info: {}", info)
        for (event in DeviceInfoProfile.toDeviceEvents(info)) {
            evaluateGBDeviceEvent(event)
        }
    }

    private fun handleBatteryInfo(info: BatteryInfo) {
        LOG.debug("Battery info: {}", info)
        val battery = GBDeviceEventBatteryInfo()
        battery.level = info.percentCharged
        evaluateGBDeviceEvent(battery)
    }

    override fun onFetchRecordedData(dataTypes: Int) {
        if (dataTypes and RecordedDataTypes.TYPE_ACTIVITY == 0) return
        if (draining) return
        draining = true
        pendingManifestPaths.clear()
        device.setBusyTask(R.string.busy_task_fetch_activity_data, context)
        device.sendDeviceUpdateIntent(context)

        // FTS runs to completion first, then CCS DailyHealth -- using CCS's DailyHealth
        // characteristic at all was observed to make the firmware's FTS read handler reject every
        // subsequent FTS request with a short 2-byte error response, even after a settle delay, as
        // if some resource FTS needs stays held once CCS has been touched. Never observed the other
        // way around, so FTS goes first.
        startReadFile(MANIFEST_PATH, isManifest = true)
    }

    private fun requestNextDailyHealth() {
        val day = pendingHealthDays.removeFirstOrNull()
        if (day == null) {
            currentHealthDay = null
            LOG.debug("Daily health sync complete")
            endDrain()
            return
        }
        currentHealthDay = day
        val builder = createTransactionBuilder("daily health ${day.time}")
        builder.write(
            UnaConstants.UUID_CHARACTERISTIC_DAILY_HEALTH,
            *UnaDailyHealthProtocol.buildRequest(
                year = day.get(Calendar.YEAR),
                month = day.get(Calendar.MONTH) + 1,
                day = day.get(Calendar.DAY_OF_MONTH),
            ),
        )
        builder.queue()
    }

    private fun startReadFile(path: String, isManifest: Boolean) {
        currentReadPath = path
        currentReadChunks = mutableMapOf()
        readingManifest = isManifest
        requestChunk(path, 0)
    }

    private fun requestChunk(path: String, offset: Int) {
        val builder = createTransactionBuilder("read $path @ $offset")
        builder.write(
            UnaConstants.UUID_CHARACTERISTIC_FTS,
            *UnaFtsProtocol.buildReadRequest(path, offset, UnaFtsProtocol.readChunkSizeFor(mtu)),
        )
        builder.queue()
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        when (characteristic.uuid) {
            UnaConstants.UUID_CHARACTERISTIC_FTS -> {
                val chunk = UnaFtsProtocol.parseReadChunk(value)
                if (chunk == null) {
                    handleReadFailure(value)
                    return true
                }
                handleReadChunk(chunk)
            }
            UnaConstants.UUID_CHARACTERISTIC_DAILY_HEALTH -> {
                val health = UnaDailyHealthProtocol.parseResponse(value)
                if (health == null) {
                    LOG.warn("Unparseable daily health notification: {}", StringUtils.bytesToHex(value))
                    return true
                }
                handleDailyHealth(health)
            }
            else -> return super.onCharacteristicChanged(gatt, characteristic, value)
        }
        return true
    }

    private fun handleDailyHealth(health: UnaDailyHealth) {
        val day = currentHealthDay
        if (day == null) {
            LOG.warn("Got a daily health response with no pending request: {}", health)
            return
        }
        persistDailyHealth(day, health)
        requestNextDailyHealth()
    }

    /**
     * A non-`0x11` response to a read request -- observed in practice as a short 2-byte packet
     * (e.g. `10 03`) instead of the expected 16-byte-plus-payload header, most likely the
     * firmware's own "file not found" error (matching the `readHandler`'s documented `File not
     * exist [%s]` string) rather than anything to do with our own request. Treated the same as an
     * empty file: for the manifest, that just means zero pending activities; for an activity file,
     * it's skipped (not marked synced) rather than left stuck retrying forever on this response.
     */
    private fun handleReadFailure(rawResponse: ByteArray) {
        val path = currentReadPath ?: return
        LOG.warn(
            "FTS read of {} failed (response: {}), treating as not found",
            path,
            StringUtils.bytesToHex(rawResponse),
        )
        onFileReadComplete(path, ByteArray(0))
    }

    private fun handleReadChunk(chunk: UnaFtsReadChunk) {
        val path = currentReadPath
        if (path == null) {
            LOG.warn("Got a read chunk with no pending read: offset={} total={}", chunk.offset, chunk.total)
            return
        }
        currentReadChunks[chunk.offset] = chunk.payload
        val nextOffset = chunk.offset + chunk.payload.size
        if (nextOffset < chunk.total) {
            requestChunk(path, nextOffset)
            return
        }
        val bytes = ByteArray(chunk.total)
        for ((offset, payload) in currentReadChunks) {
            System.arraycopy(payload, 0, bytes, offset, payload.size)
        }
        onFileReadComplete(path, bytes)
    }

    private fun onFileReadComplete(path: String, bytes: ByteArray) {
        if (readingManifest) {
            handleManifest(bytes)
        } else {
            if (handleActivityFile(path, bytes)) {
                markSynced(path)
            }
        }
        val next = pendingManifestPaths.removeFirstOrNull()
        if (next == null) {
            LOG.debug("FTS activity sync complete, starting daily health sync")
            pendingHealthDays.clear()
            val today = GregorianCalendar()
            for (i in 0 until HEALTH_HISTORY_DAYS) {
                pendingHealthDays.addLast((today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -i) })
            }
            requestNextDailyHealth()
        } else {
            startReadFile(next, isManifest = false)
        }
    }

    private fun handleManifest(bytes: ByteArray) {
        val alreadySynced = syncedPaths()
        val paths = String(bytes, Charsets.UTF_8).lineSequence()
            .map { it.trim() }
            .filter { it.endsWith(".fit") }
            .filter { it !in alreadySynced }
            .toList()
        LOG.debug("Manifest has {} new activity file(s) to sync", paths.size)
        pendingManifestPaths.addAll(paths)
    }

    /** Returns whether the file was successfully processed (parsed and persisted, if it had any
     * records) -- only then is it safe to mark the path synced and never revisit it. */
    private fun handleActivityFile(path: String, bytes: ByteArray): Boolean {
        return try {
            val fitFile = FitFile.parseIncoming(bytes)
            val records = fitFile.getRecordsByNativeMessage(NativeFITMessages.FIT_RECORD())
            persistWorkout(records, activityKindForPath(path))
        } catch (e: Exception) {
            LOG.error("Failed to parse activity file {}", path, e)
            false
        }
    }

    private fun activityKindForPath(path: String): ActivityKind = when {
        path.contains("/Running/") -> ActivityKind.RUNNING
        path.contains("/Cycling/") -> ActivityKind.CYCLING
        path.contains("/Hiking/") -> ActivityKind.HIKING
        path.contains("/Treadmill/") -> ActivityKind.TREADMILL
        path.contains("/Workout/") -> ActivityKind.EXERCISE
        else -> ActivityKind.ACTIVITY
    }

    // Each file is a single discrete recorded workout, not continuous all-day monitoring,
    // so it's stored as a BaseActivitySummary (like Garmin's FitImporter does for ACTIVITY-type
    // FIT files) rather than a per-second AbstractActivitySample stream.
    private fun persistWorkout(records: List<RecordData>, kind: ActivityKind): Boolean {
        val timestamps = records.mapNotNull { it.computedTimestamp }
        val startTimestamp = timestamps.minOrNull() ?: return true
        val endTimestamp = timestamps.maxOrNull()!!

        val heartRates = records.mapNotNull { (it.getFieldByName("heart_rate") as? Number)?.toInt() }
            .filter { it > 0 }
        val distances = records.mapNotNull { (it.getFieldByName("distance") as? Number)?.toDouble() }

        return try {
            GBApplication.acquireDB().use { dbHandler ->
                val session = dbHandler.daoSession
                val summary = ActivitySummaryParser.findOrCreateBaseActivitySummary(session, device, startTimestamp)
                summary.endTime = Date(endTimestamp * 1000L)
                summary.activityKind = kind.code

                val summaryData = ActivitySummaryData()
                if (heartRates.isNotEmpty()) {
                    summaryData.add(ActivitySummaryEntries.HR_AVG, heartRates.average(), ActivitySummaryEntries.UNIT_BPM)
                }
                distances.maxOrNull()?.let {
                    summaryData.add(ActivitySummaryEntries.DISTANCE_METERS, it, ActivitySummaryEntries.UNIT_METERS)
                }
                summary.summaryData = summaryData.toJson()

                BaseActivitySummaryProvider(device, session).persistSamples(listOf(summary), context)
            }
        } catch (e: Exception) {
            LOG.error("Error saving {} workout summary", kind, e)
            false
        }
    }

    // CCS's DailyHealth response gives one aggregate per day, not a real per-minute timeline, so
    // `activeMinutes + 1` synthetic one-minute-apart samples are persisted (all but the first
    // carrying zero steps) purely so Gadgetbridge's sample-gap-based active-minutes calculation
    // (ActivityAnalysis.calculateActivityAmounts) lands on the same number the watch reports --
    // the anchor time (noon) is arbitrary, not a real activity window.
    private fun persistDailyHealth(day: Calendar, health: UnaDailyHealth) {
        val anchor = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val anchorSeconds = (anchor.timeInMillis / 1000L).toInt()
        val sampleCount = health.activeMinutes.coerceAtLeast(0) + 1

        try {
            GBApplication.acquireDB().use { dbHandler ->
                val session = dbHandler.daoSession
                val dbUser = DBHelper.getUser(session)
                val dbDevice = DBHelper.getDevice(device, session)
                val provider = UnaDailySampleProvider(device, session)
                val samples = (0 until sampleCount).map { i ->
                    UnaDailySample().apply {
                        timestamp = anchorSeconds + i * 60
                        rawKind = ActivityKind.ACTIVITY.code
                        heartRate = health.averageHeartRate
                        restingHeartRate = health.restingHeartRate
                        steps = if (i == 0) health.steps else 0
                        floorsClimbed = if (i == 0) health.floors else 0
                        setDevice(dbDevice)
                        setUser(dbUser)
                        setProvider(provider)
                    }
                }
                provider.addGBActivitySamples(samples)
            }
        } catch (e: Exception) {
            LOG.error("Error saving daily health for {}", day.time, e)
        }
    }

    private fun syncedPaths(): Set<String> = devicePrefs.getStringSet(PREF_KEY_SYNCED_PATHS, emptySet())

    private fun markSynced(path: String) {
        val updated = HashSet(syncedPaths())
        updated.add(path)
        devicePrefs.preferences.edit {
            Prefs.putStringSet(this, PREF_KEY_SYNCED_PATHS, updated)
        }
    }

    private fun endDrain() {
        LOG.debug("Activity fetch complete")
        draining = false
        if (device.isBusy) {
            device.unsetBusyTask()
            device.sendDeviceUpdateIntent(context)
        }
        GB.signalActivityDataFinish(device)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(UnaDeviceSupport::class.java)
        private const val MANIFEST_PATH = "/Apps/latest_activity.txt"
        private const val PREF_KEY_SYNCED_PATHS = "una_synced_activity_paths"
        private const val HEALTH_HISTORY_DAYS = 7
    }
}
