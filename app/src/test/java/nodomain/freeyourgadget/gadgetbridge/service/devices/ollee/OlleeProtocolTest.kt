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

import org.junit.Test
import org.junit.Assert.*

class OlleeProtocolTest {

    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    // Test 1: buildSetClock golden vector
    @Test
    fun setClockGoldenVector() {
        val result = OlleeProtocol.buildSetClock(
            nowEpochSec = 1783729820L,
            utcOffsetSec = -21600,
            latE3 = 40140,
            lonE3 = -105144
        )
        val expected = "001aaa55fb5302239c8e516aa0abffffcc9c00004865feff0300ffff".hexToByteArray()
        assertArrayEquals(expected, result)
    }

    // Test 2: CRC matches golden frame
    @Test
    fun crcMatchesGoldenFrame() {
        val goldenFrame = "001aaa55fb5302239c8e516aa0abffffcc9c00004865feff0300ffff".hexToByteArray()
        // Inner bytes are from byte 6 onward (after 00 LEN AA 55 CRC16)
        val inner = goldenFrame.copyOfRange(6, goldenFrame.size)
        val crc = OlleeProtocol.crc16(inner)
        assertEquals(0xfb53, crc)
    }

    // Test 3: readRequest has correct framing
    @Test
    fun readRequestHasLen06() {
        val result = OlleeProtocol.readRequest(0x27)
        val expected = "0006aa552fe80227".hexToByteArray()
        assertArrayEquals(expected, result)
    }

    // Test 4: reassembler joins 20-byte chunks
    @Test
    fun reassemblerJoins20ByteChunks() {
        val goldenFrame = "001aaa55fb5302239c8e516aa0abffffcc9c00004865feff0300ffff".hexToByteArray()
        // Frame is 26 bytes; split at byte 20
        val chunk1 = goldenFrame.copyOfRange(0, 20)
        val chunk2 = goldenFrame.copyOfRange(20, goldenFrame.size)

        val reassembler = OlleeFrameReassembler()

        val frame1 = reassembler.accept(chunk1)
        assertNull("First chunk should not yield a frame", frame1)

        val frame2 = reassembler.accept(chunk2)
        assertNotNull("Second chunk should complete the frame", frame2)
        assertEquals(0x23, frame2!!.target)

        // The payload is bytes 8-25 of the frame (after 6-byte preamble + 1 cmd + 1 target)
        val expectedPayload = goldenFrame.copyOfRange(8, goldenFrame.size)
        assertArrayEquals(expectedPayload, frame2.payload)
    }

    // Test 5: reassembler drops CRC mismatch
    @Test
    fun reassemblerDropsCrcMismatch() {
        val goldenFrame = "001aaa55fb5302239c8e516aa0abffffcc9c00004865feff0300ffff".hexToByteArray()
        // Corrupt one payload byte (change byte at index 10)
        val corruptedFrame = goldenFrame.copyOf()
        corruptedFrame[10] = (corruptedFrame[10].toInt() xor 0xFF).toByte()

        val reassembler = OlleeFrameReassembler()
        val result = reassembler.accept(corruptedFrame)
        assertNull("Corrupted frame with bad CRC should be dropped", result)
    }

    // Test 6: voltage parsing trailing u16
    @Test
    fun voltageParsesTrailingU16() {
        val payload = ByteArray(36)
        payload[34] = 0x0B.toByte()
        payload[35] = 0x1B.toByte()

        val voltage = OlleeProtocol.parseVoltageMillivolts(payload)
        assertEquals(2843, voltage)
    }

    // Test 7: voltage returns null on short payload
    @Test
    fun voltageNullOnShortPayload() {
        val payload = ByteArray(10)
        val voltage = OlleeProtocol.parseVoltageMillivolts(payload)
        assertNull(voltage)
    }

