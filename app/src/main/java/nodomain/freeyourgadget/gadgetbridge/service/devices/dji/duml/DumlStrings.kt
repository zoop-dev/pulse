package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml

/**
 * DJI's packed-string convention seen inside DUML payloads: a 1-byte
 * length followed by that many UTF-8 bytes.
 */
object DumlStrings {
    /** Returns the decoded string and the offset just past it. */
    fun decodePackedString(data: ByteArray, offset: Int): Pair<String, Int> {
        require(offset < data.size) { "PackedString offset $offset out of range (${data.size} bytes)" }
        val len = data[offset].toInt() and 0xFF
        val end = offset + 1 + len
        require(end <= data.size) { "PackedString at offset $offset: length $len exceeds buffer" }
        val value = String(data, offset + 1, len, Charsets.UTF_8)
        return value to end
    }

    fun encodePackedString(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 0xFF) { "String too long for packed-string encoding: ${bytes.size} bytes" }
        return byteArrayOf(bytes.size.toByte()) + bytes
    }
}
