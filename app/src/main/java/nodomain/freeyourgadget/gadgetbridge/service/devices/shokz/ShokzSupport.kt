package nodomain.freeyourgadget.gadgetbridge.service.devices.shokz

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.multipoint.MultipointDevice
import nodomain.freeyourgadget.gadgetbridge.activities.multipoint.MultipointPairingActivity
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneBTBRDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.stringUntilNullTerminator
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import kotlin.math.floor

class ShokzSupport : AbstractHeadphoneBTBRDeviceSupport(LOG, MAX_MTU) {
    private val packetBuffer: ByteBuffer = ByteBuffer.allocate(MAX_MTU).order(ByteOrder.LITTLE_ENDIAN)

    private val messageQueue: Queue<ShokzMessage> = LinkedList()
    private var pendingMessage: ShokzMessage? = null
    private var timeoutRetries = 0
    private val timeoutHandler = Handler()

    init {
        addSupportedService(UUID_SERVICE_SHOKZ)
    }

    override fun useAutoConnect(): Boolean {
        return true
    }

    override fun setContext(gbDevice: GBDevice, btAdapter: BluetoothAdapter, context: Context) {
        super.setContext(gbDevice, btAdapter, context)
        setupMultipointBroadcastReceiver()
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        packetBuffer.clear()
        timeoutHandler.removeCallbacksAndMessages(null)
        messageQueue.clear()
        pendingMessage = null
        timeoutRetries = 0

        builder.setDeviceState(GBDevice.State.INITIALIZING)

        // Send the fw version request directly, but queue everything else, otherwise the device does not respond to all
        builder.write(*encodeCommand(ShokzCommand.FIRMWARE_GET))

        pendingMessage = ShokzMessage(ShokzCommand.FIRMWARE_GET)
        timeoutHandler.postDelayed({ onCommandTimeout() }, 2000L)
        queueCommand(ShokzCommand.MEDIA_SOURCE_GET)
        queueCommand(ShokzCommand.BATTERY_GET)
        queueCommand(ShokzCommand.EQUALIZER_GET)
        queueCommand(ShokzCommand.PLAYBACK_STATUS_GET)
        queueCommand(ShokzCommand.VOLUME_GET)
        queueCommand(ShokzCommand.MP3_PLAYBACK_MODE_GET)
        queueCommand(ShokzCommand.CONTROLS_GET)
        queueCommand(ShokzCommand.LANGUAGE_GET)

        return builder
    }

    override fun dispose() {
        synchronized(ConnectionMonitor) {
            timeoutHandler.removeCallbacksAndMessages(null)
            LocalBroadcastManager.getInstance(context).unregisterReceiver(multipointBroadcastReceiver)
            super.dispose()
        }
    }

    override fun onSocketRead(data: ByteArray) {
        packetBuffer.put(data)
        packetBuffer.flip()

        while (packetBuffer.hasRemaining()) {
            packetBuffer.mark()

            if (packetBuffer.remaining() < 10) {
                // not enough bytes for min packet
                packetBuffer.reset()
                break
            }

            val preamble = packetBuffer.getShort()
            if (preamble != PACKET_PREAMBLE) {
                LOG.warn("Unexpected byte 0x{} is not preamble, skipping 2b", preamble.toHexString())
                continue
            }

            val packetLength = packetBuffer.getShort()
            if (packetBuffer.remaining() < 4 + packetLength) {
                // not enough bytes for packetIdx + packet
                packetBuffer.reset()
                break
            }

            packetBuffer.get().toInt() // always 1?
            packetBuffer.get().toInt() // isAck?
            packetBuffer.getShort() // always 0?
            val payloadLength = packetBuffer.getShort().toInt() and 0xffff

            if (packetBuffer.remaining() < 2 + payloadLength) {
                // not enough bytes for payload + crc
                packetBuffer.reset()
                break
            }

            val crc = packetBuffer.getShort().toInt() and 0xffff
            val payload = ByteArray(payloadLength)
            packetBuffer.get(payload)

            val expectedCrc = CheckSums.crc16_maxim(payload, 0, payload.size)
            if (crc != expectedCrc) {
                LOG.warn("Invalid CRC: got 0x{}, expected 0x{}", crc.toHexString(), expectedCrc.toHexString())
                continue
            }

            var sendNext: Boolean
            try {
                sendNext = handlePayload(payload)
            } catch (e: Exception) {
                LOG.error("Failed to handle payload", e)
                sendNext = true
            }

            if (sendNext) {
                sendNextCommand()
            }
        }

        packetBuffer.compact()
    }

