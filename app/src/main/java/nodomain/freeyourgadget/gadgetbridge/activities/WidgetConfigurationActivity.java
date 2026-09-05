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
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
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
import nodomain.freeyourgadget.gadgetbridge.Widget;
import nodomain.freeyourgadget.gadgetbridge.WidgetRings;
import nodomain.freeyourgadget.gadgetbridge.WidgetSteps;
import nodomain.freeyourgadget.gadgetbridge.WidgetVitals;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWidgetMetric;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWidgetStyle;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

public class WidgetConfigurationActivity extends AbstractGBActivity {
    private static final String[] KEYS_NONE =
            {"", "steps", "distance", "calories", "sleep", "heartrate", "bodybattery", "stress", "spo2", "hrv", "respiration"};

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private List<GBDevice> allDevices = new ArrayList<>();
    private GBDevice selectedDevice;

    private String widgetType = "strip";
    private final List<Spinner> metricSpinners = new ArrayList<>();
    private final List<Boolean> metricRequired = new ArrayList<>();

    private String accentKey = "blue";
    private String themeKey = "auto";
    private String tapKey = "charts";

    private FrameLayout previewHost;
    private TextView deviceValue;
    private TextView tapValue;
    private final int[] accentViews = {R.id.widget_accent_0, R.id.widget_accent_1, R.id.widget_accent_2,
            R.id.widget_accent_3, R.id.widget_accent_4};
    private final int[] themeViews = {R.id.widget_theme_auto, R.id.widget_theme_light, R.id.widget_theme_dark};

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
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

        widgetType = detectType();
        accentKey = PulseWidgetStyle.accentKey(mAppWidgetId);
        themeKey = PulseWidgetStyle.themeKey(mAppWidgetId);
        tapKey = PulseWidgetStyle.tapKey(mAppWidgetId);

        allDevices = GBApplication.app().getDeviceManager().getDevices().stream()
                .filter(device -> {
                    final DeviceCoordinator c = device.getDeviceCoordinator();
                    return c.supportsDataFetching(device) || c.supportsActivityTracking(device);
                }).collect(Collectors.toList());

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
        if (selectedDevice != null) {
            new WidgetPreferenceStorage().saveWidgetPrefs(getApplicationContext(),
                    String.valueOf(mAppWidgetId), selectedDevice.getAddress());
        }

        previewHost = findViewById(R.id.widget_preview_host);
        deviceValue = findViewById(R.id.widget_device_value);
        tapValue = findViewById(R.id.widget_tap_value);
        findViewById(R.id.widget_device_row).setOnClickListener(v -> showDevicePicker());
        findViewById(R.id.widget_tap_row).setOnClickListener(v -> showTapPicker());
        ((Button) findViewById(R.id.widget_save_btn)).setOnClickListener(v -> save());

        buildMetricRows();
        setupAccentSwatches();
        setupThemeSegment();

