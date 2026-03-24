package nodomain.freeyourgadget.gadgetbridge.devices.redmibuds;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds.RedmiBuds6ActiveDeviceSupport;

public class RedmiBuds6ActiveCoordinator extends AbstractRedmiBudsCoordinator {

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmi_buds_6_active;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Redmi Buds 6 Active");
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return RedmiBuds6ActiveDeviceSupport.class;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds6active_sound);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds6active_gestures);
        deviceSpecificSettings.addSubScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new RedmiBudsSettingsCustomizer(device);
    }
}
