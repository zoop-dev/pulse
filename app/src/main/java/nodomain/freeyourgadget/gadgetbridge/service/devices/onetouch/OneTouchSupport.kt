package nodomain.freeyourgadget.gadgetbridge.service.devices.onetouch

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.widget.Toast
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceState
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.devices.GlucoseSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.GlucoseSample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.withTransaction
import nodomain.freeyourgadget.gadgetbridge.util.notifications.GBProgressNotification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

class OneTouchSupport : AbstractBTLESingleDeviceSupport(LOG) {
    private val deviceInfoProfile: DeviceInfoProfile<OneTouchSupport>

    private val reassemblyBuffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)

    private var lastSentMessage: OneTouchMessage? = null
    private val messageQueue: Queue<OneTouchMessage> = LinkedList()
    private var expectedChunkCount: Int = 0

    private var readings: MutableList<OneTouchMessage.ReadingRet> = ArrayList()
    private var latestSampleTimestamp: Long = 0L
    private var deviceTimeOffset: Long? = null
    private var fetchOffset: Short = 0
    private var fetchTotal: Short = 0
    private lateinit var transferNotification: GBProgressNotification

    init {
        addSupportedService(UUID_SERVICE_ONETOUCH)

        val mListener = IntentListener { intent: Intent? ->
            intent?.action?.let { action ->
                when (action) {
                    DeviceInfoProfile.ACTION_DEVICE_INFO -> {
                        handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO)!!)
                    }
                }
            }
        }

        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION)
        deviceInfoProfile = DeviceInfoProfile<OneTouchSupport>(this)
        deviceInfoProfile.addListener(mListener)
        addSupportedProfile(deviceInfoProfile)
    }

    override fun setContext(
        gbDevice: GBDevice,
        btAdapter: BluetoothAdapter,
        context: Context
    ) {
        super.setContext(gbDevice, btAdapter, context)
        transferNotification = GBProgressNotification(context, GB.NOTIFICATION_CHANNEL_ID_TRANSFER)
    }

    override fun useAutoConnect(): Boolean {
        return false
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        reassemblyBuffer.clear()
        readings.clear()
        latestSampleTimestamp = 0L
        fetchOffset = 0
        fetchTotal = 0
        lastSentMessage = null

        builder.setDeviceState(GBDevice.State.INITIALIZING)

        deviceInfoProfile.requestDeviceInfo(builder)

        builder.notify(UUID_CHARACTERISTIC_ONETOUCH_READ, true)

        // Send the first message directly, queue the rest
        val timeGet = OneTouchMessage.TimeGet
        lastSentMessage = timeGet
        builder.write(
            UUID_CHARACTERISTIC_ONETOUCH_WRITE,
            *timeGet.encode()
        )
        messageQueue.add(OneTouchMessage.ThresholdHighGet)
        messageQueue.add(OneTouchMessage.ThresholdLowGet)

        return builder
    }

    override fun onSetTime() {
        // This should be deliberate by the user, and after we sync existing samples, to avoid
        // messing up the device time
        LOG.warn("Ignoring set time request")
        //messageQueue.add(OneTouchMessage.TimeSet(Calendar.getInstance().timeInMillis))
    }

    override fun onSendConfiguration(config: String) {
        // FIXME: Device currently ignores configuration changes
        if (!BuildConfig.DEBUG) {
            LOG.warn("Ignoring configuration change in non-debug build")
            return
        }

        when (config) {
            DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_HIGH -> {
                queueMessage(OneTouchMessage.ThresholdHighSet(
                    devicePrefs.getInt(DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_HIGH, 180)
                ))
            }

            DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_LOW -> {
                queueMessage(OneTouchMessage.ThresholdLowSet(
                    devicePrefs.getInt(DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_LOW, 70)
                ))
            }

            else -> super.onSendConfiguration(config)
        }
    }

    override fun onFetchRecordedData(dataTypes: Int) {
        if (deviceTimeOffset == null) {
            // Should never happen at this point
            LOG.warn("Time offset is null, requesting it first")
            queueMessage(OneTouchMessage.TimeGet)
        } else {
            queueMessage(OneTouchMessage.ReadingCountGet)
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        if (characteristic.uuid == UUID_CHARACTERISTIC_ONETOUCH_READ) {
            handleChunk(value)
            return true
        }

        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true
        }

        LOG.warn("Unhandled characteristic changed: {} {}", characteristic.uuid, GB.hexdump(value))
        return false
    }

    private fun handleChunk(data: ByteArray) {
        if (data.isEmpty()) return

        val firstByte = data[0].toInt() and 0xFF
        val idx = firstByte and 0x3F

        // ACK
        if ((firstByte and BITMASK_ACK) != 0) {
            LOG.debug("Received ACK for chunk {}", idx)
            sendNextMessage()
            return
        }

        // Send ACK for this chunk. This might be too early, but we don't know how to NACK
        sendAck(idx)

        // Chunk (but not the first one)
        if ((firstByte and BITMASK_CHUNK) != 0) {
            LOG.debug("Received chunk {} of {}", idx + 1, expectedChunkCount)

            // Append the chunk (without the first byte)
            if (data.size > 1) {
                reassemblyBuffer.put(data.copyOfRange(1, data.size))
            }

            // Check if we have all chunks
            if (idx == expectedChunkCount - 1) {
                // Reassemble all chunks
                handlePacket(reassemblyBuffer.array().copyOfRange(0, reassemblyBuffer.position()))

                // Reset reassembly state
                reassemblyBuffer.clear()
                expectedChunkCount = 0
            }

            return
        }

        // First of multiple chunks
        if (firstByte > 1) {
            LOG.debug("Starting chunked transfer: {} chunks", firstByte)
            expectedChunkCount = firstByte
            reassemblyBuffer.clear()
            reassemblyBuffer.put(data)
            return
        }

        // Single-chunk packet
        if (firstByte == 1) {
            reassemblyBuffer.clear()
            handlePacket(data)
            return
        }
    }

    private fun handlePacket(packet: ByteArray) {
        LOG.debug("Parsing full packet: {}", GB.hexdump(packet))

        val lastCommand = lastSentMessage
        if (lastCommand == null) {
            LOG.error("Got return value, but no last message")
            sendNextMessage()
            return
        }

        when (val message = OneTouchMessage.decode(packet, lastCommand)) {
            is OneTouchMessage.TimeRet -> {
                deviceTimeOffset = System.currentTimeMillis() - message.timestampMillis
                LOG.info("Device time: {}, offset={}", message.timestampMillis, deviceTimeOffset)
                queueMessage(OneTouchMessage.ReadingCountGet)
            }

            is OneTouchMessage.ReadingRet -> {
                val sampleOffset = deviceTimeOffset
                if (sampleOffset == null) {
                    LOG.error("Unable to process sample - device time offset is not known")
                    return
                } else {
                    val actualTimestamp = message.timestampMillis + sampleOffset
                    LOG.debug(
                        "Glucose reading offset={}, index={}: {} mg/dL at {} ({} adjusted)",
                        message.offset,
                        message.index,
                        message.value,
                        message.timestampMillis,
                        actualTimestamp
                    )

                    if (actualTimestamp > latestSampleTimestamp + 2000L) {
                        // 2s offset to avoid duplicates since device time offset can vary by a few milliseconds
                        // New sample
                        readings.add(message)
                    }
                    fetchOffset++

                    if (fetchOffset < fetchTotal && actualTimestamp > latestSampleTimestamp) {
                        fetchReading(fetchOffset)
                    } else {
                        LOG.debug("Fetch finished at {}/{}, {}, {}", fetchOffset, fetchTotal, actualTimestamp, latestSampleTimestamp)
                        transferNotification.finish()
                        device.unsetBusyTask()
                        device.sendDeviceUpdateIntent(context)
                        storeReadings()
                    }
                }
            }

            is OneTouchMessage.ReadingCountRet -> {
                LOG.info("Reading count: {}", message.count)

                latestSampleTimestamp = getLatestSample()
                fetchOffset = 0
                fetchTotal = message.count

                LOG.debug("Starting fetch up to {}", latestSampleTimestamp)

                device.setBusyTask(
                    R.string.busy_task_fetch_glucose_data,
                    context
                )
                device.sendDeviceUpdateIntent(context)

                transferNotification.start(R.string.busy_task_fetch_glucose_data, 0)

                fetchReading(0)
            }

            is OneTouchMessage.ThresholdLowRet -> {
                LOG.info("Glucose low threshold: {}", message.threshold)
                evaluateGBDeviceEvent(GBDeviceEventUpdatePreferences(
                    DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_LOW,
                    message.threshold.toString()
                ))
                evaluateGBDeviceEvent(GBDeviceEventUpdateDeviceState(GBDevice.State.INITIALIZED))
            }

            is OneTouchMessage.ThresholdHighRet -> {
                LOG.info("Glucose high limit: {}", message.threshold)
                evaluateGBDeviceEvent(GBDeviceEventUpdatePreferences(
                    DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_HIGH,
                    message.threshold.toString()
                ))
            }

            else -> {
                LOG.warn("Unhandled message {}", message)
            }
        }

        lastSentMessage = null
        sendNextMessage()
    }

    private fun getLatestSample(): Long {
        try {
            GBApplication.acquireDB().use { handler ->
                val session = handler.getDaoSession()
                val sampleProvider = GlucoseSampleProvider(device, session)
                return sampleProvider.latestSample?.timestamp ?: 0L
            }
        } catch (e: Exception) {
            GB.toast(context, "Error getting latest timestamp", Toast.LENGTH_LONG, GB.ERROR, e)
        }

        return 0L
    }

    private fun sendAck(chunkIndex: Int) {
        withTransaction("ack_$chunkIndex") { builder ->
            builder.write(
                UUID_CHARACTERISTIC_ONETOUCH_WRITE,
                (BITMASK_ACK or chunkIndex).toByte()
            )
        }
    }

    private fun queueMessage(message: OneTouchMessage) {
        messageQueue.add(message)
        sendNextMessage()
    }

    private fun fetchReading(offset: Short) {
        LOG.debug("Queuing reading fetch at offset {}", offset)
        queueMessage(OneTouchMessage.ReadingGet(offset))
    }

    private fun storeReadings() {
        if (readings.isEmpty()) {
            LOG.debug("No new readings to store")
            return
        }

        val sampleOffset = deviceTimeOffset
        if (sampleOffset == null) {
            LOG.error("Unable to store samples - device time offset is not known")
            return
        }

        LOG.debug("Storing {} readings, using device time offset {}ms", readings.size, sampleOffset)

        val samples = readings.map {
            val sample = GlucoseSample()
            sample.timestamp = it.timestampMillis + sampleOffset
            sample.valueMgDl = it.value.toDouble()
            return@map sample
        }.toList()

        try {
            GBApplication.acquireDB().use { handler ->
                val session = handler.getDaoSession()
                val sampleProvider = GlucoseSampleProvider(device, session)
                sampleProvider.persistForDevice(context, device, samples)
            }
        } catch (e: Exception) {
            GB.toast(context, "Error storing glucose readings", Toast.LENGTH_LONG, GB.ERROR, e)
        }

        GB.signalActivityDataFinish(device)

        readings.clear()
    }

    private fun sendNextMessage() {
        lastSentMessage?.let {
            LOG.debug("Not sending next message - already waiting for {}", it)
            return
        }

        val message = messageQueue.poll()
        if (message == null) {
            LOG.debug("No messages found in queue")
            return
        }

        withTransaction("send ${message.javaClass.simpleName}") { builder ->
            builder.write(UUID_CHARACTERISTIC_ONETOUCH_WRITE, *message.encode())
        }

        lastSentMessage = message
    }

    private fun handleDeviceInfo(deviceInfo: DeviceInfo) {
        LOG.debug("Device info: {}", deviceInfo)

        val versionCmd = GBDeviceEventVersionInfo()

        if (deviceInfo.hardwareRevision != null) {
            versionCmd.hwVersion = deviceInfo.hardwareRevision
        }

        if (deviceInfo.firmwareRevision != null) {
            versionCmd.fwVersion = deviceInfo.firmwareRevision
            versionCmd.fwVersion2 = deviceInfo.softwareRevision
        } else if (deviceInfo.softwareRevision != null) {
            versionCmd.fwVersion = deviceInfo.softwareRevision
        }

        handleGBDeviceEvent(versionCmd)

        if (deviceInfo.manufacturerName != null) {
            handleGBDeviceEvent(GBDeviceEventUpdateDeviceInfo("MANUFACTURER: ", deviceInfo.manufacturerName))
        }

        if (deviceInfo.modelNumber != null) {
            handleGBDeviceEvent(GBDeviceEventUpdateDeviceInfo("MODEL: ", deviceInfo.modelNumber))
        }

        if (deviceInfo.serialNumber != null) {
            handleGBDeviceEvent(GBDeviceEventUpdateDeviceInfo("SERIAL: ", deviceInfo.serialNumber))
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(OneTouchSupport::class.java)

        val UUID_SERVICE_ONETOUCH: UUID = UUID.fromString("af9df7a1-e595-11e3-96b4-0002a5d5c51b")
        val UUID_CHARACTERISTIC_ONETOUCH_WRITE: UUID = UUID.fromString("af9df7a2-e595-11e3-96b4-0002a5d5c51b")
        val UUID_CHARACTERISTIC_ONETOUCH_READ: UUID = UUID.fromString("af9df7a3-e595-11e3-96b4-0002a5d5c51b")

        private const val BITMASK_ACK = 0x80
        private const val BITMASK_CHUNK = 0x40
    }
}
