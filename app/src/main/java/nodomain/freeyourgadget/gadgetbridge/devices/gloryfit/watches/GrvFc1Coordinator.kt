package nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.watches

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.GloryFitCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.regex.Pattern

class GrvFc1Coordinator : GloryFitCoordinator() {
    override fun getManufacturer(): String {
        return "GRV"
    }

    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^FC1\\(ID-[0-9A-F]{4}\\)$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_grv_fc1
    }

    override fun getAlarmSlotCount(device: GBDevice): Int {
        // alarms from app are not supported
        return 0
    }
}
