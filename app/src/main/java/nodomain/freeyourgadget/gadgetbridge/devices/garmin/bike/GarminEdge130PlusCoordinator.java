package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminEdge130PlusCoordinator extends GarminBikeComputerCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Edge 130 Plus$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_edge_130_plus;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 0; // does not seem to report the battery %
    }

    @Override
    public boolean supportsMusicInfo() {
        return false;
    }
}
