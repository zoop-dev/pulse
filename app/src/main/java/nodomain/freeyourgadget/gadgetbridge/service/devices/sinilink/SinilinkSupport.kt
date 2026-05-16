package nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import androidx.core.text.isDigitsOnly
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink.SinilinkSupport.Companion.UUID_CHARACTERISTIC_SINILINK_RX
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.UUID

class SinilinkSupport : AbstractBTLESingleDeviceSupport(LOG) {
    private var gotName = false
    private var gotFirmware = false
    private var gotStatus = false
    private var gotEvent = false

    init {
        addSupportedService(UUID_SERVICE_SINILINK)
    }

    override fun useAutoConnect(): Boolean = true

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        gotName = false
        gotFirmware = false
        gotStatus = false
        gotEvent = false

        builder.setDeviceState(GBDevice.State.INITIALIZING)

        builder.notify(UUID_CHARACTERISTIC_SINILINK_RX, true)

        // Request version
        builder.write(UUID_CHARACTERISTIC_SINILINK_TX, *encodeFrame(byteArrayOf(CMD_VERSION.toByte())))

        // Request status (volume, prompt tone, password)
        builder.write(UUID_CHARACTERISTIC_SINILINK_TX, *encodeFrame(byteArrayOf(CMD_STATUS_GET.toByte())))

        // Read the TX characteristic, this triggers an event (?)
        builder.read(UUID_CHARACTERISTIC_SINILINK_TX)

        // Request name
        builder.write(UUID_CHARACTERISTIC_SINILINK_TX, *encodeFrame(byteArrayOf(CMD_NAME_GET.toByte())))

        // Will be set to initialized once we receive the settings

        return builder
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Boolean {
        if (characteristic.uuid == UUID_CHARACTERISTIC_SINILINK_RX) {
            parseFrame(value)
            return true
        }

        return super.onCharacteristicChanged(gatt, characteristic, value)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ): Boolean {
        if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == UUID_CHARACTERISTIC_SINILINK_TX) {
            parseFrame(value)
            return true
        }

