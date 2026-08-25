package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminVivomoveCoordinator extends GarminWatchCoordinator {
    @Override
    public boolean isExperimental() {
        // Not tested, and the supported device name below is unconfirmed
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // TODO: unconfirmed device name
        return Pattern.compile("^vívomove$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivomove;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        // no vibration motor to alert with
        return 0;
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateRestingMeasurement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsSleepScore(@NonNull final GBDevice device) {
        return false;
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
    public boolean supportsRespiratoryRate(@NonNull final GBDevice device) {
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
    public boolean supportsVO2Max(@NonNull final GBDevice device) {
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

    @Override
    public boolean supportsCalendarEvents(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsFindDevice(@NonNull final GBDevice device) {
        // no vibration motor
        return false;
    }

    @Override
    public boolean supportsPai(@NonNull final GBDevice device) {
        return false;
    }
}
