package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminForerunner245Coordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Forerunner 245$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_forerunner_245;
    }

    @Override
    public boolean supportsHrvMeasurement(@NonNull final GBDevice device) {
        return false;
    }
}
