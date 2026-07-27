package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import nodomain.freeyourgadget.gadgetbridge.test.TestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies [XiaomiScooterRideHistory] against real captured `last_ride_N` strings.
 */
class XiaomiScooterRideHistoryTest : TestBase() {
    @Test
    fun testDecodeRecord_ride1() {
        val ride = XiaomiScooterRideHistory.decodeRecord("0020000300790200")
        assertEquals(2.0f, ride!!.durationMinutes, 0.01f)
        assertEquals(0.3f, ride.distanceKm, 0.01f)
        assertEquals(7.9f, ride.avgSpeedKmh, 0.01f)
        assertEquals(20.0f, ride.unknown4, 0.01f)
    }

    @Test
    fun testDecodeRecord_ride2() {
        val ride = XiaomiScooterRideHistory.decodeRecord("0020000300650040")
        assertEquals(2.0f, ride!!.durationMinutes, 0.01f)
        assertEquals(0.3f, ride.distanceKm, 0.01f)
        assertEquals(6.5f, ride.avgSpeedKmh, 0.01f)
        assertEquals(4.0f, ride.unknown4, 0.01f)
    }

    @Test
    fun testDecodeRecord_rejectsWrongLength() {
        assertNull(XiaomiScooterRideHistory.decodeRecord("00200003007902"))
    }

    @Test
    fun testDecodeRecord_rejectsNonDigits() {
        assertNull(XiaomiScooterRideHistory.decodeRecord("002000030079020x"))
    }

    @Test
    fun testSplitEntry_realCapture() {
        // One last_ride_N wire value actually packs two 16-digit ride records back to back.
        val rides = XiaomiScooterRideHistory.splitEntry("00200003007902000020000300650040")
        assertEquals(listOf("0020000300790200", "0020000300650040"), rides)
    }

    @Test
    fun testSplitEntry_rejectsWrongLength() {
        assertTrue(XiaomiScooterRideHistory.splitEntry("0020000300790200").isEmpty())
    }

    @Test
    fun testFlatten_multipleEntries() {
        val entries = listOf(
            "1111111111111111" + "2222222222222222",
            "3333333333333333" + "4444444444444444",
        )
        val flattened = XiaomiScooterRideHistory.flatten(entries)
        assertEquals(
            listOf(
                "1111111111111111",
                "2222222222222222",
                "3333333333333333",
                "4444444444444444",
            ),
            flattened
        )
    }

    @Test
    fun testNewRidesSince_firstSync() {
        val current = listOf("a", "b", "c")
        assertEquals(current, XiaomiScooterRideHistory.newRidesSince(emptyList(), current))
    }

    @Test
    fun testNewRidesSince_noNewRides() {
        val previouslySeen = listOf("a", "b", "c")
        val current = listOf("a", "b", "c")
        assertEquals(emptyList<String>(), XiaomiScooterRideHistory.newRidesSince(previouslySeen, current))
    }

    @Test
    fun testNewRidesSince_ridesShiftedTowardsOlderEnd() {
        // "a" was already imported; since then two more happened, so it now sits in the middle of
        // the window instead of at the end.
        val previouslySeen = listOf("a")
        val current = listOf("a", "d", "e")
        assertEquals(listOf("d", "e"), XiaomiScooterRideHistory.newRidesSince(previouslySeen, current))
    }

    @Test
    fun testNewRidesSince_allRidesFellOutOfWindow() {
        val previouslySeen = listOf("a", "b", "c")
        val current = listOf("d", "e", "f")
        assertEquals(current, XiaomiScooterRideHistory.newRidesSince(previouslySeen, current))
    }

    @Test
    fun testNewRidesSince_duplicateStatsCountedIndividually() {
        // Two rides already imported with identical stats ("a"); a third, genuinely new ride happens
        // to share the same stats as well. Only the extra occurrence should count as new.
        val previouslySeen = listOf("a", "a")
        val current = listOf("a", "a", "a")
        assertEquals(listOf("a"), XiaomiScooterRideHistory.newRidesSince(previouslySeen, current))
    }

    @Test
    fun testNewRidesSince_previouslySeenOrderDoesNotMatter() {
        val previouslySeen = listOf("c", "a", "b")
        val current = listOf("a", "b", "c", "d")
        assertEquals(listOf("d"), XiaomiScooterRideHistory.newRidesSince(previouslySeen, current))
    }
}
