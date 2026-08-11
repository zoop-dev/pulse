package nodomain.freeyourgadget.gadgetbridge.devices.dji

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.ble.DjiBleSupport

abstract class AbstractDjiCameraCoordinator : AbstractBLEDeviceCoordinator() {
    override fun getManufacturer(): String {
        return "DJI"
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport> {
        return DjiBleSupport::class.java
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun getBondingStyle(): Int {
        return BONDING_STYLE_NONE
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_camera
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.CAMERA
    }

    override fun getDeviceSettings(device: GBDevice): DeviceSettingsSpec = deviceSettings {

    }
}
