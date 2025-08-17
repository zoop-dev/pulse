package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;

import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

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
import nodomain.freeyourgadget.gadgetbridge.model.workout.WorkoutChart;

public class DefaultWorkoutCharts {
    public static List<WorkoutChart> buildDefaultCharts(final Context context,
                                                        final List<ActivityPoint> activityPoints,
                                                        final ActivityKind activityKind) {
        final List<WorkoutChart> charts = new LinkedList<>();
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        final List<Entry> heartRateDataPoints = new ArrayList<>();
        final List<Entry> speedDataPoints = new ArrayList<>();
        final List<Entry> elevationDataPoints = new ArrayList<>();
        boolean hasSpeedValues = false;

        for (int i = 0; i <= activityPoints.size() - 1; i++) {
            final ActivityPoint point = activityPoints.get(i);
            final long tsShorten = tsTranslation.shorten((int) point.getTime().getTime());
            if (point.getHeartRate() > 0) {
                heartRateDataPoints.add(new Entry(tsShorten, point.getHeartRate()));
            }
            if (point.getLocation() != null) {
                elevationDataPoints.add(new Entry(tsShorten, (float) point.getLocation().getAltitude()));
            }
            speedDataPoints.add(new Entry(tsShorten, point.getSpeed()));
            if (!hasSpeedValues && point.getSpeed() > 0) {
                hasSpeedValues = true;
            }
        }

        if (!heartRateDataPoints.isEmpty()) {
            final String label = String.format("%s(%s)", context.getString(R.string.heart_rate), getUnitString(context, UNIT_BPM));
            final LineDataSet dataset = createDataSet(context, heartRateDataPoints, label, Color.RED);
            charts.add(new WorkoutChart(context.getString(R.string.heart_rate), ActivitySummaryEntries.GROUP_HEART_RATE, new LineData(dataset)));
        }

        if (hasSpeedValues && !speedDataPoints.isEmpty()) {
            if (ActivityKind.isPaceActivity(activityKind)) {
                final String label = String.format("%s (%s)", context.getString(R.string.Pace), getUnitString(context, UNIT_SECONDS_PER_KM));
                final LineDataSet dataset = createDataSet(context, speedDataPoints, label, Color.BLUE);
                charts.add(new WorkoutChart(context.getString(R.string.Pace), ActivitySummaryEntries.GROUP_SPEED, new LineData(dataset), new SpeedYLabelFormatter(UNIT_SECONDS_PER_KM)));
            } else {
                final String label = String.format("%s (%s)", context.getString(R.string.Speed), getUnitString(context, UNIT_METERS_PER_SECOND));
                final LineDataSet dataset = createDataSet(context, speedDataPoints, label, Color.BLUE);
                charts.add(new WorkoutChart(context.getString(R.string.Speed), ActivitySummaryEntries.GROUP_SPEED, new LineData(dataset), new SpeedYLabelFormatter(UNIT_METERS_PER_SECOND)));
            }
        }

        if (!elevationDataPoints.isEmpty()) {
            final String label = String.format("%s (%s)", context.getString(R.string.Elevation), getUnitString(context, UNIT_METERS));
            LineDataSet dataset = createDataSet(context, elevationDataPoints, label, Color.GREEN);
            charts.add(new WorkoutChart(context.getString(R.string.Elevation), ActivitySummaryEntries.GROUP_ELEVATION, new LineData(dataset)));
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
}
