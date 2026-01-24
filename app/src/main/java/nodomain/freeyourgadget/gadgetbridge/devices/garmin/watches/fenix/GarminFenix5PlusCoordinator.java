package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.GarminWatchCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminFenix5PlusCoordinator extends GarminWatchCoordinator {
    @Override
    public boolean isExperimental() {
        // https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/3963
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^fenix 5 Plus$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_fenix_5_plus;
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
}
