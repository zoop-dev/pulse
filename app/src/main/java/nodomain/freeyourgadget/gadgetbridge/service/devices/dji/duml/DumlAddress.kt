package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml

/**
 * A DUML sender/receiver address: a 5-bit module type (bits 0-4) plus a
 * 3-bit index (bits 5-7).
 */
data class DumlAddress(val type: Int, val index: Int = 0) {
    fun toByte(): Byte = (((index and 0x07) shl 5) or (type and 0x1F)).toByte()

    override fun toString(): String {
        val name = TYPE_NAMES[type]
        return if (index == 0) {
            name ?: "0x%02x".format(type)
        } else {
            "${name ?: "0x%02x".format(type)}[$index]"
        }
    }

    companion object {
        const val TYPE_INVALID = 0x00
        const val TYPE_APP = 0x02
        const val TYPE_WIFI = 0x07

        val APP = DumlAddress(TYPE_APP)
        val WIFI = DumlAddress(TYPE_WIFI)

        // For logging only - not exhaustive, unknown types just print as hex.
        private val TYPE_NAMES = mapOf(
            TYPE_INVALID to "INVALID",
            TYPE_APP to "APP",
            TYPE_WIFI to "WIFI",
        )

        fun fromByte(b: Byte): DumlAddress {
            val v = b.toInt() and 0xFF
            return DumlAddress(type = v and 0x1F, index = (v shr 5) and 0x07)
        }
    }
}
