/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CmfPreferences {
    private static final Logger LOG = LoggerFactory.getLogger(CmfPreferences.class);

    private final CmfWatchProSupport mSupport;

    protected CmfPreferences(final CmfWatchProSupport support) {
        this.mSupport = support;
    }

    protected void onSetHeartRateMeasurementInterval(final int seconds) {
        final boolean enabled = seconds == -1;
        LOG.debug("Set HR smart monitoring = {}", enabled);

        final byte[] cmd = new byte[]{0x01, (byte) (enabled ? 0x01 : 0x00)};
        mSupport.sendCommand("set hr monitoring", CmfCommand.HEART_MONITORING_ENABLED_SET, cmd);
    }

    protected void onSendConfiguration(final String config) {
        final TransactionBuilder builder = mSupport.createTransactionBuilder("set config " + config);

        switch (config) {
            case ActivityUser.PREF_USER_STEPS_GOAL:
            case ActivityUser.PREF_USER_DISTANCE_METERS:
            case ActivityUser.PREF_USER_CALORIES_BURNT:
                setGoals(builder);
                break;
            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                setMeasurementSystem(builder);
                break;
            case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
                setLanguage(builder);
                break;
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                setTimeFormat(builder);
                break;
            case DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED:
                setDisplayOnLift(builder);
                break;
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD:
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD:
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_ACTIVE_HIGH_THRESHOLD:
            case DeviceSettingsPreferenceConst.PREF_SPO2_LOW_ALERT_THRESHOLD:
                setHeartAlerts(builder);
                break;
            case DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING:
                setSpo2MonitoringInterval(builder);
                break;
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING:
                setStressMonitoringInterval(builder);
                break;
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_START:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_END:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_START:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_END:
                setStandingReminder(builder);
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_PERIOD:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_DND:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_DND_START:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_DND_END:
                setHydrationReminder(builder);
                break;
            case HuamiConst.PREF_WORKOUT_ACTIVITY_TYPES_SORTABLE:
                setActivityTypes(builder);
                break;
            case DeviceSettingsPreferenceConst.PREF_BLUETOOTH_CALLS_ENABLED:
                setCallReminders(builder);
                break;
            default:
                LOG.warn("Unknown config changed: {}", config);
        }

        if (!builder.isEmpty()) {
            builder.queue();
        }
    }

    protected void setGoals(final TransactionBuilder builder) {
        final ActivityUser activityUser = new ActivityUser();

        if (activityUser.getStepsGoal() <= 0) {
            LOG.warn("Invalid steps goal {}", activityUser.getStepsGoal());
            return;
        }

        if (activityUser.getDistanceGoalMeters() <= 0) {
            LOG.warn("Invalid distance goal {}", activityUser.getDistanceGoalMeters());
            return;
        }

        if (activityUser.getCaloriesBurntGoal() <= 0) {
            LOG.warn("Invalid calories goal {}", activityUser.getCaloriesBurntGoal());
            return;
        }

        LOG.debug(
                "Setting goals, steps={}, distance={}, calories={}",
                activityUser.getStepsGoal(),
                activityUser.getDistanceGoalMeters(),
                activityUser.getCaloriesBurntGoal()
        );

        final ByteBuffer buf = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0); // ?
        buf.put((byte) 0); // ?
        buf.putShort((short) activityUser.getStepsGoal());
        buf.put((byte) 0); // ?
        buf.put((byte) 0); // ?
        buf.putShort((short) activityUser.getDistanceGoalMeters());
        buf.putShort((short) activityUser.getCaloriesBurntGoal());

        mSupport.sendCommand(builder, CmfCommand.GOALS_SET, buf.array());
    }

    protected void setMeasurementSystem(final TransactionBuilder builder) {
        final String measurementSystem = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, "metric");

        LOG.debug("Setting measurement system to {}", measurementSystem);

        final byte unitByte = (byte) ("metric".equals(measurementSystem) ? 0x00 : 0x01);

        final byte[] cmd = new byte[]{0x01, unitByte};
        mSupport.sendCommand(builder, CmfCommand.UNIT_LENGTH, cmd);
        mSupport.sendCommand(builder, CmfCommand.UNIT_TEMPERATURE, cmd);
    }

    protected void setLanguage(final TransactionBuilder builder) {
        String localeString = mSupport.getDevicePrefs().getString(
                DeviceSettingsPreferenceConst.PREF_LANGUAGE, DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO
        );
        if (DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO.equals(localeString)) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();

            if (nodomain.freeyourgadget.gadgetbridge.util.StringUtils.isNullOrEmpty(country)) {
                // sometimes country is null, no idea why, guess it.
                country = language;
            }
            localeString = (language + "_" + country).toLowerCase(Locale.ROOT);
        }

        String languageCommand = null;
        if (LANGUAGES.containsKey(localeString)) {
            languageCommand = localeString;
        } else {
            // Break down the language code and attempt to find it
            final String[] languageParts = localeString.split("_");
            for (int i = 0; i < languageParts.length; i++) {
                if (LANGUAGES.containsKey(languageParts[0])) {
                    languageCommand = languageParts[0];
                    break;
                }
            }
        }

        if (languageCommand == null) {
            LOG.warn("Unknown language {}", localeString);
            return;
        }

        LOG.info("Set language: {} -> {}", localeString, languageCommand);

        // FIXME watch ignores language?
        mSupport.sendCommand(builder, CmfCommand.LANGUAGE_SET, languageCommand.getBytes());
    }

    protected void setTimeFormat(final TransactionBuilder builder) {
        final String timeFormat = mSupport.getDevicePrefs().getTimeFormat();

        LOG.info("Setting time format to {}", timeFormat);

        final byte timeFormatByte = (byte) (timeFormat.equals("24h") ? 0x00 : 0x01);

        mSupport.sendCommand(builder, CmfCommand.TIME_FORMAT, timeFormatByte);
    }

    protected void setDisplayOnLift(final TransactionBuilder builder) {
        final Prefs prefs = mSupport.getDevicePrefs();

        boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED, false);

        mSupport.sendCommand(builder, CmfCommand.WAKE_ON_WRIST_RAISE, (byte) (enabled ? 0x01 : 0x00));
    }

    protected void setHeartAlerts(final TransactionBuilder builder) {
        final Prefs prefs = mSupport.getDevicePrefs();

        final int hrAlertActiveHigh = prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_ACTIVE_HIGH_THRESHOLD, 0);
        final int hrAlertHigh = prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD, 0);
        final int hrAlertLow = prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD, 0);
        final int spo2alert = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SPO2_LOW_ALERT_THRESHOLD, 0);

        final ByteBuffer buf;
        if (hrAlertActiveHigh == 0 && hrAlertHigh == 0 && hrAlertLow == 0 && spo2alert == 0) {
            buf = ByteBuffer.allocate(1).order(ByteOrder.BIG_ENDIAN);
            buf.put((byte) 0x00);
        } else {
            buf = ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN);
            buf.put((byte) 0x01);
            buf.put((byte) hrAlertLow);
            buf.put((byte) (hrAlertHigh != 0 ? hrAlertHigh : 255));
            buf.put((byte) (hrAlertActiveHigh != 0 ? hrAlertActiveHigh : 255));
            buf.put((byte) spo2alert);
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
        }

        mSupport.sendCommand(builder, CmfCommand.HEART_MONITORING_ALERTS, buf.array());
    }

    protected void setSpo2MonitoringInterval(final TransactionBuilder builder) {
        final Prefs prefs = mSupport.getDevicePrefs();
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING, false);

        LOG.debug("Set SpO2 monitoring = {}", enabled);

        final byte[] cmd = new byte[]{0x02, (byte) (enabled ? 0x01 : 0x00)};
        mSupport.sendCommand(builder, CmfCommand.HEART_MONITORING_ENABLED_SET, cmd);
    }

    protected void setStressMonitoringInterval(final TransactionBuilder builder) {
        final Prefs prefs = mSupport.getDevicePrefs();
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING, false);

        LOG.debug("Set stress monitoring = {}", enabled);

        final byte[] cmd = new byte[]{0x04, (byte) (enabled ? 0x01 : 0x00)};
        mSupport.sendCommand(builder, CmfCommand.HEART_MONITORING_ENABLED_SET, cmd);
    }

    protected void setStandingReminder(final TransactionBuilder builder) {
        final Prefs prefs = mSupport.getDevicePrefs();
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, false);
        final int threshold = prefs.getInt(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, 60);
        final boolean dnd = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND, false);
        final Date dndStart = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_START, "12:00");
        final Date dndEnd = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_END, "14:00");

        final Calendar calendar = GregorianCalendar.getInstance();

        if (threshold < 0 || threshold > 180) {
            LOG.error("Invalid inactivity threshold: {}", threshold);
            return;
        }

        final ByteBuffer buf = ByteBuffer.allocate(11).order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) (enabled ? 0x01 : 0x00));
        buf.putShort((short) threshold);

        if (enabled && dnd) {
            calendar.setTime(dndStart);
            buf.putInt((calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60));
            calendar.setTime(dndEnd);
            buf.putInt((calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60));
        } else {
            buf.putInt(0);
            buf.putInt(0);
        }

        mSupport.sendCommand(builder, CmfCommand.STANDING_REMINDER_SET, buf.array());
    }

    protected void setHydrationReminder(final TransactionBuilder builder) {
        final Prefs prefs = mSupport.getDevicePrefs();
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH, false);
        final int threshold = prefs.getInt(DeviceSettingsPreferenceConst.PREF_HYDRATION_PERIOD, 60);
        final boolean dnd = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYDRATION_DND, false);
        final Date dndStart = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_HYDRATION_DND_START, "12:00");
        final Date dndEnd = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_HYDRATION_DND_END, "14:00");

        final Calendar calendar = GregorianCalendar.getInstance();

        if (threshold < 0 || threshold > 180) {
            LOG.error("Invalid hydration threshold: {}", threshold);
            return;
        }

        final ByteBuffer buf = ByteBuffer.allocate(11).order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) (enabled ? 0x01 : 0x00));
        buf.putShort((short) threshold);

        if (enabled && dnd) {
            calendar.setTime(dndStart);
            buf.putInt((calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60));
            calendar.setTime(dndEnd);
            buf.putInt((calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60));
        } else {
            buf.putInt(0);
            buf.putInt(0);
        }

        mSupport.sendCommand(builder, CmfCommand.WATER_REMINDER_SET, buf.array());
    }

    protected void setCallReminders(final TransactionBuilder builder) {
        final Prefs prefs = mSupport.getDevicePrefs();
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_BLUETOOTH_CALLS_ENABLED, false);

        mSupport.sendCommand(builder, CmfCommand.CALL_REMINDER_REQUEST, new byte[]{0x01, (byte) (enabled ? 0x01 : 0x00)});
    }

    protected void setActivityTypes(final TransactionBuilder builder) {
        final Prefs prefs = mSupport.getDevicePrefs();
        List<String> activityTypes = new ArrayList<>(prefs.getList(HuamiConst.PREF_WORKOUT_ACTIVITY_TYPES_SORTABLE, Collections.emptyList()));

        if (activityTypes.isEmpty()) {
            activityTypes.add(CmfActivityType.OUTDOOR_RUNNING.name().toLowerCase(Locale.ROOT));
            activityTypes.add(CmfActivityType.INDOOR_RUNNING.name().toLowerCase(Locale.ROOT));
        }

        if (activityTypes.size() > 36) {
            LOG.warn("Truncating activity types list to 36");
            activityTypes = activityTypes.subList(0, 36);
        }

        final ByteBuffer buf = ByteBuffer.allocate(activityTypes.size() + 1);
        buf.put((byte) activityTypes.size());

        for (final String activityType : activityTypes) {
            buf.put(CmfActivityType.valueOf(activityType.toUpperCase(Locale.ROOT)).getCode());
        }

        mSupport.sendCommand(builder, CmfCommand.SPORTS_SET, buf.array());
    }

    protected boolean onCommand(final CmfCommand cmd, final byte[] payload) {
        // TODO handle preference replies from watch
        return false;
    }

    private static final Map<String, String> LANGUAGES = new HashMap<>() {{
        put("ar", "ar_SA");
        put("de", "de_DE");
        put("en", "en_US");
        put("es", "es_ES");
        put("fr", "fr_FR");
        put("hi", "hi_IN");
        put("in", "id_ID");
        put("it", "it_IT");
        put("ja", "ja_JP");
        put("ko", "ko_KO");
        put("zh_cn", "zh_CN");
        put("zh_hk", "zh_HK");
    }};
}
