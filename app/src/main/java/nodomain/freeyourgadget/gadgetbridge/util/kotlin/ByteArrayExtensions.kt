package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions

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

fun ByteArray.readUint16LE(offset: Int): Int {
    return BLETypeConversions.toUint16(this, offset)
}
