/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, José Rebelo,
    Pavel Elagin, Petr Vaněk, a0z

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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.databinding.FragmentWeeksleepChartBinding;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;

public class SleepPeriodFragment extends SleepFragment<SleepPeriodFragment.MyChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(SleepPeriodFragment.class);

    protected int TOTAL_DAYS = getRangeDays();
    protected int TOTAL_DAYS_FOR_AVERAGE = 0;
    private MySleepWeeklyData mySleepWeeklyData;

    private FragmentWeeksleepChartBinding binding;
    protected Locale mLocale;
    protected int mTargetValue = 0;

    private final int mOffsetHours = getOffsetHours();

    protected boolean SHOW_BALANCE;

    public static SleepPeriodFragment newInstance(int totalDays) {
        SleepPeriodFragment fragmentFirst = new SleepPeriodFragment();
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

    private MySleepWeeklyData getMySleepWeeklyData(DBHandler db, Calendar day, GBDevice device) {
        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);
        TOTAL_DAYS_FOR_AVERAGE = 0;
        long awakeWeeklyTotal = 0;
        long remWeeklyTotal = 0;
        long deepWeeklyTotal = 0;
        long lightWeeklyTotal = 0;

        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            ActivityAmounts amounts = getActivityAmountsForDay(db, day, device);
            if (calculateBalance(amounts) > 0) {
                TOTAL_DAYS_FOR_AVERAGE++;
            }

            float[] totalAmounts = getTotalsForActivityAmounts(amounts);
            int i = 0;
            deepWeeklyTotal += (long) totalAmounts[i++];
            lightWeeklyTotal += (long) totalAmounts[i++];
            if (supportsRemSleep(getChartsHost().getDevice())) {
                remWeeklyTotal += (long) totalAmounts[i++];
            }
            if (supportsAwakeSleep(getChartsHost().getDevice())) {
                awakeWeeklyTotal += (long) totalAmounts[i++];
            }

            day.add(Calendar.DATE, 1);
        }

        return new MySleepWeeklyData(awakeWeeklyTotal, remWeeklyTotal, deepWeeklyTotal, lightWeeklyTotal);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLocale = getResources().getConfiguration().locale;
        binding = FragmentWeeksleepChartBinding.inflate(inflater, container, false);

        View rootView = binding.getRoot();
        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        final int goal = getGoal();
        if (goal >= 0) {
            mTargetValue = goal;
        }

        SHOW_BALANCE = GBApplication.getPrefs().getBoolean("charts_show_balance_sleep", true);
        if (SHOW_BALANCE) {
            binding.balance.setVisibility(View.VISIBLE);
        } else {
            binding.balance.setVisibility(View.GONE);
        }

        if (!supportsSleepScore()) {
            binding.sleepScoreWrapper.setVisibility(View.GONE);
        } else {
            setupSleepScoreChart();
        }

        setupWeekChart();
        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    protected void setupWeekChart() {
        BarChart weekSleepChart = binding.weekSleepChart;
        weekSleepChart.setBackgroundColor(BACKGROUND_COLOR);
        weekSleepChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        weekSleepChart.getDescription().setText("");
        weekSleepChart.setFitBars(true);

        configureBarLineChartDefaults(weekSleepChart);

        XAxis x = weekSleepChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis y = weekSleepChart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);
        y.setDrawZeroLine(true);
        y.setSpaceBottom(0);
        y.setAxisMinimum(0);
        y.setValueFormatter(getYAxisFormatter());
        y.setEnabled(true);

        YAxis yAxisRight = weekSleepChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);

        if (TOTAL_DAYS > 7) {
            weekSleepChart.setRenderer(new AngledLabelsChartRenderer(weekSleepChart, weekSleepChart.getAnimator(), weekSleepChart.getViewPortHandler()));
        } else {
            weekSleepChart.setScaleEnabled(false);
            weekSleepChart.setTouchEnabled(false);
        }
    }

    @Override
    protected void updateChartsnUIThread(MyChartsData mcd) {
        BarChart weekSleepChart = binding.weekSleepChart;
        setupLegend(weekSleepChart);

        WeekChartsData<BarData> weekBeforeData = mcd.getWeekBeforeData();
        weekSleepChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        weekSleepChart.setData(weekBeforeData.getData());
        weekSleepChart.getXAxis().setValueFormatter(mcd.getWeekBeforeData().getXValueFormatter());
        weekSleepChart.getBarData().setValueTextSize(10f);
        weekSleepChart.getBarData().setValueTextColor(LEGEND_TEXT_COLOR);

        if (supportsSleepScore()) {
            binding.sleepScoreChart.setData(null);
            binding.sleepScoreChart.getXAxis().setValueFormatter(weekBeforeData.getXValueFormatter());
            binding.sleepScoreChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
            binding.sleepScoreChart.setData(weekBeforeData.getSleepScoreData());
            binding.sleepScoreHighest.setText(weekBeforeData.getHighestSleepScore() > 0 ? String.valueOf(weekBeforeData.getHighestSleepScore()) : getString(R.string.stats_empty_value));
            binding.sleepScoreLowest.setText(weekBeforeData.getLowestSleepScore() > 0 ? String.valueOf(weekBeforeData.getLowestSleepScore()) : getString(R.string.stats_empty_value));
            binding.sleepScoreAverage.setText(weekBeforeData.getAvgSleepScore() > 0 ? String.valueOf(weekBeforeData.getAvgSleepScore()) : getString(R.string.stats_empty_value));
        }

        // The last value is for awake time, which we do not want to include in the "total sleep time"
        final int barIgnoreLast = supportsAwakeSleep(getChartsHost().getDevice()) ? 1 : 0;
        weekSleepChart.getBarData().setValueFormatter(new BarChartStackedTimeValueFormatter(false, "", 0, barIgnoreLast));

        if (TOTAL_DAYS_FOR_AVERAGE > 0) {
            float avgDeep = Math.abs(this.mySleepWeeklyData.getTotalDeep() / TOTAL_DAYS_FOR_AVERAGE);
            binding.sleepChartLegendDeepTime.setText(DateTimeUtils.formatDurationHoursMinutes((int) avgDeep, TimeUnit.MINUTES));
            float avgLight = Math.abs(this.mySleepWeeklyData.getTotalLight() / TOTAL_DAYS_FOR_AVERAGE);
            binding.sleepChartLegendLightTime.setText(DateTimeUtils.formatDurationHoursMinutes((int) avgLight, TimeUnit.MINUTES));
            float avgRem = Math.abs(this.mySleepWeeklyData.getTotalRem() / TOTAL_DAYS_FOR_AVERAGE);
            binding.sleepChartLegendRemTime.setText(DateTimeUtils.formatDurationHoursMinutes((int) avgRem, TimeUnit.MINUTES));
            float avgAwake = Math.abs(this.mySleepWeeklyData.getTotalAwake() / TOTAL_DAYS_FOR_AVERAGE);
            binding.sleepChartLegendAwakeTime.setText(DateTimeUtils.formatDurationHoursMinutes((int) avgAwake, TimeUnit.MINUTES));
        } else {
            binding.sleepChartLegendDeepTime.setText("-");
            binding.sleepChartLegendLightTime.setText("-");
            binding.sleepChartLegendRemTime.setText("-");
            binding.sleepChartLegendAwakeTime.setText("-");
        }

        if (!supportsRemSleep(getChartsHost().getDevice())) {
            binding.sleepChartLegendRemTimeWrapper.setVisibility(View.GONE);
        }

        if (!supportsAwakeSleep(getChartsHost().getDevice())) {
            binding.sleepChartLegendAwakeTimeWrapper.setVisibility(View.GONE);
        }

        binding.sleepDates.setText(DateTimeUtils.formatDaysUntil(TOTAL_DAYS, getTSEnd()));

        binding.balance.setText(mcd.getWeekBeforeData().getBalanceMessage());
    }

    @Override
    protected MyChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        //NB: we could have omitted the day, but this way we can move things to the past easily
        WeekChartsData<BarData> weekBeforeData = refreshWeekBeforeData(db, binding.weekSleepChart, day, device);
        mySleepWeeklyData = getMySleepWeeklyData(db, day, device);

        return new MyChartsData(weekBeforeData);
    }

    protected WeekChartsData<BarData> refreshWeekBeforeData(DBHandler db, BarChart barChart, Calendar day, GBDevice device) {
        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);
        List<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<String>();

        long balance = 0;
        long daily_balance = 0;
        TOTAL_DAYS_FOR_AVERAGE = 0;
        List<Entry> sleepScoreEntities = new ArrayList<>();
        final Accumulator sleepScoreAccumulator = new Accumulator();
        final List<ILineDataSet> sleepScoreDataSets = new ArrayList<>();
        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            // Sleep stages
            ActivityAmounts amounts = getActivityAmountsForDay(db, day, device);
            daily_balance = calculateBalance(amounts);
            if (daily_balance > 0) {
                TOTAL_DAYS_FOR_AVERAGE++;
            }
            balance += daily_balance;
            entries.add(new BarEntry(counter, getTotalsForActivityAmounts(amounts)));
            labels.add(getWeeksChartsLabel(day));
            // Sleep score
            if (supportsSleepScore()) {
                List<? extends SleepScoreSample> sleepScoreSamples = getSleepScoreSamples(db, device, day);
                if (!sleepScoreSamples.isEmpty() && sleepScoreSamples.get(sleepScoreSamples.size() - 1).getSleepScore() > 0) {
                    int sleepScore = sleepScoreSamples.get(sleepScoreSamples.size() - 1).getSleepScore();
                    sleepScoreAccumulator.add(sleepScore);
                    sleepScoreEntities.add(new Entry(counter, sleepScore));
                } else {
                    if (!sleepScoreEntities.isEmpty()) {
                        List<Entry> clone = new ArrayList<>(sleepScoreEntities.size());
                        clone.addAll(sleepScoreEntities);
                        sleepScoreDataSets.add(createSleepScoreDataSet(clone));
                        sleepScoreEntities.clear();
                    }
                }
            }
            day.add(Calendar.DATE, 1);
        }
        if (!sleepScoreEntities.isEmpty()) {
            sleepScoreDataSets.add(createSleepScoreDataSet(sleepScoreEntities));
        }
        final LineData sleepScoreLineData = new LineData(sleepScoreDataSets);
        sleepScoreLineData.setHighlightEnabled(false);

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(getColors());
        set.setValueFormatter(getBarValueFormatter());

        BarData barData = new BarData(set);
        barData.setValueTextColor(Color.GRAY); //prevent tearing other graph elements with the black text. Another approach would be to hide the values cmpletely with data.setDrawValues(false);
        barData.setValueTextSize(10f);

        barChart.getAxisLeft().setAxisMaximum(Math.max(set.getYMax(), mTargetValue) + 60);

        LimitLine target = new LimitLine(mTargetValue);
        target.setLineWidth(1.5f);
        target.enableDashedLine(15f, 10f, 0f);
        target.setLineColor(getResources().getColor(R.color.chart_deep_sleep_dark));
        barChart.getAxisLeft().removeAllLimitLines();
        barChart.getAxisLeft().addLimitLine(target);

        float average = 0;
        if (TOTAL_DAYS_FOR_AVERAGE > 0) {
            average = Math.abs(balance / TOTAL_DAYS_FOR_AVERAGE);
        }
        LimitLine average_line = new LimitLine(average);
        average_line.setLineWidth(1.5f);
        average_line.enableDashedLine(15f, 10f, 0f);
        average_line.setLabel(getString(R.string.average, getAverage(average)));

        if (average > (mTargetValue)) {
            average_line.setLineColor(Color.GREEN);
            average_line.setTextColor(Color.GREEN);
        } else {
            average_line.setLineColor(Color.RED);
            average_line.setTextColor(Color.RED);
        }
        if (average > 0) {
            if (GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
                barChart.getAxisLeft().addLimitLine(average_line);
            }
        }

        if (supportsSleepScore()) {
            return new WeekChartsData(
                    barData,
                    new PreformattedXIndexLabelFormatter(labels),
                    getBalanceMessage(balance, mTargetValue),
                    sleepScoreLineData,
                    (int) Math.round(sleepScoreAccumulator.getAverage()),
                    (int) Math.round(sleepScoreAccumulator.getMax()),
                    (int) Math.round(sleepScoreAccumulator.getMin())
            );
        }
        return new WeekChartsData(barData, new PreformattedXIndexLabelFormatter(labels), getBalanceMessage(balance, mTargetValue));
    }

    protected String getWeeksChartsLabel(Calendar day) {
        if (TOTAL_DAYS > 7) {
            //month, show day date
            return String.valueOf(day.get(Calendar.DAY_OF_MONTH));
        } else {
            //week, show short day name
            return day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, mLocale);
        }
    }

    protected LineDataSet createSleepScoreDataSet(final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.sleep_score));
        lineDataSet.setColor(getResources().getColor(R.color.chart_light_sleep_light));
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setCircleColor(getResources().getColor(R.color.chart_light_sleep_light));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(true);
        lineDataSet.setValueTextSize(10f);
        lineDataSet.setValueTextColor(LEGEND_TEXT_COLOR);
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.ROOT, "%d", (int) value);
            }
        });
        return lineDataSet;
    }

    private void setupSleepScoreChart() {
        final XAxis xAxisBottom = binding.sleepScoreChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(TOTAL_DAYS - 1);
        xAxisBottom.setGranularity(1f);
        xAxisBottom.setGranularityEnabled(true);

        final YAxis yAxisLeft = binding.sleepScoreChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = binding.sleepScoreChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);

        binding.sleepScoreChart.setDoubleTapToZoomEnabled(false);
        binding.sleepScoreChart.getDescription().setEnabled(false);
        binding.sleepScoreChart.getDescription().setTextColor(LEGEND_TEXT_COLOR);
        if (TOTAL_DAYS <= 7) {
            binding.sleepScoreChart.setScaleEnabled(false);
            binding.sleepScoreChart.setTouchEnabled(false);
        }
    }

    @Override
    protected void renderCharts() {
        binding.weekSleepChart.invalidate();
        binding.sleepScoreChart.invalidate();
    }

    @Override
    public String getTitle() {
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            return getString(R.string.weeksleepchart_sleep_a_month);
        } else {
            return getString(R.string.weeksleepchart_sleep_a_week);
        }
    }

    String getPieDescription(int targetValue) {
        return getString(R.string.weeksleepchart_today_sleep_description, DateTimeUtils.formatDurationHoursMinutes(targetValue, TimeUnit.MINUTES));
    }

    int getGoal() {
        return GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_SLEEP_DURATION_MINUTES, ActivityUser.defaultUserSleepDurationGoal);
    }

    int getOffsetHours() {
        return -12;
    }


    protected long calculateBalance(ActivityAmounts activityAmounts) {
        long balance = 0;

        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            if (amount.getActivityKind() == ActivityKind.DEEP_SLEEP ||
                    amount.getActivityKind() == ActivityKind.LIGHT_SLEEP ||
                    amount.getActivityKind() == ActivityKind.REM_SLEEP) {
                balance += amount.getTotalSeconds();
            }
        }
        return (int) (balance / 60);
    }

    protected String getBalanceMessage(long balance, int targetValue) {
        if (balance > 0) {
            final long totalBalance = balance - ((long) targetValue * TOTAL_DAYS_FOR_AVERAGE);
            if (totalBalance > 0)
                return getString(R.string.overslept, getHM(totalBalance));
            else
                return getString(R.string.lack_of_sleep, getHM(Math.abs(totalBalance)));
        } else
            return getString(R.string.no_data);
    }

    float[] getTotalsForActivityAmounts(ActivityAmounts activityAmounts) {
        long totalSecondsDeepSleep = 0;
        long totalSecondsLightSleep = 0;
        long totalSecondsRemSleep = 0;
        long totalSecondsAwakeSleep = 0;
        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            if (amount.getActivityKind() == ActivityKind.DEEP_SLEEP) {
                totalSecondsDeepSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.LIGHT_SLEEP) {
                totalSecondsLightSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.REM_SLEEP) {
                totalSecondsRemSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.AWAKE_SLEEP) {
                totalSecondsAwakeSleep += amount.getTotalSeconds();
            }
        }
        int totalMinutesDeepSleep = (int) (totalSecondsDeepSleep / 60);
        int totalMinutesLightSleep = (int) (totalSecondsLightSleep / 60);
        int totalMinutesRemSleep = (int) (totalSecondsRemSleep / 60);
        int totalMinutesAwakeSleep = (int) (totalSecondsAwakeSleep / 60);

        float[] activityAmountsTotals = {totalMinutesDeepSleep, totalMinutesLightSleep};
        if (supportsRemSleep(getChartsHost().getDevice())) {
            activityAmountsTotals = ArrayUtils.add(activityAmountsTotals, totalMinutesRemSleep);
        }
        if (supportsAwakeSleep(getChartsHost().getDevice())) {
            activityAmountsTotals = ArrayUtils.add(activityAmountsTotals, totalMinutesAwakeSleep);
        }

        return activityAmountsTotals;
    }

    protected String formatPieValue(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    String[] getPieLabels() {
        String[] labels = {
                getString(R.string.abstract_chart_fragment_kind_deep_sleep),
                getString(R.string.abstract_chart_fragment_kind_light_sleep)
        };
        if (supportsRemSleep(getChartsHost().getDevice())) {
            labels = ArrayUtils.add(labels, getString(R.string.abstract_chart_fragment_kind_rem_sleep));
        }
        if (supportsAwakeSleep(getChartsHost().getDevice())) {
            labels = ArrayUtils.add(labels, getString(R.string.abstract_chart_fragment_kind_awake_sleep));
        }
        return labels;
    }

    ValueFormatter getPieValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatPieValue((long) value);
            }
        };
    }

    ValueFormatter getBarValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    ValueFormatter getYAxisFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    int[] getColors() {
        int[] colors = {akDeepSleep.color, akLightSleep.color};
        if (supportsRemSleep(getChartsHost().getDevice())) {
            colors = ArrayUtils.add(colors, akRemSleep.color);
        }
        if (supportsAwakeSleep(getChartsHost().getDevice())) {
            colors = ArrayUtils.add(colors, akAwakeSleep.color);
        }
        return colors;
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = super.createLegendEntries(chart);
        chart.getLegend().setCustom(legendEntries);

        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    private String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    String getAverage(float value) {
        return getHM((long) value);
    }

    protected static class MyChartsData extends ChartsData {
        private final WeekChartsData<BarData> weekBeforeData;

        MyChartsData(WeekChartsData<BarData> weekBeforeData) {
            this.weekBeforeData = weekBeforeData;
        }

        WeekChartsData<BarData> getWeekBeforeData() {
            return weekBeforeData;
        }
    }

    protected ActivityAmounts getActivityAmountsForDay(DBHandler db, Calendar day, GBDevice device) {

        LimitedQueue<Integer, ActivityAmounts> activityAmountCache = null;
        ActivityAmounts amounts = null;

        Activity activity = getActivity();
        int key = (int) (day.getTimeInMillis() / 1000) + (mOffsetHours * 3600);
        if (activity != null) {
            activityAmountCache = ((ActivityChartsActivity) activity).mActivityAmountCache;
            amounts = activityAmountCache.lookup(key);
        }

        if (amounts == null) {
            ActivityAnalysis analysis = new ActivityAnalysis();
            amounts = analysis.calculateActivityAmounts(getSamplesOfDay(db, day, mOffsetHours, device));
            if (activityAmountCache != null) {
                activityAmountCache.add(key, amounts);
            }
        }

        return amounts;
    }

    private List<? extends ActivitySample> getSamplesOfDay(DBHandler db, Calendar day, int offsetHours, GBDevice device) {
        int startTs;
        int endTs;

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, offsetHours);

        startTs = (int) (day.getTimeInMillis() / 1000);
        endTs = startTs + 24 * 60 * 60 - 1;

        return getSamples(db, device, startTs, endTs);
    }

    private int getRangeDays() {
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            return 30;
        } else {
            return 7;
        }
    }

    protected class WeekChartsData<T extends ChartData<?>> extends DefaultChartsData<T> {
        private final String balanceMessage;
        private LineData sleepScoresLineData;
        private int avgSleepScore;
        private int highestSleepScore;
        private int lowestSleepScore;

        public WeekChartsData(T data, PreformattedXIndexLabelFormatter xIndexLabelFormatter, String balanceMessage) {
            super(data, xIndexLabelFormatter);
            this.balanceMessage = balanceMessage;
        }

        public WeekChartsData(T data, PreformattedXIndexLabelFormatter xIndexLabelFormatter, String balanceMessage, LineData sleepScores, int avgSleepScore, int highestSleepScore, int lowestSleepScore) {
            super(data, xIndexLabelFormatter);
            this.balanceMessage = balanceMessage;
            this.sleepScoresLineData = sleepScores;
            this.avgSleepScore = avgSleepScore;
            this.highestSleepScore = highestSleepScore;
            this.lowestSleepScore = lowestSleepScore;
        }

        public int getHighestSleepScore() {
            return highestSleepScore;
        }

        public int getLowestSleepScore() {
            return lowestSleepScore;
        }

        public int getAvgSleepScore() {
            return avgSleepScore;
        }

        public String getBalanceMessage() {
            return balanceMessage;
        }

        public LineData getSleepScoreData() {
            return sleepScoresLineData;
        }
    }

    private static class MySleepWeeklyData {
        private final long totalAwake;
        private final long totalRem;
        private final long totalDeep;
        private final long totalLight;
        private final int totalDaysForAverage;

        public MySleepWeeklyData(long totalAwake, long totalRem, long totalDeep, long totalLight) {
            this.totalDeep = totalDeep;
            this.totalRem = totalRem;
            this.totalAwake = totalAwake;
            this.totalLight = totalLight;
            this.totalDaysForAverage = 0;
        }

        public long getTotalAwake() {
            return this.totalAwake;
        }

        public long getTotalRem() {
            return this.totalRem;
        }

        public long getTotalDeep() {
            return this.totalDeep;
        }

        public long getTotalLight() {
            return this.totalLight;
        }

        public int getTotalDaysForAverage() {
            return this.totalDaysForAverage;
        }
    }
}
