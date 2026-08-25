package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminVivoActiveCoordinator extends GarminWatchCoordinator {
    @Override
    public boolean isExperimental() {
        // Not tested, and the supported device name below is unconfirmed
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // TODO: unconfirmed device name
        return Pattern.compile("^vívoactive$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivoactive;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(@NonNull final GBDevice device) {
        // Does not have an HR sensor. We keep supportsHeartRateMeasurement as true since
        // it still supports pairing with HR straps.
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
    public boolean supportsVO2MultiSport(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsPai(@NonNull final GBDevice device) {
        return false;
    }
}
