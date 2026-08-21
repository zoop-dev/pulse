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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnaReadWindowTest {
    private val windowSize = 4096

    /** Payload bytes are derived from [offset] so reassembly is checked by content, not length. */
    private fun chunk(offset: Int, size: Int, total: Int, shortDelivery: Boolean = false) =
        UnaFtsReadChunk(
            offset = offset,
            total = total,
            payload = ByteArray(size) { (offset / 200 + 1).toByte() },
            deliveredLessThanAdvertised = shortDelivery,
        )

    @Test
    fun aBurstIsAccumulatedAndOnlyTheChunkFillingTheWindowAsksForAnother() {
        val window = UnaReadWindow(windowSize)
        val total = 10_000
        for (i in 0 until 20) {
            assertEquals(
                "notification $i should not have asked for another window",
                UnaReadWindow.Next.WAIT,
                window.accept(chunk(i * 200, 200, total)),
            )
        }
        assertEquals(UnaReadWindow.Next.REQUEST_NEXT_WINDOW, window.accept(chunk(4000, 96, total)))
        assertEquals(4096, window.firstByteNotHeld)
    }

    @Test
    fun aShortDeliveryAsksForTheNextWindowImmediately() {
        val window = UnaReadWindow(windowSize)
        assertEquals(
            UnaReadWindow.Next.REQUEST_NEXT_WINDOW,
            window.accept(chunk(0, 201, total = 10_000, shortDelivery = true)),
        )
        assertEquals(201, window.firstByteNotHeld)
        assertEquals(
            UnaReadWindow.Next.REQUEST_NEXT_WINDOW,
            window.accept(chunk(201, 201, total = 10_000, shortDelivery = true)),
        )
        assertEquals(402, window.firstByteNotHeld)
    }

    @Test
    fun completesWhenTheFileEndsInsideAWindow() {
        val window = UnaReadWindow(windowSize)
        assertEquals(UnaReadWindow.Next.WAIT, window.accept(chunk(0, 200, total = 350)))
        assertEquals(UnaReadWindow.Next.COMPLETE, window.accept(chunk(200, 150, total = 350)))
        assertEquals(350, window.assemble().size)
    }

    @Test
    fun assemblesTheFileInOffsetOrder() {
        val window = UnaReadWindow(windowSize)
        window.accept(chunk(0, 200, total = 400))
        window.accept(chunk(200, 200, total = 400))
        val expected = ByteArray(200) { 1 } + ByteArray(200) { 2 }
        assertArrayEquals(expected, window.assemble())
    }

    @Test
    fun aMissingNotificationHoldsFirstByteNotHeldBackSoItIsRequestedAgain() {
        val window = UnaReadWindow(windowSize)
        val total = 10_000
        window.accept(chunk(0, 200, total))
        for (i in 2 until 20) {
            window.accept(chunk(i * 200, 200, total))
        }
        assertEquals(UnaReadWindow.Next.WAIT, window.accept(chunk(4000, 96, total)))
        assertEquals(200, window.firstByteNotHeld)
    }

    @Test
    fun aRefetchedNotificationLetsTheReadCarryOn() {
        val window = UnaReadWindow(windowSize)
        val total = 600
        window.accept(chunk(0, 200, total))
        window.accept(chunk(400, 200, total))
        assertEquals(200, window.firstByteNotHeld)
        assertEquals(UnaReadWindow.Next.COMPLETE, window.accept(chunk(200, 200, total)))
        assertEquals(600, window.firstByteNotHeld)
        assertArrayEquals(
            ByteArray(200) { 1 } + ByteArray(200) { 2 } + ByteArray(200) { 3 },
            window.assemble(),
        )
    }

    @Test
    fun trimsAWindowThatOverrunsTheEndOfTheFile() {
        val window = UnaReadWindow(windowSize)
        assertEquals(UnaReadWindow.Next.COMPLETE, window.accept(chunk(0, 200, total = 150)))
        assertEquals(150, window.assemble().size)
    }

    @Test
    fun countsNotificationsAgainstRequestsAndFlagsShortDelivery() {
        val burst = UnaReadWindow(windowSize)
        val total = 10_000
        for (i in 0 until 20) burst.accept(chunk(i * 200, 200, total))
        burst.accept(chunk(4000, 96, total))
        assertEquals(21, burst.notifications)
        assertEquals(2, burst.requests)
        assertFalse(burst.sawShortDelivery)

        val perNotification = UnaReadWindow(windowSize)
        perNotification.accept(chunk(0, 201, total, shortDelivery = true))
        assertEquals(1, perNotification.notifications)
        assertEquals(2, perNotification.requests)
        assertTrue(perNotification.sawShortDelivery)
    }

    @Test
    fun aDuplicateNotificationDoesNotAdvanceTwice() {
        val window = UnaReadWindow(windowSize)
        val total = 10_000
        window.accept(chunk(0, 200, total))
        assertEquals(UnaReadWindow.Next.WAIT, window.accept(chunk(0, 200, total)))
        assertEquals(200, window.firstByteNotHeld)
    }
}