        updateDeviceLabel();
        updateTapLabel();
        updateThemeSegment();
        updateAccentSelection();
        refreshPreview();
    }

    private String detectType() {
        final AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(mAppWidgetId);
        final String cls = info != null && info.provider != null ? info.provider.getClassName() : "";
        if (cls.endsWith(".WidgetSteps")) return "stat";
        if (cls.endsWith(".WidgetRings")) return "rings";
        if (cls.endsWith(".WidgetVitals")) return "vitals";
        return "strip";
    }

    private String defaultMetrics() {
        switch (widgetType) {
            case "stat":   return "steps";
            case "rings":  return "steps,distance,calories";
            case "vitals": return "bodybattery,heartrate,spo2,respiration";
            default:       return "steps,calories,sleep";
        }
    }

    private int[] rowLabels() {
        switch (widgetType) {
            case "stat":   return new int[]{R.string.pulse_widget_metric};
            case "rings":  return new int[]{R.string.pulse_widget_metric, R.string.pulse_widget_slot_1,
                    R.string.pulse_widget_slot_2, R.string.pulse_widget_slot_3};
            case "vitals": return new int[]{R.string.pulse_widget_headline, R.string.pulse_widget_slot_1,
                    R.string.pulse_widget_slot_2, R.string.pulse_widget_slot_3};
            default:       return new int[]{R.string.pulse_widget_slot_1, R.string.pulse_widget_slot_2,
                    R.string.pulse_widget_slot_3, R.string.pulse_widget_slot_4};
        }
    }

    private void buildMetricRows() {
        final LinearLayout container = findViewById(R.id.widget_metrics_container);
        container.removeAllViews();
        metricSpinners.clear();
        metricRequired.clear();

        final int[] labels = rowLabels();
        final String[] current = GBApplication.getPrefs()
                .getString(PulseWidgetStyle.PREF_METRICS + mAppWidgetId, defaultMetrics()).split(",");

        final String[] labelsNone = optionLabels(true);
        final String[] labelsReq = optionLabels(false);
        final int gap = Math.round(getResources().getDisplayMetrics().density * 6);
        final int labelW = Math.round(getResources().getDisplayMetrics().density * 96);

        for (int i = 0; i < labels.length; i++) {
            final boolean required = i == 0 && !"strip".equals(widgetType);
            metricRequired.add(required);

            final LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            final LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = gap;
            row.setLayoutParams(rowLp);

            final TextView lbl = new TextView(this);
            lbl.setText(labels[i]);
            lbl.setTextColor(getColor(R.color.pulse_text_dim));
            lbl.setTextSize(14);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(labelW, ViewGroup.LayoutParams.WRAP_CONTENT));

            final Spinner sp = new Spinner(this);
            sp.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    R.layout.simple_spinner_item_themed, required ? labelsReq : labelsNone);
            adapter.setDropDownViewResource(R.layout.simple_spinner_item_themed);
            sp.setAdapter(adapter);

            final String key = i < current.length ? current[i].trim() : "";
            sp.setSelection(optionIndex(key, required));
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                    refreshPreview();
                }

                @Override
                public void onNothingSelected(final AdapterView<?> parent) {
                }
            });

            row.addView(lbl);
            row.addView(sp);
            container.addView(row);
            metricSpinners.add(sp);
        }
    }

    private String[] optionLabels(final boolean includeNone) {
        final int start = includeNone ? 0 : 1;
        final String[] out = new String[KEYS_NONE.length - start];
        for (int i = start; i < KEYS_NONE.length; i++) {
            out[i - start] = KEYS_NONE[i].isEmpty()
                    ? getString(R.string.pulse_widget_none)
                    : PulseWidgetMetric.fromKey(KEYS_NONE[i]).label(this);
        }
        return out;
    }

    private int optionIndex(final String key, final boolean required) {
        final int start = required ? 1 : 0;
        for (int i = start; i < KEYS_NONE.length; i++) {
            if (KEYS_NONE[i].equals(key)) {
                return i - start;
            }
        }
        return 0;
    }

    private String keyForSpinner(final int rowIndex) {
        final boolean required = metricRequired.get(rowIndex);
        final int start = required ? 1 : 0;
        final int pos = metricSpinners.get(rowIndex).getSelectedItemPosition();
        return KEYS_NONE[pos + start];
    }

    private void setupAccentSwatches() {
        for (int i = 0; i < accentViews.length; i++) {
            final int idx = i;
            final View v = findViewById(accentViews[i]);
            v.setBackgroundTintList(ColorStateList.valueOf(
                    PulseWidgetStyle.accent(PulseWidgetStyle.ACCENT_KEYS[i], false)));
            v.setOnClickListener(view -> {
                accentKey = PulseWidgetStyle.ACCENT_KEYS[idx];
                updateAccentSelection();
                refreshPreview();
            });
        }
    }

    private void updateAccentSelection() {
        for (int i = 0; i < accentViews.length; i++) {
            final View v = findViewById(accentViews[i]);
            final boolean sel = PulseWidgetStyle.ACCENT_KEYS[i].equals(accentKey);
            v.setAlpha(sel ? 1f : 0.4f);
            v.setScaleX(sel ? 1.18f : 1f);
            v.setScaleY(sel ? 1.18f : 1f);
        }
    }

    private void setupThemeSegment() {
        for (int i = 0; i < themeViews.length; i++) {
            final int idx = i;
            findViewById(themeViews[i]).setOnClickListener(v -> {
                themeKey = PulseWidgetStyle.THEME_KEYS[idx];
                updateThemeSegment();
                refreshPreview();
            });
        }
    }

    private void updateThemeSegment() {
        for (int i = 0; i < themeViews.length; i++) {
            final TextView t = findViewById(themeViews[i]);
            final boolean sel = PulseWidgetStyle.THEME_KEYS[i].equals(themeKey);
            t.setBackgroundResource(sel ? R.drawable.pulse_widget_segment_active : 0);
            t.setTextColor(getColor(sel ? R.color.pulse_text : R.color.pulse_text_dim));
        }
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
                    new WidgetPreferenceStorage().saveWidgetPrefs(getApplicationContext(),
                            String.valueOf(mAppWidgetId), selectedDevice.getAddress());
                    updateDeviceLabel();
                    refreshPreview();
                    dialog.dismiss();
                })
                .show();
    }

    private void updateDeviceLabel() {
        deviceValue.setText(selectedDevice != null
                ? selectedDevice.getAliasOrName()
                : getString(R.string.appwidget_not_connected));
    }

    private void showTapPicker() {
        final String[] labels = {
                getString(R.string.pulse_widget_tap_charts),
                getString(R.string.pulse_widget_tap_home),
                getString(R.string.pulse_widget_tap_health)};
        int checked = 0;
        for (int i = 0; i < PulseWidgetStyle.TAP_KEYS.length; i++) {
            if (PulseWidgetStyle.TAP_KEYS[i].equals(tapKey)) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pulse_widget_tap)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    tapKey = PulseWidgetStyle.TAP_KEYS[which];
                    updateTapLabel();
                    refreshPreview();
                    dialog.dismiss();
                })
                .show();
    }

    private void updateTapLabel() {
        final int res;
        switch (tapKey) {
            case "home":   res = R.string.pulse_widget_tap_home; break;
            case "health": res = R.string.pulse_widget_tap_health; break;
            default:       res = R.string.pulse_widget_tap_charts; break;
        }
        tapValue.setText(res);
    }

    private void writePrefs() {
        final List<String> chosen = new ArrayList<>();
        for (int i = 0; i < metricSpinners.size(); i++) {
            final String key = keyForSpinner(i);
            if (!key.isEmpty() && !chosen.contains(key)) {
                chosen.add(key);
            }
        }
        if (chosen.isEmpty()) {
            for (final String k : defaultMetrics().split(",")) {
                chosen.add(k);
            }
        }
        GBApplication.getPrefs().getPreferences().edit()
                .putString(PulseWidgetStyle.PREF_METRICS + mAppWidgetId, TextUtils.join(",", chosen))
                .putString(PulseWidgetStyle.PREF_ACCENT + mAppWidgetId, accentKey)
                .putString(PulseWidgetStyle.PREF_THEME + mAppWidgetId, themeKey)
                .putString(PulseWidgetStyle.PREF_TAP + mAppWidgetId, tapKey)
                .apply();
        if (selectedDevice != null) {
            new WidgetPreferenceStorage().saveWidgetPrefs(getApplicationContext(),
                    String.valueOf(mAppWidgetId), selectedDevice.getAddress());
        }
    }

    private void refreshPreview() {
        writePrefs();
        RemoteViews rv = null;
        try {
            switch (widgetType) {
                case "stat":   rv = WidgetSteps.buildViews(this, mAppWidgetId); break;
                case "rings":  rv = WidgetRings.buildViews(this, mAppWidgetId); break;
                case "vitals": rv = WidgetVitals.buildViews(this, mAppWidgetId); break;
                default:       rv = Widget.buildViews(this, mAppWidgetId); break;
            }
        } catch (final Exception ignored) {
        }
        previewHost.removeAllViews();
        if (rv != null) {
            try {
                previewHost.addView(rv.apply(getApplicationContext(), previewHost));
            } catch (final Exception ignored) {
            }
        }
    }

    private Class<?> providerClass() {
        switch (widgetType) {
            case "stat":   return WidgetSteps.class;
            case "rings":  return WidgetRings.class;
            case "vitals": return WidgetVitals.class;
            default:       return Widget.class;
        }
    }

    private void save() {
        writePrefs();
        final Intent update = new Intent(this, providerClass());
        update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId});
        sendBroadcast(update);

        final Intent resultOk = new Intent();
        resultOk.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultOk);
        finish();
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

    @Override
    public void setLanguage(final Locale language, final boolean invalidateLanguage) {
        AndroidUtils.setLanguage(this, language);
    }

    static Context appContext() {
        return GBApplication.getContext();
    }
}
