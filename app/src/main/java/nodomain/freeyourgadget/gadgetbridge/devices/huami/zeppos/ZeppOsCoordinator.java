/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SleepAsAndroidFeature;
import nodomain.freeyourgadget.gadgetbridge.devices.Vo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.WorkoutVo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiExtendedSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiLanguageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsBtbrSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsBtleSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsFwInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAssistantService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsContactsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFindDeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsLogsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsLoyaltyCardService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsPhoneService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsRemindersService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsShortcutCardsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer.ZeppOsFileTransferImpl;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public abstract class ZeppOsCoordinator extends HuamiCoordinator {
    public abstract List<String> getDeviceBluetoothNames();

    public abstract Set<Integer> getDeviceSources();

    protected Map<Integer, String> getCrcMap() {
        // A map from CRC16 to human-readable version for flashable files
        return Collections.emptyMap();
    }

    @Override
    public String getManufacturer() {
        // Actual manufacturer is Huami
        return "Amazfit";
    }

    @Override
    protected final Pattern getSupportedDeviceName() {
        // Most devices use the exact bluetooth name
        // Some devices have a " XXXX" suffix with the last 4 digits of mac address (eg. Mi Band 7)
        // *However*, some devices broadcast a 2nd bluetooth device with "-XXXX" suffix, which seem to
        // only be used for calls, and Gadgetbridge can't use for pairing.
        // **Additionally**, it was also reported on some issues such as #4827 that some devices
        // only broadcast the one with the mac address, which Gadgetbridge can use for pairing...
        final StringBuilder sb = new StringBuilder();
        sb.append("^(");
        final List<String> deviceBluetoothNames = getDeviceBluetoothNames();
        for (String name : deviceBluetoothNames) {
            sb.append(Pattern.quote(name)).append("|");
        }
        sb.setLength(sb.length() - 1); // remove last |
        sb.append(")");
        sb.append("([- ]+[A-Z0-9]{4})?$");

        return Pattern.compile(sb.toString());
    }

    @NonNull
    @Override
    public final Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        // Prioritize user choice
        DeviceCoordinator.ConnectionType connType = GBApplication.getDevicePrefs(device).getForcedConnectionTypeFromPrefs();
        if (connType == DeviceCoordinator.ConnectionType.BOTH) {
            connType = getConnectionType();
        }

        return switch (connType) {
            case BOTH, BT_CLASSIC -> ZeppOsBtbrSupport.class;
            default -> ZeppOsBtleSupport.class;
        };
    }

    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        if (supportsAgpsUpdates()) {
            final ZeppOsAgpsInstallHandler agpsInstallHandler = new ZeppOsAgpsInstallHandler(uri, context);
            if (agpsInstallHandler.isValid()) {
                return agpsInstallHandler;
            }
        }

        final ZeppOsGpxRouteInstallHandler gpxRouteInstallHandler = new ZeppOsGpxRouteInstallHandler(uri, context);
        if (gpxRouteInstallHandler.isValid()) {
            return gpxRouteInstallHandler;
        }

        final ZeppOsMapsInstallHandler mapsInstallHandler = new ZeppOsMapsInstallHandler(uri, context);
        if (mapsInstallHandler.isValid()) {
            return mapsInstallHandler;
        }

        final ZeppOsMusicInstallHandler musicInstallHandler = new ZeppOsMusicInstallHandler(uri, context);
        if (musicInstallHandler.isValid()) {
            return musicInstallHandler;
        }

        final ZeppOsFwInstallHandler fwInstallHandler = new ZeppOsFwInstallHandler(
                uri,
                context,
                getDeviceBluetoothNames(),
                getDeviceSources()
        );
        return fwInstallHandler.isValid() ? fwInstallHandler : null;
    }

    @Override
    public boolean supportsScreenshots(@NonNull final GBDevice device) {
        return hasDisplay();
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(@NonNull final GBDevice device) {
        return false; // FIXME: this is still somewhat broken and sometimes never finishes
    }

    @Override
    public boolean supportsWeather(@NonNull final GBDevice device) {
        return hasDisplay();
    }

    @Override
    public boolean supportsUnicodeEmojis(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRemSleep(@NonNull GBDevice device) {
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
    public boolean supportsSpo2(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsVO2Max(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsVO2MaxRunning(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsPai(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsSleepRespiratoryRate(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return hasDisplay();
    }

    @Override
    public boolean supportsSleepAsAndroid(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsSleepScore(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAwakeSleep(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHrvMeasurement(@NonNull final GBDevice device) {
        return !hasDisplay() || supportsDisplayItem(device, "hrv");
    }

    @Override
    public Set<SleepAsAndroidFeature> getSleepAsAndroidFeatures() {
        return EnumSet.of(SleepAsAndroidFeature.ACCELEROMETER, SleepAsAndroidFeature.HEART_RATE, SleepAsAndroidFeature.ALARMS, SleepAsAndroidFeature.NOTIFICATIONS);
    }

    @Override
    public int getWorldClocksSlotCount() {
        return hasDisplay() ? 20 : 0; // as enforced by Zepp
    }

    @Override
    public int getWorldClocksLabelLength() {
        return 30; // at least
    }

    @Override
    public boolean supportsDisabledWorldClocks(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAppsManagement(@NonNull final GBDevice device) {
        return experimentalFeatures(device);
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity(final GBDevice device) {
        return AppManagerActivity.class;
    }

    @Override
    public File getAppCacheDir() throws IOException {
        return new File(FileUtils.getExternalFilesDir(), "zepp-os-app-cache");
    }

    @Override
    public String getAppCacheSortFilename() {
        return "zepp-os-app-cache-order.txt";
    }

    @Override
    public String getAppFileExtension() {
        return ".zip";
    }

    @Override
    public boolean supportsAppListFetching(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsCalendarEvents(@NonNull final GBDevice device) {
        return hasDisplay();
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(final GBDevice device, final DaoSession session) {
        return new HuamiExtendedSampleProvider(device, session);
    }

    @Override
    public Vo2MaxSampleProvider<? extends Vo2MaxSample> getVo2MaxSampleProvider(final GBDevice device, final DaoSession session) {
        return new WorkoutVo2MaxSampleProvider(device, session);
    }

    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return new ZeppOsActivitySummaryParser(context);
    }

    @Override
    public boolean supportsAlarmSnoozing(@NonNull GBDevice device) {
        // All alarms snooze by default, there doesn't seem to be a flag that disables it
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(@NonNull final GBDevice device, int position) {
        return true;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return ZeppOsRemindersService.getSlotCount(getPrefs(device));
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return hasDisplay() ? 16 : 0;
    }

    @Override
    public int getContactsSlotCount(final GBDevice device) {
        return getPrefs(device).getInt(ZeppOsContactsService.PREF_CONTACTS_SLOT_COUNT, 0);
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        // Return all known languages by default. Unsupported languages will be removed by Huami2021SettingsCustomizer
        final List<String> allLanguages = new ArrayList<>(HuamiLanguageType.idLookup.keySet());
        allLanguages.add(0, "auto");
        return allLanguages.toArray(new String[0]);
    }

    @Override
    public PasswordCapabilityImpl.Mode getPasswordCapability() {
        return PasswordCapabilityImpl.Mode.NUMBERS_6;
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        // Return all known by default. Unsupported will be removed by Huami2021SettingsCustomizer
        return Arrays.asList(HeartRateCapability.MeasurementInterval.values());
    }

    @Override
    public boolean supportsAudioRecordings(@NonNull final GBDevice device) {
        return supportsDisplayItem(device, "voice_memos") && supportsBleFileTransfer(device, "voicememo");
    }

    @Override
    public int[] getSupportedDeviceSpecificConnectionSettings() {
        final List<Integer> settings = new ArrayList<>();

        settings.add(R.xml.devicesettings_force_connection_type);

        return ArrayUtils.addAll(
                ArrayUtils.toPrimitive(settings.toArray(new Integer[0])),
                super.getSupportedDeviceSpecificConnectionSettings()
        );
    }

    /**
     * Returns a superset of all settings supported by Zepp OS Devices. Unsupported settings are removed
     * by {@link ZeppOsSettingsCustomizer}.
     */
    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        //
        // Apps
        // TODO: These should go somewhere else
        //
        if (ZeppOsLoyaltyCardService.isSupported(getPrefs(device))) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_loyalty_cards);
        }
        if (supportsAudioRecordings(device)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_audio_recordings);
        }

        //
        // Time
        //
        if (hasDisplay()) {
            final List<Integer> dateTime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME);
            // FIXME: This "works", but the band does not update when the setting changes, so it's disabled for now
            //dateTime.add(R.xml.devicesettings_timeformat);
            dateTime.add(R.xml.devicesettings_dateformat_2);
            if (getWorldClocksSlotCount() > 0) {
                dateTime.add(R.xml.devicesettings_world_clocks);
            }
            dateTime.add(R.xml.devicesettings_zeppos_sun_moon_utc);
        }

        //
        // Display
        //
        if (hasDisplay()) {
            final List<Integer> display = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY);
            display.add(R.xml.devicesettings_huami2021_displayitems);
            display.add(R.xml.devicesettings_huami2021_shortcuts);
            if (supportsControlCenter()) {
                display.add(R.xml.devicesettings_huami2021_control_center);
            }
            if (supportsShortcutCards(device)) {
                display.add(R.xml.devicesettings_huami2021_shortcut_cards);
            }
            display.add(R.xml.devicesettings_nightmode);
            display.add(R.xml.devicesettings_sleep_mode);
            display.add(R.xml.devicesettings_liftwrist_display_sensitivity_with_smart);
            display.add(R.xml.devicesettings_password);
            display.add(R.xml.devicesettings_huami2021_watchface);
            display.add(R.xml.devicesettings_always_on_display);
            display.add(R.xml.devicesettings_screen_timeout);
            if (supportsAutoBrightness(device)) {
                display.add(R.xml.devicesettings_screen_brightness_withauto);
            } else {
                display.add(R.xml.devicesettings_screen_brightness);
            }
        }

        //
        // Health
        //
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_heartrate_sleep_alert_activity_stress_spo2);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_inactivity_dnd_no_threshold);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_goal_notification);

        //
        // Workout
        //
        if (hasDisplay()) {
            final List<Integer> workout = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.WORKOUT);
            if (hasGps(device)) {
                workout.add(R.xml.devicesettings_gps_agps);
            } else {
                // If the device has GPS, it doesn't report workout start/end to the phone
                workout.add(R.xml.devicesettings_workout_start_on_phone);
                workout.add(R.xml.devicesettings_workout_send_gps_to_band);
            }
            workout.add(R.xml.devicesettings_workout_keep_screen_on);
            workout.add(R.xml.devicesettings_workout_detection);
        }

        //
        // Notifications
        //
        if (hasDisplay()) {
            final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
            if (supportsBluetoothPhoneCalls(device)) {
                notifications.add(R.xml.devicesettings_phone_calls_watch_pair);
            } else {
                notifications.add(R.xml.devicesettings_display_caller);
            }
            notifications.add(R.xml.devicesettings_sound_and_vibration);
            notifications.add(R.xml.devicesettings_vibrationpatterns);
            notifications.add(R.xml.devicesettings_donotdisturb_withauto_and_always);
            notifications.add(R.xml.devicesettings_send_app_notifications);
            notifications.add(R.xml.devicesettings_screen_on_on_notifications);
            notifications.add(R.xml.devicesettings_autoremove_notifications);
            if (getCannedRepliesSlotCount(device) > 0) {
                notifications.add(R.xml.devicesettings_canned_reply_16);
            }
            notifications.add(R.xml.devicesettings_transliteration);
        }

        //
        // Calendar
        //
        if (supportsCalendarEvents(device)) {
            deviceSpecificSettings.addRootScreen(
                    DeviceSpecificSettingsScreen.CALENDAR,
                    R.xml.devicesettings_sync_calendar
            );
        }

        //
        // Other
        //
        if (getContactsSlotCount(device) > 0) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_contacts);
        }
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_offline_voice);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_device_actions_without_not_wear);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_phone_silent_mode);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_buttonactions_upper_long);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_buttonactions_lower_short);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_weardirection);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_camera_remote);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_morning_updates);

        //
        // Connection
        //
        final List<Integer> connection = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CONNECTION);
        connection.add(R.xml.devicesettings_expose_hr_thirdparty);
        connection.add(R.xml.devicesettings_bt_connected_advertisement);
        connection.add(R.xml.devicesettings_high_mtu);

        //
        // Developer
        //
        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        if (ZeppOsLogsService.isSupported(getPrefs(device))) {
            developer.add(R.xml.devicesettings_app_logs_start_stop);
        }
        if (supportsAssistant(device)) {
            developer.add(R.xml.devicesettings_zeppos_assistant);
        }
        if (supportsWifiHotspot(device)) {
            developer.add(R.xml.devicesettings_wifi_hotspot);
        }
        if (supportsFtpServer(device)) {
            developer.add(R.xml.devicesettings_ftp_server);
        }
        developer.add(R.xml.devicesettings_keep_activity_data_on_device);
        developer.add(R.xml.devicesettings_huami2021_fetch_operation_time_unit);

        return deviceSpecificSettings;
    }

    @Override
    public List<HuamiVibrationPatternNotificationType> getVibrationPatternNotificationTypes(final GBDevice device) {
        final List<HuamiVibrationPatternNotificationType> notificationTypes = new ArrayList<>(Arrays.asList(
                HuamiVibrationPatternNotificationType.APP_ALERTS,
                HuamiVibrationPatternNotificationType.INCOMING_CALL,
                HuamiVibrationPatternNotificationType.INCOMING_SMS,
                HuamiVibrationPatternNotificationType.GOAL_NOTIFICATION,
                HuamiVibrationPatternNotificationType.ALARM,
                HuamiVibrationPatternNotificationType.IDLE_ALERTS
        ));

        if (getReminderSlotCount(device) > 0) {
            notificationTypes.add(HuamiVibrationPatternNotificationType.EVENT_REMINDER);
        }

        if (!supportsContinuousFindDevice(device)) {
            notificationTypes.add(HuamiVibrationPatternNotificationType.FIND_BAND);
        }

        if (supportsToDoList()) {
            notificationTypes.add(HuamiVibrationPatternNotificationType.SCHEDULE);
            notificationTypes.add(HuamiVibrationPatternNotificationType.TODO_LIST);
        }

        return notificationTypes;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new ZeppOsSettingsCustomizer(device, getVibrationPatternNotificationTypes(device));
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    public final boolean supportsContinuousFindDevice(final GBDevice device) {
        return ZeppOsFindDeviceService.supportsContinuousFindDevice(getPrefs(device));
    }

    @Override
    public boolean supportsTemperatureMeasurement(@NonNull final GBDevice device) {
        return !hasDisplay() || supportsDisplayItem(device, "thermometer");
    }

    @Override
    public boolean supportsContinuousTemperature(@NonNull final GBDevice device) {
        return supportsTemperatureMeasurement(device);
    }

    public boolean supportsAgpsUpdates() {
        return hasDisplay();
    }

    /**
     * true for Zepp OS 2.0+, false for Zepp OS 1
     */
    public boolean sendAgpsAsFileTransfer() {
        return true;
    }

    public boolean supportsGpxUploads(final GBDevice device) {
        return supportsBleFileTransfer(device, "sport");
    }

    public boolean supportsControlCenter() {
        // TODO: Auto-detect control center?
        return false;
    }

    public boolean supportsToDoList() {
        // TODO: Not yet implemented
        // TODO: When implemented, query the capability like reminders
        return false;
    }

    public boolean hasDisplay() {
        return true;
    }

    public boolean mainMenuHasMoreSection() {
        // Devices that have a control center don't seem to have a "more" section in the main menu
        return !supportsControlCenter();
    }

    public boolean supportsWifiHotspot(final GBDevice device) {
        return false;
    }

    public boolean supportsFtpServer(final GBDevice device) {
        return false;
    }

    public boolean hasGps(final GBDevice device) {
        return supportsConfig(device, ZeppOsConfigService.ConfigArg.AGPS_UPDATE_TIME);
    }

    public boolean supportsAutoBrightness(final GBDevice device) {
        return supportsConfig(device, ZeppOsConfigService.ConfigArg.SCREEN_AUTO_BRIGHTNESS);
    }

    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return ZeppOsPhoneService.isSupported(getPrefs(device));
    }

    public boolean supportsShortcutCards(final GBDevice device) {
        return ZeppOsShortcutCardsService.isSupported(getPrefs(device));
    }

    public boolean supportsAssistant(final GBDevice device) {
        return experimentalFeatures(device) && ZeppOsAssistantService.isSupported(getPrefs(device));
    }

    public boolean supportsMaps(final GBDevice device) {
        return supportsDisplayItem(device, "map");
    }

    public boolean supportsMusicUpload(final GBDevice device) {
        return supportsDisplayItem(device, "music") && supportsBleFileTransfer(device, "music");
    }

    private boolean supportsConfig(final GBDevice device, final ZeppOsConfigService.ConfigArg config) {
        return ZeppOsConfigService.deviceHasConfig(getPrefs(device), config);
    }

    private boolean supportsDisplayItem(final GBDevice device, final String item) {
        return getPrefs(device).getList(
                DeviceSettingsUtils.getPrefPossibleValuesKey(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE),
                Collections.emptyList()
        ).contains(item);
    }

    private boolean supportsBleFileTransfer(final GBDevice device, final String service) {
        return getPrefs(device)
                .getStringSet(ZeppOsFileTransferImpl.PREF_SUPPORTED_SERVICES, Collections.emptySet())
                .contains(service);
    }

    public static boolean experimentalFeatures(final GBDevice device) {
        return getPrefs(device).getBoolean("zepp_os_experimental_features", false);
    }

    @Override
    public boolean validateAuthKey(final String authKey) {
        final byte[] authKeyBytes = authKey.trim().getBytes();
        return authKeyBytes.length == 32 || (authKey.trim().startsWith("0x") && authKeyBytes.length == 34);
    }
}
