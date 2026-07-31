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
 * 02 37 read / 02 36 write faces table: 6-byte header then 6-byte records
 * `[ID, 01, HIDDEN, 01, STARRED, SLOT]`. Read-modify-write: parse a readback, change one field,
 * write the whole table back. Decoded from a live readback and vendor-app writes 2026-07-30.
 */
object OlleeFacesTable {

    private const val HEADER_SIZE = 6
    private const val RECORD_SIZE = 6
    private const val RECORD_ID_INDEX = 0

    /** 1 means the face is hidden, so enabled is the *absence* of this byte. Do not invert. */
    private const val RECORD_HIDDEN_INDEX = 2

    /** Marks a face as a Favorite-glance target. Any number of faces may carry it. */
    private const val RECORD_STARRED_INDEX = 4

    /** Display position, 1-based. Independent of the record's position in the payload. */
    private const val RECORD_SLOT_INDEX = 5

    data class Face(val id: Int, val enabled: Boolean, val starred: Boolean, val slot: Int)

    /** Every record in the table, in payload order (which is by ID, not by slot). */
    fun parse(payload: ByteArray): List<Face> {
        val faces = mutableListOf<Face>()
        var offset = HEADER_SIZE
        while (offset + RECORD_SIZE <= payload.size) {
            faces.add(
                Face(
                    id = payload[offset + RECORD_ID_INDEX].toInt() and 0xFF,
                    enabled = payload[offset + RECORD_HIDDEN_INDEX].toInt() and 0xFF == 0,
                    starred = payload[offset + RECORD_STARRED_INDEX].toInt() and 0xFF != 0,
                    slot = payload[offset + RECORD_SLOT_INDEX].toInt() and 0xFF
                )
            )
            offset += RECORD_SIZE
        }
        return faces
    }

    /** Copy of [payload] with [faceId]'s hidden byte set so the face is [enabled]. */
    fun withFaceEnabled(payload: ByteArray, faceId: Int, enabled: Boolean): ByteArray =
        withByte(payload, faceId, RECORD_HIDDEN_INDEX, if (enabled) 0x00 else 0x01)

    /** Copy of [payload] with [faceId] marked as a Favorite-glance target or not. */
    fun withFaceStarred(payload: ByteArray, faceId: Int, starred: Boolean): ByteArray =
        withByte(payload, faceId, RECORD_STARRED_INDEX, if (starred) 0x01 else 0x00)

    /** Copy of [payload] with [faceId] moved to 1-based display position [slot]. */
    fun withFaceSlot(payload: ByteArray, faceId: Int, slot: Int): ByteArray =
        withByte(payload, faceId, RECORD_SLOT_INDEX, slot)

    /**
     * Copy of [payload] with slots renumbered so the faces appear in [orderedIds]. IDs missing from
     * the list keep their existing slot, so a partial list cannot silently blank the rest.
     */
    fun withOrder(payload: ByteArray, orderedIds: List<Int>): ByteArray {
        var result = payload
        orderedIds.forEachIndexed { index, faceId ->
            result = withFaceSlot(result, faceId, index + 1)
        }
        return result
    }

    fun isFaceEnabled(payload: ByteArray, faceId: Int): Boolean? =
        parse(payload).firstOrNull { it.id == faceId }?.enabled

    fun isFaceStarred(payload: ByteArray, faceId: Int): Boolean? =
        parse(payload).firstOrNull { it.id == faceId }?.starred

    fun slotOf(payload: ByteArray, faceId: Int): Int? =
        parse(payload).firstOrNull { it.id == faceId }?.slot

    private fun withByte(payload: ByteArray, faceId: Int, index: Int, value: Int): ByteArray {
        val result = payload.copyOf()
        var offset = HEADER_SIZE
        while (offset + RECORD_SIZE <= result.size) {
            if (result[offset + RECORD_ID_INDEX].toInt() and 0xFF == faceId) {
                result[offset + index] = value.toByte()
                return result
            }
            offset += RECORD_SIZE
        }
        return result
    }
}
