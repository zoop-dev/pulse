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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.RemoteViews;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

/** Pulse: the "Rings" home-screen widget — one goal as an arc, with up to three sub-stats. */
public class WidgetRings extends AppWidgetProvider {

    private static final Logger LOG = LoggerFactory.getLogger(WidgetRings.class);
    private static final String DEFAULT_METRICS = "steps,distance,calories";

    private static final int[] SUB = {R.id.rings_sub0, R.id.rings_sub1, R.id.rings_sub2};
    private static final int[] SUB_VALUE = {R.id.rings_sub0_value, R.id.rings_sub1_value, R.id.rings_sub2_value};
    private static final int[] SUB_LABEL = {R.id.rings_sub0_label, R.id.rings_sub1_label, R.id.rings_sub2_label};
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
                    for (final int id : mgr.getAppWidgetIds(new ComponentName(ctx, WidgetRings.class))) {
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

        final RemoteViews views = new RemoteViews(context.getPackageName(),
                dark ? R.layout.widget_rings_dark : R.layout.widget_rings);

        final GBDevice device = new WidgetPreferenceStorage().getDeviceForWidget(id);
        if (device == null) {
            return views;
        }

        views.setTextColor(R.id.rings_wordmark, accent);
        views.setTextColor(R.id.rings_value, PulseWidgetStyle.text(dark));
        views.setInt(R.id.rings_refresh, "setColorFilter", PulseWidgetStyle.textDim(dark));

        final String[] metrics = metrics(id);
        final PulseWidgetMetric ring = PulseWidgetMetric.fromKey(metrics.length > 0 ? metrics[0] : "steps");

        try (DBHandler db = GBApplication.acquireDbReadOnly()) {
            final DaoSession session = db.getDaoSession();
            final DailyTotals totals = DailyTotals.getDailyTotalsForDevice(device, new GregorianCalendar(), db);
            final ActivityUser user = new ActivityUser();

            final PulseWidgetMetric.Reading r = ring.read(context, device, totals, user, session);
            final float pct = r.hasBar() ? r.fraction() : 0f;

            views.setTextViewText(R.id.rings_value, r.value);
            views.setTextViewText(R.id.rings_sub, ring.label(context).toUpperCase()
                    + (r.hasBar() ? " · " + Math.round(pct * 100) + "%" : ""));

            final int size = PulseWidgetGraphics.dp(context, 132);
            views.setImageViewBitmap(R.id.rings_arc, PulseWidgetGraphics.ring(size,
                    PulseWidgetGraphics.dp(context, 13), pct, PulseWidgetStyle.track(dark), accent));

            for (int i = 0; i < SUB.length; i++) {
                final int metricIdx = i + 1;
                if (metricIdx >= metrics.length) {
                    views.setViewVisibility(SUB[i], View.GONE);
                    continue;
                }
                final PulseWidgetMetric sm = PulseWidgetMetric.fromKey(metrics[metricIdx]);
                final PulseWidgetMetric.Reading sr = sm.read(context, device, totals, user, session);
                views.setViewVisibility(SUB[i], View.VISIBLE);
                views.setTextColor(SUB_VALUE[i], PulseWidgetStyle.text(dark));
                views.setTextViewText(SUB_VALUE[i], sr.value);
                views.setTextViewText(SUB_LABEL[i], sm.label(context).toUpperCase());
            }
        } catch (final Exception e) {
            LOG.warn("WidgetRings: failed to build", e);
        }

        views.setOnClickPendingIntent(R.id.rings_root,
                PulseWidgetStyle.tapIntent(context, id, device, PulseWidgetStyle.tapKey(id)));

        final Intent refresh = new Intent(context, WidgetRings.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{id});
        views.setOnClickPendingIntent(R.id.rings_refresh, PendingIntent.getBroadcast(context, id, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        return views;
    }

    private static String[] metrics(final int id) {
        final String csv = GBApplication.getPrefs()
                .getString(PulseWidgetStyle.PREF_METRICS + id, DEFAULT_METRICS);
        final List<String> out = new ArrayList<>();
        for (final String raw : csv.split(",")) {
            final String k = raw.trim();
            if (PulseWidgetMetric.isKnown(k) && !out.contains(k)) {
                out.add(k);
            }
        }
        if (out.isEmpty()) {
            for (final String k : DEFAULT_METRICS.split(",")) {
                out.add(k);
            }
        }
        while (out.size() > 4) {
            out.remove(out.size() - 1);
        }
        return out.toArray(new String[0]);
    }
}
