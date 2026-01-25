package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminEdge840Coordinator extends GarminBikeComputerCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Edge 840$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_edge_840;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 0; // Unconfirmed: does not seem to report the battery %
    }
}
