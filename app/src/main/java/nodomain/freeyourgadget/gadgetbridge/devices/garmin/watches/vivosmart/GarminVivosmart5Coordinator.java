package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivosmart;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminVivosmart5Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívosmart 5$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivosmart_5;
    }

    @Override
    public boolean supportsTrainingLoad() {
        return false;
    }
}
