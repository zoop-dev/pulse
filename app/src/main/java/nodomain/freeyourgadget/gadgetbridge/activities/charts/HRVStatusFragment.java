/*  Copyright (C) 2017-2024 a0z, José Rebelo

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardHrvWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;


public class HRVStatusFragment extends AbstractChartFragment<HRVStatusFragment.HRVStatusWeeklyData> {
    protected static final Logger LOG = LoggerFactory.getLogger(HRVStatusFragment.class);
    private static final String ARG_VIEW_MODE = "view_mode";
    private static final String ARG_TOTAL_DAYS = "total_days";
    private static final int DEFAULT_TOTAL_DAYS = 7;
    private static final float LEGEND_FORM_SIZE = 10f;
    private static final float LEGEND_X_ENTRY_SPACE = 10f;
    private static final float LEGEND_Y_ENTRY_SPACE = 4f;
    private static final float LEGEND_FORM_TO_TEXT_SPACE = 5f;

    private enum ViewMode {
        LAST_NIGHT,
        PERIOD
    }

    private enum PeriodDataType {
        DAILY_AVERAGE,
        LAST_NIGHT_AVERAGE,
        DAILY_RANGE
    }

    private static final PeriodDataType[] PERIOD_DATA_TYPE_ORDER = {
            PeriodDataType.DAILY_AVERAGE,
            PeriodDataType.LAST_NIGHT_AVERAGE,
            PeriodDataType.DAILY_RANGE
    };
    private static final PeriodDataType DEFAULT_PERIOD_DATA_TYPE = PeriodDataType.DAILY_AVERAGE;

    protected GaugeDrawer gaugeDrawer;
    private ImageView mHRVStatusGauge;
    private CombinedChart mWeeklyHRVStatusChart;
    private ChipGroup mHRVStatusDataTypeGroup;
    private TextView mHRVStatusSevenDaysAvg;
    private TextView mHRVStatusSevenDaysAvgStatus; // Balanced, Unbalanced, Low
    private TextView mHRVStatusLastNight;
    private TextView mHRVStatusLastNightLabel;
    private TextView mHRVStatusLastNight5MinHighest;
    private TextView mHRVStatusLastNight5MinHighestLabel;
    private TextView mHRVStatusDayAvg;
    private TextView mHRVStatusBaseline;
    private TextView mDateView;
    private TextView mHRVGaugeValue;
    private TextView mHRVGaugeStatus;
    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int TEXT_COLOR;
    protected int HRV_AVERAGE_COLOR;
    protected int HRV_RANGE_COLOR;
    protected int HRV_LAST_NIGHT_COLOR;
    protected int HRV_BASELINE_FILL_COLOR;

    private ViewMode viewMode = ViewMode.PERIOD;
    private int totalDays = DEFAULT_TOTAL_DAYS;
    private boolean showDailyAverage = true;
    private boolean showLastNightAverage = true;
    private boolean showDailyRange = false;

    public static HRVStatusFragment newLastNightInstance() {
        final HRVStatusFragment fragment = new HRVStatusFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_VIEW_MODE, ViewMode.LAST_NIGHT.name());
        fragment.setArguments(args);
        return fragment;
    }

    public static HRVStatusFragment newPeriodInstance(final int totalDays) {
        final HRVStatusFragment fragment = new HRVStatusFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_VIEW_MODE, ViewMode.PERIOD.name());
        args.putInt(ARG_TOTAL_DAYS, totalDays);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (getArguments() != null) {
            final String viewModeArg = getArguments().getString(ARG_VIEW_MODE, ViewMode.PERIOD.name());
            viewMode = ViewMode.valueOf(viewModeArg);
            totalDays = getArguments().getInt(ARG_TOTAL_DAYS, DEFAULT_TOTAL_DAYS);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean isSingleDay() {
        return viewMode == ViewMode.LAST_NIGHT;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_hrv_status, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        mWeeklyHRVStatusChart = rootView.findViewById(R.id.hrv_weekly_line_chart);
        mHRVStatusLastNight = rootView.findViewById(R.id.hrv_status_last_night);
        mHRVStatusLastNightLabel = rootView.findViewById(R.id.hrv_status_last_night_label);
        mHRVStatusSevenDaysAvg = rootView.findViewById(R.id.hrv_status_seven_days_avg);
        mHRVStatusSevenDaysAvgStatus = rootView.findViewById(R.id.hrv_status_seven_days_avg_rate);
        mHRVStatusLastNight5MinHighest = rootView.findViewById(R.id.hrv_status_last_night_highest_5);
        mHRVStatusLastNight5MinHighestLabel = rootView.findViewById(R.id.hrv_status_last_night_highest_5_label);
        mHRVStatusDayAvg = rootView.findViewById(R.id.hrv_status_day_avg);
        mHRVStatusBaseline = rootView.findViewById(R.id.hrv_status_baseline);
        mDateView = rootView.findViewById(R.id.hrv_status_date_view);
        mHRVStatusGauge = rootView.findViewById(R.id.hrv_status_gauge_bar);
        mHRVGaugeValue = rootView.findViewById(R.id.hrv_gauge_value);
        mHRVGaugeStatus = rootView.findViewById(R.id.hrv_gauge_status);
        mHRVStatusDataTypeGroup = rootView.findViewById(R.id.hrv_status_chart_data_type_group);

        gaugeDrawer = new GaugeDrawer();
        updateNightlySummaryLabels();
        setupPeriodDataTypeChips(inflater);
        setupLineChart();
        refresh();

        return rootView;
    }

    private void setupPeriodDataTypeChips(final LayoutInflater inflater) {
        if (mHRVStatusDataTypeGroup == null) {
            return;
        }

        if (viewMode == ViewMode.LAST_NIGHT) {
            mHRVStatusDataTypeGroup.setVisibility(View.GONE);
            return;
        }

        mHRVStatusDataTypeGroup.setVisibility(View.VISIBLE);
        mHRVStatusDataTypeGroup.removeAllViews();
        mHRVStatusDataTypeGroup.setSingleSelection(false);
        mHRVStatusDataTypeGroup.setSelectionRequired(true);
        for (final PeriodDataType dataType : PERIOD_DATA_TYPE_ORDER) {
            addPeriodDataTypeChip(inflater, dataType);
        }
        mHRVStatusDataTypeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                Toast.makeText(requireContext(), R.string.charts_at_least_one_item, Toast.LENGTH_SHORT).show();
                final Chip defaultChip = findPeriodDataTypeChip(DEFAULT_PERIOD_DATA_TYPE);
                if (defaultChip != null) {
                    defaultChip.setChecked(true);
                }
                return;
            }

            final boolean selectedDailyAverage = isPeriodDataTypeSelected(PeriodDataType.DAILY_AVERAGE);
            final boolean selectedLastNightAverage = isPeriodDataTypeSelected(PeriodDataType.LAST_NIGHT_AVERAGE);
            final boolean selectedDailyRange = isPeriodDataTypeSelected(PeriodDataType.DAILY_RANGE);
            if (showDailyAverage == selectedDailyAverage
                    && showLastNightAverage == selectedLastNightAverage
                    && showDailyRange == selectedDailyRange) {
                return;
            }

            showDailyAverage = selectedDailyAverage;
            showLastNightAverage = selectedLastNightAverage;
            showDailyRange = selectedDailyRange;
            refresh();
        });
    }

    private void addPeriodDataTypeChip(final LayoutInflater inflater, final PeriodDataType dataType) {
        final Chip chip = (Chip) inflater.inflate(R.layout.layout_chart_chip, mHRVStatusDataTypeGroup, false);
        chip.setId(View.generateViewId());
        chip.setText(getString(getPeriodDataTypeLabel(dataType)));
        chip.setTag(dataType);
        mHRVStatusDataTypeGroup.addView(chip);
        chip.setChecked(isPeriodDataTypeVisible(dataType));
    }

    private boolean isPeriodDataTypeSelected(final PeriodDataType dataType) {
        final Chip chip = findPeriodDataTypeChip(dataType);
        return chip != null && chip.isChecked();
    }

    private boolean isPeriodDataTypeVisible(final PeriodDataType dataType) {
        switch (dataType) {
            case DAILY_AVERAGE:
                return showDailyAverage;
            case LAST_NIGHT_AVERAGE:
                return showLastNightAverage;
            case DAILY_RANGE:
                return showDailyRange;
            default:
                throw new IllegalArgumentException("Unknown period data type: " + dataType);
        }
    }

    private Chip findPeriodDataTypeChip(final PeriodDataType dataType) {
        for (int i = 0; i < mHRVStatusDataTypeGroup.getChildCount(); i++) {
            final View child = mHRVStatusDataTypeGroup.getChildAt(i);
            if (child instanceof Chip && child.getTag() == dataType) {
                return (Chip) child;
            }
        }
        return null;
    }

    private int getPeriodDataTypeLabel(final PeriodDataType dataType) {
        switch (dataType) {
            case DAILY_AVERAGE:
                return R.string.hrv_status_day_avg_legend;
            case LAST_NIGHT_AVERAGE:
                return R.string.hrv_status_nightly_avg_legend;
            case DAILY_RANGE:
                return R.string.hrv_status_daily_range_legend;
            default:
                throw new IllegalArgumentException("Unknown period data type: " + dataType);
        }
    }

    @Override
    public String getTitle() {
        return getString(R.string.pref_header_hrv_status);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        HRV_AVERAGE_COLOR = getResources().getColor(R.color.hrv_status_char_line_color);
        HRV_RANGE_COLOR = getResources().getColor(R.color.hrv_status_range_color);
        HRV_LAST_NIGHT_COLOR = getResources().getColor(R.color.hrv_status_last_night_color);
        HRV_BASELINE_FILL_COLOR = getResources().getColor(R.color.hrv_status_baseline_fill_color);
    }

    @Override
    protected HRVStatusWeeklyData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());

        final List<HRVStatusDayData> periodData = getPeriodData(db, day, device);
        final LastNightData lastNightData = viewMode == ViewMode.LAST_NIGHT ?
                getLastNightData(db, device, day) :
                new LastNightData(new ArrayList<>(), getLastNightSearchStart(day), getLastNightSearchEnd(day));
        return new HRVStatusWeeklyData(periodData, lastNightData.samples, lastNightData.start, lastNightData.end);
    }

    @Override
    protected void renderCharts() {
        mWeeklyHRVStatusChart.invalidate();
    }

    protected LineDataSet createAverageDataSet(final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.hrv_status_day_avg_legend));
        lineDataSet.setColor(HRV_AVERAGE_COLOR);
        lineDataSet.setLineWidth(2.5f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setCircleColor(HRV_AVERAGE_COLOR);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setValueTextSize(10f);
        lineDataSet.setValueTextColor(TEXT_COLOR);
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.ROOT, "%d", (int) value);
            }
        });
        return lineDataSet;
    }

    protected BarDataSet createBaselineRangeDataSet(final List<BarEntry> values) {
        final BarDataSet barDataSet = new BarDataSet(values, getString(R.string.hrv_status_baseline_label));
        barDataSet.setDrawValues(false);
        barDataSet.setDrawIcons(false);
        barDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        barDataSet.setColors(0x00000000, HRV_BASELINE_FILL_COLOR);
        barDataSet.setHighLightAlpha(0);
        barDataSet.setHighlightEnabled(false);
        return barDataSet;
    }

    protected CandleDataSet createRangeDataSet(final List<CandleEntry> values) {
        final CandleDataSet candleDataSet = new CandleDataSet(values, getString(R.string.hrv_status_daily_range_legend));
        candleDataSet.setDrawValues(false);
        candleDataSet.setDrawIcons(false);
        candleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        candleDataSet.setShadowColor(HRV_RANGE_COLOR);
        candleDataSet.setShadowWidth(2f);
        candleDataSet.setDecreasingColor(HRV_RANGE_COLOR);
        candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setIncreasingColor(HRV_RANGE_COLOR);
        candleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setNeutralColor(HRV_RANGE_COLOR);
        candleDataSet.setBarSpace(0.35f);
        candleDataSet.setShowCandleBar(true);
        candleDataSet.setHighlightEnabled(false);
        return candleDataSet;
    }

    protected LineDataSet createLastNightAverageDataSet(final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.hrv_status_nightly_avg_legend));
        lineDataSet.setColor(HRV_LAST_NIGHT_COLOR);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleRadius(4f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setCircleColor(HRV_LAST_NIGHT_COLOR);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setHighlightEnabled(false);
        return lineDataSet;
    }

    protected LineDataSet createRawHrvDataSet(final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.hrv_status_last_night_legend));
        lineDataSet.setColor(HRV_AVERAGE_COLOR);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setHighlightEnabled(false);
        return lineDataSet;
    }

    @Override
    protected void updateChartsnUIThread(HRVStatusWeeklyData weeklyData) {
        mWeeklyHRVStatusChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        if (viewMode == ViewMode.LAST_NIGHT) {
            updateLastNightChart(weeklyData);
        } else {
            updatePeriodChart(weeklyData);
        }

        HRVStatusDayData today = weeklyData.getCurrentDay();
        // Show weekly average even if we don't have full 7 days - it's computed from available data
        mHRVStatusSevenDaysAvg.setText(today.weeklyAvg > 0 ? getString(R.string.hrv_status_unit, today.weeklyAvg) :
                (today.dayAvg > 0 ? getString(R.string.hrv_status_unit, today.dayAvg) : getString(R.string.stats_empty_value)));
        updateNightlySummaryStats(weeklyData, today);
        mHRVStatusDayAvg.setText(formatHrvStatusValue(today.dayAvg));
        mHRVStatusBaseline.setText(today.baseLineBalancedLower > 0 && today.baseLineBalancedUpper > 0 ? getString(R.string.hrv_status_baseline, today.baseLineBalancedLower, today.baseLineBalancedUpper) : "-");
        switch (today.status) {
            case NONE:
                mHRVStatusSevenDaysAvgStatus.setText("-");
                mHRVStatusSevenDaysAvgStatus.setTextColor(TEXT_COLOR);
                mHRVGaugeStatus.setText("");
                mHRVGaugeStatus.setTextColor(TEXT_COLOR);
                break;
            case POOR:
                mHRVStatusSevenDaysAvgStatus.setText(getString(R.string.hrv_status_poor));
                mHRVStatusSevenDaysAvgStatus.setTextColor(getResources().getColor(R.color.hrv_status_poor));
                mHRVGaugeStatus.setText(getString(R.string.hrv_status_poor));
                mHRVGaugeStatus.setTextColor(getResources().getColor(R.color.hrv_status_poor));
                break;
            case LOW:
                mHRVStatusSevenDaysAvgStatus.setText(getString(R.string.hrv_status_low));
                mHRVStatusSevenDaysAvgStatus.setTextColor(getResources().getColor(R.color.hrv_status_low));
                mHRVGaugeStatus.setText(getString(R.string.hrv_status_low));
                mHRVGaugeStatus.setTextColor(getResources().getColor(R.color.hrv_status_low));
                break;
            case UNBALANCED:
                mHRVStatusSevenDaysAvgStatus.setText(getString(R.string.hrv_status_unbalanced));
                mHRVStatusSevenDaysAvgStatus.setTextColor(getResources().getColor(R.color.hrv_status_unbalanced));
                mHRVGaugeStatus.setText(getString(R.string.hrv_status_unbalanced));
                mHRVGaugeStatus.setTextColor(getResources().getColor(R.color.hrv_status_unbalanced));
                break;
            case BALANCED:
                mHRVStatusSevenDaysAvgStatus.setText(getString(R.string.hrv_status_balanced));
                mHRVStatusSevenDaysAvgStatus.setTextColor(getResources().getColor(R.color.hrv_status_balanced));
                mHRVGaugeStatus.setText(getString(R.string.hrv_status_balanced));
                mHRVGaugeStatus.setTextColor(getResources().getColor(R.color.hrv_status_balanced));
                break;
        }
        float value = DashboardHrvWidget.calculateGaugeValue(today.weeklyAvg, today.baseLineLowUpper, today.baseLineBalancedLower, today.baseLineBalancedUpper);
        final String valueText = value > 0 ? getString(R.string.hrv_status_unit, today.weeklyAvg) : getString(R.string.stats_empty_value);
        mHRVGaugeValue.setText(valueText);
        gaugeDrawer.drawSegmentedGauge(mHRVStatusGauge, DashboardHrvWidget.getColors(), DashboardHrvWidget.getSegments(), value, false, true);
    }

    private void updateNightlySummaryStats(final HRVStatusWeeklyData weeklyData, final HRVStatusDayData today) {
        updateNightlySummaryLabels();
        if (viewMode == ViewMode.LAST_NIGHT) {
            mHRVStatusLastNight.setText(formatHrvStatusValue(today.lastNight));
            mHRVStatusLastNight5MinHighest.setText(formatHrvStatusValue(today.lastNight5MinHigh));
            return;
        }

        mHRVStatusLastNight.setText(formatHrvStatusValue(weeklyData.getPeriodNightlyAverage()));
        mHRVStatusLastNight5MinHighest.setText(formatHrvStatusValue(weeklyData.getHighestNightlyAverage()));
    }

    private void updateNightlySummaryLabels() {
        if (viewMode == ViewMode.LAST_NIGHT) {
            mHRVStatusLastNightLabel.setText(R.string.hrv_status_last_night);
            mHRVStatusLastNight5MinHighestLabel.setText(R.string.hrv_status_last_night_highest_5);
            return;
        }

        mHRVStatusLastNightLabel.setText(R.string.hrv_status_nightly_avg);
        mHRVStatusLastNight5MinHighestLabel.setText(R.string.hrv_status_highest_nightly_avg);
    }

    private String formatHrvStatusValue(final int value) {
        return value > 0 ? getString(R.string.hrv_status_unit, value) : getString(R.string.stats_empty_value);
    }

    private void updatePeriodChart(HRVStatusWeeklyData weeklyData) {
        mDateView.setText(DateTimeUtils.formatDaysUntil(totalDays, getTSEnd()));
        final List<CandleEntry> rangeEntries = new ArrayList<>();
        final List<Entry> averageEntries = new ArrayList<>();
        final List<Entry> lastNightEntries = new ArrayList<>();
        final List<BarEntry> baselineEntries = new ArrayList<>();
        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        final List<ILineDataSet> lastNightDataSets = new ArrayList<>();
        final Accumulator axisAccumulator = new Accumulator();
        boolean hasAverageData = false;
        boolean hasLastNightData = false;

        for (final HRVStatusDayData day : weeklyData.getDaysData()) {
            if (showDailyAverage) {
                if (day.dayAvg > 0) {
                    hasAverageData = true;
                    axisAccumulator.add(day.dayAvg);
                    averageEntries.add(new Entry(day.i, day.dayAvg));
                } else {
                    addAverageDataSet(lineDataSets, averageEntries);
                }
            }

            if (showLastNightAverage) {
                if (day.lastNight > 0) {
                    hasLastNightData = true;
                    axisAccumulator.add(day.lastNight);
                    lastNightEntries.add(new Entry(day.i, day.lastNight));
                } else {
                    addLastNightAverageDataSet(lastNightDataSets, lastNightEntries);
                }
            }

            if (showDailyRange && day.dayMin > 0 && day.dayMax > 0) {
                axisAccumulator.add(day.dayMin);
                axisAccumulator.add(day.dayMax);
                rangeEntries.add(new CandleEntry(day.i, day.dayMax, day.dayMin, day.dayMin, day.dayMax));
            }

            if (day.baseLineBalancedLower > 0 && day.baseLineBalancedUpper > day.baseLineBalancedLower) {
                axisAccumulator.add(day.baseLineBalancedLower);
                axisAccumulator.add(day.baseLineBalancedUpper);
                baselineEntries.add(new BarEntry(day.i, new float[]{
                        day.baseLineBalancedLower,
                        day.baseLineBalancedUpper - day.baseLineBalancedLower
                }));
            }
        }

        if (showDailyAverage) {
            addAverageDataSet(lineDataSets, averageEntries);
        }
        if (showLastNightAverage) {
            addLastNightAverageDataSet(lastNightDataSets, lastNightEntries);
        }
        lineDataSets.addAll(lastNightDataSets);

        final boolean hasChartData = !rangeEntries.isEmpty() || !lineDataSets.isEmpty() || !baselineEntries.isEmpty();
        if (hasChartData) {
            final CombinedData combinedData = new CombinedData();
            if (!baselineEntries.isEmpty()) {
                final BarData barData = new BarData(createBaselineRangeDataSet(baselineEntries));
                barData.setBarWidth(1f);
                combinedData.setData(barData);
            }
            if (!rangeEntries.isEmpty()) {
                combinedData.setData(new CandleData(createRangeDataSet(rangeEntries)));
            }
            if (!lineDataSets.isEmpty()) {
                combinedData.setData(new LineData(lineDataSets));
            }
            mWeeklyHRVStatusChart.setData(combinedData);
        } else {
            mWeeklyHRVStatusChart.setData(null);
        }
        if (axisAccumulator.getCount() > 0) {
            mWeeklyHRVStatusChart.getAxisLeft().setAxisMaximum(Math.round(axisAccumulator.getMax()) + 15);
            mWeeklyHRVStatusChart.getAxisLeft().setAxisMinimum(Math.max(0, Math.round(axisAccumulator.getMin()) - 15));
        } else {
            mWeeklyHRVStatusChart.getAxisLeft().setAxisMaximum(120);
            mWeeklyHRVStatusChart.getAxisLeft().setAxisMinimum(0);
        }

        final XAxis x = mWeeklyHRVStatusChart.getXAxis();
        x.setValueFormatter(getHRVStatusChartDayValueFormatter(weeklyData));
        x.setAxisMinimum(0f);
        x.setAxisMaximum(totalDays - 1f);
        x.setLabelCount(Math.min(totalDays, 7), false);
        setupPeriodLegend(!rangeEntries.isEmpty(), hasAverageData, hasLastNightData, !baselineEntries.isEmpty());
    }

    private void updateLastNightChart(HRVStatusWeeklyData weeklyData) {
        final String formattedDate = new SimpleDateFormat("E, MMM dd", Locale.getDefault()).format(getEndDate());
        mDateView.setText(formattedDate);

        final List<Entry> rawEntries = new ArrayList<>();
        final Accumulator axisAccumulator = new Accumulator();
        final long startMillis = weeklyData.lastNightStart.getTimeInMillis();
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (HrvValueSample sample : weeklyData.lastNightSamples) {
            if (sample.getValue() <= 0) {
                continue;
            }

            final float x = (sample.getTimestamp() - startMillis) / 1000f;
            rawEntries.add(new Entry(x, sample.getValue()));
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            axisAccumulator.add(sample.getValue());
        }
        rawEntries.sort(Comparator.comparingDouble(Entry::getX));

        if (!rawEntries.isEmpty()) {
            final CombinedData combinedData = new CombinedData();
            combinedData.setData(new LineData(createRawHrvDataSet(rawEntries)));
            mWeeklyHRVStatusChart.setData(combinedData);
        } else {
            mWeeklyHRVStatusChart.setData(null);
        }

        if (axisAccumulator.getCount() > 0) {
            mWeeklyHRVStatusChart.getAxisLeft().setAxisMaximum(Math.round(axisAccumulator.getMax()) + 15);
            mWeeklyHRVStatusChart.getAxisLeft().setAxisMinimum(Math.max(0, Math.round(axisAccumulator.getMin()) - 15));
        } else {
            mWeeklyHRVStatusChart.getAxisLeft().setAxisMaximum(120);
            mWeeklyHRVStatusChart.getAxisLeft().setAxisMinimum(0);
        }

        final XAxis x = mWeeklyHRVStatusChart.getXAxis();
        x.setValueFormatter(getLastNightChartValueFormatter(weeklyData.lastNightStart));
        if (rawEntries.isEmpty()) {
            x.setAxisMinimum(0f);
            x.setAxisMaximum((weeklyData.lastNightEnd.getTimeInMillis() - weeklyData.lastNightStart.getTimeInMillis()) / 1000f);
        } else if (minX == maxX) {
            x.setAxisMinimum(minX - 1f);
            x.setAxisMaximum(maxX + 1f);
        } else {
            x.setAxisMinimum(minX);
            x.setAxisMaximum(maxX);
        }
        x.setLabelCount(7, true);
        setupLastNightLegend(!rawEntries.isEmpty());
    }

    private void addAverageDataSet(final List<ILineDataSet> lineDataSets, final List<Entry> averageEntries) {
        if (averageEntries.isEmpty()) {
            return;
        }

        lineDataSets.add(createAverageDataSet(new ArrayList<>(averageEntries)));
        averageEntries.clear();
    }

    private void addLastNightAverageDataSet(final List<ILineDataSet> lineDataSets, final List<Entry> lastNightEntries) {
        if (lastNightEntries.isEmpty()) {
            return;
        }

        lineDataSets.add(createLastNightAverageDataSet(new ArrayList<>(lastNightEntries)));
        lastNightEntries.clear();
    }

    private void setupPeriodLegend(final boolean hasRange,
                                   final boolean hasDailyAverage,
                                   final boolean hasLastNightAverage,
                                   final boolean hasBaseline) {
        final List<LegendEntry> legendEntries = new ArrayList<>(4);

        for (final PeriodDataType dataType : PERIOD_DATA_TYPE_ORDER) {
            if (hasPeriodDataTypeData(dataType, hasRange, hasDailyAverage, hasLastNightAverage)) {
                legendEntries.add(createPeriodDataTypeLegendEntry(dataType));
            }
        }

        if (hasBaseline) {
            legendEntries.add(createSquareLegendEntry(
                    getString(R.string.hrv_status_baseline_label),
                    HRV_BASELINE_FILL_COLOR));
        }

        setupHRVStatusLegend(legendEntries);
    }

    private void setupLastNightLegend(final boolean hasRawHrv) {
        final List<LegendEntry> legendEntries = new ArrayList<>(1);
        if (hasRawHrv) {
            legendEntries.add(createLineLegendEntry(getString(R.string.hrv_status_last_night_legend), HRV_AVERAGE_COLOR));
        }

        setupHRVStatusLegend(legendEntries);
    }

    private boolean hasPeriodDataTypeData(final PeriodDataType dataType,
                                          final boolean hasRange,
                                          final boolean hasDailyAverage,
                                          final boolean hasLastNightAverage) {
        switch (dataType) {
            case DAILY_AVERAGE:
                return hasDailyAverage;
            case LAST_NIGHT_AVERAGE:
                return hasLastNightAverage;
            case DAILY_RANGE:
                return hasRange;
            default:
                throw new IllegalArgumentException("Unknown period data type: " + dataType);
        }
    }

    private LegendEntry createPeriodDataTypeLegendEntry(final PeriodDataType dataType) {
        switch (dataType) {
            case DAILY_AVERAGE:
                return createLineLegendEntry(getString(getPeriodDataTypeLabel(dataType)), HRV_AVERAGE_COLOR);
            case LAST_NIGHT_AVERAGE:
                return createLineLegendEntry(getString(getPeriodDataTypeLabel(dataType)), HRV_LAST_NIGHT_COLOR);
            case DAILY_RANGE:
                return createSquareLegendEntry(getString(getPeriodDataTypeLabel(dataType)), HRV_RANGE_COLOR);
            default:
                throw new IllegalArgumentException("Unknown period data type: " + dataType);
        }
    }

    private LegendEntry createLineLegendEntry(final String label, final int color) {
        final LegendEntry legendEntry = new LegendEntry();
        legendEntry.label = label;
        legendEntry.form = Legend.LegendForm.LINE;
        legendEntry.formSize = LEGEND_FORM_SIZE;
        legendEntry.formLineWidth = 2f;
        legendEntry.formColor = color;
        return legendEntry;
    }

    private LegendEntry createSquareLegendEntry(final String label, final int color) {
        final LegendEntry legendEntry = new LegendEntry();
        legendEntry.label = label;
        legendEntry.form = Legend.LegendForm.SQUARE;
        legendEntry.formSize = LEGEND_FORM_SIZE;
        legendEntry.formColor = color;
        return legendEntry;
    }

    private void setupHRVStatusLegend(final List<LegendEntry> legendEntries) {
        final Legend legend = mWeeklyHRVStatusChart.getLegend();
        legend.resetCustom();
        if (legendEntries.isEmpty()) {
            legend.setEnabled(false);
            mWeeklyHRVStatusChart.notifyDataSetChanged();
            mWeeklyHRVStatusChart.requestLayout();
            return;
        }

        legend.setEnabled(true);
        legend.setTextColor(LEGEND_TEXT_COLOR);
        legend.setWordWrapEnabled(true);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(LEGEND_X_ENTRY_SPACE);
        legend.setYEntrySpace(LEGEND_Y_ENTRY_SPACE);
        legend.setFormToTextSpace(LEGEND_FORM_TO_TEXT_SPACE);
        legend.setCustom(legendEntries);
        mWeeklyHRVStatusChart.notifyDataSetChanged();
        mWeeklyHRVStatusChart.requestLayout();
    }

    private List<HRVStatusDayData> getPeriodData(DBHandler db, Calendar day, GBDevice device) {
        day = DateTimeUtils.dayStart(day);
        day.add(Calendar.DATE, -totalDays + 1);

        List<HRVStatusDayData> weeklyData = new ArrayList<>();
        for (int counter = 0; counter < totalDays; counter++) {
            int startTs = (int) (day.getTimeInMillis() / 1000);
            int endTs = startTs + 24 * 60 * 60 - 1;
            Optional<? extends HrvSummarySample> latestSummarySample = getSamples(db, device, startTs, endTs)
                    .stream()
                    .max(Comparator.comparingLong(HrvSummarySample::getTimestamp));
            List<? extends HrvValueSample> valueSamples = getHrvValueSamples(db, device, startTs, endTs);

            final Accumulator dayAccumulator = new Accumulator();
            for (HrvValueSample valueSample : valueSamples) {
                if (valueSample.getValue() > 0) {
                    dayAccumulator.add(valueSample.getValue());
                }
            }

            int avgHRV = dayAccumulator.getCount() > 0 ? (int) Math.round(dayAccumulator.getAverage()) : 0;
            int minHRV = dayAccumulator.getCount() > 0 ? (int) Math.round(dayAccumulator.getMin()) : 0;
            int maxHRV = dayAccumulator.getCount() > 0 ? (int) Math.round(dayAccumulator.getMax()) : 0;
            if (latestSummarySample.isPresent()) {
                final HrvSummarySample sample = latestSummarySample.get();
                Calendar finalDay = (Calendar) day.clone();
                weeklyData.add(new HRVStatusDayData(
                        finalDay,
                        counter,
                        sample.getTimestamp(),
                        avgHRV,
                        minHRV,
                        maxHRV,
                        sample.getWeeklyAverage() != null ? sample.getWeeklyAverage() : 0,
                        sample.getLastNightAverage() != null ? sample.getLastNightAverage() : 0,
                        sample.getLastNight5MinHigh() != null ? sample.getLastNight5MinHigh() : 0,
                        sample.getBaselineLowUpper() != null ? sample.getBaselineLowUpper() : 0,
                        sample.getBaselineBalancedLower() != null ? sample.getBaselineBalancedLower() : 0,
                        sample.getBaselineBalancedUpper() != null ? sample.getBaselineBalancedUpper() : 0,
                        sample.getStatus() != null ? sample.getStatus() : HrvSummarySample.Status.NONE
                ));
            } else {
                HRVStatusDayData d = new HRVStatusDayData(
                        (Calendar) day.clone(),
                        counter,
                        0,
                        avgHRV,
                        minHRV,
                        maxHRV,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        HrvSummarySample.Status.NONE
                );
                weeklyData.add(d);
            }

            day.add(Calendar.DATE, 1);
        }
        return weeklyData;
    }

    private List<? extends HrvSummarySample> getSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends HrvSummarySample> sampleProvider = coordinator.getHrvSummarySampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement HrvSummarySampleProvider", device);
            return new ArrayList<>();
        }
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    public List<? extends HrvValueSample> getHrvValueSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends HrvValueSample> sampleProvider = coordinator.getHrvValueSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement HrvValueSampleProvider", device);
            return new ArrayList<>();
        }
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    private LastNightData getLastNightData(final DBHandler db, final GBDevice device, final Calendar day) {
        final Calendar searchStart = getLastNightSearchStart(day);
        final Calendar searchEnd = getLastNightSearchEnd(day);
        final int startTs = (int) (searchStart.getTimeInMillis() / 1000);
        final int endTs = (int) (searchEnd.getTimeInMillis() / 1000);

        final SampleProvider<? extends AbstractActivitySample> sampleProvider =
                device.getDeviceCoordinator().getSampleProvider(device, db.getDaoSession());
        final List<? extends AbstractActivitySample> activitySamples =
                sampleProvider.getAllActivitySamples(startTs, endTs);

        if (activitySamples.isEmpty()) {
            return new LastNightData(new ArrayList<>(), searchStart, searchEnd);
        }

        final SleepAnalysis sleepAnalysis = new SleepAnalysis();
        final List<SleepAnalysis.SleepSession> sleepSessions =
                sleepAnalysis.calculateSleepSessions(activitySamples);
        if (sleepSessions.isEmpty()) {
            return new LastNightData(new ArrayList<>(), searchStart, searchEnd);
        }

        final SleepAnalysis.SleepSession lastSession = sleepSessions.get(sleepSessions.size() - 1);
        final Calendar sleepStart = Calendar.getInstance();
        sleepStart.setTime(lastSession.getSleepStart());
        final Calendar sleepEnd = Calendar.getInstance();
        sleepEnd.setTime(lastSession.getSleepEnd());

        final List<? extends HrvValueSample> samples = getHrvValueSamples(
                db,
                device,
                (int) (sleepStart.getTimeInMillis() / 1000),
                (int) (sleepEnd.getTimeInMillis() / 1000));
        return new LastNightData(samples, sleepStart, sleepEnd);
    }

    private Calendar getLastNightSearchStart(final Calendar day) {
        final Calendar lastNightStart = DateTimeUtils.dayStart((Calendar) day.clone());
        lastNightStart.add(Calendar.DATE, -1);
        lastNightStart.add(Calendar.HOUR_OF_DAY, 12);
        return lastNightStart;
    }

    private Calendar getLastNightSearchEnd(final Calendar day) {
        final Calendar lastNightEnd = DateTimeUtils.dayStart((Calendar) day.clone());
        lastNightEnd.add(Calendar.HOUR_OF_DAY, 12);
        return lastNightEnd;
    }

    private void setupLineChart() {
        mWeeklyHRVStatusChart.getDescription().setEnabled(false);
        mWeeklyHRVStatusChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR,
                CombinedChart.DrawOrder.CANDLE,
                CombinedChart.DrawOrder.LINE
        });
        mWeeklyHRVStatusChart.setTouchEnabled(false);
        mWeeklyHRVStatusChart.setPinchZoom(false);
        mWeeklyHRVStatusChart.setDoubleTapToZoomEnabled(false);


        final XAxis xAxisBottom = mWeeklyHRVStatusChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(totalDays - 1f);
        xAxisBottom.setGranularity(1f);
        xAxisBottom.setGranularityEnabled(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisLeft = mWeeklyHRVStatusChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(120);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(false);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = mWeeklyHRVStatusChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    ValueFormatter getHRVStatusChartDayValueFormatter(HRVStatusWeeklyData weeklyData) {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatHRVStatusChartValue(value, weeklyData);
            }
        };
    }

    protected String formatHRVStatusChartValue(float value, HRVStatusWeeklyData weeklyData) {
        final int dayIndex = Math.round(value);
        if (dayIndex < 0 || dayIndex >= weeklyData.getDaysData().size()) {
            return "";
        }

        HRVStatusDayData day = weeklyData.getDay(dayIndex);

        final String formatPattern = totalDays == DEFAULT_TOTAL_DAYS ? "EEE" : "dd";
        SimpleDateFormat formatLetterDay = new SimpleDateFormat(formatPattern, Locale.getDefault());
        return formatLetterDay.format(new Date(day.day.getTimeInMillis()));
    }

    ValueFormatter getLastNightChartValueFormatter(final Calendar lastNightStart) {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatLastNightChartValue(value, lastNightStart);
            }
        };
    }

    protected String formatLastNightChartValue(final float value, final Calendar lastNightStart) {
        final long timestamp = lastNightStart.getTimeInMillis() + (long) (value * 1000);
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    @Override
    protected void setupLegend(Chart<?> chart) {}

    protected static class HRVStatusWeeklyData extends ChartsData {
        private final List<HRVStatusDayData> data;
        private final List<? extends HrvValueSample> lastNightSamples;
        private final Calendar lastNightStart;
        private final Calendar lastNightEnd;

        public HRVStatusWeeklyData(final List<HRVStatusDayData> chartsData,
                                   final List<? extends HrvValueSample> lastNightSamples,
                                   final Calendar lastNightStart,
                                   final Calendar lastNightEnd) {
            this.data = chartsData;
            this.lastNightSamples = lastNightSamples;
            this.lastNightStart = lastNightStart;
            this.lastNightEnd = lastNightEnd;
        }

        public HRVStatusDayData getDay(int i) {
            return this.data.get(i);
        }

        public HRVStatusDayData getCurrentDay() {
            return this.data.get(this.data.size() - 1);
        }

        public List<HRVStatusDayData> getDaysData() {
            return data;
        }

        public int getPeriodNightlyAverage() {
            final Accumulator accumulator = new Accumulator();
            for (final HRVStatusDayData day : data) {
                if (day.lastNight > 0) {
                    accumulator.add(day.lastNight);
                }
            }
            return accumulator.getCount() > 0 ? (int) Math.round(accumulator.getAverage()) : 0;
        }

        public int getHighestNightlyAverage() {
            int highestNightlyAverage = 0;
            for (final HRVStatusDayData day : data) {
                if (day.lastNight > highestNightlyAverage) {
                    highestNightlyAverage = day.lastNight;
                }
            }
            return highestNightlyAverage;
        }
    }

    protected static class LastNightData {
        private final List<? extends HrvValueSample> samples;
        private final Calendar start;
        private final Calendar end;

        public LastNightData(final List<? extends HrvValueSample> samples,
                             final Calendar start,
                             final Calendar end) {
            this.samples = samples;
            this.start = start;
            this.end = end;
        }
    }

    protected static class HRVStatusDayData {
        public Integer i;
        public long timestamp;
        public Integer weeklyAvg;
        public Integer lastNight;
        public Integer lastNight5MinHigh;
        public Integer dayAvg;
        public Integer dayMin;
        public Integer dayMax;
        public Integer baseLineBalancedLower;
        public Integer baseLineBalancedUpper;
        public Integer baseLineLowUpper;
        public HrvSummarySample.Status status;
        public Calendar day;

        public HRVStatusDayData(Calendar day,
                                int i, long timestamp,
                                Integer dayAvg,
                                Integer dayMin,
                                Integer dayMax,
                                Integer weeklyAvg,
                                Integer lastNight,
                                Integer lastNight5MinHigh,
                                Integer baseLineLowUpper,
                                Integer baseLineBalancedLower,
                                Integer baseLineBalancedUpper,
                                HrvSummarySample.Status status) {
            this.lastNight = lastNight;
            this.weeklyAvg = weeklyAvg;
            this.lastNight5MinHigh = lastNight5MinHigh;
            this.i = i;
            this.timestamp = timestamp;
            this.status = status;
            this.day = day;
            this.dayAvg = dayAvg;
            this.dayMin = dayMin;
            this.dayMax = dayMax;
            this.baseLineLowUpper = baseLineLowUpper;
            this.baseLineBalancedLower = baseLineBalancedLower;
            this.baseLineBalancedUpper = baseLineBalancedUpper;
        }
    }
}