    override fun onSendConfiguration(config: String) {
        when (config) {
            DeviceSettingsPreferenceConst.PREF_LANGUAGE -> setLanguage()
            DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_BLUETOOTH,
            DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_MP3 -> setEqualizer(config)

            DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE -> setMediaSource()
            DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE -> setMediaPlaybackMode()
            DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_LONG_PRESS_MULTI_FUNCTION,
            DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_SIMULTANEOUS_VOLUME_UP_DOWN -> setControls()

            else -> super.onSendConfiguration(config)
        }
    }

    private fun setLanguage() {
        val localeString: String = devicePrefs.getString("language", "en")
        val language = ShokzLanguage.fromLocale(localeString) ?: run {
            LOG.warn("Unknown language {}, falling back to english", localeString)
            ShokzLanguage.ENGLISH
        }
        LOG.info("Setting language to {}", language)

        queueCommand(
            ShokzCommand.LANGUAGE_SET,
            byteArrayOf(language.code.toByte(), 0x00, 0x00, 0x00)
        )
    }

    private fun setEqualizer(config: String) {
        val value: String = devicePrefs.getString(config, "standard")
        val equalizer = ShokzEqualizer.fromPreference(value) ?: run {
            LOG.warn("Unknown equalizer {}, falling back to standard", value)
            ShokzEqualizer.STANDARD
        }

        LOG.info("Setting equalizer to {}", equalizer)

        val args = when (equalizer) {
            ShokzEqualizer.STANDARD,
            ShokzEqualizer.SWIMMING -> {
                byteArrayOf(
                    equalizer.code.toByte(), 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00
                )
            }

            ShokzEqualizer.VOCAL -> {
                byteArrayOf(
                    equalizer.code.toByte(), 0xfc.toByte(), 0x00, 0x03,
                    0x02, 0x02, 0x00, 0x00
                )
            }
        }
        queueCommand(ShokzCommand.EQUALIZER_SET, args)
    }

    private fun setMediaSource() {
        val value: String = devicePrefs.getString(DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE, "bluetooth")
        val mediaSource = ShokzMediaSource.fromPreference(value) ?: run {
            LOG.warn("Unknown media source {}, falling back to bluetooth", value)
            ShokzMediaSource.BLUETOOTH
        }

        LOG.info("Setting media source to {}", mediaSource)

        queueCommand(
            ShokzCommand.MEDIA_SOURCE_SET,
            byteArrayOf(mediaSource.code.toByte(), 0x00, 0x00, 0x00)
        )
    }

    private fun setMediaPlaybackMode() {
        val value: String = devicePrefs.getString(DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE, "normal")
        val playbackMode = ShokzMp3PlaybackMode.fromPreference(value) ?: run {
            LOG.warn("Unknown playback mode {}, falling back to normal", value)
            ShokzMp3PlaybackMode.NORMAL
        }

        LOG.info("Setting playback mode to {}", playbackMode)

        queueCommand(
            ShokzCommand.MP3_PLAYBACK_MODE_SET,
            byteArrayOf(playbackMode.code.toByte(), 0x00, 0x00, 0x00)
        )
    }

