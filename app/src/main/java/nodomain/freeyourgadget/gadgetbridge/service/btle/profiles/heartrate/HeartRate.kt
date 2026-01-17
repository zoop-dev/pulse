package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HeartRate(
    val timestamp: Long,
    val heartRate: Int,
    val sensorContact: SensorContact,
    val energyExpended: Int,
    val rrIntervals: ArrayList<Int>,
) : Parcelable {
    fun isValid(): Boolean {
        return sensorContact != SensorContact.CONTACT_NOT_DETECTED && heartRate > 0
    }
}

enum class SensorContact {
    NOT_SUPPORTED,
    CONTACT_DETECTED,
    CONTACT_NOT_DETECTED,
}
