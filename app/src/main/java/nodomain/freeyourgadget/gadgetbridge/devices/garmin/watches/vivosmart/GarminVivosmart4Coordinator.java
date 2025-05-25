package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivosmart;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;

public class GarminVivosmart4Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívosmart 4$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivosmart_4;
    }
}