    private fun setControls() {
        val valueLongPress: String = devicePrefs.getString(
            DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_LONG_PRESS_MULTI_FUNCTION,
            "standard"
        )
        val valueVolumeUpDown: String = devicePrefs.getString(
            DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_SIMULTANEOUS_VOLUME_UP_DOWN,
            "standard"
        )

        val controls = when {
            valueLongPress == "assistant" && valueVolumeUpDown == "media_source" -> ShokzControls.ASSISTANT__MEDIA_SOURCE
            valueLongPress == "media_source" && valueVolumeUpDown == "assistant" -> ShokzControls.MEDIA_SOURCE__ASSISTANT
            valueLongPress == "media_source" && valueVolumeUpDown == "media_source" -> ShokzControls.MEDIA_SOURCE__MEDIA_SOURCE
            valueLongPress == "assistant" && valueVolumeUpDown == "assistant" -> ShokzControls.ASSISTANT__ASSISTANT
            else -> {
                LOG.warn(
                    "Unknown controls {}/{}, falling back to {}",
                    valueLongPress, valueVolumeUpDown,
                    ShokzControls.ASSISTANT__MEDIA_SOURCE
                )
                ShokzControls.ASSISTANT__MEDIA_SOURCE
            }
        }

        LOG.info("Setting controls to {}", controls)

        queueCommand(
            ShokzCommand.CONTROLS_SET,
            byteArrayOf(controls.code.toByte(), 0x00, 0x00, 0x00)
        )
    }

    private fun handlePayload(payload: ByteArray): Boolean {
        if (LOG.isTraceEnabled) {
            LOG.trace("Processing payload: {}", payload.toHexString())
        }

        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val unk1 = buf.getInt() // 0x01
        if (unk1 != 0x01) {
            LOG.warn("Unexpected unk1 0x{}", unk1.toHexString())
            return true
        }
        val len = buf.getInt()
        if (len != buf.remaining()) {
            LOG.warn("Unexpected number of bytes in payload: got 0x{}, expected 0x{}", buf.remaining(), len)
            return true
        }
        val unk2 = buf.getInt() // 0x02
        if (unk2 != 0x02) {
            LOG.warn("Unexpected unk2 0x{}", unk2.toHexString())
            return true
        }
        val unk3 = buf.getInt() // 0x04
        if (unk3 != 0x04) {
            LOG.warn("Unexpected unk3 0x{}", unk3.toHexString())
            return true
        }
        val group = buf.getInt()
        val unk4 = buf.getInt() // 0x03
        if (unk4 != 0x03) {
            LOG.warn("Unexpected unk4 0x{}", unk4.toHexString())
            return true
        }
        val unk5 = buf.getInt() // 0x04
        if (unk5 != 0x04) {
            LOG.warn("Unexpected unk5 0x{}", unk5.toHexString())
            return true
        }
        val code = buf.getInt()
        val unk6 = buf.getInt() // 0x04
        if (unk6 != 0x04) {
            LOG.warn("Unexpected unk6 0x{}", unk6.toHexString())
            return true
        }
        val argsLength = buf.getInt()
        val args = ByteArray(argsLength)
        buf.get(args)

        val command = ShokzCommand.getCommand(group, code)
        if (command == null) {
            LOG.warn("Unknown command: group 0x{}, code 0x{}", group.toHexString(), code.toHexString())
            return true
        }

        handleCommand(command, args)

        return pendingMessage?.let { msg ->
            (msg.command.code or MASK_RESPONSE) == command.code
        } ?: false
    }

