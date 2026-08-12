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

import nodomain.freeyourgadget.gadgetbridge.devices.una.UnaConstants
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions

/**
 * One hour's worth of per-minute heart rates, as returned by the CCS hourly-HR command.
 *
 * [minutes] always has 60 entries indexed by minute of the hour. Zero means no reading, and zeros
 * appear between good readings rather than only at the end of the hour, so they must be dropped
 * individually. Adjacent entries often repeat in pairs, so 60 entries is not 60 independent
 * measurements.
 *
 * Zero is not the only non-measurement present: see [UnaHrRuns].
 */
data class UnaHourlyHr(val minutes: IntArray) {
    /** Readable minutes as (minute of hour, bpm), filtered by [UnaHrRuns]. */
    fun measuredMinutes(): List<Pair<Int, Int>> = UnaHrRuns.plausibleMinutes(minutes)

    // IntArray has identity equals/hashCode, which would silently break assertEquals.
    override fun equals(other: Any?): Boolean =
        this === other || (other is UnaHourlyHr && minutes.contentEquals(other.minutes))

    override fun hashCode(): Int = minutes.contentHashCode()
}

/**
 * Wire encoding for the CCS hourly heart rate command, the per-minute counterpart to
 * [UnaDailyHealthProtocol]'s whole-day aggregate. No BLE or Android dependencies, so it is
 * testable directly against captured bytes.
 */
object UnaHourlyHrProtocol {
    const val MINUTES_PER_HOUR: Int = 60

    private const val MINUTES_OFFSET = 2
    private const val RESPONSE_SIZE = MINUTES_OFFSET + MINUTES_PER_HOUR

    /**
     * `0x14 00 <year:u16LE> <month:u8> <day:u8> <hour:u8>`. Month is 1-based, hour 0..23, both
     * local wall-clock fields.
     */
    fun buildRequest(year: Int, month: Int, day: Int, hour: Int): ByteArray {
        return byteArrayOf(UnaConstants.CMD_HOURLY_HR.toByte(), 0) +
            BLETypeConversions.fromUint16(year) +
            byteArrayOf(month.toByte(), day.toByte(), hour.toByte())
    }

    /**
     * Parses `14 01 <60 x u8>`, one unsigned bpm per minute. Null if short, wrong opcode, or a
     * non-OK status.
     *
     * The status check is defensive only. The watch answers every request with `14 01`, including
     * a future date and an out-of-range `hour=25`, so an hour with no data arrives as an all-zero
     * payload rather than an error.
     */
    fun parseResponse(data: ByteArray): UnaHourlyHr? {
        if (data.size < RESPONSE_SIZE ||
            (data[0].toInt() and 0xFF) != UnaConstants.CMD_HOURLY_HR ||
            (data[1].toInt() and 0xFF) != UnaConstants.RESP_STATUS_OK
        ) return null
        val minutes = IntArray(MINUTES_PER_HOUR) { data[MINUTES_OFFSET + it].toInt() and 0xFF }
        return UnaHourlyHr(minutes)
    }
}
