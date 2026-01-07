package nodomain.freeyourgadget.gadgetbridge.prefs.migrators;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.AMAZFITBIP;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.AMAZFITCOR;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.AMAZFITCOR2;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.FITPRO;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.GALAXY_BUDS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.LEFUN;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.MIBAND;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.MIBAND2;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.MIBAND2_HRX;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.MIBAND3;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.PEBBLE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.TLW64;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceType.WATCHXPLUS;

import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.prefs.AbstractPreferenceMigrator;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * This preference migrator includes all changes up to version 55 - migrated as-is from the GBApplication class to avoid
 * introducing regresions.
 */
@SuppressWarnings("deprecation")
public class PreferenceMigrator55 extends AbstractPreferenceMigrator {
    private final SharedPreferences sharedPrefs;
    private final GBPrefs prefs;

    public PreferenceMigrator55() {
        this.sharedPrefs = GBApplication.getPrefs().getPreferences();
        this.prefs = GBApplication.getPrefs();
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void migrate(final int oldVersion, final SharedPreferences sharedPrefs, final SharedPreferences.Editor editor) {
        // this comes before all other migrations since the new column DeviceTypeName was added as non-null
        if (oldVersion < 25) {
            migrateDeviceTypes();
        }

        if (oldVersion == 0) {
            String legacyGender = sharedPrefs.getString("mi_user_gender", null);
            String legacyHeight = sharedPrefs.getString("mi_user_height_cm", null);
            String legacyWeight = sharedPrefs.getString("mi_user_weight_kg", null);
            String legacyYOB = sharedPrefs.getString("mi_user_year_of_birth", null);
            if (legacyGender != null) {
                int gender = "male".equals(legacyGender) ? 1 : "female".equals(legacyGender) ? 0 : 2;
                editor.putString(ActivityUser.PREF_USER_GENDER, Integer.toString(gender));
                editor.remove("mi_user_gender");
            }
            if (legacyHeight != null) {
                editor.putString(ActivityUser.PREF_USER_HEIGHT_CM, legacyHeight);
                editor.remove("mi_user_height_cm");
            }
            if (legacyWeight != null) {
                editor.putString(ActivityUser.PREF_USER_WEIGHT_KG, legacyWeight);
                editor.remove("mi_user_weight_kg");
            }
            if (legacyYOB != null) {
                editor.putString("activity_user_year_of_birth", legacyYOB);
                editor.remove("mi_user_year_of_birth");
            }
        }
        if (oldVersion < 2) {
            //migrate the integer version of gender introduced in version 1 to a string value, needed for the way Android accesses the shared preferences
            int legacyGender_1 = 2;
            try {
                legacyGender_1 = sharedPrefs.getInt(ActivityUser.PREF_USER_GENDER, 2);
            } catch (Exception e) {
                Log.e(TAG, "Could not access legacy activity gender", e);
            }
            editor.putString(ActivityUser.PREF_USER_GENDER, Integer.toString(legacyGender_1));
        }
        if (oldVersion < 3) {
            try (DBHandler db = acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
                for (Device dbDevice : activeDevices) {
                    SharedPreferences deviceSpecificSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    if (deviceSpecificSharedPrefs != null) {
                        SharedPreferences.Editor deviceSharedPrefsEdit = deviceSpecificSharedPrefs.edit();
                        String preferenceKey = dbDevice.getIdentifier() + "_lastSportsActivityTimeMillis";
                        long lastSportsActivityTimeMillis = sharedPrefs.getLong(preferenceKey, 0);
                        if (lastSportsActivityTimeMillis != 0) {
                            deviceSharedPrefsEdit.putLong("lastSportsActivityTimeMillis", lastSportsActivityTimeMillis);
                            editor.remove(preferenceKey);
                        }
                        preferenceKey = dbDevice.getIdentifier() + "_lastSyncTimeMillis";
                        long lastSyncTimeMillis = sharedPrefs.getLong(preferenceKey, 0);
                        if (lastSyncTimeMillis != 0) {
                            deviceSharedPrefsEdit.putLong("lastSyncTimeMillis", lastSyncTimeMillis);
                            editor.remove(preferenceKey);
                        }

                        String newLanguage = null;
                        Set<String> displayItems = null;

                        DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());

                        if (deviceType == AMAZFITBIP || deviceType == AMAZFITCOR || deviceType == AMAZFITCOR2) {
                            int oldLanguage = prefs.getInt("amazfitbip_language", -1);
                            newLanguage = "auto";
                            String[] oldLanguageLookup = {"zh_CN", "zh_TW", "en_US", "es_ES", "ru_RU", "de_DE", "it_IT", "fr_FR", "tr_TR"};
                            if (oldLanguage >= 0 && oldLanguage < oldLanguageLookup.length) {
                                newLanguage = oldLanguageLookup[oldLanguage];
                            }
                        }

                        if (deviceType == AMAZFITBIP || deviceType == AMAZFITCOR) {
                            deviceSharedPrefsEdit.putString("disconnect_notification", prefs.getString("disconnect_notification", "off"));
                            deviceSharedPrefsEdit.putString("disconnect_notification_start", prefs.getString("disconnect_notification_start", "8:00"));
                            deviceSharedPrefsEdit.putString("disconnect_notification_end", prefs.getString("disconnect_notification_end", "22:00"));
                        }
                        if (deviceType == MIBAND2 || deviceType == MIBAND2_HRX || deviceType == MIBAND3) {
                            deviceSharedPrefsEdit.putString("do_not_disturb", prefs.getString("mi2_do_not_disturb", "off"));
                            deviceSharedPrefsEdit.putString("do_not_disturb_start", prefs.getString("mi2_do_not_disturb_start", "1:00"));
                            deviceSharedPrefsEdit.putString("do_not_disturb_end", prefs.getString("mi2_do_not_disturb_end", "6:00"));
                        }
                        if (dbDevice.getManufacturer().equals("Huami")) {
                            deviceSharedPrefsEdit.putString("activate_display_on_lift_wrist", prefs.getString("activate_display_on_lift_wrist", "off"));
                            deviceSharedPrefsEdit.putString("display_on_lift_start", prefs.getString("display_on_lift_start", "0:00"));
                            deviceSharedPrefsEdit.putString("display_on_lift_end", prefs.getString("display_on_lift_end", "0:00"));
                        }
                        switch (deviceType) {
                            case MIBAND:
                                deviceSharedPrefsEdit.putBoolean("low_latency_fw_update", prefs.getBoolean("mi_low_latency_fw_update", true));
                                deviceSharedPrefsEdit.putString("device_time_offset_hours", String.valueOf(prefs.getInt("mi_device_time_offset_hours", 0)));
                                break;
                            case AMAZFITCOR:
                                displayItems = prefs.getStringSet("cor_display_items", null);
                                break;
                            case AMAZFITBIP:
                                displayItems = prefs.getStringSet("bip_display_items", null);
                                break;
                            case MIBAND2:
                            case MIBAND2_HRX:
                                displayItems = prefs.getStringSet("mi2_display_items", null);
                                deviceSharedPrefsEdit.putBoolean("mi2_enable_text_notifications", prefs.getBoolean("mi2_enable_text_notifications", true));
                                deviceSharedPrefsEdit.putString("mi2_dateformat", prefs.getString("mi2_dateformat", "dateformat_time"));
                                deviceSharedPrefsEdit.putBoolean("rotate_wrist_to_cycle_info", prefs.getBoolean("mi2_rotate_wrist_to_switch_info", false));
                                break;
                            case MIBAND3:
                                newLanguage = prefs.getString("miband3_language", "auto");
                                displayItems = prefs.getStringSet("miband3_display_items", null);
                                deviceSharedPrefsEdit.putBoolean("swipe_unlock", prefs.getBoolean("mi3_band_screen_unlock", false));
                                deviceSharedPrefsEdit.putString("night_mode", prefs.getString("mi3_night_mode", "off"));
                                deviceSharedPrefsEdit.putString("night_mode_start", prefs.getString("mi3_night_mode_start", "16:00"));
                                deviceSharedPrefsEdit.putString("night_mode_end", prefs.getString("mi3_night_mode_end", "7:00"));

                        }
                        if (displayItems != null) {
                            deviceSharedPrefsEdit.putStringSet("display_items", displayItems);
                        }
                        if (newLanguage != null) {
                            deviceSharedPrefsEdit.putString("language", newLanguage);
                        }
                        deviceSharedPrefsEdit.apply();
                    }
                }
                editor.remove("amazfitbip_language");
                editor.remove("bip_display_items");
                editor.remove("cor_display_items");
                editor.remove("disconnect_notification");
                editor.remove("disconnect_notification_start");
                editor.remove("disconnect_notification_end");
                editor.remove("activate_display_on_lift_wrist");
                editor.remove("display_on_lift_start");
                editor.remove("display_on_lift_end");

                editor.remove("mi_low_latency_fw_update");
                editor.remove("mi_device_time_offset_hours");
                editor.remove("mi2_do_not_disturb");
                editor.remove("mi2_do_not_disturb_start");
                editor.remove("mi2_do_not_disturb_end");
                editor.remove("mi2_dateformat");
                editor.remove("mi2_display_items");
                editor.remove("mi2_rotate_wrist_to_switch_info");
                editor.remove("mi2_enable_text_notifications");
                editor.remove("mi3_band_screen_unlock");
                editor.remove("mi3_night_mode");
                editor.remove("mi3_night_mode_start");
                editor.remove("mi3_night_mode_end");
                editor.remove("miband3_language");

            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 3", e);
            }
        }
        if (oldVersion < 4) {
            try (DBHandler db = acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
                for (Device dbDevice : activeDevices) {
                    SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());

                    if (deviceType == MIBAND) {
                        int deviceTimeOffsetHours = deviceSharedPrefs.getInt("device_time_offset_hours", 0);
                        deviceSharedPrefsEdit.putString("device_time_offset_hours", Integer.toString(deviceTimeOffsetHours));
                    }

                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 4", e);
            }
        }
        if (oldVersion < 5) {
            try (DBHandler db = acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
                for (Device dbDevice : activeDevices) {
                    SharedPreferences deviceSpecificSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    if (deviceSpecificSharedPrefs != null) {
                        SharedPreferences.Editor deviceSharedPrefsEdit = deviceSpecificSharedPrefs.edit();
                        DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());

                        String newWearside = null;
                        String newOrientation = null;
                        String newTimeformat = null;
                        switch (deviceType) {
                            case AMAZFITBIP:
                            case AMAZFITCOR:
                            case AMAZFITCOR2:
                            case MIBAND:
                            case MIBAND2:
                            case MIBAND2_HRX:
                            case MIBAND3:
                            case MIBAND4:
                                newWearside = prefs.getString("mi_wearside", "left");
                                break;
                            case MIBAND5:
                                newWearside = prefs.getString("mi_wearside", "left");
                                break;
                            case HPLUS:
                                newWearside = prefs.getString("hplus_wrist", "left");
                                newTimeformat = prefs.getString("hplus_timeformat", "24h");
                                break;
                            case ID115:
                                newWearside = prefs.getString("id115_wrist", "left");
                                newOrientation = prefs.getString("id115_screen_orientation", "horizontal");
                                break;
                            case ZETIME:
                                newWearside = prefs.getString("zetime_wrist", "left");
                                newTimeformat = prefs.getInt("zetime_timeformat", 1) == 2 ? "am/pm" : "24h";
                                break;
                        }
                        if (newWearside != null) {
                            deviceSharedPrefsEdit.putString("wearlocation", newWearside);
                        }
                        if (newOrientation != null) {
                            deviceSharedPrefsEdit.putString("screen_orientation", newOrientation);
                        }
                        if (newTimeformat != null) {
                            deviceSharedPrefsEdit.putString("timeformat", newTimeformat);
                        }
                        deviceSharedPrefsEdit.apply();
                    }
                }
                editor.remove("hplus_timeformat");
                editor.remove("hplus_wrist");
                editor.remove("id115_wrist");
                editor.remove("id115_screen_orientation");
                editor.remove("mi_wearside");
                editor.remove("zetime_timeformat");
                editor.remove("zetime_wrist");

            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 5", e);
            }
        }
        if (oldVersion < 6) {
            migrateBooleanPrefToPerDevicePref("mi2_enable_button_action", false, "button_action_enable", new ArrayList<>(Collections.singletonList(MIBAND2)));
            migrateBooleanPrefToPerDevicePref("mi2_button_action_vibrate", false, "button_action_vibrate", new ArrayList<>(Collections.singletonList(MIBAND2)));
            migrateStringPrefToPerDevicePref("mi_button_press_count", "6", "button_action_press_count", new ArrayList<>(Collections.singletonList(MIBAND2)));
            migrateStringPrefToPerDevicePref("mi_button_press_count_max_delay", "2000", "button_action_press_max_interval", new ArrayList<>(Collections.singletonList(MIBAND2)));
            migrateStringPrefToPerDevicePref("mi_button_press_count_match_delay", "0", "button_action_broadcast_delay", new ArrayList<>(Collections.singletonList(MIBAND2)));
            migrateStringPrefToPerDevicePref("mi_button_press_broadcast", "nodomain.freeyourgadget.gadgetbridge.ButtonPressed", "button_action_broadcast", new ArrayList<>(Collections.singletonList(MIBAND2)));
        }
        if (oldVersion < 7) {
            migrateStringPrefToPerDevicePref("mi_reserve_alarm_calendar", "0", "reserve_alarms_calendar", new ArrayList<>(Arrays.asList(MIBAND, MIBAND2)));
        }

