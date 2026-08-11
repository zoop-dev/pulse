package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml

import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.DecodeResult
import org.slf4j.LoggerFactory

/**
 * Accumulates raw bytes across multiple notifications and yields
 * complete [DumlPacket]s as soon as they're fully available. Handles both
 * one frame split across several notifications, and several frames
 * delivered in a single notification.
 */
class DumlFrameReassembler(private val maxBufferSize: Int = 4096) {
    private var buffer = ByteArray(0)

    /** Feeds newly received bytes; returns any packets that could be fully decoded so far. */
    fun feed(bytes: ByteArray): List<DumlPacket> {
        buffer += bytes
        val packets = mutableListOf<DumlPacket>()
        var offset = 0

        while (offset < buffer.size) {
            when (val result = DumlCodec.decodeOne(buffer, offset)) {
                is DecodeResult.Success -> {
                    packets.add(result.content)
                    offset += result.bytesConsumed
                }
                is DecodeResult.Invalid -> {
                    // Resynchronize by dropping one byte, rather than getting
                    // stuck forever on corruption/garbage.
                    offset += 1
                }
                DecodeResult.NeedMoreData -> break
            }
        }

        buffer = if (offset > 0) buffer.copyOfRange(offset, buffer.size) else buffer

        if (buffer.size > maxBufferSize) {
            LOG.error("DUML frame buffer overflow, resetting")
            buffer = ByteArray(0)
        }

        return packets
    }

    fun reset() {
        buffer = ByteArray(0)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DumlFrameReassembler::class.java)
    }
}
