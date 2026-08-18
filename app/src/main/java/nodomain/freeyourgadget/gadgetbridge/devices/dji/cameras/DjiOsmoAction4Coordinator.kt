package nodomain.freeyourgadget.gadgetbridge.devices.dji.cameras

import nodomain.freeyourgadget.gadgetbridge.R
import java.util.regex.Pattern

class DjiOsmoAction4Coordinator: AbstractDjiCameraCoordinator() {
    override fun getSupportedDeviceName(): Pattern {
        return Pattern.compile("^OsmoAction4-[0-9A-Z]{4}$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_dji_osmo_action_4
    }
}
