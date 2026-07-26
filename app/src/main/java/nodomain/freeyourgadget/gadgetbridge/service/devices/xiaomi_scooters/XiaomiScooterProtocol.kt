package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The TLV property protocol carried inside the AES-CCM encrypted CMD/RPT channel.
 * All commands, configuration and telemetry are one of these messages:
 *
 * ```
 * [len:1] [0x20] [txn:u16-LE] [opcode:1] [count:1] <entries...>
 * ```
 */
@Suppress("ArrayInDataClass")
object XiaomiScooterProtocol {
    private val LOG = LoggerFactory.getLogger(XiaomiScooterProtocol::class.java)

    const val OPCODE_SET = 0x00
    const val OPCODE_SET_ACK = 0x01
    const val OPCODE_GET = 0x02
    const val OPCODE_GET_RSP = 0x03
    const val OPCODE_NOTIFY = 0x04
    const val OPCODE_HELLO = 0xf0

    const val TYPE_BOOL = 0x00
    const val TYPE_U8 = 0x10
    const val TYPE_I8 = 0x20
    const val TYPE_F32 = 0x90
    const val TYPE_STR = 0xa0

    private const val MARKER = 0x20
    private const val HEADER_SIZE = 6

    /** A decoded property value, still tagged with its wire type. */
    data class RawValue(val type: Int, val data: ByteArray) {
        fun asU8(): Int? = if (type == TYPE_U8 || type == TYPE_BOOL) {
            data.getOrNull(0)?.toInt()?.and(0xff)
        } else {
            LOG.error("Attempted to get u8 from type {}", type)
            null
        }

        fun asI8(): Int? = if (type == TYPE_I8) {
            data.getOrNull(0)?.toInt()
        } else {
            LOG.error("Attempted to get i8 from type {}", type)
            null
        }

        fun asF32(): Float? = if (type == TYPE_F32) {
            if (data.size == 4) {
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).float
            } else {
                LOG.error("Unexpected size {} for f32", data.size)
                null
            }
        } else {
            LOG.error("Attempted to get f32 from type {}", type)
            null
        }

        fun asString(): String? = if (type == TYPE_STR) {
            String(data, Charsets.US_ASCII)
        } else {
            LOG.error("Attempted to get string from type {}", type)
            null
        }
    }

    /** A property to write in a SET message: 2-byte `code`, wire type, and data. */
    data class SetEntry(val code: Int, val type: Int, val data: ByteArray)

    data class DecodedEntry(val code: Int, val status: Int, val value: RawValue?)

    data class DecodedMessage(val opcode: Int, val txn: Int, val entries: List<DecodedEntry>)

    fun encodeGet(txn: Int, codes: List<Int>): ByteArray {
        val body = ByteBuffer.allocate(codes.size * 3)
        for (code in codes) {
            body.put(((code shr 8) and 0xff).toByte())
            body.put((code and 0xff).toByte())
            body.put(0x00)
        }
        return wrap(txn, OPCODE_GET, codes.size, body.array())
    }

    fun encodeSet(txn: Int, entries: List<SetEntry>): ByteArray {
        val body = ByteBuffer.allocate(entries.sumOf { 5 + it.data.size })
        for (entry in entries) {
            body.put(((entry.code shr 8) and 0xff).toByte())
            body.put((entry.code and 0xff).toByte())
            body.put(0x00) // flag
            body.put(entry.data.size.toByte())
            body.put(entry.type.toByte())
            body.put(entry.data)
        }
        return wrap(txn, OPCODE_SET, entries.size, body.array())
    }

    /** `[05 20 txn:u16-LE f0]` — the session hello, which carries no entries. */
    fun encodeHello(txn: Int): ByteArray = byteArrayOf(
        0x05, MARKER.toByte(), (txn and 0xff).toByte(), ((txn shr 8) and 0xff).toByte(), OPCODE_HELLO.toByte(),
    )

    fun decode(frame: ByteArray): DecodedMessage? {
        if (frame.size < 5) {
            LOG.error("Frame size {} is too short", frame.size)
            return null
        }
        val len = frame[0].toInt() and 0xff
        if (len != frame.size ) {
            LOG.error("Frame size {} mismatch with wire length {}", frame.size, len)
            return null
        }
        val marker = frame[1].toInt() and 0xff
        if (marker != MARKER) {
            LOG.error("Unexpected marker 0x{} at position 1", marker.toHexString())
            return null
        }
        val txn = ((frame[3].toInt() and 0xff) shl 8) or (frame[2].toInt() and 0xff)
        val opcode = frame[4].toInt() and 0xff
        if (opcode == OPCODE_HELLO) {
            return DecodedMessage(opcode, txn, emptyList())
        }
        if (frame.size < HEADER_SIZE) {
            LOG.warn("Got an empty frame")
            return null
        }

        val count = frame[5].toInt() and 0xff
        val entries = mutableListOf<DecodedEntry>()
        var offset = HEADER_SIZE
        var remaining = count
        while (remaining-- > 0) {
            if (offset + 2 > frame.size) break
            val code = ((frame[offset].toInt() and 0xff) shl 8) or (frame[offset + 1].toInt() and 0xff)
            offset += 2
            when (opcode) {
                OPCODE_GET -> {
                    offset += 1 // trailing 0x00
                }
                OPCODE_SET_ACK -> {
                    val status = readStatus(frame, offset) ?: break
                    offset += 3
                    entries.add(DecodedEntry(code, status, null))
                }
                OPCODE_SET, OPCODE_NOTIFY -> {
                    offset += 1 // flags
                    val (value, newOffset) = readValue(frame, offset) ?: break
                    offset = newOffset
                    entries.add(DecodedEntry(code, 0, value))
                }
                OPCODE_GET_RSP -> {
                    val status = readStatus(frame, offset) ?: break
                    offset += 3
                    val (value, newOffset) = readValue(frame, offset) ?: break
                    offset = newOffset
                    entries.add(DecodedEntry(code, status, value))
                }
                else -> return DecodedMessage(opcode, txn, entries)
            }
        }
        return DecodedMessage(opcode, txn, entries)
    }

    private fun wrap(txn: Int, opcode: Int, count: Int, body: ByteArray): ByteArray {
        val totalLen = HEADER_SIZE + body.size
        val buffer = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(totalLen.toByte())
        buffer.put(MARKER.toByte())
        buffer.putShort(txn.toShort())
        buffer.put(opcode.toByte())
        buffer.put(count.toByte())
        buffer.put(body)
        return buffer.array()
    }

    private fun readStatus(frame: ByteArray, offset: Int): Int? {
        if (offset + 3 > frame.size) {
            LOG.error("Unable to read status - out of bounds")
            return null
        }
        return ((frame[offset].toInt() and 0xff) shl 16) or
                ((frame[offset + 1].toInt() and 0xff) shl 8) or
                (frame[offset + 2].toInt() and 0xff)
    }

    private fun readValue(frame: ByteArray, offset: Int): Pair<RawValue, Int>? {
        if (offset + 2 > frame.size) {
            LOG.error("Unable to read value - out of bounds")
            return null
        }
        val len = frame[offset].toInt() and 0xff
        val type = frame[offset + 1].toInt() and 0xff
        val dataStart = offset + 2
        val dataEnd = dataStart + len
        if (dataEnd > frame.size) {
            LOG.error("Unable to read value data - out of bounds")
            return null
        }
        return RawValue(type, frame.copyOfRange(dataStart, dataEnd)) to dataEnd
    }
}
