package nodomain.freeyourgadget.gadgetbridge.util.healthconnect

import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class OversizedRouteTest {

    private val start: Instant = Instant.parse("2026-05-26T08:00:00Z")

    private fun route(points: Int): ExerciseRoute {
        val locations = (0 until points).map { i ->
            ExerciseRoute.Location(
                time = start.plusSeconds(i.toLong()),
                latitude = 52.5,
                longitude = 13.4
            )
        }
        return ExerciseRoute(locations)
    }

    private fun session(points: Int): ExerciseSessionRecord {
        return ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = start.plusSeconds(points.toLong()),
            endZoneOffset = ZoneOffset.UTC,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            title = "ride",
            exerciseRoute = route(points),
            metadata = Metadata.autoRecorded(Device(type = Device.TYPE_WATCH, manufacturer = "test", model = "test"))
        )
    }

    private fun sizeError(limit: Long, was: Long) = IllegalStateException(
        "android.health.connect.HealthConnectException: Record size exceeded the " +
            "single record size limit: $limit, was: $was"
    )

    private fun routeOf(record: Record): List<ExerciseRoute.Location> {
        val session = record as ExerciseSessionRecord
        return (session.exerciseRouteResult as ExerciseRouteResult.Data).exerciseRoute.route
    }

    @Test
    fun oversizedRoute_isDecimatedByRatio() {
        val original = 10000
        val records = listOf(session(original))
        val shrunk = HealthConnectUtils.shrinkOversizedRoute(records, sizeError(1_000_000, 1_700_644))
        assertNotNull(shrunk)
        val newSize = routeOf(shrunk!![0]).size
        // Target is (1000000/1700644)*0.9 ~= 0.529 of the points.
        assertEquals((original * (1_000_000.0 / 1_700_644.0) * 0.9).toInt(), newSize)
        assertTrue(newSize < original)
    }

    @Test
    fun decimatedRoute_preservesEndpointsAndUniqueTimestamps() {
        val records = listOf(session(10000))
        val shrunk = HealthConnectUtils.shrinkOversizedRoute(records, sizeError(1_000_000, 1_700_644))!!
        val pts = routeOf(shrunk[0])
        assertEquals(start, pts.first().time)
        assertEquals(start.plusSeconds(9999), pts.last().time)
        assertEquals(pts.size, pts.map { it.time }.toSet().size) // no duplicate timestamps
    }

    @Test
    fun unrelatedException_returnsNull() {
        val records = listOf(session(10000))
        assertNull(HealthConnectUtils.shrinkOversizedRoute(records, IllegalStateException("permission denied")))
    }

    @Test
    fun nonExerciseRecords_returnNull() {
        val steps = StepsRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = start.plusSeconds(60),
            endZoneOffset = ZoneOffset.UTC,
            count = 100,
            metadata = Metadata.autoRecorded(Device(type = Device.TYPE_WATCH, manufacturer = "test", model = "test"))
        )
        assertNull(HealthConnectUtils.shrinkOversizedRoute(listOf(steps), sizeError(1_000_000, 1_700_644)))
    }

    @Test
    fun sizeWithinLimit_returnsNull() {
        val records = listOf(session(10000))
        assertNull(HealthConnectUtils.shrinkOversizedRoute(records, sizeError(1_000_000, 900_000)))
    }
}
