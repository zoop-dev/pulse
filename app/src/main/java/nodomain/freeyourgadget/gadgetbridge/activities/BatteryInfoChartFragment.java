/*  Copyright (C) 2021-2026 José Rebelo, Petr Vaněk

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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.Chip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SampleXLabelFormatter;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.TimestampTranslation;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.marker.ValueMarker;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.databinding.FragmentBatteryChartBinding;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class BatteryInfoChartFragment extends AbstractGBFragment {
    private static final Logger LOG = LoggerFactory.getLogger(BatteryInfoChartFragment.class);
    private static final String PREF_SELECTED_METRICS_PREFIX = "chart_battery_selected_metrics_";
    private static final String STATE_SELECTED_METRICS = "selectedMetrics";

    private int chartTextColor;
    private int textColor;
    private int backgroundColor;

    private FragmentBatteryChartBinding binding;

    private int startTime;
    private int endTime;
    private GBDevice gbDevice;
    private int batteryIndex;
    private Set<BatteryMetric> selectedMetrics = EnumSet.of(BatteryMetric.LEVEL);
    private boolean selectedMetricsInitialized;
    private RefreshTask refreshTask;
    private final WorkoutValueFormatter valueFormatter = new WorkoutValueFormatter();

    public void setDateAndGetData(final GBDevice gbDevice, final int batteryIndex, final long startTime, final long endTime) {
        this.startTime = (int) startTime;
        this.endTime = (int) endTime;
        this.gbDevice = gbDevice;
        this.batteryIndex = batteryIndex;
        ensureSelectedMetricsLoaded();
        try {
            startRefreshTask();
        } catch (final Exception e) {
            LOG.debug("Unable to fill charts data right now:", e);
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        init();

        binding = FragmentBatteryChartBinding.inflate(inflater, container, false);

        if (savedInstanceState != null) {
            final String[] saved = savedInstanceState.getStringArray(STATE_SELECTED_METRICS);
            if (saved != null && saved.length > 0) {
                selectedMetrics = parseMetrics(saved);
                selectedMetricsInitialized = true;
            }
        } else {
            ensureSelectedMetricsLoaded();
        }

        setupChart();

        if (gbDevice != null) {
            startRefreshTask();
        }

        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray(STATE_SELECTED_METRICS, metricsToNames(selectedMetrics));
    }

    @Override
    public void onDestroyView() {
        if (refreshTask != null) {
            refreshTask.cancel(true);
            refreshTask = null;
        }
        binding = null;
        super.onDestroyView();
    }

    @Override
    public String getTitle() {
        return "";
    }

    private void init() {
        backgroundColor = GBApplication.getBackgroundColor(requireContext());
        textColor = GBApplication.getTextColor(requireContext());
        chartTextColor = GBApplication.getSecondaryTextColor(requireContext());
    }

    private void setupChart() {
        binding.batteryChart.setBackgroundColor(backgroundColor);
        binding.batteryChart.getDescription().setEnabled(false);
        binding.batteryChart.getLegend().setTextColor(textColor);
        binding.batteryChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        binding.batteryChart.getLegend().setWordWrapEnabled(true);
        binding.batteryChart.setTouchEnabled(true);
        binding.batteryChart.setDragEnabled(true);
        binding.batteryChart.setScaleEnabled(true);
        binding.batteryChart.setDrawGridBackground(false);
        binding.batteryChart.setHighlightPerDragEnabled(false);

        final XAxis x = binding.batteryChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(chartTextColor);
        x.setAvoidFirstLastClipping(true);

        final YAxis yAxisLeft = binding.batteryChart.getAxisLeft();
        yAxisLeft.setTextColor(chartTextColor);
        yAxisLeft.setDrawGridLines(true);

        final YAxis yAxisRight = binding.batteryChart.getAxisRight();
        yAxisRight.setTextColor(chartTextColor);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
    }

    private void startRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel(true);
        }
        refreshTask = new RefreshTask("Visualizing battery data", getActivity(), gbDevice, batteryIndex, startTime, endTime, selectedMetrics);
        refreshTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onMetricToggled(final BatteryMetric metric, final boolean checked, final Chip chip) {
        if (!checked && selectedMetrics.size() <= 1) {
            // keep at least one metric selected; revert the chip
            chip.setChecked(true);
            Toast.makeText(requireContext(), R.string.charts_at_least_one_item, Toast.LENGTH_SHORT).show();
            return;
        }

        final Set<BatteryMetric> updated = EnumSet.copyOf(selectedMetrics);
        if (checked) {
            updated.add(metric);
        } else {
            updated.remove(metric);
        }
        selectedMetrics = updated;
        saveSelectedMetrics(selectedMetrics);
        startRefreshTask();
    }

    private void rebuildChips(final Set<BatteryMetric> availableMetrics) {
        if (binding == null) {
            return;
        }
        binding.batteryChartChipGroup.removeAllViews();

        if (availableMetrics.size() <= 1) {
            // single chip, hide it
            binding.batteryChartChipGroup.setVisibility(View.GONE);
            return;
        }
        binding.batteryChartChipGroup.setVisibility(View.VISIBLE);

        for (final BatteryMetric metric : BatteryMetric.values()) {
            if (!availableMetrics.contains(metric)) {
                continue;
            }
            final Chip chip = new Chip(requireContext());
            chip.setText(getString(metric.labelResId));
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(selectedMetrics.contains(metric));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> onMetricToggled(metric, isChecked, (Chip) buttonView));
            binding.batteryChartChipGroup.addView(chip);
        }
    }

    private void ensureSelectedMetricsLoaded() {
        if (!selectedMetricsInitialized && gbDevice != null) {
            selectedMetrics = loadSelectedMetrics();
            selectedMetricsInitialized = true;
        }
    }

    private Set<BatteryMetric> loadSelectedMetrics() {
        if (gbDevice == null) {
            return EnumSet.of(BatteryMetric.LEVEL);
        }
        final DevicePrefs prefs = GBApplication.getDevicePrefs(gbDevice);
        final String csv = prefs.getString(PREF_SELECTED_METRICS_PREFIX + batteryIndex, null);
        if (csv == null || csv.trim().isEmpty()) {
            return EnumSet.of(BatteryMetric.LEVEL);
        }
        final Set<BatteryMetric> parsed = parseMetrics(csv.split(","));
        return parsed.isEmpty() ? EnumSet.of(BatteryMetric.LEVEL) : parsed;
    }

    private void saveSelectedMetrics(final Set<BatteryMetric> metrics) {
        if (gbDevice == null) {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for (final BatteryMetric metric : metrics) {
            //noinspection SizeReplaceableByIsEmpty isEmpty requires SDK 35
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(metric.name());
        }
        GBApplication.getDevicePrefs(gbDevice).getPreferences().edit()
                .putString(PREF_SELECTED_METRICS_PREFIX + batteryIndex, sb.toString())
                .apply();
    }

    private static Set<BatteryMetric> parseMetrics(final String[] names) {
        final Set<BatteryMetric> metrics = new LinkedHashSet<>();
        for (final String name : names) {
            try {
                metrics.add(BatteryMetric.valueOf(name.trim()));
            } catch (final IllegalArgumentException ignored) {
                // metric no longer exists, skip it
            }
        }
        return metrics;
    }

    private static String[] metricsToNames(final Set<BatteryMetric> metrics) {
        final String[] names = new String[metrics.size()];
        int i = 0;
        for (final BatteryMetric metric : metrics) {
            names[i++] = metric.name();
        }
        return names;
    }

    private String getXAxisDatePattern(final int totalDays) {
        if (totalDays <= 1) {
            return "HH:mm";
        }
        if (totalDays <= 7) {
            return "EEE dd";
        }
        if (totalDays <= 31) {
            return "dd.MM";
        }
        return "MM.yyyy";
    }

    private String formatMetricValue(final BatteryMetric metric, final double value, final boolean showUnit) {
        return valueFormatter.formatValue(value, metric.uomKey, showUnit).trim();
    }

    private String formatMetricRange(final BatteryMetric metric, final double min, final double max) {
        if (min >= max) {
            return formatMetricValue(metric, min, true);
        }
        return formatMetricValue(metric, min, false) + "–" + formatMetricValue(metric, max, true);
    }

    /**
     * Configures a real-valued (non-normalized) y-axis for the given metric's min/max, padding it
     * out to at least {@link BatteryMetric#minAxisSpan} so a nearly-flat series (e.g. idle current
     * fluctuating by a few mA) isn't stretched to fill the whole chart height.
     */
    private void configureRealAxis(final YAxis axis, final BatteryMetric metric, final float min, final float max) {
        axis.setEnabled(true);
        axis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(final float value) {
                return formatMetricValue(metric, value, false);
            }
        });

        if (metric == BatteryMetric.LEVEL) {
            // keep the familiar fixed 0-100% range for battery level
            axis.setAxisMinimum(0f);
            axis.setAxisMaximum(100f);
            return;
        }

        float axisMin = min;
        float axisMax = max;
        if (axisMax - axisMin < metric.minAxisSpan) {
            final float center = (axisMin + axisMax) / 2f;
            axisMin = center - metric.minAxisSpan / 2f;
            axisMax = center + metric.minAxisSpan / 2f;
        }
        final float padding = (axisMax - axisMin) * 0.1f;
        axis.setAxisMinimum(axisMin - padding);
        axis.setAxisMaximum(axisMax + padding);
    }

    /**
     * A small header of (icon, value) pairs - one per metric ever recorded for this battery
     * index, showing its overall most recent value - so the current readings are always visible,
     * regardless of which chips or time window are currently selected. Each pair is tinted to match
     * its line/chip.
     * <p>
     * {@link BatteryMetric#LEVEL} is deliberately skipped: it's already shown by the large
     * percentage label above this fragment, and repeating it here just duplicates that label.
     *
     * @return whether any (icon, value) pair was actually added
     */
    private boolean populateLatestValues(final LinearLayout container, final Set<BatteryMetric> availableMetrics, final Map<BatteryMetric, BatteryMetric.Sample> latestSampleByMetric) {
        container.removeAllViews();
        final LayoutInflater inflater = LayoutInflater.from(requireContext());
        boolean addedAny = false;
        for (final BatteryMetric metric : BatteryMetric.values()) {
            if (metric == BatteryMetric.LEVEL || !availableMetrics.contains(metric)) {
                continue;
            }
            final BatteryMetric.Sample latest = latestSampleByMetric.get(metric);
            if (latest == null) {
                continue;
            }
            final int metricColor = ContextCompat.getColor(requireContext(), metric.colorResId);

            final View item = inflater.inflate(R.layout.item_battery_metric_value, container, false);
            final ImageView icon = item.findViewById(R.id.battery_metric_value_icon);
            icon.setImageResource(metric.iconResId);
            icon.setColorFilter(metricColor);

            final TextView value = item.findViewById(R.id.battery_metric_value_text);
            value.setText(formatMetricValue(metric, latest.value(), true));
            value.setTextColor(metricColor);

            container.addView(item);
            addedAny = true;
        }
        return addedAny;
    }

    /**
     * A chart entry's y value formatter for a single battery metric. When several metrics are
     * plotted together, their raw values are normalized to a common 0-100 range so they can share
     * one hidden y-axis. This formatter converts a normalized value back to the real one before
     * formatting it, so the tap tooltip always shows real units.
     */
    private class MetricValueFormatter extends ValueFormatter {
        private final BatteryMetric metric;
        private final float min;
        private final float max;
        private final boolean normalized;

        MetricValueFormatter(final BatteryMetric metric, final float min, final float max, final boolean normalized) {
            this.metric = metric;
            this.min = min;
            this.max = max;
            this.normalized = normalized;
        }

        @Override
        public String getFormattedValue(final float value) {
            final float actual = normalized ? denormalize(value) : value;
            return formatMetricValue(metric, actual, false);
        }

        private float denormalize(final float normalizedY) {
            if (max <= min) {
                return min;
            }
            return min + (normalizedY / 100f) * (max - min);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class RefreshTask extends DBAccess {
        private final GBDevice device;
        private final int batteryIndex;
        private final int tsFrom;
        private final int tsTo;
        private final Set<BatteryMetric> requestedMetrics;

        private final Set<BatteryMetric> availableMetrics = EnumSet.noneOf(BatteryMetric.class);
        private Set<BatteryMetric> loadedMetrics = EnumSet.noneOf(BatteryMetric.class);
        private final Map<BatteryMetric, List<BatteryMetric.Sample>> samplesByMetric = new EnumMap<>(BatteryMetric.class);
        private final Map<BatteryMetric, BatteryMetric.Sample> latestSampleByMetric = new EnumMap<>(BatteryMetric.class);

        RefreshTask(final String task, final Context context, final GBDevice device, final int batteryIndex, final int tsFrom, final int tsTo, final Set<BatteryMetric> requestedMetrics) {
            super(task, context, false);
            this.device = device;
            this.batteryIndex = batteryIndex;
            this.tsFrom = tsFrom;
            this.tsTo = tsTo;
            this.requestedMetrics = EnumSet.copyOf(requestedMetrics);
        }

        @Override
        protected void doInBackground(final DBHandler handler) {
            for (final BatteryMetric metric : BatteryMetric.values()) {
                if (metric == BatteryMetric.LEVEL || metric.hasSamples(handler, device, batteryIndex)) {
                    availableMetrics.add(metric);
                    final BatteryMetric.Sample latest = metric.loadLatestSample(handler, device, batteryIndex);
                    if (latest != null) {
                        latestSampleByMetric.put(metric, latest);
                    }
                }
            }

            loadedMetrics = EnumSet.copyOf(requestedMetrics);
            loadedMetrics.retainAll(availableMetrics);
            if (loadedMetrics.isEmpty()) {
                loadedMetrics = EnumSet.of(BatteryMetric.LEVEL);
            }

            for (final BatteryMetric metric : loadedMetrics) {
                samplesByMetric.put(metric, metric.loadSamples(handler, device, batteryIndex, tsFrom * 1000L, tsTo * 1000L));
            }
        }

        @Override
        protected void onPostExecute(final Object o) {
            super.onPostExecute(o);
            if (getTaskError() != null || binding == null) {
                return;
            }

            if (!loadedMetrics.equals(selectedMetrics)) {
                selectedMetrics = loadedMetrics;
                saveSelectedMetrics(selectedMetrics);
            }

            rebuildChips(availableMetrics);
            renderChart();
        }

        private void renderChart() {
            final TimestampTranslation tsTranslation = new TimestampTranslation();
            tsTranslation.shorten(tsFrom);
            final float xMax = tsTranslation.shorten(tsTo);

            // only the metrics that actually have data in this window are plotted/axis-assigned;
            // a selected metric with no samples in range still shows its chip, just no line
            final List<BatteryMetric> orderedMetrics = new ArrayList<>();
            for (final BatteryMetric metric : BatteryMetric.values()) {
                final List<BatteryMetric.Sample> samples = samplesByMetric.get(metric);
                if (loadedMetrics.contains(metric) && samples != null && !samples.isEmpty()) {
                    orderedMetrics.add(metric);
                }
            }
            final boolean hasAnyData = !orderedMetrics.isEmpty();
            // 1 metric: its own real axis. 2 metrics: one real axis each (left/right). 3+: no
            // room for more real axes, so values are normalized onto one shared, hidden axis.
            final boolean dualAxis = orderedMetrics.size() == 2;
            final boolean normalized = orderedMetrics.size() >= 3;

            final List<LineDataSet> dataSets = new ArrayList<>();
            final List<ValueFormatter> markerFormatters = new ArrayList<>();
            final List<String> markerUnits = new ArrayList<>();

            BatteryMetric axisLeftMetric = null;
            float axisLeftMin = 0f;
            float axisLeftMax = 1f;
            BatteryMetric axisRightMetric = null;
            float axisRightMin = 0f;
            float axisRightMax = 1f;

            for (int i = 0; i < orderedMetrics.size(); i++) {
                final BatteryMetric metric = orderedMetrics.get(i);
                final List<BatteryMetric.Sample> rawSamples = Objects.requireNonNull(samplesByMetric.get(metric));

                final Accumulator accumulator = new Accumulator();
                for (final BatteryMetric.Sample sample : rawSamples) {
                    accumulator.add(sample.value());
                }
                final float min = (float) accumulator.getMin();
                final float max = (float) accumulator.getMax();

                final YAxis.AxisDependency axisDependency;
                if (dualAxis && i == 1) {
                    axisDependency = YAxis.AxisDependency.RIGHT;
                    axisRightMetric = metric;
                    axisRightMin = min;
                    axisRightMax = max;
                } else {
                    axisDependency = YAxis.AxisDependency.LEFT;
                    if (!normalized) {
                        axisLeftMetric = metric;
                        axisLeftMin = min;
                        axisLeftMax = max;
                    }
                }

                final List<Entry> entries = new ArrayList<>(rawSamples.size());
                for (final BatteryMetric.Sample sample : rawSamples) {
                    final float x = tsTranslation.shorten(sample.timestampSeconds());
                    final float y = normalized ? normalize(sample.value(), min, max) : sample.value();
                    entries.add(new Entry(x, y));
                }

                final String label = getString(
                        R.string.generic_metric_chart_label_with_unit,
                        getString(metric.labelResId),
                        formatMetricRange(metric, min, max)
                );

                final LineDataSet dataSet = new LineDataSet(entries, label);
                final int metricColor = ContextCompat.getColor(requireContext(), metric.colorResId);
                dataSet.setAxisDependency(axisDependency);
                dataSet.setColor(metricColor);
                dataSet.setCircleColor(metricColor);
                dataSet.setDrawCircleHole(false);
                dataSet.setCircleRadius(entries.size() > 30 ? 2.5f : 4f);
                dataSet.setDrawCircles(entries.size() <= 60);
                dataSet.setDrawValues(false);
                dataSet.setLineWidth(2f);
                dataSet.setValueTextColor(textColor);
                final MetricValueFormatter formatter = new MetricValueFormatter(metric, min, max, normalized);
                dataSet.setValueFormatter(formatter);
                dataSets.add(dataSet);
                markerFormatters.add(formatter);
                markerUnits.add(null);
            }

            binding.batteryChartEmpty.setVisibility(hasAnyData ? View.GONE : View.VISIBLE);
            // the header shows "live" readings, which are meaningless once the device has
            // disconnected and can no longer be trusted to be current
            final boolean deviceConnected = device.isConnected();
            final boolean hasLatestValues = deviceConnected
                    && populateLatestValues(binding.batteryChartLatestValues, availableMetrics, latestSampleByMetric);
            if (!deviceConnected) {
                binding.batteryChartLatestValues.removeAllViews();
            }
            binding.batteryChartLatestValues.setVisibility(hasLatestValues ? View.VISIBLE : View.GONE);

            final XAxis xAxis = binding.batteryChart.getXAxis();
            xAxis.setValueFormatter(new SampleXLabelFormatter(tsTranslation, getXAxisDatePattern(Math.max(1, (tsTo - tsFrom) / 86400))));
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(Math.max(1f, xMax));

            final YAxis yAxisLeft = binding.batteryChart.getAxisLeft();
            final YAxis yAxisRight = binding.batteryChart.getAxisRight();
            if (normalized || !hasAnyData) {
                yAxisLeft.setEnabled(!normalized);
                yAxisLeft.setValueFormatter(null);
                yAxisLeft.setAxisMinimum(normalized ? -5f : 0f);
                yAxisLeft.setAxisMaximum(normalized ? 105f : 1f);
                yAxisRight.setEnabled(false);
            } else {
                configureRealAxis(yAxisLeft, axisLeftMetric, axisLeftMin, axisLeftMax);
                if (dualAxis) {
                    configureRealAxis(yAxisRight, axisRightMetric, axisRightMin, axisRightMax);
                } else {
                    yAxisRight.setEnabled(false);
                }
            }

            final LineData lineData = new LineData();
            for (final LineDataSet dataSet : dataSets) {
                lineData.addDataSet(dataSet);
            }
            final CombinedData combinedData = new CombinedData();
            combinedData.setData(lineData);

            binding.batteryChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
            binding.batteryChart.setData(combinedData);
            binding.batteryChart.setMarker(new ValueMarker(requireContext(), combinedData, markerFormatters, markerUnits));
            binding.batteryChart.animateX(500, Easing.EaseInOutQuart);
            binding.batteryChart.invalidate();
        }

        private float normalize(final float value, final float min, final float max) {
            if (max <= min) {
                return 50f;
            }
            return (value - min) / (max - min) * 100f;
        }
    }
}
