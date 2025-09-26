/*  Copyright (C) 2025 a0z, Thomas Kuehne

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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTrainingLoadAcuteSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTrainingLoadChronicSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.WorkoutLoadSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;


public class LoadFragment extends AbstractChartFragment<LoadFragment.LoadsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(LoadFragment.class);
    protected final int TOTAL_DAYS = 30;

    protected GaugeDrawer gaugeDrawer;
    private ImageView acuteLoadRatioGauge;
    private TextView acuteLoadRatioGaugeValue;
    private TextView acuteLoadRatioGaugeStatus;

    private TextView acuteLoad;
    private TextView chronicLoad;
    private TextView thisWeekTotal;
    private TextView lastWeekTotal;
    private TextView dateHeader;
    private LineChart acuteLoadChart;
    private BarChart dailyLoadChart;
    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int TEXT_COLOR;
    protected int LOAD_COLOR;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_load, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        dateHeader = rootView.findViewById(R.id.date_view);
        dailyLoadChart = rootView.findViewById(R.id.daily_load_chart);
        thisWeekTotal = rootView.findViewById(R.id.this_week_total);
        lastWeekTotal = rootView.findViewById(R.id.last_week_total);
        if (supportsTrainingLoad()) {
            acuteLoadChart = rootView.findViewById(R.id.acute_load_chart);
            acuteLoad = rootView.findViewById(R.id.acute_load);
            chronicLoad = rootView.findViewById(R.id.chronic_load);
            acuteLoadRatioGauge = rootView.findViewById(R.id.acute_load_ratio_gauge);
            acuteLoadRatioGaugeValue = rootView.findViewById(R.id.acute_load_ratio_gauge_value);
            acuteLoadRatioGaugeStatus = rootView.findViewById(R.id.acute_load_ratio_gauge_status);
            gaugeDrawer = new GaugeDrawer();
            setupAcuteLoadChart();
        } else {
            rootView.findViewById(R.id.training_load_wrapper).setVisibility(View.GONE);
        }
        setupDailyLoadChart();
        refresh();

        return rootView;
    }

    public boolean supportsTrainingLoad() {
        final GBDevice device = getChartsHost().getDevice();
        return device.getDeviceCoordinator().supportsTrainingLoad(device);
    }

    @Override
    public String getTitle() {
        return getString(R.string.pref_header_training_load);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        LOAD_COLOR = getAcuteColor(requireContext());
    }

    @Override
    protected LoadsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());

        return getData(db, day, device);
    }

    @Override
    protected void renderCharts() {
        if (acuteLoadChart != null) {
            acuteLoadChart.invalidate();
        }
        dailyLoadChart.invalidate();
    }

    protected LineDataSet createDataSet(final List<Entry> values, String name, int color) {
        final LineDataSet lineDataSet = new LineDataSet(values, name);
        lineDataSet.setColor(color);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setCircleHoleColor(Color.WHITE);
        lineDataSet.setCircleColor(color);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        return lineDataSet;
    }

    @Override
    protected void updateChartsnUIThread(LoadsData data) {
        if (data == null) {
            return;
        }
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(getEndDate());
        dateHeader.setText(formattedDate);
        thisWeekTotal.setText(String.valueOf(data.getThisWeekLoad()));
        lastWeekTotal.setText(String.valueOf(data.getLastWeekLoad()));
        dailyLoadChart.setData(null);
        acuteLoadChart.setData(null);

        List<Entry> acuteLoadEntries = new ArrayList<>();
        List<Entry> chronicLoadEntries = new ArrayList<>();
        List<BarEntry> dailyLoadEntries = new ArrayList<>();
        data.getData().forEach((LoadData dayData) -> {
            if (dayData.acuteLoad > 0) {
                acuteLoadEntries.add(new Entry(dayData.i, dayData.acuteLoad));
            }
            if (dayData.chronicLoad > 0) {
                chronicLoadEntries.add(new Entry(dayData.i, dayData.chronicLoad));
            }
            if (dayData.load > 0) {
                dailyLoadEntries.add(new BarEntry(dayData.i, dayData.load));
            }
        });

        // Daily load chart chart.
        BarDataSet set = new BarDataSet(dailyLoadEntries, "Load");
        set.setDrawValues(true);
        set.setColors(LOAD_COLOR);
        final XAxis x = dailyLoadChart.getXAxis();
        x.setValueFormatter(getDailyLoadChartDayValueFormatter(data));
        dailyLoadChart.getAxisLeft().setAxisMaximum(Math.max(set.getYMax(), 200) + 100);
        dailyLoadChart.setRenderer(new AngledLabelsChartRenderer(dailyLoadChart, dailyLoadChart.getAnimator(), dailyLoadChart.getViewPortHandler()));
        BarData barData = new BarData(set);
        barData.setValueTextColor(TEXT_COLOR);
        barData.setValueTextSize(10f);
        dailyLoadChart.setData(barData);

        if (supportsTrainingLoad()) {
            List<LegendEntry> legendEntries = new ArrayList<>(1);
            LegendEntry acuteLoadEntry = new LegendEntry();
            acuteLoadEntry.label = getString(R.string.training_acute_load);
            acuteLoadEntry.formColor = LOAD_COLOR;
            legendEntries.add(acuteLoadEntry);
            LegendEntry chronicLoadEntry = new LegendEntry();
            chronicLoadEntry.label = getString(R.string.training_chronic_load);
            chronicLoadEntry.formColor = getResources().getColor(R.color.training_chronic_load);
            legendEntries.add(chronicLoadEntry);
            acuteLoadChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
            acuteLoadChart.getLegend().setCustom(legendEntries);
            acuteLoadChart.getXAxis().setValueFormatter(getDailyLoadChartDayValueFormatter(data));

            final List<ILineDataSet> lineDataSets = new ArrayList<>();
            lineDataSets.add(createDataSet(acuteLoadEntries, getString(R.string.training_acute_load), LOAD_COLOR));
            lineDataSets.add(createDataSet(chronicLoadEntries, getString(R.string.training_chronic_load), getResources().getColor(R.color.training_chronic_load)));
            final LineData lineData = new LineData(lineDataSets);
            acuteLoadChart.getAxisLeft().setAxisMaximum(Math.max(lineData.getYMax(), 200) + 100);
            acuteLoadChart.setData(lineData);

            // Acute load ratio gauge
            int latestAcuteLoad = data.getLatestAcuteLoad();
            int latestChronicLoad = data.getLatestChronicLoad();
            acuteLoad.setText(String.valueOf(latestAcuteLoad));
            chronicLoad.setText(String.valueOf(latestChronicLoad));
            // Gauge
            acuteLoadRatioGaugeValue.setText(String.valueOf(latestAcuteLoad));
            float value;
            if (latestAcuteLoad > 0) {
                value = (float) latestAcuteLoad / latestChronicLoad;
                if (value < 0.8) {
                    value = (float) GaugeDrawer.normalize(value, 0, 0.8, 0, 0.333);
                    acuteLoadRatioGaugeStatus.setText(getString(R.string.low));
                } else if (value < 1.5) {
                    value = (float) GaugeDrawer.normalize(value, 0.8, 1.5, 0.334f, 0.666);
                    acuteLoadRatioGaugeStatus.setText(getString(R.string.optimal));
                } else if (value < 2) {
                    value = (float) GaugeDrawer.normalize(value, 1.5, 2, 0.667f, 1);
                    acuteLoadRatioGaugeStatus.setText(getString(R.string.high));
                } else {
                    value = 1;
                    acuteLoadRatioGaugeStatus.setText(getString(R.string.very_high));
                }
            } else {
                value = 0;
                acuteLoadRatioGaugeStatus.setText(getString(R.string.none));
            }

            int[] colors = new int[]{
                    ContextCompat.getColor(GBApplication.getContext(), R.color.training_load_low),
                    ContextCompat.getColor(GBApplication.getContext(), R.color.training_load_optimal),
                    ContextCompat.getColor(GBApplication.getContext(), R.color.training_load_high),
            };
            float[] segments = new float[]{
                    0.333f, // low
                    0.333f, // optimal
                    0.333f, // high
            };
            gaugeDrawer.drawSegmentedGauge(acuteLoadRatioGauge, colors, segments, value, false, true);
        }
    }

    private LoadsData getData(DBHandler db, Calendar day, GBDevice device) {
        final ZonedDateTime now = day.toInstant().atZone(ZoneId.systemDefault());
        final ZonedDateTime startOfThisWeek = now.with(DayOfWeek.MONDAY).with(LocalTime.of(0, 0, 0));
        final ZonedDateTime endOfThisWeek = startOfThisWeek.plusDays(6).with(LocalTime.of(23, 59, 59));
        final ZonedDateTime startOfLastWeek = now.minusWeeks(1).with(DayOfWeek.MONDAY).with(LocalTime.of(0, 0, 0));
        final ZonedDateTime endOfLastWeek = startOfLastWeek.plusDays(6).with(LocalTime.of(23, 59, 59));
        day = DateTimeUtils.dayStart(day);
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);
        List<LoadData> data = new ArrayList<>();
        for (int i = 0; i < TOTAL_DAYS; i++) {
            int startTs = (int) (day.getTimeInMillis() / 1000);
            int endTs = startTs + 24 * 60 * 60 - 1;
            List<? extends WorkoutLoadSample> workoutLoadSamples = getWorkoutLoadSamples(db, device, startTs, endTs);
            int load = 0;
            int acuteLoad = 0;
            int chronicLoad = 0;
            if (!workoutLoadSamples.isEmpty()) {
                load = workoutLoadSamples.stream().mapToInt(WorkoutLoadSample::getValue).sum();
            }
            if (supportsTrainingLoad()) {
                List<? extends GenericTrainingLoadAcuteSample> workoutTrainingAcuteLoadSamples = getTrainingLoadAcuteSamples(db, device, startTs, endTs);
                if (!workoutTrainingAcuteLoadSamples.isEmpty()) {
                    acuteLoad = workoutTrainingAcuteLoadSamples.get(workoutTrainingAcuteLoadSamples.size() - 1).getValue();
                }
                List<? extends GenericTrainingLoadChronicSample> workoutTrainingChronicLoadSamples = getTrainingLoadChronicSamples(db, device, startTs, endTs);
                if (!workoutTrainingChronicLoadSamples.isEmpty()) {
                    chronicLoad = workoutTrainingChronicLoadSamples.get(workoutTrainingChronicLoadSamples.size() - 1).getValue();
                }
            }
            data.add(new LoadData((Calendar) day.clone(), load, acuteLoad, chronicLoad, i));
            day.add(Calendar.DATE, 1);
        }
        int thisWeekLoad = getWorkoutLoadSamples(db, device, (int) (startOfThisWeek.toInstant().toEpochMilli() / 1000), (int) (endOfThisWeek.toInstant().toEpochMilli() / 1000))
                .stream()
                .mapToInt(WorkoutLoadSample::getValue)
                .sum();
        int lastWeekLoad = getWorkoutLoadSamples(db, device, (int) (startOfLastWeek.toInstant().toEpochMilli() / 1000), (int) (endOfLastWeek.toInstant().toEpochMilli() / 1000))
                .stream()
                .mapToInt(WorkoutLoadSample::getValue)
                .sum();

        int latestAcuteLoad = 0;
        int latestChronicLoad = 0;
        if (supportsTrainingLoad()) {
            GenericTrainingLoadAcuteSample latestAcuteLoadSample = getLatestTrainingLoadAcuteSample(db, device, getTSEnd());
            if (latestAcuteLoadSample != null) {
                latestAcuteLoad = latestAcuteLoadSample.getValue();
            }
            GenericTrainingLoadChronicSample latestChronicLoadSample = getLatestTrainingLoadChronicSample(db, device, getTSEnd());
            if (latestChronicLoadSample != null) {
                latestChronicLoad = latestChronicLoadSample.getValue();
            }
        }
        return new LoadsData(data, latestAcuteLoad, latestChronicLoad, thisWeekLoad, lastWeekLoad);
    }

    private List<? extends WorkoutLoadSample> getWorkoutLoadSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends WorkoutLoadSample> sampleProvider = coordinator.getWorkoutLoadSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement WorkoutLoadSampleProvider", device);
            return new ArrayList<>();
        }
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    private List<? extends GenericTrainingLoadAcuteSample> getTrainingLoadAcuteSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends GenericTrainingLoadAcuteSample> sampleProvider = coordinator.getTrainingAcuteLoadSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement GenericTrainingLoadAcuteSample", device);
            return new ArrayList<>();
        }
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    private List<? extends GenericTrainingLoadChronicSample> getTrainingLoadChronicSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends GenericTrainingLoadChronicSample> sampleProvider = coordinator.getTrainingChronicLoadSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement GenericTrainingLoadChronicSample", device);
            return new ArrayList<>();
        }
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    private GenericTrainingLoadAcuteSample getLatestTrainingLoadAcuteSample(final DBHandler db, final GBDevice device, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends GenericTrainingLoadAcuteSample> sampleProvider = coordinator.getTrainingAcuteLoadSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement GenericTrainingLoadAcuteSample", device);
            return null;
        }
        return sampleProvider.getLatestSample(tsTo * 1000L);
    }

    private GenericTrainingLoadChronicSample getLatestTrainingLoadChronicSample(final DBHandler db, final GBDevice device, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends GenericTrainingLoadChronicSample> sampleProvider = coordinator.getTrainingChronicLoadSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement GenericTrainingLoadChronicSample", device);
            return null;
        }
        return sampleProvider.getLatestSample(tsTo * 1000L);
    }

    private void setupAcuteLoadChart() {
        acuteLoadChart.getDescription().setEnabled(false);
        acuteLoadChart.setTouchEnabled(false);
        acuteLoadChart.setPinchZoom(false);
        acuteLoadChart.setDoubleTapToZoomEnabled(false);


        final XAxis xAxisBottom = acuteLoadChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setAxisMaximum(29 + 0.5f);
        xAxisBottom.setAxisMinimum(0 - 0.5f);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisLeft = acuteLoadChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(1000);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(false);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = acuteLoadChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    protected void setupDailyLoadChart() {
        dailyLoadChart.getDescription().setEnabled(false);
        dailyLoadChart.setDoubleTapToZoomEnabled(false);
        dailyLoadChart.getLegend().setEnabled(false);
        dailyLoadChart.setTouchEnabled(false);

        final XAxis xAxisBottom = dailyLoadChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMaximum(29 + 0.5f);
        xAxisBottom.setAxisMinimum(0 - 0.5f);

        final YAxis yAxisLeft = dailyLoadChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setAxisMinimum(0f);

        final YAxis yAxisRight = dailyLoadChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    ValueFormatter getDailyLoadChartDayValueFormatter(LoadFragment.LoadsData data) {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                LoadFragment.LoadData day = data.getDay((int) value);
                String pattern = TOTAL_DAYS > 7 ? "dd" : "EEE";
                SimpleDateFormat formatLetterDay = new SimpleDateFormat(pattern, Locale.getDefault());
                return formatLetterDay.format(new Date(day.day.getTimeInMillis()));
            }
        };
    }

    public static int getAcuteColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.training_acute_load, typedValue, true);
        return typedValue.data;
    }

    protected void setupLegend(Chart<?> chart) {}

    protected static class LoadsData extends ChartsData {

        private int latestAcuteLoad;
        private int latestChronicLoad;
        private int thisWeekLoad;
        private int lastWeekLoad;

        private final List<LoadFragment.LoadData> data;

        public LoadsData(final List<LoadFragment.LoadData> chartsData, int latestAcuteLoad, int latestChronicLoad, int thisWeekLoad, int lastWeekLoad) {
            this.data = chartsData;
            this.latestAcuteLoad = latestAcuteLoad;
            this.latestChronicLoad = latestChronicLoad;
            this.thisWeekLoad = thisWeekLoad;
            this.lastWeekLoad = lastWeekLoad;
        }

        public LoadFragment.LoadData getDay(int i) {
            return this.data.get(i);
        }

        public LoadFragment.LoadData getCurrentDay() {
            return this.data.get(this.data.size() - 1);
        }

        public List<LoadFragment.LoadData> getData() {
            return data;
        }

        public int getLatestAcuteLoad() {
            return latestAcuteLoad;
        }

        public int getLatestChronicLoad () {
            return latestChronicLoad;
        }

        public int getLastWeekLoad() {
            return lastWeekLoad;
        }

        public int getThisWeekLoad() {
            return thisWeekLoad;
        }
    }

    protected static class LoadData {
        public Integer load;
        public Integer acuteLoad;
        public Integer chronicLoad;
        public Calendar day;
        public int i;

        public LoadData(Calendar day,
                        Integer load,
                        Integer acuteLoad,
                        Integer chronicLoad,
                        int i
        ) {
            this.load = load;
            this.chronicLoad = chronicLoad;
            this.acuteLoad = acuteLoad;
            this.day = day;
            this.i = i;
        }
    }
}
