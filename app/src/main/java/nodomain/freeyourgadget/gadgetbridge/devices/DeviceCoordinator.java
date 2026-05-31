/*  Copyright (C) 2015-2026 akasaka / Genjitsu Labs, Alicia Hormann, Andreas
    Böhler, Andreas Shimokawa, Arjan Schrijver, Carsten Pfeiffer, Damien Gaignon,
    Daniel Dakhno, Daniele Gobbetti, Dmitry Markin, JohnnySun, José Rebelo,
    Matthieu Baerts, Nephiel, Petr Vaněk, Uwe Hermann, Johannes Krude, Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DeviceChartsProvider;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.BarcodeFormat;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTrainingLoadAcuteSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTrainingLoadChronicSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.BloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;
import nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.RestingMetabolicRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;
import nodomain.freeyourgadget.gadgetbridge.model.WeightSample;
import nodomain.freeyourgadget.gadgetbridge.model.WorkoutLoadSample;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesSpec;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.ServiceDeviceSupport;

/**
 * This interface is implemented at least once for every supported gadget device.
 * It allows Gadgetbridge to generically deal with different kinds of devices
 * without actually knowing the details of any device.
 * <p/>
 * Instances will be created as needed and asked whether they support a given
 * device. If a coordinator answers true, it will be used to assist in handling
 * the given device.
 */
public interface DeviceCoordinator {
    String EXTRA_DEVICE_CANDIDATE = "nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate.EXTRA_DEVICE_CANDIDATE";
    String EXTRA_DEVICE_ALL_CANDIDATES =
        "nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate.EXTRA_DEVICE_ALL_CANDIDATES";
    /**
     * Do not attempt to bond after discovery.
     */
    int BONDING_STYLE_NONE = 0;
    /**
     * Bond after discovery.
     * This is not recommended, as there are mobile devices on which bonding does not work.
     * Prefer to use #BONDING_STYLE_ASK instead.
     */
    int BONDING_STYLE_BOND = 1;
    /**
     * Let the user decide whether to bond or not after discovery.
     * Prefer this over #BONDING_STYLE_BOND
     */
    int BONDING_STYLE_ASK = 2;

    /**
     * A secret key has to be entered before connecting
     */
    int BONDING_STYLE_REQUIRE_KEY = 3;

    /**
     * Lazy pairing, i.e. device initiated pairing is requested
     */
    int BONDING_STYLE_LAZY = 4;

    enum ConnectionType{
        BLE(false, true),
        BT_CLASSIC(true, false),
        BOTH(true, true)
        ;
        boolean usesBluetoothClassic, usesBluetoothLE;

        ConnectionType(boolean usesBluetoothClassic, boolean usesBluetoothLE) {
            this.usesBluetoothClassic = usesBluetoothClassic;
            this.usesBluetoothLE = usesBluetoothLE;
        }

        public boolean usesBluetoothLE(){
            return usesBluetoothLE;
        }

        public boolean usesBluetoothClassic(){
            return usesBluetoothClassic;
        }
    }

    enum DeviceKind {
        UNKNOWN,
        WATCH,
        PHONE,
        SCALE,
        RING,
        HEAD_MOUNTED,
        FITNESS_BAND,
        CHEST_STRAP,
        SMART_DISPLAY,
        SPEAKER,
        HEADPHONES,
        SMART_GLASSES,
        EARBUDS,
        BIKE_COMPUTER,
        SMART_CLOCK,
        THERMOMETER,
        PV_EQUIPMENT,
        GLUCOSE_METER,
        BLOOD_PRESSURE_METER,
        BATTERY_MONITOR,
    }

    /**
     * Returns the type of connection, Classic of BLE
     *
     * @return ConnectionType
     */
    ConnectionType getConnectionType();

    /**
     * Returns false is the Device is not connectable,
     * only scannable, like beacons
     *
     * @return boolean
     */
    boolean isConnectable();

    /**
     * Checks whether this coordinator handles the given candidate.
     *
     * @param candidate
     * @return true if this coordinator handles the given candidate.
     */
    boolean supports(@NonNull final GBDeviceCandidate candidate);

    /**
     * Returns a list of scan filters that shall be used to discover devices supported
     * by this coordinator.
     * @return the list of scan filters, may be empty
     */
    @NonNull
    Collection<? extends ScanFilter> createBLEScanFilters();

