package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BREATHS_PER_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KMPH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_KM;
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
        final List<Entry> heartRateDataPoints = new ArrayList<>();
        final List<Entry> speedDataPoints = new ArrayList<>();
        final List<Entry> cadenceDataPoints = new ArrayList<>();
        final List<Entry> elevationDataPoints = new ArrayList<>();
        final List<Entry> powerDataPoints = new ArrayList<>();
        final List<Entry> respiratoryRatePoints = new ArrayList<>();
        boolean hasSpeedValues = false;
        boolean hasCadenceValues = false;
        boolean hasElevationValues = false;
        final Accumulator cadenceAccumulator = new Accumulator();

        for (int i = 0; i <= activityPoints.size() - 1; i++) {
            final ActivityPoint point = activityPoints.get(i);
            final long tsShorten = tsTranslation.shorten((int) point.getTime().getTime());

            // HR
            if (point.getHeartRate() > 0) {
                heartRateDataPoints.add(new Entry(tsShorten, point.getHeartRate()));
            }

            // Elevation
            if (point.getLocation() != null && point.getLocation().getAltitude() != GPSCoordinate.UNKNOWN_ALTITUDE) {
                elevationDataPoints.add(new Entry(tsShorten, (float) point.getLocation().getAltitude()));
                if (point.getLocation().getAltitude() != 0) {
                    // Some devices provide all points at zero
                    hasElevationValues = true;
                }
            }

            // Speed
            speedDataPoints.add(new Entry(tsShorten, point.getSpeed()));
            if (!hasSpeedValues && point.getSpeed() > 0) {
                hasSpeedValues = true;
            }

            // Cadence
            cadenceDataPoints.add(new Entry(tsShorten, point.getCadence()));
            cadenceAccumulator.add(point.getCadence());
            if (!hasCadenceValues && point.getCadence() > 0) {
                hasCadenceValues = true;
            }
            if (point.getPower() >= 0) {
                powerDataPoints.add(new Entry(tsShorten, (float) point.getPower()));
            }
            if (point.getRespiratoryRate() >= 0) {
                respiratoryRatePoints.add(new Entry(tsShorten, point.getRespiratoryRate()));
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

        if (!powerDataPoints.isEmpty()) {
            charts.add(createPowerChart(context, powerDataPoints));
        }

        if (!respiratoryRatePoints.isEmpty()) {
            charts.add(createRespiratoryRateChart(context, respiratoryRatePoints));
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

    public static String getUnitString(final Context context, final String unit) {
        final int resId = context.getResources().getIdentifier(unit, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        return "";
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
