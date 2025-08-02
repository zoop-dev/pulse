package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminVivoActiveHrCoordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívoactive HR$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivoactive_hr;
    }

    @Override
    public boolean supportsTrainingLoad() {
        return false;
    }
}
