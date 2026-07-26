package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies [XiaomiScooterTransport] against the control frames observed in the login handshake capture.
 */
class XiaomiScooterTransportTest {
    @Test
    fun testParse_rcvRdy() {
        assertEquals(
            XiaomiScooterTransport.ParsedFrame.RcvRdy,
            XiaomiScooterTransport.parse(GB.hexStringToByteArray("00000101"))
        )
    }

    @Test
    fun testParse_rcvOk() {
        assertEquals(
            XiaomiScooterTransport.ParsedFrame.RcvOk,
            XiaomiScooterTransport.parse(GB.hexStringToByteArray("00000100"))
        )
    }

    @Test
    fun testParse_ack() {
        assertEquals(
            XiaomiScooterTransport.ParsedFrame.Ack,
            XiaomiScooterTransport.parse(GB.hexStringToByteArray("00000300"))
        )
    }

    @Test
    fun testParse_sendDevPubkey() {
        // SEND(type=3, frameCount=1), as written by the app before sending its ECDH public key.
        val frame =
            XiaomiScooterTransport.parse(XiaomiScooterTransport.buildSend(XiaomiScooterTransport.TYPE_DEV_PUBKEY))
        assertTrue(frame is XiaomiScooterTransport.ParsedFrame.Send)
        frame as XiaomiScooterTransport.ParsedFrame.Send
        assertEquals(XiaomiScooterTransport.TYPE_DEV_PUBKEY, frame.type)
        assertEquals(1, frame.frameCount)
    }

    @Test
    fun testParse_devicePubkeyDataFrame() {
        // "00 00 02 03" header + 64-byte device public key, as pushed by the device.
        val payload = ByteArray(64) { it.toByte() }
        val raw = byteArrayOf(0x00, 0x00, 0x02, 0x03) + payload
        val frame = XiaomiScooterTransport.parse(raw)
        assertTrue(frame is XiaomiScooterTransport.ParsedFrame.Data)
        frame as XiaomiScooterTransport.ParsedFrame.Data
        assertEquals(XiaomiScooterTransport.TYPE_DEV_PUBKEY, frame.type)
        assertArrayEquals(payload, frame.payload)
    }

    @Test
    fun testBuildDataFrame_prependsIndex() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val frame = XiaomiScooterTransport.buildDataFrame(payload, index = 1)
        assertArrayEquals(byteArrayOf(0x01, 0x00, 0x01, 0x02, 0x03), frame)
    }

    @Test
    fun testReplayKeyExchangePdu_flipsDirectionMarkerOnly() {
        // "00 00 04 00 06 f2" -- device-side pre-key header PDU.
        val deviceFrame = GB.hexStringToByteArray("0000040006f2")
        val reply = XiaomiScooterTransport.replayKeyExchangePdu(deviceFrame)
        assertArrayEquals(GB.hexStringToByteArray("0000050006f2"), reply)

        // Original buffer must not be mutated.
        assertArrayEquals(GB.hexStringToByteArray("0000040006f2"), deviceFrame)
    }

    @Test
    fun testParse_keyExchangeDataPdu() {
        // "00 00 04 01" + 240x0xf2 -- device-side pre-key data PDU.
        val raw = byteArrayOf(0x00, 0x00, 0x04, 0x01) + ByteArray(240) { 0xf2.toByte() }
        val frame = XiaomiScooterTransport.parse(raw)
        assertTrue(frame is XiaomiScooterTransport.ParsedFrame.KeyExchange)
        frame as XiaomiScooterTransport.ParsedFrame.KeyExchange
        assertTrue(frame.fromDevice)
        assertTrue(frame.isData)
        assertEquals(240, frame.payload.size)
    }
}
