package nodomain.freeyourgadget.gadgetbridge.service.devices.bm6

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.widget.Toast
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.devices.GenericTemperatureSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTemperatureSample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.LoggerFactory
import java.util.UUID

class Bm6Support : AbstractBTLESingleDeviceSupport(LOG) {
    private val decryptCipher: Cipher = Cipher.getInstance("AES/CBC/NoPadding").apply {
        init(Cipher.DECRYPT_MODE, SecretKeySpec(KEY, "AES"), IvParameterSpec(ByteArray(16)))
    }
    private val encryptCipher: Cipher = Cipher.getInstance("AES/CBC/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, SecretKeySpec(KEY, "AES"), IvParameterSpec(ByteArray(16)))
    }

    private val deviceInfoProfile: DeviceInfoProfile<Bm6Support>

    init {
        val mListener = IntentListener { intent: Intent? ->
            intent?.action?.let { action ->
                when (action) {
                    DeviceInfoProfile.ACTION_DEVICE_INFO -> {
                        @Suppress("DEPRECATION")
                        val deviceInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO, DeviceInfo::class.java)
                        } else {
                            intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO)
                        }
                        LOG.debug("Device info: {}", deviceInfo)

                        val events = DeviceInfoProfile.toDeviceEvents(deviceInfo)
                        for (event in events) {
                            handleGBDeviceEvent(event)
                        }
                    }
                }
            }
        }

        addSupportedService(UUID_SERVICE_BM6)

        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION)
        deviceInfoProfile = DeviceInfoProfile<Bm6Support>(this)
        deviceInfoProfile.addListener(mListener)
        addSupportedProfile(deviceInfoProfile)
    }

    override fun useAutoConnect(): Boolean {
        return true
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        builder.setDeviceState(GBDevice.State.INITIALIZING)
        deviceInfoProfile.requestDeviceInfo(builder)
        builder.write(UUID_CHARACTERISTIC_BM6_WRITE, *encryptCipher.doFinal(COMMAND_REQUEST))
        builder.notify(UUID_CHARACTERISTIC_BM6_NOTIFY, true)
        builder.setDeviceState(GBDevice.State.INITIALIZED)
        return builder
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        if (UUID_CHARACTERISTIC_BM6_NOTIFY == characteristic.uuid) {
            val decrypted = decryptCipher.doFinal(value)
            val (voltage, level, temperature) = parse(decrypted) ?: run {
                return true
            }
            LOG.debug("Voltage: {}V, Level: {}%, Temperature: {}°C", voltage, level, temperature)
            val batteryEvent = GBDeviceEventBatteryInfo()
            batteryEvent.voltage = voltage
            batteryEvent.level = level
            evaluateGBDeviceEvent(batteryEvent)

            if (temperature > 0) {
                val sample = GenericTemperatureSample()
                sample.timestamp = System.currentTimeMillis()
                sample.temperature = temperature.toFloat()
                sample.temperatureLocation = GenericTemperatureSample.LOCATION_UNKNOWN
                sample.temperatureType = GenericTemperatureSample.TYPE_UNKNOWN

                try {
                    GBApplication.acquireDB().use { handler ->
                        val session = handler.getDaoSession()
                        val sampleProvider = GenericTemperatureSampleProvider(device, session)

                        sampleProvider.persistSamples(sample, context)
                    }
                } catch (e: Exception) {
                    GB.toast(context, "Error saving temperature samples", Toast.LENGTH_SHORT, GB.ERROR, e)
                }
            }

            return true
        }

        return super.onCharacteristicChanged(gatt, characteristic, value)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Bm6Support::class.java)

        private val UUID_SERVICE_BM6: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_BM6_WRITE: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_BM6_NOTIFY: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")

        private val KEY = byteArrayOf(108, 101, 97, 103, 101, 110, 100, 255.toByte(), 254.toByte(), 48, 49, 48, 48, 48, 48, 57)

        // d1550700000000000000000000000000
        private val COMMAND_REQUEST = byteArrayOf(
            0xd1.toByte(), 0x55, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )

        internal fun parse(decrypted: ByteArray): Triple<Float, Int, Int>? {
            if (decrypted.size < 9) {
                LOG.warn("Decrypted data too short: {} bytes", decrypted.size)
                return null
            }
            if (decrypted[0] != 0xd1.toByte() || decrypted[1] != 0x55.toByte() || decrypted[2] != 0x07.toByte()) {
                LOG.warn("Unknown header: 0x{} 0x{} 0x{}", decrypted[0].toHexString(), decrypted[1].toHexString(), decrypted[2].toHexString())
                return null
            }
            val tempNegative = decrypted[3].toInt() and 0xFF == 0x01
            val tempValue = decrypted[4].toInt() and 0xFF
            val temperature = if (tempNegative) -tempValue else tempValue
            val level = decrypted[6].toInt() and 0xFF
            val voltageRaw = ((decrypted[7].toInt() and 0x0F) shl 8) or (decrypted[8].toInt() and 0xFF)
            val voltage = voltageRaw / 100.0f
            return Triple(voltage, level, temperature)
        }
    }
}
