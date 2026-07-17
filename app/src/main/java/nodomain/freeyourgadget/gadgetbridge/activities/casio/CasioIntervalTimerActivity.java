/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.activities.casio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioIntervalTimer;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.CasioIntervalTimerLibrary;

public class CasioIntervalTimerActivity extends AbstractGBActivity {
    private GBDevice gbDevice;
    protected CasioIntervalTimerLibrary library;
    protected TimerAdapter adapter;
    private TextView emptyView;
    private FloatingActionButton addButton;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_casio_interval_timer);

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (gbDevice == null) {
            finish();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        library = loadLibrary();

        emptyView = findViewById(R.id.casio_timer_empty);
        addButton = findViewById(R.id.casio_timer_add);
        final RecyclerView list = findViewById(R.id.casio_timer_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimerAdapter();
        list.setAdapter(adapter);

        addButton.setOnClickListener(v -> onAddClicked());
        refreshEmpty();
    }

    private CasioIntervalTimerLibrary loadLibrary() {
        final String json = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())
                .getString(CasioIntervalTimerLibrary.PREF_INTERVAL_TIMER_LIBRARY, null);
        return CasioIntervalTimerLibrary.fromJson(json);
    }

    protected void persistAndRefresh() {
        GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).edit()
                .putString(CasioIntervalTimerLibrary.PREF_INTERVAL_TIMER_LIBRARY, library.toJson())
                .apply();
        adapter.notifyDataSetChanged();
        refreshEmpty();
    }

    protected void pushActive() {
        GBApplication.deviceService(gbDevice)
                .onSendConfiguration(CasioIntervalTimerLibrary.CONFIG_INTERVAL_TIMER_ACTIVE);
    }

    private void refreshEmpty() {
        final boolean empty = library.timers.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    protected void openEditor(final int index) {
        final boolean isNew = index < 0;
        final CasioIntervalTimer working = isNew ? defaultTimer() : cloneTimer(library.timers.get(index));

        final View root = LayoutInflater.from(this)
                .inflate(R.layout.dialog_casio_interval_timer_edit, null);
        final EditText labelView = root.findViewById(R.id.timer_label);
        labelView.setText(working.label);

        final android.widget.LinearLayout slotContainer = root.findViewById(R.id.timer_slots_container);
        final EditText[] names = new EditText[CasioIntervalTimer.SLOT_COUNT];
        final android.widget.CheckBox[] skips = new android.widget.CheckBox[CasioIntervalTimer.SLOT_COUNT];
        final android.widget.NumberPicker[] mins = new android.widget.NumberPicker[CasioIntervalTimer.SLOT_COUNT];
        final android.widget.NumberPicker[] secs = new android.widget.NumberPicker[CasioIntervalTimer.SLOT_COUNT];

        final android.text.InputFilter[] nameFilters = new android.text.InputFilter[]{
                new android.text.InputFilter.AllCaps(),
                new android.text.InputFilter.LengthFilter(CasioIntervalTimer.NAME_MAX),
                (source, start, end, dest, dstart, dend) -> {
                    final StringBuilder kept = new StringBuilder();
                    for (int i = start; i < end; i++) {
                        char c = Character.toUpperCase(source.charAt(i));
                        if (c == ' ') c = '_';
                        if (CasioIntervalTimer.NAME_ALLOWED.indexOf(c) >= 0) kept.append(c);
                    }
                    return kept.toString();
                }
        };

        for (int i = 0; i < CasioIntervalTimer.SLOT_COUNT; i++) {
            final View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_casio_interval_timer_slot_edit, slotContainer, false);
            ((TextView) row.findViewById(R.id.slot_title))
                    .setText(getString(R.string.casio_interval_timer_slot, i + 1));
            names[i] = row.findViewById(R.id.slot_name);
            names[i].setFilters(nameFilters);
            names[i].setText(working.slots[i].name);
            skips[i] = row.findViewById(R.id.slot_skip);
            skips[i].setChecked(working.slots[i].skipped);
            mins[i] = row.findViewById(R.id.slot_minutes);
            mins[i].setMinValue(0);
            mins[i].setMaxValue(60);
            mins[i].setValue(working.slots[i].minutes);
            secs[i] = row.findViewById(R.id.slot_seconds);
            secs[i].setMinValue(0);
            secs[i].setMaxValue(59);
            secs[i].setValue(working.slots[i].seconds);
            slotContainer.addView(row);
        }

        final android.widget.NumberPicker repeat = root.findViewById(R.id.timer_repeat);
        repeat.setMinValue(1);
        repeat.setMaxValue(20);
        repeat.setValue(CasioIntervalTimer.clampRepeat(working.autoRepeat));

        final TextView totalView = root.findViewById(R.id.timer_total);
        final Runnable updateTotal = () -> {
            int cycle = 0;
            for (int i = 0; i < CasioIntervalTimer.SLOT_COUNT; i++) {
                if (skips[i].isChecked()) continue;
                int m = mins[i].getValue();
                int s = secs[i].getValue();
                if (m == 60) s = 0;   // clamp to the 60'00" ceiling
                cycle += m * 60 + s;
            }
            final int rep = repeat.getValue();
            totalView.setText(getString(R.string.casio_interval_timer_total,
                    CasioIntervalTimer.formatDuration(cycle), rep,
                    CasioIntervalTimer.formatDuration(cycle * rep)));
        };
        for (int i = 0; i < CasioIntervalTimer.SLOT_COUNT; i++) {
            mins[i].setOnValueChangedListener((picker, oldVal, newVal) -> updateTotal.run());
            secs[i].setOnValueChangedListener((picker, oldVal, newVal) -> updateTotal.run());
            skips[i].setOnCheckedChangeListener((button, checked) -> updateTotal.run());
        }
        repeat.setOnValueChangedListener((picker, oldVal, newVal) -> updateTotal.run());
        updateTotal.run();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.pref_casio_interval_timer_title)
                .setView(root)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    working.label = labelView.getText().toString().trim();
                    if (working.label.isEmpty()) {
                        working.label = getString(R.string.casio_interval_timer_default_label,
                                (isNew ? library.timers.size() : index) + 1);
                    }
                    for (int i = 0; i < CasioIntervalTimer.SLOT_COUNT; i++) {
                        working.slots[i].name = CasioIntervalTimer.normalizeName(names[i].getText().toString());
                        working.slots[i].skipped = skips[i].isChecked();
                        int m = mins[i].getValue();
                        int s = secs[i].getValue();
                        if (m == 60) s = 0;   // clamp to the 60'00" ceiling
                        working.slots[i].minutes = m;
                        working.slots[i].seconds = s;
                    }
                    working.autoRepeat = repeat.getValue();

                    final boolean editingActive = !isNew && index == library.activeIndex;
                    if (isNew) {
                        library.add(working);
                    } else {
                        library.timers.set(index, working);
                    }
                    persistAndRefresh();
                    if (editingActive) {
                        pushActive();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static CasioIntervalTimer defaultTimer() {
        return new CasioIntervalTimer();
    }

    private static CasioIntervalTimer cloneTimer(final CasioIntervalTimer src) {
        final CasioIntervalTimer copy = new CasioIntervalTimer();
        copy.label = src.label;
        copy.autoRepeat = src.autoRepeat;
        for (int i = 0; i < CasioIntervalTimer.SLOT_COUNT; i++) {
            copy.slots[i].name = src.slots[i].name;
            copy.slots[i].skipped = src.slots[i].skipped;
            copy.slots[i].minutes = src.slots[i].minutes;
            copy.slots[i].seconds = src.slots[i].seconds;
        }
        return copy;
    }

    private void onAddClicked() {
        if (library.timers.size() >= CasioIntervalTimerLibrary.MAX_TIMERS) {
            Toast.makeText(this, R.string.casio_interval_timer_limit_reached, Toast.LENGTH_SHORT).show();
            return;
        }
        openEditor(-1);
    }

    static String summarize(final CasioIntervalTimer t) {
        final StringBuilder sb = new StringBuilder();
        int steps = 0;
        for (final CasioIntervalTimer.Interval s : t.slots) {
            if (s.skipped) continue;
            if (sb.length() > 0) sb.append(" / ");
            sb.append(String.format(Locale.ROOT, "%02d:%02d", s.minutes, s.seconds));
            steps++;
        }
        return steps + " · " + sb + " · ×" + t.autoRepeat;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final android.widget.RadioButton active;
            final TextView label;
            final TextView summary;
            final android.widget.ImageButton delete;
            VH(final View v) {
                super(v);
                active = v.findViewById(R.id.casio_timer_item_active);
                label = v.findViewById(R.id.casio_timer_item_label);
                summary = v.findViewById(R.id.casio_timer_item_summary);
                delete = v.findViewById(R.id.casio_timer_item_delete);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_casio_interval_timer, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final VH h, final int position) {
            final CasioIntervalTimer t = library.timers.get(position);
            h.label.setText(t.label);
            h.summary.setText(summarize(t));
            h.active.setChecked(position == library.activeIndex);
            h.active.setOnClickListener(v -> {
                library.setActive(h.getBindingAdapterPosition());
                persistAndRefresh();
                pushActive();
            });
            h.itemView.setOnClickListener(v -> openEditor(h.getBindingAdapterPosition()));
            h.delete.setOnClickListener(v -> {
                library.remove(h.getBindingAdapterPosition());
                persistAndRefresh();
            });
        }

        @Override
        public int getItemCount() {
            return library.timers.size();
        }
    }
}
