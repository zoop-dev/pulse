package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import android.content.Intent
import android.os.Build
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

fun Intent.getDevice(): GBDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(GBDevice.EXTRA_DEVICE, GBDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(GBDevice.EXTRA_DEVICE)
    }
}
