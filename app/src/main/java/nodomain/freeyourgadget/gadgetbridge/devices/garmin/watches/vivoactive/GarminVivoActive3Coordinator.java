package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminVivoActive3Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        // The report on matrix did not include a space, but let's make it
        // optional just in case
        return Pattern.compile("^vívoactive *3$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivoactive_3;
    }

    @Override
    public boolean supportsTrainingLoad() {
        return false;
    }
}