    /**
     * Creates a GBDevice from a candidate device. This is used to create a device based on the
     * results of a device scan (bluetooth or otherwise).
     *
     * @param candidate - The candidate that should be converted to a GBDevice.
     * @param type      - The type of the device.
     * @return - The constructed GBDevice.
     */
    GBDevice createDevice(@NonNull final GBDeviceCandidate candidate, @NonNull final DeviceType type);

    /**
     * Creates a GBDevice from a database device. This is used to deserialize the device from the
     * persistent storage.
     *
     * @param dbDevice - The device object pulled from persistent storage.
     * @param type     - The type of the device.
     * @return - The constructed GBDevice.
     */
    GBDevice createDevice(@NonNull final Device dbDevice, @NonNull final DeviceType type);

    /**
     * Deletes all information, including all related database content about the
     * given device.
     * @throws GBException
     */
    void deleteDevice(@NonNull final GBDevice device, boolean deleteFiles) throws GBException;

    /**
     * Returns the Activity class to be started in order to perform a pairing of a
     * given device after its discovery.
     *
     * @return the activity class for pairing/initial authentication, or null if none
     */
    @Nullable
    Class<? extends Activity> getPairingActivity();

    @Nullable
    Class<? extends Activity> getCalibrationActivity();

    int getLiveActivityFragmentPulseInterval();

    /**
     * Whether the device supports fetching debug logs.
     */
    boolean supportsDebugLogs(@NonNull final GBDevice device);

    /**
     * Returns true if data fetching (activity or otherwise) is supported by the device
     * (with this coordinator).
     * This enables the sync button in control center and the device can thus be asked to send the data
     * (as opposed the device pushing the data to us by itself)
     *
     * @return
     */
    boolean supportsDataFetching(@NonNull final GBDevice device);

    /**
     * Returns true if activity tracking is supported by the device
     * (with this coordinator).
     * This enables the ActivityChartsActivity.
     *
     * @return
     */
    boolean supportsActivityTracking(@NonNull final GBDevice device);

    /**
     * Returns true if cycling data is supported by the device
     * (with this coordinator).
     * This enables the ChartsActivity.
     *
     * @return
     */
    boolean supportsCyclingData(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports recording dedicated activities, like
     * walking, hiking, running, swimming, etc. and retrieving the recorded
     * data. This is different from the constant activity tracking since the tracks are
     * usually recorded with higher precision and additional features, like e.g. GPS.
     */
    boolean supportsRecordedActivities(@NonNull final GBDevice device);

    /**
     * Returns true if stress measurement and fetching is supported by the device
     * (with this coordinator).
     */
    boolean supportsStressMeasurement(@NonNull final GBDevice device);

    boolean supportsBodyEnergy(@NonNull final GBDevice device);
    boolean supportsHrvMeasurement(@NonNull final GBDevice device);
    boolean supportsVO2Max(@NonNull final GBDevice device);

    /**
     * Returns true if this device supports distinguishing VO2 max per sport (running / cycling).
     */
    boolean supportsVO2MultiSport(@NonNull final GBDevice device);
    boolean supportsSleepMeasurement(@NonNull final GBDevice device);
    boolean supportsStepCounter(@NonNull final GBDevice device);
    boolean supportsSpeedzones(@NonNull final GBDevice device);
    boolean supportsCharts(@NonNull final GBDevice device);
    boolean supportsActiveCalories(@NonNull final GBDevice device);
    boolean supportsActivityDistance(@NonNull final GBDevice device);
    boolean supportsTrainingLoad(@NonNull final GBDevice device);
    boolean supportsGlucoseMeasurement(@NonNull final GBDevice device);

    DeviceChartsProvider getChartsProvider();

    /**
     * Returns true if measurement and fetching of body temperature is supported by the device
     * (with this coordinator).
     */
    boolean supportsTemperatureMeasurement(@NonNull final GBDevice device);

    /**
     * Returns true if continuous temperature measurement used in device
     * (with this coordinator).
     */
    boolean supportsContinuousTemperature(@NonNull final GBDevice device);

    /**
     * Returns true if SpO2 measurement and fetching is supported by the device
     * (with this coordinator).
     */
    boolean supportsSpo2(@NonNull final GBDevice device);

    /**
     * Returns true if heart rate stats (max, resting, manual) measurement and fetching is supported
     * by the device (with this coordinator).
     */
    boolean supportsHeartRateStats(@NonNull final GBDevice device);

    /**
     * Returns true if blood pressure measurement (systolic, diastolic, pulse) measurement and
     * fetching is supported by the device (with this coordinator).
     */
    boolean supportsBloodPressureMeasurement(@NonNull final GBDevice device);

    /**
     * Returns true if PAI (Personal Activity Intelligence) measurement and fetching is supported by
     * the device (with this coordinator).
     */
    boolean supportsPai(@NonNull final GBDevice device);

    /**
     * Returns the device-specific name for PAI (eg. Vitality Score).
     */
    @StringRes
    int getPaiName();

    /**
     * Returns true if the device is capable of providing the time contribution for each PAI type
     * (light, moderate, high).
     */
    boolean supportsPaiTime(@NonNull final GBDevice device);

    /**
     * Returns true if the device is capable of providing the time contribution for light PAI type.
     */
    boolean supportsPaiLow(@NonNull final GBDevice device);

    /**
     * Returns the PAI target - usually 100.
     */
    int getPaiTarget();

    /**
     * Indicates whether the device supports respiratory rate tracking.
     */
    boolean supportsRespiratoryRate(@NonNull final GBDevice device);

    /**
     * Indicates whether the device tracks respiratory rate during the day, will be false
     * if only during the night.
     */
    boolean supportsDayRespiratoryRate(@NonNull final GBDevice device);

    /**
     * Returns true if sleep respiratory rate measurement and fetching is supported by
     * the device (with this coordinator).
     */
    boolean supportsSleepRespiratoryRate(@NonNull final GBDevice device);

    /**
     * Returns true if measurement and fetching of body weight is supported by the device
     * (with this coordinator).
     */
    boolean supportsWeightMeasurement(@NonNull final GBDevice device);

    /**
     * Returns the sample provider for the device being supported.
     */
    @Nullable
    SampleProvider<? extends ActivitySample> getSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for stress data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends StressSample> getStressSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for body energy data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends BodyEnergySample> getBodyEnergySampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for HRV summary, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends HrvSummarySample> getHrvSummarySampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for HRV values, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends HrvValueSample> getHrvValueSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for Workout load values, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends WorkoutLoadSample> getWorkoutLoadSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for training acute load values, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends GenericTrainingLoadAcuteSample> getTrainingAcuteLoadSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);


