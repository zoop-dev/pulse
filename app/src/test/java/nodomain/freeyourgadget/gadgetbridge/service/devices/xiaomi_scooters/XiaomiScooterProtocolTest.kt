package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies [XiaomiScooterProtocol] encode/decode against real captured traffic.
 */
class XiaomiScooterProtocolTest {
    @Test
    fun testEncodeSet_motorLock() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 6,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_MOTOR_LOCKED,
                    XiaomiScooterProtocol.TYPE_BOOL,
                    byteArrayOf(1)
                )
            ),
        )
        assertArrayHexEquals("0c2006000001020200010001", encoded)
    }

    @Test
    fun testEncodeSet_motorUnlock() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 7,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_MOTOR_LOCKED,
                    XiaomiScooterProtocol.TYPE_BOOL,
                    byteArrayOf(0)
                )
            ),
        )
        assertArrayHexEquals("0c2007000001020200010000", encoded)
    }

    @Test
    fun testEncodeSet_ambientLightOn() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x0c,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_AMBIENT_LIGHT,
                    XiaomiScooterProtocol.TYPE_U8,
                    byteArrayOf(1)
                )
            ),
        )
        assertArrayHexEquals("0c200c000001021000011001", encoded)
    }

    @Test
    fun testEncodeSet_autoLightsOn() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x0e,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_AUTO_LIGHTS,
                    XiaomiScooterProtocol.TYPE_BOOL,
                    byteArrayOf(1)
                )
            ),
        )
        assertArrayHexEquals("0c200e000001020c00010001", encoded)
    }

    @Test
    fun testEncodeSet_smartEnergyRecoveryOn() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x17,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_SMART_ENERGY_RECOVERY,
                    XiaomiScooterProtocol.TYPE_BOOL,
                    byteArrayOf(1)
                )
            ),
        )
        assertArrayHexEquals("0c2017000001020e00010001", encoded)
    }

    @Test
    fun testEncodeSet_slopeParkingOn() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x19,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_SLOPE_PARKING,
                    XiaomiScooterProtocol.TYPE_BOOL,
                    byteArrayOf(1)
                )
            ),
        )
        assertArrayHexEquals("0c2019000001020f00010001", encoded)
    }

    @Test
    fun testEncodeSet_tcsAntiSlipOn() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x1c,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_TCS_ANTI_SLIP,
                    XiaomiScooterProtocol.TYPE_BOOL,
                    byteArrayOf(1)
                )
            ),
        )
        assertArrayHexEquals("0c201c000001020d00010001", encoded)
    }

    @Test
    fun testEncodeSet_energyRecoveryLow() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x1f,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_ENERGY_RECOVERY_INTENSITY,
                    XiaomiScooterProtocol.TYPE_U8,
                    byteArrayOf(0x1e)
                ),
            ),
        )
        assertArrayHexEquals("0c201f00000102050001101e", encoded)
    }

    @Test
    fun testEncodeSet_energyRecoveryMedium() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x1d,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_ENERGY_RECOVERY_INTENSITY,
                    XiaomiScooterProtocol.TYPE_U8,
                    byteArrayOf(0x3c)
                ),
            ),
        )
        assertArrayHexEquals("0c201d00000102050001103c", encoded)
    }

    @Test
    fun testEncodeSet_energyRecoveryHigh() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x1e,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_ENERGY_RECOVERY_INTENSITY,
                    XiaomiScooterProtocol.TYPE_U8,
                    byteArrayOf(0x5a)
                ),
            ),
        )
        assertArrayHexEquals("0c201e00000102050001105a", encoded)
    }

    @Test
    fun testEncodeSet_taillightAlwaysOnOn() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x25,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_TAILLIGHT_ALWAYS_ON,
                    XiaomiScooterProtocol.TYPE_BOOL,
                    byteArrayOf(1)
                )
            ),
        )
        assertArrayHexEquals("0c2025000001020400010001", encoded)
    }

    @Test
    fun testEncodeSet_unitsMph() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x27,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_SPEED_UNIT,
                    XiaomiScooterProtocol.TYPE_U8,
                    byteArrayOf(0)
                )
            ),
        )
        assertArrayHexEquals("0c2027000001030500011000", encoded)
    }

    @Test
    fun testEncodeSet_unitsKmh() {
        val encoded = XiaomiScooterProtocol.encodeSet(
            txn = 0x28,
            entries = listOf(
                XiaomiScooterProtocol.SetEntry(
                    XiaomiScooterProperties.CODE_SPEED_UNIT,
                    XiaomiScooterProtocol.TYPE_U8,
                    byteArrayOf(1)
                )
            ),
        )
        assertArrayHexEquals("0c2028000001030500011001", encoded)
    }

    @Test
    fun testDecodeSetAck_motorLock() {
        val message = XiaomiScooterProtocol.decode(GB.hexStringToByteArray("0b20060001010202000000"))
        assertNotNull(message)
        assertEquals(XiaomiScooterProtocol.OPCODE_SET_ACK, message!!.opcode)
        assertEquals(6, message.txn)
        assertEquals(1, message.entries.size)
        assertEquals(XiaomiScooterProperties.CODE_MOTOR_LOCKED, message.entries[0].code)
        assertEquals(0, message.entries[0].status)
        assertNull(message.entries[0].value)
    }

    @Test
    fun testDecodeNotify_rideModeSport() {
        val message = XiaomiScooterProtocol.decode(GB.hexStringToByteArray("0c203f000401010100011003"))
        assertNotNull(message)
        assertEquals(XiaomiScooterProtocol.OPCODE_NOTIFY, message!!.opcode)
        assertEquals(0x3f, message.txn)
        assertEquals(1, message.entries.size)
        val entry = message.entries[0]
        assertEquals(XiaomiScooterProperties.CODE_RIDE_MODE, entry.code)
        assertEquals(3, entry.value!!.asU8())
    }

    @Test
    fun testDecodeGetRsp_batteryVoltage() {
        // 17-byte GET_RSP: property=0104 status=000000 len=04 type=0x90(f32) data=00 e8 a7 45
        val message = XiaomiScooterProtocol.decode(GB.hexStringToByteArray("1120000003010104000000049000e8a745"))
        assertNotNull(message)
        assertEquals(XiaomiScooterProtocol.OPCODE_GET_RSP, message!!.opcode)
        assertEquals(1, message.entries.size)
        val entry = message.entries[0]
        assertEquals(XiaomiScooterProperties.CODE_VOLTAGE, entry.code)
        assertEquals(0, entry.status)
        // 5373.0 raw -> /100 = 53.73V, matching the observed 53.73 V in the app
        assertEquals(5373.0f, entry.value!!.asF32()!!, 0.01f)
    }

    @Test
    fun testDecode_rejectsTruncatedFrame() {
        assertNull(XiaomiScooterProtocol.decode(byteArrayOf(0x0c, 0x20, 0x06, 0x00)))
    }

    @Test
    fun testDecode_rejectsLengthMismatch() {
        assertNull(XiaomiScooterProtocol.decode(GB.hexStringToByteArray("0d2006000001020200010001")))
    }

    @Test
    fun testEncodeDecode_largeMessageUsesExtendedLengthBits() {
        // 6-byte header + 50 * 6-byte SET entries = 306 bytes: over the 8-bit range of a single
        // length byte, so this exercises the 13-bit length (low byte in frame[0], high bits packed
        // into the marker byte at frame[1]). 306 = 0x132 -> low byte 0x32 (50), high bits 0b00001
        // packed onto the marker (0x20 | 1 = 0x21).
        val entries = (1..50).map { code ->
            XiaomiScooterProtocol.SetEntry(code, XiaomiScooterProtocol.TYPE_BOOL, byteArrayOf(1))
        }
        val encoded = XiaomiScooterProtocol.encodeSet(txn = 1, entries = entries)
        assertEquals(306, encoded.size)
        assertEquals(0x32, encoded[0].toInt() and 0xff)
        assertEquals(0x21, encoded[1].toInt() and 0xff)

        val decoded = XiaomiScooterProtocol.decode(encoded)
        assertNotNull(decoded)
        assertEquals(50, decoded!!.entries.size)
        assertEquals(1, decoded.entries[0].code)
        assertEquals(50, decoded.entries[49].code)
    }

    @Test
    fun testDecode_realCaptureExtendedLength() {
        // Real capture: a 458-byte GET_RSP (many properties incl. ride history) whose header read
        // len=0xca (202) and marker=0x21 -- initially looked like single-byte-length corruption,
        // until working out that 458 = 0x1ca: low byte 0xca, high bits 0b00001 packed onto the
        // marker byte (0x20 | 1 = 0x21). Reconstructs that header shape with a minimal empty body.
        val frame = ByteArray(458)
        frame[0] = 0xca.toByte()
        frame[1] = 0x21.toByte()
        frame[2] = 0x01.toByte() // txn low
        frame[3] = 0x00.toByte() // txn high
        frame[4] = XiaomiScooterProtocol.OPCODE_GET_RSP.toByte()
        frame[5] = 0x00.toByte() // count

        val decoded = XiaomiScooterProtocol.decode(frame)
        assertNotNull(decoded)
        assertEquals(XiaomiScooterProtocol.OPCODE_GET_RSP, decoded!!.opcode)
        assertEquals(1, decoded.txn)
        assertEquals(0, decoded.entries.size)
    }

    @Test
    fun testEncodeHello() {
        val encoded = XiaomiScooterProtocol.encodeHello(txn = 0)
        assertArrayHexEquals("05200000f0", encoded)
        val decoded = XiaomiScooterProtocol.decode(encoded)
        assertNotNull(decoded)
        assertEquals(XiaomiScooterProtocol.OPCODE_HELLO, decoded!!.opcode)
    }

    private fun assertArrayHexEquals(expectedHex: String, actual: ByteArray) {
        assertEquals(GB.hexdump(GB.hexStringToByteArray(expectedHex)), GB.hexdump(actual))
    }
}
