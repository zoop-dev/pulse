/*  Copyright (C) 2024 Pulse

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/** Pulse streak calendar: a month grid highlighting every day you hit your goal. */
public class PulseStreakActivity extends AbstractGBActivity {

    private static final String[] METRICS = {"steps", "activetime", "sleep", "calories", "distance"};

    private final Calendar shownMonth = GregorianCalendar.getInstance();
    private GridLayout grid;
    private TextView monthLabel;
    private TextView metricValue;
    private TextView streakCount;
    private String goalMetric;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AbstractGBActivity.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_streak);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pulse_streak_page_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final Prefs prefs = GBApplication.getPrefs();
        goalMetric = prefs.getString("pulse_streak_goal", "steps");

        grid = findViewById(R.id.streak_grid);
        monthLabel = findViewById(R.id.streak_month);
        metricValue = findViewById(R.id.streak_metric_value);
        streakCount = findViewById(R.id.streak_count);

        shownMonth.set(Calendar.DAY_OF_MONTH, 1);

        findViewById(R.id.streak_prev).setOnClickListener(v -> {
            shownMonth.add(Calendar.MONTH, -1);
            renderMonth();
        });
        findViewById(R.id.streak_next).setOnClickListener(v -> {
            shownMonth.add(Calendar.MONTH, 1);
            renderMonth();
        });
        findViewById(R.id.streak_metric_row).setOnClickListener(v -> showMetricPicker());

        buildWeekdayHeader();
        updateStreakHeadline();
        updateMetricLabel();
        renderMonth();
        computeLifetime();
    }

    /** Sum every day on record across all devices into all-time totals. */
    private void computeLifetime() {
        new Thread(() -> {
            long steps = 0, distanceCm = 0, calRaw = 0, sleepMin = 0;
            int daysTracked = 0;
            long maxDaySteps = 0;
            try (nodomain.freeyourgadget.gadgetbridge.database.DBHandler db = GBApplication.acquireDB()) {
                final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
                final int stride = new nodomain.freeyourgadget.gadgetbridge.model.ActivityUser().getStepLengthCm();

                // Earliest sample timestamp across all devices
                int earliest = Integer.MAX_VALUE;
                for (final GBDevice dev : devices) {
                    try {
                        final nodomain.freeyourgadget.gadgetbridge.model.ActivitySample first =
                                dev.getDeviceCoordinator().getSampleProvider(dev, db.getDaoSession()).getFirstActivitySample();
                        if (first != null) earliest = Math.min(earliest, first.getTimestamp());
                    } catch (final Exception ignored) {
                    }
                }
                if (earliest != Integer.MAX_VALUE) {
                    final Calendar cur = GregorianCalendar.getInstance();
                    cur.setTimeInMillis(earliest * 1000L);
                    cur.set(Calendar.HOUR_OF_DAY, 12);
                    cur.set(Calendar.MINUTE, 0);
                    cur.set(Calendar.SECOND, 0);
                    final Calendar today = GregorianCalendar.getInstance();
                    while (!cur.after(today)) {
                        long daySteps = 0;
                        for (final GBDevice dev : devices) {
                            final nodomain.freeyourgadget.gadgetbridge.model.DailyTotals dt =
                                    nodomain.freeyourgadget.gadgetbridge.model.DailyTotals
                                            .getDailyTotalsForDevice(dev, (Calendar) cur.clone(), db);
                            daySteps += dt.getSteps();
                            long distCm = dt.getDistance();
                            if (distCm <= 0 && dt.getSteps() > 0) distCm = dt.getSteps() * (long) stride;
                            distanceCm += distCm;
                            calRaw += dt.getActiveCalories();
                            sleepMin += dt.getSleep();
                        }
                        steps += daySteps;
                        if (daySteps > maxDaySteps) maxDaySteps = daySteps;
                        if (daySteps > 0) daysTracked++;
                        cur.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
            } catch (final Exception e) {
                return;
            }

            final java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
            final String stepsStr = nf.format(steps);
            final String distStr = nodomain.freeyourgadget.gadgetbridge.util.FormatUtils
                    .getFormattedDistanceLabel(distanceCm * 0.01);
            final String calStr = nf.format(calRaw / 1000);
            final String sleepStr = getString(R.string.pulse_lifetime_hours, nf.format(sleepMin / 60));
            final String daysStr = getString(R.string.pulse_lifetime_days, nf.format(daysTracked));

            final long lifetimeSteps = steps;
            final double lifetimeMeters = distanceCm * 0.01;
            final long bestDaySteps = maxDaySteps;
            final int tracked = daysTracked;

            runOnUiThread(() -> {
                ((TextView) findViewById(R.id.life_steps)).setText(stepsStr);
                ((TextView) findViewById(R.id.life_distance)).setText(distStr);
                ((TextView) findViewById(R.id.life_calories)).setText(calStr);
                ((TextView) findViewById(R.id.life_sleep)).setText(sleepStr);
                ((TextView) findViewById(R.id.life_days)).setText(daysStr);
                renderAchievements(lifetimeSteps, lifetimeMeters, bestDaySteps, tracked);
            });
        }).start();
    }

    /** Render the achievement badge grid (unlocked = bright/neon, locked = dimmed). */
    private void renderAchievements(final long lifetimeSteps, final double lifetimeMeters,
                                    final long bestDaySteps, final int daysTracked) {
        final android.widget.GridLayout grid = findViewById(R.id.achievements_grid);
        if (grid == null) return;
        grid.removeAllViews();

        final nodomain.freeyourgadget.gadgetbridge.util.Prefs prefs = GBApplication.getPrefs();
        final int bestStreak = Math.max(prefs.getInt("pulse_streak_best", 0),
                prefs.getInt("pulse_streak_count", 0));

        // {iconRes, titleRes, unlocked}
        final int[][] defs = {
                {R.drawable.ic_star_gold,      R.string.pulse_ach_first_week,    daysTracked >= 7 ? 1 : 0},
                {R.drawable.ic_star_gold,      R.string.pulse_ach_first_month,   daysTracked >= 30 ? 1 : 0},
                {R.drawable.ic_bolt,           R.string.pulse_ach_week_streak,   bestStreak >= 7 ? 1 : 0},
                {R.drawable.ic_events,         R.string.pulse_ach_month_streak,  bestStreak >= 30 ? 1 : 0},
                {R.drawable.ic_events_gold,    R.string.pulse_ach_100day_streak, bestStreak >= 100 ? 1 : 0},
                {R.drawable.ic_steps,          R.string.pulse_ach_10k,           bestDaySteps >= 10000 ? 1 : 0},
                {R.drawable.ic_steps,          R.string.pulse_ach_20k,           bestDaySteps >= 20000 ? 1 : 0},
                {R.drawable.ic_bolt,           R.string.pulse_ach_30k,           bestDaySteps >= 30000 ? 1 : 0},
                {R.drawable.ic_verified,       R.string.pulse_ach_100k,          lifetimeSteps >= 100000 ? 1 : 0},
                {R.drawable.ic_verified,       R.string.pulse_ach_500k,          lifetimeSteps >= 500000 ? 1 : 0},
                {R.drawable.ic_events_gold,    R.string.pulse_ach_million,       lifetimeSteps >= 1000000 ? 1 : 0},
                {R.drawable.ic_events_gold,    R.string.pulse_ach_5m,            lifetimeSteps >= 5000000 ? 1 : 0},
                {R.drawable.ic_distance,       R.string.pulse_ach_100mi,         lifetimeMeters >= 160934 ? 1 : 0},
                {R.drawable.ic_distance_total, R.string.pulse_ach_500mi,         lifetimeMeters >= 804672 ? 1 : 0},
        };

        for (final int[] def : defs) {
            grid.addView(buildBadge(def[0], def[1], def[2] == 1));
        }
    }

    private android.view.View buildBadge(final int iconRes, final int titleRes, final boolean unlocked) {
        final android.widget.LinearLayout cell = new android.widget.LinearLayout(this);
        cell.setOrientation(android.widget.LinearLayout.VERTICAL);
        cell.setGravity(android.view.Gravity.CENTER);
        cell.setBackgroundResource(R.drawable.pulse_widget_bg);
        cell.setPadding(dp(16), dp(18), dp(16), dp(18));

        final android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
        lp.width = 0;
        lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        cell.setLayoutParams(lp);

        final android.widget.ImageView icon = new android.widget.ImageView(this);
        final android.widget.LinearLayout.LayoutParams iconLp =
                new android.widget.LinearLayout.LayoutParams(dp(32), dp(32));
        icon.setLayoutParams(iconLp);
        icon.setImageResource(iconRes);
        if (!unlocked) {
            icon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.pulse_text_dim));
        }

        final TextView label = new TextView(this);
        label.setText(titleRes);
        label.setGravity(android.view.Gravity.CENTER);
        label.setTextSize(12);
        label.setPadding(0, dp(8), 0, 0);
        label.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                unlocked ? R.color.pulse_text : R.color.pulse_text_dim));

        cell.addView(icon);
        cell.addView(label);
        cell.setAlpha(unlocked ? 1f : 0.45f);
        if (unlocked) {
            cell.setOnClickListener(v -> shareBadge(iconRes, titleRes));
        }
        return cell;
    }

    /** Render an unlocked badge into a Pulse card image and fire the share sheet. */
    private void shareBadge(final int iconRes, final int titleRes) {
        final View card = getLayoutInflater().inflate(R.layout.pulse_badge_share_card, null);
        final android.widget.ImageView icon = card.findViewById(R.id.badge_share_icon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(GBApplication.getAccentColor(this));
        ((TextView) card.findViewById(R.id.badge_share_title)).setText(titleRes);

        final int width = 1080;
        card.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        card.layout(0, 0, card.getMeasuredWidth(), card.getMeasuredHeight());
        final android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                card.getMeasuredWidth(), card.getMeasuredHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        card.draw(new android.graphics.Canvas(bmp));

        try {
            final java.io.File dir = new java.io.File(getCacheDir(), "images");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            final java.io.File out = new java.io.File(dir, "pulse_badge.png");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
            }
            AndroidUtils.shareFile(this, out, "image/png");
        } catch (final java.io.IOException e) {
            nodomain.freeyourgadget.gadgetbridge.util.GB.toast(this,
                    getString(R.string.activity_error_share_failed),
                    android.widget.Toast.LENGTH_LONG,
                    nodomain.freeyourgadget.gadgetbridge.util.GB.ERROR, e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int dp(final int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    private void updateStreakHeadline() {
        final Prefs prefs = GBApplication.getPrefs();
        final int count = prefs.getInt("pulse_streak_count", 0);
        final String last = prefs.getString("pulse_streak_last", "");
        final int shown = (dateStr(0).equals(last) || dateStr(-1).equals(last)) ? count : 0;
        streakCount.setText(String.valueOf(shown));
    }

    private void updateMetricLabel() {
        metricValue.setText(metricLabel(goalMetric));
    }

    private String metricLabel(final String metric) {
        switch (metric) {
            case "any": return getString(R.string.pulse_streak_any_goal);
            case "activetime": return getString(R.string.activity_list_summary_active_time);
            case "sleep": return getString(R.string.menuitem_sleep);
            case "calories": return getString(R.string.calories);
            case "distance": return getString(R.string.distance);
            default: return getString(R.string.steps);
        }
    }

    private void showMetricPicker() {
        // "Any goal" first, then the individual metrics.
        final String[] options = new String[METRICS.length + 1];
        options[0] = "any";
        System.arraycopy(METRICS, 0, options, 1, METRICS.length);

        final String[] labels = new String[options.length];
        int sel = 0;
        for (int i = 0; i < options.length; i++) {
            labels[i] = metricLabel(options[i]);
            if (options[i].equals(goalMetric)) sel = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pulse_streak_goal_label)
                .setSingleChoiceItems(labels, sel, (d, which) -> {
                    goalMetric = options[which];
                    GBApplication.getPrefs().getPreferences().edit()
                            .putString("pulse_streak_goal", goalMetric).apply();
                    d.dismiss();
                    updateMetricLabel();
                    renderMonth();
                })
                .show();
    }

    private void buildWeekdayHeader() {
        final LinearLayout header = findViewById(R.id.streak_weekdays);
        header.removeAllViews();
        final String[] days = {"S", "M", "T", "W", "T", "F", "S"};
        for (final String d : days) {
            final TextView tv = new TextView(this);
            final LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setText(d);
            tv.setTextColor(ContextCompat.getColor(this, R.color.pulse_text_dim));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            header.addView(tv);
        }
    }

    private void renderMonth() {
        monthLabel.setText(new java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(shownMonth.getTime()));

        grid.removeAllViews();
        final Calendar month = (Calendar) shownMonth.clone();
        month.set(Calendar.DAY_OF_MONTH, 1);
        // Sunday-first offset
        final int firstWeekday = month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        final int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);

        final TextView[] cells = new TextView[daysInMonth + 1];
        final Calendar today = GregorianCalendar.getInstance();
        final boolean isCurrentMonth = today.get(Calendar.YEAR) == month.get(Calendar.YEAR)
                && today.get(Calendar.MONTH) == month.get(Calendar.MONTH);
        final int todayDay = today.get(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstWeekday; i++) {
            grid.addView(emptyCell());
        }
        for (int d = 1; d <= daysInMonth; d++) {
            final FrameLayout cell = new FrameLayout(this);
            final GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(46);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            cell.setLayoutParams(lp);

            final TextView tv = new TextView(this);
            final FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(dp(38), dp(38));
            flp.gravity = Gravity.CENTER;
            tv.setLayoutParams(flp);
            tv.setGravity(Gravity.CENTER);
            tv.setText(String.valueOf(d));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);

            final boolean future = isCurrentMonth && d > todayDay;
            tv.setTextColor(ContextCompat.getColor(this,
                    future ? R.color.pulse_card_alt : R.color.pulse_text));
            if (isCurrentMonth && d == todayDay) {
                tv.setBackgroundResource(R.drawable.pulse_streak_today);
            }
            cell.addView(tv);
            grid.addView(cell);
            cells[d] = tv;
        }

        // Overlay the recorded streak run (source of truth — device-agnostic, instant).
        final Set<String> runKeys = streakRunDayKeys();
        if (!runKeys.isEmpty()) {
            final java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            for (int d = 1; d <= daysInMonth; d++) {
                final Calendar c = (Calendar) month.clone();
                c.set(Calendar.DAY_OF_MONTH, d);
                if (runKeys.contains(fmt.format(c.getTime()))) {
                    markHit(cells[d], isCurrentMonth && d == todayDay);
                }
            }
        }

        computeAchievements(month, daysInMonth, isCurrentMonth, todayDay, cells);
    }

    /** The consecutive days covered by the current recorded streak. */
    private Set<String> streakRunDayKeys() {
        final Prefs prefs = GBApplication.getPrefs();
        final String last = prefs.getString("pulse_streak_last", "");
        final int count = prefs.getInt("pulse_streak_count", 0);
        final Set<String> keys = new HashSet<>();
        if (last.isEmpty() || count <= 0) return keys;
        try {
            final java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            final Calendar c = GregorianCalendar.getInstance();
            c.setTime(fmt.parse(last));
            for (int i = 0; i < count; i++) {
                keys.add(fmt.format(c.getTime()));
                c.add(Calendar.DAY_OF_MONTH, -1);
            }
        } catch (final Exception ignored) {
        }
        return keys;
    }

    private void markHit(final TextView tv, final boolean isToday) {
        if (tv == null) return;
        tv.setBackgroundResource(isToday ? R.drawable.pulse_streak_today : R.drawable.pulse_streak_hit);
        tv.setTextColor(ContextCompat.getColor(this, isToday ? R.color.pulse_neon : R.color.pulse_bg));
    }

    private FrameLayout emptyCell() {
        final FrameLayout cell = new FrameLayout(this);
        final GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(46);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        cell.setLayoutParams(lp);
        return cell;
    }

    /** Compute which days hit the goal off the UI thread, then highlight them. */
    private void computeAchievements(final Calendar month, final int daysInMonth,
                                     final boolean isCurrentMonth, final int todayDay,
                                     final TextView[] cells) {
        final String metric = goalMetric;
        new Thread(() -> {
            final Set<Integer> achieved = new HashSet<>();
            final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            final int lastDay = isCurrentMonth ? todayDay : daysInMonth;
            for (int d = 1; d <= lastDay; d++) {
                final Calendar start = (Calendar) month.clone();
                start.set(Calendar.DAY_OF_MONTH, d);
                start.set(Calendar.HOUR_OF_DAY, 0);
                start.set(Calendar.MINUTE, 0);
                start.set(Calendar.SECOND, 0);
                final Calendar end = (Calendar) start.clone();
                end.add(Calendar.DAY_OF_MONTH, 1);
                end.add(Calendar.SECOND, -1);

                final int timeFrom = (int) (start.getTimeInMillis() / 1000);
                final int timeTo = (int) (end.getTimeInMillis() / 1000);

                // A day counts as hit if ANY single device met the goal that day
                // (the streak may have been earned on a different watch).
                try {
                    if (devices.isEmpty()) {
                        final DashboardFragment.DashboardData dd = new DashboardFragment.DashboardData();
                        dd.showAllDevices = true;
                        dd.showDeviceList = new HashSet<>();
                        dd.timeFrom = timeFrom;
                        dd.timeTo = timeTo;
                        if (metGoal(metric, dd)) achieved.add(d);
                    } else {
                        for (final GBDevice dev : devices) {
                            final DashboardFragment.DashboardData dd = new DashboardFragment.DashboardData();
                            dd.showAllDevices = false;
                            dd.showDeviceList = java.util.Collections.singleton(dev.getAddress());
                            dd.timeFrom = timeFrom;
                            dd.timeTo = timeTo;
                            if (metGoal(metric, dd)) {
                                achieved.add(d);
                                break;
                            }
                        }
                    }
                } catch (final Exception ignored) {
                    // skip days that fail to load
                }
            }
            runOnUiThread(() -> {
                if (!metric.equals(goalMetric)) return; // metric changed while computing
                for (final Integer d : achieved) {
                    if (d < cells.length) {
                        markHit(cells[d], isCurrentMonth && d == todayDay);
                    }
                }
            });
        }).start();
    }

    /** Whether the given goal (or, for "any", at least one goal) was met for this data window. */
    private boolean metGoal(final String metric, final DashboardFragment.DashboardData dd) {
        if ("any".equals(metric)) {
            for (final String m : METRICS) {
                if (factor(m, dd) >= 1f) return true;
            }
            return false;
        }
        return factor(metric, dd) >= 1f;
    }

    private float factor(final String metric, final DashboardFragment.DashboardData dd) {
        switch (metric) {
            case "activetime": return dd.getActiveMinutesGoalFactor();
            case "sleep": return dd.getSleepMinutesGoalFactor();
            case "calories": return dd.getActiveCaloriesGoalFactor();
            case "distance": return dd.getDistanceGoalFactor();
            default: return dd.getStepsGoalFactor();
        }
    }

    private static String dateStr(final int dayOffset) {
        final Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, dayOffset);
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void setLanguage(final Locale language, final boolean invalidateLanguage) {
        AndroidUtils.setLanguage(this, language);
    }
}
