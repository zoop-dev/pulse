package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.bloodpressure

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getSFloat
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.GregorianCalendar
import kotlin.math.roundToInt

class BloodPressureProfile<T : AbstractBTLESingleDeviceSupport>(val support: T) : AbstractBleProfile<T>(support) {
    override fun enableNotify(builder: TransactionBuilder, enable: Boolean) {
        builder.notify(GattCharacteristic.UUID_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT, enable)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        if (GattCharacteristic.UUID_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT != characteristic.uuid) {
            return false
        }

        val buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)

        val flags = buf.get().toInt()
        val systolicRaw: Float = buf.getSFloat()
        val diastolicRaw: Float = buf.getSFloat()
        val meanArterialPressureRaw: Float = buf.getSFloat()

        val systolic: Int
        val diastolic: Int
        val meanArterialPressure: Int

        if ((flags and FLAG_UNIT) != 0) {
            systolic = (systolicRaw * 7.50061683).roundToInt()
            diastolic = (diastolicRaw * 7.50061683).roundToInt()
            meanArterialPressure = (meanArterialPressureRaw * 7.50061683).roundToInt()
        } else {
            systolic = systolicRaw.roundToInt()
            diastolic = diastolicRaw.roundToInt()
            meanArterialPressure = meanArterialPressureRaw.roundToInt()
        }

        val timestamp: Long
        if ((flags and FLAG_TIMESTAMP) != 0) {
            val year = buf.getShort()
            val month = buf.get()
            val day = buf.get()

            val hour = buf.get()
            val minute = buf.get()
            val second = buf.get()

            val c = GregorianCalendar.getInstance()
            c.set(year.toInt(), month - 1, day.toInt(), hour.toInt(), minute.toInt(), second.toInt())

            timestamp = c.getTime().time
        } else {
            timestamp = System.currentTimeMillis()
        }

        val pulseRate = if ((flags and FLAG_PULSE_RATE) != 0) {
            buf.getSFloat().roundToInt()
        } else {
            null
        }

        val userId = if ((flags and FLAG_USER_ID) != 0) {
            buf.get().toInt() and 0xff
        } else {
            null
        }

        val measurementStatus = if ((flags and FLAG_MEASUREMENT_STATUS) != 0) {
            buf.get().toInt() and 0xff
        } else {
            null
        }

        val measurement = BloodPressureMeasurement(
            timestamp,
            systolic,
            diastolic,
            meanArterialPressure,
            pulseRate,
            userId,
            measurementStatus,
        )

        LOG.debug("Got blood pressure measurement: {}", measurement)

        notify(createIntent(measurement))

        return true
    }

    private fun createIntent(measurement: BloodPressureMeasurement): Intent {
        val intent = Intent(ACTION_BLOOD_PRESSURE)
        intent.putExtra(EXTRA_BLOOD_PRESSURE, measurement)
        return intent
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BloodPressureProfile::class.java)

        const val ACTION_BLOOD_PRESSURE = "nodomain.freeyourgadget.gadgetbridge.ble.profile.ACTION_BLOOD_PRESSURE"
        const val EXTRA_BLOOD_PRESSURE = "blood_pressure_measurement"

        const val FLAG_UNIT = 0x01
        const val FLAG_TIMESTAMP = 0x02
        const val FLAG_PULSE_RATE = 0x04
        const val FLAG_USER_ID = 0x08
        const val FLAG_MEASUREMENT_STATUS = 0x10
    }
}
