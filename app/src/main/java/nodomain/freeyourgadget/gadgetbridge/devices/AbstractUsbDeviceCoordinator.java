package nodomain.freeyourgadget.gadgetbridge.devices;

import android.content.SharedPreferences;

import java.util.EnumSet;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.ServiceDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public abstract class AbstractUsbDeviceCoordinator extends AbstractDeviceCoordinator {
    @Override
    protected void applyDefaultPreferences(final DevicePrefs devicePreferences, final SharedPreferences.Editor editor) {
        editor.putBoolean(DeviceSettingsPreferenceConst.PREFS_DEVICE_SUPPORT_CAN_RECONNECT, false);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.USB;
    }

    @Override
    public int getBondingStyle() {
        // FIXME: This only applies to BLE
        return BONDING_STYLE_NONE;
    }

    @Override
    public EnumSet<ServiceDeviceSupport.Flags> getInitialFlags() {
        return EnumSet.noneOf(ServiceDeviceSupport.Flags.class);
    }

    @Override
    public int[] getSupportedDebugSettings(final GBDevice device) {
        // stress test and busy checking bypass are not relevant for usb devices
        return new int[0];
    }
}
