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
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

public class BloodPressureChartFragment extends AbstractChartFragment<BloodPressureChartFragment.BloodPressureChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(BloodPressureChartFragment.class);

    static int DATA_INVALID = -1;

    private int BACKGROUND_COLOR;
    private int CHART_TEXT_COLOR;
    private int TEXT_COLOR;
    private int LEGEND_TEXT_COLOR;

    private TimestampTranslation tsTranslation;

    private TextView mDateView;
    private LineChart mChart;
    private TextView mSystolicLast;
    private TextView mDiastolicLast;
    private TextView mAverage;
    private TextView mMeasurementCount;
    private LinearLayout mManualMeasurements;
    private LinearLayout mManualMeasurementsList;
    private BloodPressureChartsData currentData;

    @Override
    protected void init() {
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(requireContext());
        LEGEND_TEXT_COLOR = TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_blood_pressure_chart, container, false);

        mDateView = rootView.findViewById(R.id.date_view);
        mChart = rootView.findViewById(R.id.blood_pressure_line_chart);
        mSystolicLast = rootView.findViewById(R.id.bp_systolic_last);
        mDiastolicLast = rootView.findViewById(R.id.bp_diastolic_last);
        mAverage = rootView.findViewById(R.id.bp_average);
        mMeasurementCount = rootView.findViewById(R.id.bp_measurement_count);
        mManualMeasurements = rootView.findViewById(R.id.manualMeasurements);
        mManualMeasurementsList = rootView.findViewById(R.id.manualMeasurementsList);

        mManualMeasurements.setVisibility(View.GONE);
        setupLineChart();

        FloatingActionButton exportFab = rootView.findViewById(R.id.bp_export_fab);
        exportFab.setOnClickListener(v -> showExportDialog());

        refresh();
        return rootView;
    }

    @Override
    protected BloodPressureChartsData refreshInBackground(final ChartsHost chartsHost, final DBHandler db, final GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        int startTs = (int) (day.getTimeInMillis() / 1000);
        int endTs = startTs + 24 * 60 * 60 - 1;
        tsTranslation = new TimestampTranslation();
        tsTranslation.shorten(startTs);
        String formattedDate = new SimpleDateFormat("E, MMM dd", Locale.getDefault()).format(chartsHost.getEndDate());
        mDateView.setText(formattedDate);
        return fetchBloodPressureData(db, device, startTs, endTs);
    }

    protected LineDataSet createDataSet(final List<Entry> values, String label, int color) {
        final LineDataSet lineDataSet = new LineDataSet(values, label);
        lineDataSet.setColor(color);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setCircleColor(color);
        lineDataSet.setCircleRadius(3f);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setLineWidth(2.2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setValueTextColor(TEXT_COLOR);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.ROOT, "%d", (int) value);
            }
        });
        return lineDataSet;
    }

    @Override
    protected void updateChartsnUIThread(BloodPressureChartsData data) {
        currentData = data;
        mManualMeasurementsList.removeAllViews();
        mManualMeasurements.setVisibility(View.GONE);

        final String emptyValue = requireContext().getString(R.string.stats_empty_value);
        mSystolicLast.setText(data.systolicLast > 0 ? String.valueOf(data.systolicLast) : emptyValue);
        mDiastolicLast.setText(data.diastolicLast > 0 ? String.valueOf(data.diastolicLast) : emptyValue);
        if (data.systolicAvg > 0 && data.diastolicAvg > 0) {
            mAverage.setText(getString(R.string.blood_pressure_avg_format, data.systolicAvg, data.diastolicAvg));
        } else {
            mAverage.setText(emptyValue);
        }
        mMeasurementCount.setText(String.valueOf(data.measurementCount));

        mChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mChart.getAxisLeft().removeAllLimitLines();

        Date date = new Date((long) getTSEnd() * 1000);
        String formattedDate = new SimpleDateFormat("E, MMM dd", Locale.getDefault()).format(date);
        mDateView.setText(formattedDate);

        final int systolicColor = ContextCompat.getColor(requireContext(), R.color.blood_pressure_systolic_color);
        final int diastolicColor = ContextCompat.getColor(requireContext(), R.color.blood_pressure_diastolic_color);

        final List<LegendEntry> legendEntries = new ArrayList<>(2);
        final LegendEntry systolicEntry = new LegendEntry();
        systolicEntry.label = getString(R.string.blood_pressure_systolic);
        systolicEntry.formColor = systolicColor;
        legendEntries.add(systolicEntry);
        final LegendEntry diastolicEntry = new LegendEntry();
        diastolicEntry.label = getString(R.string.blood_pressure_diastolic);
        diastolicEntry.formColor = diastolicColor;
        legendEntries.add(diastolicEntry);
        mChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        mChart.getLegend().setCustom(legendEntries);

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        final List<Entry> systolicEntries = new ArrayList<>();
        final List<Entry> diastolicEntries = new ArrayList<>();

        for (final BloodPressureSample sample : data.samples) {
            int ts = (int) (sample.getTimestamp() / 1000L);
            int tsShorten = tsTranslation.shorten(ts);

            if (sample.getBpSystolic() > 0) {
                systolicEntries.add(new Entry(tsShorten, sample.getBpSystolic()));
            }
            if (sample.getBpDiastolic() > 0) {
                diastolicEntries.add(new Entry(tsShorten, sample.getBpDiastolic()));
            }
        }

        if (!systolicEntries.isEmpty()) {
            lineDataSets.add(createDataSet(systolicEntries, getString(R.string.blood_pressure_systolic), systolicColor));
        }
        if (!diastolicEntries.isEmpty()) {
            lineDataSets.add(createDataSet(diastolicEntries, getString(R.string.blood_pressure_diastolic), diastolicColor));
        }

        mChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "HH:mm"));

        if (!lineDataSets.isEmpty()) {
            final LineData lineData = new LineData(lineDataSets);
            mChart.setData(lineData);
        }

        if (data.systolicAvg > 0 && GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            final LimitLine avgLine = new LimitLine(data.systolicAvg);
            avgLine.setLineColor(Color.GRAY);
            avgLine.setLineWidth(1.5f);
            avgLine.enableDashedLine(15f, 10f, 0f);
            mChart.getAxisLeft().addLimitLine(avgLine);
        }
    }

    @Override
    public String getTitle() {
        return requireContext().getString(R.string.blood_pressure);
    }

    private void showExportDialog() {
        if (currentData == null || currentData.samples.isEmpty()) {
            return;
        }
        String dateLabel = mDateView.getText().toString();
        String[] options = {"PDF", "CSV"};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.appmanager_app_share)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        BloodPressureExportHelper.exportPdf(requireContext(), currentData.samples, dateLabel, getWhiteChartBitmap());
                    } else {
                        BloodPressureExportHelper.exportCsv(requireContext(), currentData.samples);
                    }
                })
                .show();
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

    private void setupLineChart() {
        mChart.setBackgroundColor(BACKGROUND_COLOR);
        mChart.getDescription().setText("");

        final XAxis xAxisBottom = mChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(86400f);
        xAxisBottom.setLabelCount(7, true);

        final YAxis yAxisLeft = mChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(200f);
        yAxisLeft.setAxisMinimum(40f);
        yAxisLeft.setDrawTopYLabelEntry(false);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);

        final YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    @Override
    protected void setupLegend(final Chart<?> chart) {}

    @Override
    protected void renderCharts() {
        mChart.invalidate();
    }

    private List<? extends BloodPressureSample> getSamples(final DBHandler db, final GBDevice device, int startTs, int endTs) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends BloodPressureSample> sampleProvider = coordinator.getBloodPressureSampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(startTs * 1000L, endTs * 1000L);
    }

    private BloodPressureChartsData fetchBloodPressureData(DBHandler db, GBDevice device, int startTs, int endTs) {
        List<? extends BloodPressureSample> samples = getSamples(db, device, startTs, endTs);

        final Accumulator systolicAccumulator = new Accumulator();
        final Accumulator diastolicAccumulator = new Accumulator();
        int systolicLast = DATA_INVALID;
        int diastolicLast = DATA_INVALID;

        for (final BloodPressureSample sample : samples) {
            if (sample.getBpSystolic() > 0) {
                systolicAccumulator.add(sample.getBpSystolic());
                systolicLast = sample.getBpSystolic();
            }
            if (sample.getBpDiastolic() > 0) {
                diastolicAccumulator.add(sample.getBpDiastolic());
                diastolicLast = sample.getBpDiastolic();
            }
        }

        final int systolicAvg = systolicAccumulator.getCount() > 0 ? (int) Math.round(systolicAccumulator.getAverage()) : DATA_INVALID;
        final int diastolicAvg = diastolicAccumulator.getCount() > 0 ? (int) Math.round(diastolicAccumulator.getAverage()) : DATA_INVALID;
        final int measurementCount = samples.size();

        return new BloodPressureChartsData(samples, systolicAvg, diastolicAvg, systolicLast, diastolicLast, measurementCount);
    }

    protected static class BloodPressureChartsData extends ChartsData {
        public List<? extends BloodPressureSample> samples;
        public final int systolicAvg;
        public final int diastolicAvg;
        public final int systolicLast;
        public final int diastolicLast;
        public final int measurementCount;

        public BloodPressureChartsData(List<? extends BloodPressureSample> samples, int systolicAvg, int diastolicAvg,
                                       int systolicLast, int diastolicLast, int measurementCount) {
            this.samples = samples;
            this.systolicAvg = systolicAvg;
            this.diastolicAvg = diastolicAvg;
            this.systolicLast = systolicLast;
            this.diastolicLast = diastolicLast;
            this.measurementCount = measurementCount;
        }
    }
}