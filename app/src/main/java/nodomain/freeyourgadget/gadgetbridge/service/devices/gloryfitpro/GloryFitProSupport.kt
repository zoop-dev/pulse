/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.gloryfitpro

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.devices.GloryFitStepsSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.GloryFitStepsSample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.Alarm
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.UUID

/**
 * Support for the "GloryFit Pro" BLE dialect (com.yc.gloryfitpro), used by watches such as the
 * DM58 (Actions ATS3085L). Unlike the classic GloryFit protocol, this variant frames every packet
 * as `01 | CMD | MODE | FIELD | LEN | VALUE...` (MODE aa=get, ab=set, ac=report; doubled=all) and
 * runs almost entirely over the "DATA" service (0x56ff, write 0x34f1 / notify 0x34f2). MVP scope:
 * connect + device info (firmware) + set time.
 */
class GloryFitProSupport : AbstractBTLESingleDeviceSupport(LOG) {
    init {
        addSupportedService(UUID_SERVICE_CMD)
        addSupportedService(UUID_SERVICE_DATA)
    }

    override fun useAutoConnect(): Boolean {
        return true
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        builder.setDeviceState(GBDevice.State.INITIALIZING)

        builder.requestMtu(247)

        // Subscribe to both notify characteristics (CMD 0x35f2 and DATA 0x34f2).
        builder.notify(UUID_CHARACTERISTIC_CMD_READ, true)
        builder.notify(UUID_CHARACTERISTIC_DATA_READ, true)

        // Ask the watch for its device info (firmware etc.).
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *cmdGetAll(CMD_DEVICE_INFO))

