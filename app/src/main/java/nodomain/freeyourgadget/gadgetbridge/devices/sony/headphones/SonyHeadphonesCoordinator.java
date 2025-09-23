/*  Copyright (C) 2021-2024 Damien Gaignon, Daniel Dakhno, José Rebelo,
    Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.SonyHeadphonesSupport;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public abstract class SonyHeadphonesCoordinator extends AbstractBLClassicDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(SonyHeadphonesCoordinator.class);

    @Override
    public String getManufacturer() {
        return "Sony";
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new SonyHeadphonesSettingsCustomizer(device);
    }

    @Override
    public boolean suggestUnbindBeforePair() {
        return false;
    }

    @Override
    public boolean supportsPowerOff(@NonNull final GBDevice device) {
        return supports(device, SonyHeadphonesCapabilities.PowerOffFromPhone);
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        if (supports(device, SonyHeadphonesCapabilities.BatterySingle)) {
            if (supports(device, SonyHeadphonesCapabilities.BatteryDual) || supports(device, SonyHeadphonesCapabilities.BatteryDual2)) {
                LOG.error("A device can't have both single and dual battery");
                return 0;
            } else if (supports(device, SonyHeadphonesCapabilities.BatteryCase)) {
                LOG.error("Devices with single battery + case are not supported by the protocol");
                return 0;
            }
        }

        int batteryCount = 0;

        if (supports(device, SonyHeadphonesCapabilities.BatterySingle)) {
            batteryCount += 1;
        }

        if (supports(device, SonyHeadphonesCapabilities.BatteryCase)) {
            batteryCount += 1;
        }

        if (supports(device, SonyHeadphonesCapabilities.BatteryDual) || supports(device, SonyHeadphonesCapabilities.BatteryDual2)) {
            batteryCount += 2;
        }

        return batteryCount;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        final List<BatteryConfig> batteries = new ArrayList<>(3);

        if (supports(device, SonyHeadphonesCapabilities.BatterySingle)) {
            batteries.add(new BatteryConfig(batteries.size(), GBDevice.BATTERY_ICON_DEFAULT, GBDevice.BATTERY_LABEL_DEFAULT, getBatteryDefaultLowThreshold(), getBatteryDefaultFullThreshold()));
        }

        if (supports(device, SonyHeadphonesCapabilities.BatteryCase)) {
            batteries.add(new BatteryConfig(batteries.size(), R.drawable.ic_tws_case, R.string.battery_case, getBatteryDefaultLowThreshold(), getBatteryDefaultFullThreshold()));
        }

        if (supports(device, SonyHeadphonesCapabilities.BatteryDual) || supports(device, SonyHeadphonesCapabilities.BatteryDual2)) {
            batteries.add(new BatteryConfig(batteries.size(), R.drawable.ic_galaxy_buds_l, R.string.left_earbud, getBatteryDefaultLowThreshold(), getBatteryDefaultFullThreshold()));
            batteries.add(new BatteryConfig(batteries.size(), R.drawable.ic_galaxy_buds_r, R.string.right_earbud, getBatteryDefaultLowThreshold(), getBatteryDefaultFullThreshold()));
        }

        return batteries.toArray(new BatteryConfig[0]);
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        if (supports(device, SonyHeadphonesCapabilities.AmbientSoundControl) || supports(device, SonyHeadphonesCapabilities.AmbientSoundControl2)) {
            if (supports(device, SonyHeadphonesCapabilities.WindNoiseReduction)) {
                deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_ambient_sound_control_wind_noise_reduction);
            } else if (supports(device, SonyHeadphonesCapabilities.NoNoiseCancelling)) {
                deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_ambient_sound_control_no_noise_cancelling);
            } else {
                deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_ambient_sound_control);
            }

            if (supports(device, SonyHeadphonesCapabilities.AncOptimizer)) {
                deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_anc_optimizer);
            }
        }

        if (supports(device, SonyHeadphonesCapabilities.AdaptiveVolumeControl)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_adaptive_volume_control);
        }

        if (supports(device, SonyHeadphonesCapabilities.SpeakToChatConfig)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_speak_to_chat_with_settings);
        } else if (supports(device, SonyHeadphonesCapabilities.SpeakToChatEnabled)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_speak_to_chat_simple);
        }

        addSettingsUnderHeader(deviceSpecificSettings, device, R.xml.devicesettings_header_other, new LinkedHashMap<>() {{
            put(SonyHeadphonesCapabilities.AudioSettingsOnlyOnSbcCodec, R.xml.devicesettings_sony_warning_wh1000xm3);
            put(SonyHeadphonesCapabilities.EqualizerSimple, R.xml.devicesettings_sony_headphones_equalizer);
            put(SonyHeadphonesCapabilities.EqualizerWithCustomBands, R.xml.devicesettings_sony_headphones_equalizer_with_custom_bands);
            put(SonyHeadphonesCapabilities.SoundPosition, R.xml.devicesettings_sony_headphones_sound_position);
            put(SonyHeadphonesCapabilities.SurroundMode, R.xml.devicesettings_sony_headphones_surround_mode);
            put(SonyHeadphonesCapabilities.AudioUpsampling, R.xml.devicesettings_sony_headphones_audio_upsampling);
            put(SonyHeadphonesCapabilities.Volume, R.xml.devicesettings_volume);
        }});

        final List<Integer> callsAndNotif = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        callsAndNotif.add(R.xml.devicesettings_headphones);

        addSettingsUnderHeader(deviceSpecificSettings, device, R.xml.devicesettings_header_system, new LinkedHashMap<>() {{
            put(SonyHeadphonesCapabilities.WideAreaTap, R.xml.devicesettings_sony_headphones_wide_area_tap);
            put(SonyHeadphonesCapabilities.ButtonModesLeftRight, R.xml.devicesettings_sony_headphones_button_modes_left_right);
            put(SonyHeadphonesCapabilities.AmbientSoundControlButtonMode, R.xml.devicesettings_sony_headphones_ambient_sound_control_button_modes);
            put(SonyHeadphonesCapabilities.QuickAccess, R.xml.devicesettings_sony_headphones_quick_access);
            put(SonyHeadphonesCapabilities.TouchSensorSingle, R.xml.devicesettings_sony_headphones_touch_sensor_single);
            put(SonyHeadphonesCapabilities.PauseWhenTakenOff, R.xml.devicesettings_sony_headphones_pause_when_taken_off);
            put(SonyHeadphonesCapabilities.AutomaticPowerOffWhenTakenOff, R.xml.devicesettings_automatic_power_off_when_taken_off);
            put(SonyHeadphonesCapabilities.AutomaticPowerOffByTime, R.xml.devicesettings_automatic_power_off_by_time);
            put(SonyHeadphonesCapabilities.VoiceNotifications, R.xml.devicesettings_sony_headphones_notifications_voice_guide);
        }});

        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_override_features);
        developer.add(R.xml.devicesettings_sony_headphones_protocol_version);
        developer.add(R.xml.devicesettings_sony_headphones_device_info);

        return deviceSpecificSettings;
    }

    public Set<SonyHeadphonesCapabilities> getCapabilities() {
        return Collections.emptySet();
    }

    public Set<SonyHeadphonesCapabilities> getCapabilities(final GBDevice device) {
        DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);
        final boolean overrideFeatures = devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_OVERRIDE_FEATURES_ENABLED, false);
        if (overrideFeatures) {
            final Set<String> stringList = devicePrefs.getStringSet(DeviceSettingsPreferenceConst.PREF_OVERRIDE_FEATURES_LIST, Collections.emptySet());
            return stringList.stream().map(SonyHeadphonesCapabilities::valueOf).collect(Collectors.toSet());
        }
        return getCapabilities();
    }

    public boolean supports(final GBDevice device, final SonyHeadphonesCapabilities capability) {
        return getCapabilities(device).contains(capability);
    }

    /**
     * Add the preference screens for capabilities under a header. The header is also only added if at least one capability is supported by the device.
     *
     * @param deviceSpecificSettings the device specific settings
     * @param header                 the header to add, if any capability supported
     * @param capabilities           the map of capability to preference screen
     */
    private void addSettingsUnderHeader(final DeviceSpecificSettings deviceSpecificSettings,
                                        final GBDevice device,
                                        final int header,
                                        final Map<SonyHeadphonesCapabilities, Integer> capabilities) {
        final Set<SonyHeadphonesCapabilities> supportedCapabilities = new HashSet<>(capabilities.keySet());
        for (SonyHeadphonesCapabilities capability : capabilities.keySet()) {
            if (!supports(device, capability)) {
                supportedCapabilities.remove(capability);
            }
        }

        if (supportedCapabilities.isEmpty()) {
            // None of the capabilities in the map are supported
            return;
        }

        deviceSpecificSettings.addRootScreen(header);

        for (Map.Entry<SonyHeadphonesCapabilities, Integer> capabilitiesSetting : capabilities.entrySet()) {
            if (supports(device, capabilitiesSetting.getKey())) {
                deviceSpecificSettings.addRootScreen(capabilitiesSetting.getValue());
            }
        }
    }

    public boolean preferServiceV2() {
        return false;
    }

    public int getBatteryDefaultLowThreshold() {
        return 20;
    }

    public int getBatteryDefaultFullThreshold() {
        return 100;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return SonyHeadphonesSupport.class;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_sony_overhead;
    }
}
