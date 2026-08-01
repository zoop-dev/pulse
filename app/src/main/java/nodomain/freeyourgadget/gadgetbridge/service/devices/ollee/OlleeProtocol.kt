/*  Copyright (C) 2026 Ken Blizzard-Caron

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.ollee

import nodomain.freeyourgadget.gadgetbridge.devices.ollee.OlleeConstants
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums
import org.slf4j.LoggerFactory

object OlleeProtocol {

    /** Requested bytes per characteristic write, passed to TransactionBuilder.writeChunkedData. */
    const val MAX_ATT_CHUNK: Int = 20

    /** Set-clock payload field offsets (all fields little-endian) */
    private const val NOW_SEC_OFFSET = 0
    private const val OFFSET_SEC_OFFSET = 4
    private const val LAT_E3_OFFSET = 8
    private const val LON_E3_OFFSET = 12
    private const val CONSTANT_FLAG_OFFSET = 16
    private const val CONSTANT_WORD_OFFSET = 18
    private const val SET_CLOCK_PAYLOAD_SIZE = 20

    /** Constant values in set-clock payload */
    private const val CONSTANT_FLAG = 0x0003
    private const val CONSTANT_WORD = 0xFFFF

    /** Byte manipulation constants */
    private const val BYTE_MASK = 0xFF
    private const val SHIFT_BYTE_1 = 8

    /** Minimum frame length: 00, len, AA, 55, crcHi, crcLo, cmd, target */
    internal const val MIN_FRAME_LENGTH = 8

    /** CRC-16/CCITT-FALSE (init 0xFFFF) over the input bytes, via CheckSums.getCRC16. */
    fun crc16(data: ByteArray): Int = CheckSums.getCRC16(data)

    /**
     * Frames raw [payload] to [target]: 00 len AA 55 crcHi crcLo 02 target payload…
     * (len = inner.size + 4; CRC-16/CCITT-FALSE over the inner bytes).
     */
    fun buildPacket(target: Int, payload: ByteArray): ByteArray {
        require(target in 0..0xFF) { "target must be a single byte (got $target)" }

        val inner = byteArrayOf(0x02, target.toByte()) + payload
        // LEN is a single byte; guard against a payload large enough to truncate it
        require(inner.size + 4 <= 0xFF) { "payload too large for single-byte LEN (inner ${inner.size})" }
        val crc = crc16(inner)

        return byteArrayOf(
            0x00,
            (inner.size + 4).toByte(),
            0xaa.toByte(),
            0x55,
            (crc shr 8).toByte(),
            (crc and 0xFF).toByte()
        ) + inner
    }

    /** The 02 <target> read-request frame (no payload). Reply arrives with target +0x20. */
    fun readRequest(target: Int): ByteArray = buildPacket(target, ByteArray(0))

    /**
     * Set-clock write (target 0x23): wall time, signed UTC offset (sec), lat/lon in degrees×1000
     * (truncated toward zero). All fields little-endian; returns the complete framed packet.
     */
    fun buildSetClock(nowEpochSec: Long, utcOffsetSec: Int, latE3: Int, lonE3: Int): ByteArray {
        val nowSec = nowEpochSec.toInt()
        val payload = ByteArray(SET_CLOCK_PAYLOAD_SIZE)

        // [0:4] nowSec as LE uint32
        BLETypeConversions.writeUint32(payload, NOW_SEC_OFFSET, nowSec)

        // [4:8] utcOffsetSec as LE int32 (signed, two's complement via shr)
        BLETypeConversions.writeUint32(payload, OFFSET_SEC_OFFSET, utcOffsetSec)

        // [8:12] latE3 as LE int32 (signed)
        BLETypeConversions.writeUint32(payload, LAT_E3_OFFSET, latE3)

        // [12:16] lonE3 as LE int32 (signed)
        BLETypeConversions.writeUint32(payload, LON_E3_OFFSET, lonE3)

        // [16:18] CONSTANT_FLAG (0x0003) as LE
        BLETypeConversions.writeUint16(payload, CONSTANT_FLAG_OFFSET, CONSTANT_FLAG)

        // [18:20] CONSTANT_WORD (0xFFFF)
        BLETypeConversions.writeUint16(payload, CONSTANT_WORD_OFFSET, CONSTANT_WORD)

        return buildPacket(OlleeConstants.TARGET_SET_CLOCK, payload)
    }

    /**
     * Battery millivolts from a version-reply (0x4A) payload: big-endian uint16 at
     * VERSION_REPLY_VOLTAGE_OFFSET (34). Null if too short.
     */
    fun parseVoltageMillivolts(versionReplyPayload: ByteArray): Int? {
        val offset = OlleeConstants.VERSION_REPLY_VOLTAGE_OFFSET
        if (versionReplyPayload.size < offset + 2) return null
        val hi = versionReplyPayload[offset].toInt() and BYTE_MASK
        val lo = versionReplyPayload[offset + 1].toInt() and BYTE_MASK
        return (hi shl SHIFT_BYTE_1) or lo
    }

    /** ASCII hardware revision from a version-reply (0x4A) payload, e.g. "01.05.00". */
    fun parseHardwareVersion(versionReplyPayload: ByteArray): String? =
        parseVersionField(versionReplyPayload, HARDWARE_VERSION_OFFSET)

    /**
     * ASCII firmware version from a version-reply (0x4A) payload, e.g. "00.01.10". The reply holds
     * hardware and firmware as two adjacent 8-char triples, so reading all 16 as one string reports
     * the hardware revision as part of the firmware version.
     */
    fun parseFirmwareVersion(versionReplyPayload: ByteArray): String? =
        parseVersionField(versionReplyPayload, FIRMWARE_VERSION_OFFSET)

    /** Null if the payload is too short or the field is not printable ASCII. */
    private fun parseVersionField(payload: ByteArray, offset: Int): String? {
        val end = offset + VERSION_FIELD_LENGTH
        if (payload.size < end) return null
        val chars = payload.copyOfRange(offset, end)
        if (chars.any { it < 0x20 || it > 0x7E }) return null
        return String(chars, Charsets.US_ASCII).trim()
    }

    private const val HARDWARE_VERSION_OFFSET = 8
    private const val FIRMWARE_VERSION_OFFSET = 16
    private const val VERSION_FIELD_LENGTH = 8
}

