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
import nodomain.freeyourgadget.gadgetbridge.devices.una.UnaConstants
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
    fun readChunkSizeFor_usesEveryByteTheLinkCanCarry() {
        // 16-byte FTS header plus ATT's 3, so capacity is mtu - 19.
        assertEquals(81, UnaFtsProtocol.readChunkSizeFor(100))
        assertEquals(181, UnaFtsProtocol.readChunkSizeFor(200))
    }

    @Test
    fun readChunkSizeFor_neverExceedsCapacityAtTheMinimumMtu() {
        // The BLE minimum of 23 leaves 4 bytes. Asking for more advertises data the notification
        // cannot carry and hangs the read, so nothing may raise the result above capacity.
        assertEquals(4, UnaFtsProtocol.readChunkSizeFor(23))
    }

    @Test
    fun readChunkSizeFor_isCappedAtTheVerifiedMaximum() {
        // A large MTU could carry more, but only 200 has been verified against a watch.
        assertEquals(UnaConstants.MAX_READ_CHUNK_SIZE, UnaFtsProtocol.readChunkSizeFor(517))
        assertEquals(UnaConstants.MAX_READ_CHUNK_SIZE, UnaFtsProtocol.readChunkSizeFor(220))
    }

    @Test
    fun readChunkSizeFor_staysPositiveForNonsensicalInput() {
        assertEquals(1, UnaFtsProtocol.readChunkSizeFor(0))
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
        // Real captured 0x11 response mid-transfer: offset=46080, total=177756 (a 32-bit field,
        // not 16-bit as first assumed). chunkLen=128, no payload bytes appended here.
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
    fun parseReadChunk_rejectsShortPayload() {
        // Header claims a 128-byte chunk but only 4 bytes follow.
        val raw = hexToByteArray("1101000000000000800000008000000000000000")
        assertNull(UnaFtsProtocol.parseReadChunk(raw))
    }

    @Test
    fun parseReadChunk_rejectsHugeChunkLenWithoutOverflowing() {
        // chunkLen = 0xFFFFFFF0 (a corrupted/hostile value, never sent by real firmware). Naively
        // adding this to the 16-byte header size wraps a signed 32-bit Int negative, which would
        // defeat the truncation check below and crash copyOfRange instead of returning null.
        val raw = hexToByteArray("11 01 00 00 00 00 00 00 00 00 00 00 f0 ff ff ff")
        assertNull(UnaFtsProtocol.parseReadChunk(raw))
    }
}
