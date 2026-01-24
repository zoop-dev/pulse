package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminForerunner165MusicCoordinator extends GarminWatchCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Forerunner 165 Music$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_forerunner_165_music;
    }

    @Override
    public boolean supportsTrainingLoad(@NonNull final GBDevice device) {
        return false;
    }
}
