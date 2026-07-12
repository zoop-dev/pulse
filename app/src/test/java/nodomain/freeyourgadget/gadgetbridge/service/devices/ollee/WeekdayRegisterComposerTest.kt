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

import org.junit.Test
import org.junit.Assert.*

class WeekdayRegisterComposerTest {

    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    // Test 1: encode Tokyo offset +9:00 (32400 s) -> header bytes 00 00 7E 90
    @Test
    fun encodesTokyoOffset() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = 32_400
        val payload = composer.composePayload()
        val header = payload.copyOfRange(0, 4)
        assertArrayEquals("00007e90".hexToByteArray(), header)
    }

    // Test 2: encode MDT offset -6:00 (-21600 s) -> header bytes FF FF AB A0
    @Test
    fun encodeMdtOffset() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = -21_600
        val payload = composer.composePayload()
        val header = payload.copyOfRange(0, 4)
        assertArrayEquals("ffffaba0".hexToByteArray(), header)
    }

    // Test 3: encode IST offset +5:30 (19800 s) -> header bytes 00 00 4D 58
    @Test
    fun encodeIstOffset() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = 19_800
        val payload = composer.composePayload()
        val header = payload.copyOfRange(0, 4)
        assertArrayEquals("00004d58".hexToByteArray(), header)
    }

    // Test 4: badgeCount 0 -> payload text bytes are "MOTUWETHFRSASU" (ASCII)
    @Test
    fun badgeCountZeroShowsWeekdays() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = 0
        composer.badgeCount = 0
        val payload = composer.composePayload()
        assertEquals(18, payload.size)  // 4 header + 14 chars
        val text = payload.copyOfRange(4, 18).toString(Charsets.US_ASCII)
        assertEquals("MOTUWETHFRSASU", text)
    }

    // Test 5: badgeCount 6 -> seven cells of "6 "
    @Test
    fun badgeCount6ShowsSixInAllCells() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = 0
        composer.badgeCount = 6
        val payload = composer.composePayload()
        assertEquals(18, payload.size)
        val text = payload.copyOfRange(4, 18).toString(Charsets.US_ASCII)
        assertEquals("6 6 6 6 6 6 6 ", text)
    }

    // Test 6: badgeCount 1-9 are left-aligned in cells
    @Test
    fun badgeCellFormatsSingleDigits() {
        assertEquals("1 ", WeekdayRegisterComposer.badgeCell(1))
        assertEquals("5 ", WeekdayRegisterComposer.badgeCell(5))
        assertEquals("9 ", WeekdayRegisterComposer.badgeCell(9))
    }

    // Test 7: badgeCount 10 and 11 format as two digits
    @Test
    fun badgeCellFormatsTenAndEleven() {
        assertEquals("10", WeekdayRegisterComposer.badgeCell(10))
        assertEquals("11", WeekdayRegisterComposer.badgeCell(11))
    }

    // Test 8: badgeCount 10 -> seven cells of "10"
    @Test
    fun badgeCount10ShowsTenInAllCells() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = 0
        composer.badgeCount = 10
        val payload = composer.composePayload()
        val text = payload.copyOfRange(4, 18).toString(Charsets.US_ASCII)
        assertEquals("10101010101010", text)
    }

    // Test 9: badgeCount 15 caps at "11" in all cells
    @Test
    fun badgeCount15ShowsElevenInAllCells() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = 0
        composer.badgeCount = 15
        val payload = composer.composePayload()
        val text = payload.copyOfRange(4, 18).toString(Charsets.US_ASCII)
        assertEquals("11111111111111", text)
    }

    // Test 10: badgeCell caps at 11 for counts >= 12
    @Test
    fun badgeCellCapsAtEleven() {
        assertEquals("11", WeekdayRegisterComposer.badgeCell(12))
        assertEquals("11", WeekdayRegisterComposer.badgeCell(99))
        assertEquals("11", WeekdayRegisterComposer.badgeCell(4321))
    }

    // Test 11: seedFromReadback parses offset from 0x55 reply payload
    @Test
    fun seedFromReadbackParsesOffset() {
        val composer = WeekdayRegisterComposer()
        // Readback payload: 4-byte header (FF FF AB A0 = -21600) + weekday text (MOTUWETHFRSASU)
        val readbackPayload = "ffffaba04d4f545557455448465253415355".hexToByteArray()
        composer.seedFromReadback(readbackPayload)
        assertEquals(-21_600, composer.worldTimeOffsetSec)
    }

    // Test 12: seedFromReadback rejects short payload
    @Test
    fun seedFromReadbackRejectsShortPayload() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = 999  // set a non-default value
        val shortPayload = "ffff".hexToByteArray()
        composer.seedFromReadback(shortPayload)
        // worldTimeOffsetSec should not change if payload is too short
        assertEquals(999, composer.worldTimeOffsetSec)
    }

    // Test 13: offset header invariant: changing badgeCount doesn't change header bytes
    @Test
    fun offsetHeaderInvariantWithBadgeChanges() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = -21_600

        val payload0 = composer.composePayload()
        val header0 = payload0.copyOfRange(0, 4)

        composer.badgeCount = 6
        val payload6 = composer.composePayload()
        val header6 = payload6.copyOfRange(0, 4)

        composer.badgeCount = 10
        val payload10 = composer.composePayload()
        val header10 = payload10.copyOfRange(0, 4)

        assertArrayEquals(header0, header6)
        assertArrayEquals(header0, header10)
    }

    // Test 14: composePayload size is always 18 bytes (4 header + 14 text)
    @Test
    fun payloadSizeAlwaysEighteen() {
        val composer = WeekdayRegisterComposer()
        assertEquals(18, composer.composePayload().size)

        composer.badgeCount = 5
        assertEquals(18, composer.composePayload().size)

        composer.worldTimeOffsetSec = 32_400
        assertEquals(18, composer.composePayload().size)
    }

    // Test 15: badgeCount 0 with Tokyo offset
    @Test
    fun weekdaysWithTokyoOffset() {
        val composer = WeekdayRegisterComposer()
        composer.worldTimeOffsetSec = 32_400
        composer.badgeCount = 0
        val payload = composer.composePayload()
        val header = payload.copyOfRange(0, 4)
        val text = payload.copyOfRange(4, 18).toString(Charsets.US_ASCII)
        assertArrayEquals("00007e90".hexToByteArray(), header)
        assertEquals("MOTUWETHFRSASU", text)
    }
}
