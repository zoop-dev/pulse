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

class UnaDailyHealthProtocolTest {
    @Test
    fun buildRequest_realCapturedRequest() {
        // Real captured request.
        val expected = UnaFtsProtocolTest.hexToByteArray("1000ea070802")
        assertArrayEquals(expected, UnaDailyHealthProtocol.buildRequest(year = 2026, month = 8, day = 2))
    }

    @Test
    fun parseResponse_realCapturedResponse_today() {
        // Real captured 0x11 response for the request above: steps=312, floors=0, act=0, RHR=67, AHR=74.
        val raw = UnaFtsProtocolTest.hexToByteArray("1001380100000000000000000000430000004a000000")
        val health = UnaDailyHealthProtocol.parseResponse(raw)
        assertEquals(312, health?.steps)
        assertEquals(0, health?.floors)
        assertEquals(0, health?.activeMinutes)
        assertEquals(67, health?.restingHeartRate)
        assertEquals(74, health?.averageHeartRate)
    }

    @Test
    fun parseResponse_realCapturedResponse_yesterday() {
        // Real captured 0x11 response: steps=2072, floors=1, act=4, RHR=70, AHR=80.
        val raw = UnaFtsProtocolTest.hexToByteArray("10011808000001000000040000004600000050000000")
        val health = UnaDailyHealthProtocol.parseResponse(raw)
        assertEquals(2072, health?.steps)
        assertEquals(1, health?.floors)
        assertEquals(4, health?.activeMinutes)
        assertEquals(70, health?.restingHeartRate)
        assertEquals(80, health?.averageHeartRate)
    }

    @Test
    fun parseResponse_rejectsWrongOpcode() {
        val raw = ByteArray(22)
        assertNull(UnaDailyHealthProtocol.parseResponse(raw))
    }

    @Test
    fun parseResponse_rejectsTruncatedPayload() {
        assertNull(UnaDailyHealthProtocol.parseResponse(UnaFtsProtocolTest.hexToByteArray("1001")))
    }
}
