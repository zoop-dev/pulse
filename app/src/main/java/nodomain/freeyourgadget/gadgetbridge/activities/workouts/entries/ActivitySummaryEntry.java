package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries;

import android.widget.LinearLayout;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;

public abstract class ActivitySummaryEntry {
    private String group;

    protected int columnSpan;

    public ActivitySummaryEntry(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getColumnSpan() {
        return columnSpan;
    }

    public void setColumnSpan(int columnSpan) {
        this.columnSpan = columnSpan;
    }

    public abstract void populate(final String key,
                                  final LinearLayout linearLayout,
                                  final WorkoutValueFormatter workoutValueFormatter);
}
