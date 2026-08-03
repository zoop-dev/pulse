package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml

import nodomain.freeyourgadget.gadgetbridge.util.GB

/**
 * A decoded (or to-be-encoded) packet - the envelope only. What a  given
 * cmdSet/cmd's payload bytes *mean* is deliberately not this class's
 * concern; keep per-command payload (de)serialization in its own layer, the
 * same way the envelope is decoupled from BLE/transport specifics here.
 */
data class DumlPacket(
    val sender: DumlAddress,
    val receiver: DumlAddress,
    val seq: Int,
    val encryption: DumlEncryption = DumlEncryption.NONE,
    val ack: DumlAck = DumlAck.NONE,
    val packetType: DumlPacketType = DumlPacketType.REQUEST,
    val cmdSet: Int,
    val cmd: Int,
    val payload: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DumlPacket) return false
        return sender == other.sender &&
                receiver == other.receiver &&
                seq == other.seq &&
                encryption == other.encryption &&
                ack == other.ack &&
                packetType == other.packetType &&
                cmdSet == other.cmdSet &&
                cmd == other.cmd &&
                payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = sender.hashCode()
        result = 31 * result + receiver.hashCode()
        result = 31 * result + seq
        result = 31 * result + encryption.hashCode()
        result = 31 * result + ack.hashCode()
        result = 31 * result + packetType.hashCode()
        result = 31 * result + cmdSet
        result = 31 * result + cmd
        result = 31 * result + payload.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "DumlPacket(%s->%s, seq=%d, %s, cmdSet=0x%02x, cmd=0x%02x, payload=%s)".format(
            sender, receiver, seq, packetType, cmdSet, cmd, GB.hexdump(payload)
        )
    }
}