    private fun handleCommand(command: ShokzCommand, args: ByteArray) {
        val buf = ByteBuffer.wrap(args).order(ByteOrder.LITTLE_ENDIAN)

        LOG.debug("Handling command {} args={}", command, args.toHexString())

        when (command) {
            ShokzCommand.FIRMWARE_RET -> {
                val zero = buf.get().toInt()
                if (zero != 0) {
                    LOG.warn("Unexpected non-zero byte 0x{} for {}", zero.toHexString(), command)
                    return
                }
                val firmware = buf.stringUntilNullTerminator()
                if (firmware == null) {
                    LOG.warn("Failed to get firmware from payload")
                    return
                }
                LOG.info("Got firmware: {}", firmware)

                val versionInfoEvent = GBDeviceEventVersionInfo()
                versionInfoEvent.fwVersion = firmware
                evaluateGBDeviceEvent(versionInfoEvent)

                gbDevice.setUpdateState(GBDevice.State.INITIALIZED, context)
            }

            ShokzCommand.MEDIA_SOURCE_RET -> {
                val zero = buf.get().toInt()
                if (zero != 0) {
                    LOG.warn("Unexpected non-zero byte 0x{} for {}", zero.toHexString(), command)
                    return
                }
                val code = buf.get().toInt() and 0xff
                val mode = ShokzMediaSource.fromCode(code)
                if (mode == null) {
                    LOG.warn("Unknown mode code 0x{}", code.toHexString())
                    return
                }

                LOG.info("Got mode: {}", mode)

                evaluateGBDeviceEvent(
                    GBDeviceEventUpdatePreferences(
                        DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE,
                        mode.name.lowercase()
                    )
                )
            }

            ShokzCommand.BATTERY_RET -> {
                val zero = buf.get().toInt()
                if (zero != 0) {
                    LOG.warn("Unexpected non-zero byte 0x{} for {}", zero.toHexString(), command)
                    return
                }
                val batteryPercentage = ((buf.get().toInt() and 0xff) + 1) * 10

                LOG.info("Got battery: {}%", batteryPercentage)

                val batteryInfoEvent = GBDeviceEventBatteryInfo()
                batteryInfoEvent.level = batteryPercentage
                evaluateGBDeviceEvent(batteryInfoEvent)
            }

            ShokzCommand.EQUALIZER_RET, ShokzCommand.EQUALIZER_ACK -> {
                if (command == ShokzCommand.EQUALIZER_RET) {
                    val zero = buf.get().toInt()
                    if (zero != 0) {
                        LOG.warn("Unexpected non-zero byte 0x{} for {}", zero.toHexString(), command)
                        return
                    }
                }

                val code = buf.get().toInt() and 0xff
                val equalizer = ShokzEqualizer.fromCode(code)
                if (equalizer == null) {
                    LOG.warn("Unknown equalizer code 0x{}", code.toHexString())
                    return
                }

                LOG.info("Got equalizer: {}", equalizer)

                val mediaSourceValue: String =
                    devicePrefs.getString(DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE, "bluetooth")
                val mediaSource = ShokzMediaSource.fromPreference(mediaSourceValue) ?: run {
                    LOG.warn("Unknown media source {}, falling back to bluetooth", mediaSourceValue)
                    ShokzMediaSource.BLUETOOTH
                }

                devicePrefs.getString(DeviceSettingsPreferenceConst.PREF_LANGUAGE, "en")
                evaluateGBDeviceEvent(
                    GBDeviceEventUpdatePreferences(
                        when (mediaSource) {
                            ShokzMediaSource.BLUETOOTH -> DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_BLUETOOTH
                            ShokzMediaSource.MP3 -> DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_MP3
                        },
                        equalizer.name.lowercase()
                    )
                )
            }

            ShokzCommand.PLAYBACK_STATUS_RET -> {
                val zero = buf.get().toInt()
                if (zero != 0) {
                    LOG.warn("Unexpected non-zero byte 0x{} for {}", zero.toHexString(), command)
                    return
                }
                val code = buf.get().toInt() and 0xff
                val playbackStatus = ShokzPlaybackStatus.fromCode(code)
                if (playbackStatus == null) {
                    LOG.warn("Unknown playback status code 0x{}", code.toHexString())
                    return
                }

                LOG.info("Got playback status: {}", playbackStatus)

                // TODO handle
            }

            ShokzCommand.VOLUME_RET -> {
                val zero = buf.get().toInt()
                if (zero != 0) {
                    LOG.warn("Unexpected non-zero byte 0x{} for {}", zero.toHexString(), command)
                    return
                }
                val percentage = floor(((buf.get().toInt() and 0xff) / 16) * 100 + 0.5)

                LOG.info("Got volume: {}", percentage)

                // TODO handle
            }

            ShokzCommand.MP3_PLAYBACK_MODE_RET -> {
                val zero = buf.get().toInt()
                if (zero != 0) {
                    LOG.warn("Unexpected non-zero byte 0x{} for {}", zero.toHexString(), command)
                    return
                }
                val code = buf.get().toInt() and 0xff
                val playbackMode = ShokzMp3PlaybackMode.fromCode(code)
                if (playbackMode == null) {
                    LOG.warn("Unknown mp3 playback mode code 0x{}", code.toHexString())
                    return
                }

                LOG.info("Got mp3 playback mode: {}", playbackMode)

                evaluateGBDeviceEvent(
                    GBDeviceEventUpdatePreferences(
                        DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE,
                        playbackMode.name.lowercase()
                    )
                )
            }

            ShokzCommand.CONTROLS_RET -> {
                val zero = buf.get().toInt()
                if (zero != 0) {
                    LOG.warn("Unexpected non-zero byte 0x{} for {}", zero.toHexString(), command)
                    return
                }
                val code = buf.get().toInt() and 0xff
                val controls = ShokzControls.fromCode(code)
                if (controls == null) {
                    LOG.warn("Unknown controls code 0x{}", code.toHexString())
                    return
                }

                LOG.info("Got controls: {}", controls)

                val eventUpdatePreferences = GBDeviceEventUpdatePreferences()

                eventUpdatePreferences.withPreference(
                    DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_LONG_PRESS_MULTI_FUNCTION,
                    when (controls) {
                        ShokzControls.ASSISTANT__MEDIA_SOURCE, ShokzControls.ASSISTANT__ASSISTANT -> "assistant"
                        ShokzControls.MEDIA_SOURCE__ASSISTANT, ShokzControls.MEDIA_SOURCE__MEDIA_SOURCE -> "media_source"
                    }
                ).withPreference(
                    DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_SIMULTANEOUS_VOLUME_UP_DOWN,
                    when (controls) {
                        ShokzControls.ASSISTANT__MEDIA_SOURCE, ShokzControls.MEDIA_SOURCE__MEDIA_SOURCE -> "media_source"
                        ShokzControls.MEDIA_SOURCE__ASSISTANT, ShokzControls.ASSISTANT__ASSISTANT -> "assistant"
                    }
                )

                evaluateGBDeviceEvent(eventUpdatePreferences)
            }

            ShokzCommand.LANGUAGE_RET -> {
                buf.getShort() // 0
                val code = buf.get().toInt() and 0xff
                val language = ShokzLanguage.fromCode(code)
                if (language == null) {
                    LOG.warn("Unknown language code 0x{}", code.toHexString())
                    return
                }

                LOG.info("Got language: {}", language)

                evaluateGBDeviceEvent(
                    GBDeviceEventUpdatePreferences(
                        DeviceSettingsPreferenceConst.PREF_LANGUAGE,
                        language.locale
                    )
                )
            }

            ShokzCommand.MEDIA_SOURCE_NOTIFY -> {
                val code = buf.get().toInt() and 0xff
                val mediaSource = ShokzMediaSource.fromCode(code)
                if (mediaSource == null) {
                    LOG.warn("Unknown media source 0x{}", code.toHexString())
                    return
                }

                LOG.info("Got media source change: {}", mediaSource)

                evaluateGBDeviceEvent(
                    GBDeviceEventUpdatePreferences(
                        DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE,
                        mediaSource.name.lowercase()
                    )
                )
            }

            ShokzCommand.MULTIPOINT_RET -> {
                val status = buf.getInt()
                val statusBool = status == 0x00010100
                LOG.info("Multipoint status={} (0x{})", statusBool, status.toHexString())
                broadcastMultipointStatus(statusBool)
            }

            ShokzCommand.MULTIPOINT_ON_ACK -> {
                val enabled: Boolean
                if (buf.remaining() >= 4) {
                    val status = buf.getInt()
                    LOG.info("Multipoint on ACK status={}", status)
                    enabled = status == 0
                } else {
                    LOG.warn("Multipoint on ACK payload too short")
                    enabled = false
                }
                broadcastMultipointStatus(enabled)
            }

            ShokzCommand.MULTIPOINT_OFF_ACK -> {
                val disabled: Boolean
                if (buf.remaining() >= 4) {
                    val status = buf.getInt()
                    LOG.info("Multipoint off ACK status={}", status)
                    disabled = status == 0
                } else {
                    LOG.warn("Multipoint off ACK payload too short")
                    disabled = false
                }
                broadcastMultipointStatus(!disabled)
            }

            ShokzCommand.MULTIPOINT_DEVICES_RET -> {
                buf.get() // 0
                val numDevices = buf.get().toInt() and 0xff
                LOG.debug("Got {} multipoint devices", numDevices);
                val devices = mutableListOf<MultipointDevice>()
                repeat(numDevices) {
                    val idx = buf.get().toInt() and 0xff
                    val macAddress = ByteArray(6)
                    buf.get(macAddress)
                    macAddress.reverse()
                    val connected = buf.get()
                    val nameLength = buf.get().toInt() and 0xff
                    val nameBytes = ByteArray(nameLength)
                    buf.get(nameBytes)
                    val name = String(nameBytes, Charsets.UTF_8)
                    LOG.debug("Device {}: {} - {} ({})", idx, macAddress.toHexString(), name, connected)
                    devices.add(
                        MultipointDevice(
                            macAddress.joinToString(separator = ":") {
                                String.format("%02X", it)
                            },
                            name,
                            connected.toInt() == 1
                        )
                    )
                }

                broadcastMultipointList(devices)
            }

            ShokzCommand.MULTIPOINT_CONNECT_ACK -> {
                val status = buf.getInt()
                LOG.info("Multipoint connect ack, status = 0x{}", status.toHexString())
                when (status) {
                    0x00000400 -> {
                        GB.toast("Too many connected devices", Toast.LENGTH_LONG, GB.WARN)
                    }
                }
            }

            ShokzCommand.MULTIPOINT_DEVICE_CONNECTION_NOTIFY -> {
                LOG.info("Got multipoint device connection, requesting list")
                requestPairedDevices()
            }

            ShokzCommand.MULTIPOINT_DISCONNECT_ACK -> {
                LOG.info("Multipoint disconnect ack, status = 0x{}", buf.getInt().toHexString())
            }

            ShokzCommand.MULTIPOINT_PAIR_SECOND_FINISH -> {
                LOG.info("Multipoint pair second finish")
                broadcastMultipointPairing(false)
            }

            ShokzCommand.MULTIPOINT_START_PAIR_ACK -> {
                LOG.info("Multipoint start pair ack, status = 0x{}", buf.getInt().toHexString())
                broadcastMultipointPairing(true)
            }

            else -> LOG.warn("Unhandled command {}, args={}", command, args.toHexString())
        }
    }

