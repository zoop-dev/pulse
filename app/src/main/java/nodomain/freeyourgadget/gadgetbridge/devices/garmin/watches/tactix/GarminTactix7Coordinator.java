package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.tactix;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminTactix7Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^tactix 7$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_tactix_7;
    }
}
