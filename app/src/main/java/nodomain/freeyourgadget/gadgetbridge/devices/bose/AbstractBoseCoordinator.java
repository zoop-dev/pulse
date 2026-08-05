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
package nodomain.freeyourgadget.gadgetbridge.devices.bose;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.bose.BoseSupport;

public abstract class AbstractBoseCoordinator extends AbstractBLClassicDeviceCoordinator {
    /**
     * Preference key holding this model's noise cancelling level.
     */
    public abstract String getNoiseCancellingPrefKey();

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return BoseSupport.class;
    }

    @Override
    public String getManufacturer() {
        return "Bose";
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        return new BatteryConfig[]{
                new BatteryConfig(
                        0,
                        GBDevice.BATTERY_ICON_DEFAULT,
                        GBDevice.BATTERY_LABEL_DEFAULT,
                        25,
                        100
                )
        };
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_headphones);
        for (final int screen : getSupportedDeviceSpecificSettings(device)) {
            deviceSpecificSettings.addRootScreen(screen);
        }
        return deviceSpecificSettings;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.HEADPHONES;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_headphones;
    }
}
