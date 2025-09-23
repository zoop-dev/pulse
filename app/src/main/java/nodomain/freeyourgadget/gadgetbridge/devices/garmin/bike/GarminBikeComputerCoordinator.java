package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class GarminBikeComputerCoordinator extends GarminCoordinator {
    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.BIKE_COMPUTER;
    }

    @Override
    public boolean supportsActivityDataFetching(@NonNull final GBDevice device) {
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
    public boolean supportsActivityTracks(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsWeather(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        // eg. Edge 840, Edge Explore 2, but not all
        return true;
    }
}