        // Enable the watch's "find phone" feature (otherwise the watch button is greyed out).
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *byteArrayOf(PKT_HEADER, CMD_DEVICE_CONTROL, MODE_SET, 0x0a, 0x01, 0x01))

        // Read the watch's alarms so ones edited on the watch appear in Gadgetbridge (read-only sync).
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *cmdGetAll(CMD_ALARM))

        if (GBApplication.getPrefs().syncTime()) {
            setTime(builder)
        }

        // Ask for today's step total.
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *cmdGetAll(CMD_ACTIVITY_DAY))

        // FIXME: likely too early, refine once the init handshake is fully understood.
        builder.setDeviceState(GBDevice.State.INITIALIZED)

        return builder
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        when (characteristic.uuid) {
            UUID_CHARACTERISTIC_DATA_READ, UUID_CHARACTERISTIC_CMD_READ -> {
                handlePacket(value)
                return true
            }
        }
        return super.onCharacteristicChanged(gatt, characteristic, value)
    }

    private fun handlePacket(value: ByteArray) {
        if (value.size < 3 || value[0] != PKT_HEADER) {
            LOG.debug("Ignoring unexpected packet: {}", value.toHex())
            return
        }
        val cmd = value[1]
        // Continuation/terminator packet: 01 CMD MODE fd <cksum>
        if (value.size >= 4 && value[3] == PKT_TERMINATOR) {
            return
        }
        when (cmd) {
            CMD_DEVICE_INFO -> handleDeviceInfo(value)
            CMD_ACTIVITY_DAY -> handleDaySummary(value)
            CMD_MUSIC_CONTROL -> handleMusicControl(value)
            CMD_DEVICE_CONTROL -> handleDeviceControl(value)
            CMD_ALARM -> handleAlarmData(value)
            else -> LOG.debug("Unhandled cmd 0x{}: {}", Integer.toHexString(cmd.toInt() and 0xff), value.toHex())
        }
    }

    /** Music remote button: "01 e2 ac 01 02 <code> 00" (device -> phone). */
    private fun handleMusicControl(value: ByteArray) {
        if (value.size < 6 || value[3] != 0x01.toByte() || value[4] != 0x02.toByte()) {
            return
        }
        val event = when (value[5].toInt() and 0xff) {
            0x01 -> GBDeviceEventMusicControl.Event.PLAY
            0x02 -> GBDeviceEventMusicControl.Event.PAUSE
            0x03 -> GBDeviceEventMusicControl.Event.PREVIOUS
            0x04 -> GBDeviceEventMusicControl.Event.NEXT
            0x05 -> GBDeviceEventMusicControl.Event.VOLUMEUP
            0x06 -> GBDeviceEventMusicControl.Event.VOLUMEDOWN
            else -> return
        }
        evaluateGBDeviceEvent(GBDeviceEventMusicControl(event))
    }

    override fun onSetPhoneVolume(volume: Float) {
        sendVolume(volume.toInt())
    }

    /** Volume to the watch: "01 e2 ab 01 01 <vol 0..100>". */
    private fun sendVolume(percent: Int) {
        val vol = percent.coerceIn(0, 100)
        val builder = createTransactionBuilder("set phone volume")
        builder.write(
            UUID_CHARACTERISTIC_DATA_WRITE,
            *byteArrayOf(PKT_HEADER, CMD_MUSIC_CONTROL, MODE_SET, 0x01, 0x01, vol.toByte())
        )
        builder.queue()
    }

    private var lastMusicSpec: MusicSpec? = null

    override fun onSetMusicInfo(musicSpec: MusicSpec) {
        lastMusicSpec = musicSpec
        sendMusicInfo(musicSpec.artist, musicSpec.track)
    }

    override fun onSetMusicState(stateSpec: MusicStateSpec) {
        // Resend the track so the watch shows it. Keeping the "playing" icon in sync and a live
        // volume bar would require streaming updates ~2x/s like the official app; that flooded the
        // connection here, so it's left as a follow-up (TODO).
        sendMusicInfo(lastMusicSpec?.artist, lastMusicSpec?.track)
    }

    /** Music info to the watch: "01 e2 abab 00 ab 01 <artist> ab 02 <title> ab 03/04/05 <state>". */
    private fun sendMusicInfo(artistRaw: String?, titleRaw: String?) {
        val artist = (artistRaw ?: "").take(32).toByteArray(Charsets.UTF_16BE)
        val title = (titleRaw ?: "").take(48).toByteArray(Charsets.UTF_16BE)
        val data = ByteArrayOutputStream()
        data.write(byteArrayOf(PKT_HEADER, CMD_MUSIC_CONTROL, MODE_SET, MODE_SET, 0x00))
        data.write(byteArrayOf(MODE_SET, 0x01, artist.size.toByte())); data.write(artist)
        data.write(byteArrayOf(MODE_SET, 0x02, title.size.toByte())); data.write(title)
        data.write(byteArrayOf(MODE_SET, 0x03, 0x01, 0x00))
        data.write(byteArrayOf(MODE_SET, 0x04, 0x01, 0x0f))
        data.write(byteArrayOf(MODE_SET, 0x05, 0x01, 0x08))
        val dataPkt = data.toByteArray()
        var xor = 0
        for (b in dataPkt) xor = xor xor (b.toInt() and 0xff)
        val terminator = byteArrayOf(PKT_HEADER, CMD_MUSIC_CONTROL, MODE_SET, MODE_SET, PKT_TERMINATOR, xor.toByte())
        val builder = createTransactionBuilder("set music info")
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *dataPkt)
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *terminator)
        builder.queue()
    }

    override fun onFetchRecordedData(dataTypes: Int) {
        val builder = createTransactionBuilder("fetch steps")
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *cmdGetAll(CMD_ACTIVITY_DAY))
        builder.queue()
    }

    /** Day summary reply "01 c3 aaaa 00 ... aa 0c <len> <inner sub-TLV>"; steps = inner subfield 0x05. */
    private fun handleDaySummary(value: ByteArray) {
        if (value.size < 5 || value[2] != MODE_GET) return
        val metrics = parseTlv(value, 5)[FIELD_DAY_METRICS] ?: return
        val steps = parseInnerInt(metrics, SUBFIELD_STEPS) ?: return
        storeDailySteps(steps)
    }

    /** Parse a variable-length big-endian integer [subfield] from an inner "<field><len><value>" TLV blob. */
    private fun parseInnerInt(blob: ByteArray, subfield: Int): Int? {
        var i = 0
        while (i + 2 <= blob.size) {
            val field = blob[i].toInt() and 0xff
            val len = blob[i + 1].toInt() and 0xff
            if (i + 2 + len > blob.size) break
            if (field == subfield && len >= 1) {
                var v = 0
                for (k in 0 until len) v = (v shl 8) or (blob[i + 2 + k].toInt() and 0xff)
                return v
            }
            i += 2 + len
        }
        return null
    }

    /**
     * Store today's step total as a delta relative to what was already recorded today, so
     * Gadgetbridge's per-interval sum equals the watch's daily total.
     */
    private fun storeDailySteps(total: Int) {
        try {
            GBApplication.acquireDB().use { handler ->
                val session = handler.daoSession
                val provider = GloryFitStepsSampleProvider(device, session)
                val cal = GregorianCalendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val now = System.currentTimeMillis()
                val recorded = provider.getAllSamples(cal.timeInMillis, now).sumOf { it.totalSteps }
                val delta = total - recorded
                LOG.info("DM58 daily steps total={} recorded={} delta={}", total, recorded, delta)
                if (delta <= 0) return
                val sample = GloryFitStepsSample()
                sample.timestamp = now
                sample.totalSteps = delta
                sample.runningStart = 0
                sample.runningEnd = 0
                sample.runningSteps = 0
                sample.walkingStart = 0
                sample.walkingEnd = 0
                sample.walkingSteps = 0
                provider.persistSamples(listOf(sample), context)
            }
        } catch (e: Exception) {
            LOG.error("Failed to store steps", e)
        }
    }

    private fun handleDeviceInfo(value: ByteArray) {
        if (value.size >= 5 && value[2] == MODE_REPORT) {
            // Pushed status update (e.g. on charger connect): 01 a4 ac <field> <len> <battery-blob>
            val len = value[4].toInt() and 0xff
            if (value.size >= 5 + len) {
                parseBattery(value.copyOfRange(5, 5 + len))
            }
            return
        }
        // Full device info reply: 01 a4 aa aa 00 [ aa <field> <len> <val...> ]*
        val fields = parseTlv(value, 5)
        fields[FIELD_FIRMWARE]?.let { fw ->
            val info = GBDeviceEventVersionInfo()
            info.fwVersion = String(fw)
            fields[FIELD_MODEL]?.let { info.hwVersion = String(it) }
            evaluateGBDeviceEvent(info)
            LOG.info("DM58 firmware={}", String(fw))
        }
        fields[FIELD_BATTERY]?.let { parseBattery(it) }
    }

    /** Parse the 6-byte battery blob "01 01 <level> 02 01 <charging>" (sub-TLV). */
    private fun parseBattery(blob: ByteArray) {
        var i = 0
        var level = -1
        var charging = 0
        while (i + 2 <= blob.size) {
            val field = blob[i].toInt() and 0xff
            val len = blob[i + 1].toInt() and 0xff
            if (i + 2 + len > blob.size) break
            when (field) {
                0x01 -> if (len >= 1) level = blob[i + 2].toInt() and 0xff
                0x02 -> if (len >= 1) charging = blob[i + 2].toInt() and 0xff
            }
            i += 2 + len
        }
        if (level in 0..100) {
            val event = GBDeviceEventBatteryInfo()
            event.level = level
            event.state = if (charging != 0) BatteryState.BATTERY_CHARGING else BatteryState.BATTERY_NORMAL
            evaluateGBDeviceEvent(event)
            LOG.info("DM58 battery={}% charging={}", level, charging != 0)
        }
    }

    override fun onNotification(notificationSpec: NotificationSpec) {
        val title = notificationSpec.title ?: notificationSpec.sender ?: notificationSpec.sourceName ?: ""
        val body = notificationSpec.body ?: ""
        sendNotification(title, body)
    }

    /**
     * Notification: 01 b0 abab 00 | ab 03 <len> <body UTF-16BE> | ab 07 <len> <title UTF-16BE> |
     * ab 08 01 <appIcon> | ab 09 01 01, followed by a terminator 01 b0 abab fd <xor-of-data-packet>.
     * Text is truncated to fit a single MTU-247 packet (chunking is a TODO).
     */
    private fun sendNotification(titleRaw: String, bodyRaw: String) {
        val title = titleRaw.take(32).toByteArray(Charsets.UTF_16BE)
        val body = bodyRaw.take(60).toByteArray(Charsets.UTF_16BE)

        val data = ByteArrayOutputStream()
        data.write(byteArrayOf(PKT_HEADER, CMD_NOTIFICATION, MODE_SET, MODE_SET, 0x00))
        data.write(byteArrayOf(MODE_SET, 0x03, body.size.toByte())); data.write(body)
        data.write(byteArrayOf(MODE_SET, 0x07, title.size.toByte())); data.write(title)
        data.write(byteArrayOf(MODE_SET, 0x08, 0x01, 0x04))  // app icon id (generic)
        data.write(byteArrayOf(MODE_SET, 0x09, 0x01, 0x01))
        val dataPkt = data.toByteArray()

        var xor = 0
        for (b in dataPkt) xor = xor xor (b.toInt() and 0xff)
        val terminator = byteArrayOf(PKT_HEADER, CMD_NOTIFICATION, MODE_SET, MODE_SET, PKT_TERMINATOR, xor.toByte())

        val builder = createTransactionBuilder("send notification")
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *dataPkt)
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *terminator)
        builder.queue()
    }

    override fun onFindDevice(start: Boolean) {
        val builder = createTransactionBuilder("find device $start")
        builder.write(
            UUID_CHARACTERISTIC_DATA_WRITE,
            *byteArrayOf(PKT_HEADER, CMD_DEVICE_CONTROL, MODE_SET, 0x04, 0x01, if (start) 0x01 else 0x00)
        )
        builder.queue()
    }

    /** Find-phone: the watch pushes "01 a5 ac 02 01 <01 start / 00 stop>". */
    private fun handleDeviceControl(value: ByteArray) {
        if (value.size >= 6 && value[2] == MODE_REPORT && value[3] == 0x02.toByte()) {
            val event = GBDeviceEventFindPhone()
            event.event = if (value[5].toInt() != 0) {
                GBDeviceEventFindPhone.Event.START
            } else {
                GBDeviceEventFindPhone.Event.STOP
            }
            evaluateGBDeviceEvent(event)
        }
    }

    override fun onSetAlarms(alarms: ArrayList<out Alarm>) {
        // 01 c7 abab 00 [per slot: ab 01 02 <idx><idx> | ab 02 02 <idx><weekdayMask> |
        //   ab 03 02 <idx><enabled> | ab 04 03 <idx><HH><MM> | ab 05 01 <idx>] + terminator.
        val data = ByteArrayOutputStream()
        data.write(byteArrayOf(PKT_HEADER, CMD_ALARM, MODE_SET, MODE_SET, 0x00))
        for ((i, alarm) in alarms.withIndex()) {
            val idx = (i + 1).toByte()
            val rep = alarm.repetition
            var mask = 0  // bit0=Sun, bit1=Mon .. bit6=Sat
            if (rep and Alarm.ALARM_SUN.toInt() != 0) mask = mask or 0x01
            if (rep and Alarm.ALARM_MON.toInt() != 0) mask = mask or 0x02
            if (rep and Alarm.ALARM_TUE.toInt() != 0) mask = mask or 0x04
            if (rep and Alarm.ALARM_WED.toInt() != 0) mask = mask or 0x08
            if (rep and Alarm.ALARM_THU.toInt() != 0) mask = mask or 0x10
            if (rep and Alarm.ALARM_FRI.toInt() != 0) mask = mask or 0x20
            if (rep and Alarm.ALARM_SAT.toInt() != 0) mask = mask or 0x40
            data.write(byteArrayOf(MODE_SET, 0x01, 0x02, idx, idx))
            data.write(byteArrayOf(MODE_SET, 0x02, 0x02, idx, mask.toByte()))
            data.write(byteArrayOf(MODE_SET, 0x03, 0x02, idx, if (alarm.enabled) 0x01 else 0x00))
            data.write(byteArrayOf(MODE_SET, 0x04, 0x03, idx, alarm.hour.toByte(), alarm.minute.toByte()))
            data.write(byteArrayOf(MODE_SET, 0x05, 0x01, idx))
        }
        queueTlvWithTerminator("set alarms", CMD_ALARM, data.toByteArray())
    }

    /** Read reply "01 c7 aaaa 00 [0f aa 01 01 <idx> aa 02 01 <mask> aa 03 01 <en> aa 04 02 <HH><MM>]*". */
    private fun handleAlarmData(value: ByteArray) {
        if (value.size < 7 || value[2] != MODE_GET || value[4] == PKT_TERMINATOR) return
        val parsed = HashMap<Int, IntArray>() // position -> [hour, minute, mask, enabled]
        var cur: IntArray? = null
        var i = 4 // after "01 c7 aaaa"; status(00) and per-alarm separators(0f) are skipped below
        while (i < value.size) {
            if (value[i] != MODE_GET) { i += 1; continue }
            if (i + 3 > value.size) break
            val field = value[i + 1].toInt() and 0xff
            val len = value[i + 2].toInt() and 0xff
            if (i + 3 + len > value.size) break
            when (field) {
                0x01 -> {
                    val pos = (value[i + 3].toInt() and 0xff) - 1
                    cur = intArrayOf(0, 0, 0, 0)
                    if (pos >= 0) parsed[pos] = cur!!
                }
                0x02 -> cur?.set(2, value[i + 3].toInt() and 0xff)
                0x03 -> cur?.set(3, value[i + 3].toInt() and 0xff)
                0x04 -> {
                    cur?.set(0, value[i + 3].toInt() and 0xff)
                    if (len >= 2) cur?.set(1, value[i + 4].toInt() and 0xff)
                }
            }
            i += 3 + len
        }
        if (parsed.isNotEmpty()) storeAlarmsFromWatch(parsed)
    }

    private fun storeAlarmsFromWatch(parsed: Map<Int, IntArray>) {
        // Update Gadgetbridge's alarm DB only; do NOT re-send to the watch (avoids a sync loop that
        // could overwrite alarms just edited on the watch).
        val dbAlarms = DBHelper.getAlarms(device)
        for (dbAlarm in dbAlarms) {
            val a = parsed[dbAlarm.position] ?: continue
            dbAlarm.unused = false
            dbAlarm.enabled = a[3] != 0
            dbAlarm.hour = a[0]
            dbAlarm.minute = a[1]
            dbAlarm.repetition = maskToRepetition(a[2])
            DBHelper.store(dbAlarm)
        }
        LOG.info("DM58 loaded {} alarms from watch", parsed.size)
        // Refresh the alarm UI from the DB (does not re-send to the watch).
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(DeviceService.ACTION_SAVE_ALARMS))
    }

    private fun maskToRepetition(mask: Int): Int {
        var rep = 0
        if (mask and 0x01 != 0) rep = rep or Alarm.ALARM_SUN.toInt()
        if (mask and 0x02 != 0) rep = rep or Alarm.ALARM_MON.toInt()
        if (mask and 0x04 != 0) rep = rep or Alarm.ALARM_TUE.toInt()
        if (mask and 0x08 != 0) rep = rep or Alarm.ALARM_WED.toInt()
        if (mask and 0x10 != 0) rep = rep or Alarm.ALARM_THU.toInt()
        if (mask and 0x20 != 0) rep = rep or Alarm.ALARM_FRI.toInt()
        if (mask and 0x40 != 0) rep = rep or Alarm.ALARM_SAT.toInt()
        return rep
    }

    override fun onSendWeather() {
        val weather: WeatherSpec = Weather.getWeatherSpec() ?: return
        val city = (weather.location ?: "").take(24).toByteArray(Charsets.UTF_16BE)
        val cur = (weather.currentTemp - 273)
        val high = (weather.todayMaxTemp - 273)
        val low = (weather.todayMinTemp - 273)
        val humidity = weather.currentHumidity.coerceIn(0, 100)
        val uv = weather.uvIndex.toInt().coerceIn(0, 15)
        val ts = weather.timestamp

        // Chunk 0 = current weather (forecast chunks are a TODO).
        val data = ByteArrayOutputStream()
        data.write(byteArrayOf(PKT_HEADER, CMD_WEATHER, MODE_SET, MODE_SET, 0x00))
        data.write(byteArrayOf(MODE_SET, 0x05, city.size.toByte())); data.write(city)
        data.write(byteArrayOf(MODE_SET, 0x06, 0x02, hi(cur), lo(cur)))
        data.write(byteArrayOf(MODE_SET, 0x07, 0x02, hi(high), lo(high)))
        data.write(byteArrayOf(MODE_SET, 0x08, 0x02, hi(low), lo(low)))
        data.write(byteArrayOf(MODE_SET, 0x0a, 0x01, uv.toByte()))
        data.write(byteArrayOf(MODE_SET, 0x0b, 0x04, (ts ushr 24).toByte(), (ts ushr 16).toByte(), (ts ushr 8).toByte(), ts.toByte()))
        // f0f = [humidity][condition]; condition mapping to the watch's codes is a TODO.
        data.write(byteArrayOf(MODE_SET, 0x0f, 0x02, humidity.toByte(), 0x00))
        queueTlvWithTerminator("send weather", CMD_WEATHER, data.toByteArray())
    }

    private fun hi(v: Int): Byte = ((v ushr 8) and 0xff).toByte()
    private fun lo(v: Int): Byte = (v and 0xff).toByte()

    /** Write a TLV "01 CMD abab 00 ..." data packet followed by "01 CMD abab fd <xor>". */
    private fun queueTlvWithTerminator(label: String, cmd: Byte, dataPkt: ByteArray) {
        var xor = 0
        for (b in dataPkt) xor = xor xor (b.toInt() and 0xff)
        val terminator = byteArrayOf(PKT_HEADER, cmd, MODE_SET, MODE_SET, PKT_TERMINATOR, xor.toByte())
        val builder = createTransactionBuilder(label)
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *dataPkt)
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *terminator)
        builder.queue()
    }

    private fun setTime(builder: TransactionBuilder) {
        val now = GregorianCalendar.getInstance()
        val epoch = (now.timeInMillis / 1000L).toInt()  // watch expects UTC unix time
        // Timezone field: [0x80|hours][minutes] for offsets east of UTC (e.g. UTC+2 -> 0x82 0x00).
        val offsetMinutes = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET)) / 60_000
        val tzHours = Math.abs(offsetMinutes) / 60
        val tzMinutes = Math.abs(offsetMinutes) % 60
        val tzSign = if (offsetMinutes >= 0) 0x80 else 0x00
        val buf = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
        buf.put(PKT_HEADER)
        buf.put(CMD_TIME)
        buf.put(0x01)          // field: set time
        buf.put(0x04)          // len 4
        buf.putInt(epoch)      // big-endian UTC unix timestamp
        buf.put(0x10)          // field: timezone
        buf.put(0x02)          // len 2
        buf.put((tzSign or tzHours).toByte())
        buf.put(tzMinutes.toByte())
        builder.write(UUID_CHARACTERISTIC_DATA_WRITE, *buf.array())
    }

    /** Build a "get all fields" request: 01 CMD MODE MODE (mode byte doubled). */
    private fun cmdGetAll(cmd: Byte): ByteArray {
        return byteArrayOf(PKT_HEADER, cmd, MODE_GET, MODE_GET)
    }

    /** Parse repeating `<MODE> <field> <len> <value...>` TLV entries starting at [start]. */
    private fun parseTlv(value: ByteArray, start: Int): Map<Byte, ByteArray> {
        val out = HashMap<Byte, ByteArray>()
        var i = start
        while (i + 3 <= value.size) {
            // mode byte (aa/ab/ac), field, len
            val field = value[i + 1]
            val len = value[i + 2].toInt() and 0xff
            val from = i + 3
            val to = from + len
            if (to > value.size) break
            out[field] = value.copyOfRange(from, to)
            i = to
        }
        return out
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GloryFitProSupport::class.java)

        val UUID_SERVICE_CMD: UUID = UUID.fromString("000055ff-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_CMD_WRITE: UUID = UUID.fromString("000035f1-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_CMD_READ: UUID = UUID.fromString("000035f2-0000-1000-8000-00805f9b34fb")

        val UUID_SERVICE_DATA: UUID = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_DATA_WRITE: UUID = UUID.fromString("000034f1-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_DATA_READ: UUID = UUID.fromString("000034f2-0000-1000-8000-00805f9b34fb")

        const val PKT_HEADER: Byte = 0x01
        const val PKT_TERMINATOR: Byte = 0xfd.toByte()

        const val MODE_GET: Byte = 0xaa.toByte()
        const val MODE_SET: Byte = 0xab.toByte()
        const val MODE_REPORT: Byte = 0xac.toByte()

        const val CMD_BATTERY: Byte = 0xa2.toByte()
        const val CMD_TIME: Byte = 0xa3.toByte()
        const val CMD_DEVICE_INFO: Byte = 0xa4.toByte()
        const val CMD_NOTIFICATION: Byte = 0xb0.toByte()
        const val CMD_ACTIVITY_DAY: Byte = 0xc3.toByte()
        const val CMD_MUSIC_CONTROL: Byte = 0xe2.toByte()
        const val CMD_DEVICE_CONTROL: Byte = 0xa5.toByte()
        const val CMD_ALARM: Byte = 0xc7.toByte()
        const val CMD_WEATHER: Byte = 0xe0.toByte()

        const val FIELD_MODEL: Byte = 0x02
        const val FIELD_FIRMWARE: Byte = 0x15
        const val FIELD_BATTERY: Byte = 0x16
        const val FIELD_DAY_METRICS: Byte = 0x0c
        const val SUBFIELD_STEPS: Int = 0x05
    }
}
