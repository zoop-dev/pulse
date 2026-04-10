/*  Copyright (C) 2025-2026 José Rebelo, a0z, Me7c7, punchdeerflyscorpion, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BREATHS_PER_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_CELSIUS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KMPH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_PERCENTAGE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_WATT;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SpeedYLabelFormatter;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.TimestampTranslation;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.model.workout.WorkoutChart;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;

public class DefaultWorkoutCharts {
    public static List<WorkoutChart> buildDefaultCharts(final Context context,
                                                        final List<? extends ActivityPoint> activityPoints,
                                                        final ActivityKind activityKind) {
        final ActivityKind.CycleUnit cycleUnit = ActivityKind.getCycleUnit(activityKind);
        final List<WorkoutChart> charts = new LinkedList<>();
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        final int initalCapacity = activityPoints.size();
        final List<Entry> heartRateDataPoints = new ArrayList<>(initalCapacity);
        final List<Entry> speedDataPoints = new ArrayList<>(initalCapacity);
        final List<Entry> cadenceDataPoints = new ArrayList<>(initalCapacity);
        final List<Entry> elevationDataPoints = new ArrayList<>(initalCapacity);
        final List<Entry> powerDataPoints = new ArrayList<>(initalCapacity);
        final List<Entry> respiratoryRatePoints = new ArrayList<>(initalCapacity);
        final List<Entry> temperatureDataPoints = new ArrayList<>(initalCapacity);
        final List<Entry> depthDataPoints = new ArrayList<>(initalCapacity);
        final List<Entry> distancePoints = new ArrayList<>(initalCapacity);
        final List<Entry> stridePoints = new ArrayList<>(initalCapacity);
        final List<Entry> staminaPoints = new ArrayList<>(initalCapacity);
        final List<Entry> bodyEnergyPoints = new ArrayList<>(initalCapacity);
        final List<Entry> stepLengthPoints = new ArrayList<>(initalCapacity);
        final List<Entry> n2LoadPoints = new ArrayList<>(initalCapacity);
        final List<Entry> cnsToxicityPoints = new ArrayList<>(initalCapacity);

        // some activities / devices provide all points with zero values
        boolean hasSpeedValues = false;
        boolean hasCadenceValues = false;
        boolean hasElevationValues = false;
        boolean hasPowerValues = false;
        boolean hasRespiratoryRateValues = false;
        boolean hasTemperatureValues = false;
        boolean hasDepthValues = false;
        boolean hasDistanceValues = false;
        boolean hasStrideValues = false;
        boolean hasBodyEnergyValues = false;
        boolean hasStaminaValues = false;
        boolean hasStepLengthValues = false;
        boolean hasN2LoadValues = false;
        boolean hasCnsToxicityValues = false;

        final Accumulator cadenceAccumulator = new Accumulator();
        final Accumulator temperatureAccumulator = new Accumulator();

        for (int i = 0; i <= activityPoints.size() - 1; i++) {
            final ActivityPoint point = activityPoints.get(i);
            final long tsShorten = tsTranslation.shorten((int) point.getTime().getTime());

            // HR
            final int heartRate = point.getHeartRate();
            if (heartRate > 0) {
                heartRateDataPoints.add(new Entry(tsShorten, heartRate));
            }

            // Elevation
            final double elevation = point.getAltitude();
            if (elevation > GPSCoordinate.UNKNOWN_ALTITUDE) {
                elevationDataPoints.add(new Entry(tsShorten, (float) elevation));
                hasElevationValues = hasElevationValues || (elevation != 0.0);
            }

            // Speed
            final float speed = point.getSpeed();
            if(speed >= 0.0f) {
                speedDataPoints.add(new Entry(tsShorten, speed));
                hasSpeedValues = hasSpeedValues || (speed > 0.0f);
            }

            // Cadence
            final float cadence = point.getCadence();
            if(cadence >= 0.0f){
                cadenceDataPoints.add(new Entry(tsShorten, cadence));
                cadenceAccumulator.add(cadence);
                hasCadenceValues = hasCadenceValues || (cadence > 0.0f);
            }

            final float power = point.getPower();
            if (power >= 0.0f) {
                powerDataPoints.add(new Entry(tsShorten, power));
                hasPowerValues = hasPowerValues || (power > 0.0f);
            }

            final float respiratoryRate = point.getRespiratoryRate();
            if (respiratoryRate >= 0.0f) {
                respiratoryRatePoints.add(new Entry(tsShorten, respiratoryRate));
                hasRespiratoryRateValues = hasRespiratoryRateValues || (respiratoryRate > 0.0f);
            }

            // Depth (diving activity)
            final double depth = point.getDepth();
            if (depth >= 0.0) {
                depthDataPoints.add(new Entry(tsShorten, (float) -depth));
                hasDepthValues = hasDepthValues || (depth > 0.0);
            }

            // Temperature
            final double temperature = point.getTemperature();
            if (temperature > -273) {
                temperatureDataPoints.add(new Entry(tsShorten, (float) temperature));
                temperatureAccumulator.add(temperature);
                hasTemperatureValues = hasTemperatureValues || (temperature != 0.0);
            }

            // Distance
            final double distance = point.getDistance();
            if (distance >= 0.0) {
                distancePoints.add(new Entry(tsShorten, (float) distance));
                hasDistanceValues = hasDistanceValues || (distance > 0);
            }

            // Stride
            final float stride = point.getStride();
            if (stride >= 0.0f) {
                stridePoints.add(new Entry(tsShorten, stride));
                hasStrideValues = hasStrideValues || (stride > 0.0f);
            }

            // Body Energy
            final float bodyEnergy = point.getBodyEnergy();
            if (bodyEnergy >= 0.0f) {
                bodyEnergyPoints.add(new Entry(tsShorten, bodyEnergy));
                hasBodyEnergyValues = hasBodyEnergyValues || (bodyEnergy > 0.0f);
            }

            // Stamina
            final float stamina = point.getStamina();
            if (stamina >= 0.0f) {
                staminaPoints.add(new Entry(tsShorten, stamina));
                hasStaminaValues = hasStaminaValues || (stamina > 0.0f);
            }

            // Step Length
            final int stepLength = point.getStepLength();
            if (stepLength >= 0) {
                stepLengthPoints.add(new Entry(tsShorten, stepLength));
                hasStepLengthValues = hasStepLengthValues || (stepLength > 0);
            }

            // CNS Toxicity
            final float cnsToxicity = point.getCnsToxicity();
            if (cnsToxicity >= 0.0f) {
                cnsToxicityPoints.add(new Entry(tsShorten, cnsToxicity));
                hasCnsToxicityValues = hasCnsToxicityValues || (cnsToxicity > 0.0f);
            }

            // N2 Load
            final float n2Load = point.getN2Load();
            if (n2Load >= 0.0f) {
                n2LoadPoints.add(new Entry(tsShorten, n2Load));
                hasN2LoadValues = hasN2LoadValues || (n2Load > 0.0f);
            }
        }

        if (!heartRateDataPoints.isEmpty()) {
            charts.add(createHeartRateChart(context, heartRateDataPoints));
        }

        if (hasSpeedValues && !speedDataPoints.isEmpty()) {
            charts.add(createSpeedChart(context, activityKind, speedDataPoints));
        }

        if (hasCadenceValues && !cadenceDataPoints.isEmpty()) {
            charts.add(createCadenceChart(context, cycleUnit, cadenceDataPoints, cadenceAccumulator));
        }

        if (hasElevationValues && !elevationDataPoints.isEmpty()) {
            charts.add(createElevationChart(context, elevationDataPoints));
        }

        if (hasPowerValues && !powerDataPoints.isEmpty()) {
            charts.add(createPowerChart(context, powerDataPoints));
        }

        if (hasRespiratoryRateValues && !respiratoryRatePoints.isEmpty()) {
            charts.add(createRespiratoryRateChart(context, respiratoryRatePoints));
        }

        if (hasDepthValues && !depthDataPoints.isEmpty()) {
            charts.add(createDepthChart(context, depthDataPoints));
        }

        if (hasTemperatureValues && !temperatureDataPoints.isEmpty()) {
            charts.add(createTemperatureChart(context, temperatureDataPoints, temperatureAccumulator));
        }

        if (hasDistanceValues && !distancePoints.isEmpty()) {
            charts.add(createDistanceChart(context, distancePoints));
        }

        if (hasStrideValues && !stridePoints.isEmpty()) {
            charts.add(createStrideChart(context, stridePoints));
        }

        if (hasBodyEnergyValues && !bodyEnergyPoints.isEmpty()) {
            charts.add(createBodyEnergyChart(context, bodyEnergyPoints));
        }

        if (hasStaminaValues && !staminaPoints.isEmpty()) {
            charts.add(createStaminaChart(context, staminaPoints));
        }

        if (hasStepLengthValues && !stepLengthPoints.isEmpty()) {
            charts.add(createStepLengthChart(context, stepLengthPoints));
        }

        if (hasCnsToxicityValues && !cnsToxicityPoints.isEmpty()) {
            charts.add(createCnsToxicityChart(context, cnsToxicityPoints));
        }

        if (hasN2LoadValues && !n2LoadPoints.isEmpty()) {
            charts.add(createN2LoadChart(context, n2LoadPoints));
        }

        return charts;
    }

    private static WorkoutChart createElevationChart(final Context context,
                                                     final List<Entry> elevationDataPoints) {
        final String label = String.format("%s (%s)", context.getString(R.string.Elevation), getUnitString(context, UNIT_METERS));
        final LineDataSet dataset = createLineDataSet(context, elevationDataPoints, label, ContextCompat.getColor(context, R.color.chart_line_elevation));
        return new WorkoutChart(
                "elevation",
                context.getString(R.string.Elevation),
                ActivitySummaryEntries.GROUP_ELEVATION,
                new LineData(dataset),
                null,
                getUnitString(context, UNIT_METERS)
        );
    }

    private static WorkoutChart createHeartRateChart(final Context context,
                                                     final List<Entry> heartRateDataPoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.heart_rate), getUnitString(context, UNIT_BPM));
        final LineDataSet dataset = createLineDataSet(context, heartRateDataPoints, label, ContextCompat.getColor(context, R.color.chart_line_heart_rate));
        final ValueFormatter integerFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "heart_rate",
                context.getString(R.string.heart_rate),
                ActivitySummaryEntries.GROUP_HEART_RATE,
                new LineData(dataset),
                integerFormatter,
                getUnitString(context, UNIT_BPM)
        );
    }

    private static WorkoutChart createSpeedChart(final Context context,
                                                 final ActivityKind activityKind,
                                                 final List<Entry> speedDataPoints) {
        if (ActivityKind.isSwimActivity(activityKind)) {
            final String label = String.format("%s (%s)", context.getString(R.string.Pace), getUnitString(context, UNIT_MINUTES_PER_100_METERS));
            final LineDataSet dataset = createLineDataSet(context, speedDataPoints, label, ContextCompat.getColor(context, R.color.chart_line_speed));
            return new WorkoutChart(
                    "pace",
                    context.getString(R.string.Pace),
                    ActivitySummaryEntries.GROUP_SPEED,
                    new LineData(dataset),
                    new SpeedYLabelFormatter(UNIT_SECONDS_PER_100_METERS),
                    getUnitString(context, UNIT_MINUTES_PER_100_METERS)
            );
        } else if (ActivityKind.isPaceActivity(activityKind)) {
            final String label = String.format("%s (%s)", context.getString(R.string.Pace), getUnitString(context, UNIT_MINUTES_PER_KM));
            final LineDataSet dataset = createLineDataSet(context, speedDataPoints, label, ContextCompat.getColor(context, R.color.chart_line_speed));
            return new WorkoutChart(
                    "pace",
                    context.getString(R.string.Pace),
                    ActivitySummaryEntries.GROUP_SPEED,
                    new LineData(dataset),
                    new SpeedYLabelFormatter(UNIT_SECONDS_PER_KM),
                    getUnitString(context, UNIT_MINUTES_PER_KM)
            );
        } else {
            final String label = String.format("%s (%s)", context.getString(R.string.Speed), getUnitString(context, UNIT_KMPH));
            final LineDataSet dataset = createLineDataSet(context, speedDataPoints, label, ContextCompat.getColor(context, R.color.chart_line_speed));
            return new WorkoutChart(
                    "speed",
                    context.getString(R.string.Speed),
                    ActivitySummaryEntries.GROUP_SPEED,
                    new LineData(dataset),
                    new SpeedYLabelFormatter(UNIT_METERS_PER_SECOND),
                    getUnitString(context, UNIT_KMPH)
            );
        }
    }

    private static WorkoutChart createCadenceChart(final Context context,
                                                   final ActivityKind.CycleUnit cycleUnit,
                                                   final List<Entry> cadenceDataPoints,
                                                   final Accumulator cadenceAccumulator) {
        final String label = String.format("%s (%s)", context.getString(R.string.workout_cadence), getUnitString(context, getCadenceUnit(cycleUnit)));
        final ScatterDataSet dataset = createScatterDataSet(context, cadenceDataPoints, label, ContextCompat.getColor(context, R.color.chart_cadence_circle));
        final ValueFormatter integerFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        float xAxisMaximum = Math.max(
                (float) (cadenceAccumulator.getMax() + 30),
                (float) cadenceAccumulator.getAverage() * 2
        );

        return new WorkoutChart(
                "cadence",
                context.getString(R.string.workout_cadence),
                ActivitySummaryEntries.GROUP_CADENCE,
                new ScatterData(dataset),
                integerFormatter,
                getUnitString(context, UNIT_SPM),
                lineChart -> {
                    YAxis yAxisLeft = lineChart.getAxisLeft();
                    yAxisLeft.setAxisMinimum(0);
                    yAxisLeft.setAxisMaximum(xAxisMaximum);
                    YAxis yAxisRight = lineChart.getAxisRight();
                    yAxisRight.setAxisMinimum(0);
                    yAxisRight.setAxisMaximum(xAxisMaximum);
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    private static WorkoutChart createPowerChart(final Context context,
                                                 final List<Entry> powerDataPoints) {
        final String label = String.format("%s (%s)", context.getString(R.string.workout_power), getUnitString(context, UNIT_WATT));
        LineDataSet dataset = createLineDataSet(context, powerDataPoints, label, context.getResources().getColor(R.color.chart_line_power));
        final ValueFormatter integerFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart("power", context.getString(R.string.workout_power), ActivitySummaryEntries.GROUP_POWER, new LineData(dataset), integerFormatter, getUnitString(context, UNIT_WATT));
    }

    private static WorkoutChart createRespiratoryRateChart(final Context context,
                                                           final List<Entry> powerDataPoints) {
        final String label = String.format("%s (%s)", context.getString(R.string.respiratoryrate), getUnitString(context, UNIT_BREATHS_PER_MIN));
        LineDataSet dataset = createLineDataSet(context, powerDataPoints, label, context.getResources().getColor(R.color.respiratory_rate_color));
        final ValueFormatter integerFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "respiratory_rate",
                context.getString(R.string.respiratoryrate),
                ActivitySummaryEntries.GROUP_RESPIRATORY_RATE,
                new LineData(dataset),
                integerFormatter,
                getUnitString(context, UNIT_BREATHS_PER_MIN)
        );
    }

    private static WorkoutChart createDepthChart(final Context context,
                                                     final List<Entry> depthDataPoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.diving_depth), getUnitString(context, UNIT_METERS));
        final LineDataSet dataset = createLineDataSet(context, depthDataPoints, label, ContextCompat.getColor(context, R.color.chart_line_depth));
        final ValueFormatter integerFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "diving_depth",
                context.getString(R.string.diving_depth),
                ActivitySummaryEntries.GROUP_DIVING,
                new LineData(dataset),
                integerFormatter,
                getUnitString(context, UNIT_METERS)
        );
    }

    private static WorkoutChart createTemperatureChart(final Context context,
                                                       final List<Entry> temperatureDataPoints,
                                                       final Accumulator temperatureAccumulator) {
        final String label = String.format("%s(%s)", context.getString(R.string.menuitem_temperature), getUnitString(context, UNIT_CELSIUS));
        final LineDataSet dataset = createLineDataSet(context, temperatureDataPoints, label, ContextCompat.getColor(context, R.color.chart_line_heart_rate));
        final ValueFormatter integerFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "temperature",
                context.getString(R.string.menuitem_temperature),
                ActivitySummaryEntries.GROUP_TEMPERATURE,
                new LineData(dataset),
                integerFormatter,
                getUnitString(context, UNIT_CELSIUS),
                lineChart -> {
                    YAxis yAxisLeft = lineChart.getAxisLeft();
                    yAxisLeft.setAxisMinimum(0);
                    yAxisLeft.setAxisMaximum((float) Math.max(35, temperatureAccumulator.getMax() + 5));
                    YAxis yAxisRight = lineChart.getAxisRight();
                    yAxisRight.setAxisMinimum(0);
                    yAxisRight.setAxisMaximum((float) Math.max(35, temperatureAccumulator.getMax() + 5));
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    private static WorkoutChart createDistanceChart(final Context context,
                                                    final List<Entry> distancePoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.distance), getUnitString(context, UNIT_METERS));
        final LineDataSet dataset = createLineDataSet(context, distancePoints, label, ContextCompat.getColor(context, R.color.chart_line_distance));
        final ValueFormatter valueFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "chart_distance",
                context.getString(R.string.distance),
                ActivitySummaryEntries.GROUP_DISTANCE,
                new LineData(dataset),
                valueFormatter,
                getUnitString(context, UNIT_METERS)
        );
    }

    private static WorkoutChart createStrideChart(final Context context,
                                                  final List<Entry> stridePoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.stride), getUnitString(context, UNIT_MM));
        final LineDataSet dataset = createLineDataSet(context, stridePoints, label, ContextCompat.getColor(context, R.color.chart_line_stride));
        final ValueFormatter valueFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "chart_stride",
                context.getString(R.string.stride),
                ActivitySummaryEntries.GROUP_STEPS,
                new LineData(dataset),
                valueFormatter,
                getUnitString(context, UNIT_MM)
        );
    }

    private static WorkoutChart createBodyEnergyChart(final Context context,
                                                  final List<Entry> bodyEnergyPoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.body_energy), getUnitString(context, UNIT_PERCENTAGE));
        final LineDataSet dataset = createLineDataSet(context, bodyEnergyPoints, label, ContextCompat.getColor(context, R.color.chart_line_body_energy));
        final ValueFormatter valueFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "chart_body_energy",
                context.getString(R.string.body_energy),
                ActivitySummaryEntries.GROUP_TRAINING_EFFECT,
                new LineData(dataset),
                valueFormatter,
                getUnitString(context, UNIT_PERCENTAGE)
        );
    }

    private static WorkoutChart createStaminaChart(final Context context,
                                                      final List<Entry> staminaPoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.stamina), getUnitString(context, UNIT_PERCENTAGE));
        final LineDataSet dataset = createLineDataSet(context, staminaPoints, label, ContextCompat.getColor(context, R.color.chart_line_stamina));
        final ValueFormatter valueFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "chart_stamina",
                context.getString(R.string.stamina),
                ActivitySummaryEntries.GROUP_TRAINING_EFFECT,
                new LineData(dataset),
                valueFormatter,
                getUnitString(context, UNIT_PERCENTAGE)
        );
    }

    private static WorkoutChart createStepLengthChart(final Context context,
                                                  final List<Entry> stepLengthPoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.step_length), getUnitString(context, UNIT_MM));
        final LineDataSet dataset = createLineDataSet(context, stepLengthPoints, label, ContextCompat.getColor(context, R.color.chart_line_step_length));
        final ValueFormatter valueFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "chart_step_length",
                context.getString(R.string.step_length),
                ActivitySummaryEntries.GROUP_STEPS,
                new LineData(dataset),
                valueFormatter,
                getUnitString(context, UNIT_MM)
        );
    }

    private static WorkoutChart createCnsToxicityChart(final Context context,
                                                       final List<Entry> cnsToxicityPoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.diving_cns_toxicity), getUnitString(context, UNIT_PERCENTAGE));
        final LineDataSet dataset = createLineDataSet(context, cnsToxicityPoints, label, ContextCompat.getColor(context, R.color.chart_cns_toxicity));
        final ValueFormatter valueFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "chart_cns_toxicity",
                context.getString(R.string.diving_cns_toxicity),
                ActivitySummaryEntries.GROUP_DIVING,
                new LineData(dataset),
                valueFormatter,
                getUnitString(context, UNIT_PERCENTAGE)
        );
    }

    private static WorkoutChart createN2LoadChart(final Context context,
                                                       final List<Entry> n2LoadPoints) {
        final String label = String.format("%s(%s)", context.getString(R.string.diving_nitrogen_load), getUnitString(context, UNIT_PERCENTAGE));
        final LineDataSet dataset = createLineDataSet(context, n2LoadPoints, label, ContextCompat.getColor(context, R.color.chart_n2_load));
        final ValueFormatter valueFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        };
        return new WorkoutChart(
                "chart_n2_load",
                context.getString(R.string.diving_nitrogen_load),
                ActivitySummaryEntries.GROUP_DIVING,
                new LineData(dataset),
                valueFormatter,
                getUnitString(context, UNIT_PERCENTAGE)
        );
    }

    public static String getUnitString(final Context context, final String unit) {
        final int resId = context.getResources().getIdentifier(unit, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        return unit;
    }

    public static LineDataSet createLineDataSet(final Context context,
                                                final List<Entry> entities,
                                                final String label,
                                                final int color) {
        final LineDataSet dataSet = new LineDataSet(entities, label);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setCubicIntensity(0.05f);
        dataSet.setDrawCircles(false);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        dataSet.setColor(color);
        dataSet.setValueTextColor(GBApplication.getSecondaryTextColor(context));
        dataSet.setLineWidth(1.5f);
        dataSet.setHighlightLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        return dataSet;
    }

    public static ScatterDataSet createScatterDataSet(final Context context,
                                                      final List<Entry> entities,
                                                      final String label,
                                                      final int color) {
        final ScatterDataSet dataSet = new ScatterDataSet(entities, label);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        dataSet.setColor(color);
        dataSet.setValueTextColor(GBApplication.getSecondaryTextColor(context));
        dataSet.setHighlightLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeSize(10f);
        return dataSet;
    }

    public static String getCadenceUnit(final ActivityKind.CycleUnit unit) {
        return switch (unit) {
            case STROKES -> ActivitySummaryEntries.UNIT_STROKES_PER_MINUTE;
            case JUMPS -> ActivitySummaryEntries.UNIT_JUMPS_PER_MINUTE;
            case REPS -> ActivitySummaryEntries.UNIT_REPS_PER_MINUTE;
            case REVOLUTIONS -> ActivitySummaryEntries.UNIT_REVS_PER_MINUTE;
            default -> UNIT_SPM;
        };
    }
}
