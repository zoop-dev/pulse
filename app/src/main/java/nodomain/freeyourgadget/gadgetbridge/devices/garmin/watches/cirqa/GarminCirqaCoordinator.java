package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.cirqa;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminCirqaCoordinator extends GarminWatchCoordinator {
    @Override
    public boolean isExperimental() {
        // Largely untested, workout sync does not seem to work properly
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^CIRQA Smart Band$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_cirqa;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_default;
    }

    @Override
    public boolean defaultNewSyncProtocol() {
        return true;
    }
}
