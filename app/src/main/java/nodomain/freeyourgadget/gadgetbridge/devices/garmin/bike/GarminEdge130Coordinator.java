package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminEdge130Coordinator extends GarminBikeComputerCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Edge 130$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_edge_130;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 0; // does not seem to report the battery %
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsTrainingLoad(@NonNull final GBDevice device) {
        return false;
    }
}
