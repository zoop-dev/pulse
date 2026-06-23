/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.PulseWeekActivity;

/** Pulse: fires a "week in review" notification each Sunday ~7pm that opens the
 *  animated weekly breakdown. Reschedules itself after each firing; also (re)armed
 *  whenever the app is opened. */
public class PulseWeeklyRecapReceiver extends BroadcastReceiver {
    public static final String ACTION = "nodomain.freeyourgadget.gadgetbridge.PULSE_WEEKLY_RECAP";
    private static final int RECAP_NOTIF_ID = 0x50B1;
    private static final int RECAP_HOUR = 19; // 7pm

    @Override
    public void onReceive(final Context context, final Intent intent) {
        notifyRecap(context);
        schedule(context); // arm next Sunday
    }

    /** (Re)arm the next Sunday 7pm recap alarm (idempotent). */
    public static void schedule(final Context context) {
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextSundayEvening(), alarmIntent(context));
    }

    private static long nextSundayEvening() {
        final Calendar c = GregorianCalendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, RECAP_HOUR);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 7);
        }
        return c.getTimeInMillis();
    }

    private static PendingIntent alarmIntent(final Context context) {
        final Intent i = new Intent(context, PulseWeeklyRecapReceiver.class).setAction(ACTION);
        return PendingIntent.getBroadcast(context, 0x50B1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void notifyRecap(final Context context) {
        final NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        final Intent open = new Intent(context, PulseWeekActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent contentIntent = PendingIntent.getActivity(context, 0x50B2, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        final NotificationCompat.Builder b = new NotificationCompat.Builder(context, GB.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.pulse_neon))
                .setContentTitle(context.getString(R.string.pulse_week_recap_notif_title))
                .setContentText(context.getString(R.string.pulse_week_recap_notif_text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        nm.notify(RECAP_NOTIF_ID, b.build());
    }
}
