package nodomain.freeyourgadget.gadgetbridge.util.preferences

import android.text.InputFilter
import android.text.Spanned
import nodomain.freeyourgadget.gadgetbridge.BuildConfig

/**
 * help the user to input a properly formated MAC - see BluetoothAdapter.checkBluetoothAddress
 * for Bangle.js builds: also support pebble emulator addresses
 */
class MacAddressInputFilter : InputFilter {
    @Suppress("KotlinConstantConditions")
    override fun filter(
        source: CharSequence,
        start: Int, end: Int,
        dest: Spanned,
        dstart: Int, dend: Int
    ): CharSequence {
        val builder = StringBuilder()
        for (i in start..<end) {
            val c = source[i]
            if ((c in '0'..'9') || (c in 'A'..'F') || c == ':') {
                builder.append(c)
            } else if (c in 'a'..'f') {
                builder.append((c.code - 'a'.code + 'A'.code).toChar())
            } else if (BuildConfig.INTERNET_ACCESS) {
                builder.append(c)
            } else if (c == '-') {
                builder.append(':')
            }
        }
        return builder
    }
}
