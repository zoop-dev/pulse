package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.lily;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminLily2ActiveCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Lily 2 Active$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_lily_2_active;
    }
}
