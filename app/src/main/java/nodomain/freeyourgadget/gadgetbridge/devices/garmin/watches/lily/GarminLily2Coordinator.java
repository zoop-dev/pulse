package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.lily;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminLily2Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Lily 2$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_lily_2;
    }

    @Override
    public boolean supportsTrainingLoad(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsVO2MultiSport(@NonNull final GBDevice device) {
        return false;
    }
}
