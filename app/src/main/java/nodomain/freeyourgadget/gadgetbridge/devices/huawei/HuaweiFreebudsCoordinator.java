/*  Copyright (C) 2024-2025 Martin.JM, Ilya Nikitenkov

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

package nodomain.freeyourgadget.gadgetbridge.devices.huawei;


import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public abstract class HuaweiFreebudsCoordinator extends AbstractBLClassicDeviceCoordinator implements HuaweiCoordinatorSupplier {

    private final HuaweiCoordinator huaweiCoordinator = new HuaweiCoordinator(this);
    private GBDevice gbDevice;

    public HuaweiFreebudsCoordinator() {
        huaweiCoordinator.setTransactionCrypted(false);
    }

    @Override
    public String getManufacturer() {
        return "Huawei";
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_nothingear;
    }

    @Override
    public HuaweiCoordinator getHuaweiCoordinator() {
        return huaweiCoordinator;
    }

    @Override
    public HuaweiDeviceType getHuaweiType() {
        return HuaweiDeviceType.BR;
    }

    @Override
    public void setDevice(GBDevice gbDevice) {
        this.gbDevice = gbDevice;
    }

    @Override
    public GBDevice getDevice() {
        return this.gbDevice;
    }

    @Override
    public int getBondingStyle() {
        // TODO: Check if correct
        return BONDING_STYLE_ASK;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(GBDevice device) {
        BatteryConfig battery1 = new BatteryConfig(2, R.drawable.ic_tws_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(0, R.drawable.ic_nothing_ear_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(1, R.drawable.ic_nothing_ear_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    public Set<HuaweiHeadphonesCapabilities> getCapabilities() {
        return Collections.emptySet();
    }

    public Set<HuaweiHeadphonesCapabilities> getCapabilities(final GBDevice device) {
        DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);
        final boolean overrideFeatures = devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_OVERRIDE_FEATURES_ENABLED, false);
        if (overrideFeatures) {
            final Set<String> stringList = devicePrefs.getStringSet(DeviceSettingsPreferenceConst.PREF_OVERRIDE_FEATURES_LIST, Collections.emptySet());
            return stringList.stream().map(HuaweiHeadphonesCapabilities::valueOf).collect(Collectors.toSet());
        }
        return getCapabilities();
    }

    public boolean supports(final GBDevice device, final HuaweiHeadphonesCapabilities capability) {
        return getCapabilities(device).contains(capability);
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(GBDevice device) {
        DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        if (supports(device, HuaweiHeadphonesCapabilities.InEarDetection)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_huawei_headphones_in_ear_detection);
        }
        if (supports(device, HuaweiHeadphonesCapabilities.AudioModes)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_huawei_headphones_audio_modes);
        }
        if (supports(device, HuaweiHeadphonesCapabilities.AudioModes)
                && supports(device, HuaweiHeadphonesCapabilities.NoiseCancellationModes)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_huawei_headphones_noise_cancelation_modes);
        }
        if (supports(device, HuaweiHeadphonesCapabilities.AudioModes)
                && supports(device, HuaweiHeadphonesCapabilities.VoiceBoost)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_huawei_headphones_voice_boost);
        }
        if (supports(device, HuaweiHeadphonesCapabilities.BetterAudioQuality)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_huawei_headphones_better_audio_quality);
        }
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_headphones);
        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new HuaweiFreebudsSettingsCustomizer(device);
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.EARBUDS;
    }
}
