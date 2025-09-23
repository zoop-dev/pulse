package nodomain.freeyourgadget.gadgetbridge.devices.earfun;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airs.EarFunAirSDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airs.EarFunAirSSettingsCustomizer;

public class EarFunAirSCoordinator extends AbstractEarFunCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_earfun_air_s;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        if (candidate.getName().startsWith("EarFun Air S")) {
            return true;
        }

        // can't only check with name, because the device name can be changed
        // via the device settings, so we use some of the UUIDs available on the device
        // and the mac address prefix to hopefully detect this model reliably
        String[] uuids = {
                "00001101-0000-1000-8000-00805f9b34fb",
                "0000111e-0000-1000-8000-00805f9b34fb",
                "0000110b-0000-1000-8000-00805f9b34fb",
                "0000110e-0000-1000-8000-00805f9b34fb",
                "0000eb04-d102-11e1-9b23-00025b00a5a5",
                "0000eb06-d102-11e1-9b23-00025b00a5a5",
                "0000eb07-d102-11e1-9b23-00025b00a5a5",
                "0000eb05-d102-11e1-9b23-00025b00a5a5"};

        boolean allServicesSupported = Arrays.stream(uuids)
                .map(UUID::fromString)
                .map(candidate::supportsService).allMatch(b -> b);

        boolean macAddressMatches = candidate.getMacAddress().toUpperCase().startsWith("A8:99:DC");

        return allServicesSupported && macAddressMatches;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return EarFunAirSDeviceSupport.class;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 2;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_nothing_ear_l, R.string.left_earbud);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_nothing_ear_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        // Category Audio Experience
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_header_audio_experience);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_6_band_equalizer);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_air_s_sound_control);

        // Category Audio Quality & Connectivity
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_header_connectivity);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_air_s_audio_quality);

        // Category System Settings
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_header_system_settings);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_air_s_gestures);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_device_name);
        final List<Integer> callsAndNotif = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        callsAndNotif.add(R.xml.devicesettings_headphones);
        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new EarFunAirSSettingsCustomizer(device);
    }
}
