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
package nodomain.freeyourgadget.gadgetbridge;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.NumberFormat;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;

/** Pulse: a compact single-stat steps widget (today's steps + goal progress). */
public class WidgetSteps extends AppWidgetProvider {

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        for (final int id : appWidgetIds) {
            updateOne(context, appWidgetManager, id);
        }
    }

    private void updateOne(final Context context, final AppWidgetManager mgr, final int id) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_steps);

        int steps = 0;
        final int goal = new ActivityUser().getStepsGoal();
        final GBDevice device = firstDevice();
        if (device != null) {
            try {
                steps = (int) DailyTotals.getDailyTotalsForDevice(device, java.util.Calendar.getInstance()).getSteps();
            } catch (final Exception ignored) {
            }
        }

        views.setTextViewText(R.id.widget_steps_value, NumberFormat.getIntegerInstance().format(steps));
        views.setTextViewText(R.id.widget_steps_goal,
                context.getString(R.string.pulse_of_goal, NumberFormat.getIntegerInstance().format(goal)));
        final int pct = goal > 0 ? Math.min(100, Math.round(steps * 100f / goal)) : 0;
        views.setProgressBar(R.id.widget_steps_progress, 100, pct, false);

        // tap → open Pulse
        final Intent open = new Intent(context, ControlCenterv2.class);
        final PendingIntent pi = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_steps_root, pi);

        // top-right refresh → re-read + redraw this widget
        final Intent refresh = new Intent(context, WidgetSteps.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{id});
        final PendingIntent refreshPi = PendingIntent.getBroadcast(context, id, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_steps_refresh, refreshPi);

        mgr.updateAppWidget(id, views);
    }

    private GBDevice firstDevice() {
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (final GBDevice d : devices) {
            if (d.isInitialized()) {
                return d;
            }
        }
        return devices.isEmpty() ? null : devices.get(0);
    }
}
