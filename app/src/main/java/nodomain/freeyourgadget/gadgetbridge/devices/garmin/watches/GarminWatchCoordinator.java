package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;

public abstract class GarminWatchCoordinator extends GarminCoordinator {
    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_zetime;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        return supports(device, GarminCapability.REALTIME_SETTINGS) ? 0 : 10;
    }

    @Override
    public boolean supportsAlarmSounds(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAlarmBacklight(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAlarmTitlePresets(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public List<Alarm.ALARM_LABEL> getAlarmTitlePresets(@NonNull final GBDevice device) {
        return Arrays.asList(
                Alarm.ALARM_LABEL.NONE,
                Alarm.ALARM_LABEL.WAKE_UP,
                Alarm.ALARM_LABEL.WORKOUT,
                Alarm.ALARM_LABEL.REMINDER,
                Alarm.ALARM_LABEL.APPOINTMENT,
                Alarm.ALARM_LABEL.TRAINING,
                Alarm.ALARM_LABEL.CLASS,
                Alarm.ALARM_LABEL.MEDITATE,
                Alarm.ALARM_LABEL.BEDTIME
        );
    }

    @Override
    public boolean supportsCalendarEvents(@NonNull final GBDevice device) {
        return true;
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
    public boolean supportsActivityTracks(@NonNull final GBDevice device) {
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
    public boolean supportsHrvMeasurement(@NonNull final GBDevice device) {
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
    public boolean supportsHeartRateMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateRestingMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsSpo2(@NonNull GBDevice device) {
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
    public boolean supportsSleepScore(@NonNull final GBDevice device) {
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
    public boolean supportsWeather(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
