package nodomain.freeyourgadget.gadgetbridge.service.devices.shokz

data class ShokzMessage(
    val command: ShokzCommand,
    val args: ByteArray = byteArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShokzMessage

        if (command != other.command) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = command.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}
