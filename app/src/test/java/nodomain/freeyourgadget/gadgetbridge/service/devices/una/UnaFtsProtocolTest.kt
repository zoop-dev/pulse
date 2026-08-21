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

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnaFtsProtocolTest {
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
    fun buildListRequest_forApps() {
        // Real captured request for listing "/Apps/".
        val expected = hexToByteArray("500006002f417070732f")
        assertArrayEquals(expected, UnaFtsProtocol.buildListRequest("/Apps/"))
    }

    @Test
    fun parseListEntry_realCapturedAlarmEntry() {
        // Real captured 0x51 response: index=0 of 20, attr=1 (directory), name="Alarm".
        val raw = hexToByteArray("5101050000000000140000000100000080737F7CD953060000000000416C61726D")
        val entry = UnaFtsProtocol.parseListEntry(raw)
        assertEquals(0, entry?.index)
        assertEquals(20, entry?.total)
        assertEquals(1, entry?.attr)
        assertEquals("Alarm", entry?.name)
        assertTrue(entry!!.isDirectory)
    }

    @Test
    fun parseListEntry_fileHasDirectoryBitClear() {
        // Real captured entry for "app_list.json" (a file, not a directory): attr=0.
        val raw = hexToByteArray(
            "51010D000C0000001400000000000000008A7131015806007B0700006170705F6C6973742E6A736F6E"
        )
        val entry = UnaFtsProtocol.parseListEntry(raw)
        assertEquals("app_list.json", entry?.name)
        assertFalse(entry!!.isDirectory)
    }

    @Test
    fun parseListEntry_rejectsWrongOpcode() {
        val raw = hexToByteArray("11010000000000000000000000000000000000000000000000000000")
        assertNull(UnaFtsProtocol.parseListEntry(raw))
    }

    @Test
    fun parseListEntry_rejectsTruncatedHeader() {
        assertNull(UnaFtsProtocol.parseListEntry(hexToByteArray("5101")))
    }

    @Test
    fun buildReadPacingRequest_carriesStatusOkAndNoPath() {
        val request = UnaFtsProtocol.buildReadPacingRequest(offset = 4096, chunkLen = 4096)

        assertEquals(0x12, request[0].toInt() and 0xFF)
        assertEquals(0x01, request[1].toInt())
        assertEquals(0, request[2].toInt())
        assertEquals(0, request[3].toInt())
        assertEquals(4096, BLETypeConversions.toUint32(request, 4))
        assertEquals(4096, BLETypeConversions.toUint32(request, 8))
        assertEquals(12, request.size)
    }

    @Test
    fun buildReadRequest_headerLayout() {
        val path = "/Apps/GpsLab/Activity/202607/activity_20260731T173034.fit"
        val request = UnaFtsProtocol.buildReadRequest(path, offset = 46080, chunkLen = 128)

        assertEquals(0x10, request[0].toInt() and 0xFF)
        assertEquals(0, request[1].toInt())
        assertEquals(path.length, BLETypeConversions.toUint16(request, 2))
        assertEquals(46080, BLETypeConversions.toUint32(request, 4))
        assertEquals(128, BLETypeConversions.toUint32(request, 8))
        assertEquals(path, String(request, 12, path.length, Charsets.US_ASCII))
        assertEquals(12 + path.length, request.size)
    }

    @Test
    fun parseReadChunk_realCapturedChunkWith32BitTotal() {
        // Captured mid-transfer: offset=46080, total=177756 in a 32-bit field.
        val raw = hexToByteArray("1101000000b400005cb6020080000000") + ByteArray(128)
        val chunk = UnaFtsProtocol.parseReadChunk(raw)
        assertEquals(46080, chunk?.offset)
        assertEquals(177756, chunk?.total)
        assertEquals(128, chunk?.payload?.size)
    }

    @Test
    fun parseReadChunk_rejectsWrongOpcode() {
        val raw = hexToByteArray("510100000000000000000000000000") + ByteArray(128)
        assertNull(UnaFtsProtocol.parseReadChunk(raw))
    }

    @Test
    fun parseReadChunk_keepsTheBytesDeliveredWhenTheHeaderAdvertisesMore() {
        // Captured: header advertises 204, notification carries 201.
        val raw = hexToByteArray("11010000" + "00000000" + "e0f70000" + "cc000000") + ByteArray(201)
        val chunk = UnaFtsProtocol.parseReadChunk(raw)
        assertEquals(0, chunk?.offset)
        assertEquals(63456, chunk?.total)
        assertEquals(201, chunk?.payload?.size)
        assertTrue(chunk!!.deliveredLessThanAdvertised)
    }

    @Test
    fun parseReadChunk_detectsAShortfallOfASingleByte() {
        val short = hexToByteArray("11010000" + "00000000" + "e0f70000" + "cc000000") + ByteArray(203)
        assertTrue(UnaFtsProtocol.parseReadChunk(short)!!.deliveredLessThanAdvertised)
        val exact = hexToByteArray("11010000" + "00000000" + "e0f70000" + "cc000000") + ByteArray(204)
        assertFalse(UnaFtsProtocol.parseReadChunk(exact)!!.deliveredLessThanAdvertised)
    }

    @Test
    fun parseReadChunk_acceptsAHeaderSizedToItsOwnNotification() {
        val raw = hexToByteArray("1101000000b400005cb6020080000000") + ByteArray(128)
        assertFalse(UnaFtsProtocol.parseReadChunk(raw)!!.deliveredLessThanAdvertised)
    }

    @Test
    fun parseReadChunk_rejectsHeaderWithNoPayload() {
        assertNull(UnaFtsProtocol.parseReadChunk(hexToByteArray("11010000000000008000000080000000")))
    }

    @Test
    fun parseReadChunk_clampsAChunkLenThatOverflowsASignedInt() {
        val raw = hexToByteArray("11 01 00 00 00 00 00 00 00 00 00 00 f0 ff ff ff") + ByteArray(4)
        val chunk = UnaFtsProtocol.parseReadChunk(raw)
        assertEquals(4, chunk?.payload?.size)
        assertTrue(chunk!!.deliveredLessThanAdvertised)
    }

    @Test
    fun parseReadChunk_rejectsAnOverflowingChunkLenWithNothingBehindIt() {
        val raw = hexToByteArray("11 01 00 00 00 00 00 00 00 00 00 00 f0 ff ff ff")
        assertNull(UnaFtsProtocol.parseReadChunk(raw))
    }
}
