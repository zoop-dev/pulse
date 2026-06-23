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
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.WidgetAlarmsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

public class Widget extends AppWidgetProvider {
    public static final String WIDGET_CLICK = "nodomain.freeyourgadget.gadgetbridge.WidgetClick";
    public static final String APPWIDGET_DELETED = "android.appwidget.action.APPWIDGET_DELETED";

    private static final Logger LOG = LoggerFactory.getLogger(Widget.class);
    static BroadcastReceiver broadcastReceiver = null;


    private DailyTotals getSteps(GBDevice gbDevice) {
        Context context = GBApplication.getContext();
        Calendar day = GregorianCalendar.getInstance();

        if (!(context instanceof GBApplication)) {
            return new DailyTotals();
        }
        return DailyTotals.getDailyTotalsForDevice(gbDevice, day);
    }

    private String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId) {

        GBDevice deviceForWidget = new WidgetPreferenceStorage().getDeviceForWidget(appWidgetId);
        if (deviceForWidget == null) {
            LOG.debug("Widget: no device, bailing out");
            return;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        //onclick refresh
        Intent intent = new Intent(context, Widget.class);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        intent.setAction(WIDGET_CLICK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent refreshDataIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.todaywidget_header_container, refreshDataIntent);

        //open GB main window
        Intent startMainIntent = new Intent(context, ControlCenterv2.class);
        startMainIntent.setPackage(BuildConfig.APPLICATION_ID);
        PendingIntent startMainPIntent = PendingIntent.getActivity(
                context,
                0,
                startMainIntent,
                PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.todaywidget_header_icon, startMainPIntent);

        //charts (tap the widget body to open charts)
        Intent startChartsIntent = new Intent(context, ActivityChartsActivity.class);
        startChartsIntent.setPackage(BuildConfig.APPLICATION_ID);
        startChartsIntent.putExtra(GBDevice.EXTRA_DEVICE, deviceForWidget);
        PendingIntent startChartsPIntent = PendingIntent.getActivity(
                context, appWidgetId, startChartsIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.todaywidget_bottom_layout, startChartsPIntent);

        // Header: device name + battery
        views.setViewVisibility(R.id.todaywidget_battery_icon, View.GONE);
        String status = deviceForWidget.getStateString(context);
        if (deviceForWidget.isConnected() && deviceForWidget.getBatteryLevel(0) > 1) {
            views.setViewVisibility(R.id.todaywidget_battery_icon, View.VISIBLE);
            status = deviceForWidget.getBatteryLevel(0) + "%";
        }
        views.setTextViewText(R.id.todaywidget_device_status, status);
        views.setTextViewText(R.id.todaywidget_device_name,
                deviceForWidget.getAlias() != null ? deviceForWidget.getAlias() : deviceForWidget.getName());

        // Configurable stat slots
        final DailyTotals dailyTotals = getSteps(deviceForWidget);
        final ActivityUser activityUser = new ActivityUser();
        final String[] metrics = getWidgetMetrics(appWidgetId);
        final int[] slotIds = {R.id.pulse_w0, R.id.pulse_w1, R.id.pulse_w2};
        final int[] iconIds = {R.id.pulse_w0_icon, R.id.pulse_w1_icon, R.id.pulse_w2_icon};
        final int[] valueIds = {R.id.pulse_w0_value, R.id.pulse_w1_value, R.id.pulse_w2_value};
        final int[] labelIds = {R.id.pulse_w0_label, R.id.pulse_w1_label, R.id.pulse_w2_label};
        final int[] progIds = {R.id.pulse_w0_prog, R.id.pulse_w1_prog, R.id.pulse_w2_prog};
        for (int i = 0; i < 3; i++) {
            if (i < metrics.length && !metrics[i].trim().isEmpty()) {
                fillSlot(context, views, iconIds[i], valueIds[i], labelIds[i], progIds[i],
                        metrics[i].trim(), deviceForWidget, dailyTotals, activityUser);
                views.setViewVisibility(slotIds[i], View.VISIBLE);
            } else {
                views.setViewVisibility(slotIds[i], View.GONE);
            }
        }

        // Pulse: top-right refresh icon re-reads + redraws this widget.
        final Intent refreshIntent = new Intent(context, Widget.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        final android.app.PendingIntent refreshPi = android.app.PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.todaywidget_refresh, refreshPi);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static String[] getWidgetMetrics(final int appWidgetId) {
        return GBApplication.getPrefs()
                .getString("pulse_widget_metrics_" + appWidgetId, "steps,distance,sleep")
                .split(",");
    }

    private void fillSlot(final Context context, final RemoteViews views,
                          final int iconId, final int valueId, final int labelId, final int progId,
                          final String metric, final GBDevice device,
                          final DailyTotals totals, final ActivityUser user) {
        final int iconRes;
        final String label;
        final String value;
        int max = 0;
        int progress = 0;
        switch (metric) {
            case "distance": {
                // getDistance() is in cm; fall back to steps * stride when no GPS distance
                long cm = totals.getDistance();
                if (cm <= 0 && totals.getSteps() > 0) {
                    cm = totals.getSteps() * user.getStepLengthCm();
                }
                final double m = cm * 0.01;
                iconRes = R.drawable.ic_map;
                label = context.getString(R.string.distance);
                value = FormatUtils.getFormattedDistanceLabel(m);
                max = user.getDistanceGoalMeters();
                progress = (int) m;
                break;
            }
            case "calories": {
                // DailyTotals stores raw calories; dashboard shows kcal (÷1000)
                final int cal = (int) (totals.getActiveCalories() / 1000);
                iconRes = R.drawable.ic_calories;
                label = context.getString(R.string.calories);
                value = String.valueOf(cal);
                max = user.getCaloriesBurntGoal();
                progress = cal;
                break;
            }
            case "sleep": {
                final int sl = (int) totals.getSleep();
                iconRes = R.drawable.ic_nights_stay;
                label = context.getString(R.string.menuitem_sleep);
                value = sl > 0 ? getHM(sl) : context.getString(R.string.pulse_no_sleep);
                max = user.getSleepDurationGoal();
                progress = sl;
                break;
            }
            case "heartrate": {
                final int hr = latestHeartRate(device);
                iconRes = R.drawable.ic_heartrate;
                label = context.getString(R.string.menuitem_hr);
                value = hr > 0 ? String.valueOf(hr) : "-";
                break;
            }
            case "bodybattery": {
                final int be = latestBodyEnergy(device);
                iconRes = R.drawable.ic_battery_full;
                label = context.getString(R.string.body_energy);
                value = be >= 0 ? String.valueOf(be) : "-";
                max = 100;
                progress = Math.max(be, 0);
                break;
            }
            default: { // steps
                final int steps = (int) totals.getSteps();
                iconRes = R.drawable.ic_steps;
                label = context.getString(R.string.steps);
                value = String.valueOf(steps);
                max = user.getStepsGoal();
                progress = steps;
                break;
            }
        }
        views.setInt(iconId, "setBackgroundResource", iconRes);
        views.setTextViewText(valueId, value);
        views.setTextViewText(labelId, label);
        if (max > 0) {
            views.setViewVisibility(progId, View.VISIBLE);
            views.setProgressBar(progId, max, progress, false);
        } else {
            views.setViewVisibility(progId, View.INVISIBLE);
        }
    }

    private int latestHeartRate(final GBDevice device) {
        try (nodomain.freeyourgadget.gadgetbridge.database.DBHandler db = GBApplication.acquireDbReadOnly()) {
            final nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample s =
                    device.getDeviceCoordinator().getHeartRateRestingSampleProvider(device, db.getDaoSession()).getLatestSample();
            return s != null ? s.getHeartRate() : -1;
        } catch (final Exception e) {
            return -1;
        }
    }

    private int latestBodyEnergy(final GBDevice device) {
        try (nodomain.freeyourgadget.gadgetbridge.database.DBHandler db = GBApplication.acquireDbReadOnly()) {
            final nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample s =
                    device.getDeviceCoordinator().getBodyEnergySampleProvider(device, db.getDaoSession()).getLatestSample();
            return s != null ? s.getEnergy() : -1;
        } catch (final Exception e) {
            return -1;
        }
    }

    public void refreshData(int appWidgetId) {
        Context context = GBApplication.getContext();
        GBDevice deviceForWidget = new WidgetPreferenceStorage().getDeviceForWidget(appWidgetId);

        if (deviceForWidget == null || !deviceForWidget.isInitialized()) {
            GB.toast(context,
                    context.getString(R.string.device_not_connected),
                    Toast.LENGTH_SHORT, GB.ERROR);
            GBApplication.deviceService(deviceForWidget).connect();
            GB.toast(context,
                    context.getString(R.string.connecting),
                    Toast.LENGTH_SHORT, GB.INFO);

            return;
        }
        GB.toast(context,
                context.getString(R.string.busy_task_fetch_activity_data),
                Toast.LENGTH_SHORT, GB.INFO);

        GBApplication.deviceService(deviceForWidget).onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
    }

    public void updateWidget() {
        Context context = GBApplication.getContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), Widget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public void removeWidget(Context context, int appWidgetId) {
        WidgetPreferenceStorage widgetPreferenceStorage = new WidgetPreferenceStorage();
        widgetPreferenceStorage.removeWidgetById(context, appWidgetId);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        if (broadcastReceiver == null) {
            LOG.debug("gbwidget BROADCAST receiver initialized.");
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LOG.debug("gbwidget BROADCAST, action" + intent.getAction());
                    updateWidget();
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(GBApplication.ACTION_NEW_DATA);
            intentFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    @Override
    public void onDisabled(Context context) {
        if (broadcastReceiver != null) {
            AndroidUtils.safeUnregisterBroadcastReceiver(context, broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        LOG.debug("gbwidget LOCAL onReceive, action: " + intent.getAction() + intent);
        Bundle extras = intent.getExtras();
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        //this handles widget re-connection after apk updates
        if (WIDGET_CLICK.equals(intent.getAction())) {
            if (broadcastReceiver == null) {
                onEnabled(context);
            }
                refreshData(appWidgetId);
            //updateWidget();
        } else if (APPWIDGET_DELETED.equals(intent.getAction())) {
            onDisabled(context);
            removeWidget(context, appWidgetId);
        }
    }

}

