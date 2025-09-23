/*  Copyright (C) 2019 krzys_h

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumDeviceVersion;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumMetricSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumTimeSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSetting;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingBool;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingByte;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingEnum;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingInt;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingLanguage;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingRemindersToMove;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingTimeRange;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingUserInfo;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungBloodPressureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungHeartRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.moyoung.MoyoungDeviceSupport;

public abstract class AbstractMoyoungDeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao( @NonNull final DaoSession session) {
        return new HashMap<>() {{
            put(session.getMoyoungActivitySampleDao(), MoyoungActivitySampleDao.Properties.DeviceId);
            put(session.getMoyoungHeartRateSampleDao(), MoyoungHeartRateSampleDao.Properties.DeviceId);
            put(session.getMoyoungSpo2SampleDao(), MoyoungSpo2SampleDao.Properties.DeviceId);
            put(session.getMoyoungBloodPressureSampleDao(), MoyoungBloodPressureSampleDao.Properties.DeviceId);
            put(session.getMoyoungSleepStageSampleDao(), MoyoungSleepStageSampleDao.Properties.DeviceId);
            put(session.getMoyoungStressSampleDao(), MoyoungStressSampleDao.Properties.DeviceId);
        }};
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid service = new ParcelUuid(MoyoungConstants.UUID_SERVICE_MOYOUNG);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(service).build();
        return Collections.singletonList(filter);
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(GBDevice device) {
        return MoyoungDeviceSupport.class;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_LAZY;
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
    public boolean supportsSpo2(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new MoyoungActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new MoyoungSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(GBDevice device, DaoSession session) {
        return new MoyoungStressSampleProvider(device, session);
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 3;
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsCalendarEvents(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return true;
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
    public boolean supportsActivityTracks(@NonNull final GBDevice device) {
        return true;
    }

    @Nullable
    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return new MoyoungWorkoutSummaryParser(device);
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return true;
    }

    private static final MoyoungSetting<?>[] MOYOUNG_SETTINGS = {
        new MoyoungSettingUserInfo("USER_INFO", MoyoungConstants.CMD_SET_USER_INFO),
        new MoyoungSettingByte("STEP_LENGTH", (byte)-1, MoyoungConstants.CMD_SET_STEP_LENGTH),
        // (*) new MoyoungSettingEnum<>("DOMINANT_HAND", MoyoungConstants.CMD_QUERY_DOMINANT_HAND, MoyoungConstants.CMD_SET_DOMINANT_HAND, MoyoungEnumDominantHand.class),
        new MoyoungSettingInt("GOAL_STEP", MoyoungConstants.CMD_QUERY_GOAL_STEP, MoyoungConstants.CMD_SET_GOAL_STEP),
        new MoyoungSettingByte("HR_AUTO_INTERVAL", MoyoungConstants.CMD_QUERY_TIMING_MEASURE_HEART_RATE, MoyoungConstants.CMD_SET_TIMING_MEASURE_HEART_RATE),

        new MoyoungSettingEnum<>("DEVICE_VERSION", MoyoungConstants.CMD_QUERY_DEVICE_VERSION, MoyoungConstants.CMD_SET_DEVICE_VERSION, MoyoungEnumDeviceVersion.class),
        new MoyoungSettingLanguage("DEVICE_LANGUAGE", MoyoungConstants.CMD_QUERY_DEVICE_LANGUAGE, MoyoungConstants.CMD_SET_DEVICE_LANGUAGE),
        new MoyoungSettingEnum<>("TIME_SYSTEM", MoyoungConstants.CMD_QUERY_TIME_SYSTEM, MoyoungConstants.CMD_SET_TIME_SYSTEM, MoyoungEnumTimeSystem.class),
        new MoyoungSettingEnum<>("METRIC_SYSTEM", MoyoungConstants.CMD_QUERY_METRIC_SYSTEM, MoyoungConstants.CMD_SET_METRIC_SYSTEM, MoyoungEnumMetricSystem.class),

        // (*) new MoyoungSetting("DISPLAY_DEVICE_FUNCTION", MoyoungConstants.CMD_QUERY_DISPLAY_DEVICE_FUNCTION, MoyoungConstants.CMD_SET_DISPLAY_DEVICE_FUNCTION),
        // (*) new MoyoungSetting("SUPPORT_WATCH_FACE", MoyoungConstants.CMD_QUERY_SUPPORT_WATCH_FACE, (byte)-1),
        // (*) new MoyoungSetting("WATCH_FACE_LAYOUT", MoyoungConstants.CMD_QUERY_WATCH_FACE_LAYOUT, MoyoungConstants.CMD_SET_WATCH_FACE_LAYOUT),
        new MoyoungSettingByte("DISPLAY_WATCH_FACE", MoyoungConstants.CMD_QUERY_DISPLAY_WATCH_FACE, MoyoungConstants.CMD_SET_DISPLAY_WATCH_FACE),
        new MoyoungSettingBool("OTHER_MESSAGE_STATE", MoyoungConstants.CMD_QUERY_OTHER_MESSAGE_STATE, MoyoungConstants.CMD_SET_OTHER_MESSAGE_STATE),

        new MoyoungSettingBool("QUICK_VIEW", MoyoungConstants.CMD_QUERY_QUICK_VIEW, MoyoungConstants.CMD_SET_QUICK_VIEW),
        new MoyoungSettingTimeRange("QUICK_VIEW_TIME", MoyoungConstants.CMD_QUERY_QUICK_VIEW_TIME, MoyoungConstants.CMD_SET_QUICK_VIEW_TIME),
        new MoyoungSettingBool("SEDENTARY_REMINDER", MoyoungConstants.CMD_QUERY_SEDENTARY_REMINDER, MoyoungConstants.CMD_SET_SEDENTARY_REMINDER),
        new MoyoungSettingRemindersToMove("REMINDERS_TO_MOVE_PERIOD", MoyoungConstants.CMD_QUERY_REMINDERS_TO_MOVE_PERIOD, MoyoungConstants.CMD_SET_REMINDERS_TO_MOVE_PERIOD),
        new MoyoungSettingTimeRange("DO_NOT_DISTURB_TIME", MoyoungConstants.CMD_QUERY_DO_NOT_DISTURB_TIME, MoyoungConstants.CMD_SET_DO_NOT_DISTURB_TIME),
        new MoyoungSettingBool("DO_NOT_DISTURB_ONOFF", MoyoungConstants.CMD_QUERY_DO_NOT_DISTURB_TIME, MoyoungConstants.CMD_SET_DO_NOT_DISTURB_TIME),
        // (*) new MoyoungSetting("PSYCHOLOGICAL_PERIOD", MoyoungConstants.CMD_QUERY_PSYCHOLOGICAL_PERIOD, MoyoungConstants.CMD_SET_PSYCHOLOGICAL_PERIOD),

        new MoyoungSettingBool("BREATHING_LIGHT", MoyoungConstants.CMD_QUERY_BREATHING_LIGHT, MoyoungConstants.CMD_SET_BREATHING_LIGHT),
        new MoyoungSettingBool("POWER_SAVING", MoyoungConstants.CMD_QUERY_POWER_SAVING, MoyoungConstants.CMD_SET_POWER_SAVING)
    };


    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        final List<Integer> generic = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.GENERIC);
        generic.add(R.xml.devicesettings_moyoung_device_version);
        generic.add(R.xml.devicesettings_timeformat);
        generic.add(R.xml.devicesettings_moyoung_watchface);
        generic.add(R.xml.devicesettings_power_saving);
        generic.add(R.xml.devicesettings_liftwrist_display);
//        generic.add(R.xml.devicesettings_donotdisturb_no_auto);  // not supported by Colmi i28 Ultra
        generic.add(R.xml.devicesettings_donotdisturb_on_off_follow);
        generic.add(R.xml.devicesettings_camera_remote);
        if (getWorldClocksSlotCount() > 0) {
            generic.add(R.xml.devicesettings_world_clocks);
        }
        if (supportsCalendarEvents(device)) {
            generic.add(R.xml.devicesettings_sync_calendar);
        }
        final List<Integer> health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH);
        health.add(R.xml.devicesettings_heartrate_interval);
        health.add(R.xml.devicesettings_inactivity_with_steps);
        health.add(R.xml.devicesettings_workout_start_on_phone);
        return deviceSpecificSettings;
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        // TODO: use settings customizer to display the languages
        //  retrieved from the watch instead of this fixed list
        return new String[]{
                "ar_SA",
                "cs_CZ",
                "de_DE",
                "en_US",
                "es_ES",
                "fr_FR",
                "it_IT",
                "ja_JP",
                "ko_KO",
                "nl_NL",
                "pl_PL",
                "pt_PT",
                "ro_RO",
                "ru_RU",
                "uk_UA",
                "zh_CN",
        };
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.MINUTES_5,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_20,
                HeartRateCapability.MeasurementInterval.MINUTES_30
        );
    }

    public MoyoungSetting[] getSupportedSettings() {
        return MOYOUNG_SETTINGS;
    }

    public int getMtu() {
        return 20;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
