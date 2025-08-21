package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;

public class ActivitySummarySimpleEntry extends ActivitySummaryEntry {
    private final Object value;
    private final String unit;

    public ActivitySummarySimpleEntry(final Object value, final String unit) {
        this(null, value, unit);
    }

    public ActivitySummarySimpleEntry(final String group, final Object value, final String unit) {
        super(group);
        this.value = value;
        this.unit = unit;
    }

    public Object getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public int getColumnSpan() {
        return 1;
    }

    @Override
    public void populate(final String key, final LinearLayout linearLayout, final WorkoutValueFormatter workoutValueFormatter) {
        final Context context = linearLayout.getContext();

        // Value
        final TextView valueTextView = new TextView(context);
        valueTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTextView.setTextSize(20);
        valueTextView.setText(workoutValueFormatter.formatValue(value, unit));
        valueTextView.setTextColor(GBApplication.getTextColor(context));

        // Label
        final TextView labelTextView = new TextView(context);
        labelTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        labelTextView.setTextSize(12);
        labelTextView.setText(workoutValueFormatter.getStringResourceByName(key));

        if (getColumnSpan() == 1) {
            linearLayout.addView(valueTextView);
            linearLayout.addView(labelTextView);
        } else if (getColumnSpan() == 2) {
            // Label
            labelTextView.setTextSize(14);
            labelTextView.setMaxLines(1);
            labelTextView.setEllipsize(TextUtils.TruncateAt.END);

            // Value
            valueTextView.setTextSize(16);
            valueTextView.setTypeface(Typeface.create(valueTextView.getTypeface(), Typeface.BOLD));
            valueTextView.setGravity(Gravity.END);

            final View spacer = new View(context);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));

            // Layout for the labels, so the value is at the right
            final LinearLayout labelsLinearLayout = new LinearLayout(context);
            labelsLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            labelsLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            labelsLinearLayout.addView(labelTextView);
            labelsLinearLayout.addView(spacer);
            labelsLinearLayout.addView(valueTextView);

            linearLayout.addView(labelsLinearLayout);
        } else {
            throw new IllegalArgumentException("Invalid columnSpan " + getColumnSpan());
        }
    }
}
