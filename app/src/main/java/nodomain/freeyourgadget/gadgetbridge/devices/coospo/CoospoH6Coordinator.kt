package nodomain.freeyourgadget.gadgetbridge.devices.coospo

import nodomain.freeyourgadget.gadgetbridge.R
import java.util.regex.Pattern

/// #5025
class CoospoH6Coordinator: CoospoHeartRateCoordinator() {
    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^H6M [0-9]{5}$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_coospo_h6
    }
}
