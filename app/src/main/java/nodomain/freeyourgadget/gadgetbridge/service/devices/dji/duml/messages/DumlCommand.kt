package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.messages

import io.kaitai.struct.ByteBufferKaitaiStream
import io.kaitai.struct.KaitaiStream
import io.kaitai.struct.KaitaiStruct
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlAck
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlAddress
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlCmdSet
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlPacket
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlPacketType
import nodomain.freeyourgadget.gadgetbridge.util.GB
import java.nio.BufferOverflowException

/**
 * One decoded DUML message. [payload] is whatever the registered decoder for
 * (cmdSet, cmd, packetType) produced - see [DECODERS] below.
 */
class DumlCommand(
    val cmdSet: Int,
    val cmd: Int,
    val packetType: DumlPacketType,
    val payload: Any,
) {
    override fun toString(): String {
        val payloadStr = (payload as? ByteArray)?.let { GB.hexdump(it) } ?: payload.toString()
        return "DumlCommand(cmdSet=0x%02x, cmd=0x%02x, %s, payload=%s)".format(cmdSet, cmd, packetType, payloadStr)
    }

    companion object {
        // Outer key is cmdSet, inner key is (cmd, packetType) - the two directions of an
        // exchange aren't guaranteed to share a payload shape, so a decoder registered for
        // one packetType might not match the other. One entry per cmdSet file; add the
        // new file's table here when it's created.
        private val DECODERS: Map<Int, Map<Pair<Int, DumlPacketType>, (ByteArray) -> Any>> = mapOf(
            DumlCmdSet.WIFI to WIFI_DECODERS,
        )

        fun decode(cmdSet: Int, cmd: Int, packetType: DumlPacketType, payload: ByteArray): DumlCommand =
            DumlCommand(cmdSet, cmd, packetType, DECODERS[cmdSet]?.get(cmd to packetType)?.invoke(payload) ?: payload)

        fun decode(packet: DumlPacket): DumlCommand =
            decode(packet.cmdSet, packet.cmd, packet.packetType, packet.payload)
    }
}

/**
 * Turns a generated Kaitai struct's stream constructor into a byte-array decoder: wraps the
 * bytes in a [ByteBufferKaitaiStream] and calls `_read()`.
 */
fun <T : KaitaiStruct.ReadOnly> decoderFor(factory: (KaitaiStream) -> T): (ByteArray) -> T =
    { bytes -> factory(ByteBufferKaitaiStream(bytes)).apply { _read() } }

/**
 * Serializes a generated Kaitai struct back to bytes, trimmed to what it actually wrote.
 */
fun KaitaiStruct.ReadWrite.encodeToBytes(): ByteArray {
    _check()
    var size = 64
    while (true) {
        val scratch = ByteArray(size)
        try {
            _write(ByteBufferKaitaiStream(scratch))
            return scratch.copyOf(_io().pos())
        } catch (e: BufferOverflowException) {
            size *= 2
        }
    }
}

/** Wraps an encoded Kaitai struct in a full [DumlPacket] envelope. */
fun KaitaiStruct.ReadWrite.toPacket(
    cmdSet: Int,
    cmd: Int,
    sender: DumlAddress,
    receiver: DumlAddress,
    seq: Int,
    ack: DumlAck = DumlAck.ACK_AFTER_EXEC,
    packetType: DumlPacketType = DumlPacketType.REQUEST,
): DumlPacket = DumlPacket(
    sender = sender,
    receiver = receiver,
    seq = seq,
    ack = ack,
    packetType = packetType,
    cmdSet = cmdSet,
    cmd = cmd,
    payload = encodeToBytes(),
)
