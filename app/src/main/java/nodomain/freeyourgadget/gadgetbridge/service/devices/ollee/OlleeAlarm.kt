/*  Copyright (C) 2026 Ken Blizzard-Caron

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.ollee

object OlleeAlarm {

    /**
     * 33-byte 02 25 record: 13-byte settings block [enable, hourlyChime, snoozeEnable, hour, minute,
     * dayMask, chime, snoozeMin, playNow, hourMaskLo, hourMaskMid, hourMaskHi, 0xFF] + 20-byte trailer
     * (five LE u32 0x0000000C). Watch-only fields copied from [existing] (the 12-byte 0x4B read-back,
     * or null); [gbRepetitionMask] is GB's Alarm mask (MON=1..SUN=64, 0=once); playNow is always 0.
     */
    fun buildRecord(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        gbRepetitionMask: Int,
        snooze: Boolean,
        existing: ByteArray?
    ): ByteArray {
        require(hour in 0..23) { "hour must be 0..23 (got $hour)" }
        require(minute in 0..59) { "minute must be 0..59 (got $minute)" }

        val record = ByteArray(33)

        // [0] enable
        record[0] = if (enabled) 0x01 else 0x00

        // [1] hourlyChime — preserve from existing, default on
        record[1] = if (existing != null && existing.size > 1) {
            existing[1]
        } else {
            0x01 // default: hourly chime on
        }

        // [2] snoozeEnable
        record[2] = if (snooze) 0x01 else 0x00

        // [3] hour
        record[3] = hour.toByte()

        // [4] minute
        record[4] = minute.toByte()

        // [5] dayMask — convert from GB to watch layout
        record[5] = toWatchDayMask(gbRepetitionMask).toByte()

        // [6] chime — preserve from existing, default 0x00
        record[6] = if (existing != null && existing.size > 6) {
            existing[6]
        } else {
            0x00 // default: classic chime
        }

        // [7] snoozeMin — preserve from existing, default 5
        record[7] = if (existing != null && existing.size > 7) {
            existing[7]
        } else {
            0x05 // default: 5 minute snooze
        }

        // [8] playNow — always 0 from GB
        record[8] = 0x00

        // [9] hourMaskLo — preserve from existing, default 0xC0 (6:00-19:00 active-hours mask)
        record[9] = if (existing != null && existing.size > 8) {
            existing[8] // read-back byte 8 -> record byte 9
        } else {
            0xC0.toByte()
        }

        // [10] hourMaskMid — preserve from existing, default 0xFF
        record[10] = if (existing != null && existing.size > 9) {
            existing[9] // read-back byte 9 -> record byte 10
        } else {
            0xFF.toByte()
        }

        // [11] hourMaskHi — preserve from existing, default 0x0F
        record[11] = if (existing != null && existing.size > 10) {
            existing[10] // read-back byte 10 -> record byte 11
        } else {
            0x0F
        }

        // [12] terminator
        record[12] = 0xFF.toByte()

        // [13..32] trailing block — the official app writes 20 more bytes after the 13-byte
        // settings block, five little-endian u32 = 0x0000000C each. The watch ignores a 13-byte
        // (truncated) record; only the full 33-byte record is accepted. (RE 2026-07-12.)
        for (i in 0 until 5) {
            record[13 + i * 4] = 0x0C
            record[14 + i * 4] = 0x00
            record[15 + i * 4] = 0x00
            record[16 + i * 4] = 0x00
        }

        return record
    }

    /**
     * GB day-mask (MON=1..SUN=64) -> watch layout (bit1=Mon..bit7=Sun, 1=active, bit0 unused).
     * A single left shift: watch_mask = gb_mask << 1.
     */
    fun toWatchDayMask(gbRepetitionMask: Int): Int {
        return gbRepetitionMask shl 1
    }

    /**
     * Watch day-mask for a one-shot alarm (GB repetition 0). The watch has no single-fire mode
     * (mask 0x00 = never rings), so this arms the next-fire weekday — today if HH:MM is still ahead,
     * else tomorrow; repeats weekly until changed. [nowDayOfWeekIso] is ISO 1=Mon..7=Sun.
     */
    fun oneShotWatchDayMask(hour: Int, minute: Int, nowDayOfWeekIso: Int, nowHour: Int, nowMinute: Int): Int {
        require(nowDayOfWeekIso in 1..7) { "ISO day of week must be 1..7 (got $nowDayOfWeekIso)" }
        val firesToday = hour > nowHour || (hour == nowHour && minute > nowMinute)
        val fireDayIso = if (firesToday) nowDayOfWeekIso else (nowDayOfWeekIso % 7) + 1
        return 1 shl fireDayIso // watch layout: bit1=Mon..bit7=Sun
    }
}
