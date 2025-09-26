/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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


import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksController;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEvent.class);

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": ";
    }

    public abstract void evaluate(final Context context, final GBDevice device);

    /**
     * Helper method to run specific actions configured in the device preferences, upon wear state
     * or awake/asleep events.
     */
    protected void handleDeviceAction(final Context context, final GBDevice device, Set<String> actions, String message, String broadcastPackage) {
        if (actions.isEmpty()) {
            return;
        }

        LOG.debug("Handing device actions: {}", TextUtils.join(",", actions));

        final String actionBroadcast = context.getString(R.string.pref_device_action_broadcast_value);
        final String actionFitnessControlStart = context.getString(R.string.pref_device_action_fitness_app_control_start_value);
        final String actionFitnessControlStop = context.getString(R.string.pref_device_action_fitness_app_control_stop_value);
        final String actionFitnessControlToggle = context.getString(R.string.pref_device_action_fitness_app_control_toggle_value);
        final String actionMediaPlay = context.getString(R.string.pref_media_play_value);
        final String actionMediaPause = context.getString(R.string.pref_media_pause_value);
        final String actionMediaPlayPause = context.getString(R.string.pref_media_playpause_value);
        final String actionDndOff = context.getString(R.string.pref_device_action_dnd_off_value);
        final String actionDndpriority = context.getString(R.string.pref_device_action_dnd_priority_value);
        final String actionDndAlarms = context.getString(R.string.pref_device_action_dnd_alarms_value);
        final String actionDndOn = context.getString(R.string.pref_device_action_dnd_on_value);

        if (actions.contains(actionBroadcast)) {
            if (message != null) {
                Intent in = new Intent();
                in.setAction(message);
                if (StringUtils.isNotBlank(broadcastPackage)) {
                    in.setPackage(broadcastPackage);
                    LOG.info("Sending broadcast {} to {}", message, broadcastPackage);
                } else {
                    LOG.info("Sending broadcast {}", message);
                }
                context.getApplicationContext().sendBroadcast(in);
            }
        }

        if (actions.contains(actionFitnessControlStart)) {
            OpenTracksController.startRecording(context);
        } else if (actions.contains(actionFitnessControlStop)) {
            OpenTracksController.stopRecording(context);
        } else if (actions.contains(actionFitnessControlToggle)) {
            OpenTracksController.toggleRecording(context);
        }

        final String mediaAction;
        if (actions.contains(actionMediaPlayPause)) {
            mediaAction = actionMediaPlayPause;
        } else if (actions.contains(actionMediaPause)) {
            mediaAction = actionMediaPause;
        } else if (actions.contains(actionMediaPlay)) {
            mediaAction = actionMediaPlay;
        } else {
            mediaAction = null;
        }

        if (mediaAction != null) {
            GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.valueOf(mediaAction);
            deviceEventMusicControl.evaluate(context, device);
        }

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        final int interruptionFilter;
        if (actions.contains(actionDndOff)) {
            interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
        } else if (actions.contains(actionDndpriority)) {
            interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY;
        } else if (actions.contains(actionDndAlarms)) {
            interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALARMS;
        } else if (actions.contains(actionDndOn)) {
            interruptionFilter = NotificationManager.INTERRUPTION_FILTER_NONE;
        } else {
            interruptionFilter = NotificationManager.INTERRUPTION_FILTER_UNKNOWN;
        }

        if (interruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
            LOG.debug("Setting do not disturb to {}", interruptionFilter);

            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                LOG.warn("Do not disturb permissions not granted");
            }

            notificationManager.setInterruptionFilter(interruptionFilter);
        }
    }
}

