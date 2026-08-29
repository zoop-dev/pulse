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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryTableRowEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryValue;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.ExerciseCategory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionExerciseCategory;

public final class PulseSetOverrides {
    private static final Logger LOG = LoggerFactory.getLogger(PulseSetOverrides.class);
    private static final String PREF_KEY = "pulse_set_category_overrides";
    private static final String HEADER_KEY = "sets_header";
    private static final String SET_KEY_PREFIX = "set_";
    private static final String CATEGORY_COLUMN = "category";

    private PulseSetOverrides() {
    }

    private static JSONObject readRoot() {
        try {
            final String raw = GBApplication.getPrefs().getString(PREF_KEY, "{}");
            return new JSONObject(raw == null || raw.isEmpty() ? "{}" : raw);
        } catch (final Exception e) {
            return new JSONObject();
        }
    }

    private static void writeRoot(final JSONObject root) {
        GBApplication.getPrefs().getPreferences().edit().putString(PREF_KEY, root.toString()).apply();
    }

    public static Map<Integer, Integer> getOverrides(final long workoutId) {
        final Map<Integer, Integer> out = new HashMap<>();
        final JSONObject forWorkout = readRoot().optJSONObject(String.valueOf(workoutId));
        if (forWorkout != null) {
            for (final Iterator<String> it = forWorkout.keys(); it.hasNext(); ) {
                final String key = it.next();
                try {
                    out.put(Integer.parseInt(key), forWorkout.getInt(key));
                } catch (final Exception ignored) {
                }
            }
        }
        return out;
    }

    @Nullable
    public static Integer getOverride(final long workoutId, final int setNumber) {
        return getOverrides(workoutId).get(setNumber);
    }

    public static void setOverride(final long workoutId, final int setNumber, @Nullable final Integer categoryNum) {
        final JSONObject root = readRoot();
        try {
            final String workoutKey = String.valueOf(workoutId);
            JSONObject forWorkout = root.optJSONObject(workoutKey);
            if (forWorkout == null) {
                forWorkout = new JSONObject();
            }
            if (categoryNum == null) {
                forWorkout.remove(String.valueOf(setNumber));
            } else {
                forWorkout.put(String.valueOf(setNumber), (int) categoryNum);
            }
            if (forWorkout.length() == 0) {
                root.remove(workoutKey);
            } else {
                root.put(workoutKey, forWorkout);
            }
            writeRoot(root);
        } catch (final Exception e) {
            LOG.warn("Pulse: failed to store set category override", e);
        }
    }

    public static void apply(final Context context, final long workoutId, @Nullable final ActivitySummaryData data) {
        if (data == null) {
            return;
        }
        final Map<Integer, Integer> overrides = getOverrides(workoutId);
        if (overrides.isEmpty()) {
            return;
        }

        final int categoryColumn = findCategoryColumn(data);
        if (categoryColumn < 0) {
            return;
        }

        for (final Map.Entry<Integer, Integer> entry : overrides.entrySet()) {
            final ActivitySummaryEntry row = data.get(SET_KEY_PREFIX + entry.getKey());
            if (!(row instanceof ActivitySummaryTableRowEntry)) {
                continue;
            }
            final ActivitySummaryTableRowEntry tableRow = (ActivitySummaryTableRowEntry) row;
            if (categoryColumn >= tableRow.getColumns().size()) {
                continue;
            }
            final ExerciseCategory category = FieldDefinitionExerciseCategory.fromId(entry.getValue());
            tableRow.setColumn(categoryColumn,
                    new ActivitySummaryValue(context.getString(category.label), ActivitySummaryEntries.UNIT_NONE));
        }
    }

    public static int findCategoryColumn(final ActivitySummaryData data) {
        final ActivitySummaryEntry header = data.get(HEADER_KEY);
        if (!(header instanceof ActivitySummaryTableRowEntry)) {
            return -1;
        }
        final List<ActivitySummaryValue> columns = ((ActivitySummaryTableRowEntry) header).getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (CATEGORY_COLUMN.equals(columns.get(i).value())) {
                return i;
            }
        }
        return -1;
    }
}
