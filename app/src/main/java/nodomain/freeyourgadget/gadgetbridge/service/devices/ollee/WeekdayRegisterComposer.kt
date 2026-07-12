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
 * Single writer for the shared 02 34 register (4-byte BE two's-complement World Time offset + 14
 * ASCII chars = 7 two-char weekday/badge cells). Always composes the FULL payload: the register
 * carries both halves, and a partial write silently resets the watch's World Time (seen on hardware).
 */
class WeekdayRegisterComposer {

    companion object {
        private const val HEADER_SIZE = 4
        private const val WEEKDAY_SLOT_COUNT = 7
        private const val CHARS_PER_SLOT = 2
        private const val TEXT_SIZE = WEEKDAY_SLOT_COUNT * CHARS_PER_SLOT  // 14
        private const val PAYLOAD_SIZE = HEADER_SIZE + TEXT_SIZE  // 18

        private const val MAX_SINGLE_DIGIT_COUNT = 9
        private const val MAX_TWO_DIGIT_COUNT = 11

        private const val BYTE_MASK = 0xFF
        private const val SHIFT_24 = 24
        private const val SHIFT_16 = 16
        private const val SHIFT_8 = 8

        /** The captured default weekday table (Mon..Sun). */
        private val REAL_WEEKDAYS = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

        /**
         * Formats a count into the 2-cell badge slot (2 ASCII chars). The right cell garbles most
         * digits (hardware-verified), so 1..9 -> "N " (blank right), 10/11 -> "10"/"11" (0/1 render
         * cleanly), 12+ -> "11" ("11 or more").
         */
        fun badgeCell(count: Int): String = when {
            count <= MAX_SINGLE_DIGIT_COUNT -> "$count "
            count <= MAX_TWO_DIGIT_COUNT -> count.toString()
            else -> "11"
        }
    }

    /** World Time UTC offset in seconds (big-endian two's-complement in the payload). */
    var worldTimeOffsetSec: Int = 0

    /** Badge count to display (0 = no badge, weekdays shown). */
    var badgeCount: Int = 0

    /**
     * Composes the full 02 34 payload: 4-byte BE offset header + 14 ASCII chars (weekdays, or the
     * badge cell repeated). The header is preserved across badge changes — the invariant that
     * prevents the offset-clobber regression.
     */
    fun composePayload(): ByteArray {
        val header = encodeOffsetHeader(worldTimeOffsetSec)
        val textCells = if (badgeCount == 0) {
            REAL_WEEKDAYS
        } else {
            List(WEEKDAY_SLOT_COUNT) { badgeCell(badgeCount) }
        }
        val text = textCells.joinToString("").toByteArray(Charsets.US_ASCII)
        return header + text
    }

    /** Seeds [worldTimeOffsetSec] from the 0x55 weekday-register read-back; leaves badge unchanged. */
    fun seedFromReadback(getWeekdaysReplyPayload: ByteArray) {
        if (getWeekdaysReplyPayload.size >= HEADER_SIZE) {
            val decodedOffset = decodeOffsetHeader(
                getWeekdaysReplyPayload.copyOfRange(0, HEADER_SIZE)
            )
            if (decodedOffset != null) {
                worldTimeOffsetSec = decodedOffset
            }
        }
    }

    /** Encodes [offsetSeconds] to a 4-byte big-endian two's-complement representation. */
    private fun encodeOffsetHeader(offsetSeconds: Int): ByteArray {
        return byteArrayOf(
            ((offsetSeconds shr SHIFT_24) and BYTE_MASK).toByte(),
            ((offsetSeconds shr SHIFT_16) and BYTE_MASK).toByte(),
            ((offsetSeconds shr SHIFT_8) and BYTE_MASK).toByte(),
            (offsetSeconds and BYTE_MASK).toByte(),
        )
    }

    /** 4-byte BE two's-complement offset -> signed seconds; null on wrong size or > ±18h. */
    private fun decodeOffsetHeader(header: ByteArray): Int? {
        if (header.size != HEADER_SIZE) return null
        val value = (header[0].toInt() shl SHIFT_24) or
            ((header[1].toInt() and BYTE_MASK) shl SHIFT_16) or
            ((header[2].toInt() and BYTE_MASK) shl SHIFT_8) or
            (header[3].toInt() and BYTE_MASK)
        // Plausibility check: ±18 hours = ±64_800 seconds (ISO 8601 bound)
        val MAX_OFFSET_SEC = 64_800
        return value.takeIf { it in -MAX_OFFSET_SEC..MAX_OFFSET_SEC }
    }
}
