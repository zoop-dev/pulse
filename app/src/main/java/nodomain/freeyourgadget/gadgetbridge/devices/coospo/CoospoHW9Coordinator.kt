package nodomain.freeyourgadget.gadgetbridge.devices.coospo

import nodomain.freeyourgadget.gadgetbridge.R
import java.util.regex.Pattern

class CoospoHW9Coordinator: CoospoHeartRateCoordinator() {
    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^HW9 [0-9]{5}$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_coospo_hw9
    }
}
