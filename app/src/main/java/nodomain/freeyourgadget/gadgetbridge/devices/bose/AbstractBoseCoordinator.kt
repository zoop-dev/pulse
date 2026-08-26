/*  Copyright (C) 2021-2024 Damien Gaignon, Daniel Dakhno, José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.bose

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.SeekBarSetting
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.bose.BoseSupport

abstract class AbstractBoseCoordinator : AbstractBLClassicDeviceCoordinator() {
    abstract fun getMaxAnc(): Int

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport> {
        return BoseSupport::class.java
    }

    override fun getManufacturer(): String {
        return "Bose"
    }

    override fun getBatteryConfig(device: GBDevice): Array<BatteryConfig> {
        return arrayOf(
            BatteryConfig(
                0,
                GBDevice.BATTERY_ICON_DEFAULT.toInt(),
                GBDevice.BATTERY_LABEL_DEFAULT.toInt(),
                25,
                100
            )
        )
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.HEADPHONES
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_headphones
    }

    override fun getDeviceSettings(device: GBDevice): DeviceSettingsSpec = deviceSettings {
        SeekBarSetting(
            key = DeviceSettingsPreferenceConst.PREF_QC35_NOISE_CANCELLING_LEVEL,
            title = R.string.prefs_active_noise_cancelling_level,
            icon = R.drawable.ic_noise_control_on,
            defaultValue = getMaxAnc(),
            max = getMaxAnc(),
            connectedOnly = true,
        )
        xmlScreen(
            DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS,
            R.xml.devicesettings_headphones,
            connectedOnly = false,
        )
    }
}
