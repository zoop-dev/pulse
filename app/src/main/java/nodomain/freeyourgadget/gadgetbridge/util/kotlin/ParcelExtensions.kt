package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import android.os.Build
import android.os.Parcel
import android.os.Parcelable

inline fun <reified T : Parcelable> Parcel.readParcelableCompat(classLoader: ClassLoader?): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readParcelable(classLoader, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        readParcelable(classLoader)
    }
}

inline fun <reified T> Parcel.readListCompat(outVal: MutableList<in T>, classLoader: ClassLoader?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readList(outVal, classLoader, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        readList(outVal, classLoader)
    }
}
