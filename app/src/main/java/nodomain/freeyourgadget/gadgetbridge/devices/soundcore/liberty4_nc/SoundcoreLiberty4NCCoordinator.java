package nodomain.freeyourgadget.gadgetbridge.devices.soundcore.liberty4_nc;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty.SoundcoreLiberty4NCDeviceSupport;

public class SoundcoreLiberty4NCCoordinator extends AbstractBLClassicDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_soundcore_liberty4_nc;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_galaxy_buds;
    }

    @Override
    public String getManufacturer() {
        return "Anker";
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("soundcore Liberty 4 NC");
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_NONE;
    }


    @Override
    public int getBatteryCount(final GBDevice device) {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_buds_pro_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_nothing_ear_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_nothing_ear_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS);
        deviceSpecificSettings.addSubScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS, R.xml.devicesettings_sony_headphones_ambient_sound_control_button_modes);
        deviceSpecificSettings.addSubScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS, R.xml.devicesettings_soundcore_liberty_touch_options);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_soundcore_liberty);
        return deviceSpecificSettings;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.EARBUDS;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return SoundcoreLiberty4NCDeviceSupport.class;
    }
}
