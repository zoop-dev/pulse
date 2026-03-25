package nodomain.freeyourgadget.gadgetbridge.devices.redmibuds;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds.RedmiBuds8ActiveDeviceSupport;

public class RedmiBuds8ActiveCoordinator extends AbstractRedmiBudsCoordinator {

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmi_buds_8_active;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("REDMI Buds 8 Active");
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return RedmiBuds8ActiveDeviceSupport.class;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds8active_sound);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds8active_gestures);
        deviceSpecificSettings.addSubScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new RedmiBuds8ActiveSettingsCustomizer(device);
    }
}
