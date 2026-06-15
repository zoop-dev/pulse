/*  Copyright (C) 2026 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts.DefaultWorkoutCharts;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericMetricSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.MetricSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class GenericMetricChartFragment extends AbstractChartFragment<GenericMetricChartFragment.GenericMetricChartsData> {
    private static final String ARG_TOTAL_DAYS = "totalDays";
    private static final int DEFAULT_TOTAL_DAYS = 30;
    private static final String STATE_SELECTED_METRIC = "selectedMetric";

    private LineChart chart;
    private Spinner metricSpinner;
    private TextView timeSpanText;
    private TextView averageText;
    private TextView minimumText;
    private TextView maximumText;
    private TextView sampleCountText;

    private int chartTextColor;
    private int lineColor;
    private int textColor;
    private int backgroundColor;

    private int totalDays;
    private List<MetricSample.Metric> metrics = Collections.emptyList();
    private MetricSample.Metric selectedMetric;
    private WorkoutValueFormatter valueFormatter;

    public static GenericMetricChartFragment newInstance(final int totalDays) {
        final GenericMetricChartFragment fragment = new GenericMetricChartFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_TOTAL_DAYS, totalDays);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public String getTitle() {
        return getString(R.string.generic_metrics);
    }

    @Override
    protected void init() {
        totalDays = getArguments() != null ? getArguments().getInt(ARG_TOTAL_DAYS, DEFAULT_TOTAL_DAYS) : DEFAULT_TOTAL_DAYS;
        chartTextColor = GBApplication.getSecondaryTextColor(requireContext());
        textColor = GBApplication.getTextColor(requireContext());
        backgroundColor = GBApplication.getBackgroundColor(requireContext());
        lineColor = getResources().getColor(R.color.accent);
        valueFormatter = new WorkoutValueFormatter();
    }

    @Override
    protected boolean isSingleDay() {
        return totalDays == 1;
    }

    @Override
    protected GenericMetricChartsData refreshInBackground(final ChartsHost chartsHost, final DBHandler db, final GBDevice device) {
        final MetricSample.Metric metric = selectedMetric;
        final Date rangeStart = DateTimeUtils.dayStart(new Date(getTSStart() * 1000L));
        final Date rangeEnd = DateTimeUtils.dayEnd(new Date(getTSEnd() * 1000L));

        if (metric == null) {
            return GenericMetricChartsData.empty(null, createXValueFormatter(rangeStart), rangeEnd);
        }

        final List<? extends MetricSample> samples = GenericMetricSampleProvider.getMetricSamples(
                db,
                device,
                metric,
                rangeStart.getTime(),
                rangeEnd.getTime()
        );
        return createChartsData(metric, samples, rangeStart, rangeEnd);
    }

    @Override
    protected void renderCharts() {
        chart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
    }

    @Override
    protected void setupLegend(final Chart<?> chart) {
        chart.getLegend().setTextColor(textColor);
        chart.getLegend().setWordWrapEnabled(true);
    }

    @Override
    protected void updateChartsnUIThread(final GenericMetricChartsData chartsData) {
        if (chartsData.metric != selectedMetric) {
            return;
        }

        if (totalDays == 1) {
            timeSpanText.setText(DateTimeUtils.formatDate(getEndDate(), DateUtils.FORMAT_SHOW_WEEKDAY));
        } else {
            timeSpanText.setText(DateTimeUtils.formatDaysUntil(totalDays, getTSEnd()));
        }

        final XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(chartsData.xValueFormatter);
        xAxis.setAxisMinimum(chartsData.xMin);
        xAxis.setAxisMaximum(chartsData.xMax);

        chart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317

        if (chartsData.hasData()) {
            setYAxisRange(chartsData.yMin, chartsData.yMax);
            chart.setData(chartsData.lineData);
            averageText.setText(formatMetricValue(chartsData.metric, chartsData.averageValue, false));
            minimumText.setText(formatMetricValue(chartsData.metric, chartsData.yMin, false));
            maximumText.setText(formatMetricValue(chartsData.metric, chartsData.yMax, false));
        } else {
            chart.getAxisLeft().setAxisMinimum(0f);
            chart.getAxisLeft().setAxisMaximum(1f);
            averageText.setText(R.string.stats_empty_value);
            minimumText.setText(R.string.stats_empty_value);
            maximumText.setText(R.string.stats_empty_value);
        }

        sampleCountText.setText(String.valueOf(chartsData.sampleCount));
    }

    @Override
    protected int getTSStart() {
        return DateTimeUtils.shiftDays(getTSEnd(), -totalDays + 1);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_generic_metric_chart, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        metricSpinner = rootView.findViewById(R.id.generic_metric_spinner);
        timeSpanText = rootView.findViewById(R.id.generic_metric_time_span_text);
        chart = rootView.findViewById(R.id.generic_metric_chart);
        averageText = rootView.findViewById(R.id.generic_metric_average_value);
        minimumText = rootView.findViewById(R.id.generic_metric_minimum_value);
        maximumText = rootView.findViewById(R.id.generic_metric_maximum_value);
        sampleCountText = rootView.findViewById(R.id.generic_metric_sample_count);

        metrics = getAvailableMetrics(getChartsHost().getDevice());
        selectedMetric = getInitialMetric(savedInstanceState);

        setupMetricSpinner();
        setupChart();

        refresh();

        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedMetric != null) {
            outState.putString(STATE_SELECTED_METRIC, selectedMetric.name());
        }
    }

    private List<MetricSample.Metric> getAvailableMetrics(final GBDevice device) {
        final Set<MetricSample.Metric> supportedMetrics = GenericMetricSampleProvider.supportsMetrics(device);
        final List<MetricSample.Metric> availableMetrics = new ArrayList<>();
        for (final MetricSample.Metric metric : MetricSample.Metric.values()) {
            if (metric != MetricSample.Metric.UNKNOWN && supportedMetrics.contains(metric)) {
                availableMetrics.add(metric);
            }
        }
        return availableMetrics;
    }

    private MetricSample.Metric getInitialMetric(final Bundle savedInstanceState) {
        if (metrics.isEmpty()) {
            return null;
        }

        if (savedInstanceState != null) {
            final String savedMetricName = savedInstanceState.getString(STATE_SELECTED_METRIC);
            if (savedMetricName != null) {
                try {
                    final MetricSample.Metric savedMetric = MetricSample.Metric.valueOf(savedMetricName);
                    if (metrics.contains(savedMetric)) {
                        return savedMetric;
                    }
                } catch (final IllegalArgumentException ignored) {
                    // Fall back to the first recorded metric.
                }
            }
        }

        return metrics.get(0);
    }

    private void setupMetricSpinner() {
        final List<MetricItem> metricItems = new ArrayList<>();
        for (final MetricSample.Metric metric : metrics) {
            metricItems.add(new MetricItem(metric, getMetricLabel(metric)));
        }

        final ArrayAdapter<MetricItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, metricItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        metricSpinner.setAdapter(adapter);
        metricSpinner.setEnabled(metricItems.size() > 1);
        metricSpinner.setPrompt(getString(R.string.generic_metric_select_metric));

        if (selectedMetric != null) {
            metricSpinner.setSelection(metrics.indexOf(selectedMetric));
        }

        metricSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                final MetricSample.Metric metric = ((MetricItem) parent.getItemAtPosition(position)).metric;
                if (metric != selectedMetric) {
                    selectedMetric = metric;
                    refresh();
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // Keep the previous metric selected.
            }
        });
    }

    private void setupChart() {
        configureBarLineChartDefaults(chart);
        chart.setBackgroundColor(backgroundColor);
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(chartTextColor);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(true);
        xAxis.setAvoidFirstLastClipping(true);

        final YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextColor(chartTextColor);
        yAxis.setDrawGridLines(true);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(1f);
    }

    private GenericMetricChartsData createChartsData(final MetricSample.Metric metric, final List<? extends MetricSample> samples, final Date rangeStart, final Date rangeEnd) {
        final List<Entry> entries = new ArrayList<>();
        final SampleXLabelFormatter formatter = createXValueFormatter(rangeStart);
        final TimestampTranslation tsTranslation = formatter.getTsTranslation();
        float yMin = Float.MAX_VALUE;
        float yMax = -Float.MAX_VALUE;
        float totalValue = 0f;

        for (final MetricSample sample : samples) {
            final int tsSeconds = (int) (sample.getTimestamp() / 1000L);
            final float score = (float) sample.getMetricScore();
            entries.add(new Entry(tsTranslation.shorten(tsSeconds), score));
            yMin = Math.min(yMin, score);
            yMax = Math.max(yMax, score);
            totalValue += score;
        }

        if (entries.isEmpty()) {
            return GenericMetricChartsData.empty(metric, formatter, rangeEnd);
        }

        final LineDataSet dataSet = createDataSet(metric, entries);
        final float averageValue = totalValue / entries.size();
        return new GenericMetricChartsData(metric, new LineData(dataSet), formatter, getXMin(), getXMax(formatter, rangeEnd), yMin, yMax, averageValue, entries.size());
    }

    private SampleXLabelFormatter createXValueFormatter(final Date rangeStart) {
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        tsTranslation.shorten((int) (rangeStart.getTime() / 1000L));
        return new SampleXLabelFormatter(tsTranslation, getXAxisDatePattern());
    }

    private float getXMin() {
        return 0f;
    }

    private float getXMax(final SampleXLabelFormatter formatter, final Date rangeEnd) {
        return formatter.getTsTranslation().shorten((int) (rangeEnd.getTime() / 1000L));
    }

    private String getXAxisDatePattern() {
        if (totalDays == 1) {
            return "HH:mm";
        }
        if (totalDays <= 7) {
            return "EEE";
        }
        return "dd";
    }

    private LineDataSet createDataSet(final MetricSample.Metric metric, final List<Entry> entries) {
        final LineDataSet dataSet = new LineDataSet(entries, getMetricLabelWithUnit(metric));
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(lineColor);
        dataSet.setCircleColor(lineColor);
        dataSet.setDrawCircleHole(false);
        dataSet.setCircleRadius(entries.size() > 30 ? 2.5f : 4f);
        dataSet.setDrawCircles(entries.size() <= 60);
        dataSet.setDrawValues(entries.size() <= 12);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(final Entry entry) {
                return formatMetricValue(metric, entry.getY(), false);
            }
        });
        return dataSet;
    }

    private void setYAxisRange(final float min, final float max) {
        final float range = max - min;
        final float padding = range == 0f ? Math.max(1f, Math.abs(max) * 0.1f) : range * 0.1f;
        chart.getAxisLeft().setAxisMinimum(Math.max(0f, min - padding));
        chart.getAxisLeft().setAxisMaximum(max + padding);
    }

    private String getMetricLabelWithUnit(final MetricSample.Metric metric) {
        final String unit = DefaultWorkoutCharts.getUnitString(requireContext(), metric.uomKey);
        if (unit.isEmpty()) {
            return getMetricLabel(metric);
        }
        return getString(R.string.generic_metric_chart_label_with_unit, getMetricLabel(metric), unit);
    }

    private String formatMetricValue(final MetricSample.Metric metric, final double value, final boolean showUnit) {
        return valueFormatter.formatValue(value, metric.uomKey, showUnit).trim();
    }

    private String getMetricLabel(final MetricSample.Metric metric) {
        if (metric.labelResId == 0) {
            return metric.name();
        }
        return getString(metric.labelResId);
    }

    protected static class GenericMetricChartsData extends ChartsData {
        private final MetricSample.Metric metric;
        private final LineData lineData;
        private final ValueFormatter xValueFormatter;
        private final float xMin;
        private final float xMax;
        private final float yMin;
        private final float yMax;
        private final float averageValue;
        private final int sampleCount;

        private GenericMetricChartsData(final MetricSample.Metric metric, final LineData lineData, final ValueFormatter xValueFormatter, final float xMin, final float xMax, final float yMin, final float yMax, final float averageValue, final int sampleCount) {
            this.metric = metric;
            this.lineData = lineData;
            this.xValueFormatter = xValueFormatter;
            this.xMin = xMin;
            this.xMax = xMax;
            this.yMin = yMin;
            this.yMax = yMax;
            this.averageValue = averageValue;
            this.sampleCount = sampleCount;
        }

        private static GenericMetricChartsData empty(final MetricSample.Metric metric, final SampleXLabelFormatter formatter, final Date rangeEnd) {
            final TimestampTranslation tsTranslation = formatter.getTsTranslation();
            return new GenericMetricChartsData(metric, null, formatter, 0f, tsTranslation.shorten((int) (rangeEnd.getTime() / 1000L)), 0f, 1f, 0f, 0);
        }

        private boolean hasData() {
            return lineData != null && lineData.getEntryCount() > 0;
        }
    }

    private static class MetricItem {
        private final MetricSample.Metric metric;
        private final String label;

        private MetricItem(final MetricSample.Metric metric, final String label) {
            this.metric = metric;
            this.label = label;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }
}
