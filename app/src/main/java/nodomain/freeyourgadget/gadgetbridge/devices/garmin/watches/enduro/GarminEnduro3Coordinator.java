package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.enduro;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminEnduro3Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Enduro 3$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_enduro_3;
    }
}
