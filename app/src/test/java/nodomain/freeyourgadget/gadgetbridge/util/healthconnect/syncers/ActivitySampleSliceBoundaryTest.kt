package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ActivitySampleSliceBoundaryTest {

    private val sliceStart: Instant = Instant.ofEpochSecond(1_700_000_000L)
    private val sliceEnd: Instant = sliceStart.plusSeconds(14 * 60)

    // Concrete syncer with the default 1h lookback (mirrors Steps/Calories/Distance).
    private object IdempotentSyncer : AbstractActivitySampleSyncer<StepsRecord>() {
        override val logger: Logger = LoggerFactory.getLogger("test")
        override val recordClass: KClass<StepsRecord> = StepsRecord::class
        override fun convertSample(sample: ActivitySample, offset: ZoneOffset, metadata: Metadata, deviceName: String, version: Long): StepsRecord? = null
    }

    // Syncer that opts out of the lookback (mirrors a non-idempotent record type).
    private object StrictSyncer : AbstractActivitySampleSyncer<StepsRecord>() {
        override val logger: Logger = LoggerFactory.getLogger("test")
        override val recordClass: KClass<StepsRecord> = StepsRecord::class
        override val lateSampleLookback = java.time.Duration.ZERO
        override fun convertSample(sample: ActivitySample, offset: ZoneOffset, metadata: Metadata, deviceName: String, version: Long): StepsRecord? = null
    }

    private fun within(syncer: AbstractActivitySampleSyncer<*>, endTs: Instant): Boolean =
        syncer.isWithinSlice(endTs, endTs.minusSeconds(60), sliceStart, sliceEnd)

    @Test
    fun sampleInsideSlice_emitted() {
        assertTrue(within(IdempotentSyncer, sliceStart.plusSeconds(300)))
    }

    @Test
    fun lateSampleWithinLookback_recoveredByIdempotentSyncer() {
        // 30 min before the slice start: clipped without lookback, recovered with it.
        val lateTs = sliceStart.minusSeconds(30 * 60)
        assertTrue(within(IdempotentSyncer, lateTs))
    }

    @Test
    fun lateSampleWithinLookback_clippedByStrictSyncer() {
        val lateTs = sliceStart.minusSeconds(30 * 60)
        assertFalse(within(StrictSyncer, lateTs))
    }

    @Test
    fun sampleOlderThanLookback_clipped() {
        // 90 min before start is beyond the 1h window.
        val tooOld = sliceStart.minusSeconds(90 * 60)
        assertFalse(within(IdempotentSyncer, tooOld))
    }

    @Test
    fun sampleAfterSliceEnd_clippedRegardlessOfLookback() {
        // Upper bound is never relaxed: a sample whose interval starts after slice end is dropped.
        val afterEnd = sliceEnd.plusSeconds(120)
        assertFalse(within(IdempotentSyncer, afterEnd))
    }

    @Test
    fun sampleStraddlingSliceEnd_emitted() {
        // endTs just past sliceEnd but startTs (endTs-60s) still within: kept.
        val straddle = sliceEnd.plusSeconds(30)
        assertTrue(within(IdempotentSyncer, straddle))
    }

    // The clientRecordVersion must carry the supplied version verbatim (the sync run's wall-clock),
    // never the metric value: HC keeps the highest version on a clientRecordId collision, so a value
    // revised downward would be silently ignored if the value doubled as the version.
    @Test
    fun clientRecordVersion_carriesSuppliedVersion_notMetricValue() {
        val base = Metadata.autoRecorded(Device(Device.TYPE_WATCH, "Acme", "Band"))
        val version = 1_700_000_500_000L
        val meta = clientRecordMetadata(base, "steps", endEpoch, version)
        assertEquals(version, meta.clientRecordVersion)
    }

    // The dedup key (clientRecordId) is fixed by type + device + timestamp and must not vary with the
    // version, so a re-emit of the same minute collides and upserts instead of duplicating.
    @Test
    fun clientRecordId_independentOfVersion() {
        val base = Metadata.autoRecorded(Device(Device.TYPE_WATCH, "Acme", "Band"))
        val low = clientRecordMetadata(base, "steps", endEpoch, 1L)
        val high = clientRecordMetadata(base, "steps", endEpoch, 999L)
        assertEquals(low.clientRecordId, high.clientRecordId)
    }

    private val endEpoch = sliceStart.epochSecond
}
