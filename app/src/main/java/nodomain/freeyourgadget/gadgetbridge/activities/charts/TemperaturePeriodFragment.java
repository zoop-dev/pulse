/*  Copyright (C) 2026 The Gadgetbridge Contributors

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
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureUnit;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class TemperaturePeriodFragment extends AbstractChartFragment<TemperaturePeriodFragment.TemperaturePeriodData> {
    protected static final Logger LOG = LoggerFactory.getLogger(TemperaturePeriodFragment.class);

    static final int SEC_PER_DAY = 24 * 60 * 60;
    static final float DATA_INVALID = Float.NaN;

    private int BACKGROUND_COLOR;
    private int CHART_TEXT_COLOR;
    private int LEGEND_TEXT_COLOR;
    private int TEMPERATURE_COLOR;
    private int TEMPERATURE_AVG_COLOR;

    private TextView dateView;
    private TextView temperatureMinimum;
    private TextView temperatureMaximum;
    private TextView temperatureAverage;
    private CombinedChart temperatureChart;
    private int totalDays;

    private final TemperatureUnit temperatureUnit = GBApplication.getPrefs().getTemperatureUnit();

    @Override
    protected boolean isSingleDay() {
        return false;
    }

    public static TemperaturePeriodFragment newInstance(final int totalDays) {
        final TemperaturePeriodFragment fragment = new TemperaturePeriodFragment();
        final Bundle args = new Bundle();
        args.putInt("totalDays", totalDays);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        totalDays = getArguments() != null ? getArguments().getInt("totalDays") : 7;
    }

    @Override
    protected void init() {
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        TEMPERATURE_COLOR = ContextCompat.getColor(requireContext(), R.color.chart_temperature);
        TEMPERATURE_AVG_COLOR = ContextCompat.getColor(requireContext(), R.color.chart_temperature_average);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_temperature_period, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                getChartsHost().enableSwipeRefresh(scrollY == 0));

        dateView = rootView.findViewById(R.id.temperature_period_date_view);
        temperatureMinimum = rootView.findViewById(R.id.temperature_period_minimum);
        temperatureMaximum = rootView.findViewById(R.id.temperature_period_maximum);
        temperatureAverage = rootView.findViewById(R.id.temperature_period_average);
        temperatureChart = rootView.findViewById(R.id.temperature_period_chart);

        setupChart();
        refresh();
        setupLegend(temperatureChart);

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.menuitem_temperature);
    }

    private int getStartTs() {
        final Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        final Calendar startDay = DateTimeUtils.dayStart(day);
        startDay.add(Calendar.DATE, -(totalDays - 1));
        return (int) (startDay.getTimeInMillis() / 1000);
    }

    private TemperatureDayData fetchTemperatureDataForDay(final DBHandler db, final GBDevice device, final int startTs) {
        final int endTs = startTs + SEC_PER_DAY - 1;
        final List<? extends TemperatureSample> samples = getSamples(db, device, startTs, endTs);

        final Accumulator accumulator = new Accumulator();
        for (final TemperatureSample sample : samples) {
            final float temperature = sample.getTemperature();
            if (!Float.isNaN(temperature)) {
                accumulator.add(toPreferredUnit(temperature));
            }
        }

        final float average = accumulator.getCount() > 0 ? (float) accumulator.getAverage() : DATA_INVALID;
        final float minimum = accumulator.getCount() > 0 ? (float) accumulator.getMin() : DATA_INVALID;
        final float maximum = accumulator.getCount() > 0 ? (float) accumulator.getMax() : DATA_INVALID;

        return new TemperatureDayData(average, minimum, maximum);
    }

    @Override
    protected TemperaturePeriodData refreshInBackground(final ChartsHost chartsHost, final DBHandler db, final GBDevice device) {
        final int startTs = getStartTs();

        final List<TemperatureDayData> result = new ArrayList<>(totalDays);
        for (int i = 0; i < totalDays; i++) {
            final TemperatureDayData dayData = fetchTemperatureDataForDay(db, device, startTs + i * SEC_PER_DAY);
            result.add(dayData);
        }

        return new TemperaturePeriodData(result);
    }

    private List<? extends TemperatureSample> getSamples(final DBHandler db, final GBDevice device, final int startTs, final int endTs) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends TemperatureSample> sampleProvider = coordinator.getTemperatureSampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(startTs * 1000L, endTs * 1000L);
    }

    @Override
    protected void updateChartsnUIThread(final TemperaturePeriodData data) {
        final int startTs = getStartTs();
        dateView.setText(DateTimeUtils.formatDaysUntil(totalDays, getTSEnd()));

        final Accumulator avgAccumulator = new Accumulator();
        final Accumulator minAccumulator = new Accumulator();
        final Accumulator maxAccumulator = new Accumulator();

        final ArrayList<CandleEntry> candleEntries = new ArrayList<>();
        final ArrayList<Entry> avgEntries = new ArrayList<>();

        for (int i = 0; i < data.days.size(); i++) {
            final TemperatureDayData dayData = data.days.get(i);
            if (hasData(dayData.minimum) && hasData(dayData.maximum)) {
                minAccumulator.add(dayData.minimum);
                maxAccumulator.add(dayData.maximum);
                candleEntries.add(new CandleEntry(i, dayData.maximum, dayData.minimum, dayData.minimum, dayData.maximum));
            }
            if (hasData(dayData.average)) {
                avgAccumulator.add(dayData.average);
                avgEntries.add(new Entry(i, dayData.average));
            }
        }

        final float average = avgAccumulator.getCount() > 0 ? (float) avgAccumulator.getAverage() : DATA_INVALID;
        final float minimum = minAccumulator.getCount() > 0 ? (float) minAccumulator.getMin() : DATA_INVALID;
        final float maximum = maxAccumulator.getCount() > 0 ? (float) maxAccumulator.getMax() : DATA_INVALID;

        final String emptyValue = getString(R.string.stats_empty_value);
        temperatureMinimum.setText(hasData(minimum) ? formatTemperature(minimum) : emptyValue);
        temperatureMaximum.setText(hasData(maximum) ? formatTemperature(maximum) : emptyValue);
        temperatureAverage.setText(hasData(average) ? formatTemperature(average) : emptyValue);

        temperatureChart.getXAxis().setValueFormatter(createDayFormatter(startTs));
        configureYAxis(minimum, maximum);

        final CombinedData combinedData = new CombinedData();

        if (!candleEntries.isEmpty()) {
            final CandleDataSet candleDataSet = new CandleDataSet(candleEntries, getString(R.string.menuitem_temperature));
            candleDataSet.setDrawValues(false);
            candleDataSet.setDrawIcons(false);
            candleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            candleDataSet.setShadowColor(TEMPERATURE_COLOR);
            candleDataSet.setShadowWidth(2f);
            candleDataSet.setDecreasingColor(TEMPERATURE_COLOR);
            candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
            candleDataSet.setIncreasingColor(TEMPERATURE_COLOR);
            candleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
            candleDataSet.setNeutralColor(TEMPERATURE_COLOR);
            candleDataSet.setBarSpace(0.15f);
            candleDataSet.setShowCandleBar(true);
            combinedData.setData(new CandleData(candleDataSet));
        }

        if (!avgEntries.isEmpty()) {
            final ScatterDataSet scatterDataSet = new ScatterDataSet(avgEntries, getString(R.string.hr_average));
            scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            scatterDataSet.setScatterShapeSize(15f);
            scatterDataSet.setColor(TEMPERATURE_AVG_COLOR);
            scatterDataSet.setDrawValues(false);
            scatterDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            combinedData.setData(new ScatterData(scatterDataSet));
        }

        if (candleEntries.isEmpty() && avgEntries.isEmpty()) {
            temperatureChart.setData(null);
        } else {
            temperatureChart.setData(combinedData);
        }
    }

    private ValueFormatter createDayFormatter(final int startTs) {
        final String format = totalDays == 7 ? "EEE" : "dd";
        final SimpleDateFormat formatDay = new SimpleDateFormat(format, Locale.getDefault());
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(final float value) {
                final int dayIndex = Math.round(value);
                if (dayIndex < 0 || dayIndex >= totalDays) {
                    return "";
                }
                final int ts = startTs + SEC_PER_DAY * dayIndex;
                return formatDay.format(new Date(ts * 1000L));
            }
        };
    }

    private void setupChart() {
        temperatureChart.setBackgroundColor(BACKGROUND_COLOR);
        temperatureChart.getDescription().setEnabled(false);
        temperatureChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.CANDLE,
                CombinedChart.DrawOrder.SCATTER
        });

        if (totalDays <= 7) {
            temperatureChart.setTouchEnabled(false);
            temperatureChart.setPinchZoom(false);
        }
        temperatureChart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = temperatureChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setGranularity(1f);
        xAxisBottom.setGranularityEnabled(true);
        xAxisBottom.setAxisMinimum(-0.5f);
        xAxisBottom.setAxisMaximum(totalDays - 0.5f);

        final YAxis yAxisLeft = temperatureChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setGranularity(1f);
        yAxisLeft.setGranularityEnabled(true);

        configureYAxis(DATA_INVALID, DATA_INVALID);

        final YAxis yAxisRight = temperatureChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    private void configureYAxis(final float minimum, final float maximum) {
        final YAxis yAxisLeft = temperatureChart.getAxisLeft();
        if (hasData(minimum) && hasData(maximum)) {
            final float axisGap = temperatureUnit == TemperatureUnit.CELSIUS ? 3f : 6f;
            yAxisLeft.setAxisMinimum((float) Math.floor(minimum) - axisGap);
            yAxisLeft.setAxisMaximum((float) Math.ceil(maximum) + axisGap);
        } else {
            final boolean isMetric = temperatureUnit == TemperatureUnit.CELSIUS;
            yAxisLeft.setAxisMinimum((float) (isMetric ? 30f : TemperatureChartFragment.celsiusToFahrenheit(30d)));
            yAxisLeft.setAxisMaximum((float) (isMetric ? 45f : TemperatureChartFragment.celsiusToFahrenheit(45d)));
        }
    }

    @Override
    protected void setupLegend(final Chart<?> chart) {
        final List<LegendEntry> legendEntries = new ArrayList<>(2);

        final LegendEntry rangeEntry = new LegendEntry();
        rangeEntry.label = getString(R.string.menuitem_temperature);
        rangeEntry.formColor = TEMPERATURE_COLOR;
        legendEntries.add(rangeEntry);

        final LegendEntry avgEntry = new LegendEntry();
        avgEntry.label = getString(R.string.hr_average);
        avgEntry.formColor = TEMPERATURE_AVG_COLOR;
        avgEntry.form = Legend.LegendForm.CIRCLE;
        legendEntries.add(avgEntry);

        temperatureChart.getLegend().setCustom(legendEntries);
        temperatureChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        temperatureChart.getLegend().setWordWrapEnabled(true);
    }

    @Override
    protected void renderCharts() {
        temperatureChart.invalidate();
    }

    private float toPreferredUnit(final float celsius) {
        if (temperatureUnit == TemperatureUnit.CELSIUS) {
            return celsius;
        }

        return TemperatureDailyFragment.celsiusToFahrenheit(celsius);
    }

    private String formatTemperature(final float temperature) {
        final String unit = getString(temperatureUnit == TemperatureUnit.CELSIUS ? R.string.unit_celsius : R.string.unit_fahrenheit);
        return String.format(Locale.getDefault(), "%.1f %s", temperature, unit);
    }

    private static boolean hasData(final float value) {
        return !Float.isNaN(value);
    }

    protected static class TemperaturePeriodData extends ChartsData {
        public final List<TemperatureDayData> days;

        protected TemperaturePeriodData(final List<TemperatureDayData> days) {
            this.days = days;
        }
    }

    protected static class TemperatureDayData extends ChartsData {
        public final float average;
        public final float minimum;
        public final float maximum;

        protected TemperatureDayData(final float average, final float minimum, final float maximum) {
            this.average = average;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }
}
