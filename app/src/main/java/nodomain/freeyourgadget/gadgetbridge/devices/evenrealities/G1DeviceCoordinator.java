package nodomain.freeyourgadget.gadgetbridge.devices.evenrealities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities.G1Constants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities.G1DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;

/**
 * Coordinator for the Even Realities G1 smart glasses. Describes the supported capabilities of the
 * device.
 * <p>
 * This class partners with G1DeviceSupport.java and G1PairingActivity.java
 */
public class G1DeviceCoordinator extends AbstractBLEDeviceCoordinator {

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return G1DeviceSupport.class;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return G1PairingActivity.class;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // eg. G1_45_L_F2333, G1_63_R_04935.
        // Note that the G1_XX_L_YYYYY will have a corresponding G1_XX_R_ZZZZZ. The XX will match,
        // but the trailing 5 characters will not.
        return Pattern.compile("Even G1_\\d\\d_[L|R]_\\w+");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_even_realities_g1;
    }

    @Override
    public String getManufacturer() {
        return "Even Realities";
    }

    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_even_realities_g1;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_LAZY;
    }

    private int getDeviceIndexForAddress(String address) {
        return GBApplication.getDeviceSpecificSharedPrefs(address)
                            .getInt(G1Constants.Side.getIndexKey(),
                                    G1Constants.Side.INVALID.getDeviceIndex());
    }

    private GBDevice createDevice(String address, String name, String alias, String parentFolder,
                                  DeviceType deviceType) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(address);
        int deviceIndex = getDeviceIndexForAddress(address);
        if (deviceIndex == G1Constants.Side.INVALID.getDeviceIndex()) {
            // Still bonding, the devices have not been linked up. Just use the device directly.
            return new GBDevice(address, name, null, parentFolder, deviceType);
        } else if (deviceIndex == G1Constants.Side.RIGHT.getDeviceIndex()) {
            return null;
        }

        // Pull the right side info out of the prefs.
        String rightName = prefs.getString(G1Constants.Side.RIGHT.getNameKey(), "");
        String rightAddress = prefs.getString(G1Constants.Side.RIGHT.getAddressKey(), "");

        // Create the device with the left name and address.
        GBDevice gbDevice = new GBDevice(address, name, alias, parentFolder, deviceType);

        // Put the information of the right device into the left device. This will allow the multi
        // BLE device to manage both connections.
        gbDevice.addDeviceInfo(
                new GenericItem(G1Constants.Side.RIGHT.getNameKey(), rightName));
        gbDevice.addDeviceInfo(
                new GenericItem(G1Constants.Side.RIGHT.getAddressKey(), rightAddress));

        for (BatteryConfig batteryConfig : getBatteryConfig(gbDevice)) {
            gbDevice.setBatteryIcon(batteryConfig.getBatteryIcon(),
                                    batteryConfig.getBatteryIndex());
            gbDevice.setBatteryLabel(batteryConfig.getBatteryLabel(),
                                     batteryConfig.getBatteryIndex());
        }
        return gbDevice;
    }

    @Override
    public GBDevice createDevice(GBDeviceCandidate candidate, DeviceType deviceType) {
        return createDevice(candidate.getMacAddress(), candidate.getName(),
                            G1Constants.getNameFromFullName(candidate.getName()), null,
                            deviceType);
    }

    @Override
    public GBDevice createDevice(Device dbDevice, DeviceType deviceType) {
        return createDevice(dbDevice.getIdentifier(), dbDevice.getName(), dbDevice.getAlias(),
                            dbDevice.getParentFolder(), deviceType);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device,
                                @NonNull DaoSession session) throws GBException {
        // The main device is bonded as the left. Pull out the right device and delete it too if it
        // is present.
        ItemWithDetails right_name =
                gbDevice.getDeviceInfo(G1Constants.Side.RIGHT.getNameKey());
        ItemWithDetails right_address =
                gbDevice.getDeviceInfo(G1Constants.Side.RIGHT.getAddressKey());
        if (right_name != null && !right_name.getDetails().isEmpty() && right_address != null &&
            !right_address.getDetails().isEmpty()) {
            GBDevice rightDevice =
                    new GBDevice(right_address.getDetails(), right_name.getDetails(), null,
                                 gbDevice.getParentFolder(), gbDevice.getType());
            super.deleteDevice(rightDevice, true);
            BondingUtil.Unpair(GBApplication.getContext(), rightDevice.getAddress());
        }
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        BatteryConfig battery1 = new BatteryConfig(0, GBDevice.BATTERY_ICON_DEFAULT, R.string.even_realities_left_lens);
        BatteryConfig battery2 = new BatteryConfig(1, GBDevice.BATTERY_ICON_DEFAULT, R.string.even_realities_right_lens);
        BatteryConfig battery3 = new BatteryConfig(2, R.drawable.level_list_even_realities_g1_case_battery, R.string.battery_case);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public List<DeviceCardAction> getCustomActions() {
        return Collections.singletonList(new DeviceCardAction() {
            @Override
            public int getIcon(GBDevice device) {
                return R.drawable.ic_dnd;
            }

            @Override
            public String getDescription(final GBDevice device, final Context context) {
                return context.getString(R.string.silent_mode);
            }

            @Override
            public void onClick(final GBDevice device, final Context context) {
                final Intent intent = new Intent(G1Constants.INTENT_TOGGLE_SILENT_MODE);
                intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                intent.setPackage(context.getPackageName());
                context.sendBroadcast(intent);
            }
        });
    }


    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        if (device.isConnected()) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_screen_on_on_notifications);
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_screen_on_on_notifications_timeout);
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_even_realities_g1_display);
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_timeformat);

            final List<Integer> developer =
                    deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
            developer.add(R.xml.devicesettings_header_system);
            developer.add(R.xml.devicesettings_debug_logs_toggle);
        }
        return deviceSpecificSettings;
    }

    ////////////////////////////////////////////////
    // Gadget bridge feature support declarations //
    ////////////////////////////////////////////////

    @Override
    public boolean supportsWeather(final GBDevice device) {
        return true;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.SMART_GLASSES;
    }
}
