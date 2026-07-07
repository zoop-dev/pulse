/*  Copyright (C) 2026 ExploWare

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
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec
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

        if (GBApplication.getPrefs().syncTime()) {
            setTime(builder)
        }

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
            else -> LOG.debug("Unhandled cmd 0x{}: {}", Integer.toHexString(cmd.toInt() and 0xff), value.toHex())
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

        const val FIELD_MODEL: Byte = 0x02
        const val FIELD_FIRMWARE: Byte = 0x15
        const val FIELD_BATTERY: Byte = 0x16
    }
}
