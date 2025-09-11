package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;

import com.github.mikephil.charting.formatter.ValueFormatter;


import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;

public class SpeedYLabelFormatter extends ValueFormatter {
    String unit;
    private final WorkoutValueFormatter workoutValueFormatter = new WorkoutValueFormatter();

    public SpeedYLabelFormatter(String unit) {
        this.unit = unit;
    }

    @Override
    public String getFormattedValue(float value) {
        if(unit.equals(UNIT_SECONDS_PER_100_METERS)) {
            value = value > 0 ? Math.round(100.0 / value) : 0;
        } else if (unit.equals(UNIT_SECONDS_PER_KM)) {
            value = value > 0 ? Math.round((60 / (value * 3.6)) * 60) : 0;
        }
        return workoutValueFormatter.formatValue(value, unit, false);
    }
}