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
package nodomain.freeyourgadget.gadgetbridge.activities;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_ACTIVETIME_MINUTES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_CALORIES_BURNT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_GOAL_FAT_BURN_TIME_MINUTES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_GOAL_STANDING_TIME_HOURS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_GOAL_WEIGHT_KG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_SLEEP_DURATION_MINUTES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_STEPS_GOAL;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;

/** Pulse: daily goals on their own screen. */
public class PulseGoalsActivity extends AbstractSettingsActivityV2 {
    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new PulseGoalsFragment();
    }

    public static class PulseGoalsFragment extends AbstractPreferenceFragment {
        private static final String[] GOAL_KEYS = {
                PREF_USER_STEPS_GOAL, PREF_USER_SLEEP_DURATION_MINUTES, PREF_USER_ACTIVETIME_MINUTES,
                PREF_USER_CALORIES_BURNT, PREF_USER_DISTANCE_METERS, PREF_USER_GOAL_STANDING_TIME_HOURS,
                PREF_USER_GOAL_FAT_BURN_TIME_MINUTES, PREF_USER_GOAL_WEIGHT_KG
        };

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.pulse_goals, rootKey);

            // Sleep goal: a custom hours+minutes picker that persists total minutes.
            final Preference sleep = findPreference(PREF_USER_SLEEP_DURATION_MINUTES);
            if (sleep != null) {
                sleep.setSummary(formatMinutes(getSleepGoalMinutes()));
                sleep.setOnPreferenceClickListener(p -> {
                    showSleepGoalPicker(sleep);
                    return true;
                });
            }

            for (final String key : GOAL_KEYS) {
                if (PREF_USER_SLEEP_DURATION_MINUTES.equals(key)) {
                    continue;
                }
                setInputTypeFor(key, InputType.TYPE_CLASS_NUMBER);
                final Preference pref = findPreference(key);
                if (pref != null) {
                    pref.setOnPreferenceChangeListener((preference, newVal) -> {
                        GBApplication.deviceService().onSendConfiguration(key);
                        LocalBroadcastManager.getInstance(requireActivity().getApplicationContext())
                                .sendBroadcast(new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST));
                        return true;
                    });
                }
            }
        }

        private int getSleepGoalMinutes() {
            return GBApplication.getPrefs().getInt(PREF_USER_SLEEP_DURATION_MINUTES, 480);
        }

        private String formatMinutes(final int total) {
            return String.format(java.util.Locale.getDefault(), "%dh %02dm", total / 60, total % 60);
        }

        private void showSleepGoalPicker(final Preference pref) {
            final android.content.Context ctx = requireContext();
            final int current = getSleepGoalMinutes();

            final android.widget.NumberPicker hours = new android.widget.NumberPicker(ctx);
            hours.setMinValue(0);
            hours.setMaxValue(14);
            hours.setValue(current / 60);
            final android.widget.NumberPicker minutes = new android.widget.NumberPicker(ctx);
            minutes.setMinValue(0);
            minutes.setMaxValue(59);
            minutes.setValue(current % 60);

            final float d = ctx.getResources().getDisplayMetrics().density;
            final android.widget.LinearLayout row = new android.widget.LinearLayout(ctx);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER);
            row.setPadding((int) (16 * d), (int) (16 * d), (int) (16 * d), (int) (16 * d));
            row.addView(labeled(ctx, hours, "h", d));
            row.addView(labeled(ctx, minutes, "m", d));

            new androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle(R.string.pulse_sleep_goal)
                    .setView(row)
                    .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                        final int total = hours.getValue() * 60 + minutes.getValue();
                        GBApplication.getPrefs().getPreferences().edit()
                                .putString(PREF_USER_SLEEP_DURATION_MINUTES, String.valueOf(total)).apply();
                        pref.setSummary(formatMinutes(total));
                        GBApplication.deviceService().onSendConfiguration(PREF_USER_SLEEP_DURATION_MINUTES);
                        LocalBroadcastManager.getInstance(ctx.getApplicationContext())
                                .sendBroadcast(new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST));
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }

        private android.view.View labeled(final android.content.Context ctx, final android.widget.NumberPicker picker,
                                          final String unit, final float d) {
            final android.widget.LinearLayout box = new android.widget.LinearLayout(ctx);
            box.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            box.setGravity(android.view.Gravity.CENTER_VERTICAL);
            box.setPadding((int) (8 * d), 0, (int) (8 * d), 0);
            box.addView(picker);
            final android.widget.TextView label = new android.widget.TextView(ctx);
            label.setText(unit);
            label.setTextSize(18);
            label.setPadding((int) (6 * d), 0, 0, 0);
            box.addView(label);
            return box;
        }
    }
}
