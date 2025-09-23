package nodomain.freeyourgadget.gadgetbridge.devices.coospo

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.regex.Pattern

/// #5025
class CoospoHW807Coordinator: CoospoHeartRateCoordinator() {
    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^(COOSPO )?HW807$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_coospo_hw807
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.CHEST_STRAP
    }
}
