package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminEdge25Coordinator extends GarminBikeComputerCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Edge 2x$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_edge_25;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 0; // Unconfirmed: does not seem to report the battery %
    }

    @Override
    public boolean supportsVO2Max(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsWeather(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return false;
    }
}
