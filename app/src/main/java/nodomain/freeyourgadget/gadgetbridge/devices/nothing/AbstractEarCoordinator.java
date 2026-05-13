/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.nothing;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.nothing.Ear1Support;

public abstract class AbstractEarCoordinator extends AbstractBLClassicDeviceCoordinator {
    @Override
    public String getManufacturer() {
        return "Nothing";
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.EARBUDS;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_tws_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_nothing_ear_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_nothing_ear_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return Ear1Support.class;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        final List<Integer> audio = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.AUDIO);
        audio.add(R.xml.devicesettings_nothing_ear1);
        if (!getEqualizerPresets().isEmpty()) {
            audio.add(R.xml.devicesettings_nothing_equalizer);
        }
        if (supportsUltraBass()) {
            audio.add(R.xml.devicesettings_nothing_ultra_bass);
        }
        if (supportsTouchOptions()) {
            final List<Integer> touchOptions = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS);
            touchOptions.add(R.xml.devicesettings_cmf_buds_touch_options);
        }
        if (supportsSpatialAudio()) {
            audio.add(R.xml.devicesettings_nothing_spatial_audio);
        }
        if (supportsLowLatency()) {
            final List<Integer> connection = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CONNECTION);
            connection.add(R.xml.devicesettings_headphones_low_latency);
        }
        final List<Integer> callsAndNotif = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        callsAndNotif.add(R.xml.devicesettings_headphones);
        return deviceSpecificSettings;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_nothingear;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new EarSettingsCustomizer();
    }

    public abstract boolean incrementCounter();

    public boolean supportsInEarDetection() {
        return true;
    }

    public abstract boolean supportsLightAnc();

    public abstract boolean supportsTransparency();

    public abstract boolean supportsAdaptiveAnc();

    public abstract boolean supportsMediumAnc();

    public boolean supportsLowLatency() {
        return false;
    }

    public List<NothingEqualizer> getEqualizerPresets() {
        return Collections.emptyList();
    }

    public boolean supportsUltraBass() {
        return false;
    }

    public boolean supportsTouchOptions() {
        return false;
    }

    public boolean supportsSpatialAudio() { return false; }
}
