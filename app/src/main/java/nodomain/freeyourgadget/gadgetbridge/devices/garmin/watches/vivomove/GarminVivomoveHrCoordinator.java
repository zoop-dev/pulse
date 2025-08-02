package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminVivomoveHrCoordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívomove HR$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivomove_hr;
    }

    @Override
    public boolean supportsTrainingLoad() {
        return false;
    }
}
