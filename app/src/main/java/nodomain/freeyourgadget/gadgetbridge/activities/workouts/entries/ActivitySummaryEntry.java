package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;

public abstract class ActivitySummaryEntry implements Cloneable {
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

    @NonNull
    @Override
    public ActivitySummaryEntry clone() {
        try {
            return (ActivitySummaryEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
