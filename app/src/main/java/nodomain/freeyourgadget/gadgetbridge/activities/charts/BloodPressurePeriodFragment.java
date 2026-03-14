/*
    Copyright (C) 2026 Christian Breiteneder

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
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
import nodomain.freeyourgadget.gadgetbridge.model.BloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.BloodPressureExportHelper;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class BloodPressurePeriodFragment extends AbstractChartFragment<BloodPressurePeriodFragment.BloodPressurePeriodData> {
    protected static final Logger LOG = LoggerFactory.getLogger(BloodPressurePeriodFragment.class);

    static int SEC_PER_DAY = 24 * 60 * 60;
    static int DATA_INVALID = -1;

    private int BACKGROUND_COLOR;
    private int CHART_TEXT_COLOR;
    private int LEGEND_TEXT_COLOR;
    private int SYSTOLIC_COLOR;
    private int DIASTOLIC_COLOR;

    private TextView mDateView;
    private TextView mSystolicLast;
    private TextView mDiastolicLast;
    private TextView mAverage;
    private TextView mMeasurementCount;
    private CombinedChart mChart;
    private int TOTAL_DAYS;
    private List<? extends BloodPressureSample> allSamples;

    @Override
    protected boolean isSingleDay() {
        return false;
    }

    public static BloodPressurePeriodFragment newInstance(int totalDays) {
        BloodPressurePeriodFragment fragment = new BloodPressurePeriodFragment();
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
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        SYSTOLIC_COLOR = ContextCompat.getColor(requireContext(), R.color.blood_pressure_systolic_color);
        DIASTOLIC_COLOR = ContextCompat.getColor(requireContext(), R.color.blood_pressure_diastolic_color);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_blood_pressure_period, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                getChartsHost().enableSwipeRefresh(scrollY == 0)
        );

        mDateView = rootView.findViewById(R.id.date_view);
        mSystolicLast = rootView.findViewById(R.id.bp_systolic_last);
        mDiastolicLast = rootView.findViewById(R.id.bp_diastolic_last);
        mAverage = rootView.findViewById(R.id.bp_average);
        mMeasurementCount = rootView.findViewById(R.id.bp_measurement_count);
        mChart = rootView.findViewById(R.id.blood_pressure_chart);

        setupChart();

        FloatingActionButton exportFab = rootView.findViewById(R.id.bp_export_fab);
        exportFab.setOnClickListener(v -> showExportDialog());

        refresh();
        setupLegend(mChart);

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.blood_pressure);
    }

    private int getStartTs() {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        return (int) (day.getTimeInMillis() / 1000) - SEC_PER_DAY * (TOTAL_DAYS - 1);
    }

    private BloodPressureDayData fetchDataForDay(DBHandler db, GBDevice device, int startTs) {
        int endTs = startTs + SEC_PER_DAY - 1;
        List<? extends BloodPressureSample> samples = getSamples(db, device, startTs, endTs);

        final Accumulator systolicAcc = new Accumulator();
        final Accumulator diastolicAcc = new Accumulator();

        for (final BloodPressureSample sample : samples) {
            if (sample.getBpSystolic() > 0) {
                systolicAcc.add(sample.getBpSystolic());
            }
            if (sample.getBpDiastolic() > 0) {
                diastolicAcc.add(sample.getBpDiastolic());
            }
        }

        final int systolicAvg = systolicAcc.getCount() > 0 ? (int) Math.round(systolicAcc.getAverage()) : DATA_INVALID;
        final int systolicMin = systolicAcc.getCount() > 0 ? (int) Math.round(systolicAcc.getMin()) : DATA_INVALID;
        final int systolicMax = systolicAcc.getCount() > 0 ? (int) Math.round(systolicAcc.getMax()) : DATA_INVALID;
        final int diastolicAvg = diastolicAcc.getCount() > 0 ? (int) Math.round(diastolicAcc.getAverage()) : DATA_INVALID;
        final int diastolicMin = diastolicAcc.getCount() > 0 ? (int) Math.round(diastolicAcc.getMin()) : DATA_INVALID;
        final int diastolicMax = diastolicAcc.getCount() > 0 ? (int) Math.round(diastolicAcc.getMax()) : DATA_INVALID;

        return new BloodPressureDayData(systolicAvg, systolicMin, systolicMax, diastolicAvg, diastolicMin, diastolicMax, samples.size());
    }

    @Override
    protected BloodPressurePeriodData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        final int startTs = getStartTs();
        final int endTs = startTs + SEC_PER_DAY * TOTAL_DAYS - 1;

        List<BloodPressureDayData> result = new ArrayList<>();
        for (int i = 0; i < TOTAL_DAYS; i++) {
            BloodPressureDayData dayData = fetchDataForDay(db, device, startTs + i * SEC_PER_DAY);
            result.add(dayData);
        }

        allSamples = getSamples(db, device, startTs, endTs);

        return new BloodPressurePeriodData(result);
    }

    private List<? extends BloodPressureSample> getSamples(DBHandler db, GBDevice device, int startTs, int endTs) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends BloodPressureSample> sampleProvider = coordinator.getBloodPressureSampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(startTs * 1000L, endTs * 1000L);
    }

    @Override
    protected void updateChartsnUIThread(BloodPressurePeriodData data) {
        final int startTs = getStartTs();
        mDateView.setText(DateTimeUtils.formatDaysUntil(TOTAL_DAYS, getTSEnd()));

        final Accumulator systolicAvgAcc = new Accumulator();
        final Accumulator diastolicAvgAcc = new Accumulator();

        final ArrayList<CandleEntry> systolicCandleEntries = new ArrayList<>();
        final ArrayList<CandleEntry> diastolicCandleEntries = new ArrayList<>();

        for (int i = 0; i < data.days.size(); i++) {
            final BloodPressureDayData dayData = data.days.get(i);
            if (dayData.systolicMin > 0 && dayData.systolicMax > 0) {
                systolicAvgAcc.add(dayData.systolicAvg);
                systolicCandleEntries.add(new CandleEntry(i, dayData.systolicMax, dayData.systolicMin, dayData.systolicMin, dayData.systolicMax));
            }
            if (dayData.diastolicMin > 0 && dayData.diastolicMax > 0) {
                diastolicAvgAcc.add(dayData.diastolicAvg);
                diastolicCandleEntries.add(new CandleEntry(i, dayData.diastolicMax, dayData.diastolicMin, dayData.diastolicMin, dayData.diastolicMax));
            }
        }

        final String emptyValue = requireContext().getString(R.string.stats_empty_value);
        final int systolicAvg = systolicAvgAcc.getCount() > 0 ? (int) Math.round(systolicAvgAcc.getAverage()) : DATA_INVALID;
        final int diastolicAvg = diastolicAvgAcc.getCount() > 0 ? (int) Math.round(diastolicAvgAcc.getAverage()) : DATA_INVALID;

        // Last day with valid data
        int systolicLast = DATA_INVALID;
        int diastolicLast = DATA_INVALID;
        int totalMeasurements = 0;
        for (int i = data.days.size() - 1; i >= 0; i--) {
            final BloodPressureDayData dayData = data.days.get(i);
            if (dayData.systolicAvg > 0 && systolicLast == DATA_INVALID) {
                systolicLast = dayData.systolicAvg;
            }
            if (dayData.diastolicAvg > 0 && diastolicLast == DATA_INVALID) {
                diastolicLast = dayData.diastolicAvg;
            }
            totalMeasurements += dayData.measurementCount;
        }

        mSystolicLast.setText(systolicLast > 0 ? String.valueOf(systolicLast) : emptyValue);
        mDiastolicLast.setText(diastolicLast > 0 ? String.valueOf(diastolicLast) : emptyValue);
        if (systolicAvg > 0 && diastolicAvg > 0) {
            mAverage.setText(getString(R.string.blood_pressure_avg_format, systolicAvg, diastolicAvg));
        } else {
            mAverage.setText(emptyValue);
        }
        mMeasurementCount.setText(String.valueOf(totalMeasurements));

        mChart.getXAxis().setValueFormatter(createDayFormatter(startTs));

        final CombinedData combinedData = new CombinedData();

        // Systolic candle data (range bars)
        if (!systolicCandleEntries.isEmpty()) {
            CandleDataSet systolicCandleDataSet = new CandleDataSet(systolicCandleEntries, getString(R.string.blood_pressure_systolic));
            systolicCandleDataSet.setDrawValues(false);
            systolicCandleDataSet.setDrawIcons(false);
            systolicCandleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            systolicCandleDataSet.setShadowColor(SYSTOLIC_COLOR);
            systolicCandleDataSet.setShadowWidth(2f);
            systolicCandleDataSet.setDecreasingColor(SYSTOLIC_COLOR);
            systolicCandleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
            systolicCandleDataSet.setIncreasingColor(SYSTOLIC_COLOR);
            systolicCandleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
            systolicCandleDataSet.setNeutralColor(SYSTOLIC_COLOR);
            systolicCandleDataSet.setBarSpace(0.15f);
            systolicCandleDataSet.setShowCandleBar(true);

            // Diastolic candle data as second set
            if (!diastolicCandleEntries.isEmpty()) {
                CandleDataSet diastolicCandleDataSet = new CandleDataSet(diastolicCandleEntries, getString(R.string.blood_pressure_diastolic));
                diastolicCandleDataSet.setDrawValues(false);
                diastolicCandleDataSet.setDrawIcons(false);
                diastolicCandleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                diastolicCandleDataSet.setShadowColor(DIASTOLIC_COLOR);
                diastolicCandleDataSet.setShadowWidth(2f);
                diastolicCandleDataSet.setDecreasingColor(DIASTOLIC_COLOR);
                diastolicCandleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
                diastolicCandleDataSet.setIncreasingColor(DIASTOLIC_COLOR);
                diastolicCandleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
                diastolicCandleDataSet.setNeutralColor(DIASTOLIC_COLOR);
                diastolicCandleDataSet.setBarSpace(0.15f);
                diastolicCandleDataSet.setShowCandleBar(true);
                combinedData.setData(new CandleData(systolicCandleDataSet, diastolicCandleDataSet));
            } else {
                combinedData.setData(new CandleData(systolicCandleDataSet));
            }
        }

        mChart.setData(combinedData);
    }

    private void showExportDialog() {
        if (allSamples == null || allSamples.isEmpty()) {
            return;
        }
        String dateLabel = mDateView.getText().toString();
        String[] options = {"PDF", "CSV"};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.appmanager_app_share)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        BloodPressureExportHelper.exportPdf(requireContext(), allSamples, dateLabel, getWhiteChartBitmap());
                    } else {
                        BloodPressureExportHelper.exportCsv(requireContext(), allSamples);
                    }
                })
                .show();
    }

    private ValueFormatter createDayFormatter(final int startTs) {
        final String fmt = TOTAL_DAYS == 7 ? "EEE" : "dd";
        final SimpleDateFormat formatDay = new SimpleDateFormat(fmt, Locale.getDefault());
        return new ValueFormatter() {
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
    }

    private Bitmap getWhiteChartBitmap() {
        // Temporarily switch to light colors for PDF export
        mChart.setBackgroundColor(0xFFFFFFFF);
        mChart.getXAxis().setTextColor(0xFF000000);
        mChart.getAxisLeft().setTextColor(0xFF000000);
        mChart.getAxisRight().setAxisLineColor(0xFF000000);
        mChart.getLegend().setTextColor(0xFF000000);
        mChart.invalidate();

        Bitmap bitmap = mChart.getChartBitmap();

        // Restore original colors
        mChart.setBackgroundColor(BACKGROUND_COLOR);
        mChart.getXAxis().setTextColor(CHART_TEXT_COLOR);
        mChart.getAxisLeft().setTextColor(CHART_TEXT_COLOR);
        mChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        mChart.invalidate();

        return bitmap;
    }

    private void setupChart() {
        mChart.setBackgroundColor(BACKGROUND_COLOR);
        mChart.getDescription().setEnabled(false);
        mChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.CANDLE
        });

        if (TOTAL_DAYS <= 7) {
            mChart.setTouchEnabled(false);
            mChart.setPinchZoom(false);
        }
        mChart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = mChart.getXAxis();
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

        final YAxis yAxisLeft = mChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(200f);
        yAxisLeft.setAxisMinimum(40f);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setGranularity(10f);
        yAxisLeft.setGranularityEnabled(true);

        final YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(2);

        LegendEntry systolicEntry = new LegendEntry();
        systolicEntry.label = getString(R.string.blood_pressure_systolic);
        systolicEntry.formColor = SYSTOLIC_COLOR;
        legendEntries.add(systolicEntry);

        LegendEntry diastolicEntry = new LegendEntry();
        diastolicEntry.label = getString(R.string.blood_pressure_diastolic);
        diastolicEntry.formColor = DIASTOLIC_COLOR;
        legendEntries.add(diastolicEntry);

        mChart.getLegend().setCustom(legendEntries);
        mChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        mChart.getLegend().setWordWrapEnabled(true);
    }

    @Override
    protected void renderCharts() {
        mChart.invalidate();
    }

    protected static class BloodPressurePeriodData extends ChartsData {
        public List<BloodPressureDayData> days;

        protected BloodPressurePeriodData(List<BloodPressureDayData> days) {
            this.days = days;
        }
    }

    protected static class BloodPressureDayData extends ChartsData {
        public int systolicAvg;
        public int systolicMin;
        public int systolicMax;
        public int diastolicAvg;
        public int diastolicMin;
        public int diastolicMax;
        public int measurementCount;

        protected BloodPressureDayData(int systolicAvg, int systolicMin, int systolicMax,
                                       int diastolicAvg, int diastolicMin, int diastolicMax,
                                       int measurementCount) {
            this.systolicAvg = systolicAvg;
            this.systolicMin = systolicMin;
            this.systolicMax = systolicMax;
            this.diastolicAvg = diastolicAvg;
            this.diastolicMin = diastolicMin;
            this.diastolicMax = diastolicMax;
            this.measurementCount = measurementCount;
        }
    }
}