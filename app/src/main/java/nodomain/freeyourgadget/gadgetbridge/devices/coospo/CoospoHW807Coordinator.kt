package nodomain.freeyourgadget.gadgetbridge.devices.coospo

import nodomain.freeyourgadget.gadgetbridge.R
import java.util.regex.Pattern

/// #5025
class CoospoHW807Coordinator: CoospoHeartRateCoordinator() {
    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^(COOSPO )?HW807$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_coospo_h6
    }
}
