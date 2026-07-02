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
package nodomain.freeyourgadget.gadgetbridge.activities.xiaomi;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.VibrationPatternData;
import nodomain.freeyourgadget.gadgetbridge.devices.VibrationPatternDataSource;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiVibrationDataSource;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiVibrationManager;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiVibrationPatternsActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiVibrationPatternsActivity.class);

    private static final long WRITE_FALLBACK_MS = 5000;

    private GBDevice gbDevice;
    private VibrationPatternDataSource dataSource;
    private VibrationAdapter adapter;
    private TextView emptyView;
    private View addButton;
    private boolean writable;
    private boolean showIds;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
            (prefs, key) -> {
                if (XiaomiVibrationManager.PREF_PATTERNS.equals(key)) {
                    reload();
                }
            };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_xiaomi_vibration_patterns);

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (gbDevice == null) {
            LOG.error("gbDevice must not be null");
            finish();
            return;
        }

        dataSource = new XiaomiVibrationDataSource(this, gbDevice);

        final boolean experimental = GBApplication.getPrefs().experimentalSettings();
        writable = gbDevice.getDeviceCoordinator().supportsCustomVibrationPatterns(gbDevice) && experimental;
        showIds = experimental;

        emptyView = findViewById(R.id.vibration_patterns_empty);

        findViewById(R.id.vibration_refresh_button).setOnClickListener(v -> refreshFromDevice());
        addButton = findViewById(R.id.vibration_add_button);
        addButton.setOnClickListener(v -> showAddDialog());
        addButton.setVisibility(writable ? View.VISIBLE : View.GONE);

        adapter = new VibrationAdapter(this);

        final RecyclerView recyclerView = findViewById(R.id.vibration_patterns_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())
                .registerOnSharedPreferenceChangeListener(prefsListener);
        reload();
        requestPatternsFromDevice();
    }

    @Override
    protected void onPause() {
        GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())
                .unregisterOnSharedPreferenceChangeListener(prefsListener);
        super.onPause();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void reload() {
        final List<Row> rows = buildRows();
        adapter.setRows(rows);
        adapter.notifyDataSetChanged();

        emptyView.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        addButton.setEnabled(true);
    }

    private List<Row> buildRows() {
        final VibrationPatternData data = dataSource.loadData();

        final List<Row> rows = new ArrayList<>();

        if (!data.typeMappings.isEmpty()) {
            rows.add(Row.header(getString(R.string.xiaomi_vibration_section_types)));
            for (final VibrationPatternData.TypeEntry t : data.typeMappings) {
                rows.add(Row.item(
                        t.typeName,
                        withId(t.patternName, t.patternId),
                        -1
                ));
            }
        }

        if (!data.customPatterns.isEmpty()) {
            rows.add(Row.header(getString(R.string.xiaomi_vibration_section_patterns)));
            for (final VibrationPatternData.PatternEntry p : data.customPatterns) {
                final boolean deletable = writable && p.canDelete;
                rows.add(Row.item(
                        withId(p.name, p.id),
                        p.typeName,
                        deletable ? p.id : -1
                ));
            }
        }

        return rows;
    }

    private String withId(final String label, final int id) {
        return showIds ? label + " (id " + id + ")" : label;
    }

    private boolean requireConnected() {
        if (gbDevice.isInitialized()) {
            return true;
        }
        GB.toast(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.WARN);
        return false;
    }

    private void requestPatternsFromDevice() {
        if (!gbDevice.isInitialized()) {
            return;
        }
        dataSource.requestRefresh();
    }

    private void refreshFromDevice() {
        if (!requireConnected()) {
            return;
        }
        requestPatternsFromDevice();
        GB.toast(this, getString(R.string.xiaomi_vibration_refresh_requested), Toast.LENGTH_SHORT, GB.INFO);
    }

    private void showAddDialog() {
        if (!requireConnected()) {
            return;
        }

        final List<Integer> typeIds = dataSource.getSelectableNotificationTypeIds();

        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_xiaomi_vibration_add, null);
        final EditText nameField = view.findViewById(R.id.vibration_add_name);
        final Spinner typeSpinner = view.findViewById(R.id.vibration_add_type);
        final LinearLayout segments = view.findViewById(R.id.vibration_add_segments);

        final String[] typeNames = new String[typeIds.size()];
        for (int i = 0; i < typeIds.size(); i++) {
            typeNames[i] = dataSource.getNotificationTypeName(typeIds.get(i));
        }
        final ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeNames);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        addSegmentRow(segments, new VibrationPatternData.Segment(true, 200, 60));

        view.findViewById(R.id.vibration_add_segment).setOnClickListener(v -> addSegmentRow(segments, null));

        new AlertDialog.Builder(this)
                .setTitle(R.string.xiaomi_vibration_add_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final String name = nameField.getText().toString().trim();
                    final int type = typeIds.get(typeSpinner.getSelectedItemPosition());
                    final List<VibrationPatternData.Segment> segs = collectSegments(segments);
                    if (segs.isEmpty()) {
                        GB.toast(this, getString(R.string.xiaomi_vibration_no_segments), Toast.LENGTH_SHORT, GB.WARN);
                        return;
                    }
                    sendAddPattern(name, type, segs);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static int parseIntOrZero(final String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    private void addSegmentRow(final LinearLayout container, final VibrationPatternData.Segment segment) {
        final View row = LayoutInflater.from(this)
                .inflate(R.layout.item_xiaomi_vibration_segment_edit, container, false);
        final SwitchCompat onSwitch = row.findViewById(R.id.segment_on);
        final EditText strengthField = row.findViewById(R.id.segment_strength);

        final boolean on = segment == null || segment.on;
        onSwitch.setChecked(on);
        if (segment != null) {
            ((EditText) row.findViewById(R.id.segment_ms)).setText(String.valueOf(segment.durationMs));
            if (segment.strengthPercent > 0) {
                strengthField.setText(String.valueOf(segment.strengthPercent));
            }
        }
        strengthField.setEnabled(on);
        onSwitch.setOnCheckedChangeListener((button, checked) -> strengthField.setEnabled(checked));
        row.findViewById(R.id.segment_remove).setOnClickListener(v -> container.removeView(row));

        container.addView(row);
    }

    private List<VibrationPatternData.Segment> collectSegments(final LinearLayout container) {
        final List<VibrationPatternData.Segment> segs = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            final View row = container.getChildAt(i);
            final boolean on = ((SwitchCompat) row.findViewById(R.id.segment_on)).isChecked();
            final int ms = parseIntOrZero(((EditText) row.findViewById(R.id.segment_ms)).getText().toString());
            final int strength = parseIntOrZero(((EditText) row.findViewById(R.id.segment_strength)).getText().toString());
            segs.add(new VibrationPatternData.Segment(on, ms, on && strength > 0 ? strength : 0));
        }
        return segs;
    }

    private void sendAddPattern(final String name, final int typeId,
                                final List<VibrationPatternData.Segment> segments) {
        dataSource.addPattern(name, typeId, segments);
        GB.toast(this, getString(R.string.xiaomi_vibration_add_requested), Toast.LENGTH_SHORT, GB.INFO);
        addButton.setEnabled(false);
        emptyView.postDelayed(this::reload, WRITE_FALLBACK_MS);
    }

    private void confirmDelete(final int id, final String name) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.xiaomi_vibration_delete_title)
                .setMessage(getString(R.string.xiaomi_vibration_delete_message, name))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> deletePattern(id))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deletePattern(final int id) {
        if (!requireConnected()) {
            return;
        }
        dataSource.deletePattern(id);
        emptyView.postDelayed(this::reload, WRITE_FALLBACK_MS);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class Row {
        static final int VIEW_HEADER = 0;
        static final int VIEW_ITEM = 1;

        final int viewType;
        final String title;
        final String subtitle;
        final int deleteId;

        private Row(final int viewType, final String title, final String subtitle, final int deleteId) {
            this.viewType = viewType;
            this.title = title;
            this.subtitle = subtitle;
            this.deleteId = deleteId;
        }

        static Row header(final String title) {
            return new Row(VIEW_HEADER, title, null, -1);
        }

        static Row item(final String title, final String subtitle, final int deleteId) {
            return new Row(VIEW_ITEM, title, subtitle, deleteId);
        }
    }

    private static class VibrationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final XiaomiVibrationPatternsActivity activity;
        private final List<Row> rows = new ArrayList<>();

        VibrationAdapter(final XiaomiVibrationPatternsActivity activity) {
            this.activity = activity;
        }

        void setRows(final List<Row> rows) {
            this.rows.clear();
            this.rows.addAll(rows);
        }

        @Override
        public int getItemViewType(final int position) {
            return rows.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == Row.VIEW_HEADER) {
                return new HeaderViewHolder(inflater.inflate(R.layout.item_xiaomi_vibration_header, parent, false));
            }
            return new ItemViewHolder(inflater.inflate(R.layout.item_xiaomi_vibration_pattern, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
            final Row row = rows.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).title.setText(row.title);
                return;
            }

            final ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.name.setText(row.title);
            itemHolder.description.setText(row.subtitle);
            itemHolder.description.setVisibility(row.subtitle == null || row.subtitle.isEmpty() ? View.GONE : View.VISIBLE);

            final boolean deletable = row.deleteId >= 0;
            itemHolder.delete.setVisibility(deletable ? View.VISIBLE : View.GONE);
            itemHolder.delete.setOnClickListener(deletable ? v -> activity.confirmDelete(row.deleteId, row.title) : null);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            final TextView title;

            HeaderViewHolder(@NonNull final View itemView) {
                super(itemView);
                title = (TextView) itemView;
            }
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView description;
            final ImageButton delete;

            ItemViewHolder(@NonNull final View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.vibration_pattern_name);
                description = itemView.findViewById(R.id.vibration_pattern_description);
                delete = itemView.findViewById(R.id.vibration_pattern_delete);
            }
        }
    }
}
