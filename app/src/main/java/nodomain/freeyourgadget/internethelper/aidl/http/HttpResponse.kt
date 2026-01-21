package nodomain.freeyourgadget.internethelper.aidl.http

import android.os.ParcelFileDescriptor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HttpResponse(
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: ParcelFileDescriptor? = null,
) : Parcelable
