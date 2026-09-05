/*  Copyright (C) 2019-2024 Andreas Shimokawa, Carsten Pfeiffer, Ganblejs,
    José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWidgetGraphics;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWidgetMetric;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWidgetStyle;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

/** Pulse: the "Stat strip" home-screen widget — 2–4 metric-tinted slots with progress bars. */
public class Widget extends AppWidgetProvider {
    public static final String WIDGET_CLICK = "nodomain.freeyourgadget.gadgetbridge.WidgetClick";
    public static final String APPWIDGET_DELETED = "android.appwidget.action.APPWIDGET_DELETED";

    private static final Logger LOG = LoggerFactory.getLogger(Widget.class);
    private static final String DEFAULT_METRICS = "steps,calories,sleep";
    static BroadcastReceiver broadcastReceiver = null;

    private static final int[] SLOT = {R.id.strip_s0, R.id.strip_s1, R.id.strip_s2, R.id.strip_s3};
    private static final int[] CHIP = {R.id.strip_s0_chip, R.id.strip_s1_chip, R.id.strip_s2_chip, R.id.strip_s3_chip};
    private static final int[] VAL = {R.id.strip_s0_value, R.id.strip_s1_value, R.id.strip_s2_value, R.id.strip_s3_value};
    private static final int[] LBL = {R.id.strip_s0_label, R.id.strip_s1_label, R.id.strip_s2_label, R.id.strip_s3_label};
    private static final int[] BAR = {R.id.strip_s0_bar, R.id.strip_s1_bar, R.id.strip_s2_bar, R.id.strip_s3_bar};

