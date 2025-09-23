package nodomain.freeyourgadget.gadgetbridge.devices.shokz

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.regex.Pattern

class ShokzOpenSwimProCoordinator: ShokzCoordinator() {
    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^OpenSwim Pro by Shokz$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_shokz_openswim_pro
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.HEADPHONES
    }
}
