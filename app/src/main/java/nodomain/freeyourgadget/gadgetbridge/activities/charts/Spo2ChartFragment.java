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

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
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
import nodomain.freeyourgadget.gadgetbridge.databinding.FragmentSpo2Binding;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2ManualMeasurement;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;

// Based on StressDailyFragment

public class Spo2ChartFragment extends AbstractChartFragment<Spo2ChartFragment.Spo2ChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(Spo2ChartFragment.class);

    static int DATA_INVALID = -1;

    private FragmentSpo2Binding binding;

    private int BACKGROUND_COLOR;
    private int CHART_TEXT_COLOR;
    private int TEXT_COLOR;
    private int LEGEND_TEXT_COLOR;

    private TimestampTranslation tsTranslation;

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
        binding = FragmentSpo2Binding.inflate(inflater, container, false);
        binding.manualMeasurements.setVisibility(View.GONE);
        setupLineChart();
        refresh();
        return binding.getRoot();
    }

    @Override
    protected Spo2ChartsData refreshInBackground(final ChartsHost chartsHost, final DBHandler db, final GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        day.add(Calendar.DATE, 0);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, 0);
        int startTs = (int) (day.getTimeInMillis() / 1000);
        int endTs = startTs + 24 * 60 * 60 - 1;
        tsTranslation = new TimestampTranslation();
        tsTranslation.shorten(startTs);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(chartsHost.getEndDate());
        binding.dateView.setText(formattedDate);
        return fetchSpo2Data(db, device, startTs, endTs);
    }

    protected LineDataSet createDataSet(final List<Entry> values, boolean manualPoints) {
        final LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.pref_header_spo2));
        lineDataSet.setColor(getResources().getColor(R.color.spo2_color));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2.2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setValueTextColor(TEXT_COLOR);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        if (manualPoints) {
            lineDataSet.setDrawCircles(true);
            lineDataSet.enableDashedLine(0f,1f,0f);
        }
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.ROOT, "%d", (int) value);
            }
        });
        return lineDataSet;
    }

    @Override
    protected void updateChartsnUIThread(Spo2ChartsData data) {
        binding.manualMeasurementsList.removeAllViews();
        binding.manualMeasurements.setVisibility(View.GONE);
        final String emptyValue = requireContext().getString(R.string.stats_empty_value);
        binding.spo2Minimum.setText(data.minimum > 0 ? String.valueOf(data.minimum) : emptyValue);
        binding.spo2Maximum.setText(data.maximum > 0 ? String.valueOf(data.maximum) : emptyValue);
        binding.spo2Average.setText(data.average > 0 ? String.valueOf(data.average) : emptyValue);
        binding.spo2LineChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        binding.spo2LineChart.getAxisLeft().removeAllLimitLines();
        Date date = new Date((long) getTSEnd() * 1000);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(date);
        binding.dateView.setText(formattedDate);

        final List<LegendEntry> legendEntries = new ArrayList<>(1);
        final LegendEntry spo2RateEntry = new LegendEntry();
        spo2RateEntry.label = getString(R.string.pref_header_spo2);
        spo2RateEntry.formColor = getResources().getColor(R.color.spo2_color);
        legendEntries.add(spo2RateEntry);
        final LegendEntry spo2RateAvg = new LegendEntry();
        spo2RateAvg.label = getString(R.string.stress_average);
        spo2RateAvg.formColor = Color.GRAY;
        legendEntries.add(spo2RateAvg);
        binding.spo2LineChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        binding.spo2LineChart.getLegend().setCustom(legendEntries);

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        List<Entry> measurementsEntries = new ArrayList<>();
        List<Entry> manualMeasurementsEntries = new ArrayList<>();
        List<Spo2Sample> manualMeasurementSamples = new ArrayList<Spo2Sample>();
        int lastTsShorten = 0;
        for (final Spo2Sample sample : data.samples) {
            int ts = (int) (sample.getTimestamp() / 1000L);
            int tsShorten = tsTranslation.shorten(ts);
            if (sample.getType() == Spo2Sample.Type.MANUAL) {
                manualMeasurementsEntries.add(new Entry(tsShorten, sample.getSpo2()));
                manualMeasurementSamples.add(sample);
                continue;
            }
            if (lastTsShorten == 0 || (tsShorten - lastTsShorten) <= 300) {
                measurementsEntries.add(new Entry(tsShorten, sample.getSpo2()));
            } else {
                if (!measurementsEntries.isEmpty()) {
                    List<Entry> clone = new ArrayList<>(measurementsEntries.size());
                    clone.addAll(measurementsEntries);
                    lineDataSets.add(createDataSet(clone, false));
                    measurementsEntries.clear();
                }
            }
            lastTsShorten = tsShorten;
            measurementsEntries.add(new Entry(tsShorten, sample.getSpo2()));
        }

        if (!measurementsEntries.isEmpty()) {
            lineDataSets.add(createDataSet(measurementsEntries, false));
        }
        if (!manualMeasurementsEntries.isEmpty()) {
            lineDataSets.add(createDataSet(manualMeasurementsEntries, true));
        }

        binding.spo2LineChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "HH:mm"));

        final LineData lineData = new LineData(lineDataSets);

        if (data.average > 0 && GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            final LimitLine averageLine = new LimitLine(data.average);
            averageLine.setLineColor(Color.GRAY);
            averageLine.setLineWidth(1.5f);
            averageLine.enableDashedLine(15f, 10f, 0f);
            binding.spo2LineChart.getAxisLeft().addLimitLine(averageLine);
        }

        if (!manualMeasurementSamples.isEmpty()) {
            for (Spo2Sample sample : manualMeasurementSamples) {
                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_spo2_manual_measurment, binding.manualMeasurementsList, false);
                TextView timeText = itemView.findViewById(R.id.timeText);
                TextView valueText = itemView.findViewById(R.id.valueText);
                Spo2ManualMeasurement measurement = new Spo2ManualMeasurement(sample.getTimestamp(), sample.getSpo2());
                timeText.setText(measurement.getTime());
                valueText.setText(measurement.getValue());
                binding.manualMeasurementsList.addView(itemView);
            }
            binding.manualMeasurementsList.getChildAt(binding.manualMeasurementsList.getChildCount() - 1)
                    .findViewById(R.id.separator)
                    .setVisibility(View.GONE);
            binding.manualMeasurements.setVisibility(View.VISIBLE);
        }

        binding.spo2LineChart.setData(lineData);
    }

    @Override
    public String getTitle() {
        return requireContext().getString(R.string.pref_header_spo2);
    }

    private void setupLineChart() {
        binding.spo2LineChart.setBackgroundColor(BACKGROUND_COLOR);
        binding.spo2LineChart.getDescription().setText("");

        final XAxis xAxisBottom = binding.spo2LineChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(86400f);
        xAxisBottom.setLabelCount(7, true);

        final YAxis yAxisLeft = binding.spo2LineChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100.5f);
        yAxisLeft.setAxisMinimum(65f);
        yAxisLeft.setDrawTopYLabelEntry(false);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);

        final YAxis yAxisRight = binding.spo2LineChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    @Override
    protected void setupLegend(final Chart<?> chart) {}

    @Override
    protected void renderCharts() {
        binding.spo2LineChart.invalidate();
    }

    private List<? extends Spo2Sample> getSamples(final DBHandler db, final GBDevice device, int startTs, int endTs) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends Spo2Sample> sampleProvider = coordinator.getSpo2SampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(startTs * 1000L, endTs * 1000L);
    }


    private Spo2ChartsData fetchSpo2Data(DBHandler db, GBDevice device, int startTs, int endTs) {
        List<? extends Spo2Sample> samples = getSamples(db, device, startTs, endTs);

        final Accumulator accumulator = new Accumulator();
        for (int i = 0; i < samples.size(); i++) {
            final Spo2Sample sample = samples.get(i);
            if (sample.getSpo2() > 0) {
                accumulator.add(sample.getSpo2());
            }
        }

        final int average = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getAverage()) : DATA_INVALID;
        final int minimum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMin()) : DATA_INVALID;
        final int maximum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMax()) : DATA_INVALID;

        return new Spo2ChartsData(samples, average, minimum, maximum);
    }

    protected static class Spo2ChartsData extends ChartsData {
        public List<? extends Spo2Sample> samples;
        public final int average;
        public final int minimum;
        public final int maximum;

        public Spo2ChartsData(List<? extends Spo2Sample> samples, int average, int minimum, int maximum) {
            this.samples = samples;
            this.average = average;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }

}
