package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;

public class ActivitySummarySimpleEntry extends ActivitySummaryEntry {
    private final Object value;
    private final String unit;

    public ActivitySummarySimpleEntry(final Object value, final String unit) {
        this(null, value, unit);
    }

    public ActivitySummarySimpleEntry(final String group, final Object value, final String unit) {
        this(group, value, unit, 1);
    }

    public ActivitySummarySimpleEntry(final String group, final Object value, final String unit, final int columnSpan) {
        super(group);
        this.value = value;
        this.unit = unit;
        this.columnSpan = columnSpan;
    }

    public Object getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public void populate(final String key, final LinearLayout linearLayout, final WorkoutValueFormatter workoutValueFormatter) {
        final Context context = linearLayout.getContext();

        // Value
        final TextView valueTextView = new TextView(context);
        valueTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTextView.setTextSize(20);
        valueTextView.setText(workoutValueFormatter.formatValue(value, unit));

        // Label
        final TextView labelTextView = new TextView(context);
        labelTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        labelTextView.setTextSize(12);
        labelTextView.setText(workoutValueFormatter.getStringResourceByName(key));

        if (columnSpan == 1) {
            linearLayout.addView(valueTextView);
            linearLayout.addView(labelTextView);
        } else if (columnSpan == 2) {
            // Label
            labelTextView.setTextSize(14);
            labelTextView.setMaxLines(1);
            labelTextView.setEllipsize(TextUtils.TruncateAt.END);

            // Value
            valueTextView.setTextSize(16);
            valueTextView.setTypeface(Typeface.create(valueTextView.getTypeface(), Typeface.BOLD));
            valueTextView.setGravity(Gravity.END);

            // Layout for the labels, so the value is at the right
            final LinearLayout labelsLinearLayout = new LinearLayout(context);
            labelsLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            labelsLinearLayout.addView(labelTextView);
            labelsLinearLayout.addView(valueTextView);

            linearLayout.addView(labelsLinearLayout);
        } else {
            throw new IllegalArgumentException("Invalid columnSpan " + columnSpan);
        }
    }
}
