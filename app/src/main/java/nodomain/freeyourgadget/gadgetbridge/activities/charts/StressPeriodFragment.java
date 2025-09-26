/*  Copyright (C) 2023-2024

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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class StressPeriodFragment extends StressFragment<StressPeriodFragment.MyChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(StressPeriodFragment.class);

    protected int TOTAL_DAYS = getRangeDays();
    protected int TOTAL_DAYS_FOR_AVERAGE = 0;

    private TextView relaxedStressTimeText;
    private TextView mildStressTimeText;
    private TextView moderateStressTimeText;
    private TextView highStressTimeText;
    private TextView stressDatesText;
    private PieChart mStressLevelsPieChart;
    private BarChart mWeekChart;

    private MyStressWeeklyData myStressWeeklyData;
    private boolean showStressLevelInPercents = false;
    protected Locale mLocale;

    public static StressPeriodFragment newInstance(int totalDays) {
        StressPeriodFragment fragment = new StressPeriodFragment();
        Bundle args = new Bundle();
        args.putInt("totalDays", totalDays);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOTAL_DAYS = getArguments() != null ? getArguments().getInt("totalDays") : 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLocale = getResources().getConfiguration().locale;
        View rootView = inflater.inflate(R.layout.fragment_weekstress_chart, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        mWeekChart = rootView.findViewById(R.id.weekstresschart);
        mStressLevelsPieChart = rootView.findViewById(R.id.stress_pie_chart);
        relaxedStressTimeText = rootView.findViewById(R.id.stress_chart_relaxed_time);
        mildStressTimeText = rootView.findViewById(R.id.stress_chart_mild_time);
        moderateStressTimeText = rootView.findViewById(R.id.stress_chart_moderate_time);
        highStressTimeText = rootView.findViewById(R.id.stress_chart_high_time);
        stressDatesText = rootView.findViewById(R.id.stress_dates);

        setupPieChart();
        setupWeekChart();
        refresh();

        return rootView;
    }

    @Override
    protected MyChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        DefaultChartsData<BarData> weekBeforeData = refreshWeekBeforeStressData(db, mWeekChart, day, device);
        myStressWeeklyData = getMyStressWeeklyData(db, day, device);

        return new MyChartsData(weekBeforeData);
    }

    @Override
    protected void updateChartsnUIThread(MyChartsData mcd) {
        setupLegend(mWeekChart);

        mWeekChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mWeekChart.setData(mcd.getWeekBeforeData().getData());
        mWeekChart.getXAxis().setValueFormatter(mcd.getWeekBeforeData().getXValueFormatter());
        mWeekChart.getBarData().setValueTextSize(10f);

        updatePieChart();
        updateStressTimeTexts();

        stressDatesText.setText(DateTimeUtils.formatDaysUntil(TOTAL_DAYS, getTSEnd()));
    }

    private void updatePieChart() {
        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> pieColors = new ArrayList<>();

        if (TOTAL_DAYS_FOR_AVERAGE > 0 && myStressWeeklyData != null) {
            long totalTime = myStressWeeklyData.totalStressTime();
            if (totalTime > 0) {
                if (myStressWeeklyData.totalRelaxed() > 0) {
                    pieEntries.add(new PieEntry(myStressWeeklyData.totalRelaxed(),
                            StressType.RELAXED.getLabel(getContext())));
                    pieColors.add(StressType.RELAXED.getColor(getContext()));
                }
                if (myStressWeeklyData.totalMild() > 0) {
                    pieEntries.add(new PieEntry(myStressWeeklyData.totalMild(),
                            StressType.MILD.getLabel(getContext())));
                    pieColors.add(StressType.MILD.getColor(getContext()));
                }
                if (myStressWeeklyData.totalModerate() > 0) {
                    pieEntries.add(new PieEntry(myStressWeeklyData.totalModerate(),
                            StressType.MODERATE.getLabel(getContext())));
                    pieColors.add(StressType.MODERATE.getColor(getContext()));
                }
                if (myStressWeeklyData.totalHigh() > 0) {
                    pieEntries.add(new PieEntry(myStressWeeklyData.totalHigh(),
                            StressType.HIGH.getLabel(getContext())));
                    pieColors.add(StressType.HIGH.getColor(getContext()));
                }
            }
        }

        if (pieEntries.isEmpty()) {
            pieEntries.add(new PieEntry(1));
            pieColors.add(getResources().getColor(R.color.gauge_line_color));
        }

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(pieColors);
        pieDataSet.setValueTextColor(DESCRIPTION_COLOR);
        pieDataSet.setValueTextSize(13f);
        pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieDataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieDataSet.setDrawValues(false);
        pieDataSet.setSliceSpace(2f);
        PieData pieData = new PieData(pieDataSet);

        mStressLevelsPieChart.setData(pieData);

        // Set center text with average stress level
        if (myStressWeeklyData != null && myStressWeeklyData.averageStress() > 0) {
            int avgStress = myStressWeeklyData.averageStress();
            int noc = String.valueOf(avgStress).length();
            SpannableString centerText = new SpannableString(avgStress + "\n" +
                    getContext().getString(R.string.stress_average));
            centerText.setSpan(new RelativeSizeSpan(1.75f), 0, noc, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            centerText.setSpan(new RelativeSizeSpan(0.72f), noc, centerText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mStressLevelsPieChart.setCenterText(centerText);
        } else {
            SpannableString centerText = new SpannableString("-\n" +
                    getContext().getString(R.string.stress_average));
            centerText.setSpan(new RelativeSizeSpan(1.25f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            centerText.setSpan(new RelativeSizeSpan(0.72f), 2, centerText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mStressLevelsPieChart.setCenterText(centerText);
        }
    }

    private void updateStressTimeTexts() {
        if (TOTAL_DAYS_FOR_AVERAGE > 0 && myStressWeeklyData != null) {
            int relaxedAvg = (int) (myStressWeeklyData.totalRelaxed() / TOTAL_DAYS_FOR_AVERAGE);
            int mildAvg = (int) (myStressWeeklyData.totalMild() / TOTAL_DAYS_FOR_AVERAGE);
            int moderateAvg = (int) (myStressWeeklyData.totalModerate() / TOTAL_DAYS_FOR_AVERAGE);
            int highAvg = (int) (myStressWeeklyData.totalHigh() / TOTAL_DAYS_FOR_AVERAGE);

            if (showStressLevelInPercents) {
                long totalDailyAvg = relaxedAvg + mildAvg + moderateAvg + highAvg;
                relaxedStressTimeText.setText(String.format(Locale.ROOT, "%d%%",
                        totalDailyAvg > 0 ? Math.round(100f * relaxedAvg / totalDailyAvg) : 0));
                mildStressTimeText.setText(String.format(Locale.ROOT, "%d%%",
                        totalDailyAvg > 0 ? Math.round(100f * mildAvg / totalDailyAvg) : 0));
                moderateStressTimeText.setText(String.format(Locale.ROOT, "%d%%",
                        totalDailyAvg > 0 ? Math.round(100f * moderateAvg / totalDailyAvg) : 0));
                highStressTimeText.setText(String.format(Locale.ROOT, "%d%%",
                        totalDailyAvg > 0 ? Math.round(100f * highAvg / totalDailyAvg) : 0));
            } else {
                relaxedStressTimeText.setText(DateTimeUtils.formatDurationHoursMinutes(relaxedAvg, TimeUnit.SECONDS));
                mildStressTimeText.setText(DateTimeUtils.formatDurationHoursMinutes(mildAvg, TimeUnit.SECONDS));
                moderateStressTimeText.setText(DateTimeUtils.formatDurationHoursMinutes(moderateAvg, TimeUnit.SECONDS));
                highStressTimeText.setText(DateTimeUtils.formatDurationHoursMinutes(highAvg, TimeUnit.SECONDS));
            }
        } else {
            relaxedStressTimeText.setText("-");
            mildStressTimeText.setText("-");
            moderateStressTimeText.setText("-");
            highStressTimeText.setText("-");
        }
    }

    private MyStressWeeklyData getMyStressWeeklyData(DBHandler db, Calendar day, GBDevice device) {
        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);
        TOTAL_DAYS_FOR_AVERAGE = 0;

        long relaxedWeeklyTotal = 0;
        long mildWeeklyTotal = 0;
        long moderateWeeklyTotal = 0;
        long highWeeklyTotal = 0;
        long totalStressTime = 0;
        long avgStressSum = 0;
        long avgStressSamples = 0;

        int[] stressRanges = device.getDeviceCoordinator().getStressRanges();
        showStressLevelInPercents = device.getDeviceCoordinator().showStressLevelInPercents();

        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        int sampleRate = 60; // Default sample rate
        int[] params = coordinator.getStressChartParameters();
        if (params != null && params.length > 0) {
            sampleRate = params[0];
        }

        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            Calendar dayStart = (Calendar) day.clone();
            Calendar dayEnd = (Calendar) day.clone();
            dayEnd.add(Calendar.DAY_OF_MONTH, 1);

            List<? extends StressSample> samples = getStressSamples(db, device,
                    (int) (dayStart.getTimeInMillis() / 1000),
                    (int) (dayEnd.getTimeInMillis() / 1000));

            if (!samples.isEmpty()) {
                TOTAL_DAYS_FOR_AVERAGE++;

                Map<StressType, Integer> dailyTotals = calculateStressTotals(samples, stressRanges, sampleRate);
                relaxedWeeklyTotal += dailyTotals.getOrDefault(StressType.RELAXED, 0);
                mildWeeklyTotal += dailyTotals.getOrDefault(StressType.MILD, 0);
                moderateWeeklyTotal += dailyTotals.getOrDefault(StressType.MODERATE, 0);
                highWeeklyTotal += dailyTotals.getOrDefault(StressType.HIGH, 0);

                // Calculate average stress for the day
                int dailyAverage = calculateAverageStress(samples);
                if (dailyAverage > 0) {
                    avgStressSum += dailyAverage;
                    avgStressSamples++;
                }
            }

            day.add(Calendar.DATE, 1);
        }

        totalStressTime = relaxedWeeklyTotal + mildWeeklyTotal + moderateWeeklyTotal + highWeeklyTotal;
        int averageStress = avgStressSamples > 0 ? Math.round((float) avgStressSum / avgStressSamples) : 0;

        return new MyStressWeeklyData(relaxedWeeklyTotal, mildWeeklyTotal, moderateWeeklyTotal,
                highWeeklyTotal, totalStressTime, averageStress);
    }

    private DefaultChartsData<BarData> refreshWeekBeforeStressData(DBHandler db, BarChart chart, Calendar day, GBDevice device) {
        day = (Calendar) day.clone();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);
        List<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int[] colors = new int[TOTAL_DAYS * 5]; // 4 stress types + unknown type
        int colorIndex = 0;

        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        int sampleRate = 60;
        int[] params = coordinator.getStressChartParameters();
        if (params != null && params.length > 0) {
            sampleRate = params[0];
        }

        Calendar now = Calendar.getInstance();

        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            Calendar dayStart = (Calendar) day.clone();
            Calendar dayEnd = (Calendar) day.clone();
            dayEnd.add(Calendar.DAY_OF_MONTH, 1);

            List<? extends StressSample> samples = getStressSamples(db, device,
                    (int) (dayStart.getTimeInMillis() / 1000),
                    (int) (dayEnd.getTimeInMillis() / 1000));

            Map<StressType, Integer> dailyTotals = calculateStressTotals(samples,
                    device.getDeviceCoordinator().getStressRanges(), sampleRate);

            float[] yValues = new float[5]; // For stacked bar chart
            int idx = 0;

            float totalMinutesTracked = dailyTotals.values().stream().reduce(0, Integer::sum) / 60f;

            // Calculate the total possible minutes for this day, excluding future time
            float totalPossibleMinutes;
            if (dayEnd.before(now) || dayEnd.equals(now)) {
                // Full day in the past
                totalPossibleMinutes = 24 * 60;
            } else if (dayStart.after(now)) {
                // Full day in the future
                totalPossibleMinutes = 0;
            } else {
                // Partial day (current day) - only count minutes up to now
                long minutesFromStartToNow = (now.getTimeInMillis() - dayStart.getTimeInMillis()) / (1000 * 60);
                totalPossibleMinutes = Math.max(0, minutesFromStartToNow);
            }

            float untrackedMins = Math.max(totalPossibleMinutes - totalMinutesTracked, 0);

            yValues[idx++] = dailyTotals.get(StressType.HIGH) / 60f;
            yValues[idx++] = dailyTotals.get(StressType.MODERATE) / 60f;
            yValues[idx++] = dailyTotals.get(StressType.MILD) / 60f;
            yValues[idx++] = dailyTotals.get(StressType.RELAXED) / 60f;
            yValues[idx++] = untrackedMins;

            colors[colorIndex++] = StressType.HIGH.getColor(getContext());
            colors[colorIndex++] = StressType.MODERATE.getColor(getContext());
            colors[colorIndex++] = StressType.MILD.getColor(getContext());
            colors[colorIndex++] = StressType.RELAXED.getColor(getContext());
            colors[colorIndex++] = StressType.UNKNOWN.getColor(getContext());

            entries.add(new BarEntry(counter, yValues));

            labels.add(TOTAL_DAYS > 7
                    ? String.valueOf(day.get(Calendar.DAY_OF_MONTH))
                    : day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, mLocale));

            day.add(Calendar.DATE, 1);
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColors(colors);
        set.setStackLabels(new String[]{
                StressType.HIGH.getLabel(getContext()),
                StressType.MODERATE.getLabel(getContext()),
                StressType.MILD.getLabel(getContext()),
                StressType.RELAXED.getLabel(getContext()),
                StressType.UNKNOWN.getLabel(getContext())
        });

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);

        BarData barData = new BarData(dataSets);
        barData.setBarWidth(0.9f);

        return new DefaultChartsData<>(barData, new PreformattedXIndexLabelFormatter(labels));
    }

    private void setupPieChart() {
        mStressLevelsPieChart.setBackgroundColor(BACKGROUND_COLOR);
        mStressLevelsPieChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mStressLevelsPieChart.setEntryLabelColor(DESCRIPTION_COLOR);
        mStressLevelsPieChart.getDescription().setText("");
        mStressLevelsPieChart.setNoDataText("");
        mStressLevelsPieChart.setTouchEnabled(false);
        mStressLevelsPieChart.setCenterTextColor(GBApplication.getTextColor(getContext()));
        mStressLevelsPieChart.setCenterTextSize(18f);
        mStressLevelsPieChart.setHoleColor(requireContext().getResources().getColor(R.color.transparent));
        mStressLevelsPieChart.setHoleRadius(85);
        mStressLevelsPieChart.setDrawEntryLabels(false);
        mStressLevelsPieChart.getLegend().setEnabled(false);
    }

    protected void setupWeekChart() {
        mWeekChart.setBackgroundColor(BACKGROUND_COLOR);
        mWeekChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mWeekChart.getDescription().setText("");
        mWeekChart.setFitBars(true);

        configureBarLineChartDefaults(mWeekChart);

        XAxis x = mWeekChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        if (TOTAL_DAYS > 7) {
            x.setSpaceMin(0);
        }

        YAxis y = mWeekChart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setEnabled(true);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);
        y.setDrawZeroLine(true);
        y.setSpaceBottom(0);
        y.setAxisMinimum(0);
        y.setAxisMaximum(24 * 60);
        y.setValueFormatter(getYAxisFormatter());

        YAxis yAxisRight = mWeekChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);

        if (TOTAL_DAYS > 7) {
            mWeekChart.setRenderer(new AngledLabelsChartRenderer(mWeekChart, mWeekChart.getAnimator(),
                    mWeekChart.getViewPortHandler()));
        } else {
            mWeekChart.setScaleEnabled(false);
            mWeekChart.setTouchEnabled(false);
        }
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = createLegendEntries(chart);
        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    @Override
    protected void renderCharts() {
        mWeekChart.invalidate();
        mStressLevelsPieChart.invalidate();
    }

    private int getRangeDays() {
        return GBApplication.getPrefs().getBoolean("charts_range", true) ? 30 : 7;
    }

    ValueFormatter getYAxisFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    @Override
    protected boolean isSingleDay() {
        return false;
    }

    protected static class MyChartsData extends ChartsData {
        private final DefaultChartsData<BarData> weekBeforeData;

        public MyChartsData(DefaultChartsData<BarData> weekBeforeData) {
            this.weekBeforeData = weekBeforeData;
        }

        public DefaultChartsData<BarData> getWeekBeforeData() {
            return weekBeforeData;
        }
    }

    private record MyStressWeeklyData(long totalRelaxed,
                                      long totalMild,
                                      long totalModerate,
                                      long totalHigh,
                                      long totalStressTime,
                                      int averageStress) {
    }
}