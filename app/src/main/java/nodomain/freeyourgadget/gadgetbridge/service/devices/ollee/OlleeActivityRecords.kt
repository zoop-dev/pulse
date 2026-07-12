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

object OlleeActivityRecords {

    data class Record(val typeFlags: Long, val startTs: Long, val endTs: Long, val steps: Long)

    /** Parses the 0x47 count reply (u32 BE). Throws if payload shorter than 4 bytes. */
    fun parseCount(payload: ByteArray): Long {
        require(payload.size >= 4) { "Payload must be at least 4 bytes, got ${payload.size}" }
        return readU32BE(payload, 0)
    }

    /** Parses one 0x48 record reply: exactly 16 bytes = four u32 BE fields. Throws on wrong length. */
    fun parseRecord(payload: ByteArray): Record {
        require(payload.size == 16) { "Payload must be exactly 16 bytes, got ${payload.size}" }
        val typeFlags = readU32BE(payload, 0)
        val startTs = readU32BE(payload, 4)
        val endTs = readU32BE(payload, 8)
        val steps = readU32BE(payload, 12)
        return Record(typeFlags, startTs, endTs, steps)
    }

    private fun readU32BE(data: ByteArray, offset: Int): Long {
        return ((data[offset].toInt() and 0xFF).toLong() shl 24) or
                ((data[offset + 1].toInt() and 0xFF).toLong() shl 16) or
                ((data[offset + 2].toInt() and 0xFF).toLong() shl 8) or
                (data[offset + 3].toInt() and 0xFF).toLong()
    }
}
