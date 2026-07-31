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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OlleeFacesTableTest {

    private fun String.hex(): ByteArray =
        ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    /** Verbatim readback from the Ollee Copper, 2026-07-30: 17 faces, all shown, Timer starred. */
    private val live = (
        "040000000000" +
            "050100010001" + "060100010002" + "070100010003" + "080100010011" +
            "090100010104" + "0A0100010005" + "0B0100010008" + "0C0100010009" +
            "0D010001000A" + "0E010001000B" + "0F010001000C" + "10010001000D" +
            "11010001000F" + "12010001000E" + "130100010007" + "140100010006" +
            "150100010010"
        ).hex()

    @Test
    fun parsesSeventeenRecords() {
        assertEquals(108, live.size)
        assertEquals(17, OlleeFacesTable.parse(live).size)
    }

    @Test
    fun allFacesAreEnabledInTheLiveTable() {
        // Byte 2 is 0 on every record and the vendor app showed every face switched ON, which is
        // what proves byte 2 means "hidden" rather than "enabled".
        assertTrue(OlleeFacesTable.parse(live).all { it.enabled })
    }

    @Test
    fun onlyTimerIsStarred() {
        val starred = OlleeFacesTable.parse(live).filter { it.starred }.map { it.id }
        assertEquals(listOf(0x09), starred)
    }

    @Test
    fun slotOrderIsNotPayloadOrder() {
        // Records are laid out by ID, but Set Clock (0x08) sits at the last display slot. The two
        // disagreeing is what identifies byte 5 as the display order.
        val faces = OlleeFacesTable.parse(live)
        assertEquals(listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21),
            faces.map { it.id })
        assertEquals(17, OlleeFacesTable.slotOf(live, 0x08))
        assertEquals(4, OlleeFacesTable.slotOf(live, 0x09))
    }

    @Test
    fun disablingWritesOneToTheHiddenByte() {
        // Hardware 2026-07-30: turning Flashlight (0x0E) off in the vendor app produced a frame
        // whose only change was this byte going 0 -> 1.
        val result = OlleeFacesTable.withFaceEnabled(live, 0x0E, false)
        assertEquals(1, result[6 + 6 * 9 + 2].toInt())
        assertFalse(OlleeFacesTable.isFaceEnabled(result, 0x0E)!!)
        assertTrue(OlleeFacesTable.isFaceEnabled(result, 0x0F)!!)
    }

    @Test
    fun enablingWritesZeroToTheHiddenByte() {
        val hidden = OlleeFacesTable.withFaceEnabled(live, 0x0E, false)
        val shown = OlleeFacesTable.withFaceEnabled(hidden, 0x0E, true)
        assertTrue(OlleeFacesTable.isFaceEnabled(shown, 0x0E)!!)
        assertArrayEqualsIgnoringNothing(live, shown)
    }

    @Test
    fun starringIsIndependentPerFace() {
        // Starring Alarm while Timer was starred produced a frame with byte 4 set on both, so the
        // star is not a single mutually-exclusive selection.
        val both = OlleeFacesTable.withFaceStarred(live, 0x05, true)
        assertTrue(OlleeFacesTable.isFaceStarred(both, 0x05)!!)
        assertTrue(OlleeFacesTable.isFaceStarred(both, 0x09)!!)
    }

    @Test
    fun reorderRenumbersSlotsFromOne() {
        val result = OlleeFacesTable.withOrder(live, listOf(0x09, 0x06, 0x05))
        assertEquals(1, OlleeFacesTable.slotOf(result, 0x09))
        assertEquals(2, OlleeFacesTable.slotOf(result, 0x06))
        assertEquals(3, OlleeFacesTable.slotOf(result, 0x05))
    }

    @Test
    fun facesOmittedFromAReorderKeepTheirSlot() {
        // A partial list must not blank the rest of the table.
        val result = OlleeFacesTable.withOrder(live, listOf(0x09))
        assertEquals(17, OlleeFacesTable.slotOf(result, 0x08))
        assertEquals(5, OlleeFacesTable.slotOf(result, 0x0A))
    }

    @Test
    fun unknownFaceLeavesThePayloadUntouched() {
        val result = OlleeFacesTable.withFaceEnabled(live, 0x42, false)
        assertArrayEqualsIgnoringNothing(live, result)
        assertNull(OlleeFacesTable.isFaceEnabled(live, 0x42))
    }

    @Test
    fun aTruncatedRecordIsIgnoredRatherThanCrashing() {
        val truncated = live.copyOf(live.size - 3)
        assertEquals(16, OlleeFacesTable.parse(truncated).size)
        assertNull(OlleeFacesTable.isFaceEnabled(truncated, 0x15))
    }

    private fun assertArrayEqualsIgnoringNothing(expected: ByteArray, actual: ByteArray) {
        assertEquals(expected.toList(), actual.toList())
    }
}
