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
 * Pure request/response encoding for the FTS wire protocol -- no BLE or Android dependencies,
 * so this is unit-testable directly against captured bytes.
 */
object UnaFtsProtocol {
    private const val LIST_ENTRY_HEADER_SIZE = 28
    private const val READ_CHUNK_HEADER_SIZE = 16

    /** 0x50 00 <path_len:u16LE> <path>. */
    fun buildListRequest(path: String): ByteArray {
        val pathBytes = path.toByteArray(Charsets.US_ASCII)
        return byteArrayOf(UnaConstants.CMD_LIST_DIR.toByte(), 0) +
            BLETypeConversions.fromUint16(pathBytes.size) +
            pathBytes
    }

    /** 0x10 00 <path_len:u16LE> <offset:u32LE> <chunk_len:u32LE> <path>, one request per chunk. */
    fun buildReadRequest(path: String, offset: Int, chunkLen: Int = UnaConstants.READ_CHUNK_SIZE): ByteArray {
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
        // chunkLen is an untrusted, wire-supplied u32 read into a signed Int -- comparing/adding
        // it directly against data.size (as `HEADER + chunkLen`) can wrap negative for a
        // corrupted or hostile value, silently defeating this truncation check. Masking to its
        // real unsigned value in a Long before comparing closes that hole.
        val chunkLenUnsigned = chunkLen.toLong() and 0xFFFFFFFFL
        if (data.size.toLong() < READ_CHUNK_HEADER_SIZE + chunkLenUnsigned) return null
        val payload = data.copyOfRange(READ_CHUNK_HEADER_SIZE, READ_CHUNK_HEADER_SIZE + chunkLen)
        return UnaFtsReadChunk(offset, total, payload)
    }
}
