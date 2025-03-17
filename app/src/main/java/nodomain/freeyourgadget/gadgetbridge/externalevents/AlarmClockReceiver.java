/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

public class AlarmClockReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmClockReceiver.class);

    public static final String PACKAGE_AOSP = "com.android.deskclock";
    public static final String PACKAGE_CLOCK_GOOGLE = "com.google.android.deskclock";

    /**
     * AlarmActivity and AlarmService (when unbound) listen for this broadcast intent
     * so that other applications can snooze the alarm (after ALARM_ALERT_ACTION and before
     * ALARM_DONE_ACTION).
     */
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    /**
     * AlarmActivity and AlarmService listen for this broadcast intent so that other
     * applications can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    /** A public action sent by AlarmService when the alarm has started. */
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";
    public static final String GOOGLE_CLOCK_ALARM_ALERT_ACTION = "com.google.android.deskclock.action.ALARM_ALERT";

    /** A public action sent by AlarmService when the alarm has stopped for any reason. */
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";
    public static final String GOOGLE_CLOCK_ALARM_DONE_ACTION = "com.google.android.deskclock.action.ALARM_DONE";
    private int lastId;


    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        LOG.debug("Got alarm action: {}", action);

        final String packageName;
        if (action.startsWith(PACKAGE_AOSP)) {
            packageName = PACKAGE_AOSP;
        } else if (action.startsWith(PACKAGE_CLOCK_GOOGLE)) {
            packageName = PACKAGE_CLOCK_GOOGLE;
        } else {
            LOG.warn("Unknown clock app package name");
            return;
        }

        if (GBApplication.getPrefs().getString("notification_list_is_blacklist", "true").equals("true")) {
            if (GBApplication.appIsNotifBlacklisted(packageName)) {
                LOG.info("Ignoring alarm action, application is blacklisted");
                return;
            }
        } else {
            if (!GBApplication.appIsNotifBlacklisted(packageName)) {
                LOG.info("Ignoring alarm action, application is not whitelisted");
                return;
            }
        }

        if (ALARM_ALERT_ACTION.equals(action) || GOOGLE_CLOCK_ALARM_ALERT_ACTION.equals(action)) {
            sendAlarm(context, true);
        } else if (ALARM_DONE_ACTION.equals(action) || GOOGLE_CLOCK_ALARM_DONE_ACTION.equals(action)) {
            sendAlarm(context, false);
        }
    }

    private synchronized void sendAlarm(Context context, boolean on) {
        dismissLastAlarm();
        if (on) {
            NotificationSpec notificationSpec = new NotificationSpec();
            //TODO: can we attach a dismiss action to the notification and not use the notification ID explicitly?
            lastId = notificationSpec.getId();
            notificationSpec.type = NotificationType.GENERIC_ALARM_CLOCK;
            notificationSpec.sourceName = "ALARMCLOCKRECEIVER";
            notificationSpec.attachedActions = new ArrayList<>();

            // DISMISS ALL action
            NotificationSpec.Action dismissAllAction = new NotificationSpec.Action();
            dismissAllAction.title = context.getString(R.string.notifications_dismiss_all);
            dismissAllAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_DISMISS_ALL;
            notificationSpec.attachedActions.add(dismissAllAction);

            // can we get the alarm title somehow?
            GBApplication.deviceService().onNotification(notificationSpec);
        }
    }

    private void dismissLastAlarm() {
        if (lastId != 0) {
            GBApplication.deviceService().onDeleteNotification(lastId);
            lastId = 0;
        }
    }
}
