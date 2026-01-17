package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.bloodpressure

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BloodPressureMeasurement(
    val timestamp: Long,
    val systolicMmHg: Int,
    val diastolicMmHg: Int,
    val meanArterialPressure: Int,
    val pulseRate: Int?,
    val userId: Int?,
    val measurementStatus: Int?,
): Parcelable
