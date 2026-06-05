package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

// Guards the cursor-advance contract the orchestrator depends on: when temperature records are
// inserted, buildStatistics must carry the furthest-forward record timestamp, or the sync cursor
// freezes and re-inserts the whole span every run. Asserting on the returned SyncerStatistics keeps
// the test failing if the field is ever dropped from buildStatistics.
//
// Boundary: this covers buildStatistics, not sync()'s one-line delegation to it. Re-inlining sync()
// to build SyncerStatistics directly and bypass buildStatistics would reintroduce the bug without
// failing here; that wire is guarded by review, since exercising sync() needs static GBApplication/
// HealthConnectClient mocks this suite does not carry.
class TemperatureSyncerCursorTest {

    private val base: Instant = Instant.ofEpochSecond(1_700_000_000L)
    private val metadata: Metadata = Metadata.unknownRecordingMethod(
        Device(type = Device.TYPE_WATCH, manufacturer = "test", model = "test")
    )

    private fun body(at: Instant) = BodyTemperatureRecord(
        time = at,
        zoneOffset = ZoneOffset.UTC,
        temperature = Temperature.celsius(36.5),
        metadata = metadata
    )

    private fun skin(start: Instant, end: Instant) = SkinTemperatureRecord(
        startTime = start,
        startZoneOffset = ZoneOffset.UTC,
        endTime = end,
        endZoneOffset = ZoneOffset.UTC,
        deltas = listOf(SkinTemperatureRecord.Delta(start, TemperatureDelta.celsius(0.1))),
        baseline = Temperature.celsius(33.0),
        metadata = metadata
    )

    @Test
    fun noRecords_holdsCursor() {
        val stats = TemperatureSyncer.buildStatistics(emptyList(), recordsSkipped = 0)
        assertNull(stats.latestRecordTimestamp)
        assertEquals(0, stats.recordsSynced)
    }

    @Test
    fun bodyRecords_advanceToLatestTime() {
        val records: List<Record> = listOf(
            body(base.plusSeconds(60)),
            body(base.plusSeconds(3600)),
            body(base.plusSeconds(120))
        )
        val stats = TemperatureSyncer.buildStatistics(records, recordsSkipped = 0)
        assertEquals(base.plusSeconds(3600), stats.latestRecordTimestamp)
        assertEquals(3, stats.recordsSynced)
        assertEquals("Temperature", stats.recordType)
    }

    @Test
    fun skinRecord_advancesToEndTime() {
        val records: List<Record> = listOf(skin(base.plusSeconds(100), base.plusSeconds(900)))
        val stats = TemperatureSyncer.buildStatistics(records, recordsSkipped = 0)
        assertEquals(base.plusSeconds(900), stats.latestRecordTimestamp)
    }

    @Test
    fun mixedBodyAndSkin_advanceToFurthestForwardEdge() {
        val records: List<Record> = listOf(
            body(base.plusSeconds(500)),
            skin(base.plusSeconds(100), base.plusSeconds(7200))
        )
        val stats = TemperatureSyncer.buildStatistics(records, recordsSkipped = 0)
        assertEquals(base.plusSeconds(7200), stats.latestRecordTimestamp)
    }
}