        return super.onCharacteristicRead(gatt, characteristic, value, status)
    }

    private fun parseFrame(frame: ByteArray) {
        if (frame.size < 5) {
            LOG.warn("Frame too short: {}", frame.size)
            return
        }

        if (frame[0].toInt() and 0xff != FRAME_SOF) {
            LOG.warn("Unexpected SOF: 0x{}", Integer.toHexString(frame[0].toInt() and 0xff))
            return
        }

        val declaredLen = frame[1].toInt() and 0xff
        if (declaredLen != frame.size) {
            LOG.warn("Length mismatch: declared={}, actual={}", declaredLen, frame.size)
            return
        }

        val checksumHiIdx = frame.size - 2
        val calculated = frame.slice(0 until checksumHiIdx).sumOf { it.toInt() and 0xff } % 65536
        val received = ((frame[checksumHiIdx].toInt() and 0xff) shl 8) or (frame[checksumHiIdx + 1].toInt() and 0xff)
        if (calculated != received) {
            LOG.warn(
                "Checksum mismatch: calculated=0x{}, received=0x{}",
                Integer.toHexString(calculated),
                Integer.toHexString(received),
            )
            return
        }

        val payload = frame.sliceArray(2 until checksumHiIdx)
        if (payload.isEmpty()) {
            LOG.warn("Empty payload")
            return
        }

        try {
            handlePayload(payload)
        } catch (e: Exception) {
            LOG.error("Failed to handle payload", e)
        }

        if (gotName && gotStatus && gotFirmware && gotEvent && !device.isInitialized) {
            device.state = GBDevice.State.INITIALIZED
            device.sendDeviceUpdateIntent(context)
        }
    }

    private fun handlePayload(payload: ByteArray) {
        LOG.debug("Got payload: {}", payload.toHexString())

        when (val cmd = payload[0].toInt() and 0xff) {
            CMD_STATUS_GET -> handleStatus(payload)
            CMD_EVENT_NOTIFICATION_1, CMD_EVENT_NOTIFICATION_2 -> handleEvent(payload)
            CMD_VERSION -> handleVersion(payload)
            CMD_NAME_GET -> handleName(payload)
            else -> {
                LOG.debug("Unhandled notification command: 0x{}", Integer.toHexString(cmd))
            }
        }
    }

    private fun handleStatus(payload: ByteArray) {
        if (payload.size != 12) {
            LOG.warn("Status payload has unexpected size: {}", payload.size)
            return
        }

        val promptTone = payload[1].toInt() and 0xff != 0
        val passwordEnabled = payload[2].toInt() and 0xff != 0
        val volume = payload[3].toInt() and 0xff
        val passwordTxt = String(payload.sliceArray(4..7), Charsets.US_ASCII)

        LOG.debug(
            "Status: volume={}, promptTone={}, passwordEnabled={}, passwordTxt={}",
            volume,
            promptTone,
            passwordEnabled,
            passwordTxt
        )

        evaluateGBDeviceEvent(
            GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsPreferenceConst.PREF_VOLUME, volume)
                .withPreference(DeviceSettingsPreferenceConst.PREF_PROMPT_TONE, promptTone)
                .withPreference(PasswordCapabilityImpl.PREF_PASSWORD_ENABLED, passwordEnabled)
                .withPreference(PasswordCapabilityImpl.PREF_PASSWORD, passwordTxt)
        )

        gotStatus = true
    }

    private fun handleEvent(payload: ByteArray) {
        if (payload.size != 11) {
            LOG.warn("Unexpected event payload size: {}", payload.size)
        }

        val sourceCode = payload[2].toInt() and 0xff
        val playbackStateCode = payload[3].toInt() and 0xff
        val playbackModeCode = payload[5].toInt() and 0xff
        val equalizerCode = payload[6].toInt() and 0xff

        val source = SinilinkMediaSource.fromCode(sourceCode)
        val playbackState = SinilinkPlaybackState.fromCode(playbackStateCode)
        val playbackMode = SinilinkPlaybackMode.fromCode(playbackModeCode)
        val equalizer = SinilinkEqualizer.fromCode(equalizerCode)

        LOG.debug(
            "Event: source={}, playbackState={}, playbackMode={}, equalizer={}",
            source,
            playbackState,
            playbackMode,
            equalizer
        )

        val event = GBDeviceEventUpdatePreferences()
        source?.let { event.withPreference(DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE, it.name.lowercase()) }
        playbackState?.let { device.setExtraInfo("playback_state", it.name) }
        playbackMode?.let {
            event.withPreference(
                DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE,
                it.name.lowercase()
            )
        }
        equalizer?.let {
            event.withPreference(
                DeviceSettingsPreferenceConst.PREF_HEADPHONES_EQUALIZER,
                it.name.lowercase()
            )
        }
        evaluateGBDeviceEvent(event)

        // Propagate device state to device card
        device.sendDeviceUpdateIntent(context)

        val version = String(payload.sliceArray(7 until 11), Charsets.US_ASCII)
        val versionEvent = GBDeviceEventVersionInfo()
        versionEvent.fwVersion = version
        evaluateGBDeviceEvent(versionEvent)
        gotEvent = true
    }

    private fun handleVersion(payload: ByteArray) {
        if (payload.size != 5) {
            LOG.warn("Unexpected version payload size: {}", payload.size)
            return
        }

        val version = String(payload.sliceArray(1 until 5), Charsets.US_ASCII)
        LOG.debug("Version: {}", version)
        val event = GBDeviceEventVersionInfo()
        event.fwVersion = version
        evaluateGBDeviceEvent(event)
        gotFirmware = true
    }

    private fun handleName(payload: ByteArray) {
        if (payload.size < 11) {
            LOG.warn("Unexpected name payload size: {}", payload.size)
            return
        }
        val name = String(payload.sliceArray(1 until 11), Charsets.US_ASCII).trimEnd('\u0000')
        LOG.debug("Device name: {}", name)
        evaluateGBDeviceEvent(
            GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsPreferenceConst.PREF_DEVICE_NAME, name)
        )
        gotName = true
    }

    override fun onSendConfiguration(config: String) {
        val prefs = Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address))
        when (config) {
            SinilinkButton.PREVIOUS.name,
            SinilinkButton.PLAY_PAUSE.name,
            SinilinkButton.NEXT.name -> {
                val button = SinilinkButton.valueOf(config)
                LOG.debug("Sending button: {}", button)
                sendSimpleCommand(button.code)
            }

            DeviceSettingsPreferenceConst.PREF_HEADPHONES_EQUALIZER -> {
                val value = prefs.getString(
                    DeviceSettingsPreferenceConst.PREF_HEADPHONES_EQUALIZER,
                    SinilinkEqualizer.NORMAL.name.lowercase()
                )
                LOG.debug("Setting equalizer to {}", value)
                SinilinkEqualizer.fromPreference(value)?.let { sendSimpleCommand(it.code) }
            }

            DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE -> {
                val value = prefs.getString(
                    DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE,
                    SinilinkPlaybackMode.SINGLE_HEAD.name.lowercase()
                )
                LOG.debug("Setting playback mode to {}", value)
                SinilinkPlaybackMode.fromPreference(value)?.let { sendSimpleCommand(it.code) }
            }

            DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE -> {
                val value = prefs.getString(
                    DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE,
                    SinilinkMediaSource.BLUETOOTH.name.lowercase()
                )
                LOG.debug("Setting media source to {}", value)
                SinilinkMediaSource.fromPreference(value)?.let { sendSimpleCommand(it.code) }
            }

            DeviceSettingsPreferenceConst.PREF_VOLUME -> {
                val value = prefs.getInt(DeviceSettingsPreferenceConst.PREF_VOLUME, 10).coerceIn(0, 30)
                LOG.debug("Setting volume to {}", value)
                sendVolumeCommand(value)
            }

            DeviceSettingsPreferenceConst.PREF_PROMPT_TONE -> {
                LOG.debug("Toggling prompt tone")
                sendSimpleCommand(CMD_PROMPT_TONE_TOGGLE)
            }

            DeviceSettingsPreferenceConst.PREF_DEVICE_NAME -> {
                val value = prefs.getString(DeviceSettingsPreferenceConst.PREF_DEVICE_NAME, "XinYi").replace("[^\\x20-\\x7e]", "")
                LOG.debug("Setting device name to {}", value)
                if (value.isEmpty() || value.length > 10) {
                    LOG.warn("Invalid name: {}", value)
                    return
                }
                LOG.debug("Setting name to {}", value)
                sendTextCommand(CMD_NAME_SET.toByte(), value)
            }

            PasswordCapabilityImpl.PREF_PASSWORD_ENABLED -> {
                LOG.debug("Toggling password")
                sendSimpleCommand(CMD_PASSWORD_TOGGLE)
            }

            PasswordCapabilityImpl.PREF_PASSWORD -> {
                val value = prefs.getString(PasswordCapabilityImpl.PREF_PASSWORD, "1234")
                if (value.length != 4 || !value.isDigitsOnly()) {
                    LOG.warn("Invalid password: {}", value)
                    return
                }
                LOG.debug("Setting password to {}", value)
                sendTextCommand(CMD_PASSWORD_SET.toByte(), value)
            }

            else -> super.onSendConfiguration(config)
        }
    }

    private fun sendSimpleCommand(cmd: Int) {
        val builder = createTransactionBuilder("sinilink_cmd_0x${Integer.toHexString(cmd)}")
        builder.write(UUID_CHARACTERISTIC_SINILINK_TX, *encodeFrame(byteArrayOf(cmd.toByte())))
        builder.queue()
    }

    private fun sendVolumeCommand(volume: Int) {
        val payload = ByteArray(11)
        payload[0] = CMD_SET_VOLUME.toByte()
        payload[1] = volume.toByte()
        val builder = createTransactionBuilder("sinilink_set_volume")
        builder.write(UUID_CHARACTERISTIC_SINILINK_TX, *encodeFrame(payload))
        builder.queue()
    }

    private fun sendTextCommand(command: Byte, txt: String) {
        val buf = ByteBuffer.allocate(11)
        buf.put(command)
        buf.put(txt.encodeToByteArray())
        val builder = createTransactionBuilder("sinilink_set_text")
        builder.write(UUID_CHARACTERISTIC_SINILINK_TX, *encodeFrame(buf.array()))
        builder.queue()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SinilinkSupport::class.java)

        private const val FRAME_SOF = 0x7e

        private const val CMD_EVENT_NOTIFICATION_1 = 0xa8
        private const val CMD_NAME_GET = 0x20
        private const val CMD_NAME_SET = 0x1a
        private const val CMD_PASSWORD_SET = 0x1b
        private const val CMD_SET_VOLUME = 0x1d
        private const val CMD_VERSION = 0x1e
        private const val CMD_STATUS_GET = 0x1f
        private const val CMD_EVENT_NOTIFICATION_2 = 0xc3
        private const val CMD_PROMPT_TONE_TOGGLE = 0x18
        private const val CMD_PASSWORD_TOGGLE = 0x19

        val UUID_SERVICE_SINILINK: UUID = UUID.fromString("0000ae00-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_SINILINK_RX: UUID = UUID.fromString("0000ae04-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_SINILINK_TX: UUID = UUID.fromString("0000ae10-0000-1000-8000-00805f9b34fb")

        /// Frame format: [0x7e][total_length][payload...][checksum_hi][checksum_lo]
        /// Checksum = sum of all bytes before checksum, mod 65536, big-endian
        fun encodeFrame(payload: ByteArray): ByteArray {
            val totalLen = 2 + payload.size + 2 // SOF + len + payload + checksum(2)
            val frame = ByteArray(totalLen)
            frame[0] = FRAME_SOF.toByte()
            frame[1] = totalLen.toByte()
            payload.copyInto(frame, 2)
            val checksum = frame.slice(0 until (totalLen - 2)).sumOf { it.toInt() and 0xff } % 65536
            frame[totalLen - 2] = (checksum shr 8).toByte()
            frame[totalLen - 1] = (checksum and 0xff).toByte()
            return frame
        }
    }
}
