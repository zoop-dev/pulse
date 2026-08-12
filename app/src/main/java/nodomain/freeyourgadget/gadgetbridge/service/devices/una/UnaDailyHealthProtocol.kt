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

/** One day's aggregate from a CCS DailyHealth response. */
data class UnaDailyHealth(
    val steps: Int,
    val floors: Int,
    val activeMinutes: Int,
    val restingHeartRate: Int,
    val averageHeartRate: Int,
)

/**
 * Wire encoding for the CCS daily health command. No BLE or Android dependencies, so it is
 * testable directly against captured bytes.
 */
object UnaDailyHealthProtocol {
    // Five consecutive u32LE fields starting right after the 2-byte opcode/marker header, each
    // FIELD_SIZE past the last, expressed relative to each other so the layout cannot drift if a
    // field is inserted or resized.
    private const val FIELD_SIZE = 4
    private const val STEPS_OFFSET = 2
    private const val FLOORS_OFFSET = STEPS_OFFSET + FIELD_SIZE
    private const val ACTIVE_MINUTES_OFFSET = FLOORS_OFFSET + FIELD_SIZE
    private const val RESTING_HR_OFFSET = ACTIVE_MINUTES_OFFSET + FIELD_SIZE
    private const val AVERAGE_HR_OFFSET = RESTING_HR_OFFSET + FIELD_SIZE
    private const val RESPONSE_SIZE = AVERAGE_HR_OFFSET + FIELD_SIZE

    /** 0x10 00 <year:u16LE> <month:u8> <day:u8>. */
    fun buildRequest(year: Int, month: Int, day: Int): ByteArray {
        return byteArrayOf(UnaConstants.CMD_DAILY_HEALTH.toByte(), 0) +
            BLETypeConversions.fromUint16(year) +
            byteArrayOf(month.toByte(), day.toByte())
    }

    /**
     * Parses `10 01 <5 x u32LE>`. The opcode is echoed from the request, with status in byte 1.
     * Null if short, wrong opcode, or a non-OK status.
     */
    fun parseResponse(data: ByteArray): UnaDailyHealth? {
        if (data.size < RESPONSE_SIZE ||
            (data[0].toInt() and 0xFF) != UnaConstants.CMD_DAILY_HEALTH ||
            (data[1].toInt() and 0xFF) != UnaConstants.RESP_STATUS_OK
        ) return null
        return UnaDailyHealth(
            steps = BLETypeConversions.toUint32(data, STEPS_OFFSET),
            floors = BLETypeConversions.toUint32(data, FLOORS_OFFSET),
            activeMinutes = BLETypeConversions.toUint32(data, ACTIVE_MINUTES_OFFSET),
            restingHeartRate = BLETypeConversions.toUint32(data, RESTING_HR_OFFSET),
            averageHeartRate = BLETypeConversions.toUint32(data, AVERAGE_HR_OFFSET),
        )
    }
}
