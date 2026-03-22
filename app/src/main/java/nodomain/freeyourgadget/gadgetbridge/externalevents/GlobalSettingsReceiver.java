/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class GlobalSettingsReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalSettingsReceiver.class);

    public static final String COMMAND = "nodomain.freeyourgadget.gadgetbridge.action.SET_GLOBAL_SETTING";

    @Override
    @SuppressLint("ApplySharedPref")
    // use commit to ensure it's already applied when we call the device
    public void onReceive(final Context context, final Intent intent) {
        if (!COMMAND.equals(intent.getAction())) {
            LOG.warn("Unexpected action {}", intent.getAction());
        }

        final GBPrefs prefs = GBApplication.getPrefs();

        if (!prefs.getBoolean("intent_api_allow_global_settings", false)) {
            LOG.warn("Setting global settings from 3rd party apps not allowed");
            return;
        }

        final String key = intent.getStringExtra("key");
        final Object value = intent.getExtras().get("value");

        if (key == null) {
            LOG.warn("No key specified");
            return;
        }

        LOG.info("Setting '{}' to '{}'", key, value);

        final SharedPreferences.Editor editor = prefs.getPreferences().edit();

        if (value == null) {
            editor.remove(key);
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
            editor.putStringSet(key, (Set) value);
        } else {
            LOG.warn("Unknown preference value type {} for {}", value.getClass(), key);
        }

        editor.commit();

        GBApplication.deviceService().onSendConfiguration(key);
    }
}
