package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

import java.util.regex.Pattern;

public class GarminInstinctCrossoverCoordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct Crossover$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_crossover;
    }
}
