/*  Copyright (C) 2023-2024 Yoran Vulker

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

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.SleepState;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class GBDeviceEventSleepStateDetection extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventSleepStateDetection.class);

    public final SleepState sleepState;

    public GBDeviceEventSleepStateDetection(final SleepState sleepState) {
        this.sleepState = sleepState;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + String.format(Locale.ROOT, "sleepState=%s", sleepState);
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        if (this.sleepState == SleepState.UNKNOWN) {
            return;
        }

        final String actionPreferenceKey, messagePreferenceKey, packagePreferenceKey;
        final int defaultBroadcastMessageResource;

        switch (this.sleepState) {
            case AWAKE:
                actionPreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_WOKE_UP_SELECTIONS;
                messagePreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_WOKE_UP_BROADCAST_ACTION;
                packagePreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_WOKE_UP_BROADCAST_PACKAGE;
                defaultBroadcastMessageResource = R.string.prefs_events_forwarding_wokeup_broadcast_default_value;
                break;
            case ASLEEP:
                actionPreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_FELL_SLEEP_SELECTIONS;
                messagePreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST_ACTION;
                packagePreferenceKey = DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST_PACKAGE;
                defaultBroadcastMessageResource = R.string.prefs_events_forwarding_fellsleep_broadcast_default_value;
                break;
            default:
                LOG.error("Unable to deduce action and broadcast message preference key for sleep state {}", this.sleepState);
                return;
        }

        final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);

        final Set<String> actions = devicePrefs.getStringSet(actionPreferenceKey, Collections.emptySet());

        if (actions.isEmpty()) {
            return;
        }

        String broadcastMessage = devicePrefs.getString(messagePreferenceKey, context.getString(defaultBroadcastMessageResource));
        String broadcastPackage = devicePrefs.getString(packagePreferenceKey, "");
        handleDeviceAction(context, device, actions, broadcastMessage, broadcastPackage);
    }
}
