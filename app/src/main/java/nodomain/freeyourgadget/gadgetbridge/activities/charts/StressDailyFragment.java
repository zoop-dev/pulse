/*  Copyright (C) 2023-2024 Daniel Dakhno, José Rebelo

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

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class StressDailyFragment extends StressFragment<StressDailyFragment.StressChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(StressDailyFragment.class);

    private LineChart mStressChart;
    private PieChart mStressLevelsPieChart;
    private TextView stressChartRelaxedTime;
    private TextView stressChartMildTime;
    private TextView stressChartModerateTime;
    private TextView stressChartHighTime;
    private TextView stressDate;

    private String STRESS_AVERAGE_LABEL;

    private final Prefs prefs = GBApplication.getPrefs();
    private final boolean CHARTS_SLEEP_RANGE_24H = prefs.getBoolean("chart_sleep_range_24h", false);
    private final boolean SHOW_CHARTS_AVERAGE = prefs.getBoolean("charts_show_average", true);

    private boolean showStressLevelInPercents = false;

    @Override
    protected void init() {
        super.init();
        STRESS_AVERAGE_LABEL = requireContext().getString(R.string.charts_legend_stress_average);
    }

    @Override
    protected StressChartsData refreshInBackground(final ChartsHost chartsHost, final DBHandler db, final GBDevice device) {
        int tsEnd = getTSEnd();
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(tsEnd * 1000L);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        final int tsStart = (int) (day.getTimeInMillis() / 1000);
        tsEnd = tsStart + 24 * 60 * 60 - 1;
        final List<? extends StressSample> samples = getStressSamples(db, device, tsStart, tsEnd);

        LOG.info("Got {} stress samples", samples.size());

        ensureStartAndEndSamples((List<StressSample>) samples, tsStart, tsEnd);

        showStressLevelInPercents = device.getDeviceCoordinator().showStressLevelInPercents();

        return new StressChartsDataBuilder(samples, device.getDeviceCoordinator().getStressRanges(), device.getDeviceCoordinator().getStressChartParameters()).build();
    }

    protected LineDataSet createDataSet(final StressType stressType, final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, stressType.getLabel(requireContext()));
        lineDataSet.setColor(stressType.getColor(requireContext()));
        lineDataSet.setDrawFilled(true);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setFillColor(stressType.getColor(requireContext()));
        lineDataSet.setFillAlpha(255);
        lineDataSet.setDrawValues(false);
        lineDataSet.setValueTextColor(CHART_TEXT_COLOR);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        return lineDataSet;
    }

    private void setZoneValue(TextView tv, Integer value, long totalStressTime) {
        if(showStressLevelInPercents) {
            int valuePercent = (value == null || totalStressTime == 0)? 0: (int) Math.round(((double) value / totalStressTime) * 100);
            tv.setText(String.format(Locale.ROOT,"%d%%", valuePercent));
        } else {
            if (value != null && value > 0) {
                tv.setText(DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.SECONDS));
            } else {
                tv.setText(R.string.stats_empty_value);
            }
        }
    }

    @Override
    protected void updateChartsnUIThread(final StressChartsData stressData) {
        final PieData pieData = stressData.getPieData();

        Date date = new Date((long) this.getTSEnd() * 1000);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(date);
        stressDate.setText(formattedDate);

        Map<StressType, Integer> stressZoneTimes = stressData.getStressZoneTimes();
        setZoneValue(stressChartRelaxedTime, stressZoneTimes.get(StressType.RELAXED), stressData.getTotalStressTime());
        setZoneValue(stressChartMildTime, stressZoneTimes.get(StressType.MILD), stressData.getTotalStressTime());
        setZoneValue(stressChartModerateTime, stressZoneTimes.get(StressType.MODERATE), stressData.getTotalStressTime());
        setZoneValue(stressChartHighTime, stressZoneTimes.get(StressType.HIGH), stressData.getTotalStressTime());

        if (stressData.getAverage() > 0) {
            int noc = String.valueOf(stressData.getAverage()).length();
            SpannableString pieChartCenterText = new SpannableString(stressData.getAverage() + "\n" + requireContext().getString(R.string.stress_average));
            pieChartCenterText.setSpan(new RelativeSizeSpan(1.75f), 0, noc, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pieChartCenterText.setSpan(new RelativeSizeSpan(0.72f), noc, pieChartCenterText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pieChartCenterText.setSpan(new ForegroundColorSpan(SUB_TEXT_COLOR), noc, pieChartCenterText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mStressLevelsPieChart.setCenterText(pieChartCenterText);
        } else {
            SpannableString pieChartCenterText = new SpannableString("-\n" + requireContext().getString(R.string.stress_average));
            pieChartCenterText.setSpan(new RelativeSizeSpan(1.25f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pieChartCenterText.setSpan(new RelativeSizeSpan(0.72f), 2, pieChartCenterText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            pieChartCenterText.setSpan(new ForegroundColorSpan(SUB_TEXT_COLOR), 2, pieChartCenterText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mStressLevelsPieChart.setCenterText(pieChartCenterText);
        }
        mStressLevelsPieChart.setData(pieData);

        final DefaultChartsData<LineData> chartsData = stressData.getChartsData();
        mStressChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mStressChart.getXAxis().setValueFormatter(chartsData.getXValueFormatter());
        mStressChart.setData(chartsData.getData());
        mStressChart.getAxisRight().removeAllLimitLines();

        if (stressData.getAverage() > 0) {
            final LimitLine averageLine = new LimitLine(stressData.getAverage());
            averageLine.setLineColor(Color.GRAY);
            averageLine.setLineWidth(1.5f);
            averageLine.enableDashedLine(15f, 10f, 0f);
            mStressChart.getAxisRight().addLimitLine(averageLine);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_stresschart, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> getChartsHost().enableSwipeRefresh(scrollY == 0));

        mStressChart = rootView.findViewById(R.id.stress_line_chart);
        mStressLevelsPieChart = rootView.findViewById(R.id.stress_pie_chart);
        stressChartRelaxedTime = rootView.findViewById(R.id.stress_chart_relaxed_time);
        stressChartMildTime = rootView.findViewById(R.id.stress_chart_mild_time);
        stressChartModerateTime = rootView.findViewById(R.id.stress_chart_moderate_time);
        stressChartHighTime = rootView.findViewById(R.id.stress_chart_high_time);
        stressDate = rootView.findViewById(R.id.stress_date);

        setupLineChart();
        setupPieChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
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

    private void setupLineChart() {
        mStressChart.setBackgroundColor(BACKGROUND_COLOR);
        mStressChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(mStressChart);

        final XAxis x = mStressChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setAxisMinimum(0f);
        x.setAxisMaximum(86400f);

        final YAxis yAxisLeft = mStressChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100f);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(false);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);

        final YAxis yAxisRight = mStressChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
        yAxisRight.setAxisMaximum(100f);
        yAxisRight.setAxisMinimum(0);
    }

    @Override
    protected void setupLegend(final Chart<?> chart) {
        final List<LegendEntry> legendEntries = createLegendEntries(chart);

        if (!CHARTS_SLEEP_RANGE_24H && SHOW_CHARTS_AVERAGE) {
            final LegendEntry averageEntry = new LegendEntry();
            averageEntry.label = STRESS_AVERAGE_LABEL;
            averageEntry.formColor = Color.GRAY;
            legendEntries.add(averageEntry);
        }

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected void renderCharts() {
        mStressChart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
        mStressLevelsPieChart.invalidate();
    }

    protected class StressChartsDataBuilder {
        private static final int UNKNOWN_VAL = 2;

        private final List<? extends StressSample> samples;
        private final int[] stressRanges;

        private final int sampleRate;
        private final int interval;
        private final int delta;

        private final TimestampTranslation tsTranslation = new TimestampTranslation();

        private final Map<StressType, List<Entry>> lineEntriesPerLevel = new HashMap<>();
        private final Map<StressType, Integer> accumulator = new HashMap<>();

        int previousTs;
        int currentTypeStartTs;
        StressType previousStressType;
        long averageSum;
        long averageNumSamples;

        public StressChartsDataBuilder(final List<? extends StressSample> samples, final int[] stressRanges, final int[] dataParameters) {
            this.samples = samples;
            this.stressRanges = stressRanges;
            this.sampleRate = dataParameters[0];
            this.interval = dataParameters[1];
            this.delta = dataParameters[2];
        }

        private void reset() {
            tsTranslation.reset();
            lineEntriesPerLevel.clear();
            accumulator.clear();
            for (final StressType stressType : StressType.values()) {
                lineEntriesPerLevel.put(stressType, new ArrayList<>());
                accumulator.put(stressType, 0);
            }
            previousTs = 0;
            currentTypeStartTs = 0;
            previousStressType = StressType.UNKNOWN;
        }

        private void processSamples() {
            reset();

            for (final StressSample sample : samples) {
                processSample(sample);
            }

            // Add the last block, if any
            if (currentTypeStartTs != previousTs) {
                set(previousTs, previousStressType, samples.get(samples.size() - 1).getStress());
            }
        }

        private void processSample(final StressSample sample) {
            final StressType stressType = StressType.fromStress(sample.getStress(), stressRanges);
            final int ts = tsTranslation.shorten((int) (sample.getTimestamp() / 1000L));

            if (ts == 0) {
                // First sample
                previousTs = ts;
                currentTypeStartTs = ts;
                previousStressType = stressType;
                if(interval > 0 && sample.getStress() > 0) {
                    int endTime = interval - delta;
                    set(ts, stressType, sample.getStress());
                    set(endTime - 1, stressType, sample.getStress());
                    set(endTime, StressType.UNKNOWN, UNKNOWN_VAL);
                } else {
                    set(ts, stressType, sample.getStress());
                }
                return;
            }

            if(interval > 0) {
                // For interval devices bars chard should be used.
                // Emulate bars by drawing unknown type on the start and end of interval with delta for spaces.
                if(sample.getStress() > 0) {
                    int startTime = (((ts / interval)) * interval) + delta;
                    int endTime = (((ts / interval) + 1) * interval) - delta;

                    set(startTime, StressType.UNKNOWN, UNKNOWN_VAL);
                    set(startTime + 1, stressType, sample.getStress());
                    set(endTime - 1, stressType, sample.getStress());
                    set(endTime, StressType.UNKNOWN, UNKNOWN_VAL);
                }  else {
                    set(ts, stressType, sample.getStress());
                }
            } else {
                if (ts - previousTs > sampleRate * 10) {
                    // More than 15 minutes since last sample
                    // Set to unknown right after the last sample we got until the current time
                    int lastEndTs = Math.min(previousTs + sampleRate * 5, ts - 1);
                    set(lastEndTs, StressType.UNKNOWN, UNKNOWN_VAL);
                    set(ts - 1, StressType.UNKNOWN, UNKNOWN_VAL);
                }

                set(ts, stressType, sample.getStress());
            }

            if (!stressType.equals(previousStressType)) {
                currentTypeStartTs = ts;
            }

            accumulator.computeIfPresent(stressType, (k, v) -> v + sampleRate);

            if (stressType != StressType.UNKNOWN) {
                averageSum += sample.getStress();
                averageNumSamples++;
            }

            previousStressType = stressType;
            previousTs = ts;
        }

        private void set(final int ts, final StressType stressType, final int stress) {
            for (final Map.Entry<StressType, List<Entry>> stressTypeListEntry : lineEntriesPerLevel.entrySet()) {
                if (stressTypeListEntry.getKey() == stressType) {
                    stressTypeListEntry.getValue().add(new Entry(ts, stress));
                } else {
                    stressTypeListEntry.getValue().add(new Entry(ts, 0));
                }
            }
        }

        public StressChartsData build() {
            processSamples();

            final List<ILineDataSet> lineDataSets = new ArrayList<>();
            final List<PieEntry> pieEntries = new ArrayList<>();
            final List<Integer> pieColors = new ArrayList<>();
            final Map<StressType, Integer> stressZoneTimes = new HashMap<>();

            long totalStressTime = 0;
            for (final StressType stressType : StressType.values()) {
                final List<Entry> stressEntries = lineEntriesPerLevel.get(stressType);
                lineDataSets.add(createDataSet(stressType, stressEntries));

                final Integer stressTime = accumulator.get(stressType);
                stressZoneTimes.put(stressType, stressTime);

                if (stressType != StressType.UNKNOWN && stressTime != null && stressTime != 0) {
                    totalStressTime += stressTime;
                    pieEntries.add(new PieEntry(stressTime, stressType.getLabel(requireContext())));
                    pieColors.add(stressType.getColor(requireContext()));
                }
            }

            if (pieEntries.isEmpty()) {
                pieEntries.add(new PieEntry(1));
                pieColors.add(getResources().getColor(R.color.gauge_line_color));
            }

            final PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
            pieDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS);
                }
            });
            pieDataSet.setColors(pieColors);
            pieDataSet.setValueTextColor(DESCRIPTION_COLOR);
            pieDataSet.setValueTextSize(13f);
            pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            pieDataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            pieDataSet.setDrawValues(false);
            pieDataSet.setSliceSpace(2f);
            final PieData pieData = new PieData(pieDataSet);

            final LineData lineData = new LineData(lineDataSets);
            final ValueFormatter xValueFormatter = new SampleXLabelFormatter(tsTranslation, "HH:mm");
            final DefaultChartsData<LineData> chartsData = new DefaultChartsData<>(lineData, xValueFormatter);
            return new StressChartsData(pieData, chartsData, Math.round((float) averageSum / averageNumSamples), stressZoneTimes, totalStressTime);
        }
    }

    protected static class StressChartsData extends ChartsData {
        private final PieData pieData;
        private final DefaultChartsData<LineData> chartsData;
        private final int average;
        private final Map<StressType, Integer> stressZoneTimes;
        private final long totalStressTime;

        public StressChartsData(final PieData pieData, final DefaultChartsData<LineData> chartsData, final int average, Map<StressType, Integer> stressZoneTimes, long totalStressTime) {
            this.pieData = pieData;
            this.chartsData = chartsData;
            this.average = average;
            this.stressZoneTimes = stressZoneTimes;
            this.totalStressTime = totalStressTime;
        }

        public Map<StressType, Integer> getStressZoneTimes() {
            return stressZoneTimes;
        }

        public PieData getPieData() {
            return pieData;
        }

        public DefaultChartsData<LineData> getChartsData() {
            return chartsData;
        }

        public int getAverage() {
            return average;
        }

        public long getTotalStressTime() {
            return totalStressTime;
        }
    }
}