/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.model.heartratezones;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;


public abstract class HeartRateZonesSpec {

    public enum PostureType {
        UPRIGHT,
        SITTING,
        SWIMMING,
        OTHER
    }

    public static final String HR_ZONES_PREF_KEY_PREFIX = "heart_rate_zones_";
    public static final String HR_ZONES_PREF_KEY = HR_ZONES_PREF_KEY_PREFIX + "posture_%s_method_%s";
    public static final String HR_ZONES_PREF_WARNING_ENABLED_KEY = HR_ZONES_PREF_KEY_PREFIX + "posture_%s_warning_enabled";
    public static final String HR_ZONES_PREF_HEART_RATE_LIMIT_KEY = HR_ZONES_PREF_KEY_PREFIX + "posture_%s_heart_rate_limit";
    public static final String HR_ZONES_PREF_CALCULATE_METHOD_KEY = HR_ZONES_PREF_KEY_PREFIX + "posture_%s_calculate_method";

    protected final GBDevice device;

    public HeartRateZonesSpec(GBDevice device) {
        this.device = device;
    }


    public abstract String getNameByType(Context context, PostureType type);

    public abstract List<HeartRateZonesConfig> getDeviceConfig();


    private String getPrefKey(HeartRateZonesSpec.PostureType type, HeartRateZones.CalculationMethod method) {
        return String.format(Locale.ROOT, HR_ZONES_PREF_KEY, type.toString(), method.toString());
    }

    protected String getHrZoneConfig(PostureType type, HeartRateZones.CalculationMethod method) {
        DevicePrefs devicePreferences = GBApplication.getDevicePrefs(device);
        String key = getPrefKey(type, method);
        return devicePreferences.getString(key, null);
    }

    protected HeartRateZonesConfig loadOrCreateHeartRateZonesConfig(PostureType type, boolean warningEnable, int warningHRLimit, HeartRateZones.CalculationMethod currentCalculationMethod, List<HeartRateZones> configByMethods) {
        DevicePrefs devicePreferences = GBApplication.getDevicePrefs(device);

        warningEnable = devicePreferences.getBoolean(String.format(Locale.ROOT, HR_ZONES_PREF_WARNING_ENABLED_KEY, type.toString()), warningEnable);
        warningHRLimit = devicePreferences.getInt(String.format(Locale.ROOT, HR_ZONES_PREF_HEART_RATE_LIMIT_KEY, type.toString()), warningHRLimit);
        HeartRateZones.CalculationMethod[] values = HeartRateZones.CalculationMethod.values();
        int val = devicePreferences.getInt(String.format(Locale.ROOT, HR_ZONES_PREF_CALCULATE_METHOD_KEY, type.toString()), currentCalculationMethod.ordinal());
        if (val >= 0 && val < values.length) {
            currentCalculationMethod = values[val];
        }
        return new HeartRateZonesConfig(type,
                warningEnable,
                warningHRLimit,
                currentCalculationMethod,
                configByMethods
        );
    }

    public void saveConfig(HeartRateZonesConfig config) {
        final DevicePrefs devicePreferences = GBApplication.getDevicePrefs(device);
        final SharedPreferences.Editor editor = devicePreferences.getPreferences().edit();

        editor.putBoolean(String.format(Locale.ROOT, HR_ZONES_PREF_WARNING_ENABLED_KEY, config.getType().toString()), config.getWarningEnable());
        editor.putInt(String.format(Locale.ROOT, HR_ZONES_PREF_HEART_RATE_LIMIT_KEY, config.getType().toString()), config.getWarningHRLimit());
        editor.putInt(String.format(Locale.ROOT, HR_ZONES_PREF_CALCULATE_METHOD_KEY, config.getType().toString()), config.getCurrentCalculationMethod().ordinal());

        Gson gson = new Gson();
        for (HeartRateZones zn : config.getConfigByMethods()) {
            String key = getPrefKey(config.getType(), zn.getMethod());
            editor.putString(key, gson.toJson(zn));
        }
        editor.apply();
    }

    public void clearConfig() {
        final DevicePrefs devicePreferences = GBApplication.getDevicePrefs(device);
        SharedPreferences sharedPrefs = devicePreferences.getPreferences();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Map<String, ?> allEntries = sharedPrefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if(entry.getKey().startsWith(HR_ZONES_PREF_KEY_PREFIX)) {
                editor.remove(entry.getKey());
            }
        }
        editor.apply();
    }
}