    /**
     * Returns the sample provider for training chronic load values, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends GenericTrainingLoadChronicSample> getTrainingChronicLoadSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);


    /**
     * Returns the sample provider for VO2 max values, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends Vo2MaxSample> getVo2MaxSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the stress ranges (relaxed, mild, moderate, high), so that stress can be categorized.
     */
    int[] getStressRanges();

    /**
     * Returns true if stress level percentages are displayed instead of actual time.
     */
    boolean showStressLevelInPercents();

    /**
     * Returns the stress data parameters (sampleRate, interval, delta) used for chart drawing
     */
    int[] getStressChartParameters();
    /**
     * Returns the sample provider for temperature data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for SpO2 data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for Cycling data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<CyclingSample> getCyclingSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for max HR data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends HeartRateSample> getHeartRateMaxSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for resting HR data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for manual HR data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends HeartRateSample> getHeartRateManualSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for PAI data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends PaiSample> getPaiSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for sleep respiratory rate data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends RespiratoryRateSample> getRespiratoryRateSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for weight data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends WeightSample> getWeightSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    @Nullable
    TimeSampleProvider<? extends RestingMetabolicRateSample> getRestingMetabolicRateProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    @Nullable
    TimeSampleProvider<? extends SleepScoreSample> getSleepScoreProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the sample provider for blood pressure data, for the device being supported.
     */
    @Nullable
    TimeSampleProvider<? extends BloodPressureSample> getBloodPressureSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session);

    /**
     * Returns the {@link ActivitySummaryParser} for the device being supported.
     *
     * @return
     */
    ActivitySummaryParser getActivitySummaryParser(@NonNull final GBDevice device, @NonNull final Context context);

    /**
     * Returns the {@link ActivityTrackProvider} for the device being supported.
     */
    ActivityTrackProvider getActivityTrackProvider(@NonNull final GBDevice device, @NonNull final Context context);

    /**
     * Returns true if this device/coordinator supports installing files like firmware,
     * watchfaces, gps, resources, fonts...
     *
     * @return
     */
    boolean supportsFlashing(@NonNull final GBDevice device);

    /**
     * Finds an {@link InstallHandler} for the given {@link Uri} that can install the given
     * uri on the device being managed.
     * @return the {@link InstallHandler} or {@code null} if the {@link Uri} cannot be installed
     *   on the device
     */
    @Nullable
    InstallHandler findInstallHandler(Uri uri, @NonNull final Bundle options, @NonNull final Context context);

    /**
     * Returns true if this device/coordinator supports taking screenshots.
     *
     * @return
     */
    boolean supportsScreenshots(@NonNull final GBDevice device);

    /**
     * Returns the number of alarms this device/coordinator supports
     * Shall return 0 also if it is not possible to set alarms via
     * protocol, but only on the smart device itself.
     *
     * @return
     */
    int getAlarmSlotCount(@NonNull final GBDevice device);

    /**
     * Returns true if this device/coordinator supports an alarm with smart wakeup for the current position
     * @param alarmPosition Position of the alarm
     */
    boolean supportsSmartWakeup(@NonNull final GBDevice device, int alarmPosition);

    /**
     * Returns true if the smart alarm at the specified position supports setting an interval for this device/coordinator
     * @param alarmPosition Position of the alarm
     */
    boolean supportsSmartWakeupInterval(@NonNull final GBDevice device, int alarmPosition);

    /**
     * Returns true if the alarm at the specified position *must* be a smart alarm for this device/coordinator
     * @param alarmPosition Position of the alarm
     * @return True if it must be a smart alarm, false otherwise
     */
    boolean forcedSmartWakeup(@NonNull final GBDevice device, int alarmPosition);

    /**
     * Returns true if this device/coordinator supports alarm snoozing
     * @return
     */
    boolean supportsAlarmSnoozing(@NonNull final GBDevice device);

    /**
     * Returns true if this device/coordinator supports alarm titles
     * @return
     */
    boolean supportsAlarmTitle(@NonNull final GBDevice device);

    /**
     * Returns the character limit for the alarm title, negative if no limit.
     * @return
     */
    int getAlarmTitleLimit(@NonNull final GBDevice device);

    /**
     * Returns true if this device/coordinator supports alarm descriptions
     * @return
     */
    boolean supportsAlarmDescription(@NonNull final GBDevice device);

    boolean supportsAlarmSounds(@NonNull final GBDevice device);
    boolean supportsAlarmBacklight(@NonNull final GBDevice device);
    boolean supportsAlarmTitlePresets(@NonNull final GBDevice device);
    List<Alarm.ALARM_LABEL> getAlarmTitlePresets(@NonNull final GBDevice device);

    /**
     * Returns true if the given device supports heart rate measurements.
     * @return
     */
    boolean supportsHeartRateMeasurement(@NonNull final GBDevice device);

    /**
     * Returns true if the given device supports resting heart rate measurements.
     */
    boolean supportsHeartRateRestingMeasurement(@NonNull final GBDevice device);

    /**
     * Returns true if the device supports triggering manual one-shot heart rate measurements.
     */
    boolean supportsManualHeartRateMeasurement(@NonNull final GBDevice device);

    /**
     * Returns the readable name of the manufacturer.
     */
    String getManufacturer();

    /**
     * Returns true if this device/coordinator supports managing device apps.
     *
     * @return
     */
    boolean supportsAppsManagement(@NonNull final GBDevice device);

    boolean supportsCachedAppManagement(@NonNull final GBDevice device);
    boolean supportsInstalledAppManagement(@NonNull final GBDevice device);
    boolean supportsWatchfaceManagement(@NonNull final GBDevice device);

    /**
     * Returns the Activity class that will be used to manage device apps.
     *
     * @return
     */
    @Nullable
    Class<? extends Activity> getAppsManagementActivity(@NonNull final GBDevice device);

    /**
     * Returns the Activity class that will be used to design watchfaces.
     *
     * @return
     */
    @Nullable
    Class<? extends Activity> getWatchfaceDesignerActivity(@NonNull final GBDevice device);

    /**
     * Returns the Activity class that will be used to download apps/watchfaces.
     */
    @Nullable
    Class<? extends Activity> getAppStoreActivity(@NonNull final GBDevice device);

    /**
     * Returns the Activity class that will be used to configure apps.
     */
    @Nullable
    Class<? extends Activity> getAppConfigurationActivity(@NonNull final GBDevice device);

    /**
     * Returns the device app cache directory.
     */
    @Nullable
    File getAppCacheDir() throws IOException;

    /**
     * Returns the dedicated writable export directory for this device.
     */
    File getWritableExportDirectory(@NonNull final GBDevice device, boolean createIfRequired) throws IOException;

    /**
     * Returns a String containing the device app sort order filename.
     */
    @Nullable
    String getAppCacheSortFilename();

    /**
     * Returns a String containing the file extension for watch apps.
     */
    @Nullable
    String getAppFileExtension();

    /**
     * Indicated whether the device supports fetching a list of its apps.
     */
    boolean supportsAppListFetching(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports reordering of apps.
     */
    boolean supportsAppReordering(@NonNull final GBDevice device);

    /**
     * Returns how/if the given device should be bonded before connecting to it.
     */
    int getBondingStyle();

    /**
     * Whether it is recommended to unbind the device before pairing due to compatibility issues. Returns false
     * if the device is known to pair without issues even when already bound in Android bluetooth settings.
     */
    boolean suggestUnbindBeforePair();

    /**
     * Returns true if this device is in an experimental state / not tested.
     */
    boolean isExperimental();

    /**
     * Indicates whether the device has some kind of calender we can sync to.
     * Also used for generated sunrise/sunset events
     */
    boolean supportsCalendarEvents(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports getting a stream of live data.
     * This can be live HR, steps etc.
     */
    boolean supportsRealtimeData(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports REM sleep tracking.
     */
    boolean supportsRemSleep(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports Awake sleep tracking.
     */
    boolean supportsAwakeSleep(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports determining a sleep score in a 0-100 range.
     */
    boolean supportsSleepScore(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports current weather and/or weather
     * forecast display.
     */
    boolean supportsWeather(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports being found by vibrating,
     * making some sound or lighting up
     */
    boolean supportsFindDevice(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports displaying music information
     * like artist, title, album, play state etc.
     */
    boolean supportsMusicInfo(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports features required by Sleep As Android
     */
    boolean supportsSleepAsAndroid(@NonNull final GBDevice device);

    /**
     * Indicates the maximum reminder message length.
     */
    int getMaximumReminderMessageLength();

    /**
     * Indicates the maximum number of reminder slots available in the device.
     */
    int getReminderSlotCount(@NonNull final GBDevice device);

    /**
     * Indicates whether reminders have a time of day.
     */
    boolean getRemindersHaveTime();

    /**
     * Indicates whether some reminder slots are used for calendar events.
     */
    boolean getReserveReminderSlotsForCalendar();

    /**
     * Indicates the maximum number of canned replies available in the device.
     */
    int getCannedRepliesSlotCount(@NonNull final GBDevice device);

    /**
     * Indicates the maximum number of slots available for world clocks in the device.
     */
    int getWorldClocksSlotCount();

    /**
     * Indicates the maximum label length for a world clock in the device.
     */
    int getWorldClocksLabelLength();

    /**
     * Indicates whether the device supports disabled world clocks that can be enabled through
     * a menu on the device.
     */
    boolean supportsDisabledWorldClocks(@NonNull final GBDevice device);

    /**
     * Indicates whether the device supports recording and syncing audio recordings.
     */
    boolean supportsAudioRecordings(@NonNull final GBDevice device);

    /**
     * Indicates the maximum number of slots available for contacts in the device.
     */
    int getContactsSlotCount(@NonNull final GBDevice device);

    /**
     * Indicates whether the device has an led which supports custom colors
     */
    boolean supportsLedColor(@NonNull final GBDevice device);

    /**
     * Indicates whether the device's led supports any RGB color,
     * or only preset colors
     */
    boolean supportsRgbLedColor(@NonNull final GBDevice device);

    /**
     * Returns the preset colors supported by the device, if any, in ARGB, with alpha = 255
     */
    @NonNull
    int[] getColorPresets();

    /**
     * Indicates whether the device supports unicode emojis.
     */
    boolean supportsUnicodeEmojis(@NonNull final GBDevice device);

    /**
     * Returns the set of supported sleep as Android features
      * @return Set
     */
    Set<SleepAsAndroidFeature> getSleepAsAndroidFeatures();

    /**
     * Returns device specific settings related to connection
     *
     * @return int[]
     */
    int[] getSupportedDeviceSpecificConnectionSettings();

    /**
     * Returns device specific settings related to the Auth key
     * @return int[]
     */
    int[] getSupportedDeviceSpecificAuthenticationSettings();

    /**
     * Returns device specific debug settings. This section is only shown in debug builds, and all behavior-altering
     * preferences should be gate-kept by the BuildConfig.DEBUG flag.
     */
    int[] getSupportedDebugSettings(@NonNull final GBDevice device);

    /**
     * Returns device specific experimental settings. This screen is only shown when the global experimental settings
     * is enabled.
     */
    @Nullable
    int[] getSupportedDeviceSpecificExperimentalSettings(@NonNull final GBDevice device);

    /**
     * Indicates which device specific settings the device supports (not per device type or family, but unique per device).
     *
     * @deprecated use getDeviceSpecificSettings
     */
    @Deprecated
    @Nullable
    int[] getSupportedDeviceSpecificSettings(@NonNull final GBDevice device);

    /**
     * Returns the device-specific settings supported by this specific device. See
     * {@link DeviceSpecificSettings} for more information
     */
    @Nullable
    DeviceSpecificSettings getDeviceSpecificSettings(@NonNull final GBDevice device);

    /**
     * Returns the {@link DeviceSpecificSettingsCustomizer}, allowing for the customization of the devices specific settings screen.
     */
    @Nullable
    DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(@NonNull final GBDevice device);

    /**
     * Indicates which device specific language the device supports
     */
    @Nullable
    String[] getSupportedLanguageSettings(@NonNull final GBDevice device);

    /**
     *
     * Multiple battery support: Indicates how many batteries the device has.
     * 1 is default, 3 is maximum at the moment (as per UI layout)
     * 0 will disable the battery from the UI
     */
    int getBatteryCount(@NonNull final GBDevice device);

    BatteryConfig[] getBatteryConfig(@NonNull final GBDevice device);

    /**
     * Returns true if the device battery level is reported by the OS (usually for headsets)
     */
    boolean supportsOSBatteryLevel(@NonNull final GBDevice device);


    boolean addBatteryPollingSettings();

    boolean supportsPowerOff(@NonNull final GBDevice device);

    PasswordCapabilityImpl.Mode getPasswordCapability();

    List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals();

    /**
     * Whether the device supports screens with configurable widgets.
     */
    boolean supportsWidgets(@NonNull final GBDevice device);

    /**
     * Gets the {@link WidgetManager} for this device. Must not be null if supportsWidgets is true.
     */
    @Nullable
    WidgetManager getWidgetManager(@NonNull final GBDevice device);

    boolean supportsNavigation(@NonNull final GBDevice device);

    int getOrderPriority();

    @NonNull
    Class<? extends DeviceSupport> getDeviceSupportClass(@NonNull final GBDevice device);

    EnumSet<ServiceDeviceSupport.Flags> getInitialFlags();

    @StringRes
    int getDeviceNameResource();

    @DrawableRes
    int getDefaultIconResource();

    /**
     * Whether the device supports a variety of vibration patterns for notifications.
     */
    boolean supportsNotificationVibrationPatterns(@NonNull final GBDevice device);
    /**
     * Whether the device supports a variety of vibration pattern repetitions for notifications.
     */
    boolean supportsNotificationVibrationRepetitionPatterns(@NonNull final GBDevice device);

    /**
     * Whether the device supports a variety of LED patterns for notifications.
     */
    boolean supportsNotificationLedPatterns(@NonNull final GBDevice device);
    /**
     * What vibration pattern repetitions for notifications are supported by the device.
     */
     AbstractNotificationPattern[] getNotificationVibrationPatterns();
    /**
     * What vibration pattern repetitions for notifications are supported by the device.
     * Technote: this is not an int or a range because some devices (e.g. Wena 3) only allow
     * a very specific set of value combinations here.
     */
    AbstractNotificationPattern[] getNotificationVibrationRepetitionPatterns();
    /**
     * What LED patterns for notifications are supported by the device.
     */
    AbstractNotificationPattern[] getNotificationLedPatterns();

    boolean validateAuthKey(String authKey);

    @Nullable
    String getAuthHelp();

    List<DeviceCardAction> getCustomActions();

    DeviceKind getDeviceKind(@NonNull final GBDevice device);

    HeartRateZonesSpec getHeartRateZonesSpec(@NonNull final GBDevice device);

    Set<BarcodeFormat> getSupportedBarcodeFormats(@NonNull final GBDevice device);

    /**
     * @return delay in ms for this device to wait before a reconnection attempt is made.
     */
    int getReconnectionDelay();

    /**
     * Returns whether the device supports changing bluetooth connection priority.
     * @return
     */
    boolean supportsConnectionPriority();
}
