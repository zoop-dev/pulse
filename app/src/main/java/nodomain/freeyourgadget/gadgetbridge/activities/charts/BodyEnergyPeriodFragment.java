package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
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
import nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class BodyEnergyPeriodFragment extends AbstractChartFragment<BodyEnergyPeriodFragment.BodyEnergyPeriodData> {
    protected static final Logger LOG = LoggerFactory.getLogger(BodyEnergyPeriodFragment.class);

    static int SEC_PER_DAY = 24 * 60 * 60;
    static int DATA_INVALID = -1;

    private int BACKGROUND_COLOR;
    private int CHART_TEXT_COLOR;
    private int LEGEND_TEXT_COLOR;
    private int BODY_ENERGY_COLOR;

    private TextView mDateView;
    private TextView bodyEnergyMinimum;
    private TextView bodyEnergyMaximum;
    private CandleStickChart bodyEnergyChart;
    private int TOTAL_DAYS;

    @Override
    protected boolean isSingleDay() {
        return false;
    }

    public static BodyEnergyPeriodFragment newInstance(int totalDays) {
        BodyEnergyPeriodFragment fragment = new BodyEnergyPeriodFragment();
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
        BODY_ENERGY_COLOR = ContextCompat.getColor(requireContext(), R.color.body_energy_level_color);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_body_energy_period, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        mDateView = rootView.findViewById(R.id.date_view);
        bodyEnergyMinimum = rootView.findViewById(R.id.body_energy_minimum);
        bodyEnergyMaximum = rootView.findViewById(R.id.body_energy_maximum);
        bodyEnergyChart = rootView.findViewById(R.id.body_energy_chart);

        setupChart();
        refresh();
        setupLegend(bodyEnergyChart);

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.body_energy);
    }

    private int getStartTs() {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        return (int) (day.getTimeInMillis() / 1000) - SEC_PER_DAY * (TOTAL_DAYS - 1);
    }

    private BodyEnergyDayData fetchBodyEnergyDataForDay(DBHandler db, GBDevice device, int startTs) {
        int endTs = startTs + SEC_PER_DAY - 1;
        List<? extends BodyEnergySample> samples = getSamples(db, device, startTs, endTs);

        final Accumulator accumulator = new Accumulator();
        for (final BodyEnergySample sample : samples) {
            if (sample.getEnergy() > 0) {
                accumulator.add(sample.getEnergy());
            }
        }

        final int minimum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMin()) : DATA_INVALID;
        final int maximum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMax()) : DATA_INVALID;

        return new BodyEnergyDayData(samples, minimum, maximum);
    }

    @Override
    protected BodyEnergyPeriodData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        final int startTs = getStartTs();

        List<BodyEnergyDayData> result = new ArrayList<>();
        for (int i = 0; i < TOTAL_DAYS; i++) {
            BodyEnergyDayData dayData = fetchBodyEnergyDataForDay(db, device, startTs + i * SEC_PER_DAY);
            result.add(dayData);
        }
        return new BodyEnergyPeriodData(result);
    }

    private List<? extends BodyEnergySample> getSamples(DBHandler db, GBDevice device, int startTs, int endTs) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends BodyEnergySample> sampleProvider = coordinator.getBodyEnergySampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(startTs * 1000L, endTs * 1000L);
    }

    @Override
    protected void updateChartsnUIThread(BodyEnergyPeriodData data) {
        final int startTs = getStartTs();
        mDateView.setText(DateTimeUtils.formatDaysUntil(TOTAL_DAYS, getTSEnd()));

        final Accumulator minAccumulator = new Accumulator();
        final Accumulator maxAccumulator = new Accumulator();

        final ArrayList<CandleEntry> candleEntries = new ArrayList<>();

        for (int i = 0; i < data.days.size(); i++) {
            final BodyEnergyDayData dayData = data.days.get(i);
            if (dayData.minimum > 0 && dayData.maximum > 0) {
                minAccumulator.add(dayData.minimum);
                maxAccumulator.add(dayData.maximum);
                candleEntries.add(new CandleEntry(i, dayData.maximum, dayData.minimum, dayData.minimum, dayData.maximum));
            }
        }

        final String emptyValue = requireContext().getString(R.string.stats_empty_value);
        final int minimum = minAccumulator.getCount() > 0 ? (int) Math.round(minAccumulator.getMin()) : DATA_INVALID;
        final int maximum = maxAccumulator.getCount() > 0 ? (int) Math.round(maxAccumulator.getMax()) : DATA_INVALID;

        bodyEnergyMinimum.setText(minimum > 0 ? String.valueOf(minimum) : emptyValue);
        bodyEnergyMaximum.setText(maximum > 0 ? String.valueOf(maximum) : emptyValue);

        final String fmt = TOTAL_DAYS == 7 ? "EEE" : "dd";
        SimpleDateFormat formatDay = new SimpleDateFormat(fmt, Locale.getDefault());
        ValueFormatter formatter = new ValueFormatter() {
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
        bodyEnergyChart.getXAxis().setValueFormatter(formatter);

        if (!candleEntries.isEmpty()) {
            CandleDataSet candleDataSet = new CandleDataSet(candleEntries, getString(R.string.body_energy));
            candleDataSet.setDrawValues(false);
            candleDataSet.setDrawIcons(false);
            candleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            candleDataSet.setShadowColor(BODY_ENERGY_COLOR);
            candleDataSet.setShadowWidth(2f);
            candleDataSet.setDecreasingColor(BODY_ENERGY_COLOR);
            candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
            candleDataSet.setIncreasingColor(BODY_ENERGY_COLOR);
            candleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
            candleDataSet.setNeutralColor(BODY_ENERGY_COLOR);
            candleDataSet.setBarSpace(0.3f);
            candleDataSet.setShowCandleBar(true);

            CandleData candleData = new CandleData(candleDataSet);
            bodyEnergyChart.setData(candleData);
        } else {
            bodyEnergyChart.setData(null);
        }

    }

    private void setupChart() {
        bodyEnergyChart.setBackgroundColor(BACKGROUND_COLOR);
        bodyEnergyChart.getDescription().setEnabled(false);

        if (TOTAL_DAYS <= 7) {
            bodyEnergyChart.setTouchEnabled(false);
            bodyEnergyChart.setPinchZoom(false);
        }
        bodyEnergyChart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = bodyEnergyChart.getXAxis();
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

        final YAxis yAxisLeft = bodyEnergyChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100f);
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setGranularity(10f);
        yAxisLeft.setGranularityEnabled(true);

        final YAxis yAxisRight = bodyEnergyChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(1);

        LegendEntry rangeEntry = new LegendEntry();
        rangeEntry.label = getString(R.string.body_energy);
        rangeEntry.formColor = BODY_ENERGY_COLOR;
        legendEntries.add(rangeEntry);

        bodyEnergyChart.getLegend().setCustom(legendEntries);
        bodyEnergyChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        bodyEnergyChart.getLegend().setWordWrapEnabled(true);
    }

    @Override
    protected void renderCharts() {
        bodyEnergyChart.invalidate();
    }

    protected static class BodyEnergyPeriodData extends ChartsData {
        public List<BodyEnergyDayData> days;

        protected BodyEnergyPeriodData(List<BodyEnergyDayData> days) {
            this.days = days;
        }
    }

    protected static class BodyEnergyDayData extends ChartsData {
        public List<? extends BodyEnergySample> samples;
        public int minimum;
        public int maximum;

        protected BodyEnergyDayData(List<? extends BodyEnergySample> samples, int minimum, int maximum) {
            this.samples = samples;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }
}

