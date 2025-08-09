package nodomain.freeyourgadget.gadgetbridge.activities.multipoint

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MultipointDevice(
    val address: String,
    val name: String?,
    val isConnected: Boolean = false
) : Parcelable
