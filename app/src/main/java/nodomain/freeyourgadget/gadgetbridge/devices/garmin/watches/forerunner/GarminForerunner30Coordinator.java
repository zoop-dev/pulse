package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminForerunner30Coordinator extends GarminWatchCoordinator {
    @Override
    public boolean isExperimental() {
        // Not tested, and the supported device name below is unconfirmed
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // TODO: unconfirmed device name
        return Pattern.compile("^Forerunner 30$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_forerunner_30;
    }

    @Override
    public boolean supportsTrainingLoad(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsSpo2(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsBodyEnergy(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsStressMeasurement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHrvMeasurement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsRespiratoryRate(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsVO2MultiSport(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsWeather(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull final GBDevice device) {
        return false;
    }
}
