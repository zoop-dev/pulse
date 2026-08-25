package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminEdgeExplore1000Coordinator extends GarminBikeComputerCoordinator {
    @Override
    public boolean isExperimental() {
        // Not tested, and the supported device name below is unconfirmed
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // TODO: unconfirmed device name
        return Pattern.compile("^Edge Explore 1000$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_edge_explore_1000;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        return 0; // does not seem to report the battery %
    }

    @Override
    public boolean supportsTrainingLoad(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsVO2Max(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsFindDevice(@NonNull final GBDevice device) {
        return false;
    }
}
