package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.TimeWeightedAverageAccumulator;

public class HeartRatePeriodFragment extends AbstractChartFragment<HeartRatePeriodFragment.HeartRatePeriodData> {

    protected static final Logger LOG = LoggerFactory.getLogger(HeartRatePeriodFragment.class);

    static int SEC_PER_DAY = 24 * 60 * 60;
    static int DATA_INVALID = -1;

    protected int HEARTRATE_COLOR;
    protected int HEARTRATE_MIN_COLOR;
    protected int HEARTRATE_RESTING_COLOR;
    protected int HEARTRATE_MAX_COLOR;
    protected int CHART_TEXT_COLOR;
    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int TEXT_COLOR;

    private TextView mDateView;
    private TextView hrResting;
    private TextView hrAverage;
    private TextView hrMinimum;
    private TextView hrMaximum;
    private LineChart hrLineChart;
    private int TOTAL_DAYS;

    @Override
    protected boolean isSingleDay() {
        return false;
    }

    public static HeartRatePeriodFragment newInstance(int totalDays) {
        HeartRatePeriodFragment fragmentFirst = new HeartRatePeriodFragment();
        Bundle args = new Bundle();
        args.putInt("totalDays", totalDays);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOTAL_DAYS = getArguments() != null ? getArguments().getInt("totalDays") : 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_heart_rate, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> getChartsHost().enableSwipeRefresh(scrollY == 0));

        mDateView = rootView.findViewById(R.id.hr_date_view);
        hrLineChart = rootView.findViewById(R.id.heart_rate_line_chart);
        hrResting = rootView.findViewById(R.id.hr_resting);
        hrAverage = rootView.findViewById(R.id.hr_average);
        hrMinimum = rootView.findViewById(R.id.hr_minimum);
        hrMaximum = rootView.findViewById(R.id.hr_maximum);
        final LinearLayout heartRateRestingWrapper = rootView.findViewById(R.id.hr_resting_wrapper);

        setupChart();
        refresh();
        setupLegend(hrLineChart);

        if (!supportsHeartRateRestingMeasurement()) {
            heartRateRestingWrapper.setVisibility(View.GONE);
        }

