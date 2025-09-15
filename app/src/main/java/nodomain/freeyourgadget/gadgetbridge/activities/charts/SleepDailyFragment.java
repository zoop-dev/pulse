/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Dikay900, José Rebelo, ozkanpakdil, Pavel Elagin, Petr Vaněk, Q-er, a0z

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
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis.SleepSession;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep.AbstractOverlayData;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep.OverlayDataFloat;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep.OverlayDataInt;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep.SimpleSleepDetailsOverlay;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep.SleepDetailsOverlay;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep.SleepDetailsView;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.databinding.FragmentSleepchartBinding;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.model.TimeSample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class SleepDailyFragment extends SleepFragment<SleepDailyFragment.MyChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(SleepDailyFragment.class);

    private FragmentSleepchartBinding binding;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;
    Prefs prefs = GBApplication.getPrefs();
    private final boolean CHARTS_SLEEP_RANGE_24H = prefs.getBoolean("chart_sleep_range_24h", false);
    private final boolean SHOW_CHARTS_AVERAGE = prefs.getBoolean("charts_show_average", true);
    private final int sleepLinesLimit = prefs.getInt("chart_sleep_lines_limit", 6);

    @Override
    protected boolean isSingleDay() {
        return true;
    }

    @Override
    protected MyChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> samples;
        if (CHARTS_SLEEP_RANGE_24H) {
            samples = getSamples(db, device);
        } else {
            samples = getSamplesofSleep(db, device);
        }
        List<? extends SleepScoreSample> sleepScoreSamples = new ArrayList<>();
        if (supportsSleepScore()) {
            sleepScoreSamples = getSleepScoreSamples(db, device, getTSStart(), getTSEnd());
        }
        MySleepChartsData mySleepChartsData = refreshSleepAmounts(samples, sleepScoreSamples);

        if (!CHARTS_SLEEP_RANGE_24H) {
            if (!mySleepChartsData.sleepSessions.isEmpty()) {
                long tstart = mySleepChartsData.sleepSessions.get(0).getSleepStart().getTime() / 1000;
                long tend = mySleepChartsData.sleepSessions.get(mySleepChartsData.sleepSessions.size() - 1).getSleepEnd().getTime() / 1000;

                for (Iterator<? extends ActivitySample> iterator = samples.iterator(); iterator.hasNext(); ) {
                    ActivitySample sample = iterator.next();
                    if (sample.getTimestamp() < tstart || sample.getTimestamp() > tend) {
                        iterator.remove();
                    }
                }
            }
        }
        DefaultChartsData<LineData> chartsData = refresh(device, samples);
        Triple<Float, Integer, Integer> hrData = calculateHrData(samples);
        Triple<Float, Float, Float> intensityData = calculateIntensityData(samples);

        List<SleepDetailsView.SleepDetail> stages = prepareStages(samples);

        AbstractOverlayData overlay = null;
        if (currentOverlay == OverlayType.HEART_RATE) {
            int average = Math.round(hrData.getLeft());
            average = SHOW_CHARTS_AVERAGE && average > 0 ? average : OverlayDataInt.NO_DATA;
            overlay = new OverlayDataInt(20, 140, prepareHR(samples), average, HEARTRATE_COLOR, Color.RED);
        } else if (currentOverlay == OverlayType.SPO2) {
            overlay = new OverlayDataInt(68, 100, prepareSpO2Overlay(db, device, samples.get(0).getTimestamp() * 1000L, samples.get(samples.size() - 1).getTimestamp() * 1000L), OverlayDataInt.NO_DATA, ContextCompat.getColor(requireContext(), R.color.spo2_color), Color.RED);
        } else if (currentOverlay == OverlayType.TEMPERATURE) {
            overlay = new OverlayDataFloat(28, 45, prepareTemperature(db, device, samples.get(0).getTimestamp() * 1000L, samples.get(samples.size() - 1).getTimestamp() * 1000L), OverlayDataFloat.NO_DATA, CHART_TEXT_COLOR, Color.RED);
        } else if (currentOverlay == OverlayType.RESPIRATORY_RATE) {
            final float[] respiratoryRateData = prepareRespiratoryRate(db, device, samples.get(0).getTimestamp() * 1000L, samples.get(samples.size() - 1).getTimestamp() * 1000L);
            final Accumulator accumulator = new Accumulator();
            for (float value : respiratoryRateData) {
                accumulator.add(value);
            }
            overlay = new OverlayDataFloat(
                    (float) (accumulator.getMin() / 2),
                    (float) (1.5d * accumulator.getMax()),
                    respiratoryRateData,
                    OverlayDataFloat.NO_DATA,
                    ContextCompat.getColor(requireContext(), R.color.respiratory_rate_color),
                    Color.RED
            );
        }
        return new MyChartsData(mySleepChartsData, chartsData, hrData.getLeft(), hrData.getMiddle(), hrData.getRight(), intensityData.getLeft(), intensityData.getMiddle(), intensityData.getRight(), stages, overlay);
    }

    private long getSamplesInterval(List<? extends TimeSample> samples) {
        long interval = 60000; // NOTE: assume max interval 1 minute.
        for (int i = 1; i < samples.size(); i++) {
            long delta = samples.get(i).getTimestamp() - samples.get(i - 1).getTimestamp();
            if (delta < interval) {
                interval = delta;
            }
        }
        return interval;
    }

    public float[] prepareTemperature(DBHandler db, GBDevice device, long tsStart, long tsEnd) {

        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        TimeSampleProvider<? extends TemperatureSample> provider = coordinator.getTemperatureSampleProvider(device, db.getDaoSession());

        List<? extends TemperatureSample> samples = provider.getAllSamples(tsStart, tsEnd);
        if (samples.isEmpty())
            return null;

        long interval = getSamplesInterval(samples);
        int count = (int) ((float) (tsEnd - tsStart) / interval);
        if (count == 0)
            return null;

        float[] result = new float[count];
        Arrays.fill(result, -1);

        for (TemperatureSample sp : samples) {
            int idx = (int) ((float) (sp.getTimestamp() - tsStart) / interval);
            if (idx > 0 && idx < count) {
                result[idx] = sp.getTemperature();
            }
        }

        return result;
    }

    public float[] prepareRespiratoryRate(DBHandler db, GBDevice device, long tsStart, long tsEnd) {

        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        TimeSampleProvider<? extends RespiratoryRateSample> provider = coordinator.getRespiratoryRateSampleProvider(device, db.getDaoSession());

        List<? extends RespiratoryRateSample> samples = provider.getAllSamples(tsStart, tsEnd);
        if (samples.isEmpty())
            return null;

        long interval = getSamplesInterval(samples);
        int count = (int) ((float) (tsEnd - tsStart) / interval);
        if (count == 0)
            return null;

        float[] result = new float[count];
        Arrays.fill(result, -1);

        for (RespiratoryRateSample sp : samples) {
            int idx = (int) ((float) (sp.getTimestamp() - tsStart) / interval);
            if (idx > 0 && idx < count) {
                result[idx] = sp.getRespiratoryRate();
            }
        }

        return result;
    }

    public int[] prepareSpO2Overlay(DBHandler db, GBDevice device, long tsStart, long tsEnd) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        TimeSampleProvider<? extends Spo2Sample> provider = coordinator.getSpo2SampleProvider(device, db.getDaoSession());

        List<? extends Spo2Sample> samples = provider.getAllSamples(tsStart, tsEnd);

        if (samples.isEmpty())
            return null;

        long interval = getSamplesInterval(samples);
        int count = (int) ((float) (tsEnd - tsStart) / interval);

        if (count == 0)
            return null;

        int[] result = new int[count];
        Arrays.fill(result, -1);

        for (Spo2Sample sp : samples) {
            int idx = (int) ((float) (sp.getTimestamp() - tsStart) / interval);
            if (idx > 0 && idx < count) {
                result[idx] = sp.getSpo2();
            }
        }
        return result;
    }

    public int[] prepareHR(List<? extends ActivitySample> samples) {
        if (samples.isEmpty()) {
            return null;
        }

        //NOTE: Overlay data. The start and end times of the samples should correspond to the sleep stages.
        // The time interval between samples is not important (it can be in minutes, seconds, etc.), but all intervals must be filled.
        // If there is a gap in the raw data, 'NO_DATA' should be used.
        // As we exactly know the intervals, we can use primitive types (which have better performance during drawing).
        // Samples already prepared in minute's interval  can simply be copied with filtering.

        TimestampTranslation tsTranslation = new TimestampTranslation();
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();

        int[] result = new int[samples.size()];
        int idx = 0;
        int lastTsShorten = 0;
        for (ActivitySample sample : samples) {
            if (sample.getKind() == ActivityKind.NOT_WORN || !heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                result[idx++] = -1;
                continue;
            }
            int tsShorten = tsTranslation.shorten(sample.getTimestamp());
            if (lastTsShorten == 0 || (tsShorten - lastTsShorten) <= 60 * HeartRateUtils.MAX_HR_MEASUREMENTS_GAP_MINUTES) {
                result[idx++] = sample.getHeartRate();
            } else {
                result[idx++] = OverlayDataInt.NO_DATA;
            }
            lastTsShorten = tsShorten;
        }

        return result;
    }


    private MySleepChartsData refreshSleepAmounts(List<? extends ActivitySample> samples, List<? extends SleepScoreSample> sleepScoreSamples) {
        SleepAnalysis sleepAnalysis = new SleepAnalysis();
        List<SleepSession> sleepSessions = sleepAnalysis.calculateSleepSessions(samples);

        final long lightSleepDuration = calculateLightSleepDuration(sleepSessions);
        final long deepSleepDuration = calculateDeepSleepDuration(sleepSessions);
        final long remSleepDuration = calculateRemSleepDuration(sleepSessions);
        final long awakeSleepDuration = calculateAwakeSleepDuration(sleepSessions);
        final long totalSeconds = lightSleepDuration + deepSleepDuration + remSleepDuration;

        int sleepScore = 0;
        if (!sleepScoreSamples.isEmpty()) {
            sleepScore = sleepScoreSamples.get(sleepScoreSamples.size() - 1).getSleepScore();
        }

        return new MySleepChartsData(sleepSessions, totalSeconds, awakeSleepDuration, remSleepDuration, deepSleepDuration, lightSleepDuration, sleepScore);
    }

    private long calculateLightSleepDuration(List<SleepSession> sleepSessions) {
        long result = 0;
        for (SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getLightSleepDuration();
        }
        return result;
    }

    private long calculateDeepSleepDuration(List<SleepSession> sleepSessions) {
        long result = 0;
        for (SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getDeepSleepDuration();
        }
        return result;
    }

    private long calculateRemSleepDuration(List<SleepSession> sleepSessions) {
        long result = 0;
        for (SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getRemSleepDuration();
        }
        return result;
    }

    private long calculateAwakeSleepDuration(List<SleepSession> sleepSessions) {
        long result = 0;
        for (SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getAwakeSleepDuration();
        }
        return result;
    }

    protected void sleepStagesGaugeUpdate(MySleepChartsData pieData) {
        int[] colors = new int[]{
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_light_sleep_light),
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_deep_sleep_light),
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_rem_sleep_light),
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_awake_sleep_light),
        };
        long total = pieData.getTotalSleep() + pieData.getTotalAwake();
        float[] segments = new float[]{
                pieData.getTotalLight() > 0 ? (float) pieData.getTotalLight() / total : 0,
                pieData.getTotalDeep() > 0 ? (float) pieData.getTotalDeep() / total : 0,
                pieData.getTotalRem() > 0 ? (float) pieData.getTotalRem() / total : 0,
                pieData.getTotalAwake() > 0 ? (float) pieData.getTotalAwake() / total : 0,
        };
        final int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                300,
                GBApplication.getContext().getResources().getDisplayMetrics()
        );
        String lowerText = "";
        if (supportsSleepScore()) {
            lowerText = GBApplication.getContext().getString(R.string.sleep_score_value, pieData.getSleepScore());
        }
        binding.sleepStagesGauge.setImageBitmap(GaugeDrawer.drawCircleGaugeSegmented(
                width,
                width / 15,
                colors,
                segments,
                true,
                String.valueOf(timeStringFormat(pieData.getTotalSleep())),
                lowerText,
                getContext()
        ));
    }

    private String timeStringFormat(long seconds) {
        return DateTimeUtils.formatDurationHoursMinutes(seconds, TimeUnit.SECONDS);
    }

    @Override
    protected void updateChartsnUIThread(MyChartsData mcd) {
        MySleepChartsData pieData = mcd.getPieData();

        if (mcd.getStages() != null) {
            SleepDetailsOverlay overlay = (mcd.getOverlayData() != null) ? new SimpleSleepDetailsOverlay(mcd.getOverlayData(), Color.BLACK) : null;
            binding.sleepDetails.setData(mcd.getStages(), overlay);
        }

        Date date = new Date((long) this.getTSEnd() * 1000);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(date);
        binding.sleepDate.setText(formattedDate);

        sleepStagesGaugeUpdate(pieData);

        if (!pieData.sleepSessions.isEmpty()) {
            binding.sleepChartLegendAwakeTime.setText(timeStringFormat(pieData.getTotalAwake()));
            binding.sleepChartLegendRemTime.setText(timeStringFormat(pieData.getTotalRem()));
            binding.sleepChartLegendDeepTime.setText(timeStringFormat(pieData.getTotalDeep()));
            binding.sleepChartLegendLightTime.setText(timeStringFormat(pieData.getTotalLight()));
        } else {
            binding.sleepChartLegendAwakeTime.setText("-");
            binding.sleepChartLegendRemTime.setText("-");
            binding.sleepChartLegendDeepTime.setText("-");
            binding.sleepChartLegendLightTime.setText("-");
        }
        if (!supportsRemSleep(getChartsHost().getDevice())) {
            binding.sleepChartLegendRemTimeWrapper.setVisibility(View.GONE);
        }
        if (!supportsAwakeSleep(getChartsHost().getDevice())) {
            binding.sleepChartLegendAwakeTimeWrapper.setVisibility(View.GONE);
        }
        binding.sleepchartInfo.setText(buildYouSleptText(pieData));
        binding.sleepchartInfo.setMovementMethod(new ScrollingMovementMethod());

        binding.sleepchart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        binding.sleepchart.getXAxis().setValueFormatter(mcd.getChartsData().getXValueFormatter());
        binding.sleepchart.getAxisLeft().setDrawLabels(false);

        binding.sleepchart.setData(mcd.getChartsData().getData());
        int heartRateMin = mcd.getHeartRateAxisMin();
        int heartRateMax = mcd.getHeartRateAxisMax();
        int heartRateAvg = Math.round(mcd.getHeartRateAverage());
        float intensityTotal = mcd.getIntensityTotal();
        binding.sleepHrLowest.setText(String.valueOf(heartRateMin > 0 ? heartRateMin : "-"));
        binding.sleepHrHighest.setText(String.valueOf(heartRateMax > 0 ? heartRateMax : "-"));
        binding.sleepHrAverage.setText(String.valueOf(heartRateAvg > 0 ? heartRateAvg : "-"));
        binding.sleepMovementIntensity.setText(intensityTotal > 0 ? new DecimalFormat("###.#").format(intensityTotal) : "-");

        if (supportsHeartrate(getChartsHost().getDevice()) && SHOW_CHARTS_AVERAGE) {
            if (mcd.getHeartRateAxisMax() != 0 || mcd.getHeartRateAxisMin() != 0) {
                binding.sleepchart.getAxisRight().setAxisMaximum(mcd.getHeartRateAxisMax() + (mcd.getHeartRateAxisMin() / 2f));
                binding.sleepchart.getAxisRight().setAxisMinimum(mcd.getHeartRateAxisMin() / 2f);
            }
            LimitLine hrAverage_line = new LimitLine(mcd.getHeartRateAverage());
            hrAverage_line.setLineColor(Color.RED);
            hrAverage_line.setLineWidth(1.5f);
            hrAverage_line.enableDashedLine(15f, 10f, 0f);
            binding.sleepchart.getAxisRight().removeAllLimitLines();
            binding.sleepchart.getAxisRight().addLimitLine(hrAverage_line);
        }
    }

    private Triple<Float, Integer, Integer> calculateHrData(List<? extends ActivitySample> samples) {
        if (samples.isEmpty()) {
            return Triple.of(0f, 0, 0);
        }

        final Accumulator accumulator = new Accumulator();
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();
        for (ActivitySample sample : samples) {
            if (ActivityKind.isSleep(sample.getKind())) {
                int heartRate = sample.getHeartRate();
                if (heartRateUtilsInstance.isValidHeartRateValue(heartRate)) {
                    accumulator.add(heartRate);
                }
            }
        }
        if (accumulator.getCount() == 0) {
            return Triple.of(0f, 0, 0);
        }

        return Triple.of((float) accumulator.getAverage(), (int) accumulator.getMin(), (int) accumulator.getMax());
    }

    private Triple<Float, Float, Float> calculateIntensityData(List<? extends ActivitySample> samples) {
        if (samples.isEmpty()) {
            return Triple.of(0f, 0f, 0f);
        }

        final Accumulator accumulator = new Accumulator();
        for (ActivitySample s : samples) {
            if (s.getKind() == ActivityKind.LIGHT_SLEEP || s.getKind() == ActivityKind.DEEP_SLEEP) {
                float intensity = s.getIntensity();
                accumulator.add(intensity);
            }
        }
        if (accumulator.getCount() == 0) {
            return Triple.of(0f, 0f, 0f);
        }

        return Triple.of((float) accumulator.getSum(), (float) accumulator.getMin(), (float) accumulator.getMax());
    }

    private String buildYouSleptText(MySleepChartsData pieData) {
        final StringBuilder result = new StringBuilder();
        if (!pieData.getSleepSessions().isEmpty()) {
            for (SleepSession sleepSession : pieData.getSleepSessions()) {
                if (result.length() > 0) {
                    result.append("  |  ");
                }
                String from = DateTimeUtils.timeToString(sleepSession.getSleepStart());
                String to = DateTimeUtils.timeToString(sleepSession.getSleepEnd());
                result.append(String.format("%s - %s", from, to));
            }
        }
        return result.toString();
    }

    private enum OverlayType {
        NONE,
        HEART_RATE,
        SPO2,
        TEMPERATURE,
        RESPIRATORY_RATE,
    }

    private OverlayType currentOverlay = OverlayType.NONE;

    public boolean supportsSpO2(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.supportsSpo2(device);
    }

    public boolean supportsTemperature(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.supportsTemperatureMeasurement(device);
    }

    public boolean supportsSleepRespiratoryRate(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.supportsSleepRespiratoryRate(device);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentSleepchartBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> getChartsHost().enableSwipeRefresh(scrollY == 0));

        ChipGroup chipGroup = binding.sleepChartOverlayGroup;

        chipGroup.setOnCheckedStateChangeListener((group, list) -> {
            currentOverlay = list.isEmpty() ? OverlayType.NONE : (OverlayType) group.findViewById(list.get(0)).getTag();
            refresh();
        });

        if (supportsHeartrate(getChartsHost().getDevice())) {
            Chip hrChip = (Chip) inflater.inflate(R.layout.layout_chart_chip, chipGroup, false);
            hrChip.setText(ContextCompat.getString(GBApplication.getContext(), R.string.heart_rate));
            hrChip.setChipIconVisible(true);
            hrChip.setChipIconResource(R.drawable.ic_heartrate);
            hrChip.setTag(OverlayType.HEART_RATE);
            chipGroup.addView(hrChip);
        }

        if (supportsSpO2(getChartsHost().getDevice())) {
            Chip spo2Chip = (Chip) inflater.inflate(R.layout.layout_chart_chip, chipGroup, false);
            spo2Chip.setText(ContextCompat.getString(GBApplication.getContext(), R.string.menuitem_spo2));
            spo2Chip.setChipIconVisible(true);
            spo2Chip.setChipIconResource(R.drawable.ic_spo2);
            spo2Chip.setTag(OverlayType.SPO2);
            chipGroup.addView(spo2Chip);
        }

        if (supportsTemperature(getChartsHost().getDevice())) {
            Chip tempChip = (Chip) inflater.inflate(R.layout.layout_chart_chip, chipGroup, false);
            tempChip.setText(ContextCompat.getString(GBApplication.getContext(), R.string.menuitem_temperature));
            tempChip.setChipIconVisible(true);
            tempChip.setChipIconResource(R.drawable.ic_temperature);
            tempChip.setTag(OverlayType.TEMPERATURE);
            chipGroup.addView(tempChip);
        }

        if (supportsSleepRespiratoryRate(getChartsHost().getDevice())) {
            Chip tempChip = (Chip) inflater.inflate(R.layout.layout_chart_chip, chipGroup, false);
            tempChip.setText(ContextCompat.getString(GBApplication.getContext(), R.string.respiratoryrate));
            tempChip.setChipIconVisible(true);
            tempChip.setChipIconResource(R.drawable.ic_pulmonology);
            tempChip.setTag(OverlayType.RESPIRATORY_RATE);
            chipGroup.addView(tempChip);
        }

        binding.sleepchartInfo.setMaxLines(sleepLinesLimit);

        setupActivityChart();

        int[] config = new int[]{
                getIndexOfActivity(ActivityKind.AWAKE_SLEEP),
                getIndexOfActivity(ActivityKind.REM_SLEEP),
                getIndexOfActivity(ActivityKind.LIGHT_SLEEP),
                getIndexOfActivity(ActivityKind.DEEP_SLEEP)
        };
        binding.sleepDetails.setConfig(config);

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(ChartsHost.REFRESH)) {
            // TODO: use LimitLines to visualize smart alarms?
            mSmartAlarmFrom = intent.getIntExtra("smartalarm_from", -1);
            mSmartAlarmTo = intent.getIntExtra("smartalarm_to", -1);
            mTimestampFrom = intent.getIntExtra("recording_base_timestamp", -1);
            mSmartAlarmGoneOff = intent.getIntExtra("alarm_gone_off", -1);
            refresh();
        } else {
            super.onReceive(context, intent);
        }
    }

    private void setupActivityChart() {
        binding.sleepchart.setBackgroundColor(BACKGROUND_COLOR);
        binding.sleepchart.getDescription().setTextColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(binding.sleepchart);

        XAxis x = binding.sleepchart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = binding.sleepchart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setAxisMaximum(1f);
        y.setAxisMinimum(0);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);
        y.setEnabled(true);

        YAxis yAxisRight = binding.sleepchart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(supportsHeartrate(getChartsHost().getDevice()));
        yAxisRight.setDrawLabels(true);
        yAxisRight.setDrawTopYLabelEntry(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
        yAxisRight.setAxisMaximum(HeartRateUtils.getInstance().getMaxHeartRate());
        yAxisRight.setAxisMinimum(HeartRateUtils.getInstance().getMinHeartRate());
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = super.createLegendEntries(chart);

        if (supportsHeartrate(getChartsHost().getDevice())) {
            LegendEntry hrEntry = new LegendEntry();
            hrEntry.label = HEARTRATE_LABEL;
            hrEntry.formColor = HEARTRATE_COLOR;
            legendEntries.add(hrEntry);
            if (SHOW_CHARTS_AVERAGE) {
                LegendEntry hrAverageEntry = new LegendEntry();
                hrAverageEntry.label = HEARTRATE_AVERAGE_LABEL;
                hrAverageEntry.formColor = Color.RED;
                legendEntries.add(hrAverageEntry);
            }
        }
        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected void renderCharts() {
        binding.sleepchart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
    }

    protected static class MySleepChartsData extends ChartsData {
        private final long totalSleep;
        private final long totalAwake;
        private final long totalRem;
        private final long totalDeep;
        private final long totalLight;
        private final int sleepScore;
        private final List<SleepSession> sleepSessions;

        public MySleepChartsData(List<SleepSession> sleepSessions, long totalSleep, long totalAwake, long totalRem, long totalDeep, long totalLight, int sleepScore) {
            this.sleepSessions = sleepSessions;
            this.totalAwake = totalAwake;
            this.totalSleep = totalSleep;
            this.totalRem = totalRem;
            this.totalDeep = totalDeep;
            this.totalLight = totalLight;
            this.sleepScore = sleepScore;
        }

        public long getTotalSleep() {
            return totalSleep;
        }

        public long getTotalAwake() {
            return totalAwake;
        }

        public long getTotalRem() {
            return totalRem;
        }

        public long getTotalDeep() {
            return totalDeep;
        }

        public long getTotalLight() {
            return totalLight;
        }

        public int getSleepScore() {
            return sleepScore;
        }

        public List<SleepSession> getSleepSessions() {
            return sleepSessions;
        }
    }

    protected static class MyChartsData extends ChartsData {
        private final DefaultChartsData<LineData> chartsData;
        private final MySleepChartsData pieData;
        private final float heartRateAverage;
        private final int heartRateAxisMax;
        private final int heartRateAxisMin;
        private final float intensityAxisMax;
        private final float intensityAxisMin;
        private final float intensityTotal;

        private final List<SleepDetailsView.SleepDetail> stages;

        private final AbstractOverlayData overlayData;

        public MyChartsData(MySleepChartsData pieData, DefaultChartsData<LineData> chartsData, float heartRateAverage, int heartRateAxisMin, int heartRateAxisMax, float intensityTotal, float intensityAxisMin, float intensityAxisMax, List<SleepDetailsView.SleepDetail> stages, AbstractOverlayData overlayData) {
            this.pieData = pieData;
            this.chartsData = chartsData;
            this.heartRateAverage = heartRateAverage;
            this.heartRateAxisMax = heartRateAxisMax;
            this.heartRateAxisMin = heartRateAxisMin;
            this.intensityTotal = intensityTotal;
            this.intensityAxisMin = intensityAxisMin;
            this.intensityAxisMax = intensityAxisMax;
            this.stages = stages;
            this.overlayData = overlayData;
        }

        public MySleepChartsData getPieData() {
            return pieData;
        }

        public DefaultChartsData<LineData> getChartsData() {
            return chartsData;
        }

        public float getHeartRateAverage() {
            return heartRateAverage;
        }

        public int getHeartRateAxisMax() {
            return heartRateAxisMax;
        }

        public int getHeartRateAxisMin() {
            return heartRateAxisMin;
        }

        public float getIntensityAxisMax() {
            return intensityAxisMax;
        }

        public float getIntensityAxisMin() {
            return intensityAxisMin;
        }

        public float getIntensityTotal() {
            return intensityTotal;
        }

        public List<SleepDetailsView.SleepDetail> getStages() {
            return stages;
        }

        public AbstractOverlayData getOverlayData() {
            return overlayData;
        }
    }
}
