package nodomain.freeyourgadget.internethelper.aidl.http

import android.os.ParcelFileDescriptor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class HttpRequest(
    val url: String,
    val method: Method,
    val headers: Map<String, String> = emptyMap(),
    val body: ParcelFileDescriptor? = null,
    val allowInsecure: Boolean = false,
) : Parcelable {
    @Suppress("unused")  // they must be kept so they can be sent
    enum class Method {
        GET,
        POST,
        HEAD,
        PUT,
        PATCH,
        DELETE,
        OPTIONS,
    }
}