        return rootView;
    }

    public boolean supportsHeartRateRestingMeasurement() {
        final GBDevice device = getChartsHost().getDevice();
        return device.getDeviceCoordinator().supportsHeartRateRestingMeasurement(device);
    }

    protected List<? extends AbstractActivitySample> getActivitySamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = device.getDeviceCoordinator().getSampleProvider(device, db.getDaoSession());
        return provider.getAllActivitySamplesHighRes(tsFrom, tsTo);
    }

    @Override
    public String getTitle() {
        return getString(R.string.heart_rate);
    }

    @Override
    protected void init() {
        Prefs prefs = GBApplication.getPrefs();
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(getContext());
        DESCRIPTION_COLOR = LEGEND_TEXT_COLOR = TEXT_COLOR = GBApplication.getTextColor(getContext());
        if (prefs.getBoolean("chart_heartrate_color", false)) {
            HEARTRATE_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate_alternative);
        } else {
            HEARTRATE_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate);
        }
        HEARTRATE_MIN_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate_minimum);
        HEARTRATE_MAX_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate_maximum);
        HEARTRATE_RESTING_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate_resting);
    }

    private HeartRateData fetchHeartRateDataForDay(ChartsHost chartsHost, DBHandler db, GBDevice device, int startTs) {
        int endTs = startTs + SEC_PER_DAY - 1;
        List<? extends ActivitySample> samples = getActivitySamples(db, device, startTs, endTs);
        final HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();

        int restingHeartRate = DATA_INVALID;
        if (supportsHeartRateRestingMeasurement()) {
            restingHeartRate = device.getDeviceCoordinator()
                    .getHeartRateRestingSampleProvider(device, db.getDaoSession())
                    .getAllSamples(startTs * 1000L, endTs * 1000L)
                    .stream()
                    .max(Comparator.comparingLong(HeartRateSample::getTimestamp))
                    .map(HeartRateSample::getHeartRate)
                    .orElse(DATA_INVALID);
        }

        final TimeWeightedAverageAccumulator accumulator = new TimeWeightedAverageAccumulator(60 * HeartRateUtils.MAX_HR_MEASUREMENTS_GAP_MINUTES, 60);
        for (int i = 0; i < samples.size(); i++) {
            final ActivitySample sample = samples.get(i);
            if (heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                accumulator.add(sample.getTimestamp(), sample.getHeartRate());
            }
        }

        final int average = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getAverage()) : DATA_INVALID;
        final int minimum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMin()) : DATA_INVALID;
        final int maximum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMax()) : DATA_INVALID;

        return new HeartRateData(samples, restingHeartRate, average, minimum, maximum);
    }

    @Override
    protected HeartRatePeriodData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Pair<Integer, Integer> startAndEndTs = getStartAndEndTS();
        final int startTs = startAndEndTs.getKey();

        List<HeartRateData> result = new ArrayList<>();
        for (int i = 0; i < TOTAL_DAYS; i++) {
            HeartRateData dayData = fetchHeartRateDataForDay(chartsHost, db, device, startTs + i * SEC_PER_DAY);
            result.add(dayData);
        }
        return new HeartRatePeriodData(result);
    }

    @Override
    protected void renderCharts() {
        hrLineChart.invalidate();
    }

    private void setupChart() {
        hrLineChart.setBackgroundColor(BACKGROUND_COLOR);
        hrLineChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        hrLineChart.getDescription().setEnabled(false);

        XAxis x = hrLineChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxisLeft = hrLineChart.getAxisLeft();
        yAxisLeft.setEnabled(true);
        YAxis yAxisRight = hrLineChart.getAxisRight();
        yAxisRight.setDrawLabels(true);

        YAxis[] yAxisArr = {yAxisLeft, yAxisRight};
        for (YAxis y : yAxisArr) {
            y.setAxisMaximum(HeartRateUtils.getInstance().getMaxHeartRate());
            y.setAxisMinimum(HeartRateUtils.getInstance().getMinHeartRate());
            y.setDrawGridLines(false);
            y.setDrawTopYLabelEntry(true);
            y.setTextColor(CHART_TEXT_COLOR);
        }

        refresh();
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(4);

        if (TOTAL_DAYS == 1) {
            LegendEntry hrEntry = new LegendEntry();
            hrEntry.label = getTitle();
            hrEntry.formColor = HEARTRATE_COLOR;
            legendEntries.add(hrEntry);
        } else {
            LegendEntry hrMinEntry = new LegendEntry();
            hrMinEntry.label = getString(R.string.hr_minimum);
            hrMinEntry.formColor = HEARTRATE_MIN_COLOR;
            legendEntries.add(hrMinEntry);
        }

        if (supportsHeartRateRestingMeasurement() && TOTAL_DAYS != 1) {
            LegendEntry hrRestingEntry = new LegendEntry();
            hrRestingEntry.label = getString(R.string.hr_resting);
            hrRestingEntry.formColor = HEARTRATE_RESTING_COLOR;
            legendEntries.add(hrRestingEntry);
        }

        if (GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            LegendEntry hrAverageEntry = new LegendEntry();
            hrAverageEntry.label = getString(R.string.hr_average);
            hrAverageEntry.formColor = TOTAL_DAYS != 1 ? HEARTRATE_COLOR : Color.RED;
            legendEntries.add(hrAverageEntry);
        }

        if (TOTAL_DAYS != 1) {
            LegendEntry hrMaxEntry = new LegendEntry();
            hrMaxEntry.label = getString(R.string.hr_maximum);
            hrMaxEntry.formColor = HEARTRATE_MAX_COLOR;
            legendEntries.add(hrMaxEntry);
        }

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
    }

    protected LineDataSet createHeartRateDataSet(final List<Entry> values, int color) {
        LineDataSet dataSet = new LineDataSet(values, "Heart Rate");
        dataSet.setLineWidth(1.5f);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(true);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        dataSet.setColor(color);
        dataSet.setValueTextColor(TEXT_COLOR);
        dataSet.setValueTextSize(10f);
        return dataSet;
    }

    private Pair<Integer, Integer> getStartAndEndTS() {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        day.add(Calendar.DATE, 0);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, 0);
        int startTs = (int) (day.getTimeInMillis() / 1000) - SEC_PER_DAY * (TOTAL_DAYS - 1);
        int endTs = startTs + SEC_PER_DAY * TOTAL_DAYS - 1;
        return Pair.of(startTs, endTs);
    }

    private void setStatistics(int average, int minimum, int maximum, int resting) {
        hrAverage.setText(average > 0 ? getString(R.string.bpm_value_unit, average) : "-");
        hrMinimum.setText(minimum > 0 ? getString(R.string.bpm_value_unit, minimum) : "-");
        hrMaximum.setText(maximum > 0 ? getString(R.string.bpm_value_unit, maximum) : "-");
        hrResting.setText(resting > 0 ? getString(R.string.bpm_value_unit, resting) : "-");

        if (minimum > 0) {
            hrLineChart.getAxisLeft().setAxisMinimum(Math.max(minimum - 30, 0));
            hrLineChart.getAxisRight().setAxisMinimum(Math.max(minimum - 30, 0));
        }
        if (maximum > 0) {
            hrLineChart.getAxisLeft().setAxisMaximum(maximum + 30);
            hrLineChart.getAxisRight().setAxisMaximum(maximum + 30);
        }
    }

    @Override
    protected void updateChartsnUIThread(HeartRatePeriodData data) {
        Pair<Integer, Integer> startAndEndTs = getStartAndEndTS();
        final int startTs = startAndEndTs.getKey();
        final int endTs = startAndEndTs.getValue();

        //Date date = new Date((long) endTs * 1000);
        mDateView.setText(DateTimeUtils.formatDaysUntil(TOTAL_DAYS, getTSEnd()));
        final XAxis x = hrLineChart.getXAxis();
        if (TOTAL_DAYS == 1) {
            setOneDayData(data.samples.get(0), endTs);
            x.setAxisMinimum(0f);
            x.setAxisMaximum(86400f);
        } else {
            setMultipleDaysData(data, startTs, endTs);
            x.setAxisMinimum(0);
            // If the timestamp is used as XAxis, the chart library formats
            // the labels not at 0:00, which causes a shift in the labels
            x.setAxisMaximum(TOTAL_DAYS - 1);
        }
    }

    private void setOneDayData(HeartRateData data, int endTs) {
        Date date = new Date((long) endTs * 1000);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(date);
        mDateView.setText(formattedDate);

        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();
        final List<Entry> lineEntries = new ArrayList<>();
        List<? extends ActivitySample> samples = data.samples;
        final TimestampTranslation tsTranslation = new TimestampTranslation();

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        int lastTs = 0;
        for (int i = 0; i < samples.size(); i++) {
            final ActivitySample sample = samples.get(i);
            if (!heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                continue;
            }
            final int ts = sample.getTimestamp();
            final int shortTs = tsTranslation.shorten(ts);
            if (lastTs == 0 || (ts - lastTs) <= 60 * HeartRateUtils.MAX_HR_MEASUREMENTS_GAP_MINUTES) {
                lineEntries.add(new Entry(shortTs, sample.getHeartRate()));
            } else {
                if (!lineEntries.isEmpty()) {
                    List<Entry> clone = new ArrayList<>(lineEntries.size());
                    clone.addAll(lineEntries);
                    lineDataSets.add(createHeartRateDataSet(clone, HEARTRATE_COLOR));
                    lineEntries.clear();
                }
                lineEntries.add(new Entry(shortTs, sample.getHeartRate()));
            }
            lastTs = ts;
        }
        hrLineChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "HH:mm"));
        if (!lineEntries.isEmpty()) {
            lineDataSets.add(createHeartRateDataSet(lineEntries, HEARTRATE_COLOR));
        }

        setStatistics(data.average, data.minimum, data.maximum, data.restingHeartRate);

        hrLineChart.setData(new LineData(lineDataSets));
        hrLineChart.getAxisLeft().removeAllLimitLines();

        if (data.average > 0 && GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            final LimitLine averageLine = new LimitLine(data.average);
            averageLine.setLineWidth(1.5f);
            averageLine.enableDashedLine(15f, 10f, 0f);
            averageLine.setLineColor(Color.RED);
            hrLineChart.getAxisLeft().addLimitLine(averageLine);
        }

        //if (data.restingHeartRate > 0) {
        //    final LimitLine restingLine = new LimitLine(data.restingHeartRate);
        //    restingLine.setLineWidth(1.5f);
        //    restingLine.enableDashedLine(15f, 10f, 0f);
        //    restingLine.setLineColor(HEARTRATE_RESTING_COLOR);
        //    hrLineChart.getAxisLeft().addLimitLine(restingLine);
        //}
    }

    private void setMultipleDaysData(HeartRatePeriodData data, int startTs, int endTs) {
        List<HeartRateData> samples = data.samples;
        final Accumulator avgAccumulator = new Accumulator();
        final Accumulator minAccumulator = new Accumulator();
        final Accumulator maxAccumulator = new Accumulator();
        final Accumulator restingAccumulator = new Accumulator();

        final ArrayList<Entry> avgLineData = new ArrayList<>();
        final ArrayList<Entry> minLineData = new ArrayList<>();
        final ArrayList<Entry> maxLineData = new ArrayList<>();
        final ArrayList<Entry> restingLineData = new ArrayList<>();

        for (int i = 0; i < samples.size(); i++) {
            final HeartRateData hrData = samples.get(i);
            if (hrData.average > 0) {
                avgAccumulator.add(hrData.average);
                avgLineData.add(new Entry(i, hrData.average));
            }
            if (hrData.minimum > 0) {
                minAccumulator.add(hrData.minimum);
                minLineData.add(new Entry(i, hrData.minimum));
            }
            if (hrData.maximum > 0) {
                maxAccumulator.add(hrData.maximum);
                maxLineData.add(new Entry(i, hrData.maximum));
            }
            if (hrData.restingHeartRate > 0) {
                restingAccumulator.add(hrData.restingHeartRate);
                restingLineData.add(new Entry(i, hrData.restingHeartRate));
            }
        }

        final String fmt = TOTAL_DAYS == 7 ? "EEE" : "dd";
        SimpleDateFormat formatDay = new SimpleDateFormat(fmt, Locale.getDefault());
        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int ts = startTs + SEC_PER_DAY * (int) value;
                return formatDay.format(new Date(ts * 1000L));
            }
        };
        hrLineChart.getXAxis().setValueFormatter(formatter);

        final int average = avgAccumulator.getCount() > 0 ? (int) Math.round(avgAccumulator.getAverage()) : DATA_INVALID;
        final int minimum = minAccumulator.getCount() > 0 ? (int) Math.round(minAccumulator.getMin()) : DATA_INVALID;
        final int maximum = maxAccumulator.getCount() > 0 ? (int) Math.round(maxAccumulator.getMax()) : DATA_INVALID;
        final int restingAvg = restingAccumulator.getCount() > 0 ? (int) Math.round(restingAccumulator.getAverage()) : DATA_INVALID;
        setStatistics(average, minimum, maximum, restingAvg);

        List<ILineDataSet> dataSets = new ArrayList<>();
        if (GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            dataSets.add(createHeartRateDataSet(avgLineData, HEARTRATE_COLOR));
        }
        dataSets.add(createHeartRateDataSet(minLineData, HEARTRATE_MIN_COLOR));
        dataSets.add(createHeartRateDataSet(maxLineData, HEARTRATE_MAX_COLOR));
        dataSets.add(createHeartRateDataSet(restingLineData, HEARTRATE_RESTING_COLOR));

        hrLineChart.setData(new LineData(dataSets));
    }

    protected static class HeartRatePeriodData extends ChartsData {
        public List<HeartRateData> samples;

        protected HeartRatePeriodData(List<HeartRateData> samples) {
            this.samples = samples;
        }
    }

    protected static class HeartRateData extends ChartsData {
        public List<? extends ActivitySample> samples;
        public int restingHeartRate;
        public int average;
        public int minimum;
        public int maximum;

        protected HeartRateData(List<? extends ActivitySample> samples, int restingHeartRate, int average, int minimum, int maximum) {
            this.samples = samples;
            this.restingHeartRate = restingHeartRate;
            this.average = average;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }
}
