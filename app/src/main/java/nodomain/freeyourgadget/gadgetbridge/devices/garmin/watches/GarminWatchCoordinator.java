package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class GarminWatchCoordinator extends GarminCoordinator {
    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_zetime;
    }

    @Override
    public boolean supportsCalendarEvents(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracks(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsStressMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsBodyEnergy(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHrvMeasurement(final GBDevice device) {
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
    public boolean supportsVO2MaxRunning(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActiveCalories(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public int[] getStressRanges() {
        // 1-25 = relaxed
        // 26-50 = low
        // 51-80 = moderate
        // 76-100 = high
        return new int[]{1, 26, 51, 76};
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateRestingMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRemSleep(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAwakeSleep(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsSleepScore(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRespiratoryRate(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsDayRespiratoryRate(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsPai(@NonNull GBDevice device) {
        // Intensity Minutes
        return true;
    }

    @Override
    public int getPaiName() {
        return R.string.garmin_intensity_minutes;
    }

    @Override
    public boolean supportsPaiTime(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsPaiLow(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public int getPaiTarget() {
        return 150;
    }

    @Override
    public boolean supportsTrainingLoad(@NonNull GBDevice device) {
        // Not all devices support it
        return true;
    }

    @Override
    public boolean supportsWorkoutLoad(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return true;
    }
}
