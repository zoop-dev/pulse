/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.ComputedHrvSummarySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictData;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValuesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiEcgDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiEcgSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiEcgSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiEmotionsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiHrvValueSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStatsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiTemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSpO2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummaryAdditionalValuesSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSwimSegmentsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWorkoutGbParser;

public abstract class HuaweiCoordinator extends AbstractDeviceCoordinator {

    public enum HuaweiDeviceType {
        AW(0),     //BLE behind
        BR(1),
        BLE(2),
        SMART(5)   //BLE behind
        ;

        final int huaweiType;

        HuaweiDeviceType(int huaweiType) {
            this.huaweiType = huaweiType;
        }

        public int getType() {
            return huaweiType;
        }
    }

    public boolean isTransactionCrypted() {
        return true;
    }

    public abstract HuaweiDeviceType getHuaweiType();

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid huaweiService = new ParcelUuid(HuaweiConstants.UUID_SERVICE_HUAWEI_SERVICE);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(huaweiService).build();
        return Collections.singletonList(filter);
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new HuaweiSettingsCustomizer(device);
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "ar_SA",
                "cs_CZ",
                "da_DK",
                "de_DE",
                "el_GR",
                "en_GB",
                "en_US",
                "es_ES",
                "fr_FR",
                "he_IL",
                "it_IT",
                "id_ID",
                "ko_KO",
                "nl_NL",
                "pl_PL",
                "pt_PT",
                "pt_BR",
                "ro_RO",
                "ru_RU",
                "sv_SE",
                "th_TH",
                "ja_JP",
                "tr_TR",
                "uk_UA",
                "zh_CN",
                "zh_TW",
        };
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        final List<Integer> settings = new ArrayList<>();
        settings.add(R.xml.devicesettings_huawei_account);
        if (getConnectionType() == ConnectionType.BLE) {
            settings.add(R.xml.devicesettings_miband6_new_protocol);
        }
        return ArrayUtils.toPrimitive(settings.toArray(new Integer[0]));
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice, @NonNull final Device device, @NonNull final DaoSession session) {
        // Clear capabilities shared preferences
        GBApplication.getContext().getSharedPreferences(HuaweiState.getCapabilitiesSharedPreferencesName(gbDevice.getAddress()), Context.MODE_PRIVATE).edit().clear().apply();
        // Before #5629, we would persist capabilities per device type, so clear those as well if they exist
        GBApplication.getContext().getSharedPreferences("huawei_coordinator_capatilities" + gbDevice.getType().name(), Context.MODE_PRIVATE).edit().clear().apply();

        final long deviceId = device.getId();

        deleteBy(session.getHuaweiActivitySampleDao(), HuaweiActivitySampleDao.Properties.DeviceId, deviceId);
        deleteBy(session.getHuaweiSleepStageSampleDao(), HuaweiSleepStageSampleDao.Properties.DeviceId, deviceId);
        deleteBy(session.getHuaweiSleepStatsSampleDao(), HuaweiSleepStatsSampleDao.Properties.DeviceId, deviceId);
        deleteBy(session.getHuaweiStressSampleDao(), HuaweiStressSampleDao.Properties.DeviceId, deviceId);
        deleteBy(session.getHuaweiHrvValueSampleDao(), HuaweiHrvValueSampleDao.Properties.DeviceId, deviceId);
        deleteBy(session.getHuaweiTemperatureSampleDao(), HuaweiTemperatureSampleDao.Properties.DeviceId, deviceId);
        deleteBy(session.getHuaweiEmotionsSampleDao(), HuaweiEmotionsSampleDao.Properties.DeviceId, deviceId);

        final QueryBuilder<HuaweiWorkoutSummarySample> qbWorkoutSummarySample = session.getHuaweiWorkoutSummarySampleDao().queryBuilder();
        final List<HuaweiWorkoutSummarySample> workouts = qbWorkoutSummarySample.where(HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId)).build().list();

        for (HuaweiWorkoutSummarySample sample : workouts) {
            deleteBy(session.getHuaweiWorkoutSummaryAdditionalValuesSampleDao(), HuaweiWorkoutSummaryAdditionalValuesSampleDao.Properties.WorkoutId, sample.getWorkoutId());
            deleteBy(session.getHuaweiWorkoutDataSampleDao(), HuaweiWorkoutDataSampleDao.Properties.WorkoutId, sample.getWorkoutId());
            deleteBy(session.getHuaweiWorkoutPaceSampleDao(), HuaweiWorkoutPaceSampleDao.Properties.WorkoutId, sample.getWorkoutId());
            deleteBy(session.getHuaweiWorkoutSwimSegmentsSampleDao(), HuaweiWorkoutSwimSegmentsSampleDao.Properties.WorkoutId, sample.getWorkoutId());
            deleteBy(session.getHuaweiWorkoutSpO2SampleDao(), HuaweiWorkoutSpO2SampleDao.Properties.WorkoutId, sample.getWorkoutId());
        }

        deleteBy(session.getHuaweiWorkoutSummarySampleDao(), HuaweiWorkoutSummarySampleDao.Properties.DeviceId, deviceId);
        deleteBy(session.getBaseActivitySummaryDao(), BaseActivitySummaryDao.Properties.DeviceId, deviceId);

        final QueryBuilder<HuaweiDictData> qbDictData = session.getHuaweiDictDataDao().queryBuilder();
        final List<HuaweiDictData> dictData = qbDictData.where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId)).build().list();
        for (HuaweiDictData data : dictData) {
            deleteBy(session.getHuaweiDictDataValuesDao(), HuaweiDictDataValuesDao.Properties.DictId, data.getDictId());
        }

        deleteBy(session.getHuaweiDictDataDao(), HuaweiDictDataDao.Properties.DeviceId, deviceId);

        final QueryBuilder<HuaweiEcgSummarySample> qbEcgSummary = session.getHuaweiEcgSummarySampleDao().queryBuilder();
        final List<HuaweiEcgSummarySample> ecgSummary = qbEcgSummary.where(HuaweiEcgSummarySampleDao.Properties.DeviceId.eq(deviceId)).build().list();
        for (HuaweiEcgSummarySample sample : ecgSummary) {
            deleteBy(session.getHuaweiEcgDataSampleDao(), HuaweiEcgDataSampleDao.Properties.EcgId, sample.getEcgId());
        }
        deleteBy(session.getHuaweiEcgSummarySampleDao(), HuaweiEcgSummarySampleDao.Properties.DeviceId, deviceId);
    }

    @Override
    public String getManufacturer() {
        return "Huawei";
    }

    @Override
    public boolean supportsSmartWakeup(@NonNull final GBDevice device, final int position) {
        return HuaweiDeviceStateManager.get(device).supportsSmartAlarm(device, position);
    }

    @Override
    public boolean supportsSmartWakeupInterval(@NonNull final GBDevice device, final int alarmPosition) {
        return supportsSmartWakeup(device, alarmPosition);
    }

    @Override
    public boolean forcedSmartWakeup(final GBDevice device, final int alarmPosition) {
        return HuaweiDeviceStateManager.get(device).forcedSmartWakeup(device, alarmPosition);
    }

    @Override
    public boolean supportsAlarmTitle(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsWeather();
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity(final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getAppManagerActivity();
    }

    @Override
    public boolean supportsAppListFetching(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getSupportsAppListFetching();
    }

    @Override
    public boolean supportsAppsManagement(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getSupportsAppsManagement();
    }

    @Override
    public boolean supportsWatchfaceManagement(@NonNull final GBDevice device) {
        return supportsAppsManagement(device);
    }

    @Override
    public boolean supportsInstalledAppManagement(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getSupportsInstalledAppManagement();
    }

    @Override
    public boolean supportsCachedAppManagement(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getSupportsCachedAppManagement();
    }

    @Override
    public boolean supportsFlashing(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getSupportsFlashing();
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getAlarmSlotCount(device);
    }

    @Override
    public int getContactsSlotCount(GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getContactsSlotCount(device);
    }

    @Override
    public int getCannedRepliesSlotCount(GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getCannedRepliesSlotCount(device);
    }

    @Override
    public boolean supportsCalendarEvents(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsCalendarEvents();
    }

    @Override
    public boolean supportsDataFetching(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActiveCalories(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRecordedActivities(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsHeartRate(device);
    }

    @Override
    public boolean supportsSpo2(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsSPo2(device);
    }

    @Override
    public boolean supportsMusicInfo(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsMusic();
    }

    @Override
    public boolean supportsTemperatureMeasurement(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsTemperature();
    }

    @Override
    public boolean supportsContinuousTemperature(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsTemperature();
    }

    @Override
    public boolean supportsStressMeasurement(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsAutoStress();
    }

    @Override
    public boolean supportsFindDevice(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsFindDeviceAbility();
    }

    @Override
    public boolean supportsRemSleep(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getSupportsNewTrueSleep();
    }

    @Override
    public boolean supportsAwakeSleep(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getSupportsNewTrueSleep();
    }

    @Override
    public boolean supportsSleepScore(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getSupportsNewTrueSleep();
    }

    @Override
    public boolean supportsHrvMeasurement(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsHRV();
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        final HuaweiInstallHandler handler = new HuaweiInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return new HuaweiWorkoutGbParser(device, context);
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new HuaweiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new HuaweiSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(final GBDevice device, final DaoSession session) {
        return new HuaweiCompatTemperatureSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(final GBDevice device, final DaoSession session) {
        return new HuaweiStressSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends SleepScoreSample> getSleepScoreProvider(final GBDevice device, final DaoSession session) {
        return new HuaweiSleepScoreSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HrvValueSample> getHrvValueSampleProvider(final GBDevice device, final DaoSession session) {
        return new HuaweiHrvValueSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HrvSummarySample> getHrvSummarySampleProvider(GBDevice device, DaoSession session) {
        return new ComputedHrvSummarySampleProvider(getHrvValueSampleProvider(device, session), device, session);
    }

    @Override
    public int[] getStressRanges() {
        // 1-29 = relaxed
        // 30-59 = mild
        // 60-79 = moderate
        // 80-100 = high
        return new int[]{1, 30, 60, 80};
    }

    @Override
    public boolean showStressLevelInPercents() {
        return true;
    }

    @Override
    public int[] getStressChartParameters() {
        // For Huawei devices stress data is provided every 30 minutes. So draw it as bars with delta
        return new int[]{1800, 1800, 400};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final HuaweiState deviceState = HuaweiDeviceStateManager.get(device);

        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        // Health
        if (deviceState.supportsInactivityWarnings())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_inactivity_sheduled);
        if (deviceState.supportsTruSleep())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_trusleep);
        if (deviceState.supportsHeartRate()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_heartrate_automatic_enable);
            if (deviceState.supportsRealtimeHeartRate())
                deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_heart_rate_realtime);
            if (deviceState.supportsHighHeartRateAlert())
                deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_heart_rate_huawei_high_alert);
            if (deviceState.supportsLowHeartRateAlert())
                deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_heart_rate_huawei_low_alert);
        }
        if (deviceState.supportsSPo2()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_spo_automatic_enable);
            if (deviceState.supportsLowSPo2Alert())
                deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_spo_low_alert);
        }
        if (deviceState.supportsTemperature()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_temperature_automatic_enable);
        }
        if (deviceState.supportsAutoStress()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_stress);
        }
        if (deviceState.supportsArrhythmia() && deviceState.isShowForceCountrySpecificFeatures(device)) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_arrhythmia);
        }
        if (deviceState.supportsECG() && deviceState.isShowForceCountrySpecificFeatures(device)) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_ecg);
        }
        if (deviceState.supportsArterialStiffnessDetection() && deviceState.isShowForceCountrySpecificFeatures(device)) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_arterial_stiffness_detection);
        }
        if (deviceState.supportsThreeCircle() || deviceState.supportsThreeCircleLite()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_huawei_activity_reminders);
        }

        // Notifications
        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.NOTIFICATIONS);
        notifications.add(R.xml.devicesettings_notifications_enable);
        if (deviceState.supportsNotificationsRepeatedNotify() || deviceState.supportsNotificationsRemoveSingle()) {
            notifications.add(R.xml.devicesettings_autoremove_notifications);
        }
        if (deviceState.supportsNotificationPicture()) {
            notifications.add(R.xml.devicesettings_notifications_pictures);
        }
        if (deviceState.getCannedRepliesSlotCount(device) > 0) {
            notifications.add(R.xml.devicesettings_canned_reply_16);
        }
        if (deviceState.supportsNotificationOnBluetoothLoss())
            notifications.add(R.xml.devicesettings_disconnectnotification_noshed);
        if (deviceState.supportsDoNotDisturb(device))
            notifications.add(R.xml.devicesettings_donotdisturb_allday_liftwirst_notwear);
        if (deviceState.supportsNotificationsAddIconTimestamp() && device.isConnected()) {
            notifications.add(R.xml.devicesettings_upload_notifications_app_icon);
        }

        // Workout
        if (deviceState.supportsSendingGps())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.WORKOUT, R.xml.devicesettings_workout_send_gps_to_band);

        if (deviceState.supportsTrack() || deviceState.supportsHeartRateZones())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.WORKOUT, R.xml.devicesettings_heartrate_settings);

        // Other
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_find_phone);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_disable_find_phone_with_dnd);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_allow_accept_calls);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_allow_reject_calls);

        // Camera control
        if (deviceState.supportsCameraRemote())
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_camera_remote);

        //Contacts
        if (deviceState.getContactsSlotCount(device) > 0) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_contacts);
        }

        //Music
        if (deviceState.supportsMusicUploading() && deviceState.getMusicInfoParams() != null && device.isConnected()) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_musicmanagement);
        }

        if (deviceState.supportsSendCountryCode()) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_huawei_features);
        }

        // Time
        if (deviceState.supportsDateFormat()) {
            final List<Integer> dateTime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME);
            dateTime.add(R.xml.devicesettings_dateformat);
            dateTime.add(R.xml.devicesettings_timeformat);
        }

        //Calendar
        if (deviceState.supportsP2PService() && deviceState.supportsCalendar()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALENDAR, R.xml.devicesettings_sync_calendar);
        }

        // Display
        if (deviceState.supportsWearLocation(device))
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_wearlocation);
        if (deviceState.supportsAutoWorkMode())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_workmode);
        if (deviceState.supportsActivateOnLift())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_liftwrist_display_noshed);
        if (deviceState.supportsRotateToCycleInfo())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_rotatewrist_cycleinfo);
        // Currently on main setting menu.
        /*if (deviceState.supportsLanguageSetting())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_language_generic);*/
        if (deviceState.supportsTemperature()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_temperature_scale_cf);
        }

        // Developer
        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_huawei_debug);
        if (deviceState.supportsGpsAndTimeToDevice())
            developer.add(R.xml.devicesettings_huawei_gps_and_time);

        return deviceSpecificSettings;
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }

    @Override
    public HeartRateZonesSpec getHeartRateZonesSpec(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).getHeartRateZonesSpec(device);
    }

    @Override
    public boolean supportsNavigation(@NonNull final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsNavigation();
    }
}
