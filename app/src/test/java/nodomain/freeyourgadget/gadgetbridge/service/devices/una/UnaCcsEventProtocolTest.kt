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

import nodomain.freeyourgadget.gadgetbridge.service.devices.una.UnaFtsProtocolTest.Companion.hexToByteArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnaCcsEventProtocolTest {
    @Test
    fun parseActivitySaved_realCapturedTreadmillEvent() {
        // Captured from the watch after saving a Treadmill workout. The id is Treadmill's entry in
        // the watch's own /Apps/app_list.json.
        val event = UnaCcsEventProtocol.parseActivitySaved(hexToByteArray("010035608f2cb9e4d7a1"))
        assertEquals("A1D7E4B92C8F6035", event?.appIdHex)
    }

    @Test
    fun parseActivitySaved_realCapturedWalkEvent() {
        val event = UnaCcsEventProtocol.parseActivitySaved(hexToByteArray("0100824af0c9b7d3e5a1"))
        assertEquals("A1E5D3B7C9F04A82", event?.appIdHex)
    }

    @Test
    fun parseActivitySaved_ignoresTheFrameSentOnEverySubscribe() {
        // 0x04 arrives immediately after subscribing on every connection, so treating it as an
        // activity would sync on connect forever.
        assertNull(UnaCcsEventProtocol.parseActivitySaved(hexToByteArray("040000")))
    }

    @Test
    fun parseActivitySaved_rejectsShortFrame() {
        assertNull(UnaCcsEventProtocol.parseActivitySaved(hexToByteArray("010035608f2cb9e4")))
    }

    @Test
    fun parseActivitySaved_rejectsEmptyFrame() {
        assertNull(UnaCcsEventProtocol.parseActivitySaved(ByteArray(0)))
    }

    @Test
    fun parseActivitySaved_readsAnAppIdWithTheTopBitSet() {
        // App ids run past Long.MAX_VALUE, so the id is carried as a signed Long and only has to
        // survive being formatted back to the hex the watch uses.
        val event = UnaCcsEventProtocol.parseActivitySaved(hexToByteArray("0100ffffffffffffff"+"ff"))
        assertEquals("FFFFFFFFFFFFFFFF", event?.appIdHex)
    }
}
