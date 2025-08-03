package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminEdge540Coordinator extends GarminBikeComputerCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Edge 540$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_edge_540;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 0; // does not seem to report the battery %
    }
}
