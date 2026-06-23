/*  Copyright (C) 2020-2024 Arjan Schrijver, Daniel Dakhno, Petr Vaněk

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

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

public class WidgetConfigurationActivity extends AbstractGBActivity {
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private List<GBDevice> allDevices = new ArrayList<>();
    private GBDevice selectedDevice;

    // metric keys; index 0 = "none" (empty slot)
    private static final String[] METRIC_KEYS =
            {"", "steps", "distance", "calories", "sleep", "heartrate", "bodybattery"};

    private final Spinner[] slots = new Spinner[3];
    private TextView deviceValue;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // cancelled result by default (in case the user backs out)
        final Intent cancelled = new Intent();
        cancelled.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, cancelled);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setContentView(R.layout.activity_pulse_widget_config);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pulse_widget_title);
        }

        allDevices = GBApplication.app().getDeviceManager().getDevices().stream()
                .filter(device -> {
                    final DeviceCoordinator coordinator = device.getDeviceCoordinator();
                    return coordinator.supportsDataFetching(device) || coordinator.supportsActivityTracking(device);
                }).collect(Collectors.toList());

        // pre-select previously chosen device (reconfigure) or the first one
        final GBDevice existing = new WidgetPreferenceStorage().getDeviceForWidget(mAppWidgetId);
        if (existing != null) {
            for (final GBDevice d : allDevices) {
                if (d.getAddress().equals(existing.getAddress())) {
                    selectedDevice = d;
                    break;
                }
            }
        }
        if (selectedDevice == null && !allDevices.isEmpty()) {
            selectedDevice = allDevices.get(0);
        }

        deviceValue = findViewById(R.id.widget_device_value);
        findViewById(R.id.widget_device_row).setOnClickListener(v -> showDevicePicker());

        slots[0] = findViewById(R.id.widget_slot0);
        slots[1] = findViewById(R.id.widget_slot1);
        slots[2] = findViewById(R.id.widget_slot2);

        final String[] labels = metricLabels();
        for (final Spinner spinner : slots) {
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    R.layout.simple_spinner_item_themed, labels);
            adapter.setDropDownViewResource(R.layout.simple_spinner_item_themed);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                    updatePreview();
                }

                @Override
                public void onNothingSelected(final AdapterView<?> parent) { }
            });
        }

        // restore current metric selection
        final String[] current = nodomain.freeyourgadget.gadgetbridge.Widget.getWidgetMetrics(mAppWidgetId);
        final String[] defaults = {"steps", "distance", "sleep"};
        for (int i = 0; i < 3; i++) {
            final String key = i < current.length ? current[i].trim() : (i < defaults.length ? defaults[i] : "");
            slots[i].setSelection(indexOfMetric(key));
        }

        findViewById(R.id.widget_save_btn).setOnClickListener(v -> save());

        updateDeviceLabel();
        updatePreview();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuItem done = menu.add(0, 1, 0, android.R.string.ok);
        done.setIcon(R.drawable.ic_done);
        done.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == 1) {
            save();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDevicePicker() {
        if (allDevices.isEmpty()) {
            return;
        }
        final String[] names = new String[allDevices.size()];
        int checked = 0;
        for (int i = 0; i < allDevices.size(); i++) {
            names[i] = allDevices.get(i).getAliasOrName();
            if (selectedDevice != null && allDevices.get(i).getAddress().equals(selectedDevice.getAddress())) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.widget_settings_select_device_title)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    selectedDevice = allDevices.get(which);
                    updateDeviceLabel();
                    updatePreview();
                    dialog.dismiss();
                })
                .show();
    }

    private void updateDeviceLabel() {
        deviceValue.setText(selectedDevice != null
                ? selectedDevice.getAliasOrName()
                : getString(R.string.appwidget_not_connected));
    }

    private String[] metricLabels() {
        return new String[]{
                getString(R.string.pulse_widget_none),
                getString(R.string.steps),
                getString(R.string.distance),
                getString(R.string.calories),
                getString(R.string.menuitem_sleep),
                getString(R.string.menuitem_hr),
                getString(R.string.body_energy),
        };
    }

    private int indexOfMetric(final String key) {
        for (int i = 0; i < METRIC_KEYS.length; i++) {
            if (METRIC_KEYS[i].equals(key)) {
                return i;
            }
        }
        return 0;
    }

    private void updatePreview() {
        final TextView name = findViewById(R.id.todaywidget_device_name);
        if (name != null) {
            name.setText(selectedDevice != null ? selectedDevice.getAliasOrName() : getString(R.string.app_name));
        }
        // header battery/status are placeholders in the preview
        final View battery = findViewById(R.id.todaywidget_battery_icon);
        if (battery != null) battery.setVisibility(View.GONE);
        final TextView status = findViewById(R.id.todaywidget_device_status);
        if (status != null) status.setText("");

        final int[] slotIds = {R.id.pulse_w0, R.id.pulse_w1, R.id.pulse_w2};
        final int[] iconIds = {R.id.pulse_w0_icon, R.id.pulse_w1_icon, R.id.pulse_w2_icon};
        final int[] valueIds = {R.id.pulse_w0_value, R.id.pulse_w1_value, R.id.pulse_w2_value};
        final int[] labelIds = {R.id.pulse_w0_label, R.id.pulse_w1_label, R.id.pulse_w2_label};
        final int[] progIds = {R.id.pulse_w0_prog, R.id.pulse_w1_prog, R.id.pulse_w2_prog};

        for (int i = 0; i < 3; i++) {
            final String key = METRIC_KEYS[slots[i].getSelectedItemPosition()];
            final View slot = findViewById(slotIds[i]);
            if (key.isEmpty()) {
                slot.setVisibility(View.GONE);
                continue;
            }
            slot.setVisibility(View.VISIBLE);
            ((ImageView) findViewById(iconIds[i])).setBackgroundResource(previewIcon(key));
            ((TextView) findViewById(valueIds[i])).setText(previewValue(key));
            ((TextView) findViewById(labelIds[i])).setText(previewLabel(key));
            final ProgressBar prog = findViewById(progIds[i]);
            final int pct = previewProgress(key);
            if (pct >= 0) {
                prog.setVisibility(View.VISIBLE);
                prog.setMax(100);
                prog.setProgress(pct);
            } else {
                prog.setVisibility(View.INVISIBLE);
            }
        }
    }

    private int previewIcon(final String key) {
        switch (key) {
            case "distance": return R.drawable.ic_map;
            case "calories": return R.drawable.ic_calories;
            case "sleep": return R.drawable.ic_nights_stay;
            case "heartrate": return R.drawable.ic_heartrate;
            case "bodybattery": return R.drawable.ic_battery_full;
            default: return R.drawable.ic_steps;
        }
    }

    private String previewLabel(final String key) {
        switch (key) {
            case "distance": return getString(R.string.distance);
            case "calories": return getString(R.string.calories);
            case "sleep": return getString(R.string.menuitem_sleep);
            case "heartrate": return getString(R.string.menuitem_hr);
            case "bodybattery": return getString(R.string.body_energy);
            default: return getString(R.string.steps);
        }
    }

    // illustrative sample values for the preview only
    private String previewValue(final String key) {
        switch (key) {
            case "distance": return "5.2 km";
            case "calories": return "420";
            case "sleep": return "7h 20m";
            case "heartrate": return "68";
            case "bodybattery": return "74";
            default: return "8,432";
        }
    }

    private int previewProgress(final String key) {
        switch (key) {
            case "heartrate": return -1; // no goal bar
            case "bodybattery": return 74;
            case "distance": return 65;
            case "calories": return 55;
            case "sleep": return 90;
            default: return 80;
        }
    }

    private void save() {
        if (selectedDevice != null) {
            new WidgetPreferenceStorage().saveWidgetPrefs(getApplicationContext(),
                    String.valueOf(mAppWidgetId), selectedDevice.getAddress());
        }
        final List<String> chosen = new ArrayList<>();
        for (final Spinner spinner : slots) {
            final String key = METRIC_KEYS[spinner.getSelectedItemPosition()];
            if (!key.isEmpty() && !chosen.contains(key)) {
                chosen.add(key);
            }
        }
        if (chosen.isEmpty()) {
            chosen.add("steps");
        }
        GBApplication.getPrefs().getPreferences().edit()
                .putString("pulse_widget_metrics_" + mAppWidgetId,
                        android.text.TextUtils.join(",", chosen))
                .apply();

        final Intent update = new Intent(this, nodomain.freeyourgadget.gadgetbridge.Widget.class);
        update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId});
        sendBroadcast(update);

        final Intent resultOk = new Intent();
        resultOk.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultOk);
        finish();
    }

    @Override
    public void setLanguage(final Locale language, final boolean invalidateLanguage) {
        AndroidUtils.setLanguage(this, language);
    }
}