/**
 * Reassembles fragmented 20-byte BLE notifications into complete frames. Feed each fragment to
 * [accept], which returns a [Frame] once LEN is satisfied and CRC is valid; stray leading bytes that
 * cannot begin a valid frame are dropped.
 */
class OlleeFrameReassembler {
    private companion object {
        private val LOG = LoggerFactory.getLogger(OlleeFrameReassembler::class.java)
    }

    private var buffer = ByteArray(256)
    private var size = 0

    /**
     * Accepts a fragment, returning a complete [Frame] if ready else null (leading junk dropped).
     * The watch pipelines replies: one 20-byte fragment can carry the tail of one frame and the head
     * of the next, so the caller must drain [pending] until null after each accepted fragment.
     */
    fun accept(chunk: ByteArray): Frame? {
        // Grow buffer if needed
        if (size + chunk.size > buffer.size) {
            val newSize = (buffer.size * 2).coerceAtLeast(size + chunk.size)
            val newBuffer = ByteArray(newSize)
            System.arraycopy(buffer, 0, newBuffer, 0, size)
            buffer = newBuffer
        }

        // Append chunk to buffer
        System.arraycopy(chunk, 0, buffer, size, chunk.size)
        size += chunk.size

        return extractFrame()
    }

    /** Returns the next complete frame already buffered, if any. Call until null. */
    fun pending(): Frame? = extractFrame()

    private fun extractFrame(): Frame? {
        while (true) {
            var pos = 0

            // Drop leading bytes that can't begin a valid frame, re-syncing to the next candidate
            while (pos < size && buffer[pos] != 0x00.toByte()) {
                pos++
            }

            // Check for AA 55 magic once 4 bytes are available
            while (pos + 4 <= size && (buffer[pos + 2] != 0xAA.toByte() || buffer[pos + 3] != 0x55.toByte())) {
                pos++
                while (pos < size && buffer[pos] != 0x00.toByte()) {
                    pos++
                }
            }

            // Frame length is LEN + 2 bytes (00 LEN AA 55 CRC CRC ... )
            if (pos + 4 > size) {
                compact(pos)
                return null
            }
            val len = buffer[pos + 1].toInt() and 0xFF
            val total = len + 2
            if (pos + total > size) {
                compact(pos)
                return null
            }

            val frameBytes = buffer.copyOfRange(pos, pos + total)
            compact(pos + total)

            val frame = parseFrameRaw(frameBytes)
            // On CRC/header failure the bad frame is consumed; keep scanning — a valid
            // frame may already be buffered behind it.
            if (frame != null) return frame
        }
    }

    /** Discards everything before [from], keeping any partial frame that follows. */
    private fun compact(from: Int) {
        if (from <= 0) return
        val keep = size - from
        if (keep > 0) {
            System.arraycopy(buffer, from, buffer, 0, keep)
        }
        size = keep
    }

    fun reset() {
        size = 0
    }

    /**
     * Parses a complete framed message (00 LEN AA 55 crcHi crcLo 02 target payload…) into a [Frame].
     * Null if too short, missing the AA 55 magic, or CRC mismatch.
     */
    private fun parseFrameRaw(bytes: ByteArray): Frame? {
        val hasValidHeader = bytes.size >= OlleeProtocol.MIN_FRAME_LENGTH &&
            bytes[2] == 0xAA.toByte() && bytes[3] == 0x55.toByte()
        if (!hasValidHeader) {
            LOG.warn("Dropping frame with invalid header ({} bytes)", bytes.size)
            return null
        }

        val crcField = ((bytes[4].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
        val inner = bytes.copyOfRange(6, bytes.size)

        val computedCrc = OlleeProtocol.crc16(inner)
        if (computedCrc != crcField) {
            LOG.warn("Dropping frame with CRC mismatch (field=0x{}, computed=0x{})",
                Integer.toHexString(crcField), Integer.toHexString(computedCrc))
            return null
        }

        val target = inner[1].toInt() and 0xFF
        val payload = inner.copyOfRange(2, inner.size)
        return Frame(target, payload)
    }
}

/**
 * A parsed frame from the watch: the target byte and the payload (without framing or CRC).
 */
data class Frame(val target: Int, val payload: ByteArray)
