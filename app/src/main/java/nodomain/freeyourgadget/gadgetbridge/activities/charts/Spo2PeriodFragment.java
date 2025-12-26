/*  Copyright (C) 2023-2024 Martin.JM, a0z

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

import android.graphics.Paint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class Spo2PeriodFragment extends AbstractChartFragment<Spo2PeriodFragment.Spo2PeriodData> {
    protected static final Logger LOG = LoggerFactory.getLogger(Spo2PeriodFragment.class);

    static int SEC_PER_DAY = 24 * 60 * 60;
    static int DATA_INVALID = -1;

    private int BACKGROUND_COLOR;
    private int CHART_TEXT_COLOR;
    private int LEGEND_TEXT_COLOR;
    private int SPO2_COLOR;
    private int SPO2_AVG_COLOR;

    private TextView mDateView;
    private TextView spo2Minimum;
    private TextView spo2Maximum;
    private TextView spo2Average;
    private CombinedChart spo2Chart;
    private int TOTAL_DAYS;

    @Override
    protected boolean isSingleDay() {
        return false;
    }

    public static Spo2PeriodFragment newInstance(int totalDays) {
        Spo2PeriodFragment fragment = new Spo2PeriodFragment();
        Bundle args = new Bundle();
        args.putInt("totalDays", totalDays);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOTAL_DAYS = getArguments() != null ? getArguments().getInt("totalDays") : 7;
    }

    @Override
    protected void init() {
        TypedValue runningColor = new TypedValue();
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        SPO2_COLOR = ContextCompat.getColor(requireContext(), R.color.spo2_color);
        requireContext().getTheme().resolveAttribute(R.attr.spo2_avg_color, runningColor, true);
        SPO2_AVG_COLOR = runningColor.data;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_spo2_period, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        mDateView = rootView.findViewById(R.id.date_view);
        spo2Minimum = rootView.findViewById(R.id.spo2_minimum);
        spo2Maximum = rootView.findViewById(R.id.spo2_maximum);
        spo2Average = rootView.findViewById(R.id.spo2_average);
        spo2Chart = rootView.findViewById(R.id.spo2_chart);

        setupChart();
        refresh();
        setupLegend(spo2Chart);

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.pref_header_spo2);
    }

    private int getStartTs() {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        return (int) (day.getTimeInMillis() / 1000) - SEC_PER_DAY * (TOTAL_DAYS - 1);
    }

    private Spo2DayData fetchSpo2DataForDay(DBHandler db, GBDevice device, int startTs) {
        int endTs = startTs + SEC_PER_DAY - 1;
        List<? extends Spo2Sample> samples = getSamples(db, device, startTs, endTs);

        final Accumulator accumulator = new Accumulator();
        for (final Spo2Sample sample : samples) {
            if (sample.getSpo2() > 0) {
                accumulator.add(sample.getSpo2());
            }
        }

        final int average = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getAverage()) : DATA_INVALID;
        final int minimum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMin()) : DATA_INVALID;
        final int maximum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMax()) : DATA_INVALID;

        return new Spo2DayData(average, minimum, maximum);
    }

    @Override
    protected Spo2PeriodData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        final int startTs = getStartTs();

        List<Spo2DayData> result = new ArrayList<>();
        for (int i = 0; i < TOTAL_DAYS; i++) {
            Spo2DayData dayData = fetchSpo2DataForDay(db, device, startTs + i * SEC_PER_DAY);
            result.add(dayData);
        }
        return new Spo2PeriodData(result);
    }

    private List<? extends Spo2Sample> getSamples(DBHandler db, GBDevice device, int startTs, int endTs) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends Spo2Sample> sampleProvider = coordinator.getSpo2SampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(startTs * 1000L, endTs * 1000L);
    }

    @Override
    protected void updateChartsnUIThread(Spo2PeriodData data) {
        final int startTs = getStartTs();
        mDateView.setText(DateTimeUtils.formatDaysUntil(TOTAL_DAYS, getTSEnd()));

        final Accumulator avgAccumulator = new Accumulator();
        final Accumulator minAccumulator = new Accumulator();
        final Accumulator maxAccumulator = new Accumulator();

        final ArrayList<CandleEntry> candleEntries = new ArrayList<>();
        final ArrayList<Entry> avgEntries = new ArrayList<>();

        for (int i = 0; i < data.days.size(); i++) {
            final Spo2DayData dayData = data.days.get(i);
            if (dayData.minimum > 0 && dayData.maximum > 0) {
                avgAccumulator.add(dayData.average);
                minAccumulator.add(dayData.minimum);
                maxAccumulator.add(dayData.maximum);
                // CandleEntry: x, shadowH (high), shadowL (low), open, close
                candleEntries.add(new CandleEntry(i, dayData.maximum, dayData.minimum, dayData.minimum, dayData.maximum));
                // Scatter entry for daily average
                avgEntries.add(new Entry(i, dayData.average));
            }
        }

        final String emptyValue = requireContext().getString(R.string.stats_empty_value);
        final int average = avgAccumulator.getCount() > 0 ? (int) Math.round(avgAccumulator.getAverage()) : DATA_INVALID;
        final int minimum = minAccumulator.getCount() > 0 ? (int) Math.round(minAccumulator.getMin()) : DATA_INVALID;
        final int maximum = maxAccumulator.getCount() > 0 ? (int) Math.round(maxAccumulator.getMax()) : DATA_INVALID;

        spo2Minimum.setText(minimum > 0 ? getString(R.string.battery_percentage_str, String.valueOf(minimum)) : emptyValue);
        spo2Maximum.setText(maximum > 0 ? getString(R.string.battery_percentage_str, String.valueOf(maximum)) : emptyValue);
        spo2Average.setText(average > 0 ? getString(R.string.battery_percentage_str, String.valueOf(average)) : emptyValue);

        final String fmt = TOTAL_DAYS == 7 ? "EEE" : "dd";
        SimpleDateFormat formatDay = new SimpleDateFormat(fmt, Locale.getDefault());
        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int dayIndex = Math.round(value);
                if (dayIndex < 0 || dayIndex >= TOTAL_DAYS) {
                    return "";
                }
                int ts = startTs + SEC_PER_DAY * dayIndex;
                return formatDay.format(new Date(ts * 1000L));
            }
        };
        spo2Chart.getXAxis().setValueFormatter(formatter);

        if (minimum > 0) {
            spo2Chart.getAxisLeft().setAxisMinimum(Math.max(5 * ((minimum - 5) / 5), 0));
        }

        final CombinedData combinedData = new CombinedData();

        // Candle data for range bars
        if (!candleEntries.isEmpty()) {
            CandleDataSet candleDataSet = new CandleDataSet(candleEntries, getString(R.string.pref_header_spo2));
            candleDataSet.setDrawValues(false);
            candleDataSet.setDrawIcons(false);
            candleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            candleDataSet.setShadowColor(SPO2_COLOR);
            candleDataSet.setShadowWidth(2f);
            candleDataSet.setDecreasingColor(SPO2_COLOR);
            candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
            candleDataSet.setIncreasingColor(SPO2_COLOR);
            candleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
            candleDataSet.setNeutralColor(SPO2_COLOR);
            candleDataSet.setBarSpace(0.15f);
            candleDataSet.setShowCandleBar(true);
            combinedData.setData(new CandleData(candleDataSet));
        }

        // Scatter data for daily average markers
        if (!avgEntries.isEmpty()) {
            ScatterDataSet scatterDataSet = new ScatterDataSet(avgEntries, getString(R.string.hr_average));
            scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            scatterDataSet.setScatterShapeSize(15f);
            scatterDataSet.setColor(SPO2_AVG_COLOR);
            scatterDataSet.setDrawValues(false);
            scatterDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            combinedData.setData(new ScatterData(scatterDataSet));
        }

        spo2Chart.setData(combinedData);
    }

    private void setupChart() {
        spo2Chart.setBackgroundColor(BACKGROUND_COLOR);
        spo2Chart.getDescription().setEnabled(false);
        spo2Chart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.CANDLE,
                CombinedChart.DrawOrder.SCATTER
        });

        if (TOTAL_DAYS <= 7) {
            spo2Chart.setTouchEnabled(false);
            spo2Chart.setPinchZoom(false);
        }
        spo2Chart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = spo2Chart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setGranularity(1f);
        xAxisBottom.setGranularityEnabled(true);
        xAxisBottom.setAxisMinimum(-0.5f);
        xAxisBottom.setAxisMaximum(TOTAL_DAYS - 0.5f);

        final YAxis yAxisLeft = spo2Chart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100f);
        yAxisLeft.setAxisMinimum(85f);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setGranularity(5f);
        yAxisLeft.setGranularityEnabled(true);

        final YAxis yAxisRight = spo2Chart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(2);

        LegendEntry rangeEntry = new LegendEntry();
        rangeEntry.label = getString(R.string.pref_header_spo2);
        rangeEntry.formColor = SPO2_COLOR;
        legendEntries.add(rangeEntry);

        LegendEntry avgEntry = new LegendEntry();
        avgEntry.label = getString(R.string.hr_average);
        avgEntry.formColor = SPO2_AVG_COLOR;
        avgEntry.form = Legend.LegendForm.CIRCLE;
        legendEntries.add(avgEntry);

        spo2Chart.getLegend().setCustom(legendEntries);
        spo2Chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        spo2Chart.getLegend().setWordWrapEnabled(true);
    }

    @Override
    protected void renderCharts() {
        spo2Chart.invalidate();
    }

    protected static class Spo2PeriodData extends ChartsData {
        public List<Spo2DayData> days;

        protected Spo2PeriodData(List<Spo2DayData> days) {
            this.days = days;
        }
    }

    protected static class Spo2DayData extends ChartsData {
        public int average;
        public int minimum;
        public int maximum;

        protected Spo2DayData(int average, int minimum, int maximum) {
            this.average = average;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }
}