    public static String[] getWidgetMetrics(final int appWidgetId) {
        final String csv = GBApplication.getPrefs()
                .getString(PulseWidgetStyle.PREF_METRICS + appWidgetId, DEFAULT_METRICS);
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

    private void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                 final int appWidgetId) {
        final RemoteViews views = buildViews(context, appWidgetId);
        if (views != null) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static RemoteViews buildViews(final Context context, final int appWidgetId) {
        final GBDevice device = new WidgetPreferenceStorage().getDeviceForWidget(appWidgetId);
        if (device == null) {
            LOG.debug("Widget: no device, bailing out");
            return null;
        }

        final String accentKey = PulseWidgetStyle.accentKey(appWidgetId);
        final boolean dark = PulseWidgetStyle.isDark(context, PulseWidgetStyle.themeKey(appWidgetId));
        final int accent = PulseWidgetStyle.accent(accentKey, dark);
        final int track = PulseWidgetStyle.track(dark);
        final int textDim = PulseWidgetStyle.textDim(dark);

        final RemoteViews views = new RemoteViews(context.getPackageName(),
                dark ? R.layout.widget_dark : R.layout.widget);

        views.setTextColor(R.id.strip_wordmark, accent);
        views.setInt(R.id.strip_refresh, "setColorFilter", textDim);
        views.setTextViewText(R.id.strip_device, device.getAlias() != null ? device.getAlias() : device.getName());

        final String[] metrics = getWidgetMetrics(appWidgetId);
        final ActivityUser user = new ActivityUser();
        final int chipPx = PulseWidgetGraphics.dp(context, 28);
        final int barH = PulseWidgetGraphics.dp(context, 5);

        try (DBHandler db = GBApplication.acquireDbReadOnly()) {
            final DaoSession session = db.getDaoSession();
            final DailyTotals totals = DailyTotals.getDailyTotalsForDevice(device, GregorianCalendar.getInstance(), db);

            final long lastSync = lastSyncMillis(device, session);
            if (lastSync > 0) {
                views.setViewVisibility(R.id.strip_sync_dot, View.VISIBLE);
                views.setTextViewText(R.id.strip_status, DateUtils.getRelativeTimeSpanString(
                        lastSync, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE));
            } else {
                views.setViewVisibility(R.id.strip_sync_dot, View.GONE);
                views.setTextViewText(R.id.strip_status, device.getStateString(context));
            }

            for (int i = 0; i < 4; i++) {
                if (i >= metrics.length) {
                    views.setViewVisibility(SLOT[i], View.GONE);
                    continue;
                }
                views.setViewVisibility(SLOT[i], View.VISIBLE);
                final PulseWidgetMetric metric = PulseWidgetMetric.fromKey(metrics[i]);
                final int tint = metric.tint(dark);
                final PulseWidgetMetric.Reading r = metric.read(context, device, totals, user, session);

                views.setImageViewBitmap(CHIP[i],
                        PulseWidgetGraphics.chip(context, chipPx, chipPx * 0.34f, metric.iconRes, tint));
                views.setTextViewText(VAL[i], r.value);
                views.setTextViewText(LBL[i], metric.label(context).toUpperCase());
                if (r.hasBar()) {
                    views.setViewVisibility(BAR[i], View.VISIBLE);
                    views.setImageViewBitmap(BAR[i],
                            PulseWidgetGraphics.bar(360, barH, r.fraction(), track, tint));
                } else {
                    views.setViewVisibility(BAR[i], View.INVISIBLE);
                }
            }
        } catch (final Exception e) {
            LOG.warn("Widget: failed to build strip", e);
        }

        final PendingIntent tap = PulseWidgetStyle.tapIntent(context, appWidgetId, device,
                PulseWidgetStyle.tapKey(appWidgetId));
        views.setOnClickPendingIntent(R.id.strip_slots, tap);
        views.setOnClickPendingIntent(R.id.strip_wordmark, tap);

        final Intent refreshIntent = new Intent(context, Widget.class);
        refreshIntent.setPackage(BuildConfig.APPLICATION_ID);
        refreshIntent.setAction(WIDGET_CLICK);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        final PendingIntent refreshPi = PendingIntent.getBroadcast(context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.strip_refresh, refreshPi);
        views.setOnClickPendingIntent(R.id.strip_device, refreshPi);

        return views;
    }

    private static long lastSyncMillis(final GBDevice device, final DaoSession session) {
        try {
            final SampleProvider<? extends ActivitySample> p =
                    device.getDeviceCoordinator().getSampleProvider(device, session);
            final ActivitySample s = p.getLatestActivitySample();
            return s != null ? s.getTimestamp() * 1000L : 0L;
        } catch (final Exception e) {
            return 0L;
        }
    }

    public void refreshData(final int appWidgetId) {
        final Context context = GBApplication.getContext();
        final GBDevice device = new WidgetPreferenceStorage().getDeviceForWidget(appWidgetId);
        if (device == null || !device.isInitialized()) {
            GB.toast(context, context.getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.ERROR);
            if (device != null) {
                GBApplication.deviceService(device).connect();
                GB.toast(context, context.getString(R.string.connecting), Toast.LENGTH_SHORT, GB.INFO);
            }
            return;
        }
        GB.toast(context, context.getString(R.string.busy_task_fetch_activity_data), Toast.LENGTH_SHORT, GB.INFO);
        GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
    }

    public void updateWidget() {
        final Context context = GBApplication.getContext();
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final ComponentName thisAppWidget = new ComponentName(context.getPackageName(), Widget.class.getName());
        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public void removeWidget(final Context context, final int appWidgetId) {
        new WidgetPreferenceStorage().removeWidgetById(context, appWidgetId);
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        for (final int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(final Context context) {
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    updateWidget();
                }
            };
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(GBApplication.ACTION_NEW_DATA);
            intentFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    @Override
    public void onDisabled(final Context context) {
        if (broadcastReceiver != null) {
            AndroidUtils.safeUnregisterBroadcastReceiver(context, broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        final Bundle extras = intent.getExtras();
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (WIDGET_CLICK.equals(intent.getAction())) {
            if (broadcastReceiver == null) {
                onEnabled(context);
            }
            refreshData(appWidgetId);
        } else if (APPWIDGET_DELETED.equals(intent.getAction())) {
            onDisabled(context);
            removeWidget(context, appWidgetId);
        }
    }
}
