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

/**
 * Drops heart rates the watch reports while it is off the wrist.
 *
 * Instead of the zero it uses for other no-data cases, the firmware holds one identical value for
 * hours. It affects both per-minute sources and the watch's own FIT recordings, so it originates
 * below this interface: https://github.com/UNAWatch/una-sdk/issues/282. If the firmware starts
 * emitting zero, this file can go.
 *
 * The held value cannot be recognised on its own. It differs every occurrence and settles within a
 * couple of bpm of the last real reading, so only duration identifies it.
 */
internal object UnaHrRuns {
    /**
     * Chosen on cost of error rather than any boundary in the data, because keeping a fabricated
     * heart rate is worse than dropping a real one: a gap is visibly missing, a stuck value is
     * silently false and drags the daily min, max and average with it.
     *
     * The artifact reaches down into durations a real heart rate can also produce, so no threshold
     * separates them cleanly. Short runs must survive: the watch's output repeats in adjacent
     * pairs, giving it an effective resolution of about two minutes.
     */
    const val MIN_STUCK_RUN_MINUTES: Int = 8

    /**
     * Keeps the (index, bpm) pairs worth storing from [minutes], indexed by minute of the hour for
     * the CCS command or of the day for a daily record.
     *
     * Zeros are dropped and also break a run, so two short flat stretches either side of a gap stay
     * two short runs.
     *
     * The caller's window bounds what is detectable. A whole day is exact; within a single hour a
     * run straddling the boundary is only partly visible, so a stuck stretch starting near the end
     * of an hour can survive. Today's data comes an hour at a time from CCS and is therefore less
     * thoroughly cleaned than a past day, which self-corrects when the day's file overwrites those
     * timestamps.
     */
    fun plausibleMinutes(minutes: IntArray): List<Pair<Int, Int>> {
        val kept = mutableListOf<Pair<Int, Int>>()
        var runStart = 0
        while (runStart < minutes.size) {
            val value = minutes[runStart]
            var runEnd = runStart
            while (runEnd + 1 < minutes.size && minutes[runEnd + 1] == value) {
                runEnd++
            }
            if (value > 0 && runEnd - runStart + 1 < MIN_STUCK_RUN_MINUTES) {
                for (i in runStart..runEnd) {
                    kept.add(i to value)
                }
            }
            runStart = runEnd + 1
        }
        return kept
    }
}
