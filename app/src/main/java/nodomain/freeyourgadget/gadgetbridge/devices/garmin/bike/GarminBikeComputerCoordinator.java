package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class GarminBikeComputerCoordinator extends GarminCoordinator {
    @Override
    public boolean supportsActivityDataFetching(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTabs(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsSleepMeasurement(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsStepCounter(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsSpeedzones(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsActiveCalories(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsVO2Max(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsVO2MaxCycling(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracks(final GBDevice device) {
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
    public boolean supportsWeather(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFindDevice(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(GBDevice device) {
        // eg. Edge 840, Edge Explore 2, but not all
        return true;
    }
}
