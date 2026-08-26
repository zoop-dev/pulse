package nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.watches

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.GloryFitCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.regex.Pattern

class SwissPeakCoordinator : GloryFitCoordinator() {
    override fun getManufacturer(): String {
        return "GloryFit"
    }

    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^Swiss Peak-[0-9A-F]{4}$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_swiss_peak
    }

    override fun supportsSpo2(device: GBDevice): Boolean {
        // Fetching gets stuck
        return false
    }

    override fun getAlarmSlotCount(device: GBDevice): Int {
        // 8 slots, but alarms from app are not supported
        return 0
    }
}
