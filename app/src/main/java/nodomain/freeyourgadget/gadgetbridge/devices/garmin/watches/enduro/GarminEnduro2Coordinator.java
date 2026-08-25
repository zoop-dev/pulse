package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.enduro;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminEnduro2Coordinator extends GarminWatchCoordinator {
    @Override
    public boolean isExperimental() {
        // Not tested, and the supported device name below is unconfirmed
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // TODO: unconfirmed device name
        return Pattern.compile("^Enduro 2$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_enduro_2;
    }
}
