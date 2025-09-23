package nodomain.freeyourgadget.gadgetbridge.devices.garmin.gps;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminETrexSeCoordinator extends GarminCoordinator {
    @Override
    public boolean supportsActivityDataFetching(@NonNull final GBDevice device) {
        // for gps tracks
        return true;
    }

    @Override
    public boolean supportsActivityTracks(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        // does not seem to report the battery %
        return 0;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^eTrex SE$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_etrex_se;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.UNKNOWN;
    }
}
