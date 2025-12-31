package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import kotlin.math.roundToInt

fun Int.coerceIn(min: Int, max: Int, step: Int): Int {
    return ((this / step.toDouble()).roundToInt() * step).coerceIn(min, max)
}
