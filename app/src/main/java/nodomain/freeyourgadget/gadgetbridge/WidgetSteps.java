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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.RemoteViews;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.NumberFormat;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWidgetGraphics;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWidgetMetric;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWidgetStyle;

/** Pulse: a compact single-stat widget — any metric, with goal progress. */
public class WidgetSteps extends AppWidgetProvider {

    private static final String DEFAULT_METRIC = "steps";
    private static BroadcastReceiver dataReceiver = null;

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        for (final int id : appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, id));
        }
    }

    @Override
    public void onEnabled(final Context context) {
        if (dataReceiver == null) {
            dataReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context ctx, final Intent intent) {
                    final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
                    final int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, WidgetSteps.class));
                    for (final int id : ids) {
                        mgr.updateAppWidget(id, buildViews(ctx, id));
                    }
                }
            };
            final IntentFilter f = new IntentFilter();
            f.addAction(GBApplication.ACTION_NEW_DATA);
            f.addAction(GBDevice.ACTION_DEVICE_CHANGED);
            LocalBroadcastManager.getInstance(context).registerReceiver(dataReceiver, f);
        }
    }

    @Override
    public void onDisabled(final Context context) {
        if (dataReceiver != null) {
            nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils
                    .safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(context), dataReceiver);
            dataReceiver = null;
        }
    }

    public static RemoteViews buildViews(final Context context, final int id) {
        final boolean dark = PulseWidgetStyle.isDark(context, PulseWidgetStyle.themeKey(id));
        final int accent = PulseWidgetStyle.accent(PulseWidgetStyle.accentKey(id), dark);
        final int track = PulseWidgetStyle.track(dark);

        final RemoteViews views = new RemoteViews(context.getPackageName(),
                dark ? R.layout.widget_steps_dark : R.layout.widget_steps);

        final PulseWidgetMetric metric = PulseWidgetMetric.fromKey(metricKey(id));
        final int tint = metric.tint(dark);
        views.setTextColor(R.id.stat_value, accent);
        views.setInt(R.id.stat_refresh, "setColorFilter", PulseWidgetStyle.textDim(dark));
        views.setTextViewText(R.id.stat_label, metric.label(context).toUpperCase());

        final int chipPx = PulseWidgetGraphics.dp(context, 34);
        views.setImageViewBitmap(R.id.stat_chip,
                PulseWidgetGraphics.chip(context, chipPx, chipPx * 0.34f, metric.iconRes, tint));

        final GBDevice device = firstDevice();
        PulseWidgetMetric.Reading r = new PulseWidgetMetric.Reading("–", 0, 0);
        if (device != null) {
            try (DBHandler db = GBApplication.acquireDbReadOnly()) {
                final DaoSession session = db.getDaoSession();
                final DailyTotals totals = DailyTotals.getDailyTotalsForDevice(device, GregorianCalendar.getInstance(), db);
                r = metric.read(context, device, totals, new ActivityUser(), session);
            } catch (final Exception ignored) {
            }
        }

        views.setTextViewText(R.id.stat_value, r.value);
        if (r.hasBar()) {
            final int barH = PulseWidgetGraphics.dp(context, 6);
            views.setViewVisibility(R.id.stat_bar, View.VISIBLE);
            views.setImageViewBitmap(R.id.stat_bar,
                    PulseWidgetGraphics.bar(360, barH, r.fraction(), track, accent));
            views.setViewVisibility(R.id.stat_goal, View.VISIBLE);
            views.setTextViewText(R.id.stat_goal, context.getString(R.string.pulse_of_goal,
                    NumberFormat.getIntegerInstance().format(r.max)));
        } else {
            views.setViewVisibility(R.id.stat_bar, View.GONE);
            views.setViewVisibility(R.id.stat_goal, View.GONE);
        }

        views.setOnClickPendingIntent(R.id.stat_root,
                PulseWidgetStyle.tapIntent(context, id, device, PulseWidgetStyle.tapKey(id)));

        final Intent refresh = new Intent(context, WidgetSteps.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{id});
        views.setOnClickPendingIntent(R.id.stat_refresh, android.app.PendingIntent.getBroadcast(
                context, id, refresh,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE));

        return views;
    }

    private static String metricKey(final int id) {
        final String csv = GBApplication.getPrefs()
                .getString(PulseWidgetStyle.PREF_METRICS + id, DEFAULT_METRIC);
        final String first = csv.split(",")[0].trim();
        return PulseWidgetMetric.isKnown(first) ? first : DEFAULT_METRIC;
    }

    private static GBDevice firstDevice() {
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (final GBDevice d : devices) {
            if (d.isInitialized()) {
                return d;
            }
        }
        return devices.isEmpty() ? null : devices.get(0);
    }
}
