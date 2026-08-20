package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

fun Intent.getDevice(): GBDevice? {
    return getParcelableCompat(GBDevice.EXTRA_DEVICE)
}

inline fun <reified T : Parcelable> Intent.getParcelableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}
