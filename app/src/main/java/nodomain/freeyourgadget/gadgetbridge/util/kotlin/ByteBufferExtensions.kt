package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils
import java.nio.ByteBuffer
import kotlin.math.pow

fun ByteBuffer.stringUntilNullTerminator(): String? {
    return StringUtils.untilNullTerminator(this)
}

fun ByteBuffer.getSFloat(): Float {
    val raw = getShort().toInt() and 0xFFFF

    var mantissa = raw and 0x0FFF
    if (mantissa >= 0x0800) {
        mantissa -= 0x1000
    }

    var exponent = (raw shr 12) and 0x0F
    if (exponent >= 0x08) {
        exponent -= 0x10
    }

    return (mantissa * 10.0.pow(exponent.toDouble())).toFloat()
}
