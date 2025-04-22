package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class GarminWatchCoordinator extends GarminCoordinator {
    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_zetime;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_zetime_disabled;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsStressMeasurement() {
        return true;
    }

    @Override
    public boolean supportsBodyEnergy() {
        return true;
    }

    @Override
    public boolean supportsHrvMeasurement(final GBDevice device) {
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
    public boolean supportsVO2MaxRunning() {
        return true;
    }

    @Override
    public boolean supportsActiveCalories() {
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
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRemSleep() {
        return true;
    }

    @Override
    public boolean supportsAwakeSleep() {
        return true;
    }

    @Override
    public boolean supportsSleepScore(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRespiratoryRate() {
        return true;
    }

    @Override
    public boolean supportsDayRespiratoryRate() {
        return true;
    }

    @Override
    public boolean supportsPai() {
        // Intensity Minutes
        return true;
    }

    @Override
    public int getPaiName() {
        return R.string.garmin_intensity_minutes;
    }

    @Override
    public boolean supportsPaiTime() {
        return true;
    }

    @Override
    public boolean supportsPaiLow() {
        return false;
    }

    @Override
    public int getPaiTarget() {
        return 150;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }
}
