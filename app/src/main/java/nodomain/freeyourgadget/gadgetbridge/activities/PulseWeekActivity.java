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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/** Pulse: an animated "Week in Review" — weekly stats, an adaptive challenge,
 *  and all-time personal records (flagged when set this week). */
public class PulseWeekActivity extends AbstractGBActivity {

    private final NumberFormat nf = NumberFormat.getIntegerInstance();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AbstractGBActivity.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_week);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pulse_week_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        compute();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void compute() {
        new Thread(() -> {
            final WeekData d = new WeekData();
            try (DBHandler db = GBApplication.acquireDB()) {
                final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();

                // week boundaries (Mon 00:00 .. now), and last week
                final Calendar weekStart = GregorianCalendar.getInstance();
                weekStart.set(Calendar.HOUR_OF_DAY, 0);
                weekStart.set(Calendar.MINUTE, 0);
                weekStart.set(Calendar.SECOND, 0);
                weekStart.set(Calendar.MILLISECOND, 0);
                while (weekStart.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    weekStart.add(Calendar.DAY_OF_MONTH, -1);
                }
                final long weekStartMs = weekStart.getTimeInMillis();
                final long lastWeekStartMs = weekStartMs - 7L * 86400000L;

                // earliest sample across devices
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
                    final int stride = new ActivityUser().getStepLengthCm();
                    final Calendar cur = GregorianCalendar.getInstance();
                    cur.setTimeInMillis(earliest * 1000L);
                    cur.set(Calendar.HOUR_OF_DAY, 12);
                    cur.set(Calendar.MINUTE, 0);
                    cur.set(Calendar.SECOND, 0);
                    final Calendar today = GregorianCalendar.getInstance();

                    while (!cur.after(today)) {
                        long daySteps = 0, dayDistCm = 0, dayCalRaw = 0, daySleep = 0;
                        for (final GBDevice dev : devices) {
                            final DailyTotals dt = DailyTotals.getDailyTotalsForDevice(dev, (Calendar) cur.clone(), db);
                            daySteps += dt.getSteps();
                            long distCm = dt.getDistance();
                            if (distCm <= 0 && dt.getSteps() > 0) distCm = dt.getSteps() * (long) stride;
                            dayDistCm += distCm;
                            dayCalRaw += dt.getActiveCalories();
                            daySleep += dt.getSleep();
                        }
                        final long dayCal = dayCalRaw / 1000;
                        final long dayMs = cur.getTimeInMillis();
                        final boolean inWeek = dayMs >= weekStartMs;

                        // all-time per-day records (note if set this week)
                        if (daySteps > d.prSteps) { d.prSteps = daySteps; d.prStepsNew = inWeek; }
                        if (dayDistCm > d.prDistCm) { d.prDistCm = dayDistCm; d.prDistNew = inWeek; }
                        if (dayCal > d.prCal) { d.prCal = dayCal; d.prCalNew = inWeek; }

                        if (inWeek) {
                            d.weekSteps += daySteps;
                            d.weekDistCm += dayDistCm;
                            d.weekCal += dayCal;
                            d.weekSleep += daySleep;
                            if (daySleep > 0) d.sleepDays++;
                            if (daySteps > 0) d.activeDays++;
                            if (daySteps > d.bestDaySteps) d.bestDaySteps = daySteps;
                            d.daysElapsed++;
                        } else if (dayMs >= lastWeekStartMs) {
                            d.lastWeekSteps += daySteps;
                        }
                        cur.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }

                // longest workout (all time)
                final List<BaseActivitySummary> summaries = db.getDaoSession().getBaseActivitySummaryDao()
                        .queryBuilder().list();
                for (final BaseActivitySummary s : summaries) {
                    if (s.getStartTime() != null && s.getEndTime() != null) {
                        final long sec = (s.getEndTime().getTime() - s.getStartTime().getTime()) / 1000L;
                        if (sec > d.prWorkoutSec) {
                            d.prWorkoutSec = sec;
                            d.prWorkoutNew = s.getStartTime().getTime() >= weekStartMs;
                        }
                    }
                }
            } catch (final Exception e) {
                return;
            }

            final Prefs prefs = GBApplication.getPrefs();
            d.prStreak = Math.max(prefs.getInt("pulse_streak_best", 0), prefs.getInt("pulse_streak_count", 0));

            // adaptive challenge target
            final int dailyGoal = new ActivityUser().getStepsGoal();
            final long floor = (long) dailyGoal * 7L;
            final long adaptive = Math.round(d.lastWeekSteps * 1.1);
            d.target = Math.max(floor, adaptive);
            if (d.target <= 0) d.target = 70000;

            runOnUiThread(() -> render(d));
        }).start();
    }

    private void render(final WeekData d) {
        if (isFinishing() || isDestroyed()) return;

        // week range label
        final Calendar ws = GregorianCalendar.getInstance();
        ws.set(Calendar.HOUR_OF_DAY, 0); ws.set(Calendar.MINUTE, 0); ws.set(Calendar.SECOND, 0);
        while (ws.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) ws.add(Calendar.DAY_OF_MONTH, -1);
        final DateFormat md = new java.text.SimpleDateFormat("MMM d", Locale.getDefault());
        ((TextView) findViewById(R.id.week_range)).setText(
                md.format(ws.getTime()) + " – " + md.format(new Date()));

        // challenge
        final boolean done = d.weekSteps >= d.target;
        ((TextView) findViewById(R.id.week_challenge_status)).setText(done
                ? getString(R.string.pulse_week_challenge_done)
                : nf.format(d.target) + " steps");
        ((TextView) findViewById(R.id.week_challenge_caption)).setText(
                getString(R.string.pulse_week_challenge_progress, nf.format(d.weekSteps), nf.format(d.target)));

        // delta vs last week
        final TextView delta = findViewById(R.id.week_delta);
        if (d.lastWeekSteps > 0) {
            final int pct = (int) Math.round((d.weekSteps - d.lastWeekSteps) * 100.0 / d.lastWeekSteps);
            if (pct > 0) {
                delta.setText(getString(R.string.pulse_week_vs_up, pct));
                delta.setTextColor(ContextCompat.getColor(this, R.color.pulse_mint));
            } else if (pct < 0) {
                delta.setText(getString(R.string.pulse_week_vs_down, -pct));
                delta.setTextColor(ContextCompat.getColor(this, R.color.pulse_text_dim));
            } else {
                delta.setText(getString(R.string.pulse_week_vs_same));
                delta.setTextColor(ContextCompat.getColor(this, R.color.pulse_text_dim));
            }
        } else {
            delta.setVisibility(View.GONE);
        }

        // stat cells
        ((TextView) findViewById(R.id.week_distance)).setText(FormatUtils.getFormattedDistanceLabel(d.weekDistCm * 0.01));
        ((TextView) findViewById(R.id.week_calories)).setText(nf.format(d.weekCal));
        ((TextView) findViewById(R.id.week_sleep)).setText(
                d.sleepDays > 0 ? (d.weekSleep / d.sleepDays / 60) + "h" : "—");
        ((TextView) findViewById(R.id.week_activedays)).setText(d.activeDays + " / 7");
        ((TextView) findViewById(R.id.week_bestday)).setText(nf.format(d.bestDaySteps));
        ((TextView) findViewById(R.id.week_avgday)).setText(
                nf.format(d.daysElapsed > 0 ? d.weekSteps / d.daysElapsed : 0));

        // personal records
        final LinearLayout prs = findViewById(R.id.week_prs);
        prs.removeAllViews();
        prs.addView(prRow(getString(R.string.pulse_pr_steps), nf.format(d.prSteps), d.prStepsNew));
        prs.addView(prRow(getString(R.string.pulse_pr_distance), FormatUtils.getFormattedDistanceLabel(d.prDistCm * 0.01), d.prDistNew));
        prs.addView(prRow(getString(R.string.pulse_pr_calories), nf.format(d.prCal), d.prCalNew));
        if (d.prStreak > 0) {
            prs.addView(prRow(getString(R.string.pulse_pr_streak), getString(R.string.pulse_pr_streak_days, d.prStreak), false));
        }
        if (d.prWorkoutSec > 0) {
            prs.addView(prRow(getString(R.string.pulse_pr_workout), formatDuration(d.prWorkoutSec), d.prWorkoutNew));
        }

        animateIn((LinearLayout) findViewById(R.id.week_root));
        animateCountUp((TextView) findViewById(R.id.week_steps), d.weekSteps);
        animateChallengeBar((float) Math.min(1.0, d.target > 0 ? (double) d.weekSteps / d.target : 0));
    }

    private View prRow(final String label, final String value, final boolean isNew) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.pulse_widget_bg);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);

        final LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(ContextCompat.getColor(this, R.color.pulse_text_dim));
        lbl.setTextSize(13);
        left.addView(lbl);
        if (isNew) {
            final TextView tag = new TextView(this);
            tag.setText(R.string.pulse_pr_new);
            tag.setTextColor(nodomain.freeyourgadget.gadgetbridge.GBApplication.getAccentColor(this));
            tag.setTextSize(10);
            tag.setLetterSpacing(0.08f);
            tag.setPadding(0, dp(2), 0, 0);
            left.addView(tag);
        }
        row.addView(left);

        final TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(isNew ? GBApplication.getAccentColor(this) : ContextCompat.getColor(this, R.color.pulse_text));
        val.setTextSize(18);
        val.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.unbounded));
        row.addView(val);
        return row;
    }

    // ---- animations ----

    private void animateIn(final LinearLayout root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            final View child = root.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(40));
            child.animate().alpha(1f).translationY(0f)
                    .setStartDelay(i * 70L).setDuration(380)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void animateCountUp(final TextView view, final long target) {
        final ValueAnimator a = ValueAnimator.ofInt(0, (int) target);
        a.setDuration(900);
        a.setStartDelay(150);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(anim -> view.setText(nf.format((int) anim.getAnimatedValue())));
        view.setText(nf.format(0));
        a.start();
    }

    private void animateChallengeBar(final float progress) {
        final View fill = findViewById(R.id.week_challenge_fill);
        final View gap = findViewById(R.id.week_challenge_gap);
        final ValueAnimator a = ValueAnimator.ofFloat(0f, progress);
        a.setDuration(800);
        a.setStartDelay(300);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(anim -> {
            final float p = (float) anim.getAnimatedValue();
            ((LinearLayout.LayoutParams) fill.getLayoutParams()).weight = p;
            ((LinearLayout.LayoutParams) gap.getLayoutParams()).weight = 1f - p;
            fill.requestLayout();
        });
        a.start();
    }

    private String formatDuration(final long totalSeconds) {
        final long h = totalSeconds / 3600;
        final long m = (totalSeconds % 3600) / 60;
        return h > 0 ? String.format(Locale.getDefault(), "%dh %dm", h, m)
                     : String.format(Locale.getDefault(), "%dm", m);
    }

    private int dp(final int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    /** Computed week + records, passed from the worker thread to the UI. */
    private static final class WeekData {
        long weekSteps, weekDistCm, weekCal, weekSleep, lastWeekSteps, bestDaySteps, target;
        int activeDays, sleepDays, daysElapsed;
        long prSteps, prDistCm, prCal, prWorkoutSec;
        int prStreak;
        boolean prStepsNew, prDistNew, prCalNew, prWorkoutNew;
    }
}
