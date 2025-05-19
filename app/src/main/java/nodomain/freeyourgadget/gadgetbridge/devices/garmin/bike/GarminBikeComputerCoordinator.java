package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class GarminBikeComputerCoordinator extends GarminCoordinator {
    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsActivityTabs() {
        return false;
    }

    @Override
    public boolean supportsSleepMeasurement() {
        return false;
    }

    @Override
    public boolean supportsStepCounter() {
        return false;
    }

    @Override
    public boolean supportsSpeedzones() {
        return false;
    }

    @Override
    public boolean supportsActiveCalories() {
        return true;
    }

    @Override
    public boolean supportsVO2Max() {
        return true;
    }

    @Override
    public boolean supportsVO2MaxCycling() {
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
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        // eg. Edge 840, Edge Explore 2, but not all
        return true;
    }
}
