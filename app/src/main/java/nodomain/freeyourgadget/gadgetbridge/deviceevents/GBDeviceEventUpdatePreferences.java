/*  Copyright (C) 2021-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GBDeviceEventUpdatePreferences extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventUpdatePreferences.class);

    public final Map<String, Object> preferences;

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("preferences: ");
        for (Map.Entry<String, Object> e : preferences.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append(", ");
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    public GBDeviceEventUpdatePreferences() {
        this.preferences = new HashMap<>();
    }

    public GBDeviceEventUpdatePreferences(final Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public GBDeviceEventUpdatePreferences(final String key, final Object value) {
        this.preferences = new HashMap<>();
        this.preferences.put(key, value);
    }

    public GBDeviceEventUpdatePreferences withPreference(final String key, final Object value) {
        this.preferences.put(key, value);

        return this;
    }

    public GBDeviceEventUpdatePreferences withPreferences(final Map<String, Object> preferences) {
        this.preferences.putAll(preferences);

        return this;
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        update(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
        device.sendDeviceUpdateIntent(context);
    }

    /**
     * Update a {@link SharedPreferences} instance with the preferences in the event.
     *
     * @param prefs the SharedPreferences object to update.
     */
    private void update(final SharedPreferences prefs) {
        final SharedPreferences.Editor editor = prefs.edit();

        for (Map.Entry<String, Object> e : preferences.entrySet()) {
            final String key = e.getKey();
            final Object value = e.getValue();

            LOG.trace("Updating {} = {}", key, value);

            if (value == null) {
                editor.remove(key);
            } else if (value instanceof Short) {
                editor.putInt(key, (Short) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Set) {
                //noinspection unchecked,rawtypes
                editor.putStringSet(key, (Set) value);
            } else {
                LOG.warn("Unknown preference value type {} for {}", value.getClass(), key);
            }
        }

        editor.apply();
    }
}
