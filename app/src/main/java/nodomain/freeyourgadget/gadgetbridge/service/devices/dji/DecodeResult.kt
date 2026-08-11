package nodomain.freeyourgadget.gadgetbridge.service.devices.dji

sealed class DecodeResult<out T> {
    data class Success<out T>(val content: T, val bytesConsumed: Int) : DecodeResult<T>()
    data object NeedMoreData : DecodeResult<Nothing>()
    data class Invalid(val reason: String) : DecodeResult<Nothing>()
}
