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

class OlleeFacesTableTest {

    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    /**
     * Builds a full 14-record faces table. Slot->ID: 01=05 02=07 03=09 04=11 05=06 06=0D 07=08
     * 08=0E 09=0A 0A=0B 0B=0C 0C=0F 0D=10 0E=12. [unknownByteForId] overrides the ?? byte (default 0).
     */
    private fun buildFacesPayload(
        enabledIds: Set<Int> = emptySet(),
        unknownByteForId: Map<Int, Int> = emptyMap()
    ): ByteArray {
        val payload = mutableListOf<Byte>()

        // 6-byte header
        payload.add(0x04)
        payload.add(0x00)
        payload.add(0x00)
        payload.add(0x00)
        payload.add(0x00)
        payload.add(0x00)

        // Slot to (ID, slot) mapping
        val slotToId = mapOf(
            0x01 to 0x05,
            0x02 to 0x07,
            0x03 to 0x09,
            0x04 to 0x11,
            0x05 to 0x06,
            0x06 to 0x0D,
            0x07 to 0x08,
            0x08 to 0x0E,
            0x09 to 0x0A,
            0x0A to 0x0B,
            0x0B to 0x0C,
            0x0C to 0x0F,
            0x0D to 0x10,
            0x0E to 0x12
        )

        // 14 records in slot order
        for (slot in 0x01..0x0E) {
            val faceId = slotToId[slot] ?: error("Unknown slot $slot")
            val enabled = if (enabledIds.contains(faceId)) 0x01 else 0x00
            val unknownByte = unknownByteForId[faceId] ?: 0x00

            payload.add(faceId.toByte())
            payload.add(0x01)
            payload.add(enabled.toByte())
            payload.add(0x01)
            payload.add(unknownByte.toByte())
            payload.add(slot.toByte())
        }

        return payload.toByteArray()
    }

    // Test 1: Build full 14-record table payload
    @Test
    fun buildFacesPayloadCreates14Records() {
        val payload = buildFacesPayload()
        // 6-byte header + 14 * 6-byte records = 90 bytes
        assertEquals(90, payload.size)
    }

    // Test 2: withFaceEnabled flips exactly one byte for ID 0x06 (World Time)
    @Test
    fun withFaceEnabledFlipsExactlyOneByte() {
        val payload = buildFacesPayload(
            enabledIds = setOf(0x0B), // Temperature enabled
            unknownByteForId = mapOf(0x0B to 0x2A) // Mark World Time's ?? byte as 0x2A
        )
        // Slot 5 is ID 0x06 (World Time), currently disabled
        val result = OlleeFacesTable.withFaceEnabled(payload, 0x06, true)

        // Check length is unchanged
        assertEquals(payload.size, result.size)

        // Find the World Time record (slot 5)
        // Header is 6 bytes, then records start. Slot 5 is the 5th record (index 4)
        val recordOffset = 6 + 4 * 6 // bytes into payload
        val enabledByteOffset = recordOffset + 2

        // The enabled byte should have flipped from 0x00 to 0x01
        assertEquals(0x00.toByte(), payload[enabledByteOffset])
        assertEquals(0x01.toByte(), result[enabledByteOffset])

        // All other bytes should be identical
        for (i in payload.indices) {
            if (i != enabledByteOffset) {
                assertEquals(
                    "Byte at index $i differs",
                    payload[i], result[i]
                )
            }
        }
    }

    // Test 3: withFaceEnabled on already-enabled face returns content-equal payload
    @Test
    fun withFaceEnabledAlreadyEnabledReturnsSameContent() {
        val payload = buildFacesPayload(enabledIds = setOf(0x06)) // World Time already enabled
        val result = OlleeFacesTable.withFaceEnabled(payload, 0x06, true)

        // Should be equal in content
        assertArrayEquals(payload, result)
    }

