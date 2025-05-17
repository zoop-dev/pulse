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
import nodomain.freeyourgadget.gadgetbridge.model.WearingState;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class GBDeviceEventWearState extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventWearState.class);

    public final WearingState wearingState;

    public GBDeviceEventWearState(final WearingState wearingState) {
        this.wearingState = wearingState;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + String.format(Locale.ROOT, "wearingState=%s", wearingState);
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        if (this.wearingState == WearingState.UNKNOWN) {
            LOG.warn("WEAR_STATE state is UNKNOWN, aborting further evaluation");
            return;
        }

        if (this.wearingState != WearingState.NOT_WEARING) {
            LOG.debug("WEAR_STATE state is not NOT_WEARING, aborting further evaluation");
        }

        final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);

        Set<String> actionOnUnwear = devicePrefs.getStringSet(
                DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_START_NON_WEAR_SELECTIONS,
                Collections.emptySet()
        );

        // check if an action is set
        if (actionOnUnwear.isEmpty()) {
            return;
        }

        String broadcastMessage = devicePrefs.getString(
                DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST_ACTION,
                context.getString(R.string.prefs_events_forwarding_startnonwear_broadcast_default_value)
        );

        String broadcastPackage = devicePrefs.getString(
                DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST_PACKAGE,
                ""
        );

        handleDeviceAction(context, device, actionOnUnwear, broadcastMessage, broadcastPackage);
    }
}
