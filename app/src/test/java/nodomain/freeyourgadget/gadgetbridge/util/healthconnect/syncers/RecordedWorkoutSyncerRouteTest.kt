package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.Date

class RecordedWorkoutSyncerRouteTest {

    private val start: Instant = Instant.parse("2026-05-26T21:10:31Z")
    private val end: Instant = Instant.parse("2026-05-26T21:19:36Z")
    private val device = "test-device"

    private fun point(secondsFromStart: Long, lat: Double = 52.5, lng: Double = 13.4): ActivityPoint {
        val p = ActivityPoint(Date.from(start.plusSeconds(secondsFromStart)))
        p.location = GPSCoordinate(lng, lat, GPSCoordinate.UNKNOWN_ALTITUDE)
        return p
    }

    @Test
    fun emptyInput_returnsNull() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(emptyList(), start, end, device)
        assertNull(route)
    }

    @Test
    fun singlePoint_returnsNull() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(listOf(point(0)), start, end, device)
        assertNull(route)
    }

    @Test
    fun twoUniqueTimestamps_returnsRoute() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(point(0), point(1)), start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }

    @Test
    fun duplicateTimestamps_areDeduped() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(point(0), point(0, lat = 52.6), point(1)), start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }

    @Test
    fun allSameTimestamp_returnsNull() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(point(5), point(5), point(5)), start, end, device
        )
        assertNull(route)
    }

    @Test
    fun reverseOrderTimestamps_routeBuilt() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(point(10), point(5), point(0)), start, end, device
        )
        assertNotNull(route)
        assertEquals(3, route!!.route.size)
    }

    @Test
    fun outOfWindow_dropped() {
        val before = ActivityPoint(Date.from(start.minusSeconds(5))).apply {
            location = GPSCoordinate(13.4, 52.5, GPSCoordinate.UNKNOWN_ALTITUDE)
        }
        val after = ActivityPoint(Date.from(end.plusSeconds(5))).apply {
            location = GPSCoordinate(13.4, 52.5, GPSCoordinate.UNKNOWN_ALTITUDE)
        }
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(before, point(1), point(2), after), start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }

    @Test
    fun nanLatitude_dropped() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(point(0, lat = Double.NaN), point(1), point(2)),
            start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }

    @Test
    fun outOfRangeLatitude_dropped() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(point(0, lat = 95.0), point(1), point(2)),
            start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }

    @Test
    fun outOfRangeLongitude_dropped() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(point(0, lng = 200.0), point(1), point(2)),
            start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }

    @Test
    fun infiniteLongitude_dropped() {
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(point(0, lng = Double.POSITIVE_INFINITY), point(1), point(2)),
            start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }

    @Test
    fun pointWithNullLocation_dropped() {
        val noLoc = ActivityPoint(Date.from(start.plusSeconds(0)))
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(noLoc, point(1), point(2)), start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }

    @Test
    fun pointWithNullTime_dropped() {
        val noTime = ActivityPoint().apply {
            location = GPSCoordinate(13.4, 52.5, GPSCoordinate.UNKNOWN_ALTITUDE)
        }
        val route = RecordedWorkoutSyncer.buildSanitisedRoute(
            listOf(noTime, point(1), point(2)), start, end, device
        )
        assertNotNull(route)
        assertEquals(2, route!!.route.size)
    }
}