    // Test 4: isFaceEnabled reflects the ENABLED byte
    @Test
    fun isFaceEnabledReflectsEnabledByte() {
        val payload = buildFacesPayload(
            enabledIds = setOf(0x06) // World Time enabled
        )

        assertTrue("World Time (0x06) should be enabled", OlleeFacesTable.isFaceEnabled(payload, 0x06)!!)
        assertFalse("Temperature (0x0B) should be disabled", OlleeFacesTable.isFaceEnabled(payload, 0x0B)!!)
    }

    // Test 5: isFaceEnabled returns null for unknown ID
    @Test
    fun isFaceEnabledReturnsNullForUnknownId() {
        val payload = buildFacesPayload()
        val result = OlleeFacesTable.isFaceEnabled(payload, 0x42)
        assertNull("Unknown ID 0x42 should return null", result)
    }

    // Test 6: isFaceEnabled returns null for too-short payload
    @Test
    fun isFaceEnabledReturnsNullForTooShortPayload() {
        val tooShort = byteArrayOf(0x04, 0x00, 0x00)
        val result = OlleeFacesTable.isFaceEnabled(tooShort, 0x06)
        assertNull("Too-short payload should return null", result)
    }

    // Test 7: The ?? byte survives withFaceEnabled
    @Test
    fun unknownBytePreservedOnWithFaceEnabled() {
        val payload = buildFacesPayload(
            enabledIds = setOf(0x0A), // Step Counter enabled, World Time disabled
            unknownByteForId = mapOf(0x06 to 0x2A, 0x0A to 0x3B) // Different ?? bytes
        )

        // Flip World Time (0x06) to enabled
        val result = OlleeFacesTable.withFaceEnabled(payload, 0x06, true)

        // Find World Time record (slot 5, record index 4)
        val recordOffset = 6 + 4 * 6
        val unknownByteOffset = recordOffset + 4

        // The ?? byte (0x2A) should be preserved
        assertEquals(0x2A.toByte(), payload[unknownByteOffset])
        assertEquals(0x2A.toByte(), result[unknownByteOffset])

        // Also verify Step Counter's ?? byte is untouched
        // Slot 9 is record index 8
        val stepCounterRecordOffset = 6 + 8 * 6
        val stepCounterUnknownOffset = stepCounterRecordOffset + 4
        assertEquals(0x3B.toByte(), result[stepCounterUnknownOffset])
    }

    // Test 8: withFaceEnabled disabling a face
    @Test
    fun withFaceEnabledDisablingFace() {
        val payload = buildFacesPayload(enabledIds = setOf(0x06, 0x0B)) // Both enabled
        val result = OlleeFacesTable.withFaceEnabled(payload, 0x06, false)

        // World Time should be disabled in result
        assertFalse("World Time should be disabled", OlleeFacesTable.isFaceEnabled(result, 0x06)!!)
        // Temperature should still be enabled
        assertTrue("Temperature should still be enabled", OlleeFacesTable.isFaceEnabled(result, 0x0B)!!)
    }

    // Test 9: Trailing partial record is tolerated (payload with < 6 bytes at end)
    @Test
    fun trailingPartialRecordTolerated() {
        val fullPayload = buildFacesPayload(enabledIds = setOf(0x06))
        // Remove last 3 bytes from last record
        val truncated = fullPayload.copyOfRange(0, fullPayload.size - 3)

        // isFaceEnabled should still work on earlier records
        assertTrue(OlleeFacesTable.isFaceEnabled(truncated, 0x06)!!)

        // Attempting to query a face in the truncated record should return null
        // (because it can't be found completely)
        val result = OlleeFacesTable.isFaceEnabled(truncated, 0x12) // Game C (slot 0E, last record)
        assertNull("Incomplete record for Game C should return null", result)
    }

    // Test 10: withFaceEnabled on unknown face returns payload unchanged
    @Test
    fun withFaceEnabledUnknownFaceReturnsUnchanged() {
        val payload = buildFacesPayload(enabledIds = setOf(0x06))
        val result = OlleeFacesTable.withFaceEnabled(payload, 0x42, true)

        // Content should be identical (no change)
        assertArrayEquals(payload, result)
    }
}
