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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnaHourlyHrProtocolTest {
    @Test
    fun buildRequest_matchesDailyHealthShapePlusHour() {
        // 0x14 00 <year u16LE> <month> <day> <hour>, local wall-clock fields.
        val expected = UnaFtsProtocolTest.hexToByteArray("1400ea0708020e")
        assertArrayEquals(
            expected,
            UnaHourlyHrProtocol.buildRequest(year = 2026, month = 8, day = 2, hour = 14),
        )
    }

    @Test
    fun buildRequest_encodesMidnightHourAsZero() {
        val request = UnaHourlyHrProtocol.buildRequest(year = 2026, month = 1, day = 31, hour = 0)
        assertArrayEquals(UnaFtsProtocolTest.hexToByteArray("1400ea07011f00"), request)
    }

    @Test
    fun parseResponse_readsOneHeartRatePerMinute() {
        val minutes = IntArray(60) { 60 + it }
        val hr = UnaHourlyHrProtocol.parseResponse(response(minutes))
        assertEquals(minutes.toList(), hr?.minutes?.toList())
    }

    @Test
    fun parseResponse_readsRatesAboveSignedByteRange() {
        // 0xC8 is negative as a signed Kotlin Byte, so 200 must come back as 200, not -56.
        val minutes = IntArray(60).also { it[7] = 200 }
        val hr = UnaHourlyHrProtocol.parseResponse(response(minutes))
        assertEquals(200, hr?.minutes?.get(7))
    }

    @Test
    fun measuredMinutes_dropsUnmeasuredZeroMinutes() {
        // A partially-filled hour: only minutes 0, 1 and 59 carry a reading.
        val minutes = IntArray(60)
        minutes[0] = 72
        minutes[1] = 74
        minutes[59] = 68
        val hr = UnaHourlyHrProtocol.parseResponse(response(minutes))
        assertEquals(listOf(0 to 72, 1 to 74, 59 to 68), hr?.measuredMinutes())
    }

    @Test
    fun measuredMinutes_isEmptyForAnHourWithNoReadings() {
        val hr = UnaHourlyHrProtocol.parseResponse(response(IntArray(60)))
        assertEquals(emptyList<Pair<Int, Int>>(), hr?.measuredMinutes())
    }

    @Test
    fun parseResponse_realCapturedResponse_partiallyMeasuredHour() {
        // Real 0x14 response for an hour the watch was put on partway through, so the first 39
        // minutes are zero and the rest carry readings.
        val raw = UnaFtsProtocolTest.hexToByteArray(
            "1401" + "00".repeat(39) + "696969696971714747475757454537374646514d3e"
        )
        val hr = UnaHourlyHrProtocol.parseResponse(raw)
        val measured = hr!!.measuredMinutes()
        assertEquals(21, measured.size)
        assertEquals(39 to 105, measured.first())
        assertEquals(59 to 62, measured.last())
    }

    @Test
    fun parseResponse_realCapturedResponse_emptyHourStillReportsSuccess() {
        // Real response for an hour the watch was not worn. It is NOT an error: the watch
        // answers 14 01 with an all-zero payload, so "no data" has to be read off the payload
        // rather than the status byte.
        val raw = UnaFtsProtocolTest.hexToByteArray("1401" + "00".repeat(60))
        val hr = UnaHourlyHrProtocol.parseResponse(raw)
        assertEquals(emptyList<Pair<Int, Int>>(), hr?.measuredMinutes())
    }

    @Test
    fun parseResponse_realCapturedResponse_isolatedZeroMidHour() {
        // Real response where minute 5 is zero between good readings, so zeros cannot be treated
        // as "the hour ends here".
        val raw = UnaFtsProtocolTest.hexToByteArray(
            "140142424b4b4600484848444443434141404042424b4747414141414342" +
                "524b4b4646376062605d5660465455554644443e3e4040434341413e3e45" +
                "4343"
        )
        val hr = UnaHourlyHrProtocol.parseResponse(raw)
        val measured = hr!!.measuredMinutes()
        assertEquals(59, measured.size)
        // A zero sandwiched between two good readings, not a trailing gap.
        assertEquals(70, hr.minutes[4])
        assertEquals(0, hr.minutes[5])
        assertEquals(72, hr.minutes[6])
    }

    @Test
    fun parseResponse_rejectsErrorStatus() {
        // Same opcode, non-OK status: the watch has no matrix for the requested hour.
        val raw = response(IntArray(60)).also { it[1] = 0x02 }
        assertNull(UnaHourlyHrProtocol.parseResponse(raw))
    }

    @Test
    fun parseResponse_rejectsWrongOpcode() {
        // A daily-health response arriving on the same characteristic must not parse as HR.
        val raw = UnaFtsProtocolTest.hexToByteArray("1001380100000000000000000000430000004a000000")
        assertNull(UnaHourlyHrProtocol.parseResponse(raw))
    }

    @Test
    fun parseResponse_rejectsTruncatedPayload() {
        val raw = response(IntArray(60)).copyOfRange(0, 61)
        assertNull(UnaHourlyHrProtocol.parseResponse(raw))
    }

    /** Builds a well-formed `14 01 <60 x u8>` response carrying [minutes]. */
    private fun response(minutes: IntArray): ByteArray {
        val raw = ByteArray(2 + 60)
        raw[0] = 0x14
        raw[1] = 0x01
        for (i in minutes.indices) {
            raw[2 + i] = minutes[i].toByte()
        }
        return raw
    }
}
