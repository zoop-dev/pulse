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

import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

/**
 * One day's health record as the watch stores it on its own filesystem, at
 * `/DailyHealth/<YYYYMM>/dh_<YYYYMMDD>.json`.
 *
 * Richer than the CCS commands: one read covers a whole day rather than an hour per round trip,
 * about two weeks are retained, and floors are split into up and down where CCS reports one
 * combined figure. It is the same underlying data the CCS hourly command serves in slices.
 *
 * [hrPerMinute] is indexed by minute of the day, 1440 long once the day closes, with zero meaning
 * no reading. It is the only per-minute series the firmware stores; steps, floors and active
 * minutes exist only as daily totals. Zero is not the only non-measurement present, see
 * [UnaHrRuns].
 */
data class UnaDailyHealthFile(
    val steps: Int,
    val floorsUp: Int,
    val floorsDown: Int,
    val activeMinutes: Int,
    val restingHeartRate: Int,
    val averageHeartRate: Int,
    val hrPerMinute: IntArray,
) {
    /**
     * Readable minutes as (minute of day, bpm), filtered by [UnaHrRuns]. A whole day in one array
     * is the exact case for that filter, with no window boundary to hide a run behind.
     */
    fun measuredMinutes(): List<Pair<Int, Int>> = UnaHrRuns.plausibleMinutes(hrPerMinute)

    // IntArray has identity equals/hashCode, which would silently break assertEquals.
    override fun equals(other: Any?): Boolean =
        this === other || (other is UnaDailyHealthFile &&
            steps == other.steps && floorsUp == other.floorsUp && floorsDown == other.floorsDown &&
            activeMinutes == other.activeMinutes && restingHeartRate == other.restingHeartRate &&
            averageHeartRate == other.averageHeartRate &&
            hrPerMinute.contentEquals(other.hrPerMinute))

    override fun hashCode(): Int = 31 * steps + hrPerMinute.contentHashCode()

    companion object {
        const val MINUTES_PER_DAY: Int = 1440

        /** `/DailyHealth/<YYYYMM>/dh_<YYYYMMDD>.json`, from local calendar fields. */
        fun pathFor(day: Calendar): String {
            val year = day.get(Calendar.YEAR)
            val month = day.get(Calendar.MONTH) + 1
            val date = day.get(Calendar.DAY_OF_MONTH)
            return String.format(
                Locale.ROOT, "/DailyHealth/%04d%02d/dh_%04d%02d%02d.json",
                year, month, year, month, date,
            )
        }

        /**
         * Null if the bytes are not the expected JSON. Missing fields default to zero rather than
         * failing the whole day.
         *
         * An over-long `hrPerMinute` is truncated: indices become timestamps, so entries past
         * midnight would land on the following day. A short one is kept, since a day in progress
         * legitimately has fewer.
         */
        fun parse(bytes: ByteArray): UnaDailyHealthFile? {
            if (bytes.isEmpty()) return null
            return try {
                val json = JSONObject(String(bytes, Charsets.UTF_8))
                val array = json.optJSONArray("hrPerMinute")
                val count = minOf(array?.length() ?: 0, MINUTES_PER_DAY)
                val hr = IntArray(count) { array!!.optInt(it, 0) }
                UnaDailyHealthFile(
                    steps = json.optInt("dailySteps", 0),
                    floorsUp = json.optInt("dailyFloorsUp", 0),
                    floorsDown = json.optInt("dailyFloorsDown", 0),
                    activeMinutes = json.optInt("dailyActivityMinutes", 0),
                    restingHeartRate = json.optInt("restingHeartRate", 0),
                    averageHeartRate = json.optInt("averageHeartRate", 0),
                    hrPerMinute = hr,
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
