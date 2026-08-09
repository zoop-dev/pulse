/*  Copyright (C) 2024-2026 a0z, José Rebelo, Thomas Kuehne

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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.AbstractDashboardVO2MaxWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.Vo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class VO2MaxPeriodFragment extends AbstractChartFragment<VO2MaxPeriodFragment.VO2MaxData> {
    protected static final Logger LOG = LoggerFactory.getLogger(VO2MaxPeriodFragment.class);

    private static final String ARG_TOTAL_DAYS = "totalDays";
    private static final String ARG_SHOW_GAUGES = "showGauges";
    private static final int DEFAULT_TOTAL_DAYS = 30;
    private static final float MIN_VALUE_LABEL_SPACING_DP = 32f;
    private static final float ESTIMATED_Y_AXIS_WIDTH_DP = 40f;

    private TextView mDateView;
    private LineChart vo2MaxChart;
    private int totalDays;
    private boolean showGauges;
    private float density;
    private final Set<Entry> labeledEntries = new HashSet<>();
    GBDevice device;

    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int TEXT_COLOR;

    private TextView vo2MaxRunningValue;
    private TextView vo2MaxCyclingValue;
    private TextView vo2MaxValue;
    private ImageView vo2MaxRunningGauge;
    private ImageView vo2MaxCyclingGauge;
    private ImageView vo2MaxGauge;
    protected GaugeDrawer gaugeDrawer = new GaugeDrawer();
    private RelativeLayout vo2maxCyclingWrapper;
    private RelativeLayout vo2maxRunningWrapper;
    private RelativeLayout vo2maxWrapper;
    private GridLayout tilesGridWrapper;

    public static VO2MaxPeriodFragment newInstance(final int totalDays, final boolean showGauges) {
        final VO2MaxPeriodFragment fragment = new VO2MaxPeriodFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_TOTAL_DAYS, totalDays);
        args.putBoolean(ARG_SHOW_GAUGES, showGauges);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final int layoutRes = showGauges ? R.layout.fragment_vo2max_month : R.layout.fragment_vo2max_period;
        View rootView = inflater.inflate(layoutRes, container, false);

        mDateView = rootView.findViewById(R.id.vo2max_date_view);
        vo2MaxChart = rootView.findViewById(R.id.vo2max_chart);
        device = getChartsHost().getDevice();

        if (showGauges) {
            vo2MaxRunningValue = rootView.findViewById(R.id.vo2max_running_gauge_value);
            vo2MaxCyclingValue = rootView.findViewById(R.id.vo2max_cycling_gauge_value);
            vo2MaxValue = rootView.findViewById(R.id.vo2max_gauge_value);
            vo2MaxRunningGauge = rootView.findViewById(R.id.vo2max_running_gauge);
            vo2MaxCyclingGauge = rootView.findViewById(R.id.vo2max_cycling_gauge);
            vo2MaxGauge = rootView.findViewById(R.id.vo2max_gauge);
            vo2maxCyclingWrapper = rootView.findViewById(R.id.vo2max_cycling_card_layout);
            vo2maxRunningWrapper = rootView.findViewById(R.id.vo2max_running_card_layout);
            vo2maxWrapper = rootView.findViewById(R.id.vo2max_card_layout);
            tilesGridWrapper = rootView.findViewById(R.id.tiles_grid_wrapper);
            if (!supportsVO2MultiSport(device)) {
                tilesGridWrapper.removeView(vo2maxCyclingWrapper);
                tilesGridWrapper.removeView(vo2maxRunningWrapper);
            } else {
                tilesGridWrapper.removeView(vo2maxWrapper);
            }
        }

        setupVO2MaxChart();
        refresh();
        setupLegend(vo2MaxChart);

        return rootView;
    }

    public boolean supportsVO2MultiSport(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.supportsVO2MultiSport(device);
    }

    @Override
    public String getTitle() {
        return getString(R.string.menuitem_vo2_max);
    }

    @Override
    protected boolean isSingleDay() {
        return totalDays == 1;
    }

    @Override
    protected int getTSStart() {
        return DateTimeUtils.shiftDays(getTSEnd(), -totalDays + 1);
    }

    @Override
    protected void init() {
        totalDays = getArguments() != null ? getArguments().getInt(ARG_TOTAL_DAYS, DEFAULT_TOTAL_DAYS) : DEFAULT_TOTAL_DAYS;
        showGauges = getArguments() != null && getArguments().getBoolean(ARG_SHOW_GAUGES, false);
        density = getResources().getDisplayMetrics().density;
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
    }

    @Override
    protected VO2MaxData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        final Date rangeStart = DateTimeUtils.dayStart(new Date(getTSStart() * 1000L));
        final Date rangeEnd = DateTimeUtils.dayEnd(new Date(getTSEnd() * 1000L));
        final int tsFrom = (int) (rangeStart.getTime() / 1000L);
        final int tsTo = (int) (rangeEnd.getTime() / 1000L);

        List<VO2MaxRecord> records = new ArrayList<>();
        List<? extends Vo2MaxSample> samples = getAllSamples(db, device, tsFrom, tsTo);
        for (Vo2MaxSample sample : samples) {
            records.add(new VO2MaxRecord(sample.getTimestamp() / 1000, sample.getValue(), sample.getType()));
        }

        Map<Vo2MaxSample.Type, VO2MaxRecord> latestValues = null;
        if (showGauges) {
            latestValues = new HashMap<>();
            for (Vo2MaxSample.Type type : Vo2MaxSample.Type.values()) {
                Vo2MaxSample sample = getLatestVo2MaxSample(db, device, type);
                if (sample != null) {
                    latestValues.put(type, new VO2MaxRecord(sample.getTimestamp() / 1000, sample.getValue(), type));
                }
            }
        }

        return new VO2MaxData(records, tsFrom, tsTo, latestValues);
    }

    @Override
    protected void updateChartsnUIThread(VO2MaxData vo2MaxData) {
        mDateView.setText(DateTimeUtils.formatDaysUntil(totalDays, getTSEnd()));

        final TimestampTranslation tsTranslation = new TimestampTranslation();
        tsTranslation.shorten(vo2MaxData.tsFrom);

        List<Entry> runningEntries = new ArrayList<>();
        List<Entry> cyclingEntries = new ArrayList<>();
        List<Entry> allEntries = new ArrayList<>();
        final Accumulator accumulator = new Accumulator();
        vo2MaxData.records.forEach((record) -> {
            final int x = tsTranslation.shorten((int) record.timestamp);
            Entry entry = new Entry(x, record.value);
            allEntries.add(entry);
            accumulator.add(record.value);
            switch (record.type) {
                case RUNNING:
                    runningEntries.add(entry);
                    break;
                case CYCLING:
                    cyclingEntries.add(entry);
                    break;
            }
        });
        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        if (supportsVO2MultiSport(device)) {
            lineDataSets.add(createDataSet(runningEntries, getResources().getColor(R.color.vo2max_running_char_line_color), getString(R.string.vo2max_running)));
            lineDataSets.add(createDataSet(cyclingEntries, getResources().getColor(R.color.vo2max_cycling_char_line_color), getString(R.string.vo2max_cycling)));
        } else {
            lineDataSets.add(createDataSet(allEntries, getResources().getColor(R.color.vo2max_running_char_line_color), getString(R.string.menuitem_vo2_max)));
        }
        final LineData lineData = new LineData(lineDataSets);
        vo2MaxChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "dd/MM"));
        vo2MaxChart.getXAxis().setAxisMinimum(0f);
        vo2MaxChart.getXAxis().setAxisMaximum(tsTranslation.shorten(vo2MaxData.tsTo));
        if (accumulator.getCount() > 0) {
            vo2MaxChart.getAxisLeft().setAxisMinimum(Math.max(0, (float) accumulator.getMin() - 2));
            vo2MaxChart.getAxisLeft().setAxisMaximum(Math.min(100, (float) accumulator.getMax() + 2));
        }
        vo2MaxChart.setData(lineData);
        updateValueLabelVisibility(true);

        if (showGauges) {
            updateGaugeTiles(vo2MaxData);
        }
    }

    private void updateGaugeTiles(final VO2MaxData vo2MaxData) {
        final int[] colors = AbstractDashboardVO2MaxWidget.getColors();
        final float[] segments = AbstractDashboardVO2MaxWidget.getSegments();
        final ActivityUser activityUser = new ActivityUser();
        final int age = activityUser.getAgeAt(LocalDate.ofInstant(getEndDate().toInstant(), ZoneId.systemDefault()));
        if (supportsVO2MultiSport(device)) {
            // Running
            VO2MaxRecord latestRunningRecord = vo2MaxData.getLatestValue(Vo2MaxSample.Type.RUNNING);
            float runningVO2MaxValue = VO2MaxRanges.INSTANCE.calculateVO2MaxPercentile(latestRunningRecord != null ? latestRunningRecord.value : 0, age, activityUser.getGender());
            vo2MaxRunningValue.setText(latestRunningRecord != null ? formatVO2MaxValue(latestRunningRecord.value) : "-");
            gaugeDrawer.drawSegmentedGauge(vo2MaxRunningGauge, colors, segments, runningVO2MaxValue, false, true);

            // Cycling
            VO2MaxRecord latestCyclingRecord = vo2MaxData.getLatestValue(Vo2MaxSample.Type.CYCLING);
            float cyclingVO2MaxValue = VO2MaxRanges.INSTANCE.calculateVO2MaxPercentile(latestCyclingRecord != null ? latestCyclingRecord.value : 0, age, activityUser.getGender());
            gaugeDrawer.drawSegmentedGauge(vo2MaxCyclingGauge, colors, segments, cyclingVO2MaxValue, false, true);
            vo2MaxCyclingValue.setText(latestCyclingRecord != null ? formatVO2MaxValue(latestCyclingRecord.value) : "-");
        } else {
            VO2MaxRecord latestRecord = vo2MaxData.getLatestValue(Vo2MaxSample.Type.ANY);
            float vO2MaxValue = VO2MaxRanges.INSTANCE.calculateVO2MaxPercentile(latestRecord != null ? latestRecord.value : 0, age, activityUser.getGender());
            gaugeDrawer.drawSegmentedGauge(vo2MaxGauge, colors, segments, vO2MaxValue, false, true);
            vo2MaxValue.setText(latestRecord != null ? formatVO2MaxValue(latestRecord.value) : "-");
        }
    }

    /**
     * Update value labels visibility.
     */
    private void updateValueLabelVisibility(final boolean justLoadedData) {
        labeledEntries.clear();

        final LineData lineData = vo2MaxChart.getData();
        if (lineData == null) {
            vo2MaxChart.invalidate();
            return;
        }

        final float lowestVisibleX;
        final float highestVisibleX;
        if (justLoadedData) {
            lowestVisibleX = vo2MaxChart.getXAxis().getAxisMinimum();
            highestVisibleX = vo2MaxChart.getXAxis().getAxisMaximum();
        } else {
            lowestVisibleX = vo2MaxChart.getLowestVisibleX();
            highestVisibleX = vo2MaxChart.getHighestVisibleX();
        }

        float contentWidthPx = vo2MaxChart.getViewPortHandler().contentWidth();
        if (contentWidthPx <= 0) {
            // The view itself may also not have been laid out yet on a first load.
            contentWidthPx = getResources().getDisplayMetrics().widthPixels - ESTIMATED_Y_AXIS_WIDTH_DP * density;
        }

        final List<Entry> visibleSorted = new ArrayList<>();
        for (final ILineDataSet dataSet : lineData.getDataSets()) {
            for (int i = 0; i < dataSet.getEntryCount(); i++) {
                final Entry entry = dataSet.getEntryForIndex(i);
                if (entry.getX() >= lowestVisibleX && entry.getX() <= highestVisibleX) {
                    visibleSorted.add(entry);
                }
            }
        }
        if (visibleSorted.isEmpty()) {
            vo2MaxChart.invalidate();
            return;
        }
        Collections.sort(visibleSorted, (a, b) -> Float.compare(a.getX(), b.getX()));

        final int maxLabelSlots = contentWidthPx > 0
                ? Math.max(2, (int) (contentWidthPx / (MIN_VALUE_LABEL_SPACING_DP * density)))
                : visibleSorted.size();

        if (visibleSorted.size() <= maxLabelSlots) {
            labeledEntries.addAll(visibleSorted);
        } else {
            final int lastIndex = visibleSorted.size() - 1;
            for (int slot = 0; slot < maxLabelSlots; slot++) {
                final int index = Math.round(slot * (float) lastIndex / (maxLabelSlots - 1));
                labeledEntries.add(visibleSorted.get(index));
            }
        }

        vo2MaxChart.invalidate();
    }

    protected LineDataSet createDataSet(final List<Entry> values, int color, String label) {
        final LineDataSet lineDataSet = new LineDataSet(values, label);
        lineDataSet.setColor(color);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setCircleColor(color);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(true);
        lineDataSet.setValueTextSize(10f);
        lineDataSet.setValueTextColor(TEXT_COLOR);
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(final Entry entry) {
                // Blank out points that updateValueLabelVisibility() decided are too close to
                // an already-labeled neighbor at the current zoom level.
                return labeledEntries.contains(entry) ? formatVO2MaxValue(entry.getY()) : "";
            }
        });
        return lineDataSet;
    }

    private static String formatVO2MaxValue(final float value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    @Override
    protected void renderCharts() {
        vo2MaxChart.invalidate();
    }

    public List<? extends Vo2MaxSample> getAllSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends Vo2MaxSample> sampleProvider = coordinator.getVo2MaxSampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    public Vo2MaxSample getLatestVo2MaxSample(final DBHandler db, final GBDevice device, Vo2MaxSample.Type type) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final Vo2MaxSampleProvider sampleProvider = (Vo2MaxSampleProvider) coordinator.getVo2MaxSampleProvider(device, db.getDaoSession());
        return sampleProvider.getLatestSample(type, getTSEnd() * 1000L);
    }

    private void setupVO2MaxChart() {
        final XAxis xAxisBottom = vo2MaxChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisLeft = vo2MaxChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = vo2MaxChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);

        vo2MaxChart.setMaxVisibleValueCount(Integer.MAX_VALUE);
        vo2MaxChart.setScaleXEnabled(true);
        vo2MaxChart.setScaleYEnabled(false);
        vo2MaxChart.setDragEnabled(true);
        vo2MaxChart.setDoubleTapToZoomEnabled(true);
        vo2MaxChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                updateValueLabelVisibility(false);
            }

            @Override
            public void onChartLongPressed(MotionEvent me) {
            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
                updateValueLabelVisibility(false);
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                updateValueLabelVisibility(false);
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                updateValueLabelVisibility(false);
            }
        });
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
    }

    protected static class VO2MaxRecord {
        float value;
        long timestamp;
        Vo2MaxSample.Type type;

        protected VO2MaxRecord(long timestamp, float value, Vo2MaxSample.Type type) {
            this.timestamp = timestamp;
            this.value = value;
            this.type = type;
        }
    }

    protected static class VO2MaxData extends ChartsData {
        private final List<? extends VO2MaxRecord> records;
        private final int tsFrom;
        private final int tsTo;
        @Nullable
        private final Map<Vo2MaxSample.Type, VO2MaxRecord> latestValues;

        protected VO2MaxData(List<? extends VO2MaxRecord> records, int tsFrom, int tsTo, @Nullable Map<Vo2MaxSample.Type, VO2MaxRecord> latestValues) {
            this.records = records;
            this.tsFrom = tsFrom;
            this.tsTo = tsTo;
            this.latestValues = latestValues;
        }

        @Nullable
        public VO2MaxRecord getLatestValue(Vo2MaxSample.Type type) {
            return latestValues != null ? latestValues.get(type) : null;
        }
    }
}
