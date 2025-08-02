package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminVenuSq2Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Venu Sq 2$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_venu_sq_2;
    }

    @Override
    public boolean supportsTrainingLoad() {
        return false;
    }
}
