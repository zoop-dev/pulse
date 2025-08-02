package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminVivomoveSportCoordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívomove Sport$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivomove_sport;
    }

    @Override
    public boolean supportsTrainingLoad() {
        return false;
    }
}
