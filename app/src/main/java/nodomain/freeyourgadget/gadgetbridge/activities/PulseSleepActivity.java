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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;

/** Sleep insights: device sleep score (or a computed fallback), last-night stage
 *  breakdown, a 7-night trend and a one-line takeaway. */
public class PulseSleepActivity extends AbstractGBActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AbstractGBActivity.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_sleep);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pulse_sleep_title);
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
            final SleepData d = new SleepData();
            try (DBHandler db = GBApplication.acquireDB()) {
                final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
                final long now = System.currentTimeMillis();

                // device sleep score (latest)
                for (final GBDevice dev : devices) {
                    try {
                        final SleepScoreSample s = dev.getDeviceCoordinator()
                                .getSleepScoreProvider(dev, db.getDaoSession()).getLatestSample(now);
                        if (s != null) d.score = Math.max(d.score, s.getSleepScore());
                    } catch (final Exception ignored) {
                    }
                }

                // last night's stages: noon-to-noon window around the night
                final Calendar to = GregorianCalendar.getInstance();
                to.set(Calendar.HOUR_OF_DAY, 12);
                to.set(Calendar.MINUTE, 0); to.set(Calendar.SECOND, 0); to.set(Calendar.MILLISECOND, 0);
                if (to.getTimeInMillis() > now) to.setTimeInMillis(now);
                final int toSec = (int) (to.getTimeInMillis() / 1000L);
                final int fromSec = toSec - 24 * 3600;

                SleepAnalysis.SleepSession best = null;
                long bestAsleep = -1;
                List<? extends ActivitySample> bestSamples = null;
                List<SleepAnalysis.SleepSession> bestDeviceSessions = null;
                for (final GBDevice dev : devices) {
                    try {
                        final List<? extends ActivitySample> samples = dev.getDeviceCoordinator()
                                .getSampleProvider(dev, db.getDaoSession()).getAllActivitySamples(fromSec, toSec);
                        final List<SleepAnalysis.SleepSession> sessions = new SleepAnalysis().calculateSleepSessions(samples);
                        for (final SleepAnalysis.SleepSession s : sessions) {
                            final long asleep = s.getLightSleepDuration() + s.getDeepSleepDuration() + s.getRemSleepDuration();
                            if (asleep > bestAsleep) {
                                bestAsleep = asleep;
                                best = s;
                                bestSamples = samples;
                                bestDeviceSessions = sessions;
                            }
                        }
                    } catch (final Exception ignored) {
                    }
                }
                if (best != null) {
                    d.hasNight = true;
                    d.deepMin = best.getDeepSleepDuration() / 60;
                    d.lightMin = best.getLightSleepDuration() / 60;
                    d.remMin = best.getRemSleepDuration() / 60;
                    d.awakeMin = best.getAwakeSleepDuration() / 60;
                    d.bedTime = best.getSleepStart();
                    d.wakeTime = best.getSleepEnd();
                    buildHypnogram(d, bestSamples, best.getSleepStart().getTime() / 1000, best.getSleepEnd().getTime() / 1000);

                    // naps = additional sleep sessions (>= 10 min asleep) besides the main night
                    if (bestDeviceSessions != null) {
                        for (final SleepAnalysis.SleepSession s : bestDeviceSessions) {
                            if (s == best) continue;
                            final long a = (s.getLightSleepDuration() + s.getDeepSleepDuration() + s.getRemSleepDuration()) / 60;
                            if (a >= 10) {
                                d.napCount++;
                                d.napMin += a;
                            }
                        }
                    }
                }

                // 7-night totals (asleep minutes per night)
                final Calendar day = GregorianCalendar.getInstance();
                for (int i = 6; i >= 0; i--) {
                    final Calendar c = (Calendar) day.clone();
                    c.add(Calendar.DAY_OF_MONTH, -i);
                    long mins = 0;
                    for (final GBDevice dev : devices) {
                        mins += DailyTotals.getDailyTotalsForDevice(dev, (Calendar) c.clone(), db).getSleep();
                    }
                    d.week[6 - i] = mins;
                    d.weekLabel[6 - i] = new SimpleDateFormat("EEEEE", Locale.getDefault()).format(c.getTime());
                }
            } catch (final Exception e) {
                return;
            }

            d.goalMin = new ActivityUser().getSleepDurationGoal();
            final long asleep = d.deepMin + d.lightMin + d.remMin;
            // fallback score from duration + stage balance if the device gave none
            if (d.score <= 0 && d.hasNight && d.goalMin > 0) {
                double s = 70.0 * asleep / d.goalMin;                 // duration share
                final long total = asleep + d.awakeMin;
                if (total > 0) s += 30.0 * (d.deepMin + d.remMin) / total; // quality share
                d.score = (int) Math.max(1, Math.min(100, Math.round(s)));
            }

            runOnUiThread(() -> render(d));
        }).start();
    }

    /** Merge the night's samples into colored stage runs for the hypnogram band. */
    private void buildHypnogram(final SleepData d, final List<? extends ActivitySample> samples,
                                final long startSec, final long endSec) {
        if (samples == null) return;
        int lastColor = 0;
        for (int i = 0; i < samples.size(); i++) {
            final ActivitySample s = samples.get(i);
            final long ts = s.getTimestamp();
            if (ts < startSec || ts > endSec) continue;
            final int color = stageColor(s.getKind());
            if (color == 0) continue;
            long dur = (i + 1 < samples.size()) ? (samples.get(i + 1).getTimestamp() - ts) : (endSec - ts);
            if (dur <= 0) dur = 60;
            if (dur > 3600) dur = 3600; // clamp gaps
            if (color == lastColor && !d.hypnoDur.isEmpty()) {
                d.hypnoDur.set(d.hypnoDur.size() - 1, d.hypnoDur.get(d.hypnoDur.size() - 1) + dur);
            } else {
                d.hypnoColor.add(color);
                d.hypnoDur.add(dur);
                lastColor = color;
            }
        }
    }

    private int stageColor(final ActivityKind kind) {
        if (kind == ActivityKind.DEEP_SLEEP) return R.color.pulse_purple;
        if (kind == ActivityKind.LIGHT_SLEEP) return R.color.pulse_neon_cyan;
        if (kind == ActivityKind.REM_SLEEP) return R.color.pulse_neon;
        if (kind == ActivityKind.AWAKE_SLEEP) return R.color.pulse_ring_hr;
        return 0;
    }

    private void render(final SleepData d) {
        if (isFinishing() || isDestroyed()) return;

        if (!d.hasNight && d.score <= 0) {
            findViewById(R.id.sleep_empty).setVisibility(View.VISIBLE);
            findViewById(R.id.sleep_score_block).setVisibility(View.GONE);
            findViewById(R.id.sleep_lastnight_card).setVisibility(View.GONE);
            findViewById(R.id.sleep_insight).setVisibility(View.GONE);
        } else {
            renderScore(d);
            renderNight(d);
        }
        renderWeek(d);
        animateIn((LinearLayout) findViewById(R.id.sleep_root));
    }

    private void renderScore(final SleepData d) {
        final TextView score = findViewById(R.id.sleep_score);
        final TextView word = findViewById(R.id.sleep_score_word);
        final int color; final int wordRes;
        if (d.score >= 85) { color = R.color.pulse_mint; wordRes = R.string.pulse_sleep_q_excellent; }
        else if (d.score >= 70) { color = R.color.pulse_neon_cyan; wordRes = R.string.pulse_sleep_q_good; }
        else if (d.score >= 50) { color = R.color.pulse_ring_cal; wordRes = R.string.pulse_sleep_q_fair; }
        else { color = R.color.pulse_ring_hr; wordRes = R.string.pulse_sleep_q_poor; }
        score.setTextColor(ContextCompat.getColor(this, color));
        word.setTextColor(ContextCompat.getColor(this, color));
        word.setText(wordRes);
        animateCountUp(score, d.score);
    }

    private void renderNight(final SleepData d) {
        final long asleep = d.deepMin + d.lightMin + d.remMin;
        ((TextView) findViewById(R.id.sleep_duration)).setText(hm(asleep));
        final SimpleDateFormat tf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        if (d.bedTime != null && d.wakeTime != null) {
            String times = getString(R.string.pulse_sleep_bed_wake, tf.format(d.bedTime), tf.format(d.wakeTime));
            if (d.napCount > 0) {
                times += "   ·   " + d.napCount + (d.napCount == 1 ? " nap " : " naps ") + hm(d.napMin);
            }
            ((TextView) findViewById(R.id.sleep_times)).setText(times);
        }

        // hypnogram timeline (the night's stages in sequence)
        final LinearLayout hypno = findViewById(R.id.sleep_hypnogram);
        hypno.removeAllViews();
        for (int i = 0; i < d.hypnoColor.size(); i++) {
            final View seg = new View(this);
            seg.setBackgroundColor(ContextCompat.getColor(this, d.hypnoColor.get(i)));
            hypno.addView(seg, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) (long) d.hypnoDur.get(i)));
        }

        // stage bar + legend
        final LinearLayout bar = findViewById(R.id.sleep_stage_bar);
        final LinearLayout legend = findViewById(R.id.sleep_stage_legend);
        bar.removeAllViews(); legend.removeAllViews();
        addStage(bar, legend, R.string.pulse_sleep_deep, R.color.pulse_purple, d.deepMin);
        addStage(bar, legend, R.string.pulse_sleep_light, R.color.pulse_neon_cyan, d.lightMin);
        addStage(bar, legend, R.string.pulse_sleep_rem, R.color.pulse_neon, d.remMin);
        addStage(bar, legend, R.string.pulse_sleep_awake, R.color.pulse_ring_hr, d.awakeMin);

        // insight
        final TextView insight = findViewById(R.id.sleep_insight);
        final long total = asleep + d.awakeMin;
        if (asleep >= d.goalMin && d.goalMin > 0) {
            insight.setText(R.string.pulse_sleep_insight_goal);
        } else if (d.goalMin > 0 && asleep < d.goalMin) {
            insight.setText(getString(R.string.pulse_sleep_insight_under, hm(d.goalMin - asleep), hm(d.goalMin)));
        } else if (total > 0) {
            insight.setText(getString(R.string.pulse_sleep_insight_deep, (int) Math.round(100.0 * d.deepMin / total)));
        }
    }

    private void addStage(final LinearLayout bar, final LinearLayout legend,
                          final int labelRes, final int colorRes, final long mins) {
        if (mins <= 0) return;
        @ColorInt final int color = ContextCompat.getColor(this, colorRes);

        final View seg = new View(this);
        seg.setBackgroundColor(color);
        bar.addView(seg, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) mins));

        final LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        final LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        item.setLayoutParams(ilp);
        final View dot = new View(this);
        dot.setBackgroundColor(color);
        item.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));
        final TextView t = new TextView(this);
        t.setText(getString(labelRes) + "  " + hm(mins));
        t.setTextColor(ContextCompat.getColor(this, R.color.pulse_text_dim));
        t.setTextSize(11);
        t.setPadding(dp(5), 0, 0, 0);
        item.addView(t);
        legend.addView(item);
    }

    private void renderWeek(final SleepData d) {
        final LinearLayout bars = findViewById(R.id.sleep_week_bars);
        bars.removeAllViews();
        long max = 1, sum = 0; int nights = 0;
        for (final long m : d.week) { if (m > max) max = m; if (m > 0) { sum += m; nights++; } }
        ((TextView) findViewById(R.id.sleep_avg)).setText(
                nights > 0 ? getString(R.string.pulse_sleep_avg, hm(sum / nights)) : "");

        for (int i = 0; i < 7; i++) {
            final LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER | Gravity.BOTTOM);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

            // Thick rounded pills, matching the Health page mini charts (empty = a clean dot)
            final int barW = dp(32);
            final long v = d.week[i];
            final android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
            pill.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            pill.setCornerRadius(barW / 2f);
            pill.setColor(ContextCompat.getColor(this, R.color.pulse_purple));
            final View bar = new View(this);
            bar.setBackground(pill);
            bar.setAlpha(v > 0 ? 0.9f : 0.25f);
            final int h = v > 0 ? Math.max(barW, (int) (dp(96) * v / (double) max)) : barW;
            final LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(barW, h);
            blp.gravity = Gravity.CENTER_HORIZONTAL;
            col.addView(bar, blp);

            final TextView lbl = new TextView(this);
            lbl.setText(d.weekLabel[i]);
            lbl.setTextColor(ContextCompat.getColor(this, R.color.pulse_text_dim));
            lbl.setTextSize(11);
            lbl.setGravity(Gravity.CENTER);
            lbl.setPadding(0, dp(6), 0, 0);
            col.addView(lbl);
            bars.addView(col);
        }
    }

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

    private void animateCountUp(final TextView view, final int target) {
        final ValueAnimator a = ValueAnimator.ofInt(0, target);
        a.setDuration(900);
        a.setStartDelay(150);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(anim -> view.setText(String.valueOf((int) anim.getAnimatedValue())));
        view.setText("0");
        a.start();
    }

    private String hm(final long mins) {
        final long h = mins / 60, m = mins % 60;
        return h > 0 ? String.format(Locale.getDefault(), "%dh %dm", h, m)
                     : String.format(Locale.getDefault(), "%dm", m);
    }

    private int dp(final int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    private static final class SleepData {
        int score;
        boolean hasNight;
        long deepMin, lightMin, remMin, awakeMin, goalMin, napMin;
        int napCount;
        Date bedTime, wakeTime;
        final long[] week = new long[7];
        final String[] weekLabel = new String[7];
        final java.util.List<Integer> hypnoColor = new java.util.ArrayList<>();
        final java.util.List<Long> hypnoDur = new java.util.ArrayList<>();
    }
}
