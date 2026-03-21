package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.descent;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminDescentG1Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Descent G1$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_descent_g1;
    }
}