        if (oldVersion < 8) {
            for (int i = 1; i <= 16; i++) {
                String message = prefs.getString("canned_message_dismisscall_" + i, null);
                if (message != null) {
                    migrateStringPrefToPerDevicePref("canned_message_dismisscall_" + i, "", "canned_message_dismisscall_" + i, new ArrayList<>(Collections.singletonList(PEBBLE)));
                }
            }
            for (int i = 1; i <= 16; i++) {
                String message = prefs.getString("canned_reply_" + i, null);
                if (message != null) {
                    migrateStringPrefToPerDevicePref("canned_reply_" + i, "", "canned_reply_" + i, new ArrayList<>(Collections.singletonList(PEBBLE)));
                }
            }
        }
        if (oldVersion < 9) {
            try (DBHandler db = acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
                migrateBooleanPrefToPerDevicePref("transliteration", false, "pref_transliteration_enabled", (ArrayList) activeDevices);
                Log.w(TAG, "migrating transliteration settings");
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 9", e);
            }
        }
        if (oldVersion < 10) {
            //migrate the string version of pref_galaxy_buds_ambient_volume to int due to transition to SeekBarPreference
            try (DBHandler db = acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
                for (Device dbDevice : activeDevices) {
                    SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());

                    if (deviceType == GALAXY_BUDS) {
                        GB.log("migrating Galaxy Buds volume", GB.INFO, null);
                        String volume = deviceSharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOLUME, "1");
                        deviceSharedPrefsEdit.putInt(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOLUME, Integer.parseInt(volume));
                    }
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 10", e);
            }
        }
        if (oldVersion < 11) {
            try (DBHandler db = acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
                for (Device dbDevice : activeDevices) {
                    SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
                    if (deviceType == WATCHXPLUS || deviceType == FITPRO || deviceType == LEFUN) {
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_enable", deviceSharedPrefs.getBoolean("pref_longsit_switch", false));
                        deviceSharedPrefsEdit.remove("pref_longsit_switch");
                    }
                    if (deviceType == WATCHXPLUS || deviceType == FITPRO) {
                        deviceSharedPrefsEdit.putString("inactivity_warnings_start", deviceSharedPrefs.getString("pref_longsit_start", "06:00"));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_end", deviceSharedPrefs.getString("pref_longsit_end", "23:00"));
                        deviceSharedPrefsEdit.remove("pref_longsit_start");
                        deviceSharedPrefsEdit.remove("pref_longsit_end");
                    }
                    if (deviceType == WATCHXPLUS || deviceType == LEFUN) {
                        deviceSharedPrefsEdit.putString("inactivity_warnings_threshold", deviceSharedPrefs.getString("pref_longsit_period", "60"));
                        deviceSharedPrefsEdit.remove("pref_longsit_period");
                    }
                    if (deviceType == TLW64) {
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_enable_noshed", deviceSharedPrefs.getBoolean("screen_longsit_noshed", false));
                        deviceSharedPrefsEdit.remove("screen_longsit_noshed");
                    }
                    if (dbDevice.getManufacturer().equals("Huami")) {
                        editor.putBoolean("inactivity_warnings_dnd", prefs.getBoolean("mi2_inactivity_warnings_dnd", false));
                        editor.putString("inactivity_warnings_dnd_start", prefs.getString("mi2_inactivity_warnings_dnd_start", "12:00"));
                        editor.putString("inactivity_warnings_dnd_end", prefs.getString("mi2_inactivity_warnings_dnd_end", "14:00"));
                        editor.putBoolean("inactivity_warnings_enable", prefs.getBoolean("mi2_inactivity_warnings", false));
                        editor.putInt("inactivity_warnings_threshold", prefs.getInt("mi2_inactivity_warnings_threshold", 60));
                        editor.putString("inactivity_warnings_start", prefs.getString("mi2_inactivity_warnings_start", "06:00"));
                        editor.putString("inactivity_warnings_end", prefs.getString("mi2_inactivity_warnings_end", "22:00"));
                    }
                    switch (deviceType) {
                        case LEFUN:
                            deviceSharedPrefsEdit.putString("language", deviceSharedPrefs.getString("pref_lefun_interface_language", "0"));
                            deviceSharedPrefsEdit.remove("pref_lefun_interface_language");
                            break;
                        case FITPRO:
                            deviceSharedPrefsEdit.putString("inactivity_warnings_threshold", deviceSharedPrefs.getString("pref_longsit_period", "4"));
                            deviceSharedPrefsEdit.remove("pref_longsit_period");
                            break;
                        case ZETIME:
                            editor.putString("do_not_disturb", prefs.getString("zetime_do_not_disturb", "off"));
                            editor.putString("do_not_disturb_start", prefs.getString("zetime_do_not_disturb_start", "22:00"));
                            editor.putString("do_not_disturb_end", prefs.getString("zetime_do_not_disturb_end", "07:00"));
                            editor.putBoolean("inactivity_warnings_enable", prefs.getBoolean("zetime_inactivity_warnings", false));
                            editor.putString("inactivity_warnings_start", prefs.getString("zetime_inactivity_warnings_start", "06:00"));
                            editor.putString("inactivity_warnings_end", prefs.getString("zetime_inactivity_warnings_end", "22:00"));
                            editor.putInt("inactivity_warnings_threshold", prefs.getInt("zetime_inactivity_warnings_threshold", 60));
                            editor.putBoolean("inactivity_warnings_mo", prefs.getBoolean("zetime_prefs_inactivity_repetitions_mo", false));
                            editor.putBoolean("inactivity_warnings_tu", prefs.getBoolean("zetime_prefs_inactivity_repetitions_tu", false));
                            editor.putBoolean("inactivity_warnings_we", prefs.getBoolean("zetime_prefs_inactivity_repetitions_we", false));
                            editor.putBoolean("inactivity_warnings_th", prefs.getBoolean("zetime_prefs_inactivity_repetitions_th", false));
                            editor.putBoolean("inactivity_warnings_fr", prefs.getBoolean("zetime_prefs_inactivity_repetitions_fr", false));
                            editor.putBoolean("inactivity_warnings_sa", prefs.getBoolean("zetime_prefs_inactivity_repetitions_sa", false));
                            editor.putBoolean("inactivity_warnings_su", prefs.getBoolean("zetime_prefs_inactivity_repetitions_su", false));
                            break;
                    }
                    deviceSharedPrefsEdit.apply();
                }
                editor.putInt("fitness_goal", prefs.getInt("mi_fitness_goal", 8000));

                editor.remove("zetime_do_not_disturb");
                editor.remove("zetime_do_not_disturb_start");
                editor.remove("zetime_do_not_disturb_end");
                editor.remove("zetime_inactivity_warnings");
                editor.remove("zetime_inactivity_warnings_start");
                editor.remove("zetime_inactivity_warnings_end");
                editor.remove("zetime_inactivity_warnings_threshold");
                editor.remove("zetime_prefs_inactivity_repetitions_mo");
                editor.remove("zetime_prefs_inactivity_repetitions_tu");
                editor.remove("zetime_prefs_inactivity_repetitions_we");
                editor.remove("zetime_prefs_inactivity_repetitions_th");
                editor.remove("zetime_prefs_inactivity_repetitions_fr");
                editor.remove("zetime_prefs_inactivity_repetitions_sa");
                editor.remove("zetime_prefs_inactivity_repetitions_su");
                editor.remove("mi2_inactivity_warnings_dnd");
                editor.remove("mi2_inactivity_warnings_dnd_start");
                editor.remove("mi2_inactivity_warnings_dnd_end");
                editor.remove("mi2_inactivity_warnings");
                editor.remove("mi2_inactivity_warnings_threshold");
                editor.remove("mi2_inactivity_warnings_start");
                editor.remove("mi2_inactivity_warnings_end");
                editor.remove("mi_fitness_goal");
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 11", e);
            }
        }
        if (oldVersion < 12) {
            // Convert preferences that were wrongly migrated to int, since Android saves them as Strings internally
            editor.putString("inactivity_warnings_threshold", String.valueOf(prefs.getInt("inactivity_warnings_threshold", 60)));
            editor.putString("fitness_goal", String.valueOf(prefs.getInt("fitness_goal", 8000)));
        }

        if (oldVersion < 13) {
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                    if (dbDevice.getManufacturer().equals("Huami")) {
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_enable", prefs.getBoolean("inactivity_warnings_enable", false));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_threshold", prefs.getString("inactivity_warnings_threshold", "60"));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_start", prefs.getString("inactivity_warnings_start", "06:00"));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_end", prefs.getString("inactivity_warnings_end", "22:00"));

                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_dnd", prefs.getBoolean("inactivity_warnings_dnd", false));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_dnd_start", prefs.getString("inactivity_warnings_dnd_start", "12:00"));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_dnd_end", prefs.getString("inactivity_warnings_dnd_end", "14:00"));

                        deviceSharedPrefsEdit.putBoolean("fitness_goal_notification", prefs.getBoolean("mi2_goal_notification", false));
                    }

                    // Not removing the first 4 preferences since they're still used by some devices (ZeTime)
                    editor.remove("inactivity_warnings_dnd");
                    editor.remove("inactivity_warnings_dnd_start");
                    editor.remove("inactivity_warnings_dnd_end");
                    editor.remove("mi2_goal_notification");

                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 13", e);
            }
        }

        if (oldVersion < 14) {
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                    if (DeviceType.MIBAND.equals(dbDevice.getType()) || dbDevice.getManufacturer().equals("Huami")) {
                        deviceSharedPrefsEdit.putBoolean("heartrate_sleep_detection", prefs.getBoolean("mi_hr_sleep_detection", false));
                        deviceSharedPrefsEdit.putString("heartrate_measurement_interval", prefs.getString("heartrate_measurement_interval", "0"));
                    }

                    // Not removing heartrate_measurement_interval since it's still used by some devices (ZeTime)
                    editor.remove("mi_hr_sleep_detection");

                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 14", e);
            }
        }

        if (oldVersion < 15) {
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                    if (DeviceType.FITPRO.equals(dbDevice.getType())) {
                        editor.remove("inactivity_warnings_threshold");
                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 15", e);
            }
        }

        if (oldVersion < 16) {
            // If transliteration was enabled for a device, migrate it to the per-language setting
            final String defaultLanguagesIfEnabled = "extended_ascii,common_symbols,scandinavian,german,russian,hebrew,greek,ukranian,arabic,persian,latvian,lithuanian,polish,estonian,icelandic,czech,turkish,bengali,korean,hungarian";
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                    if (deviceSharedPrefs.getBoolean("pref_transliteration_enabled", false)) {
                        deviceSharedPrefsEdit.putString("pref_transliteration_languages", defaultLanguagesIfEnabled);
                    }

                    deviceSharedPrefsEdit.remove("pref_transliteration_enabled");

                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 16", e);
            }
        }

        if (oldVersion < 17) {
            final HashSet<String> calendarBlacklist = (HashSet<String>) prefs.getStringSet(GBPrefs.CALENDAR_BLACKLIST, null);

            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                    deviceSharedPrefsEdit.putBoolean("sync_calendar", prefs.getBoolean("enable_calendar_sync", true));

                    if (calendarBlacklist != null) {
                        Prefs.putStringSet(deviceSharedPrefsEdit, GBPrefs.CALENDAR_BLACKLIST, calendarBlacklist);
                    }

                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 17", e);
            }

            editor.remove(GBPrefs.CALENDAR_BLACKLIST);
        }

        if (oldVersion < 18) {
            // Migrate the default value for Huami find band vibration pattern
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    if (!dbDevice.getManufacturer().equals("Huami")) {
                        continue;
                    }

                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                    deviceSharedPrefsEdit.putString("huami_vibration_profile_find_band", "long");
                    deviceSharedPrefsEdit.putString("huami_vibration_count_find_band", "1");

                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 18", e);
            }
        }

        if (oldVersion < 19) {
            //remove old ble scanning prefences, now unsupported
            editor.remove("disable_new_ble_scanning");
        }

        if (oldVersion < 20) {
            // Add the new stress tab to all devices
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        newPrefValue = chartsTabsValue + ",stress";
                    } else {
                        newPrefValue = "stress";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 20", e);
            }
        }

        if (oldVersion < 21) {
            // Add the new PAI tab to all devices
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        newPrefValue = chartsTabsValue + ",pai";
                    } else {
                        newPrefValue = "pai";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 21", e);
            }
        }

        if (oldVersion < 22) {
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
                    if (deviceType == MIBAND2) {
                        final String name = dbDevice.getName();
                        if ("Mi Band HRX".equalsIgnoreCase(name) || "Mi Band 2i".equalsIgnoreCase(name)) {
                            dbDevice.setTypeName(DeviceType.MIBAND2_HRX.name());
                            daoSession.getDeviceDao().update(dbDevice);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 22", e);
            }
        }

        if (oldVersion < 26) {
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        newPrefValue = chartsTabsValue + ",spo2";
                    } else {
                        newPrefValue = "spo2";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 26", e);
            }
        }

        if (oldVersion < 27) {
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                    for (final Map.Entry<String, ?> entry : deviceSharedPrefs.getAll().entrySet()) {
                        final String key = entry.getKey();
                        if (key.startsWith("huami_2021_known_config_")) {
                            deviceSharedPrefsEdit.putString(
                                    key.replace("huami_2021_known_config_", "") + "_is_known",
                                    entry.getValue().toString()
                            );
                        } else if (key.endsWith("_huami_2021_possible_values")) {
                            deviceSharedPrefsEdit.putString(
                                    key.replace("_huami_2021_possible_values", "") + "_possible_values",
                                    entry.getValue().toString()
                            );
                        }
                    }

                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 27", e);
            }
        }

        if (oldVersion < 28) {
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    boolean shouldApply = false;

                    if (!"UNKNOWN".equals(deviceSharedPrefs.getString("events_forwarding_fellsleep_action_selection", "UNKNOWN"))) {
                        shouldApply = true;
                        deviceSharedPrefsEdit.putStringSet(
                                "events_forwarding_fellsleep_action_selections",
                                Collections.singleton(deviceSharedPrefs.getString("events_forwarding_fellsleep_action_selection", "UNKNOWN"))
                        );
                    }
                    if (!"UNKNOWN".equals(deviceSharedPrefs.getString("events_forwarding_wokeup_action_selection", "UNKNOWN"))) {
                        shouldApply = true;
                        deviceSharedPrefsEdit.putStringSet(
                                "events_forwarding_wokeup_action_selections",
                                Collections.singleton(deviceSharedPrefs.getString("events_forwarding_wokeup_action_selection", "UNKNOWN"))
                        );
                    }
                    if (!"UNKNOWN".equals(deviceSharedPrefs.getString("events_forwarding_startnonwear_action_selection", "UNKNOWN"))) {
                        shouldApply = true;
                        deviceSharedPrefsEdit.putStringSet(
                                "events_forwarding_startnonwear_action_selections",
                                Collections.singleton(deviceSharedPrefs.getString("events_forwarding_startnonwear_action_selection", "UNKNOWN"))
                        );
                    }

                    if (shouldApply) {
                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 28", e);
            }
        }

        if (oldVersion < 29) {
            // Migrate HPlus preferences to device-specific
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
                    if (deviceType == DeviceType.HPLUS) {
                        final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                        final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                        deviceSharedPrefsEdit.putString("hplus_screentime", sharedPrefs.getString("hplus_screentime", "5"));
                        deviceSharedPrefsEdit.putBoolean("hplus_alldayhr", sharedPrefs.getBoolean("hplus_alldayhr", true));
                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 29", e);
            }
        }

        if (oldVersion < 30) {
            // Migrate QHybrid preferences to device-specific
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
                    if (deviceType == DeviceType.FOSSILQHYBRID) {
                        final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                        final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                        deviceSharedPrefsEdit.putInt("QHYBRID_TIME_OFFSET", sharedPrefs.getInt("QHYBRID_TIME_OFFSET", 0));
                        deviceSharedPrefsEdit.putInt("QHYBRID_TIMEZONE_OFFSET", sharedPrefs.getInt("QHYBRID_TIMEZONE_OFFSET", 0));
                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 30", e);
            }
        }

        if (oldVersion < 31) {
            // Add the new HRV Status tab to all devices
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        newPrefValue = chartsTabsValue + ",hrvstatus";
                    } else {
                        newPrefValue = "hrvstatus";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 31", e);
            }
        }

        if (oldVersion < 32) {
            // Add the new body energy tab to all devices
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        newPrefValue = chartsTabsValue + ",bodyenergy";
                    } else {
                        newPrefValue = "bodyenergy";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 32", e);
            }
        }

        if (oldVersion < 33) {
            // Remove sleep week tab from all devices, since it does not exist anymore
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        newPrefValue = chartsTabsValue.replace(",sleepweek", "");
                    } else {
                        newPrefValue = chartsTabsValue;
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 33", e);
            }
        }

        if (oldVersion < 34) {
            // Migrate Mi Band preferences to device-specific
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
                    if (deviceType == MIBAND || deviceType == MIBAND2 || deviceType == MIBAND2_HRX) {
                        final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                        final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                        deviceSharedPrefsEdit.putString("mi_vibration_profile_generic_sms", sharedPrefs.getString("mi_vibration_profile_generic_sms", "staccato"));
                        deviceSharedPrefsEdit.putString("mi_vibration_count_generic_sms", sharedPrefs.getString("mi_vibration_count_generic_sms", "3"));
                        deviceSharedPrefsEdit.putString("mi_vibration_profile_incoming_call", sharedPrefs.getString("mi_vibration_profile_incoming_call", "ring"));
                        deviceSharedPrefsEdit.putString("mi_vibration_count_incoming_call", sharedPrefs.getString("mi_vibration_count_incoming_call", "60"));
                        deviceSharedPrefsEdit.putString("mi_vibration_profile_generic_email", sharedPrefs.getString("mi_vibration_profile_generic_email", "medium"));
                        deviceSharedPrefsEdit.putString("mi_vibration_count_generic_email", sharedPrefs.getString("mi_vibration_count_generic_email", "2"));
                        deviceSharedPrefsEdit.putString("mi_vibration_profile_generic_chat", sharedPrefs.getString("mi_vibration_profile_generic_chat", "waterdrop"));
                        deviceSharedPrefsEdit.putString("mi_vibration_count_generic_chat", sharedPrefs.getString("mi_vibration_count_generic_chat", "3"));
                        deviceSharedPrefsEdit.putString("mi_vibration_profile_generic_social", sharedPrefs.getString("mi_vibration_profile_generic_social", "waterdrop"));
                        deviceSharedPrefsEdit.putString("mi_vibration_count_generic_social", sharedPrefs.getString("mi_vibration_count_generic_social", "3"));
                        deviceSharedPrefsEdit.putString("mi_vibration_profile_alarm_clock", sharedPrefs.getString("mi_vibration_profile_alarm_clock", "alarm_clock"));
                        deviceSharedPrefsEdit.putString("mi_vibration_count_alarm_clock", sharedPrefs.getString("mi_vibration_count_alarm_clock", "3"));
                        deviceSharedPrefsEdit.putString("mi_vibration_profile_generic_navigation", sharedPrefs.getString("mi_vibration_profile_generic_navigation", "waterdrop"));
                        deviceSharedPrefsEdit.putString("mi_vibration_count_generic_navigation", sharedPrefs.getString("mi_vibration_count_generic_navigation", "3"));
                        deviceSharedPrefsEdit.putString("mi_vibration_profile_generic", sharedPrefs.getString("mi_vibration_profile_generic", "waterdrop"));
                        deviceSharedPrefsEdit.putString("mi_vibration_count_generic", sharedPrefs.getString("mi_vibration_count_generic", "3"));

                        if (deviceType == MIBAND) {
                            deviceSharedPrefsEdit.putBoolean("keep_activity_data_on_device", sharedPrefs.getBoolean("mi_dont_ack_transfer", false));
                        }

                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 34", e);
            }
        }

        if (oldVersion < 35) {
            // Migrate ZeTime preferences to device-specific
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
                    if (deviceType == DeviceType.ZETIME) {
                        final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                        final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                        // Vibration Profiles
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_sms", sharedPrefs.getString("zetime_vibration_profile_sms", "2"));
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_incoming_call", sharedPrefs.getString("zetime_vibration_profile_incoming_call", "13"));
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_missed_call", sharedPrefs.getString("zetime_vibration_profile_missed_call", "12"));
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_generic_email", sharedPrefs.getString("zetime_vibration_profile_generic_email", "12"));
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_generic_social", sharedPrefs.getString("zetime_vibration_profile_generic_social", "12"));
                        deviceSharedPrefsEdit.putString("zetime_alarm_signaling", sharedPrefs.getString("zetime_alarm_signaling", "11"));
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_calendar", sharedPrefs.getString("zetime_vibration_profile_calendar", "12"));
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_inactivity", sharedPrefs.getString("zetime_vibration_profile_inactivity", "12"));
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_lowpower", sharedPrefs.getString("zetime_vibration_profile_lowpower", "4"));
                        deviceSharedPrefsEdit.putString("zetime_vibration_profile_antiloss", sharedPrefs.getString("zetime_vibration_profile_antiloss", "13"));
                        // DND
                        deviceSharedPrefsEdit.putString("do_not_disturb_no_auto", sharedPrefs.getString("do_not_disturb", "off"));
                        deviceSharedPrefsEdit.putString("do_not_disturb_no_auto_start", sharedPrefs.getString("do_not_disturb_start", "22:00"));
                        deviceSharedPrefsEdit.putString("do_not_disturb_no_auto_end", sharedPrefs.getString("do_not_disturb_end", "07:00"));
                        // HR
                        deviceSharedPrefsEdit.putString("heartrate_measurement_interval", sharedPrefs.getString("heartrate_measurement_interval", "0"));
                        deviceSharedPrefsEdit.putBoolean("zetime_heartrate_alarm_enable", sharedPrefs.getBoolean("zetime_heartrate_alarm_enable", false));
                        deviceSharedPrefsEdit.putString("alarm_max_heart_rate", sharedPrefs.getString("alarm_max_heart_rate", "180"));
                        deviceSharedPrefsEdit.putString("alarm_min_heart_rate", sharedPrefs.getString("alarm_min_heart_rate", "60"));
                        // Inactivity warnings
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_enable", sharedPrefs.getBoolean("inactivity_warnings_enable", false));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_threshold", sharedPrefs.getString("inactivity_warnings_threshold", "60"));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_start", sharedPrefs.getString("inactivity_warnings_start", "06:00"));
                        deviceSharedPrefsEdit.putString("inactivity_warnings_end", sharedPrefs.getString("inactivity_warnings_end", "22:00"));
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_mo", sharedPrefs.getBoolean("inactivity_warnings_mo", false));
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_tu", sharedPrefs.getBoolean("inactivity_warnings_tu", false));
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_we", sharedPrefs.getBoolean("inactivity_warnings_we", false));
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_th", sharedPrefs.getBoolean("inactivity_warnings_th", false));
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_fr", sharedPrefs.getBoolean("inactivity_warnings_fr", false));
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_sa", sharedPrefs.getBoolean("inactivity_warnings_sa", false));
                        deviceSharedPrefsEdit.putBoolean("inactivity_warnings_su", sharedPrefs.getBoolean("inactivity_warnings_su", false));
                        // Developer settings
                        deviceSharedPrefsEdit.putBoolean("keep_activity_data_on_device", sharedPrefs.getBoolean("zetime_dont_del_actdata", false));
                        // Activity info
                        deviceSharedPrefsEdit.putBoolean("zetime_activity_tracking", sharedPrefs.getBoolean("zetime_activity_tracking", false));
                        deviceSharedPrefsEdit.putString("zetime_calories_type", sharedPrefs.getString("zetime_calories_type", "0"));
                        // Display
                        deviceSharedPrefsEdit.putString("zetime_screentime", sharedPrefs.getString("zetime_screentime", "30"));
                        deviceSharedPrefsEdit.putBoolean("zetime_handmove_display", sharedPrefs.getBoolean("zetime_handmove_display", false));
                        deviceSharedPrefsEdit.putString("zetime_analog_mode", sharedPrefs.getString("zetime_analog_mode", "0"));
                        // Date format
                        deviceSharedPrefsEdit.putString("zetime_date_format", sharedPrefs.getString("zetime_date_format", "2"));
                        // Unused, but migrate it anyway
                        deviceSharedPrefsEdit.putString("zetime_shock_strength", sharedPrefs.getString("zetime_shock_strength", "255"));

                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 35", e);
            }
        }
        if (oldVersion < 36) {
            // Migrate Pebble preferences to device-specific
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
                    if (deviceType == PEBBLE) {
                        final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                        final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();

                        deviceSharedPrefsEdit.putBoolean("pebble_enable_outgoing_call", sharedPrefs.getBoolean("pebble_enable_outgoing_call", true));
                        deviceSharedPrefsEdit.putString("pebble_pref_privacy_mode", sharedPrefs.getString("pebble_pref_privacy_mode", "off"));
                        deviceSharedPrefsEdit.putBoolean("send_sunrise_sunset", sharedPrefs.getBoolean("send_sunrise_sunset", false));
                        deviceSharedPrefsEdit.putString("pebble_activitytracker", sharedPrefs.getString("pebble_activitytracker", String.valueOf(SampleProvider.PROVIDER_PEBBLE_HEALTH)));
                        deviceSharedPrefsEdit.putBoolean("pebble_sync_health", sharedPrefs.getBoolean("pebble_sync_health", true));
                        deviceSharedPrefsEdit.putBoolean("pebble_health_store_raw", sharedPrefs.getBoolean("pebble_health_store_raw", true));
                        deviceSharedPrefsEdit.putBoolean("pebble_sync_misfit", sharedPrefs.getBoolean("pebble_sync_misfit", true));
                        deviceSharedPrefsEdit.putBoolean("pebble_sync_morpheuz", sharedPrefs.getBoolean("pebble_sync_morpheuz", true));
                        deviceSharedPrefsEdit.putBoolean("pebble_force_untested", sharedPrefs.getBoolean("pebble_force_untested", false));
                        deviceSharedPrefsEdit.putBoolean("pebble_force_le", sharedPrefs.getBoolean("pebble_force_le", false));
                        deviceSharedPrefsEdit.putString("pebble_mtu_limit", sharedPrefs.getString("pebble_mtu_limit", "512"));
                        deviceSharedPrefsEdit.putBoolean("pebble_gatt_clientonly", sharedPrefs.getBoolean("pebble_gatt_clientonly", false));
                        deviceSharedPrefsEdit.putBoolean("pebble_enable_applogs", sharedPrefs.getBoolean("pebble_enable_applogs", false));
                        deviceSharedPrefsEdit.putBoolean("third_party_apps_set_settings", sharedPrefs.getBoolean("pebble_enable_pebblekit", false));
                        deviceSharedPrefsEdit.putBoolean("pebble_always_ack_pebblekit", sharedPrefs.getBoolean("pebble_always_ack_pebblekit", false));
                        deviceSharedPrefsEdit.putBoolean("pebble_enable_background_javascript", sharedPrefs.getBoolean("pebble_enable_background_javascript", false));

                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 36", e);
            }
        }

        if (oldVersion < 37) {
            // Add new dashboard widgets
            final String dashboardWidgetsOrder = sharedPrefs.getString("pref_dashboard_widgets_order", null);
            if (!StringUtils.isBlank(dashboardWidgetsOrder) && !dashboardWidgetsOrder.contains("bodyenergy")) {
                editor.putString("pref_dashboard_widgets_order", dashboardWidgetsOrder + ",bodyenergy,stress_segmented,hrv");
            }
        }

        if (oldVersion < 38) {
            // Migrate year of birth to date of birth
            try {
                final String yearOfBirth = sharedPrefs.getString("activity_user_year_of_birth", null);
                if (StringUtils.isNotBlank(yearOfBirth)) {
                    final int yearOfBirthValue = Integer.parseInt(yearOfBirth);
                    if (yearOfBirthValue > 1800 && yearOfBirthValue < 3000) {
                        editor.putString("activity_user_date_of_birth", String.format(Locale.ROOT, "%s-01-01", yearOfBirth.trim()));
                    } else {
                        Log.e(TAG, "Year of birth out of range, not migrating - " + yearOfBirth);
                    }
                }
            } catch (final Exception e) {
                Log.e(TAG, "Failed to migrate year of birth to date of birth in version 38", e);
            }
        }

        if (oldVersion < 39) {
            // Add the new Heart Rate tab to all devices
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    String newPrefValue = chartsTabsValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        if (!chartsTabsValue.contains("heartrate")) {
                            newPrefValue = newPrefValue + ",heartrate";
                        }
                    } else {
                        newPrefValue = "heartrate";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 39", e);
            }
        }

        if (oldVersion < 40) {
            // Add the new VO2Max tab to all devices
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        newPrefValue = chartsTabsValue + ",vo2max";
                    } else {
                        newPrefValue = "vo2max";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 40", e);
            }
        }

        if (oldVersion < 41) {
            // Add vo2max widget.
            final String dashboardWidgetsOrder = sharedPrefs.getString("pref_dashboard_widgets_order", null);
            if (!StringUtils.isBlank(dashboardWidgetsOrder) && !dashboardWidgetsOrder.contains("vo2max")) {
                editor.putString("pref_dashboard_widgets_order", dashboardWidgetsOrder + ",vo2max");
            }
        }

        if (oldVersion < 42) {
            // Enable crash notification by default on debug builds
            if (!prefs.contains("crash_notification")) {
                editor.putBoolean("crash_notification", GBApplication.isDebug());
            }
        }

        if (oldVersion < 43) {
            // Add the new calories tab to all devices.
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        if (!chartsTabsValue.contains("calories")) {
                            newPrefValue = chartsTabsValue + ",calories";
                        } else {
                            newPrefValue = chartsTabsValue;
                        }
                    } else {
                        newPrefValue = "calories";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 43", e);
            }
        }

        if (oldVersion < 44) {
            // Users upgrading to this version don't need to see the welcome screen
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                if (!activeDevices.isEmpty()) {
                    editor.putBoolean("first_run", false);
                }
            } catch (final Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 44", e);
            }
        }

        if (oldVersion < 45) {
            // Add the new respiratory rate tab to all devices.
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        if (!chartsTabsValue.contains("respiratoryrate")) {
                            newPrefValue = chartsTabsValue + ",respiratoryrate";
                        } else {
                            newPrefValue = chartsTabsValue;
                        }
                    } else {
                        newPrefValue = "respiratoryrate";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 45", e);
            }
        }

        if (oldVersion < 46) {
            // Enable calendar sync on Garmin devices by default
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    if (dbDevice.getTypeName().startsWith("GARMIN")) {
                        final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                        final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                        deviceSharedPrefsEdit.putBoolean("sync_calendar", true);
                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 46", e);
            }
        }

        if (oldVersion < 47) {
            if (prefs.contains("activity_user_goal_standing_time_minutes")) {
                editor.putString("activity_user_goal_standing_hours", prefs.getString("activity_user_goal_standing_time_minutes", "12"));
                editor.remove("activity_user_goal_standing_time_minutes");
            }
        }

        if (oldVersion < 48) {
            // Fix the reversed notification time prefs
            if (prefs.getNotificationTimesEnabled()) {
                final String start = prefs.getString("notification_times_start", "08:00");
                final String end = prefs.getString("notification_times_end", "22:00");
                editor.putString("notification_times_start", end);
                editor.putString("notification_times_end", start);
            }
        }

        if (oldVersion < 49) {
            // Migrate Lenovo Watch X language preference
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (Device dbDevice : activeDevices) {
                    final DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());
                    if (deviceType == WATCHXPLUS) {
                        final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                        final String languageVal = deviceSharedPrefs.getString("language", "1");
                        final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                        switch (languageVal) {
                            case "0":
                                deviceSharedPrefsEdit.putString("language", "zh_CN");
                                break;
                            case "1":
                            default:
                                deviceSharedPrefsEdit.putString("language", "en_US");
                                break;

                        }
                        deviceSharedPrefsEdit.apply();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 49", e);
            }
        }

        if (oldVersion < 50) {
            // Add the new Load tab.
            try (DBHandler db = acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                for (final Device dbDevice : activeDevices) {
                    final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());

                    final String chartsTabsValue = deviceSharedPrefs.getString("charts_tabs", null);
                    if (chartsTabsValue == null) {
                        continue;
                    }

                    final String newPrefValue;
                    if (!StringUtils.isBlank(chartsTabsValue)) {
                        if (!chartsTabsValue.contains("load")) {
                            newPrefValue = chartsTabsValue + ",load";
                        } else {
                            newPrefValue = chartsTabsValue;
                        }
                    } else {
                        newPrefValue = "load";
                    }

                    final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                    deviceSharedPrefsEdit.putString("charts_tabs", newPrefValue);
                    deviceSharedPrefsEdit.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to migrate prefs to version 50", e);
            }
        }

        if (oldVersion < 51) {
            if (prefs.contains("activity_user_sleep_duration")) {
                int hours = prefs.getInt("activity_user_sleep_duration", -1);
                if (hours > -1){
                    editor.putString("activity_user_sleep_duration_minutes", String.valueOf(hours * 60));
                }
            }
        }

        if (oldVersion < 53) {
            if (prefs.contains("activity_user_sleep_duration_minutes")) {
                final int minutes = prefs.getInt("activity_user_sleep_duration_minutes", 7 * 60);
                editor.remove("activity_user_sleep_duration_minutes");
                editor.putString("activity_user_sleep_duration_minutes", String.valueOf(minutes));
            }
        }

        if (oldVersion < 54) {
            // #5414 - Some old Android versions misbehave
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try (DBHandler db = acquireDB()) {
                    final DaoSession daoSession = db.getDaoSession();
                    final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

                    for (final Device dbDevice : activeDevices) {
                        final SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                        final SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
                        deviceSharedPrefsEdit.putBoolean("connection_force_legacy_gatt", true);
                        deviceSharedPrefsEdit.apply();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to migrate prefs to version 54", e);
                }
            }
        }
    }

    private DBHandler acquireDB() throws GBException {
        return GBApplication.acquireDB();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void migrateDeviceTypes() {
        try (DBHandler db = acquireDB()) {
            final InputStream inputStream = GBApplication.app().getAssets().open("migrations/devicetype.json");
            final byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            final JSONObject deviceMapping = new JSONObject(new String(buffer));
            final JSONObject deviceIdNameMapping = deviceMapping.getJSONObject("by-id");

            final DaoSession daoSession = db.getDaoSession();
            final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);

            for (Device dbDevice : activeDevices) {
                String deviceTypeName = dbDevice.getTypeName();
                if (deviceTypeName.isEmpty() || deviceTypeName.equals("UNKNOWN")) {
                    deviceTypeName = deviceIdNameMapping.optString(
                            String.valueOf(dbDevice.getType()),
                            "UNKNOWN"
                    );
                    dbDevice.setTypeName(deviceTypeName);
                    daoSession.getDeviceDao().update(dbDevice);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to migrate device types", e);
        }
    }

    private void migrateStringPrefToPerDevicePref(String globalPref, String globalPrefDefault, String perDevicePref, ArrayList<DeviceType> deviceTypes) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        String globalPrefValue = prefs.getString(globalPref, globalPrefDefault);
        try (DBHandler db = acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
            for (Device dbDevice : activeDevices) {
                SharedPreferences deviceSpecificSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                if (deviceSpecificSharedPrefs != null) {
                    SharedPreferences.Editor deviceSharedPrefsEdit = deviceSpecificSharedPrefs.edit();
                    DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());

                    if (deviceTypes.contains(deviceType)) {
                        Log.i(TAG, "migrating global string preference " + globalPref + " for " + deviceType.name() + " " + dbDevice.getIdentifier());
                        deviceSharedPrefsEdit.putString(perDevicePref, globalPrefValue);
                    }
                    deviceSharedPrefsEdit.apply();
                }
            }
            editor.remove(globalPref);
            editor.apply();
        } catch (Exception e) {
            Log.w(TAG, "error acquiring DB lock");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void migrateBooleanPrefToPerDevicePref(String globalPref, Boolean globalPrefDefault, String perDevicePref, ArrayList<DeviceType> deviceTypes) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        boolean globalPrefValue = prefs.getBoolean(globalPref, globalPrefDefault);
        try (DBHandler db = acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
            for (Device dbDevice : activeDevices) {
                SharedPreferences deviceSpecificSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                if (deviceSpecificSharedPrefs != null) {
                    SharedPreferences.Editor deviceSharedPrefsEdit = deviceSpecificSharedPrefs.edit();
                    DeviceType deviceType = DeviceType.fromName(dbDevice.getTypeName());

                    if (deviceTypes.contains(deviceType)) {
                        Log.i(TAG, "migrating global boolean preference " + globalPref + " for " + deviceType.name() + " " + dbDevice.getIdentifier());
                        deviceSharedPrefsEdit.putBoolean(perDevicePref, globalPrefValue);
                    }
                    deviceSharedPrefsEdit.apply();
                }
            }
            editor.remove(globalPref);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to migrate " + globalPref, e);
        }
    }
}
