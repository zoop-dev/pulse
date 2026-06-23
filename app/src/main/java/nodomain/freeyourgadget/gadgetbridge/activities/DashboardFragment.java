/*  Copyright (C) 2023-2024 Arjan Schrijver, José Rebelo

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.card.MaterialCardView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.views.PulseRingView;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.AbstractDashboardWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardCaloriesActiveGoalWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardActiveTimeWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardBodyEnergyWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardCalendarActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardCaloriesTotalSegmentedWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardDistanceWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardGoalsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardHrvWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardBloodPressureWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardPaiWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSleepScoreWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSleepWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStepsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressBreakdownWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressSegmentedWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressSimpleWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardTodayWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxCyclingWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxAnyWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxRunningWidget;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.CachedValue;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class DashboardFragment extends Fragment implements MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardFragment.class);

    private final Calendar day = GregorianCalendar.getInstance();
    private TextView textViewDate;
    private TextView detailsTitle;
    private TextView arrowRight;
    private GridLayout gridLayout;
    // Pulse hero
    private static final String ARG_SECTION = "section";
    private String section = "today";
    private PulseRingView pulseRing;
    private TextView ringValue;
    private TextView ringGoal;
    private LinearLayout statColumn;
    private TextView deviceName;
    private TextView deviceBattery;
    private View deviceDot;
    private TextView ringLabel;
    private TextView streakCount;
    private View loadingOverlay;
    private boolean hasLoadedOnce = false;
    private final Map<String, Integer> extraMetrics = new ConcurrentHashMap<>();
    private final Map<String, int[]> healthHistory = new ConcurrentHashMap<>(); // metric → 7 daily values (oldest..today)
    private static final String DEFAULT_HEALTH_METRICS = "heartrate,bodybattery,stress,spo2,hrv,respiration";

    // Pulse: inline Sleep-tab detail
    private LinearLayout sleepDetailContainer;
    private volatile int sleepScore;
    private volatile boolean sleepHasNight;
    private volatile long sleepDeep, sleepLight, sleepRem, sleepAwake, sleepGoalMin;
    private volatile java.util.Date sleepBed, sleepWake;
    private final int[] sleepWeek = new int[7];

    public static DashboardFragment newInstance(final String section) {
        final DashboardFragment f = new DashboardFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_SECTION, section);
        f.setArguments(args);
        return f;
    }

    // Widgets shown per tab (Garmin Vívosmart 5 graceful "no data" for unsupported)
    // All Pulse-native metrics available to surface as cards.
    static final String[] ALL_METRICS = {
            "steps", "distance", "activetime", "calories", "sleep",
            "heartrate", "bodybattery", "stress", "spo2", "hrv", "respiration"
    };
    private static final String DEFAULT_TODAY_METRICS = String.join(",", ALL_METRICS);

    /** Which metric cards the grid shows. Today is user-customizable; other tabs are curated. */
    private String[] pulseMetrics() {
        switch (section) {
            case "fitness":
                return new String[]{"steps", "distance", "activetime", "calories", "heartrate"};
            case "sleep":
                return new String[]{"respiration", "hrv", "bodybattery"};
            case "health":
                return GBApplication.getPrefs().getString("pulse_health_metrics", DEFAULT_HEALTH_METRICS).split(",");
            default: // today
                return GBApplication.getPrefs().getString("pulse_today_metrics", DEFAULT_TODAY_METRICS).split(",");
        }
    }

    private String sectionTitle() {
        switch (section) {
            case "fitness": return getString(R.string.pulse_tab_fitness);
            case "sleep":   return getString(R.string.pulse_tab_sleep);
            case "health":  return getString(R.string.pulse_tab_health);
            default:        return getString(R.string.activity_summary_today);
        }
    }
    private final Map<String, AbstractDashboardWidget> widgetMap = new HashMap<>();
    private DashboardData dashboardData = new DashboardData();
    private boolean isConfigChanged = false;
    // While true (the default), the dashboard snaps to the real "today" — so it rolls over at midnight.
    private boolean followingToday = true;

    private ActivityResultLauncher<Intent> calendarLauncher;
    private final ActivityResultCallback<ActivityResult> calendarCallback = result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            long timeMillis = result.getData().getLongExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, 0);
            if (timeMillis != 0) {
                day.setTimeInMillis(timeMillis);
                followingToday = DateTimeUtils.isSameDay(GregorianCalendar.getInstance(), day);
                fullRefresh();
            }
        }
    };

    public static final String ACTION_CONFIG_CHANGE = "nodomain.freeyourgadget.gadgetbridge.activities.dashboardfragment.action.config_change";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case GBApplication.ACTION_NEW_DATA:
                    final GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null) {
                        if (dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) {
                            refresh();
                        }
                    }
                    break;
                case ACTION_CONFIG_CHANGE:
                    isConfigChanged = true;
                    break;
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (getArguments() != null) {
            section = getArguments().getString(ARG_SECTION, "today");
        }
        View dashboardView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        textViewDate = dashboardView.findViewById(R.id.dashboard_date);
        detailsTitle = dashboardView.findViewById(R.id.pulse_details_title);
        gridLayout = dashboardView.findViewById(R.id.dashboard_gridlayout);
        sleepDetailContainer = dashboardView.findViewById(R.id.pulse_sleep_detail);

        // Pulse hero
        pulseRing = dashboardView.findViewById(R.id.pulse_ring);
        ringValue = dashboardView.findViewById(R.id.pulse_ring_value);
        ringGoal = dashboardView.findViewById(R.id.pulse_ring_goal);
        statColumn = dashboardView.findViewById(R.id.pulse_stat_column);
        deviceName = dashboardView.findViewById(R.id.pulse_device_name);
        deviceBattery = dashboardView.findViewById(R.id.pulse_device_battery);
        deviceDot = dashboardView.findViewById(R.id.pulse_device_dot);
        ringLabel = dashboardView.findViewById(R.id.pulse_ring_label);
        streakCount = dashboardView.findViewById(R.id.pulse_streak_count);
        loadingOverlay = dashboardView.findViewById(R.id.pulse_loading);

        final boolean isToday = "today".equals(section);
        // Today, Sleep and Fitness get a hero ring; Health is just a metrics grid.
        final boolean hasHero = isToday || "sleep".equals(section) || "fitness".equals(section);

        final LinearLayout heroRow = dashboardView.findViewById(R.id.pulse_hero_row);
        final View pills = dashboardView.findViewById(R.id.pulse_pills);
        final View deviceChip = dashboardView.findViewById(R.id.pulse_device_chip);
        // Pulse: device + battery now live in the toolbar, so hide the in-content chip
        deviceChip.setVisibility(View.GONE);
        final View greeting = dashboardView.findViewById(R.id.dashboard_greeting);

        if (!hasHero) {
            heroRow.setVisibility(View.GONE);
        } else if (!isToday) {
            // Sleep/Fitness: keep the ring, drop the Today-only stat cards, center the ring
            statColumn.setVisibility(View.GONE);
            heroRow.setGravity(android.view.Gravity.CENTER);
        }
        if (!isToday) {
            pills.setVisibility(View.GONE);
            deviceChip.setVisibility(View.GONE);
            greeting.setVisibility(View.GONE);
            textViewDate.setText(sectionTitle());
        } else if (greeting instanceof TextView) {
            ((TextView) greeting).setText(buildGreeting());
        }

        deviceChip.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), DevicesActivity.class)));

        // Tap the goal under the steps ring to set a custom daily step goal
        if (isToday) {
            ringGoal.setOnClickListener(v -> showStepGoalDialog());
            pulseRing.setOnClickListener(v -> openChart("stepsweek", R.string.steps, ""));
            pulseRing.setOnLongClickListener(v -> {
                showRingMetricDialog();
                return true;
            });
        } else if ("sleep".equals(section)) {
            // Tap the sleep ring → Pulse sleep insights.
            pulseRing.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), PulseSleepActivity.class)));
        }

        dashboardView.findViewById(R.id.pulse_btn_log).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PulseStreakActivity.class)));
        dashboardView.findViewById(R.id.pulse_btn_start).setOnClickListener(v -> {
            final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            final boolean anyConnected = devices.stream().anyMatch(GBDevice::isInitialized);
            if (anyConnected) {
                GBApplication.deviceService().onFetchRecordedData(
                        nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes.TYPE_SYNC);
                nodomain.freeyourgadget.gadgetbridge.util.GB.toast(requireContext(),
                        getString(R.string.busy_task_fetch_activity_data),
                        android.widget.Toast.LENGTH_SHORT,
                        nodomain.freeyourgadget.gadgetbridge.util.GB.INFO);
            } else {
                nodomain.freeyourgadget.gadgetbridge.util.GB.toast(requireContext(),
                        getString(R.string.info_no_devices_connected),
                        android.widget.Toast.LENGTH_SHORT,
                        nodomain.freeyourgadget.gadgetbridge.util.GB.WARN);
            }
        });
        // Edit = drag-and-drop customize which metric cards the Today tab shows
        dashboardView.findViewById(R.id.pulse_btn_edit).setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), PulseDashboardEditActivity.class)));

        calendarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                calendarCallback
        );

        // Increase column count on landscape, tablets and open foldables
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (displayMetrics.widthPixels / displayMetrics.density >= 600) {
            gridLayout.setColumnCount(4);
        }

        final TextView arrowLeft = dashboardView.findViewById(R.id.arrow_left);
        arrowLeft.setOnClickListener(v -> {
            day.add(Calendar.DAY_OF_MONTH, -1);
            followingToday = false;
            refresh();
        });
        arrowRight = dashboardView.findViewById(R.id.arrow_right);
        arrowRight.setOnClickListener(v -> {
            Calendar today = GregorianCalendar.getInstance();
            if (!DateTimeUtils.isSameDay(today, day)) {
                day.add(Calendar.DAY_OF_MONTH, 1);
                followingToday = DateTimeUtils.isSameDay(today, day);
                refresh();
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("dashboard_data") && dashboardData.isEmpty()) {
            dashboardData = (DashboardData) savedInstanceState.getSerializable("dashboard_data");
        } else if (dashboardData.isEmpty()) {
            reloadPreferences();
        }

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(GBApplication.ACTION_NEW_DATA);
        filterLocal.addAction(ACTION_CONFIG_CHANGE);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filterLocal);

        // Pulse: warm stats as soon as the view is created — ViewPager keeps offscreen tabs in
        // STARTED (not RESUMED), so onResume never fires for them. This makes all 4 tabs preload.
        refresh();

        return dashboardView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isConfigChanged) {
            isConfigChanged = false;
            fullRefresh();
        } else if (dashboardData.isEmpty() || !widgetMap.containsKey("today")) {
            refresh();
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("dashboard_data", dashboardData);
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.dashboard_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.dashboard_show_calendar) {
            final Intent intent = new Intent(requireActivity(), DashboardCalendarActivity.class);
            intent.putExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, day.getTimeInMillis());
            calendarLauncher.launch(intent);
            return true;
        } else if (itemId == R.id.dashboard_settings) {
            final Intent intent = new Intent(requireActivity(), DashboardPreferencesActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.pulse_share_today) {
            shareToday();
            return true;
        }
        return false;
    }

    /** Render today's stats into a Pulse card image and fire the share sheet. */
    private void shareToday() {
        final Context ctx = requireContext();
        final java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
        final java.util.Locale loc = java.util.Locale.getDefault();

        final View card = getLayoutInflater().inflate(R.layout.pulse_share_card, null);

        final int steps = dashboardData.getStepsTotal();
        final int stepsGoal = new ActivityUser().getStepsGoal();
        final long activeMin = dashboardData.getActiveMinutesTotal();
        final long sleepMin = dashboardData.getSleepMinutesTotal();
        final int streak = GBApplication.getPrefs().getInt("pulse_streak_count", 0);

        ((TextView) card.findViewById(R.id.share_date)).setText(
                java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(day.getTime()));
        ((TextView) card.findViewById(R.id.share_steps)).setText(nf.format(steps));
        ((TextView) card.findViewById(R.id.share_steps_goal)).setText(
                getString(R.string.pulse_of_goal, nf.format(stepsGoal))
                        + " " + getString(R.string.steps).toLowerCase(loc));
        ((TextView) card.findViewById(R.id.share_distance)).setText(
                nodomain.freeyourgadget.gadgetbridge.util.FormatUtils.getFormattedDistanceLabel(dashboardData.getDistanceTotal()));
        ((TextView) card.findViewById(R.id.share_calories)).setText(nf.format(dashboardData.getActiveCaloriesTotal()));
        ((TextView) card.findViewById(R.id.share_active)).setText(
                activeMin >= 60 ? String.format(loc, "%dh %dm", activeMin / 60, activeMin % 60) : String.format(loc, "%dm", activeMin));
        ((TextView) card.findViewById(R.id.share_sleep)).setText(
                sleepMin > 0 ? String.format(loc, "%dh %dm", sleepMin / 60, sleepMin % 60) : getString(R.string.pulse_no_sleep));
        final TextView streakView = card.findViewById(R.id.share_streak);
        streakView.setText(streak > 0 ? getString(R.string.pulse_share_streak_fmt, streak) : "");

        // Render the card to a fixed-width bitmap.
        final int width = 1080;
        card.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        card.layout(0, 0, card.getMeasuredWidth(), card.getMeasuredHeight());
        final android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                card.getMeasuredWidth(), card.getMeasuredHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        card.draw(new android.graphics.Canvas(bmp));

        try {
            final java.io.File dir = new java.io.File(ctx.getCacheDir(), "images");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            final java.io.File out = new java.io.File(dir, "pulse_today.png");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
            }
            nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils.shareFile(ctx, out, "image/png");
        } catch (final java.io.IOException e) {
            nodomain.freeyourgadget.gadgetbridge.util.GB.toast(ctx,
                    getString(R.string.activity_error_share_failed),
                    android.widget.Toast.LENGTH_LONG,
                    nodomain.freeyourgadget.gadgetbridge.util.GB.ERROR, e);
        }
    }

    private void fullRefresh() {
        gridLayout.removeAllViews();
        widgetMap.clear();
        refresh();
    }

    private void refresh() {
        // Roll over to the real today (e.g. when the app was left open past midnight)
        if (followingToday) {
            final Calendar now = GregorianCalendar.getInstance();
            day.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        }
        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        dashboardData.clear();
        reloadPreferences();

        // Show the loading overlay on first load so nothing pops in afterwards.
        if (!hasLoadedOnce && loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }

        // Warm the (DB-heavy) stats off the UI thread, then draw on the UI thread.
        new Thread(() -> {
            try {
                warmData();
            } catch (final Exception e) {
                LOG.warn("Failed to warm dashboard data", e);
            }
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                draw();
                hasLoadedOnce = true;
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                }
            });
        }, "pulse-dashboard-warm").start();
    }

    /** Pulse: pre-compute the heavy stat queries so the UI-thread draw only hits caches. */
    private void warmData() {
        dashboardData.getStepsTotal();
        dashboardData.getStepsGoalFactor();
        dashboardData.getDistanceTotal();
        dashboardData.getDistanceGoalFactor();
        dashboardData.getActiveMinutesTotal();
        dashboardData.getActiveMinutesGoalFactor();
        dashboardData.getActiveCaloriesTotal();
        dashboardData.getActiveCaloriesGoalFactor();
        dashboardData.getSleepMinutesTotal();
        dashboardData.getSleepMinutesGoalFactor();
        warmExtraMetrics();
        if ("health".equals(section)) {
            warmHealthHistory();
        } else if ("sleep".equals(section)) {
            warmSleepDetail();
        }
    }

    /** Pulse: latest values for the richer Garmin metrics (HR, Body Battery, stress, SpO2, …). */
    private void warmExtraMetrics() {
        extraMetrics.clear();
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (devices.isEmpty()) {
            return;
        }
        final GBDevice dev = devices.get(0);
        try (nodomain.freeyourgadget.gadgetbridge.database.DBHandler db = GBApplication.acquireDbReadOnly()) {
            final nodomain.freeyourgadget.gadgetbridge.entities.DaoSession s = db.getDaoSession();
            final nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator c = dev.getDeviceCoordinator();
            try {
                final nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample x = c.getBodyEnergySampleProvider(dev, s).getLatestSample();
                if (x != null) extraMetrics.put("bodybattery", x.getEnergy());
            } catch (final Exception ignored) { }
            try {
                final nodomain.freeyourgadget.gadgetbridge.model.StressSample x = c.getStressSampleProvider(dev, s).getLatestSample();
                if (x != null) extraMetrics.put("stress", x.getStress());
            } catch (final Exception ignored) { }
            try {
                final nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample x = c.getSpo2SampleProvider(dev, s).getLatestSample();
                if (x != null) extraMetrics.put("spo2", x.getSpo2());
            } catch (final Exception ignored) { }
            try {
                final nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample x = c.getHeartRateRestingSampleProvider(dev, s).getLatestSample();
                if (x != null && x.getHeartRate() > 0) extraMetrics.put("heartrate", x.getHeartRate());
            } catch (final Exception ignored) { }
            try {
                final nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample x = c.getHrvValueSampleProvider(dev, s).getLatestSample();
                if (x != null) extraMetrics.put("hrv", x.getValue());
            } catch (final Exception ignored) { }
            try {
                final nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample x = c.getRespiratoryRateSampleProvider(dev, s).getLatestSample();
                if (x != null) extraMetrics.put("respiration", Math.round(x.getRespiratoryRate()));
            } catch (final Exception ignored) { }
        } catch (final Exception e) {
            LOG.warn("Pulse: extra metrics query failed", e);
        }
    }

    private void reloadPreferences() {
        Prefs prefs = GBApplication.getPrefs();
        // Pulse: show only the primary device (the one in the toolbar), not an aggregate of every
        // paired device — otherwise a second watch's data gets mixed into the active one.
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (!devices.isEmpty()) {
            dashboardData.showAllDevices = false;
            dashboardData.showDeviceList = java.util.Collections.singleton(devices.get(0).getAddress());
        } else {
            dashboardData.showAllDevices = true;
            dashboardData.showDeviceList = new HashSet<>();
        }
        dashboardData.hrIntervalSecs = prefs.getInt("dashboard_widget_today_hr_interval", 1) * 60;
        dashboardData.timeTo = (int) (day.getTimeInMillis() / 1000);
        dashboardData.timeFrom = DateTimeUtils.shiftDays(dashboardData.timeTo, -1);
    }

    private void draw() {
        final boolean isToday = "today".equals(section);

        // The Details header reflects the day currently being viewed (all tabs).
        final Calendar today = GregorianCalendar.getInstance();
        final boolean sameDay = DateTimeUtils.isSameDay(today, day);
        final String dayLabel = sameDay
                ? requireContext().getString(R.string.activity_summary_today)
                : DateTimeUtils.formatDate(day.getTime(), DateUtils.FORMAT_SHOW_WEEKDAY);
        detailsTitle.setText(dayLabel);
        arrowRight.setAlpha(sameDay ? 0.5f : 1f);
        if (isToday) {
            textViewDate.setText(dayLabel);
            updateDeviceChip();
            updateStreak();
        }
        if (isToday || "sleep".equals(section) || "fitness".equals(section)) {
            updateHero();
        }

        // Pulse: native stat cards (no Gadgetbridge gauge fragments)
        drawPulseCards();
    }

    /** Pulse-native stat cards built straight from DashboardData. */
    private void drawPulseCards() {
        gridLayout.removeAllViews();
        final String[] metrics = pulseMetrics();

        // Sleep tab: an inline sleep-detail block above the supporting cards.
        if (sleepDetailContainer != null) {
            sleepDetailContainer.setVisibility("sleep".equals(section) ? View.VISIBLE : View.GONE);
            if ("sleep".equals(section)) {
                renderSleepDetail();
            }
        }

        // Health tab: a customizable 2-column grid of "cube" cards with 7-day mini charts.
        if ("health".equals(section)) {
            gridLayout.setColumnCount(2);
            for (final String metric : metrics) {
                addHealthCubeCard(metric);
            }
            addHealthCustomizeCard();
            return;
        }

        // On Today the first 3 metrics are shown as hero cards, so the grid starts after them.
        final int start = "today".equals(section) ? Math.min(HERO_CARD_COUNT, metrics.length) : 0;
        for (int i = start; i < metrics.length; i++) {
            addPulseStatCard(metrics[i]);
        }
    }

    private String metricLabel(final String m) {
        switch (m) {
            case "any":         return getString(R.string.pulse_streak_any_goal);
            case "distance":    return getString(R.string.distance);
            case "activetime":  return getString(R.string.activity_list_summary_active_time);
            case "calories":    return getString(R.string.calories);
            case "sleep":       return getString(R.string.menuitem_sleep);
            case "heartrate":   return getString(R.string.menuitem_hr);
            case "bodybattery": return getString(R.string.body_energy);
            case "stress":      return getString(R.string.menuitem_stress);
            case "spo2":        return getString(R.string.pref_header_spo2);
            case "hrv":         return getString(R.string.hrv);
            case "respiration": return getString(R.string.respiratoryrate);
            default:            return getString(R.string.steps);
        }
    }

    /** Pulse: pick which metric cards the Today tab shows. */
    private void showMetricPickerDialog() {
        final java.util.List<String> enabled = new java.util.ArrayList<>(java.util.Arrays.asList(
                GBApplication.getPrefs().getString("pulse_today_metrics", DEFAULT_TODAY_METRICS).split(",")));
        final String[] labels = new String[ALL_METRICS.length];
        final boolean[] checked = new boolean[ALL_METRICS.length];
        for (int i = 0; i < ALL_METRICS.length; i++) {
            labels[i] = metricLabel(ALL_METRICS[i]);
            checked[i] = enabled.contains(ALL_METRICS[i]);
        }
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.pulse_customize_today)
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < ALL_METRICS.length; i++) {
                        if (checked[i]) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(ALL_METRICS[i]);
                        }
                    }
                    GBApplication.getPrefs().getPreferences().edit()
                            .putString("pulse_today_metrics", sb.toString()).apply();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Open the in-depth Gadgetbridge chart for a metric. */
    private void openChart(final String chartTab, final int titleRes, final String mode) {
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (devices.isEmpty()) return;
        final Intent i = new Intent(requireActivity(), ActivityChartsActivity.class);
        i.putExtra(GBDevice.EXTRA_DEVICE, devices.get(0));
        i.putExtra(ActivityChartsActivity.EXTRA_SINGLE_FRAGMENT_NAME, chartTab);
        i.putExtra(ActivityChartsActivity.EXTRA_ACTIONBAR_TITLE, titleRes);
        i.putExtra(ActivityChartsActivity.EXTRA_TIMESTAMP, dashboardData.timeTo);
        i.putExtra(ActivityChartsActivity.EXTRA_MODE, mode);
        startActivity(i);
    }

    private MetricSpec resolveMetric(final String metric) {
        final Context ctx = requireContext();
        final java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
        final java.util.Locale loc = java.util.Locale.getDefault();

        final int iconRes, chipRes, tint, titleRes;
        final String label, value, chartTab, chartMode;
        final float factor;

        switch (metric) {
            case "distance": {
                final float meters = dashboardData.getDistanceTotal();
                iconRes = R.drawable.ic_map; chipRes = R.drawable.pulse_chip_steps; tint = nodomain.freeyourgadget.gadgetbridge.GBApplication.getAccentColor(ctx);
                label = getString(R.string.distance);
                value = nodomain.freeyourgadget.gadgetbridge.util.FormatUtils.getFormattedDistanceLabel(meters);
                factor = dashboardData.getDistanceGoalFactor();
                chartTab = "stepsweek"; titleRes = R.string.distance; chartMode = "";
                break;
            }
            case "activetime": {
                final long mins = dashboardData.getActiveMinutesTotal();
                iconRes = R.drawable.ic_activity_exercise; chipRes = R.drawable.pulse_chip_sleep; tint = ContextCompat.getColor(ctx, R.color.pulse_mint);
                label = getString(R.string.activity_list_summary_active_time);
                value = mins >= 60 ? String.format(loc, "%dh %dm", mins / 60, mins % 60) : String.format(loc, "%dm", mins);
                factor = dashboardData.getActiveMinutesGoalFactor();
                chartTab = "activity"; titleRes = R.string.activity_list_summary_active_time; chartMode = "";
                break;
            }
            case "calories": {
                final int cal = dashboardData.getActiveCaloriesTotal();
                iconRes = R.drawable.ic_calories; chipRes = R.drawable.pulse_chip_hr; tint = ContextCompat.getColor(ctx, R.color.pulse_ring_hr);
                label = getString(R.string.calories);
                value = cal > 0 ? nf.format(cal) : getString(R.string.stats_empty_value);
                factor = dashboardData.getActiveCaloriesGoalFactor();
                chartTab = "calories"; titleRes = R.string.active_calories; chartMode = "ACTIVE_CALORIES_GOAL";
                break;
            }
            case "sleep": {
                final long mins = dashboardData.getSleepMinutesTotal();
                iconRes = R.drawable.ic_nights_stay; chipRes = R.drawable.pulse_chip_sleep; tint = ContextCompat.getColor(ctx, R.color.pulse_purple);
                label = getString(R.string.menuitem_sleep);
                value = mins > 0 ? String.format(loc, "%dh %dm", mins / 60, mins % 60) : getString(R.string.stats_empty_value);
                factor = dashboardData.getSleepMinutesGoalFactor();
                chartTab = "sleep"; titleRes = R.string.menuitem_sleep; chartMode = "";
                break;
            }
            case "heartrate": {
                final Integer v = extraMetrics.get("heartrate");
                iconRes = R.drawable.ic_heartrate; chipRes = R.drawable.pulse_chip_hr; tint = ContextCompat.getColor(ctx, R.color.pulse_ring_hr);
                label = getString(R.string.menuitem_hr);
                value = v != null ? String.valueOf(v) : getString(R.string.stats_empty_value);
                // bar fills across a typical 40–200 bpm range
                factor = v != null ? (v - 40) / 160f : 0f;
                chartTab = "heartrate"; titleRes = R.string.menuitem_hr; chartMode = "";
                break;
            }
            case "bodybattery": {
                final Integer v = extraMetrics.get("bodybattery");
                iconRes = R.drawable.ic_battery_full; chipRes = R.drawable.pulse_chip_sleep; tint = ContextCompat.getColor(ctx, R.color.pulse_mint);
                label = getString(R.string.body_energy);
                value = v != null ? String.valueOf(v) : getString(R.string.stats_empty_value);
                factor = v != null ? v / 100f : 0f;
                chartTab = "bodyenergy"; titleRes = R.string.body_energy; chartMode = "";
                break;
            }
            case "stress": {
                final Integer v = extraMetrics.get("stress");
                iconRes = R.drawable.ic_heartrate; chipRes = R.drawable.pulse_chip_hr; tint = ContextCompat.getColor(ctx, R.color.pulse_ring_cal);
                label = getString(R.string.menuitem_stress);
                value = v != null ? String.valueOf(v) : getString(R.string.stats_empty_value);
                factor = v != null ? v / 100f : 0f;
                chartTab = "stress"; titleRes = R.string.menuitem_stress; chartMode = "";
                break;
            }
            case "spo2": {
                final Integer v = extraMetrics.get("spo2");
                iconRes = R.drawable.ic_heart; chipRes = R.drawable.pulse_chip_steps; tint = ContextCompat.getColor(ctx, R.color.pulse_neon_cyan);
                label = getString(R.string.pref_header_spo2);
                value = v != null ? v + "%" : getString(R.string.stats_empty_value);
                factor = v != null ? v / 100f : 0f;
                chartTab = "spo2"; titleRes = R.string.pref_header_spo2; chartMode = "";
                break;
            }
            case "hrv": {
                final Integer v = extraMetrics.get("hrv");
                iconRes = R.drawable.ic_heartrate; chipRes = R.drawable.pulse_chip_steps; tint = nodomain.freeyourgadget.gadgetbridge.GBApplication.getAccentColor(ctx);
                label = getString(R.string.hrv);
                value = v != null ? v + " ms" : getString(R.string.stats_empty_value);
                // bar fills across a typical 0–120 ms range
                factor = v != null ? v / 120f : 0f;
                chartTab = "hrvstatus"; titleRes = R.string.hrv; chartMode = "";
                break;
            }
            case "respiration": {
                final Integer v = extraMetrics.get("respiration");
                iconRes = R.drawable.ic_heart; chipRes = R.drawable.pulse_chip_sleep; tint = ContextCompat.getColor(ctx, R.color.pulse_purple);
                label = getString(R.string.respiratoryrate);
                value = v != null ? String.valueOf(v) : getString(R.string.stats_empty_value);
                // bar fills across a typical 6–30 breaths/min range
                factor = v != null ? (v - 6) / 24f : 0f;
                chartTab = "respiratoryrate"; titleRes = R.string.respiratoryrate; chartMode = "";
                break;
            }
            default: { // steps
                final int steps = dashboardData.getStepsTotal();
                iconRes = R.drawable.ic_steps; chipRes = R.drawable.pulse_chip_steps; tint = ContextCompat.getColor(ctx, R.color.pulse_ring_steps);
                label = getString(R.string.steps);
                value = nf.format(steps);
                factor = dashboardData.getStepsGoalFactor();
                chartTab = "stepsweek"; titleRes = R.string.steps; chartMode = "";
                break;
            }
        }
        return new MetricSpec(iconRes, chipRes, tint, titleRes, label, value, chartTab, chartMode, factor);
    }

    /** Holds everything needed to render a metric card. */
    private static final class MetricSpec {
        final int iconRes, chipRes, tint, titleRes;
        final String label, value, chartTab, chartMode;
        final float factor;

        MetricSpec(final int iconRes, final int chipRes, final int tint, final int titleRes,
                   final String label, final String value, final String chartTab,
                   final String chartMode, final float factor) {
            this.iconRes = iconRes; this.chipRes = chipRes; this.tint = tint; this.titleRes = titleRes;
            this.label = label; this.value = value; this.chartTab = chartTab;
            this.chartMode = chartMode; this.factor = factor;
        }
    }

    /** Fetch 7 daily values (oldest..today) for each Health metric, for the mini charts. */
    private void warmHealthHistory() {
        healthHistory.clear();
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (devices.isEmpty()) return;
        final GBDevice dev = devices.get(0);
        try (nodomain.freeyourgadget.gadgetbridge.database.DBHandler db = GBApplication.acquireDbReadOnly()) {
            final nodomain.freeyourgadget.gadgetbridge.entities.DaoSession s = db.getDaoSession();
            final nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator c = dev.getDeviceCoordinator();
            final Calendar today = Calendar.getInstance();
            for (final String metric : pulseMetrics()) {
                final int[] vals = new int[7];
                for (int i = 0; i < 7; i++) {
                    final Calendar day = (Calendar) today.clone();
                    day.add(Calendar.DAY_OF_MONTH, -(6 - i));
                    vals[i] = healthDailyValue(metric, day, dev, c, s, db);
                }
                healthHistory.put(metric, vals);
            }
        } catch (final Exception e) {
            LOG.warn("Pulse: health history query failed", e);
        }
    }

    private int healthDailyValue(final String metric, final Calendar day, final GBDevice dev,
                                 final nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator c,
                                 final nodomain.freeyourgadget.gadgetbridge.entities.DaoSession s,
                                 final nodomain.freeyourgadget.gadgetbridge.database.DBHandler db) {
        final Calendar d0 = (Calendar) day.clone();
        d0.set(Calendar.HOUR_OF_DAY, 0); d0.set(Calendar.MINUTE, 0); d0.set(Calendar.SECOND, 0); d0.set(Calendar.MILLISECOND, 0);
        final long from = d0.getTimeInMillis();
        final long to = from + 86400000L - 1;
        try {
            switch (metric) {
                case "steps":    return (int) DailyTotals.getDailyTotalsForDevice(dev, day, db).getSteps();
                case "distance": return (int) DailyTotals.getDailyTotalsForDevice(dev, day, db).getDistance();
                case "calories": return (int) (DailyTotals.getDailyTotalsForDevice(dev, day, db).getActiveCalories() / 1000);
                case "activetime":
                case "sleep":    return (int) DailyTotals.getDailyTotalsForDevice(dev, day, db).getSleep();
                case "bodybattery": {
                    long sum = 0; int n = 0;
                    for (final nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample x : c.getBodyEnergySampleProvider(dev, s).getAllSamples(from, to)) { sum += x.getEnergy(); n++; }
                    return n > 0 ? (int) (sum / n) : 0;
                }
                case "stress": {
                    long sum = 0; int n = 0;
                    for (final nodomain.freeyourgadget.gadgetbridge.model.StressSample x : c.getStressSampleProvider(dev, s).getAllSamples(from, to)) { if (x.getStress() > 0) { sum += x.getStress(); n++; } }
                    return n > 0 ? (int) (sum / n) : 0;
                }
                case "spo2": {
                    long sum = 0; int n = 0;
                    for (final nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample x : c.getSpo2SampleProvider(dev, s).getAllSamples(from, to)) { if (x.getSpo2() > 0) { sum += x.getSpo2(); n++; } }
                    return n > 0 ? (int) (sum / n) : 0;
                }
                case "hrv": {
                    long sum = 0; int n = 0;
                    for (final nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample x : c.getHrvValueSampleProvider(dev, s).getAllSamples(from, to)) { sum += x.getValue(); n++; }
                    return n > 0 ? (int) (sum / n) : 0;
                }
                case "respiration": {
                    double sum = 0; int n = 0;
                    for (final nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample x : c.getRespiratoryRateSampleProvider(dev, s).getAllSamples(from, to)) { if (x.getRespiratoryRate() > 0) { sum += x.getRespiratoryRate(); n++; } }
                    return n > 0 ? (int) Math.round(sum / n) : 0;
                }
                default: {
                    long sum = 0; int n = 0;
                    for (final nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample x : c.getHeartRateRestingSampleProvider(dev, s).getAllSamples(from, to)) { if (x.getHeartRate() > 0) { sum += x.getHeartRate(); n++; } }
                    return n > 0 ? (int) (sum / n) : 0;
                }
            }
        } catch (final Exception ignored) {
            return 0;
        }
    }

    /** A Health "cube" card: name, big value and a 7-day mini bar chart with weekday labels. */
    private void addHealthCubeCard(final String metric) {
        final Context ctx = requireContext();
        final float scale = getResources().getDisplayMetrics().density;
        final MetricSpec spc = resolveMetric(metric);

        final com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(ctx);
        card.setRadius(dp(20, scale));
        card.setCardElevation(0);
        card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.pulse_card));
        final GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f));
        lp.width = 0;
        lp.setMargins(dp(5, scale), dp(5, scale), dp(5, scale), dp(5, scale));
        card.setLayoutParams(lp);

        final LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16, scale), dp(16, scale), dp(16, scale), dp(14, scale));

        final TextView name = new TextView(ctx);
        name.setText(spc.label);
        name.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text_dim));
        name.setTextSize(13);
        root.addView(name);

        final TextView value = new TextView(ctx);
        value.setText(spc.value);
        value.setTextColor(spc.tint);
        value.setTextSize(26);
        value.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(ctx, R.font.unbounded));
        final LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vlp.topMargin = dp(4, scale);
        root.addView(value, vlp);

        root.addView(buildMiniChart(ctx, scale, healthHistory.get(metric), spc.tint));

        card.setClickable(true);
        card.setRippleColorResource(R.color.pulse_card_alt);
        final int titleRes = spc.titleRes;
        final String chartTab = spc.chartTab, chartMode = spc.chartMode;
        card.setOnClickListener(v -> openChart(chartTab, titleRes, chartMode));
        card.addView(root);
        gridLayout.addView(card);
    }

    /** 7-day mini bar chart with weekday labels (oldest..today). */
    private View buildMiniChart(final Context ctx, final float scale, final int[] hist, final int tint) {
        final LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(14, scale);
        row.setLayoutParams(rowLp);

        int max = 1;
        if (hist != null) { for (final int v : hist) { if (v > max) max = v; } }
        final int barAreaH = dp(40, scale);
        final java.text.SimpleDateFormat dayFmt = new java.text.SimpleDateFormat("EEEEE", java.util.Locale.getDefault());
        final Calendar today = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            final LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            final LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            col.setLayoutParams(colLp);

            final LinearLayout barBox = new LinearLayout(ctx);
            barBox.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            barBox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barAreaH));
            final int v = (hist != null && i < hist.length) ? hist[i] : 0;
            final int barW = dp(14, scale);
            // Pill-shaped bars (rounded top + bottom), à la Google Health. Minimum is a clean dot.
            final int barH = v > 0 ? Math.max(barW, Math.round(barAreaH * (v / (float) max))) : barW;
            final android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
            pill.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            pill.setCornerRadius(barW / 2f);
            pill.setColor(tint);
            final View bar = new View(ctx);
            bar.setBackground(pill);
            bar.setAlpha(v > 0 ? 0.9f : 0.25f);
            barBox.addView(bar, new LinearLayout.LayoutParams(barW, barH));
            col.addView(barBox);

            final TextView lbl = new TextView(ctx);
            final Calendar d = (Calendar) today.clone();
            d.add(Calendar.DAY_OF_MONTH, -(6 - i));
            lbl.setText(dayFmt.format(d.getTime()));
            lbl.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text_dim));
            lbl.setTextSize(9);
            lbl.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            lbl.setPadding(0, dp(5, scale), 0, 0);
            col.addView(lbl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row.addView(col);
        }
        return row;
    }

    /** A trailing "Customize" cube on the Health grid → pick which metrics show. */
    private void addHealthCustomizeCard() {
        final Context ctx = requireContext();
        final float scale = getResources().getDisplayMetrics().density;
        final com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(ctx);
        card.setRadius(dp(20, scale));
        card.setCardElevation(0);
        card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.pulse_card));
        card.setStrokeColor(ContextCompat.getColor(ctx, R.color.pulse_card_alt));
        card.setStrokeWidth(dp(1, scale));
        final GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f));
        lp.width = 0;
        lp.setMargins(dp(5, scale), dp(5, scale), dp(5, scale), dp(5, scale));
        card.setLayoutParams(lp);

        final LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setPadding(dp(16, scale), dp(28, scale), dp(16, scale), dp(28, scale));
        final android.widget.ImageView plus = new android.widget.ImageView(ctx);
        plus.setImageResource(R.drawable.ic_add);
        plus.setColorFilter(GBApplication.getAccentColor(ctx));
        plus.setLayoutParams(new LinearLayout.LayoutParams(dp(28, scale), dp(28, scale)));
        final TextView t = new TextView(ctx);
        t.setText(R.string.pulse_customize_health);
        t.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text_dim));
        t.setTextSize(13);
        t.setPadding(0, dp(8, scale), 0, 0);
        root.addView(plus);
        root.addView(t);

        card.setClickable(true);
        card.setRippleColorResource(R.color.pulse_card_alt);
        card.setOnClickListener(v -> showMetricPicker("pulse_health_metrics", DEFAULT_HEALTH_METRICS, R.string.pulse_customize_health));
        card.addView(root);
        gridLayout.addView(card);
    }

    /** Multi-select which metrics a customizable tab shows. */
    private void showMetricPicker(final String prefKey, final String def, final int titleRes) {
        final java.util.List<String> enabled = new java.util.ArrayList<>(java.util.Arrays.asList(
                GBApplication.getPrefs().getString(prefKey, def).split(",")));
        final String[] labels = new String[ALL_METRICS.length];
        final boolean[] checked = new boolean[ALL_METRICS.length];
        for (int i = 0; i < ALL_METRICS.length; i++) {
            labels[i] = metricLabel(ALL_METRICS[i]);
            checked[i] = enabled.contains(ALL_METRICS[i]);
        }
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < ALL_METRICS.length; i++) {
                        if (checked[i]) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(ALL_METRICS[i]);
                        }
                    }
                    GBApplication.getPrefs().getPreferences().edit().putString(prefKey, sb.toString()).apply();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Compute the Sleep tab's inline detail (score, last-night stages, 7-night totals). */
    private void warmSleepDetail() {
        sleepHasNight = false; sleepScore = 0;
        sleepDeep = sleepLight = sleepRem = sleepAwake = 0; sleepBed = null; sleepWake = null;
        java.util.Arrays.fill(sleepWeek, 0);
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (devices.isEmpty()) return;
        try (nodomain.freeyourgadget.gadgetbridge.database.DBHandler db = GBApplication.acquireDbReadOnly()) {
            final nodomain.freeyourgadget.gadgetbridge.entities.DaoSession s = db.getDaoSession();
            final long now = System.currentTimeMillis();
            for (final GBDevice dev : devices) {
                try {
                    final nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample x =
                            dev.getDeviceCoordinator().getSleepScoreProvider(dev, s).getLatestSample(now);
                    if (x != null) sleepScore = Math.max(sleepScore, x.getSleepScore());
                } catch (final Exception ignored) { }
            }
            final Calendar to = Calendar.getInstance();
            to.set(Calendar.HOUR_OF_DAY, 12); to.set(Calendar.MINUTE, 0); to.set(Calendar.SECOND, 0); to.set(Calendar.MILLISECOND, 0);
            if (to.getTimeInMillis() > now) to.setTimeInMillis(now);
            final int toSec = (int) (to.getTimeInMillis() / 1000L);
            final int fromSec = toSec - 24 * 3600;
            nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis.SleepSession best = null;
            long bestAsleep = -1;
            for (final GBDevice dev : devices) {
                try {
                    final List<? extends nodomain.freeyourgadget.gadgetbridge.model.ActivitySample> samples =
                            dev.getDeviceCoordinator().getSampleProvider(dev, s).getAllActivitySamples(fromSec, toSec);
                    for (final nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis.SleepSession ss :
                            new nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis().calculateSleepSessions(samples)) {
                        final long asleep = ss.getLightSleepDuration() + ss.getDeepSleepDuration() + ss.getRemSleepDuration();
                        if (asleep > bestAsleep) { bestAsleep = asleep; best = ss; }
                    }
                } catch (final Exception ignored) { }
            }
            if (best != null) {
                sleepHasNight = true;
                sleepDeep = best.getDeepSleepDuration() / 60;
                sleepLight = best.getLightSleepDuration() / 60;
                sleepRem = best.getRemSleepDuration() / 60;
                sleepAwake = best.getAwakeSleepDuration() / 60;
                sleepBed = best.getSleepStart();
                sleepWake = best.getSleepEnd();
            }
            final Calendar today = Calendar.getInstance();
            for (int i = 0; i < 7; i++) {
                final Calendar day = (Calendar) today.clone();
                day.add(Calendar.DAY_OF_MONTH, -(6 - i));
                long mins = 0;
                for (final GBDevice dev : devices) {
                    mins += DailyTotals.getDailyTotalsForDevice(dev, day, db).getSleep();
                }
                sleepWeek[i] = (int) mins;
            }
        } catch (final Exception e) {
            LOG.warn("Pulse: sleep detail query failed", e);
        }
        sleepGoalMin = new ActivityUser().getSleepDurationGoal();
        final long asleep = sleepDeep + sleepLight + sleepRem;
        if (sleepScore <= 0 && sleepHasNight && sleepGoalMin > 0) {
            double sc = 70.0 * asleep / sleepGoalMin;
            final long total = asleep + sleepAwake;
            if (total > 0) sc += 30.0 * (sleepDeep + sleepRem) / total;
            sleepScore = (int) Math.max(1, Math.min(100, Math.round(sc)));
        }
    }

    /** Render the inline Sleep-tab detail card; tap opens the full sleep screen. */
    private void renderSleepDetail() {
        final Context ctx = requireContext();
        final float scale = getResources().getDisplayMetrics().density;
        sleepDetailContainer.removeAllViews();

        final com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(ctx);
        card.setRadius(dp(20, scale));
        card.setCardElevation(0);
        card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.pulse_card));
        card.setClickable(true);
        card.setRippleColorResource(R.color.pulse_card_alt);
        card.setOnClickListener(v -> startActivity(new Intent(requireContext(), PulseSleepActivity.class)));

        final LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18, scale), dp(18, scale), dp(18, scale), dp(16, scale));

        if (!sleepHasNight && sleepScore <= 0) {
            final TextView empty = new TextView(ctx);
            empty.setText(R.string.pulse_sleep_no_data);
            empty.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text_dim));
            empty.setTextSize(14);
            root.addView(empty);
            card.addView(root);
            sleepDetailContainer.addView(card);
            return;
        }

        // score + quality word
        final int color; final int wordRes;
        if (sleepScore >= 85) { color = R.color.pulse_mint; wordRes = R.string.pulse_sleep_q_excellent; }
        else if (sleepScore >= 70) { color = R.color.pulse_neon_cyan; wordRes = R.string.pulse_sleep_q_good; }
        else if (sleepScore >= 50) { color = R.color.pulse_ring_cal; wordRes = R.string.pulse_sleep_q_fair; }
        else { color = R.color.pulse_ring_hr; wordRes = R.string.pulse_sleep_q_poor; }

        final LinearLayout scoreRow = new LinearLayout(ctx);
        scoreRow.setOrientation(LinearLayout.HORIZONTAL);
        scoreRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        final TextView scoreView = new TextView(ctx);
        scoreView.setText(String.valueOf(sleepScore));
        scoreView.setTextColor(ContextCompat.getColor(ctx, color));
        scoreView.setTextSize(40);
        scoreView.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(ctx, R.font.unbounded));
        scoreRow.addView(scoreView);
        final LinearLayout scoreText = new LinearLayout(ctx);
        scoreText.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.leftMargin = dp(12, scale);
        scoreText.setLayoutParams(stLp);
        final TextView scoreLbl = new TextView(ctx);
        scoreLbl.setText(R.string.pulse_sleep_score);
        scoreLbl.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text_dim));
        scoreLbl.setTextSize(13);
        final TextView word = new TextView(ctx);
        word.setText(wordRes);
        word.setTextColor(ContextCompat.getColor(ctx, color));
        word.setTextSize(15);
        word.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(ctx, R.font.unbounded));
        scoreText.addView(scoreLbl); scoreText.addView(word);
        scoreRow.addView(scoreText);
        root.addView(scoreRow);

        // bed → wake + asleep duration
        if (sleepBed != null && sleepWake != null) {
            final long asleep = sleepDeep + sleepLight + sleepRem;
            final java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
            final TextView times = new TextView(ctx);
            times.setText(asleepHm(asleep) + "   ·   " + tf.format(sleepBed) + " → " + tf.format(sleepWake));
            times.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text_dim));
            times.setTextSize(13);
            final LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tLp.topMargin = dp(4, scale);
            root.addView(times, tLp);
        }

        // stage proportions bar + legend
        final LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        final LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10, scale));
        barLp.topMargin = dp(14, scale);
        bar.setLayoutParams(barLp);
        final LinearLayout legend = new LinearLayout(ctx);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        final LinearLayout.LayoutParams legLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        legLp.topMargin = dp(10, scale);
        legend.setLayoutParams(legLp);
        addSleepStage(bar, legend, R.string.pulse_sleep_deep, R.color.pulse_purple, sleepDeep, scale);
        addSleepStage(bar, legend, R.string.pulse_sleep_light, R.color.pulse_neon_cyan, sleepLight, scale);
        addSleepStage(bar, legend, R.string.pulse_sleep_rem, R.color.pulse_neon, sleepRem, scale);
        addSleepStage(bar, legend, R.string.pulse_sleep_awake, R.color.pulse_ring_hr, sleepAwake, scale);
        root.addView(bar);
        root.addView(legend);

        // 7-night trend
        root.addView(buildMiniChart(ctx, scale, sleepWeek, ContextCompat.getColor(ctx, R.color.pulse_purple)));

        card.addView(root);
        sleepDetailContainer.addView(card);
    }

    private void addSleepStage(final LinearLayout bar, final LinearLayout legend, final int labelRes,
                               final int colorRes, final long mins, final float scale) {
        if (mins <= 0) return;
        final int color = ContextCompat.getColor(requireContext(), colorRes);
        final View seg = new View(requireContext());
        seg.setBackgroundColor(color);
        bar.addView(seg, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) mins));
        final LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final View dot = new View(requireContext());
        dot.setBackgroundColor(color);
        item.addView(dot, new LinearLayout.LayoutParams(dp(8, scale), dp(8, scale)));
        final TextView t = new TextView(requireContext());
        t.setText(getString(labelRes) + " " + asleepHm(mins));
        t.setTextColor(ContextCompat.getColor(requireContext(), R.color.pulse_text_dim));
        t.setTextSize(10);
        t.setPadding(dp(4, scale), 0, 0, 0);
        item.addView(t);
        legend.addView(item);
    }

    private String asleepHm(final long mins) {
        return mins >= 60 ? (mins / 60) + "h " + (mins % 60) + "m" : mins + "m";
    }

    private void addPulseStatCard(final String metric) {
        final Context ctx = requireContext();
        final float scale = ctx.getResources().getDisplayMetrics().density;
        final MetricSpec spc = resolveMetric(metric);
        final int iconRes = spc.iconRes, chipRes = spc.chipRes, tint = spc.tint, titleRes = spc.titleRes;
        final String label = spc.label, value = spc.value, chartTab = spc.chartTab, chartMode = spc.chartMode;
        final float factor = spc.factor;

        final MaterialCardView card = new MaterialCardView(ctx);
        card.setRadius(dp(20, scale));
        card.setCardElevation(0);
        card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.pulse_card));
        final GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1f));
        lp.width = 0;
        lp.setMargins(dp(6, scale), dp(6, scale), dp(6, scale), dp(6, scale));
        card.setLayoutParams(lp);

        final LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14, scale), dp(14, scale), dp(14, scale), dp(14, scale));

        // icon chip + label
        final LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        final ImageView icon = new ImageView(ctx);
        icon.setBackgroundResource(chipRes);
        final int chipPad = dp(7, scale);
        icon.setPadding(chipPad, chipPad, chipPad, chipPad);
        icon.setImageResource(iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(tint));
        topRow.addView(icon, new LinearLayout.LayoutParams(dp(32, scale), dp(32, scale)));
        final TextView labelView = new TextView(ctx);
        labelView.setText(label);
        labelView.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text_dim));
        labelView.setTextSize(12);
        final LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelLp.setMarginStart(dp(9, scale));
        topRow.addView(labelView, labelLp);
        root.addView(topRow);

        // big value
        final TextView valueView = new TextView(ctx);
        valueView.setText(value);
        valueView.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text));
        valueView.setTextSize(24);
        valueView.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(ctx, R.font.unbounded));
        final LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueLp.topMargin = dp(8, scale);
        root.addView(valueView, valueLp);

        // thin progress bar
        final LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundResource(R.drawable.pulse_bar_track);
        final LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5, scale));
        barLp.topMargin = dp(12, scale);
        bar.setLayoutParams(barLp);
        final float clamped = Math.max(0f, Math.min(1f, factor));
        if (clamped > 0f) {
            final View fill = new View(ctx);
            fill.setBackgroundResource(R.drawable.pulse_bar_fill);
            fill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tint));
            bar.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, clamped));
        }
        if (clamped < 1f) {
            bar.addView(new View(ctx), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - clamped));
        }
        root.addView(bar);

        card.setClickable(true);
        card.setRippleColorResource(R.color.pulse_card_alt);
        card.setOnClickListener(v -> {
            if ("sleep".equals(metric)) {
                startActivity(new Intent(requireContext(), PulseSleepActivity.class));
            } else {
                openChart(chartTab, titleRes, chartMode);
            }
        });

        card.addView(root);
        gridLayout.addView(card);
    }

    /** Apply an alpha (0..1) to an opaque colour. */
    private static int withAlpha(final int color, final float alpha) {
        return (color & 0x00FFFFFF) | (Math.round(alpha * 255f) << 24);
    }

    // Goal-based metrics the hero ring can display.
    static final String[] RING_METRICS = {"steps", "activetime", "sleep", "calories", "distance"};

    private float goalFactor(final String metric) {
        switch (metric) {
            case "activetime": return dashboardData.getActiveMinutesGoalFactor();
            case "sleep":      return dashboardData.getSleepMinutesGoalFactor();
            case "calories":   return dashboardData.getActiveCaloriesGoalFactor();
            case "distance":   return dashboardData.getDistanceGoalFactor();
            default:           return dashboardData.getStepsGoalFactor();
        }
    }

    private static String dateStr(final int dayOffset) {
        final Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, dayOffset);
        return String.format(java.util.Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** Pulse streaks: +1 each day you hit your chosen goal, Duolingo-style. */
    private void updateStreak() {
        if (streakCount == null) return;
        final Prefs prefs = GBApplication.getPrefs();
        final String goal = prefs.getString("pulse_streak_goal", "steps");
        final String today = dateStr(0);
        final String yesterday = dateStr(-1);
        String last = prefs.getString("pulse_streak_last", "");
        int count = prefs.getInt("pulse_streak_count", 0);

        // Only credit a streak while actually viewing today's (live) data.
        final boolean viewingToday = DateTimeUtils.isSameDay(GregorianCalendar.getInstance(), day);
        final boolean reached = "any".equals(goal) ? anyGoalReached() : goalFactor(goal) >= 1f;
        if (viewingToday && reached && !today.equals(last)) {
            count = yesterday.equals(last) ? count + 1 : 1;
            last = today;
            final int best = Math.max(count, prefs.getInt("pulse_streak_best", 0));
            prefs.getPreferences().edit()
                    .putString("pulse_streak_last", last)
                    .putInt("pulse_streak_count", count)
                    .putInt("pulse_streak_best", best)
                    .apply();
            sendPulseNotification(getString(R.string.pulse_streak_notif_title, count),
                    getString(R.string.pulse_streak_notif_text, metricLabel(goal)));
        }
        final int shown = (today.equals(last) || yesterday.equals(last)) ? count : 0;
        streakCount.setText(String.valueOf(shown));
    }

    /** Time-of-day greeting, personalised with the user's first name when set. */
    private String buildGreeting() {
        final int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        final int res = h < 12 ? R.string.pulse_greeting_morning
                : (h < 18 ? R.string.pulse_greeting_afternoon : R.string.pulse_greeting_evening);
        String greeting = getString(res);
        final String name = new ActivityUser().getName();
        if (name != null && !name.trim().isEmpty() && !name.equalsIgnoreCase("gadgetbridge-user")) {
            greeting += ", " + name.trim().split("\\s+")[0];
        }
        return greeting;
    }

    /** A celebratory pop on the hero ring + confetti when a goal is hit. */
    private void celebrateRing() {
        if (isAdded()) {
            final View content = requireActivity().findViewById(android.R.id.content);
            if (content instanceof ViewGroup) {
                nodomain.freeyourgadget.gadgetbridge.activities.views.PulseConfettiView
                        .celebrate((ViewGroup) content);
            }
        }
        if (pulseRing == null) return;
        pulseRing.animate().scaleX(1.12f).scaleY(1.12f).setDuration(180)
                .withEndAction(() -> {
                    if (pulseRing != null) {
                        pulseRing.animate().scaleX(1f).scaleY(1f).setDuration(240)
                                .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                    }
                }).start();
    }

    /** Send a Pulse phone notification. */
    private void sendPulseNotification(final String title, final String text) {
        final Context ctx = requireContext();
        final android.app.NotificationManager nm =
                (android.app.NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        final Intent open = new Intent(ctx, ControlCenterv2.class);
        final android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
                ctx, 0, open, android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        final androidx.core.app.NotificationCompat.Builder b =
                new androidx.core.app.NotificationCompat.Builder(ctx, nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setColor(nodomain.freeyourgadget.gadgetbridge.GBApplication.getAccentColor(ctx))
                        .setAutoCancel(true)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pi);
        try {
            nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
        } catch (final SecurityException ignored) {
            // POST_NOTIFICATIONS not granted
        }
    }

    /** When the hero-ring goal is newly hit today: pop the ring + notify (once per day). */
    private void checkGoalReached() {
        final boolean viewingToday = DateTimeUtils.isSameDay(GregorianCalendar.getInstance(), day);
        if (!viewingToday) return;
        final Prefs prefs = GBApplication.getPrefs();
        final String today = dateStr(0);
        // Notify for every goal completed today (each metric is de-duped by its own key).
        boolean celebrated = false;
        for (final String metric : RING_METRICS) {
            final String key = "pulse_goal_notified_" + metric;
            if (goalFactor(metric) >= 1f && !today.equals(prefs.getString(key, ""))) {
                prefs.getPreferences().edit().putString(key, today).apply();
                if (!celebrated) {
                    celebrateRing();
                    celebrated = true;
                }
                sendPulseNotification(getString(R.string.pulse_goal_reached_title),
                        getString(R.string.pulse_goal_reached_text, metricLabel(metric)));
            }
        }
    }

    /** True if any tracked goal has been met (for the "any goal" streak mode). */
    private boolean anyGoalReached() {
        for (final String metric : RING_METRICS) {
            if (goalFactor(metric) >= 1f) return true;
        }
        return false;
    }

    private void showStreakDialog() {
        final Prefs prefs = GBApplication.getPrefs();
        final int count = prefs.getInt("pulse_streak_count", 0);
        final String today = dateStr(0);
        final String yesterday = dateStr(-1);
        final String last = prefs.getString("pulse_streak_last", "");
        final int shown = (today.equals(last) || yesterday.equals(last)) ? count : 0;

        final String[] labels = new String[RING_METRICS.length];
        for (int i = 0; i < RING_METRICS.length; i++) labels[i] = metricLabel(RING_METRICS[i]);
        final String goal = prefs.getString("pulse_streak_goal", "steps");
        int sel = 0;
        for (int i = 0; i < RING_METRICS.length; i++) if (RING_METRICS[i].equals(goal)) sel = i;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.pulse_streak_title, shown))
                .setSingleChoiceItems(labels, sel, (d, which) -> {
                    prefs.getPreferences().edit()
                            .putString("pulse_streak_goal", RING_METRICS[which]).apply();
                    d.dismiss();
                    refresh();
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void setRing(final String label, final String value, final String goal, final float factor) {
        pulseRing.setProgress(factor);
        ringLabel.setText(label);
        setRingValue(value);
        ringGoal.setText(goal);
    }

    /** Sets the big ring value, shrinking the font for word placeholders like "No sleep". */
    private void setRingValue(final String value) {
        final boolean numeric = !value.isEmpty() && Character.isDigit(value.charAt(0));
        ringValue.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, numeric ? 36f : 24f);
        ringValue.setText(value);
    }

    /** Render the hero ring for the chosen metric. */
    private void renderRing(final String metric) {
        final java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
        final java.util.Locale loc = java.util.Locale.getDefault();
        switch (metric) {
            case "activetime": {
                final long m = dashboardData.getActiveMinutesTotal();
                final float f = dashboardData.getActiveMinutesGoalFactor();
                final int goal = new ActivityUser().getActiveTimeGoalMinutes();
                setRing(getString(R.string.activity_list_summary_active_time),
                        m >= 60 ? String.format(loc, "%dh %dm", m / 60, m % 60) : String.format(loc, "%dm", m),
                        goal > 0 ? getString(R.string.pulse_of_goal, goal + "m") : "", f);
                return;
            }
            case "sleep": {
                final long m = dashboardData.getSleepMinutesTotal();
                final int goal = new ActivityUser().getSleepDurationGoal();
                setRing(getString(R.string.menuitem_sleep),
                        m > 0 ? String.format(loc, "%dh %dm", m / 60, m % 60) : getString(R.string.pulse_no_sleep),
                        getString(R.string.pulse_of_goal, (goal / 60) + "h"),
                        goal > 0 ? (float) m / goal : 0f);
                return;
            }
            case "calories": {
                final int c = dashboardData.getActiveCaloriesTotal();
                final float f = dashboardData.getActiveCaloriesGoalFactor();
                final int goal = new ActivityUser().getCaloriesBurntGoal();
                setRing(getString(R.string.calories),
                        c > 0 ? nf.format(c) : getString(R.string.stats_empty_value),
                        goal > 0 ? getString(R.string.pulse_of_goal, nf.format(goal)) : "", f);
                return;
            }
            case "distance": {
                final float me = dashboardData.getDistanceTotal();
                final float f = dashboardData.getDistanceGoalFactor();
                setRing(getString(R.string.distance),
                        nodomain.freeyourgadget.gadgetbridge.util.FormatUtils.getFormattedDistanceLabel(me),
                        "", f);
                return;
            }
            default: {
                final int s = dashboardData.getStepsTotal();
                final int goal = new ActivityUser().getStepsGoal();
                setRing(getString(R.string.steps), nf.format(s),
                        getString(R.string.pulse_of_goal, nf.format(goal)), goal > 0 ? (float) s / goal : 0f);
            }
        }
    }

    /** Long-press the ring to pick which metric it shows. */
    private void showRingMetricDialog() {
        final String[] labels = new String[RING_METRICS.length];
        for (int i = 0; i < RING_METRICS.length; i++) labels[i] = metricLabel(RING_METRICS[i]);
        final String current = GBApplication.getPrefs().getString("pulse_ring_metric", "steps");
        int sel = 0;
        for (int i = 0; i < RING_METRICS.length; i++) if (RING_METRICS[i].equals(current)) sel = i;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.pulse_ring_metric_title)
                .setSingleChoiceItems(labels, sel, (d, which) -> {
                    GBApplication.getPrefs().getPreferences().edit()
                            .putString("pulse_ring_metric", RING_METRICS[which]).apply();
                    d.dismiss();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static int dp(final int value, final float scale) {
        return (int) (value * scale + 0.5f);
    }

    /** Pulse: device name + battery + connection dot in the Today header chip. */
    private void updateDeviceChip() {
        if (deviceName == null) return;
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (devices.isEmpty()) {
            deviceName.setText(R.string.pulse_no_device);
            deviceBattery.setText("");
            deviceDot.setAlpha(0.3f);
            return;
        }
        final GBDevice dev = devices.get(0);
        deviceName.setText(dev.getAliasOrName());
        deviceDot.setAlpha(dev.isInitialized() ? 1f : 0.3f);
        final int battery = dev.getBatteryLevel();
        if (battery >= 0 && battery <= 100) {
            deviceBattery.setText(getString(R.string.battery_percentage_str, String.valueOf(battery)));
        } else {
            deviceBattery.setText("");
        }
    }

    /**
     * Pulse: set a custom daily step goal. The number sets the app-side goal (drives the ring).
     * "On watch" opens Garmin's realtime settings, the only path that can change a setting on the
     * device itself — available when the watch exposes a step-goal entry in its own menus.
     */
    private void showStepGoalDialog() {
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(new ActivityUser().getStepsGoal()));
        input.setSelectAllOnFocus(true);

        final androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.pulse_set_step_goal)
                .setMessage(R.string.pulse_step_goal_hint)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    try {
                        final int goal = Integer.parseInt(input.getText().toString().trim());
                        if (goal > 0) {
                            GBApplication.getPrefs().getPreferences().edit()
                                    .putString(ActivityUser.PREF_USER_STEPS_GOAL, String.valueOf(goal)).apply();
                            refresh();
                        }
                    } catch (final NumberFormatException ignored) {
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (!devices.isEmpty()) {
            b.setNeutralButton(R.string.pulse_goal_on_watch, (d, w) -> {
                final Intent i = new Intent(requireActivity(),
                        nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminRealtimeSettingsActivity.class);
                i.putExtra(GBDevice.EXTRA_DEVICE, devices.get(0));
                startActivity(i);
            });
        }
        b.show();
    }

    /** Pulse: fill the hero ring (and Today's stat cards) with section-appropriate data. */
    private void updateHero() {
        if (pulseRing == null) return;
        final java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
        final java.util.Locale loc = java.util.Locale.getDefault();

        if ("sleep".equals(section)) {
            final long mins = dashboardData.getSleepMinutesTotal();
            final int goal = new ActivityUser().getSleepDurationGoal();
            pulseRing.setProgress(goal > 0 ? (float) mins / goal : 0f);
            ringLabel.setText(R.string.menuitem_sleep);
            setRingValue(mins > 0 ? String.format(loc, "%dh %dm", mins / 60, mins % 60) : getString(R.string.pulse_no_sleep));
            ringGoal.setText(getString(R.string.pulse_of_goal, (goal / 60) + "h"));
            return;
        }
        if ("fitness".equals(section)) {
            final long mins = dashboardData.getActiveMinutesTotal();
            final float factor = dashboardData.getActiveMinutesGoalFactor();
            final int goal = new ActivityUser().getActiveTimeGoalMinutes();
            pulseRing.setProgress(factor);
            ringLabel.setText(R.string.pulse_active_label);
            setRingValue(String.format(loc, "%dm", mins));
            ringGoal.setText(goal > 0 ? getString(R.string.pulse_of_goal, goal + "m") : "");
            return;
        }

        // Today: a configurable hero ring + the first 3 enabled metrics as hero cards
        renderRing(GBApplication.getPrefs().getString("pulse_ring_metric", "steps"));

        if (statColumn != null) {
            statColumn.removeAllViews();
            final String[] metrics = pulseMetrics();
            final int heroCount = Math.min(HERO_CARD_COUNT, metrics.length);
            for (int i = 0; i < heroCount; i++) {
                addHeroCard(statColumn, metrics[i], i > 0);
            }
        }

        checkGoalReached();
    }

    private static final int HERO_CARD_COUNT = 3;

    /** Compact hero card (icon + label + value), tap opens the chart. */
    private void addHeroCard(final LinearLayout col, final String metric, final boolean topMargin) {
        final Context ctx = requireContext();
        final float scale = ctx.getResources().getDisplayMetrics().density;
        final MetricSpec spc = resolveMetric(metric);
        final boolean hasData = spc.value != null && !spc.value.isEmpty()
                && !spc.value.equals(getString(R.string.stats_empty_value));
        final boolean tinted = hasData && spc.factor >= 0f;
        final float frac = Math.max(0f, Math.min(1f, spc.factor));

        final MaterialCardView card = new MaterialCardView(ctx);
        card.setRadius(dp(20, scale));
        card.setCardElevation(0);
        card.setCardBackgroundColor(tinted ? withAlpha(spc.tint, 0.16f) : ContextCompat.getColor(ctx, R.color.pulse_card));
        final LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (topMargin) clp.topMargin = dp(10, scale);
        card.setLayoutParams(clp);
        card.setClickable(true);
        card.setRippleColorResource(R.color.pulse_card_alt);
        card.setOnClickListener(v -> {
            if ("sleep".equals(metric)) {
                startActivity(new Intent(requireContext(), PulseSleepActivity.class));
            } else {
                openChart(spc.chartTab, spc.titleRes, spc.chartMode);
            }
        });

        final LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10, scale), dp(10, scale), dp(10, scale), dp(10, scale));

        final ImageView icon = new ImageView(ctx);
        final android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
        pill.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        pill.setCornerRadius(dp(12, scale));
        pill.setColor(ContextCompat.getColor(ctx, R.color.pulse_bg));
        icon.setBackground(pill);
        final int cp = dp(7, scale);
        icon.setPadding(cp, cp, cp, cp);
        icon.setImageResource(spc.iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                tinted ? spc.tint : ContextCompat.getColor(ctx, R.color.pulse_text_dim)));
        row.addView(icon, new LinearLayout.LayoutParams(dp(34, scale), dp(34, scale)));

        final LinearLayout tcol = new LinearLayout(ctx);
        tcol.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMarginStart(dp(10, scale));
        final TextView lab = new TextView(ctx);
        lab.setText(spc.label);
        lab.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text_dim));
        lab.setTextSize(12);
        final TextView val = new TextView(ctx);
        val.setText(spc.value);
        val.setTextColor(ContextCompat.getColor(ctx, R.color.pulse_text));
        val.setTextSize(18);
        val.setTypeface(val.getTypeface(), android.graphics.Typeface.BOLD);
        tcol.addView(lab);
        tcol.addView(val);
        row.addView(tcol, tlp);

        // Fixed content height so the proportional fill is bounded (this column isn't
        // height-constrained like the grid, so MATCH_PARENT would grow unbounded).
        final int tileH = dp(60, scale);
        final android.widget.FrameLayout frame = new android.widget.FrameLayout(ctx);
        // Clip the (rectangular) fill layer to the card's rounded corners.
        final float radiusPx = dp(20, scale);
        frame.setClipToOutline(true);
        frame.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(final View v, final android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radiusPx);
            }
        });
        if (tinted && frac > 0f) {
            final LinearLayout fillRow = new LinearLayout(ctx);
            fillRow.setOrientation(LinearLayout.HORIZONTAL);
            fillRow.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            final View fill = new View(ctx);
            fill.setBackgroundColor(withAlpha(spc.tint, 0.34f));
            fillRow.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, frac));
            if (frac < 1f) {
                fillRow.addView(new View(ctx), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - frac));
            }
            frame.addView(fillRow);
        }
        frame.addView(row, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        card.addView(frame, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, tileH));
        col.addView(card);
    }

    private void createWidget(AbstractDashboardWidget widgetObj, boolean cardsEnabled, int columnSpan) {
        final float scale = requireContext().getResources().getDisplayMetrics().density;
        FragmentContainerView fragment = new FragmentContainerView(requireActivity());
        int fragmentId = View.generateViewId();
        fragment.setId(fragmentId);
        getChildFragmentManager()
                .beginTransaction()
                .replace(fragmentId, widgetObj)
                .commitAllowingStateLoss(); // FIXME: #4007

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSpan, GridLayout.FILL, 1f)
        );
        layoutParams.width = 0;
        int pixels_8dp = (int) (8 * scale + 0.5f);
        layoutParams.setMargins(pixels_8dp, pixels_8dp, pixels_8dp, pixels_8dp);

        if (cardsEnabled) {
            MaterialCardView card = new MaterialCardView(requireActivity());
            // Pulse: Fitbit-style rounded, flat cards
            int pixels_22dp = (int) (22 * scale + 0.5f);
            int pixels_12dp = (int) (12 * scale + 0.5f);
            card.setRadius(pixels_22dp);
            card.setCardElevation(0);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pulse_card));
            card.setContentPadding(pixels_12dp, pixels_12dp, pixels_12dp, pixels_12dp);
            card.setLayoutParams(layoutParams);
            card.addView(fragment);
            gridLayout.addView(card);
        } else {
            fragment.setLayoutParams(layoutParams);
            gridLayout.addView(fragment);
        }
    }

    /**
     * This class serves as a data collection object for all data points used by the various
     * dashboard widgets. Since retrieving this data can be costly, this class makes sure it will
     * only be done once. It will be passed to every widget, making sure they have the necessary
     * data available.
     */
    public static class DashboardData implements Serializable {
        public boolean showAllDevices;
        public Set<String> showDeviceList;
        public int hrIntervalSecs;
        public int timeFrom;
        public int timeTo;
        public final List<GeneralizedActivity> generalizedActivities = Collections.synchronizedList(new ArrayList<>());
        private final CachedValue<Integer> stepsTotal = new CachedValue<>();
        private final CachedValue<Float> stepsGoalFactor = new CachedValue<>();
        private final CachedValue<Integer> restingCaloriesTotal = new CachedValue<>();
        private final CachedValue<Integer> activeCaloriesTotal = new CachedValue<>();
        private final CachedValue<Float> activeCaloriesGoalFactor = new CachedValue<>();
        private final CachedValue<Long> sleepTotalMinutes = new CachedValue<>();
        private final CachedValue<Float> sleepGoalFactor = new CachedValue<>();
        private final CachedValue<Float> distanceTotalMeters = new CachedValue<>();
        private final CachedValue<Float> distanceGoalFactor = new CachedValue<>();
        private final CachedValue<Long> activeMinutesTotal = new CachedValue<>();
        private final CachedValue<Float> activeMinutesGoalFactor = new CachedValue<>();
        private final Map<String, Serializable> genericData = new ConcurrentHashMap<>();

        public void clear() {
            restingCaloriesTotal.clear();
            activeCaloriesTotal.clear();
            activeCaloriesGoalFactor.clear();
            stepsTotal.clear();
            stepsGoalFactor.clear();
            sleepTotalMinutes.clear();
            sleepGoalFactor.clear();
            distanceTotalMeters.clear();
            distanceGoalFactor.clear();
            activeMinutesTotal.clear();
            activeMinutesGoalFactor.clear();
            generalizedActivities.clear();
            genericData.clear();
        }

        public boolean isEmpty() {
            return (stepsTotal.isEmpty() &&
                    stepsGoalFactor.isEmpty() &&
                    restingCaloriesTotal.isEmpty() &&
                    activeCaloriesTotal.isEmpty() &&
                    activeCaloriesGoalFactor.isEmpty() &&
                    sleepTotalMinutes.isEmpty() &&
                    sleepGoalFactor.isEmpty() &&
                    distanceTotalMeters.isEmpty() &&
                    distanceGoalFactor.isEmpty() &&
                    activeMinutesTotal.isEmpty() &&
                    activeMinutesGoalFactor.isEmpty() &&
                    genericData.isEmpty() &&
                    generalizedActivities.isEmpty());
        }

        public int getStepsTotal() {
            return stepsTotal.get(() -> DashboardUtils.getStepsTotal(this));
        }

        public float getStepsGoalFactor() {
            return stepsGoalFactor.get(() -> DashboardUtils.getStepsGoalFactor(this));
        }

        public float getDistanceTotal() {
            return distanceTotalMeters.get(() -> DashboardUtils.getDistanceTotal(this));
        }

        public float getDistanceGoalFactor() {
            return distanceGoalFactor.get(() -> DashboardUtils.getDistanceGoalFactor(this));
        }

        public long getActiveMinutesTotal() {
            return activeMinutesTotal.get(() -> DashboardUtils.getActiveMinutesTotal(this));
        }

        public float getActiveMinutesGoalFactor() {
            return activeMinutesGoalFactor.get(() -> DashboardUtils.getActiveMinutesGoalFactor(this));
        }

        public long getSleepMinutesTotal() {
            return sleepTotalMinutes.get(() -> DashboardUtils.getSleepMinutesTotal(this));
        }

        public float getSleepMinutesGoalFactor() {
            return sleepGoalFactor.get(() -> DashboardUtils.getSleepMinutesGoalFactor(this));
        }

        public int getActiveCaloriesTotal() {
            return activeCaloriesTotal.get(() -> DashboardUtils.getActiveCaloriesTotal(this));
        }

        public int getRestingCaloriesTotal() {
            return restingCaloriesTotal.get(() -> DashboardUtils.getRestingCaloriesTotal(this));
        }

        public float getActiveCaloriesGoalFactor() {
            return activeCaloriesGoalFactor.get(() -> DashboardUtils.getActiveCaloriesGoalFactor(this));
        }

        public void put(final String key, final Serializable value) {
            genericData.put(key, value);
        }

        public Serializable get(final String key) {
            return genericData.get(key);
        }

        /**
         * @noinspection UnusedReturnValue
         */
        public Serializable computeIfAbsent(final String key, final Supplier<Serializable> supplier) {
            return genericData.computeIfAbsent(key, absent -> supplier.get());
        }

        public static class GeneralizedActivity implements Serializable {
            public ActivityKind activityKind;
            public long timeFrom;
            public long timeTo;

            public GeneralizedActivity(ActivityKind activityKind, long timeFrom, long timeTo) {
                this.activityKind = activityKind;
                this.timeFrom = timeFrom;
                this.timeTo = timeTo;
            }

            @NonNull
            @Override
            public String toString() {
                return "Generalized activity: timeFrom=" + timeFrom + ", timeTo=" + timeTo + ", activityKind=" + activityKind + ", calculated duration: " + (timeTo - timeFrom) + " seconds";
            }
        }
    }
}