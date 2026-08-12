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
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

class UnaDailyHealthFileTest {
    @Test
    fun pathFor_matchesTheWatchesOwnNaming() {
        // Matches the naming the watch uses.
        assertEquals(
            "/DailyHealth/202608/dh_20260810.json",
            UnaDailyHealthFile.pathFor(day(2026, Calendar.AUGUST, 10)),
        )
    }

    @Test
    fun pathFor_zeroPadsSingleDigitMonths() {
        assertEquals(
            "/DailyHealth/202601/dh_20260105.json",
            UnaDailyHealthFile.pathFor(day(2026, Calendar.JANUARY, 5)),
        )
    }

    @Test
    fun parse_realRecordShape() {
        // Field names and value shape of a real record.
        val file = UnaDailyHealthFile.parse(
            record(steps = 1666, up = 3, down = 2, active = 0, resting = 67, average = 77)
        )
        assertEquals(1666, file?.steps)
        assertEquals(3, file?.floorsUp)
        assertEquals(2, file?.floorsDown)
        assertEquals(0, file?.activeMinutes)
        assertEquals(67, file?.restingHeartRate)
        assertEquals(77, file?.averageHeartRate)
        assertEquals(UnaDailyHealthFile.MINUTES_PER_DAY, file?.hrPerMinute?.size)
    }

    @Test
    fun measuredMinutes_indexesByMinuteOfDay() {
        // Minute 720 is 12:00. Real values, byte-identical to what the CCS hourly command
        // returns for the same hour.
        val hr = IntArray(UnaDailyHealthFile.MINUTES_PER_DAY)
        hr[720] = 72
        hr[721] = 92
        hr[722] = 0
        hr[723] = 108
        val file = UnaDailyHealthFile.parse(record(hrPerMinute = hr))!!
        val noon = file.measuredMinutes().filter { it.first in 720..723 }
        assertEquals(listOf(720 to 72, 721 to 92, 723 to 108), noon)
    }

    @Test
    fun measuredMinutes_dropsTheDaysUnmeasuredMinutes() {
        val hr = IntArray(UnaDailyHealthFile.MINUTES_PER_DAY)
        hr[0] = 60
        hr[1439] = 58
        val file = UnaDailyHealthFile.parse(record(hrPerMinute = hr))!!
        assertEquals(listOf(0 to 60, 1439 to 58), file.measuredMinutes())
    }

    @Test
    fun parse_keepsAShortArrayForADayStillInProgress() {
        // Deliberately varying: a constant 600 minutes would be discarded as an off-wrist stuck
        // run (see UnaHrRuns), which would test the filter rather than the array handling.
        val file = UnaDailyHealthFile.parse(record(hrPerMinute = IntArray(600) { 60 + it % 17 }))
        assertEquals(600, file?.hrPerMinute?.size)
        assertEquals(600, file?.measuredMinutes()?.size)
    }

    @Test
    fun parse_truncatesAnOverlongArrayToOneDay() {
        // Indices are turned straight into timestamps, so anything past 1440 would silently land
        // on the following day.
        val file = UnaDailyHealthFile.parse(record(hrPerMinute = IntArray(1500) { 60 }))
        assertEquals(UnaDailyHealthFile.MINUTES_PER_DAY, file?.hrPerMinute?.size)
    }

    @Test
    fun parse_toleratesMissingFields() {
        val file = UnaDailyHealthFile.parse("""{"dailySteps":42}""".toByteArray())
        assertEquals(42, file?.steps)
        assertEquals(0, file?.averageHeartRate)
        assertEquals(0, file?.hrPerMinute?.size)
    }

    @Test
    fun parse_rejectsEmptyAndNonJsonInput() {
        assertNull(UnaDailyHealthFile.parse(ByteArray(0)))
        assertNull(UnaDailyHealthFile.parse("not json at all".toByteArray()))
        // A truncated read must not half-parse into a plausible-looking day.
        assertNull(UnaDailyHealthFile.parse("""{"dailySteps":1666,"hrPerMin""".toByteArray()))
    }

    private fun day(year: Int, month: Int, dayOfMonth: Int): Calendar =
        GregorianCalendar(year, month, dayOfMonth)

    private fun record(
        steps: Int = 0,
        up: Int = 0,
        down: Int = 0,
        active: Int = 0,
        resting: Int = 0,
        average: Int = 0,
        hrPerMinute: IntArray = IntArray(UnaDailyHealthFile.MINUTES_PER_DAY),
    ): ByteArray = ("""
        {"dailySteps":$steps,"dailyFloorsUp":$up,"dailyFloorsDown":$down,
         "dailyActivityMinutes":$active,"restingHeartRate":$resting,
         "averageHeartRate":$average,"hrPerMinute":[${hrPerMinute.joinToString(",")}]}
    """).toByteArray()
}
