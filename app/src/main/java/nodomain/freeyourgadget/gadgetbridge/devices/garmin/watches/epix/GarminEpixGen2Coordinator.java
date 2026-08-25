package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.epix;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminEpixGen2Coordinator extends GarminWatchCoordinator {
    @Override
    public boolean isExperimental() {
        // Not tested, and the supported device name below is unconfirmed
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // TODO: unconfirmed device name. The older unrelated "epix" (2015) already broadcasts
        //  the literal string "EPIX", so we assume lowercase here. In any case, the product
        //  number has priority.
        return Pattern.compile("^epix( - \\d+mm)?$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_epix_gen2;
    }
}
