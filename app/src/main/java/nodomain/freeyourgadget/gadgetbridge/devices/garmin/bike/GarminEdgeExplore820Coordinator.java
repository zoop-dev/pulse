package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminEdgeExplore820Coordinator extends GarminBikeComputerCoordinator {
    @Override
    public boolean isExperimental() {
        // Not tested, and the supported device name below is unconfirmed
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // TODO: unconfirmed device name
        return Pattern.compile("^Edge Explore 820$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_edge_explore_820;
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
