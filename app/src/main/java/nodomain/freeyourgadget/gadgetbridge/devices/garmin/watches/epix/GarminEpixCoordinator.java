package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.epix;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminEpixCoordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^EPIX$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_epix;
    }
}
