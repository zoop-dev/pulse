package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;

import android.content.Context;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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

public class DefaultWorkoutCharts {
    public static List<WorkoutChart> buildDefaultCharts(final Context context,
                                                        final List<ActivityPoint> activityPoints,
                                                        final ActivityKind activityKind) {
        final ActivityKind.CycleUnit cycleUnit = ActivityKind.getCycleUnit(activityKind);
        final List<WorkoutChart> charts = new LinkedList<>();
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        final List<Entry> heartRateDataPoints = new ArrayList<>();
        final List<Entry> speedDataPoints = new ArrayList<>();
        final List<Entry> cadenceDataPoints = new ArrayList<>();
        final List<Entry> elevationDataPoints = new ArrayList<>();
        boolean hasSpeedValues = false;
        boolean hasCadenceValues = false;

        for (int i = 0; i <= activityPoints.size() - 1; i++) {
            final ActivityPoint point = activityPoints.get(i);
            final long tsShorten = tsTranslation.shorten((int) point.getTime().getTime());
            if (point.getHeartRate() > 0) {
                heartRateDataPoints.add(new Entry(tsShorten, point.getHeartRate()));
            }
            if (point.getLocation() != null && point.getLocation().getAltitude() != GPSCoordinate.UNKNOWN_ALTITUDE) {
                elevationDataPoints.add(new Entry(tsShorten, (float) point.getLocation().getAltitude()));
            }
            speedDataPoints.add(new Entry(tsShorten, point.getSpeed()));
            if (!hasSpeedValues && point.getSpeed() > 0) {
                hasSpeedValues = true;
            }
            cadenceDataPoints.add(new Entry(tsShorten, point.getCadence()));
            if (!hasCadenceValues && point.getCadence() > 0) {
                hasCadenceValues = true;
            }
        }

        if (!heartRateDataPoints.isEmpty()) {
            final String label = String.format("%s(%s)", context.getString(R.string.heart_rate), getUnitString(context, UNIT_BPM));
            final LineDataSet dataset = createDataSet(context, heartRateDataPoints, label, context.getResources().getColor(R.color.chart_line_heart_rate));
            ValueFormatter integerFormatter = new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            };
            charts.add(new WorkoutChart("heart_rate", context.getString(R.string.heart_rate), ActivitySummaryEntries.GROUP_HEART_RATE, new LineData(dataset), integerFormatter, getUnitString(context, UNIT_BPM)));
        }

        if (hasSpeedValues && !speedDataPoints.isEmpty()) {
            if (ActivityKind.isPaceActivity(activityKind)) {
                final String label = String.format("%s (%s)", context.getString(R.string.Pace), getUnitString(context, UNIT_MINUTES_PER_KM));
                final LineDataSet dataset = createDataSet(context, speedDataPoints, label, context.getResources().getColor(R.color.chart_line_speed));
                charts.add(new WorkoutChart("pace", context.getString(R.string.Pace), ActivitySummaryEntries.GROUP_SPEED, new LineData(dataset), new SpeedYLabelFormatter(UNIT_MINUTES_PER_KM), getUnitString(context, UNIT_MINUTES_PER_KM)));
            } else {
                final String label = String.format("%s (%s)", context.getString(R.string.Speed), getUnitString(context, UNIT_METERS_PER_SECOND));
                final LineDataSet dataset = createDataSet(context, speedDataPoints, label, context.getResources().getColor(R.color.chart_line_speed));
                charts.add(new WorkoutChart("speed", context.getString(R.string.Speed), ActivitySummaryEntries.GROUP_SPEED, new LineData(dataset), new SpeedYLabelFormatter(UNIT_METERS_PER_SECOND), getUnitString(context, UNIT_METERS_PER_SECOND)));
            }
        }

        if (hasCadenceValues && !cadenceDataPoints.isEmpty()) {
            final String label = String.format("%s (%s)", context.getString(R.string.workout_cadence), getUnitString(context, getCadenceUnit(cycleUnit)));
            final LineDataSet dataset = createDataSet(context, cadenceDataPoints, label, context.getResources().getColor(R.color.chart_line_speed));
            final ValueFormatter integerFormatter = new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            };
            charts.add(new WorkoutChart("cadence", context.getString(R.string.workout_cadence), ActivitySummaryEntries.GROUP_CADENCE, new LineData(dataset), integerFormatter));
        }

        if (!elevationDataPoints.isEmpty()) {
            final String label = String.format("%s (%s)", context.getString(R.string.Elevation), getUnitString(context, UNIT_METERS));
            LineDataSet dataset = createDataSet(context, elevationDataPoints, label, context.getResources().getColor(R.color.chart_line_elevation));
            charts.add(new WorkoutChart("elevation", context.getString(R.string.Elevation), ActivitySummaryEntries.GROUP_ELEVATION, new LineData(dataset), null, getUnitString(context, UNIT_METERS)));
        }

        return charts;
    }

    public static String getUnitString(final Context context, final String unit) {
        final int resId = context.getResources().getIdentifier(unit, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        return "";
    }

    public static LineDataSet createDataSet(final Context context,
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

    public static String getCadenceUnit(final ActivityKind.CycleUnit unit) {
        return switch (unit) {
            case STROKES -> ActivitySummaryEntries.UNIT_STROKES_PER_MINUTE;
            case JUMPS -> ActivitySummaryEntries.UNIT_JUMPS_PER_MINUTE;
            case REPS -> ActivitySummaryEntries.UNIT_REPS_PER_MINUTE;
            case REVOLUTIONS -> ActivitySummaryEntries.UNIT_REVS_PER_MINUTE;
            default -> ActivitySummaryEntries.UNIT_SPM;
        };
    }
}