    // Test 8: pipelined replies — one fragment carries the tail of one frame and the head
    // of the next. Fragments captured verbatim from the watch (init read burst, 2026-07-11):
    // version reply (0x4A, 44 bytes) immediately followed by alarm readback (0x4B, 40 bytes).
    @Test
    fun reassemblerKeepsPipelinedFrameTail() {
        val f1 = "002AAA555421024A444541444245454630312E30".hexToByteArray()
        val f2 = "352E303030302E30312E31304445414442454546".hexToByteArray()
        val f3 = "00000B200026AA559381024B0000010C00000005".hexToByteArray()
        val f4 = "C0FF0FFF0C0000000C0000000C0000000C000000".hexToByteArray()
        val f5 = "0C000000".hexToByteArray()

        val reassembler = OlleeFrameReassembler()

        assertNull(reassembler.accept(f1))
        assertNull(reassembler.accept(f2))

        // f3 completes the version frame AND starts the alarm frame
        val version = reassembler.accept(f3)
        assertNotNull("f3 should complete the version frame", version)
        assertEquals(0x4A, version!!.target)
        assertEquals(2848, OlleeProtocol.parseVoltageMillivolts(version.payload))
        assertNull("Alarm frame is still incomplete", reassembler.pending())

        assertNull(reassembler.accept(f4))
        val alarm = reassembler.accept(f5)
        assertNotNull("f5 should complete the alarm frame", alarm)
        assertEquals(0x4B, alarm!!.target)
        assertArrayEquals(
            "0000010C00000005C0FF0FFF0C0000000C0000000C0000000C0000000C000000".hexToByteArray(),
            alarm.payload
        )
    }

    // Test 9: two complete frames in a single chunk — accept yields the first, pending drains the second
    @Test
    fun reassemblerDrainsTwoFramesFromOneChunk() {
        val readA = OlleeProtocol.readRequest(0x2A)
        val readB = OlleeProtocol.readRequest(0x2B)

        val reassembler = OlleeFrameReassembler()
        val first = reassembler.accept(readA + readB)
        assertNotNull(first)
        assertEquals(0x2A, first!!.target)

        val second = reassembler.pending()
        assertNotNull(second)
        assertEquals(0x2B, second!!.target)

        assertNull(reassembler.pending())
    }

    // Test 10: hardware and firmware split out of the real 0x4A payload (hardware capture)
    @Test
    fun firmwareVersionGoldenVector() {
        val payload = (
            "4445414442454546" +                    // "DEADBEEF"
            "30312E30352E3030" +                    // "01.05.00" hardware
            "30302E30312E3130" +                    // "00.01.10" firmware
            "4445414442454546" +                    // "DEADBEEF"
            "0000" + "0B20"                         // padding + 2848 mV
        ).hexToByteArray()

        // Two adjacent triples, not one 16-char string: the run of zeros hides the seam.
        assertEquals("01.05.00", OlleeProtocol.parseHardwareVersion(payload))
        assertEquals("00.01.10", OlleeProtocol.parseFirmwareVersion(payload))
        assertEquals(2848, OlleeProtocol.parseVoltageMillivolts(payload))
    }

    // Test 11: both versions are null on short or non-ASCII payloads
    @Test
    fun firmwareVersionNullOnBadPayload() {
        assertNull(OlleeProtocol.parseFirmwareVersion(ByteArray(10)))
        assertNull(OlleeProtocol.parseFirmwareVersion(ByteArray(36))) // all zero bytes
        assertNull(OlleeProtocol.parseHardwareVersion(ByteArray(10)))
        assertNull(OlleeProtocol.parseHardwareVersion(ByteArray(36)))
    }

    // Test 11b: a payload long enough for hardware but not firmware yields only the hardware half
    @Test
    fun hardwareVersionSurvivesATruncatedPayload() {
        val payload = ("4445414442454546" + "30312E30352E3030").hexToByteArray()
        assertEquals("01.05.00", OlleeProtocol.parseHardwareVersion(payload))
        assertNull(OlleeProtocol.parseFirmwareVersion(payload))
    }
}
