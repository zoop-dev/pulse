package nodomain.freeyourgadget.gadgetbridge.devices.earfun;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airpro4.EarFunAirPro4DeviceSupport;

public class EarFunAirPro4Coordinator extends AbstractEarFunCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_earfun_air_pro_4;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        if (candidate.getName().startsWith("EarFun Air Pro 4")) {
            return true;
        }

        // can't only check with name, because the device name can be changed
        // via the device settings, so we use some of the UUIDs available on the device
        // and the mac address prefix to hopefully detect this model reliable
        String UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb";
        String[] uuidPrefixes = {"0000180f", "0000180a"};

        boolean allServicesSupported = Arrays.stream(uuidPrefixes)
                .map(uuidPrefix -> UUID.fromString(uuidPrefix + UUID_SUFFIX))
                .map(candidate::supportsService).allMatch(b -> b);

        boolean macAddressMatches = candidate.getMacAddress().toUpperCase().startsWith("70:5A:6F");

        return allServicesSupported && macAddressMatches;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return EarFunAirPro4DeviceSupport.class;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        BatteryConfig battery1 = new BatteryConfig(2, R.drawable.ic_buds_pro_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(0, R.drawable.ic_nothing_ear_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(1, R.drawable.ic_nothing_ear_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_air_pro_4_headphones);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_air_pro_4_gestures);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_device_name);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_earfun_10_band_equalizer);
        return deviceSpecificSettings;
    }
}
