package nodomain.freeyourgadget.gadgetbridge.devices.garmin.gps;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminGpsmap66sCoordinator extends GarminCoordinator {
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
        return Pattern.compile("^GPSMAP 66S( #\\d+)?");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_gpsmap_66s;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.UNKNOWN;
    }
}
