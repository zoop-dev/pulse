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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnaHrRunsTest {
    private val threshold = UnaHrRuns.MIN_STUCK_RUN_MINUTES

    @Test
    fun keepsOrdinaryVaryingData() {
        val minutes = intArrayOf(62, 62, 60, 60, 65, 71, 70, 68)
        assertEquals(
            listOf(0 to 62, 1 to 62, 2 to 60, 3 to 60, 4 to 65, 5 to 71, 6 to 70, 7 to 68),
            UnaHrRuns.plausibleMinutes(minutes),
        )
    }

    @Test
    fun dropsZerosAsNoData() {
        val minutes = intArrayOf(0, 72, 0, 74, 0)
        assertEquals(listOf(1 to 72, 3 to 74), UnaHrRuns.plausibleMinutes(minutes))
    }

    @Test
    fun keepsARunOneShortOfTheThreshold() {
        val minutes = IntArray(threshold - 1) { 63 }
        assertEquals(threshold - 1, UnaHrRuns.plausibleMinutes(minutes).size)
    }

    @Test
    fun dropsARunExactlyAtTheThreshold() {
        val minutes = IntArray(threshold) { 63 }
        assertEquals(emptyList<Pair<Int, Int>>(), UnaHrRuns.plausibleMinutes(minutes))
    }

    @Test
    fun dropsOnlyTheStuckRunAndKeepsWhatSurroundsIt() {
        // Real shape: normal readings, the watch comes off and holds one value, then normal again.
        val minutes = intArrayOf(65, 66) + IntArray(threshold + 10) { 63 } + intArrayOf(70, 71)
        val kept = UnaHrRuns.plausibleMinutes(minutes)
        assertEquals(listOf(0 to 65, 1 to 66, minutes.size - 2 to 70, minutes.size - 1 to 71), kept)
    }

    @Test
    fun aZeroBreaksARunSoNeitherHalfIsLongEnoughToDrop() {
        // Two sub-threshold stretches of the same value either side of a gap are two runs, not
        // one long one, even though together they exceed the threshold.
        val half = threshold - 1
        val minutes = IntArray(half) { 62 } + intArrayOf(0) + IntArray(half) { 62 }
        val kept = UnaHrRuns.plausibleMinutes(minutes)
        assertEquals(half * 2, kept.size)
        assertTrue(kept.none { it.first == half })
    }

    @Test
    fun dropsAnEntirelyStuckHour() {
        // The common CCS case: a whole 60-minute window of one value while off-wrist.
        val minutes = IntArray(60) { 95 }
        assertEquals(emptyList<Pair<Int, Int>>(), UnaHrRuns.plausibleMinutes(minutes))
    }

    @Test
    fun realCapturedDay_dropsTheElevenHourStuckRunAndKeepsTheRest() {
        // A long off-wrist stretch inside a day that otherwise has ordinary readings.
        val day = IntArray(UnaDailyHealthFile.MINUTES_PER_DAY)
        for (i in 0 until 640) day[i] = 60 + (i % 17)          // ordinary variation
        for (i in 640 until 640 + 679) day[i] = 63              // the stuck stretch
        for (i in 640 + 679 until day.size) day[i] = 70 + (i % 11)
        val kept = UnaHrRuns.plausibleMinutes(day)
        assertTrue(kept.none { it.first in 640 until 640 + 679 })
        assertEquals(day.size - 679, kept.size)
    }

    @Test
    fun aGenuineFlatRestingStretchUnderTheThresholdSurvives() {
        // The cost of being wrong in the other direction: a real, very steady rest period must
        // not be discarded just for being flat.
        val minutes = IntArray(threshold - 1) { 58 }
        assertEquals(threshold - 1, UnaHrRuns.plausibleMinutes(minutes).size)
    }

    @Test
    fun handlesEmptyAndSingleEntryInput() {
        assertEquals(emptyList<Pair<Int, Int>>(), UnaHrRuns.plausibleMinutes(IntArray(0)))
        assertEquals(listOf(0 to 70), UnaHrRuns.plausibleMinutes(intArrayOf(70)))
        assertEquals(emptyList<Pair<Int, Int>>(), UnaHrRuns.plausibleMinutes(intArrayOf(0)))
    }
}
