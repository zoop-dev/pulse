package nodomain.freeyourgadget.gadgetbridge.devices.redmibuds;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds.RedmiBuds3ProDeviceSupport;

public class RedmiBuds3ProCoordinator extends AbstractRedmiBudsCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_redmi_buds_3_pro;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Redmi Buds 3 Pro");
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return RedmiBuds3ProDeviceSupport.class;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds3pro_headphones);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds3pro_sound);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_redmibuds3pro_gestures);
        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new RedmiBudsSettingsCustomizer(device);
    }
}
