package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu;

import android.content.SharedPreferences;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class GarminVenuX1Coordinator extends GarminWatchCoordinator {
    @Override
    public GBDevice createDevice(final GBDeviceCandidate candidate, final DeviceType deviceType) {
        final GBDevice gbDevice = super.createDevice(candidate, deviceType);
        final DevicePrefs devicePreferences = GBApplication.getDevicePrefs(gbDevice);
        final SharedPreferences.Editor editor = devicePreferences.getPreferences().edit();

        // #5021 - Venu X1 misses a lot of files without the new sync protocol
        editor.putBoolean("new_sync_protocol", true);

        editor.apply();

        return gbDevice;
    }

    @Override
    public boolean isExperimental() {
        // #5021 - potential pairing issues
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Venu X1$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_venu_x1;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_amazfit_bip;
    }
}
