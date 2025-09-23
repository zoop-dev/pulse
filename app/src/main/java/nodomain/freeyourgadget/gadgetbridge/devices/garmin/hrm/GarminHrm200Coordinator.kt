package nodomain.freeyourgadget.gadgetbridge.devices.garmin.hrm

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.regex.Pattern

/// #5110
class GarminHrm200Coordinator: GarminHrmCoordinator() {
    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^HRM 200$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_garmin_hrm_200
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.CHEST_STRAP
    }
}
