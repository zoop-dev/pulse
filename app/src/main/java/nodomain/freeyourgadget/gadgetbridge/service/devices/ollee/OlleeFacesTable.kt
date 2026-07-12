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

/**
 * 02 37 read / 02 36 write faces table: header then 6-byte records [ID, 01, ENABLED, 01, ??, SLOT].
 * Read-modify-write helper: parse a read-back payload, flip one face's ENABLED byte, write back.
 */
object OlleeFacesTable {

    private const val HEADER_SIZE = 6
    private const val RECORD_SIZE = 6
    private const val RECORD_ID_INDEX = 0
    private const val RECORD_ENABLED_INDEX = 2

    /**
     * Copy of [getFacesReplyPayload] (from a 02 37 read) with [faceId]'s ENABLED byte set to
     * [enabled]; all other bytes identical. Returned unchanged if [faceId] has no record.
     */
    fun withFaceEnabled(getFacesReplyPayload: ByteArray, faceId: Int, enabled: Boolean): ByteArray {
        val enabledByte = if (enabled) 0x01.toByte() else 0x00.toByte()

        // Start with a copy of the payload
        val result = getFacesReplyPayload.copyOf()

        // Iterate through records starting after the 6-byte header
        var offset = HEADER_SIZE
        while (offset + RECORD_SIZE <= result.size) {
            val recordId = result[offset + RECORD_ID_INDEX].toInt() and 0xFF
            if (recordId == faceId) {
                // Found the matching record; flip the ENABLED byte
                result[offset + RECORD_ENABLED_INDEX] = enabledByte
                return result
            }
            offset += RECORD_SIZE
        }

        // Face not found; return the payload unchanged
        return result
    }

    /** ENABLED state of [faceId] in a 02 37 payload; null if no record or payload too short. */
    fun isFaceEnabled(getFacesReplyPayload: ByteArray, faceId: Int): Boolean? {
        // Iterate through records starting after the 6-byte header
        var offset = HEADER_SIZE
        while (offset + RECORD_SIZE <= getFacesReplyPayload.size) {
            val recordId = getFacesReplyPayload[offset + RECORD_ID_INDEX].toInt() and 0xFF
            if (recordId == faceId) {
                // Found the matching record; read the ENABLED byte
                val enabledByte = getFacesReplyPayload[offset + RECORD_ENABLED_INDEX].toInt() and 0xFF
                return enabledByte != 0x00
            }
            offset += RECORD_SIZE
        }

        // Face not found (either unknown ID or incomplete record); return null
        return null
    }
}
