package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.epix;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminEpixProCoordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^EPIX PRO - \\d+mm$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_epix_pro;
    }
}
