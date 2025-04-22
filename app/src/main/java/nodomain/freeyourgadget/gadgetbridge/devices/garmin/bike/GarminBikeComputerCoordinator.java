package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class GarminBikeComputerCoordinator extends GarminCoordinator {
    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        // TODO does it?
        return true;
    }
}
