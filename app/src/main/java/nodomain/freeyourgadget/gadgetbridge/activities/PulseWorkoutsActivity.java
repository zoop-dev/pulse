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

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutDetailsActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

/** Pulse: a clean list of recorded workouts; tap one to open its full details. */
public class PulseWorkoutsActivity extends AbstractGBActivity {

    private LinearLayout list;
    private TextView empty;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AbstractGBActivity.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_workouts);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pulse_workouts_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        list = findViewById(R.id.workouts_list);
        empty = findViewById(R.id.workouts_empty);
        load();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void load() {
        new Thread(() -> {
            final List<BaseActivitySummary> summaries = new ArrayList<>();
            try (DBHandler db = GBApplication.acquireDB()) {
                summaries.addAll(db.getDaoSession().getBaseActivitySummaryDao().queryBuilder()
                        .orderDesc(BaseActivitySummaryDao.Properties.StartTime)
                        .list());
            } catch (final Exception ignored) {
                // leave empty
            }

            // A device is needed to open the (reused) details screen.
            final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            final GBDevice device = devices.isEmpty() ? null : devices.get(0);

            runOnUiThread(() -> {
                if (summaries.isEmpty()) {
                    empty.setVisibility(View.VISIBLE);
                    return;
                }
                empty.setVisibility(View.GONE);
                list.removeAllViews();
                for (final BaseActivitySummary s : summaries) {
                    list.addView(buildCard(s, device));
                }
            });
        }).start();
    }

    private View buildCard(final BaseActivitySummary summary, final GBDevice device) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.pulse_widget_bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        final LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);

        final ActivityKind kind = ActivityKind.fromCode(summary.getActivityKind());

        final ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));
        icon.setImageResource(kind.getIcon());
        icon.setColorFilter(nodomain.freeyourgadget.gadgetbridge.GBApplication.getAccentColor(this));

        final LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.leftMargin = dp(16);
        text.setLayoutParams(textLp);

        final TextView title = new TextView(this);
        final String name = summary.getName();
        title.setText(name != null && !name.trim().isEmpty() ? name : kind.getLabel(this));
        title.setTextColor(ContextCompat.getColor(this, R.color.pulse_text));
        title.setTextSize(16);

        final TextView sub = new TextView(this);
        sub.setText(subtitle(summary));
        sub.setTextColor(ContextCompat.getColor(this, R.color.pulse_text_dim));
        sub.setTextSize(13);
        sub.setPadding(0, dp(2), 0, 0);

        text.addView(title);
        text.addView(sub);
        card.addView(icon);
        card.addView(text);

        if (device != null) {
            card.setOnClickListener(v -> openDetails(summary, device));
        }
        return card;
    }

    /** "Mon, Jun 22 · 32m" */
    private String subtitle(final BaseActivitySummary summary) {
        final Date start = summary.getStartTime();
        final String date = start != null
                ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(start) : "";
        final Date end = summary.getEndTime();
        if (start != null && end != null) {
            final long secs = (end.getTime() - start.getTime()) / 1000L;
            return date + " · " + formatDuration(secs);
        }
        return date;
    }

    private String formatDuration(final long totalSeconds) {
        final long h = totalSeconds / 3600;
        final long m = (totalSeconds % 3600) / 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", h, m);
        }
        return String.format(Locale.getDefault(), "%dm", m);
    }

    private void openDetails(final BaseActivitySummary summary, final GBDevice device) {
        final Intent intent = new Intent(this, WorkoutDetailsActivity.class);
        intent.putExtra(GBDevice.EXTRA_DEVICE, device);
        intent.putExtra("position", 0);
        intent.putExtra("itemsFilter", new ArrayList<>(Collections.singletonList(summary.getId())));
        startActivity(intent);
    }

    private int dp(final int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }
}
