package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml

/**
 * Pure encode/decode logic for a single DUML v1 frame. Reassembling frames that
 * arrive split across multiple notifications is [DumlFrameReassembler]'s
 * job, built on top of [decodeOne] here.
 *
 * Frame layout:
 *   0        magic, always 0x55
 *   1-2      length (bits 0-9, LE) | protocol version (bits 10-15)
 *   3        header CRC8 (over bytes 0-2)
 *   4        sender:   bits 0-4 = module type, bits 5-7 = index
 *   5        receiver: bits 0-4 = module type, bits 5-7 = index
 *   6-7      sequence counter (big-endian - see class-level note on this
 *            in DjiSupport/wherever this is used; this is the one field
 *            that plausibly differs from other DUML transports/generations)
 *   8        bits 0-2 = encryption, bits 5-6 = ack, bit 7 = packet type
 *   9        command set
 *   10       command
 *   11..N-3  payload
 *   N-2..N-1 CRC16 (LE, over bytes 0..N-3)
 */
object DumlCodec {
    const val MAGIC: Byte = 0x55.toByte()
    const val PROTOCOL_VERSION = 1
    const val MIN_FRAME_SIZE = 13 // everything except payload

    /**
     * Tries to decode exactly one frame starting at [offset] in [data].
     * Does not assume [data] contains only one frame - only bytes up to
     * the decoded frame's own length field are consumed/considered.
     */
    fun decodeOne(data: ByteArray, offset: Int = 0): DumlDecodeResult {
        val available = data.size - offset
        if (available < 1) return DumlDecodeResult.NeedMoreData
        if (data[offset] != MAGIC) {
            return DumlDecodeResult.Invalid("bad magic byte 0x%02x".format(data[offset]))
        }
        if (available < 3) return DumlDecodeResult.NeedMoreData

        val lengthAndVersion = (data[offset + 1].toInt() and 0xFF) or
                ((data[offset + 2].toInt() and 0xFF) shl 8)
        val frameLength = lengthAndVersion and 0x3FF
        val version = (lengthAndVersion shr 10) and 0x3F

        if (frameLength < MIN_FRAME_SIZE) {
            return DumlDecodeResult.Invalid("implausible frame length $frameLength")
        }
        if (version != PROTOCOL_VERSION) {
            return DumlDecodeResult.Invalid("unsupported DUML version $version")
        }
        if (available < frameLength) return DumlDecodeResult.NeedMoreData

        val headerCrcGot = data[offset + 3].toInt() and 0xFF
        val headerCrcCalc = crc8(data, offset, 3)
        if (headerCrcGot != headerCrcCalc) {
            return DumlDecodeResult.Invalid(
                "header CRC8 mismatch (got 0x%02x, expected 0x%02x)".format(headerCrcGot, headerCrcCalc)
            )
        }

        val crc16Got = (data[offset + frameLength - 2].toInt() and 0xFF) or
                ((data[offset + frameLength - 1].toInt() and 0xFF) shl 8)
        val crc16Calc = crc16(data, offset, frameLength - 2)
        if (crc16Got != crc16Calc) {
            return DumlDecodeResult.Invalid(
                "CRC16 mismatch (got 0x%04x, expected 0x%04x)".format(crc16Got, crc16Calc)
            )
        }

        val sender = DumlAddress.fromByte(data[offset + 4])
        val receiver = DumlAddress.fromByte(data[offset + 5])
        // seq is 16-bit BE - the only BE field in the frame
        val seq = ((data[offset + 6].toInt() and 0xFF) shl 8) or (data[offset + 7].toInt() and 0xFF)

        val flags = data[offset + 8].toInt() and 0xFF
        val encryption = DumlEncryption.fromValue(flags and 0x07)
        val ack = DumlAck.fromValue((flags shr 5) and 0x03)
        val packetType = DumlPacketType.fromValue((flags shr 7) and 0x01)

        val cmdSet = data[offset + 9].toInt() and 0xFF
        val cmd = data[offset + 10].toInt() and 0xFF
        val payload = data.copyOfRange(offset + 11, offset + frameLength - 2)

        val packet = DumlPacket(sender, receiver, seq, encryption, ack, packetType, cmdSet, cmd, payload)
        return DumlDecodeResult.Success(packet, frameLength)
    }

    /** Convenience one-shot decode for a buffer expected to hold exactly one complete frame. */
    fun decode(data: ByteArray): DumlPacket {
        return when (val result = decodeOne(data, 0)) {
            is DumlDecodeResult.Success -> result.packet
            DumlDecodeResult.NeedMoreData -> throw IllegalArgumentException("Incomplete DUML frame")
            is DumlDecodeResult.Invalid -> throw IllegalArgumentException("Invalid DUML frame: ${result.reason}")
        }
    }

    fun encode(packet: DumlPacket): ByteArray {
        val frameLength = MIN_FRAME_SIZE + packet.payload.size
        val buf = ByteArray(frameLength)

        buf[0] = MAGIC
        val lengthAndVersion = (frameLength and 0x3FF) or ((PROTOCOL_VERSION and 0x3F) shl 10)
        buf[1] = (lengthAndVersion and 0xFF).toByte()
        buf[2] = ((lengthAndVersion shr 8) and 0xFF).toByte()
        buf[3] = crc8(buf, 0, 3).toByte()

        buf[4] = packet.sender.toByte()
        buf[5] = packet.receiver.toByte()
        // seq is 16-bit BE - the only BE field in the frame
        buf[6] = ((packet.seq shr 8) and 0xFF).toByte()
        buf[7] = (packet.seq and 0xFF).toByte()

        val flags = (packet.encryption.value and 0x07) or
                ((packet.ack.value and 0x03) shl 5) or
                ((packet.packetType.value and 0x01) shl 7)
        buf[8] = flags.toByte()

        buf[9] = packet.cmdSet.toByte()
        buf[10] = packet.cmd.toByte()
        System.arraycopy(packet.payload, 0, buf, 11, packet.payload.size)

        val crc16 = crc16(buf, 0, frameLength - 2)
        buf[frameLength - 2] = (crc16 and 0xFF).toByte()
        buf[frameLength - 1] = ((crc16 shr 8) and 0xFF).toByte()

        return buf
    }

    // CRC8: poly=0x31, init=0xEE, reflected in/out -> reflected poly 0x8C, reflected init 0x77
    fun crc8(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        var crc = 0x77
        for (i in offset until offset + length) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8C else crc ushr 1 }
        }
        return crc and 0xFF
    }

    // CRC16: poly=0x1021, init=0x496C, reflected in/out -> reflected poly 0x8408, reflected init 0x3692
    fun crc16(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        var crc = 0x3692
        for (i in offset until offset + length) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8408 else crc ushr 1 }
        }
        return crc and 0xFFFF
    }
}

sealed class DumlDecodeResult {
    data class Success(val packet: DumlPacket, val bytesConsumed: Int) : DumlDecodeResult()
    data object NeedMoreData : DumlDecodeResult()
    data class Invalid(val reason: String) : DumlDecodeResult()
}
