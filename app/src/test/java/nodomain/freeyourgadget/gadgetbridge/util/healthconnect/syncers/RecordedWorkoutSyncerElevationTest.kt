package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordedWorkoutSyncerElevationTest {

    private fun pt(altitude: Double? = null): ActivityPoint {
        val p = ActivityPoint()
        if (altitude != null) {
            p.altitude = altitude
        }
        return p
    }

    @Test
    fun empty_isZero() {
        assertEquals(0.0, RecordedWorkoutSyncer.cumulativeElevationGain(emptyList()), 0.0)
    }

    @Test
    fun single_isZero() {
        assertEquals(0.0, RecordedWorkoutSyncer.cumulativeElevationGain(listOf(pt(100.0))), 0.0)
    }

    @Test
    fun monotonicIncrease_sumsDeltas() {
        assertEquals(
            30.0,
            RecordedWorkoutSyncer.cumulativeElevationGain(listOf(pt(100.0), pt(110.0), pt(130.0))),
            1e-6
        )
    }

    @Test
    fun upsAndDowns_onlyPositiveDeltasCounted() {
        assertEquals(
            25.0,
            RecordedWorkoutSyncer.cumulativeElevationGain(listOf(pt(100.0), pt(110.0), pt(105.0), pt(120.0))),
            1e-6
        )
    }

    @Test
    fun unknownAltitude_skippedWithoutBreakingComparison() {
        // The middle point has no altitude; 100 -> 130 across the gap counts once.
        assertEquals(
            30.0,
            RecordedWorkoutSyncer.cumulativeElevationGain(listOf(pt(100.0), pt(), pt(130.0))),
            1e-6
        )
    }

    @Test
    fun allUnknown_isZero() {
        assertEquals(0.0, RecordedWorkoutSyncer.cumulativeElevationGain(listOf(pt(), pt(), pt())), 0.0)
    }
}
