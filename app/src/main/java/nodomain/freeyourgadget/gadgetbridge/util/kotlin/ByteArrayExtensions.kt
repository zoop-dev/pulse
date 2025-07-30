package nodomain.freeyourgadget.gadgetbridge.util.kotlin

fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (prefix.size > this.size) {
        return false
    }
    for (i in prefix.indices) {
        if (this[i] != prefix[i]) {
            return false
        }
    }
    return true
}