    private fun queueCommand(
        command: ShokzCommand,
        args: ByteArray = byteArrayOf()
    ) {
        messageQueue.add(ShokzMessage(command, args))

        if (pendingMessage == null) {
            sendNextCommand()
        }
    }

    private fun onCommandTimeout() {
        if (timeoutRetries++ < 3) {
            LOG.warn("Timed out waiting for response, retrying attempt {}", timeoutRetries)
            pendingMessage?.let { msg ->
                sendMessage(msg)
                return
            }
        }
        LOG.warn("Timed out waiting for response, giving up")
        sendNextCommand()
    }

    private fun sendNextCommand() {
        timeoutHandler.removeCallbacksAndMessages(null)
        timeoutRetries = 0

        pendingMessage = messageQueue.poll()
        pendingMessage?.let { msg ->
            LOG.debug("Sending next command in queue: {}", msg.command)
            sendMessage(msg)
            return
        }
        LOG.debug("No more commands in the queue")
    }

    private fun sendMessage(message: ShokzMessage) {
        val builder = createTransactionBuilder(message.command.name.lowercase())
        builder.write(*encodeCommand(message.command, message.args))
        builder.queue()

        when (message.command) {
            ShokzCommand.MULTIPOINT_PAIR_SECOND_FINISH,
            ShokzCommand.MULTIPOINT_START_PAIR_REQ -> {
                sendNextCommand()
            }

            else -> {
                timeoutHandler.postDelayed({ onCommandTimeout() }, 2000L)
            }
        }
    }

