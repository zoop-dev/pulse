/*  Copyright (C) 2026 Toby Murray

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.una

import nodomain.freeyourgadget.gadgetbridge.devices.una.UnaConstants
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions

/** A single entry streamed back from a 0x50 directory-listing request. */
data class UnaFtsListEntry(val index: Int, val total: Int, val attr: Int, val name: String) {
    /** FTS reports directories with attr bit 0 set; files clear it. */
    val isDirectory: Boolean get() = (attr and 0x1) != 0
}

/** One chunk of a whole-file read (0x10/0x12 request, 0x11 response). */
data class UnaFtsReadChunk(val offset: Int, val total: Int, val payload: ByteArray)

/**
 * Wire encoding for FTS. No BLE or Android dependencies, so it is testable directly against
 * captured bytes. Protocol reference: UNA's Docs/BLE-File-Transfer-Service.md.
 */
object UnaFtsProtocol {
    private const val LIST_ENTRY_HEADER_SIZE = 28
    /** Bytes of `0x11` header before the payload, which a chunk size must leave room for. */
    const val READ_CHUNK_HEADER_SIZE = 16

    /** ATT's own per-notification overhead, which the MTU has to cover alongside the payload. */
    private const val ATT_NOTIFICATION_OVERHEAD = 3

    /**
     * Largest chunk one notification can carry at [mtu]. The firmware returns the requested bytes
     * behind a 16-byte header in a single notification, so the frame must fit the MTU less ATT's
     * three bytes.
     *
     * `mtu - 19` is the real limit, not a safety margin. Above it the firmware replies with a
     * header advertising more bytes than the notification carries and never sends the remainder,
     * so a client that believes the header waits forever
     * (https://github.com/UNAWatch/una-sdk/issues/272).
     *
     * Clamping is therefore one-directional: [UnaConstants.MAX_READ_CHUNK_SIZE] may lower the
     * result but nothing may raise it, since anything above capacity hangs. A link too small to
     * carry a useful chunk reads slowly rather than incorrectly.
     */
    fun readChunkSizeFor(mtu: Int): Int =
        (mtu - ATT_NOTIFICATION_OVERHEAD - READ_CHUNK_HEADER_SIZE)
            .coerceAtMost(UnaConstants.MAX_READ_CHUNK_SIZE)
            .coerceAtLeast(1)

    /** 0x50 00 <path_len:u16LE> <path>. */
    fun buildListRequest(path: String): ByteArray {
        val pathBytes = path.toByteArray(Charsets.US_ASCII)
        return byteArrayOf(UnaConstants.CMD_LIST_DIR.toByte(), 0) +
            BLETypeConversions.fromUint16(pathBytes.size) +
            pathBytes
    }

    /** 0x10 00 <path_len:u16LE> <offset:u32LE> <chunk_len:u32LE> <path>, one request per chunk. */
    fun buildReadRequest(path: String, offset: Int, chunkLen: Int): ByteArray {
        val pathBytes = path.toByteArray(Charsets.US_ASCII)
        return byteArrayOf(UnaConstants.CMD_READ.toByte(), 0) +
            BLETypeConversions.fromUint16(pathBytes.size) +
            BLETypeConversions.fromUint32(offset) +
            BLETypeConversions.fromUint32(chunkLen) +
            pathBytes
    }

    /**
     * Parses a 0x51 list-entry notification. Bytes 16-27 (mtime and/or reserved, not confirmed
     * which) are present but unused. Null if too short or the wrong opcode.
     */
    fun parseListEntry(data: ByteArray): UnaFtsListEntry? {
        if (data.size < LIST_ENTRY_HEADER_SIZE || (data[0].toInt() and 0xFF) != UnaConstants.RESP_LIST_ENTRY) return null
        val nameLen = BLETypeConversions.toUint16(data, 2)
        val index = BLETypeConversions.toUint32(data, 4)
        val total = BLETypeConversions.toUint32(data, 8)
        val attr = BLETypeConversions.toUint32(data, 12)
        if (data.size < LIST_ENTRY_HEADER_SIZE + nameLen) return null
        val name = String(data, LIST_ENTRY_HEADER_SIZE, nameLen, Charsets.US_ASCII)
        return UnaFtsListEntry(index, total, attr, name)
    }

    /** Parses a 0x11 read-chunk notification. Null if too short or the wrong opcode. */
    fun parseReadChunk(data: ByteArray): UnaFtsReadChunk? {
        if (data.size < READ_CHUNK_HEADER_SIZE || (data[0].toInt() and 0xFF) != UnaConstants.RESP_READ_CHUNK) return null
        val offset = BLETypeConversions.toUint32(data, 4)
        val total = BLETypeConversions.toUint32(data, 8)
        val chunkLen = BLETypeConversions.toUint32(data, 12)
        // chunkLen is an untrusted wire-supplied u32 in a signed Int, so `HEADER + chunkLen` can
        // wrap negative and defeat this check. Compare as unsigned in a Long instead.
        val chunkLenUnsigned = chunkLen.toLong() and 0xFFFFFFFFL
        if (data.size.toLong() < READ_CHUNK_HEADER_SIZE + chunkLenUnsigned) return null
        val payload = data.copyOfRange(READ_CHUNK_HEADER_SIZE, READ_CHUNK_HEADER_SIZE + chunkLen)
        return UnaFtsReadChunk(offset, total, payload)
    }
}
