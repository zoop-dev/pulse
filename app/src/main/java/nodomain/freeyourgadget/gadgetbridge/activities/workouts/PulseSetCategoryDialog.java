/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.activities.workouts;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryTableRowEntry;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.workout.Workout;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.ExerciseCategory;
import nodomain.freeyourgadget.gadgetbridge.util.PulseSetOverrides;

public final class PulseSetCategoryDialog {
    private PulseSetCategoryDialog() {
    }

    public static boolean hasSets(@Nullable final ActivitySummaryData data) {
        if (data == null) {
            return false;
        }
        for (final String key : data.getKeys()) {
            if (key.matches("set_\\d+")) {
                return true;
            }
        }
        return false;
    }

    public static void show(final Context context, final Workout workout, final Runnable onSaved) {
        final ActivitySummaryData data = workout.getData();
        final long workoutId = workout.getSummary().getId();

        final List<Integer> setNumbers = new ArrayList<>();
        for (final String key : data.getKeys()) {
            if (key.matches("set_\\d+")) {
                try {
                    setNumbers.add(Integer.parseInt(key.substring("set_".length())));
                } catch (final NumberFormatException ignored) {
                }
            }
        }
        Collections.sort(setNumbers);
        if (setNumbers.isEmpty()) {
            return;
        }

        final int categoryColumn = PulseSetOverrides.findCategoryColumn(data);
        final ExerciseCategory[] categories = ExerciseCategory.values();

        final List<String> options = new ArrayList<>();
        options.add(context.getString(R.string.pulse_set_category_keep));
        for (final ExerciseCategory category : categories) {
            options.add(context.getString(category.label));
        }

        final LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        final int pad = dp(context, 20);
        root.setPadding(pad, dp(context, 8), pad, dp(context, 8));

        final List<Spinner> spinners = new ArrayList<>();
        for (final int setNumber : setNumbers) {
            final TextView label = new TextView(context);
            String title = context.getString(R.string.pulse_set_category_set, setNumber);
            final String current = currentLabel(context, data, setNumber, categoryColumn);
            if (current != null) {
                title += "  ·  " + current;
            }
            label.setText(title);
            label.setTextSize(13);
            label.setPadding(0, dp(context, 12), 0, dp(context, 2));
            root.addView(label);

            final Spinner spinner = new Spinner(context);
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            final Integer override = PulseSetOverrides.getOverride(workoutId, setNumber);
            if (override != null) {
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].num == override) {
                        spinner.setSelection(i + 1);
                        break;
                    }
                }
            }
            spinner.setTag(setNumber);
            spinners.add(spinner);
            root.addView(spinner);
        }

        final ScrollView scroll = new ScrollView(context);
        scroll.addView(root);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.pulse_set_category_title)
                .setView(scroll)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    for (final Spinner spinner : spinners) {
                        final int setNumber = (Integer) spinner.getTag();
                        final int pos = spinner.getSelectedItemPosition();
                        if (pos <= 0) {
                            PulseSetOverrides.setOverride(workoutId, setNumber, null);
                        } else {
                            PulseSetOverrides.setOverride(workoutId, setNumber, categories[pos - 1].num);
                        }
                    }
                    onSaved.run();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Nullable
    private static String currentLabel(final Context context, final ActivitySummaryData data,
                                       final int setNumber, final int categoryColumn) {
        if (categoryColumn < 0) {
            return null;
        }
        final ActivitySummaryEntry row = data.get("set_" + setNumber);
        if (!(row instanceof ActivitySummaryTableRowEntry)) {
            return null;
        }
        final ActivitySummaryTableRowEntry tableRow = (ActivitySummaryTableRowEntry) row;
        if (categoryColumn >= tableRow.getColumns().size()) {
            return null;
        }
        final Object value = tableRow.getColumns().get(categoryColumn).value();
        return value instanceof String ? (String) value : null;
    }

    private static int dp(final Context context, final int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }
}
