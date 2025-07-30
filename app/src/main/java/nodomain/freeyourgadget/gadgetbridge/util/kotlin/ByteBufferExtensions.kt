package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils
import java.nio.ByteBuffer

fun ByteBuffer.stringUntilNullTerminator(): String? {
    return StringUtils.untilNullTerminator(this)
}