    private fun setupMultipointBroadcastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(MultipointPairingActivity.ACTION_MULTIPOINT_ENABLE)
            addAction(MultipointPairingActivity.ACTION_MULTIPOINT_DISABLE)
            addAction(MultipointPairingActivity.ACTION_MULTIPOINT_GET_DEVICES)
            addAction(MultipointPairingActivity.ACTION_MULTIPOINT_GET_STATUS)
            addAction(MultipointPairingActivity.ACTION_MULTIPOINT_CONNECT_DEVICE)
            addAction(MultipointPairingActivity.ACTION_MULTIPOINT_DISCONNECT_DEVICE)
            addAction(MultipointPairingActivity.ACTION_MULTIPOINT_START_PAIRING)
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(
            multipointBroadcastReceiver,
            intentFilter
        )
    }

    private val multipointBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.getParcelableExtra<GBDevice>(GBDevice.EXTRA_DEVICE)
            if (device?.address != gbDevice.address) {
                return // not for this device
            }

            when (intent.action) {
                MultipointPairingActivity.ACTION_MULTIPOINT_ENABLE -> {
                    LOG.info("Enabling multipoint")
                    queueCommand(ShokzCommand.MULTIPOINT_ON)
                }

                MultipointPairingActivity.ACTION_MULTIPOINT_DISABLE -> {
                    val macAddress = "02:00:00:00:00:00"  // TODO bluetoothAdapter.address
                    LOG.debug("Disabling multipoint from mac address {}", macAddress)
                    val macAddressBytes = macAddress.replace(":", "").hexToByteArray()
                    macAddressBytes.reverse()
                    queueCommand(ShokzCommand.MULTIPOINT_OFF, macAddressBytes)
                }

                MultipointPairingActivity.ACTION_MULTIPOINT_GET_DEVICES -> requestPairedDevices()
                MultipointPairingActivity.ACTION_MULTIPOINT_GET_STATUS -> requestMultipointStatus()
                MultipointPairingActivity.ACTION_MULTIPOINT_CONNECT_DEVICE -> {
                    val deviceAddress = intent.getStringExtra(MultipointPairingActivity.EXTRA_DEVICE_ADDRESS)
                    if (deviceAddress != null) {
                        connectToDevice(deviceAddress)
                    }
                }

                MultipointPairingActivity.ACTION_MULTIPOINT_DISCONNECT_DEVICE -> {
                    val deviceAddress = intent.getStringExtra(MultipointPairingActivity.EXTRA_DEVICE_ADDRESS)
                    if (deviceAddress != null) {
                        disconnectFromDevice(deviceAddress)
                    }
                }

                MultipointPairingActivity.ACTION_MULTIPOINT_START_PAIRING -> {
                    val enabled = intent.getBooleanExtra(MultipointPairingActivity.EXTRA_PAIRING_ENABLED, false)
                    startPairingMode(enabled)
                }

                else -> LOG.warn("Unknown action {}", intent.action)
            }
        }
    }

    private fun requestPairedDevices() {
        LOG.info("Requesting paired devices")
        queueCommand(ShokzCommand.MULTIPOINT_DEVICES_GET)
    }

    private fun requestMultipointStatus() {
        LOG.info("Requesting multipoint status")
        queueCommand(ShokzCommand.MULTIPOINT_GET)
    }

    private fun connectToDevice(deviceAddress: String) {
        LOG.info("Connecting to {}", deviceAddress)

        val macAddress = deviceAddress.replace(":", "").hexToByteArray()
        macAddress.reverse()
        queueCommand(ShokzCommand.MULTIPOINT_CONNECT_REQ, macAddress)
    }

    private fun disconnectFromDevice(deviceAddress: String) {
        LOG.info("Disconnecting from {}", deviceAddress)

        val macAddress = deviceAddress.replace(":", "").hexToByteArray()
        macAddress.reverse()
        queueCommand(ShokzCommand.MULTIPOINT_DISCONNECT_REQ, macAddress)
    }

    private fun startPairingMode(enabled: Boolean) {
        LOG.info("Starting pairing mode enabled={}", enabled)
        if (enabled) {
            queueCommand(ShokzCommand.MULTIPOINT_START_PAIR_REQ)
        } else {
            queueCommand(ShokzCommand.MULTIPOINT_PAIR_SECOND_FINISH)
        }
    }

    private fun broadcastMultipointStatus(enabled: Boolean) {
        val intent = Intent(MultipointPairingActivity.ACTION_MULTIPOINT_STATUS_UPDATE).apply {
            putExtra(GBDevice.EXTRA_DEVICE, device)
            putExtra(MultipointPairingActivity.EXTRA_MULTIPOINT_ENABLED, enabled)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun broadcastMultipointPairing(enabled: Boolean) {
        val intent = Intent(MultipointPairingActivity.ACTION_MULTIPOINT_PAIRING_UPDATE).apply {
            putExtra(GBDevice.EXTRA_DEVICE, device)
            putExtra(MultipointPairingActivity.EXTRA_PAIRING_ENABLED, enabled)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun broadcastMultipointList(devices: List<MultipointDevice>) {
        val intent = Intent(MultipointPairingActivity.ACTION_MULTIPOINT_DEVICE_LIST).apply {
            putExtra(GBDevice.EXTRA_DEVICE, device)
            putParcelableArrayListExtra(
                MultipointPairingActivity.EXTRA_DEVICE_LIST,
                ArrayList(devices)
            )
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ShokzSupport::class.java)
        private const val MAX_MTU: Int = 2048
        private const val PACKET_PREAMBLE: Short = 0x5aa5.toShort() // LE
        private const val MASK_RESPONSE: Int = 0x8000

        // This does not show up in the discovered services?
        private val UUID_SERVICE_SHOKZ: UUID = UUID.fromString("0000fef0-0000-1000-8000-00805f9b34fb")

        /// Each command seems to have the following format (all u32 LE)
        /// 0x01 length 0x02 0x04 group 0x03 0x04 command 0x04 args_length args
        fun encodeCommand(
            command: ShokzCommand,
            args: ByteArray = byteArrayOf()
        ): ByteArray {
            val buf = ByteBuffer.allocate(52 + args.size).order(ByteOrder.LITTLE_ENDIAN)
            buf.putShort(PACKET_PREAMBLE)
            buf.putShort((buf.limit() - 8).toShort())
            buf.putInt(0x01)
            buf.putShort((buf.limit() - 12).toShort())
            // CRC will be added later
            buf.position(buf.position() + 2)
            buf.putInt(0x01)
            buf.putInt(buf.limit() - 20)
            buf.putInt(0x02)
            buf.putInt(0x04)
            buf.putInt(command.group)
            buf.putInt(0x03)
            buf.putInt(0x04)
            buf.putInt(command.code)
            buf.putInt(0x04)
            buf.putInt(args.size)
            buf.put(args)

            buf.putShort(10, CheckSums.crc16_maxim(buf.array(), 12, buf.limit() - 12).toShort())

            return buf.array()
        }
    }
}
