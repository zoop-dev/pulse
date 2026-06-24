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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

/** Pulse: drag-and-drop editor for the Today metric cards (top 3 = hero, rest = grid). */
public class PulseDashboardEditActivity extends AbstractGBActivity {
    private final List<String> metrics = new ArrayList<>();
    private final Set<String> enabled = new HashSet<>();
    private ItemTouchHelper touchHelper;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        AbstractGBActivity.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_edit);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.pulse_customize_today);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.pulse_bg)));
        }
        getWindow().setStatusBarColor(getResources().getColor(R.color.pulse_bg));

        // Expanded layout toggle: stack the hero (no swiping) vs the swipeable carousel
        final com.google.android.material.materialswitch.MaterialSwitch expandedSwitch =
                findViewById(R.id.pulse_expanded_switch);
        expandedSwitch.setChecked(GBApplication.getPrefs().getBoolean("pulse_today_expanded", false));
        expandedSwitch.setOnCheckedChangeListener((b, checked) ->
                GBApplication.getPrefs().getPreferences().edit()
                        .putBoolean("pulse_today_expanded", checked).apply());

        // Load saved order (enabled, in order), then append any disabled metrics
        final String saved = GBApplication.getPrefs().getString("pulse_today_metrics",
                String.join(",", DashboardFragment.ALL_METRICS));
        for (final String m : saved.split(",")) {
            if (isKnown(m) && !metrics.contains(m)) {
                metrics.add(m);
                enabled.add(m);
            }
        }
        for (final String m : DashboardFragment.ALL_METRICS) {
            if (!metrics.contains(m)) {
                metrics.add(m);
            }
        }

        final RecyclerView list = findViewById(R.id.pulse_edit_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        final Adapter adapter = new Adapter();
        list.setAdapter(adapter);

        final float liftZ = getResources().getDisplayMetrics().density * 8f;
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull final RecyclerView rv, @NonNull final RecyclerView.ViewHolder vh,
                                  @NonNull final RecyclerView.ViewHolder target) {
                final int from = vh.getAdapterPosition();
                final int to = target.getAdapterPosition();
                Collections.swap(metrics, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder vh, final int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // Long-press anywhere on a row to start dragging (the handle still works too).
                return true;
            }

            @Override
            public void onSelectedChanged(final RecyclerView.ViewHolder vh, final int actionState) {
                super.onSelectedChanged(vh, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && vh != null) {
                    vh.itemView.animate().translationZ(liftZ).scaleX(1.03f).scaleY(1.03f)
                            .setDuration(140).start();
                }
            }

            @Override
            public void clearView(@NonNull final RecyclerView rv, @NonNull final RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                vh.itemView.animate().translationZ(0f).scaleX(1f).scaleY(1f).setDuration(140).start();
            }
        });
        touchHelper.attachToRecyclerView(list);
    }

    private static boolean isKnown(final String m) {
        for (final String k : DashboardFragment.ALL_METRICS) {
            if (k.equals(m)) return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    private void save() {
        final StringBuilder sb = new StringBuilder();
        for (final String m : metrics) {
            if (enabled.contains(m)) {
                if (sb.length() > 0) sb.append(",");
                sb.append(m);
            }
        }
        GBApplication.getPrefs().getPreferences().edit()
                .putString("pulse_today_metrics", sb.toString()).apply();
        final Intent intent = new Intent(DashboardFragment.ACTION_CONFIG_CHANGE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String labelFor(final String m) {
        switch (m) {
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
            case "intensity":   return getString(R.string.pulse_intensity);
            default:            return getString(R.string.steps);
        }
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pulse_metric, parent, false);
            return new Holder(v);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull final Holder h, final int position) {
            final String metric = metrics.get(h.getAdapterPosition());
            h.label.setText(labelFor(metric));
            h.toggle.setOnCheckedChangeListener(null);
            h.toggle.setChecked(enabled.contains(metric));
            h.toggle.setOnCheckedChangeListener((b, checked) -> {
                final String m = metrics.get(h.getAdapterPosition());
                if (checked) enabled.add(m); else enabled.remove(m);
            });
            h.drag.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(h);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return metrics.size();
        }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        final TextView label;
        final MaterialSwitch toggle;
        final View drag;

        Holder(@NonNull final View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.pulse_metric_label);
            toggle = itemView.findViewById(R.id.pulse_metric_switch);
            drag = itemView.findViewById(R.id.pulse_metric_drag);
        }
    }
}
