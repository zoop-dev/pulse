package nodomain.freeyourgadget.gadgetbridge.service.devices.bm2

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
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
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.UUID

class Bm2Support : AbstractBTLESingleDeviceSupport(LOG) {
    private val cipher: Cipher = Cipher.getInstance("AES/CBC/NoPadding").apply {
        init(Cipher.DECRYPT_MODE, SecretKeySpec(KEY, "AES"), IvParameterSpec(ByteArray(16)))
    }

    private val deviceInfoProfile: DeviceInfoProfile<Bm2Support>

    init {
        val mListener = IntentListener { intent: Intent? ->
            intent?.action?.let { action ->
                when (action) {
                    DeviceInfoProfile.ACTION_DEVICE_INFO -> {
                        @Suppress("DEPRECATION")
                        var deviceInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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

        addSupportedService(UUID_SERVICE_BM2)

        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION)
        deviceInfoProfile = DeviceInfoProfile<Bm2Support>(this)
        deviceInfoProfile.addListener(mListener)
        addSupportedProfile(deviceInfoProfile)
    }

    override fun useAutoConnect(): Boolean {
        return true
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        builder.setDeviceState(GBDevice.State.INITIALIZING)
        builder.notify(UUID_CHARACTERISTIC_BM2, true)
        builder.setDeviceState(GBDevice.State.INITIALIZED)
        return builder
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        if (UUID_CHARACTERISTIC_BM2 == characteristic.uuid) {
            val (voltage, level) = parse(cipher.doFinal(value)) ?: run {
                return true // Logged upstream
            }
            LOG.debug("Voltage: {}V, Level: {}%", voltage, level)
            val batteryEvent = GBDeviceEventBatteryInfo()
            batteryEvent.voltage = voltage
            batteryEvent.level = level
            evaluateGBDeviceEvent(batteryEvent)
            return true
        }

        return super.onCharacteristicChanged(gatt, characteristic, value)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Bm2Support::class.java)

        private val UUID_SERVICE_BM2: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_BM2: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")

        private val KEY = byteArrayOf(108, 101, 97, 103, 101, 110, 100, -1, -2, 49, 56, 56, 50, 52, 54, 54)

        internal fun parse(decrypted: ByteArray): Pair<Float, Int>? {
            val buf = ByteBuffer.wrap(decrypted)
            val preamble = buf.get()
            if (preamble != 0xf5.toByte()) {
                LOG.warn("Unknown preamble 0x{}", preamble.toHexString())
                return null
            }
            val voltageRaw = (buf.getShort().toInt() and 0xFFFF) ushr 4
            val level = buf.get().toInt() and 0xFF
            val voltage = voltageRaw / 100.0f
            return Pair(voltage, level)
        }
    }
}
