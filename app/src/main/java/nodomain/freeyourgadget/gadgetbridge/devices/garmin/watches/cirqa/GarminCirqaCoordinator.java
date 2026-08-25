package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.cirqa;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminCirqaCoordinator extends GarminWatchCoordinator {
    @Override
    public boolean isExperimental() {
        // Largely untested, workout sync does not seem to work properly
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^CIRQA Smart Band$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_cirqa;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_default;
    }

    @Override
    public boolean defaultNewSyncProtocol() {
        return true;
    }

    @Override
    public boolean supportsWeather(@NonNull final GBDevice device) {
        // No screen
        return false;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsCalendarEvents(@NonNull final GBDevice device) {
        // No screen
        return false;
    }

    @Override
    public boolean supportsAlarmSounds(@NonNull final GBDevice device) {
        // No speaker
        return false;
    }

    @Override
    public boolean supportsAlarmBacklight(@NonNull final GBDevice device) {
        // No screen
        return false;
    }

    @Override
    public boolean supportsAlarmTitlePresets(@NonNull final GBDevice device) {
        // No screen
        return false;
    }
}
