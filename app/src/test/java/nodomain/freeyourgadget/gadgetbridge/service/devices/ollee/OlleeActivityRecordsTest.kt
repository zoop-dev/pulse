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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OlleeActivityRecordsTest {

    companion object {
        fun hexToByteArray(hex: String): ByteArray {
            val cleaned = hex.replace(" ", "").replace("\n", "")
            require(cleaned.length % 2 == 0) { "Hex string must have even length" }
            return ByteArray(cleaned.length / 2) { i ->
                cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }
    }

    @Test
    fun testParseCount() {
        val payload = hexToByteArray("00000003")
        assertEquals(3L, OlleeActivityRecords.parseCount(payload))
    }

    @Test
    fun testParseSnoopCaptureRecords() {
        // Snoop capture records
        val record1Hex = "000000006a51ec446a51ec4800000000"
        val record1 = OlleeActivityRecords.parseRecord(hexToByteArray(record1Hex))
        assertEquals(0L, record1.typeFlags)
        assertEquals(0x6a51ec44L, record1.startTs)
        assertEquals(0x6a51ec48L, record1.endTs)
        assertEquals(0L, record1.steps)

        val record2Hex = "000000006a51ec486a51ecc400000014"
        val record2 = OlleeActivityRecords.parseRecord(hexToByteArray(record2Hex))
        assertEquals(0L, record2.typeFlags)
        assertEquals(0x6a51ec48L, record2.startTs)
        assertEquals(0x6a51ecc4L, record2.endTs)
        assertEquals(20L, record2.steps)

        val record3Hex = "000000006a51ecc46a51ed4f0000001a"
        val record3 = OlleeActivityRecords.parseRecord(hexToByteArray(record3Hex))
        assertEquals(0L, record3.typeFlags)
        assertEquals(0x6a51ecc4L, record3.startTs)
        assertEquals(0x6a51ed4fL, record3.endTs)
        assertEquals(26L, record3.steps)

        // Verify steps sum to 46
        assertEquals(46L, record1.steps + record2.steps + record3.steps)
    }

    @Test
    fun testParseReplayCaptureRecords() {
        // Replay capture records
        val record1Hex = "000000006a51ed4f6a51f800000000aa"
        val record1 = OlleeActivityRecords.parseRecord(hexToByteArray(record1Hex))
        assertEquals(170L, record1.steps)
        assertEquals(0x6a51ed4fL, record1.startTs)
        assertEquals(0x6a51f800L, record1.endTs)

        val record2Hex = "000000006a51f8006a52061000000000"
        val record2 = OlleeActivityRecords.parseRecord(hexToByteArray(record2Hex))
        assertEquals(0L, record2.steps)
        val interval2 = record2.endTs - record2.startTs
        assertEquals(3600L, interval2)

        val record3Hex = "000000006a5206106a52142000000000"
        val record3 = OlleeActivityRecords.parseRecord(hexToByteArray(record3Hex))
        assertEquals(0L, record3.steps)
        val interval3 = record3.endTs - record3.startTs
        assertEquals(3600L, interval3)
    }

    @Test
    fun testChainProperty() {
        // Snoop capture chain property
        val records = listOf(
            OlleeActivityRecords.parseRecord(hexToByteArray("000000006a51ec446a51ec4800000000")),
            OlleeActivityRecords.parseRecord(hexToByteArray("000000006a51ec486a51ecc400000014")),
            OlleeActivityRecords.parseRecord(hexToByteArray("000000006a51ecc46a51ed4f0000001a"))
        )

        for (i in 0 until records.size - 1) {
            assertEquals(records[i].endTs, records[i + 1].startTs)
        }

        // Replay capture chain property
        val replayRecords = listOf(
            OlleeActivityRecords.parseRecord(hexToByteArray("000000006a51ed4f6a51f800000000aa")),
            OlleeActivityRecords.parseRecord(hexToByteArray("000000006a51f8006a52061000000000")),
            OlleeActivityRecords.parseRecord(hexToByteArray("000000006a5206106a52142000000000"))
        )

        for (i in 0 until replayRecords.size - 1) {
            assertEquals(replayRecords[i].endTs, replayRecords[i + 1].startTs)
        }
    }

    @Test
    fun testParseRecordWith15BytesThrows() {
        val payload = hexToByteArray("000000006a51ec446a51ec48000000")  // 15 bytes
        assertThrows(IllegalArgumentException::class.java) {
            OlleeActivityRecords.parseRecord(payload)
        }
    }

    @Test
    fun testParseCountWith3BytesThrows() {
        val payload = hexToByteArray("000000")  // 3 bytes
        assertThrows(IllegalArgumentException::class.java) {
            OlleeActivityRecords.parseCount(payload)
        }
    }

    @Test
    fun testParseRecordUnsignedHighBitValues() {
        // Record with steps field "80000001" -> 2147483649L
        val recordHex = "00000000000000000000000080000001"
        val record = OlleeActivityRecords.parseRecord(hexToByteArray(recordHex))
        assertEquals(2147483649L, record.steps)
    }
}
