package nodomain.freeyourgadget.gadgetbridge.util

open class LiveDataEvent<out T>(private val content: T) {
    private var hasBeenHandled = false

    /**
     * Returns the content if it hasn't been handled already.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}
