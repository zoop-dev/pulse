/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.aawireless;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.aawireless.AAWirelessPrefs;
import nodomain.freeyourgadget.gadgetbridge.service.devices.aawireless.AAWirelessSupport;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class AAWirelessCoordinator extends AbstractBLClassicDeviceCoordinator {
    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // AAW 1 - AAWireless-xUkL1YH0
        // AAW 2 - Normal mode: AAWireless-12345abc
        // AAW 2 - Dongle mode: AndroidAuto-AAW12345abc
        return Pattern.compile("^(AAWireless-|AndroidAuto-AAW)[0-9a-zA-Z]+$");
    }

    @Override
    public String getManufacturer() {
        return "AAWireless";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return AAWirelessSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_aawireless;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_car;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 0;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        return new BatteryConfig[0];
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);

        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_aawireless_paired_phones);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_country);
        if (devicePrefs.getBoolean(AAWirelessPrefs.PREF_HAS_BUTTON, false)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_aawireless_button);
        }
        // TODO deviceSpecificSettings.addRootScreen(R.xml.devicesettings_aawireless_auto_standby);

        final List<Integer> advanced = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.ADVANCED_SETTINGS);
        advanced.add(R.xml.devicesettings_aawireless_advanced);

        final List<Integer> connection = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CONNECTION);
        connection.add(R.xml.devicesettings_wifi_frequency_channel);

        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new AAWirelessSettingsCustomizer();
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.UNKNOWN;
    }
}
