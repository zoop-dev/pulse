package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The flow-control transport shared by the AUTH_DATA, CMD and RPT characteristics.
 * Every message (key-exchange PDU, encrypted command, encrypted telemetry report)
 * is carried as a short control frame (all starting with a leading `0x0000`, i.e. outside
 * the 1..64 chunk-sequence range) followed by one data frame.
 */
object XiaomiScooterTransport {
    /** Key-exchange PDU / SEND message type: the device's ECDH public key. */
    const val TYPE_DEV_PUBKEY = 0x03

    /** Key-exchange PDU / SEND message type: the login-confirmation token. */
    const val TYPE_DEV_LOGIN_INFO = 0x05

    /** SEND/DATA message type used on the CMD/RPT pipes for ordinary encrypted messages. */
    const val TYPE_APP_MESSAGE = 0x00

    val ACK: ByteArray = byteArrayOf(0x00, 0x00, 0x03, 0x00)

    /**
     * Written by the receiver of a multi-frame [ParsedFrame.Send] to signal it's ready for the
     * indexed data frames that follow. Every other channel only ever *parses* this (it's the
     * device's reply to our own SEND); the RPT channel is the one case where the roles reverse
     * and we have to write it ourselves, e.g. when the device announces a chunked telemetry push
     * (`SEND(type=0, frameCount=N)` for N>1) that doesn't fit the single-frame `00 00 02 TT` push.
     */
    val RCV_RDY: ByteArray = byteArrayOf(0x00, 0x00, 0x01, 0x01)

    /** `[00 00 00 TT NN 00]` — begin a message of the given type and frame count. */
    fun buildSend(type: Int, frameCount: Int = 1): ByteArray =
        byteArrayOf(0x00, 0x00, 0x00, type.toByte(), frameCount.toByte(), 0x00)

    /** `[index u16-LE][payload]` — a single (index=1) outgoing data frame. */
    fun buildDataFrame(payload: ByteArray, index: Int = 1): ByteArray {
        val buffer = ByteBuffer.allocate(2 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(index.toShort())
        buffer.put(payload)
        return buffer.array()
    }

    /**
     * Replays a device-originated key-exchange PDU (`00 00 04 ..`) back as the app-side PDU
     * (`00 00 05 ..`), verbatim except for the direction marker byte. These fixed, 0xf2-filled
     * PDUs precede the real ECDH exchange; their purpose is not understood, so we simply
     * replay them.
     */
    fun replayKeyExchangePdu(deviceFrame: ByteArray): ByteArray {
        require(deviceFrame.size >= 3 && (deviceFrame[2].toInt() and 0xff) == 0x04) {
            "Not a device key-exchange PDU"
        }
        val reply = deviceFrame.copyOf()
        reply[2] = 0x05
        return reply
    }

    @Suppress("ArrayInDataClass")
    sealed class ParsedFrame {
        data object RcvRdy : ParsedFrame()
        data object RcvOk : ParsedFrame()
        data object Ack : ParsedFrame()
        data class Send(val type: Int, val frameCount: Int) : ParsedFrame()
        data class Data(val type: Int, val payload: ByteArray) : ParsedFrame()
        data class KeyExchange(val fromDevice: Boolean, val isData: Boolean, val payload: ByteArray) : ParsedFrame()

        /**
         * `[index u16-LE, 1..64][payload]` — one chunk of a multi-frame message announced by a
         * preceding [Send] with `frameCount > 1`. Distinct from [Data] (`00 00 02 TT ...`), which
         * is the format used for a single, unprompted push with no [Send] preamble.
         */
        data class IndexedData(val index: Int, val payload: ByteArray) : ParsedFrame()
        data class Unknown(val raw: ByteArray) : ParsedFrame()
    }

    fun parse(frame: ByteArray): ParsedFrame {
        if (frame.size < 2) {
            return ParsedFrame.Unknown(frame)
        }
        val leading = (frame[0].toInt() and 0xff) or ((frame[1].toInt() and 0xff) shl 8)
        if (leading in 1..64) {
            return ParsedFrame.IndexedData(index = leading, payload = frame.copyOfRange(2, frame.size))
        }
        if (frame.size < 4 || frame[0].toInt() != 0 || frame[1].toInt() != 0) {
            return ParsedFrame.Unknown(frame)
        }
        return when (frame[2].toInt() and 0xff) {
            0x00 -> ParsedFrame.Send(
                type = frame[3].toInt() and 0xff,
                frameCount = if (frame.size > 4) frame[4].toInt() and 0xff else 1,
            )

            0x01 -> if ((frame[3].toInt() and 0xff) == 1) ParsedFrame.RcvRdy else ParsedFrame.RcvOk
            0x02 -> ParsedFrame.Data(type = frame[3].toInt() and 0xff, payload = frame.copyOfRange(4, frame.size))
            0x03 -> ParsedFrame.Ack
            0x04, 0x05 -> ParsedFrame.KeyExchange(
                fromDevice = (frame[2].toInt() and 0xff) == 0x04,
                isData = frame[3].toInt() == 0x01,
                payload = frame.copyOfRange(4, frame.size),
            )

            else -> ParsedFrame.Unknown(frame)
        }
    }
}
