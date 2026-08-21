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

data class UnaFtsReadChunk(
    val offset: Int,
    val total: Int,
    val payload: ByteArray,
    val deliveredLessThanAdvertised: Boolean,
)

/**
 * Wire encoding for FTS. No BLE or Android dependencies, so it is testable directly against
 * captured bytes. Protocol reference: UNA's Docs/BLE-File-Transfer-Service.md.
 */
object UnaFtsProtocol {
    private const val LIST_ENTRY_HEADER_SIZE = 28
    const val READ_CHUNK_HEADER_SIZE = 16
    private const val STATUS_OK = 0x01

    private fun uint32AsLong(data: ByteArray, offset: Int): Long =
        BLETypeConversions.toUint32(data, offset).toLong() and 0xFFFFFFFFL

    /** 0x50 00 <path_len:u16LE> <path>. */
    fun buildListRequest(path: String): ByteArray {
        val pathBytes = path.toByteArray(Charsets.US_ASCII)
        return byteArrayOf(UnaConstants.CMD_LIST_DIR.toByte(), 0) +
            BLETypeConversions.fromUint16(pathBytes.size) +
            pathBytes
    }

    /** 0x10 00 <path_len:u16LE> <offset:u32LE> <chunk_len:u32LE> <path>. */
    fun buildReadRequest(path: String, offset: Int, chunkLen: Int): ByteArray {
        val pathBytes = path.toByteArray(Charsets.US_ASCII)
        return byteArrayOf(UnaConstants.CMD_READ.toByte(), 0) +
            BLETypeConversions.fromUint16(pathBytes.size) +
            BLETypeConversions.fromUint32(offset) +
            BLETypeConversions.fromUint32(chunkLen) +
            pathBytes
    }

    /** 0x12 01 0000 <offset:u32LE> <chunk_len:u32LE>. */
    fun buildReadPacingRequest(offset: Int, chunkLen: Int): ByteArray =
        byteArrayOf(UnaConstants.CMD_READ_PACING.toByte(), STATUS_OK.toByte(), 0, 0) +
            BLETypeConversions.fromUint32(offset) +
            BLETypeConversions.fromUint32(chunkLen)

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

    /** Parses a 0x11 read-chunk notification, or null if it carries no usable payload. */
    fun parseReadChunk(data: ByteArray): UnaFtsReadChunk? {
        if (data.size < READ_CHUNK_HEADER_SIZE || (data[0].toInt() and 0xFF) != UnaConstants.RESP_READ_CHUNK) return null
        val offset = BLETypeConversions.toUint32(data, 4)
        val total = BLETypeConversions.toUint32(data, 8)
        val advertised = uint32AsLong(data, 12)
        val delivered = (data.size - READ_CHUNK_HEADER_SIZE).toLong()
        // Firmware can advertise more than it sends: https://github.com/UNAWatch/una-sdk/issues/272
        val payloadLen = minOf(advertised, delivered).toInt()
        if (payloadLen <= 0) return null
        val payload = data.copyOfRange(READ_CHUNK_HEADER_SIZE, READ_CHUNK_HEADER_SIZE + payloadLen)
        return UnaFtsReadChunk(offset, total, payload, deliveredLessThanAdvertised = advertised > payloadLen)
    }
}
