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
import java.util.Calendar;
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

/** Pulse: the "Vitals" widget — a headline metric with chart, plus a slim trend list. */
public class WidgetVitals extends AppWidgetProvider {

    private static final Logger LOG = LoggerFactory.getLogger(WidgetVitals.class);
    private static final String DEFAULT_METRICS = "bodybattery,heartrate,spo2,respiration";

    private static final int[] ROW = {R.id.vitals_r0, R.id.vitals_r1, R.id.vitals_r2};
    private static final int[] ROW_CHIP = {R.id.vitals_r0_chip, R.id.vitals_r1_chip, R.id.vitals_r2_chip};
    private static final int[] ROW_NAME = {R.id.vitals_r0_name, R.id.vitals_r1_name, R.id.vitals_r2_name};
    private static final int[] ROW_VAL = {R.id.vitals_r0_value, R.id.vitals_r1_value, R.id.vitals_r2_value};
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
                    for (final int id : mgr.getAppWidgetIds(new ComponentName(ctx, WidgetVitals.class))) {
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
        final int textColor = PulseWidgetStyle.text(dark);

        final RemoteViews views = new RemoteViews(context.getPackageName(),
                dark ? R.layout.widget_vitals_dark : R.layout.widget_vitals);

        final GBDevice device = new WidgetPreferenceStorage().getDeviceForWidget(id);
        if (device == null) {
            return views;
        }

        views.setInt(R.id.vitals_refresh, "setColorFilter", PulseWidgetStyle.textDim(dark));

        final String[] tokens = metrics(id);
        final PulseWidgetMetric head = PulseWidgetMetric.fromKey(tokens.length > 0 ? tokens[0] : "bodybattery");

        try (DBHandler db = GBApplication.acquireDbReadOnly()) {
            final DaoSession session = db.getDaoSession();
            final ActivityUser user = new ActivityUser();
            final DailyTotals totals = DailyTotals.getDailyTotalsForDevice(device, new GregorianCalendar(), db);

            final PulseWidgetMetric.Reading hr = head.read(context, device, totals, user, session);
            final int cp = PulseWidgetGraphics.dp(context, 32);
            views.setImageViewBitmap(R.id.vitals_head_chip,
                    PulseWidgetGraphics.chip(context, cp, cp * 0.32f, head.iconRes, accent));
            views.setTextViewText(R.id.vitals_head_name, head.label(context));
            views.setTextViewText(R.id.vitals_head_value, hr.value);
            views.setTextViewText(R.id.vitals_head_unit, "");

            if (isTotals(head)) {
                final int[] week = weekSeries(device, db, head);
                views.setViewVisibility(R.id.vitals_head_spark, View.VISIBLE);
                views.setImageViewBitmap(R.id.vitals_head_spark, PulseWidgetGraphics.spark(
                        PulseWidgetGraphics.dp(context, 300), PulseWidgetGraphics.dp(context, 40),
                        week, accent, true, PulseWidgetGraphics.dp(context, 2)));

                long sum = 0;
                int n = 0;
                for (int i = 0; i < 6; i++) {
                    if (week[i] > 0) {
                        sum += week[i];
                        n++;
                    }
                }
                if (n > 0 && week[6] > 0) {
                    final float avg = sum / (float) n;
                    final float diff = week[6] - avg;
                    final int pctDiff = avg > 0 ? Math.abs(Math.round(diff / avg * 100f)) : 0;
                    final String text;
                    final int color;
                    if (pctDiff < 5) {
                        text = "Steady vs 7-day avg";
                        color = PulseWidgetStyle.textDim(dark);
                    } else if (diff > 0) {
                        text = "▲ " + pctDiff + "% vs 7-day avg";
                        color = dark ? 0xFF4AD6A0 : 0xFF1FA877;
                    } else {
                        text = "▼ " + pctDiff + "% vs 7-day avg";
                        color = dark ? 0xFFFF6B6B : 0xFFE5484D;
                    }
                    views.setViewVisibility(R.id.vitals_head_delta, View.VISIBLE);
                    views.setTextViewText(R.id.vitals_head_delta, text);
                    views.setTextColor(R.id.vitals_head_delta, color);
                } else {
                    views.setViewVisibility(R.id.vitals_head_delta, View.GONE);
                }
            } else {
                views.setViewVisibility(R.id.vitals_head_spark, View.GONE);
                views.setViewVisibility(R.id.vitals_head_delta, View.GONE);
            }

            for (int i = 0; i < 3; i++) {
                final int tokenIndex = i + 1;
                if (tokenIndex >= tokens.length) {
                    views.setViewVisibility(ROW[i], View.GONE);
                    continue;
                }
                final PulseWidgetMetric rm = PulseWidgetMetric.fromKey(tokens[tokenIndex]);
                final PulseWidgetMetric.Reading rr = rm.read(context, device, totals, user, session);
                final int rcp = PulseWidgetGraphics.dp(context, 24);
                views.setViewVisibility(ROW[i], View.VISIBLE);
                views.setImageViewBitmap(ROW_CHIP[i],
                        PulseWidgetGraphics.chip(context, rcp, rcp * 0.32f, rm.iconRes, rm.tint(dark)));
                views.setTextViewText(ROW_NAME[i], rm.label(context));
                views.setTextViewText(ROW_VAL[i], rr.value);
                views.setTextColor(ROW_NAME[i], textColor);
                views.setTextColor(ROW_VAL[i], textColor);
            }
        } catch (final Exception e) {
            LOG.warn("WidgetVitals: failed to build views", e);
        }

        views.setOnClickPendingIntent(R.id.vitals_root,
                PulseWidgetStyle.tapIntent(context, id, device, PulseWidgetStyle.tapKey(id)));

        final Intent refresh = new Intent(context, WidgetVitals.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{id});
        views.setOnClickPendingIntent(R.id.vitals_refresh, PendingIntent.getBroadcast(
                context, id, refresh,
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
        return out.toArray(new String[0]);
    }

    private static boolean isTotals(final PulseWidgetMetric m) {
        return m == PulseWidgetMetric.STEPS || m == PulseWidgetMetric.DISTANCE
                || m == PulseWidgetMetric.CALORIES || m == PulseWidgetMetric.SLEEP;
    }

    private static int totalsField(final PulseWidgetMetric m, final DailyTotals t) {
        switch (m) {
            case DISTANCE: return (int) t.getDistance();
            case CALORIES: return (int) (t.getActiveCalories() / 1000);
            case SLEEP:    return (int) t.getSleep();
            default:       return (int) t.getSteps();
        }
    }

    private static int[] weekSeries(final GBDevice device, final DBHandler db, final PulseWidgetMetric m) {
        final int[] week = new int[7];
        final Calendar anchor = new GregorianCalendar();
        for (int i = 0; i < 7; i++) {
            final Calendar day = (Calendar) anchor.clone();
            day.add(Calendar.DAY_OF_MONTH, -(6 - i));
            try {
                week[i] = totalsField(m, DailyTotals.getDailyTotalsForDevice(device, day, db));
            } catch (final Exception ignored) {
                week[i] = 0;
            }
        }
        return week;
    }
}
