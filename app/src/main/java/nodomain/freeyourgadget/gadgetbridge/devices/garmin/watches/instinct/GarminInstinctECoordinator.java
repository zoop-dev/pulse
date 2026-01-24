package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminInstinctECoordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct E - \\d+mm$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_e;
    }

    @Override
    public boolean supportsTrainingLoad(@NonNull final GBDevice device) {
        return false;
    }
}
