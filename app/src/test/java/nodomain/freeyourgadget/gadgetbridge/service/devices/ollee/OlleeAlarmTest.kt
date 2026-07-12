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

import nodomain.freeyourgadget.gadgetbridge.model.Alarm
import org.junit.Test
import org.junit.Assert.*

class OlleeAlarmTest {

    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /**
     * (a) GB daily mask 127 (bits 0-6) maps to the watch's every-day mask 0xFE (bits 1-7),
     * matching FreeOllee's daily-alarm vector.
     */
    @Test
    fun buildRecordWithGbAlarmDailyProducesWatchMask0xFE() {
        val record = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 13,
            minute = 30,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = false,
            existing = null
        )
        assertEquals(33, record.size)
        assertEquals(0xFE, record[5].toInt() and 0xFF) // dayMask at index 5
    }

    /** toWatchDayMask maps GB MON=1..SUN=64 to the watch's bit1=Mon..bit7=Sun (1=active, bit0 unused). */
    @Test
    fun toWatchDayMaskMapsAllIndividualGbDaysCorrectly() {
        assertEquals(0x02, OlleeAlarm.toWatchDayMask(Alarm.ALARM_MON.toInt()))   // MON=1 -> bit1
        assertEquals(0x04, OlleeAlarm.toWatchDayMask(Alarm.ALARM_TUE.toInt()))   // TUE=2 -> bit2
        assertEquals(0x08, OlleeAlarm.toWatchDayMask(Alarm.ALARM_WED.toInt()))   // WED=4 -> bit3
        assertEquals(0x10, OlleeAlarm.toWatchDayMask(Alarm.ALARM_THU.toInt()))   // THU=8 -> bit4
        assertEquals(0x20, OlleeAlarm.toWatchDayMask(Alarm.ALARM_FRI.toInt()))   // FRI=16 -> bit5
        assertEquals(0x40, OlleeAlarm.toWatchDayMask(Alarm.ALARM_SAT.toInt()))   // SAT=32 -> bit6
        assertEquals(0x80, OlleeAlarm.toWatchDayMask(Alarm.ALARM_SUN.toInt()))   // SUN=64 -> bit7
    }

    /**
     * (b) With existing = a capture-confirmed 12-byte 0x4B read-back (no playNow), the built record
     * preserves the watch-only bytes verbatim at record indexes 1, 6, 7, 9, 10, 11.
     */
    @Test
    fun buildRecordPreservesWatchOnlyFieldsFromExistingPayload() {
        val existing = "00010017007E0A05C0FF0FFF".hexToByteArray() // 12 bytes (read-back format, no playNow)
        val record = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 7,
            minute = 15,
            gbRepetitionMask = Alarm.ALARM_MON.toInt() or Alarm.ALARM_WED.toInt() or Alarm.ALARM_FRI.toInt(),
            snooze = false,
            existing = existing
        )

        // Map read-back offsets (12 bytes, no playNow) to record offsets (13 bytes with playNow):
        // Read-back byte 1 (hourlyChime) -> record byte 1
        assertEquals(existing[1], record[1])
        // Read-back byte 6 (chime) -> record byte 6
        assertEquals(existing[6], record[6])
        // Read-back byte 7 (snoozeMin) -> record byte 7
        assertEquals(existing[7], record[7])
        // Read-back byte 8 (hourMaskLo) -> record byte 9
        assertEquals(existing[8], record[9])
        // Read-back byte 9 (hourMaskMid) -> record byte 10
        assertEquals(existing[9], record[10])
        // Read-back byte 10 (hourMaskHi) -> record byte 11
        assertEquals(existing[10], record[11])
    }

    /**
     * (c) playNow byte (index 8) is always 0.
     */
    @Test
    fun buildRecordAlwaysSetsPlayNowTo0() {
        val record = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 7,
            minute = 5,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = false,
            existing = null
        )
        assertEquals(0x00.toByte(), record[8])
    }

    /**
     * (d) record length is always 33, byte 12 is the 0xFF settings-block terminator, and the
     * 20-byte trailing block is five LE u32 0x0000000C.
     */
    @Test
    fun buildRecordHasLength33AndTerminator0xFF() {
        val record1 = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 0,
            minute = 0,
            gbRepetitionMask = 0,
            snooze = false,
            existing = null
        )
        assertEquals(33, record1.size)
        assertEquals(0xFF.toByte(), record1[12])
        assertTrailingBlock(record1)

        val record2 = OlleeAlarm.buildRecord(
            enabled = false,
            hour = 23,
            minute = 59,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = true,
            existing = "00010017007E0A05C0FF0FFF".hexToByteArray()
        )
        assertEquals(33, record2.size)
        assertEquals(0xFF.toByte(), record2[12])
        assertTrailingBlock(record2)
    }

    private fun assertTrailingBlock(record: ByteArray) {
        for (i in 0 until 5) {
            assertEquals(0x0C, record[13 + i * 4].toInt() and 0xFF)
            assertEquals(0x00, record[14 + i * 4].toInt() and 0xFF)
            assertEquals(0x00, record[15 + i * 4].toInt() and 0xFF)
            assertEquals(0x00, record[16 + i * 4].toInt() and 0xFF)
        }
    }

    /**
     * Test that the record contains the input hour and minute at the correct positions.
     */
    @Test
    fun buildRecordSetsHourAndMinuteCorrectly() {
        val record = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 14,
            minute = 45,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = false,
            existing = null
        )
        assertEquals(14, record[3].toInt() and 0xFF) // hour at index 3
        assertEquals(45, record[4].toInt() and 0xFF) // minute at index 4
    }

    /**
     * Test that the record contains the input enabled flag at the correct position.
     */
    @Test
    fun buildRecordSetsEnableByteCorrectly() {
        val enabledRecord = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 7,
            minute = 30,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = false,
            existing = null
        )
        assertEquals(0x01.toByte(), enabledRecord[0])

        val disabledRecord = OlleeAlarm.buildRecord(
            enabled = false,
            hour = 7,
            minute = 30,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = false,
            existing = null
        )
        assertEquals(0x00.toByte(), disabledRecord[0])
    }

    /**
     * Test that snooze enable byte is set correctly (matching FreeOllee behavior).
     */
    @Test
    fun buildRecordSetsSnoozeByte() {
        val snoozeRecord = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 7,
            minute = 30,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = true,
            existing = null
        )
        assertEquals(0x01.toByte(), snoozeRecord[2])

        val noSnoozeRecord = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 7,
            minute = 30,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = false,
            existing = null
        )
        assertEquals(0x00.toByte(), noSnoozeRecord[2])
    }

    /**
     * Test that when existing is null, watch-only fields use sensible defaults.
     */
    @Test
    fun buildRecordUsesDefaultsWhenExistingIsNull() {
        val record = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 7,
            minute = 30,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(),
            snooze = false,
            existing = null
        )
        // When no existing payload, expect defaults:
        // hourlyChime=1 (on), chime=0, snoozeMin=5, hourMask=0xC0FF0F (6:00-19:00)
        assertEquals(0x01.toByte(), record[1]) // hourlyChime on
        assertEquals(0x00.toByte(), record[6]) // chime index 0
        assertEquals(0x05.toByte(), record[7]) // snooze period 5 min
        assertEquals(0xC0.toByte(), record[9]) // hourMaskLo
        assertEquals(0xFF.toByte(), record[10]) // hourMaskMid
        assertEquals(0x0F.toByte(), record[11]) // hourMaskHi
    }

    /**
     * Port of FreeOllee's alarm test vector: 1:30 PM (13:30), enabled, daily.
     */
    @Test
    fun buildRecordReproducesFreeOlleeTestVector() {
        val record = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 13,
            minute = 30,
            gbRepetitionMask = Alarm.ALARM_DAILY.toInt(), // 127 -> 0xFE
            snooze = false,
            existing = null
        )
        // Verify key fields match FreeOllee's test
        assertEquals(0x01.toByte(), record[0]) // enabled
        assertEquals(0x01.toByte(), record[1]) // hourlyChime on (default)
        assertEquals(0x00.toByte(), record[2]) // snooze off
        assertEquals(13, record[3].toInt() and 0xFF) // hour
        assertEquals(30, record[4].toInt() and 0xFF) // minute
        assertEquals(0xFE.toByte(), record[5]) // dayMask every day
        // record[6] is chime (default 0x00 when no existing)
        assertEquals(0x05.toByte(), record[7]) // snoozeMin (default)
        assertEquals(0x00.toByte(), record[8]) // playNow always 0
    }

    /**
     * Test once-alarm (ALARM_ONCE = 0).
     */
    @Test
    fun buildRecordWithAlarmOnceMapsToWatchMask0x00() {
        val record = OlleeAlarm.buildRecord(
            enabled = true,
            hour = 7,
            minute = 0,
            gbRepetitionMask = Alarm.ALARM_ONCE.toInt(),
            snooze = false,
            existing = null
        )
        assertEquals(0x00, record[5].toInt() and 0xFF)
    }

    /** One-shot alarm still ahead today (Wed, 07:00 alarm at 06:30 now) arms Wednesday = bit 3. */
    @Test
    fun oneShotAheadTodayArmsTodayBit() {
        assertEquals(
            0x08,
            OlleeAlarm.oneShotWatchDayMask(hour = 7, minute = 0, nowDayOfWeekIso = 3, nowHour = 6, nowMinute = 30)
        )
    }

    /** One-shot already passed today (Wed, 07:00 alarm at 07:00 now) arms Thursday = bit 4. */
    @Test
    fun oneShotPassedTodayArmsTomorrowBit() {
        assertEquals(
            0x10,
            OlleeAlarm.oneShotWatchDayMask(hour = 7, minute = 0, nowDayOfWeekIso = 3, nowHour = 7, nowMinute = 0)
        )
    }

    /** One-shot passed on Sunday wraps to Monday = bit 1. */
    @Test
    fun oneShotSundayWrapsToMonday() {
        assertEquals(
            0x02,
            OlleeAlarm.oneShotWatchDayMask(hour = 6, minute = 0, nowDayOfWeekIso = 7, nowHour = 22, nowMinute = 0)
        )
    }
}
