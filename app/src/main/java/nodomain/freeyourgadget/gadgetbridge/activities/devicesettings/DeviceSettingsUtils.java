/*  Copyright (C) 2023-2024 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public final class DeviceSettingsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceSettingsUtils.class);

    private DeviceSettingsUtils() {
        // utility class
    }

    /**
     * Returns the preference key where to save the list of possible value for a preference, comma-separated.
     */
    public static String getPrefPossibleValuesKey(final CharSequence key) {
        return String.format(Locale.ROOT, "%s_possible_values", key);
    }

    /**
     * Returns the preference key where to save the list of entry labels for a preference, comma-separated.
     */
    public static String getPrefPossibleValueLabelsKey(final CharSequence key) {
        return String.format(Locale.ROOT, "%s_possible_value_labels", key);
    }

    /**
     * Returns the preference key where to that a config was reported as supported (boolean).
     */
    public static String getPrefKnownConfig(final CharSequence key) {
        return String.format(Locale.ROOT, "%s_is_known", key);
    }

    /**
     * Populates a list preference, or hides it if no known supported values are known.
     * @noinspection ConstantValue
     */
    public static void populateOrHideListPreference(final CharSequence prefKey,
                                                    final DeviceSpecificSettingsHandler handler,
                                                    final Prefs prefs) {
        final Preference pref = handler.findPreference(prefKey);
        if (pref == null) {
            return;
        }

        // Get the list of possible values for this preference, as reported by the band
        final List<String> possibleValues = prefs.getList(getPrefPossibleValuesKey(prefKey), null);
        if (possibleValues == null || possibleValues.isEmpty()) {
            // The band hasn't reported this setting, so we don't know the possible values.
            // Hide it
            pref.setVisible(false);

            return;
        }

        final CharSequence[] originalEntries;
        final CharSequence[] originalValues;

        if (pref instanceof ListPreference) {
            originalEntries = ((ListPreference) pref).getEntries();
            originalValues = ((ListPreference) pref).getEntryValues();
        } else if (pref instanceof MultiSelectListPreference) {
            originalEntries = ((MultiSelectListPreference) pref).getEntries();
            originalValues = ((MultiSelectListPreference) pref).getEntryValues();
        } else {
            LOG.error("Unknown list pref class {}", pref.getClass().getName());
            return;
        }

        final Map<CharSequence, CharSequence> entryNames = new HashMap<>();
        final List<String> knownLabels = prefs.getList(getPrefPossibleValueLabelsKey(prefKey), null);
        if (knownLabels != null) {
            // We got some known labels from the watch
            if (knownLabels.size() != possibleValues.size()) {
                LOG.warn(
                        "Number of possible values ({}) and labels ({}) for {} differs - this should never happen",
                        possibleValues.size(),
                        knownLabels.size(),
                        prefKey
                );

                // Abort and hide preference - we can't safely recover from this
                pref.setVisible(false);
                return;
            }

            for (int i = 0; i < knownLabels.size(); i++) {
                entryNames.put(possibleValues.get(i), knownLabels.get(i));
            }
        } else {
            for (int i = 0; i < originalEntries.length; i++) {
                entryNames.put(originalValues[i], originalEntries[i]);
            }
        }

        final CharSequence[] entries = new CharSequence[possibleValues.size()];
        final CharSequence[] values = new CharSequence[possibleValues.size()];
        for (int i = 0; i < possibleValues.size(); i++) {
            final String possibleValue = possibleValues.get(i);
            final CharSequence knownLabel = entryNames.get(possibleValue);

            entries[i] = Objects.requireNonNullElseGet(
                    knownLabel,
                    () -> handler.getContext().getString(R.string.menuitem_unknown_app, possibleValue)
            );
            values[i] = possibleValue;
        }

        if (pref instanceof ListPreference) {
            ((ListPreference) pref).setEntries(entries);
            ((ListPreference) pref).setEntryValues(values);
        } else if (pref instanceof MultiSelectListPreference) {
            ((MultiSelectListPreference) pref).setEntries(entries);
            ((MultiSelectListPreference) pref).setEntryValues(values);
        }
    }

    /**
     * Hides the the prefToHide preference if none of the preferences in the preferences list are
     * visible.
     */
    public static void hidePrefIfNoneVisible(final DeviceSpecificSettingsHandler handler,
                                             final String prefToHide,
                                             final Iterable<String> subPrefs) {
        final Preference pref = handler.findPreference(prefToHide);
        if (pref == null) {
            return;
        }

        for (final String subPrefKey : subPrefs) {
            final Preference subPref = handler.findPreference(subPrefKey);
            if (subPref == null) {
                continue;
            }
            if (subPref.isVisible()) {
                // At least one preference is visible
                return;
            }
        }

        // No preference was visible, hide
        pref.setVisible(false);
    }

    public static void enforceMinMax(final EditTextPreference pref, final int minValue, final int maxValue) {
        if (minValue >= maxValue) {
            LOG.warn("Invalid min/max values for {}: {}/{}", pref.getKey(), minValue, maxValue);
            return;
        }

        pref.setOnBindEditTextListener(p -> {
            p.setInputType(InputType.TYPE_CLASS_NUMBER);
            p.setFilters(new InputFilter[]{new MinMaxInputFilter(minValue, maxValue)});
            p.setSelection(p.getText().length());
        });
    }

    public static void sortListPreference(final ListPreference listPreference, final boolean keepFirst) {
        final CharSequence[] entries = listPreference.getEntries();
        final CharSequence[] entryValues = listPreference.getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            LOG.warn("Invalid entries or values to sort");
            return;
        }

        final int length = entries.length;
        final String[][] combined = new String[length][2];

        for (int i = 0; i < length; i++) {
            combined[i][0] = entries[i].toString();
            combined[i][1] = entryValues[i].toString();
        }

        // Sort, keeping "the first" at the top
        Arrays.sort(combined, keepFirst ? 1 : 0, length, Comparator.comparing(o -> o[0]));

        // Reassign sorted values
        for (int i = 0; i < length; i++) {
            entries[i] = combined[i][0];
            entryValues[i] = combined[i][1];
        }

        listPreference.setEntries(entries);
        listPreference.setEntryValues(entryValues);
    }

    public static final class MinMaxInputFilter implements InputFilter {
        private final int min;
        private final int max;

        public MinMaxInputFilter(final int min, final int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                final int input = Integer.parseInt(dest.toString() + source.toString());
                if (input >= min && input <= max) {
                    return null;
                }
            } catch (final NumberFormatException ignored) {
            }
            return "";
        }
    }

    public static void addConfirmablePreferenceHandlerFor(final DeviceSpecificSettingsHandler handler,
                                                          final String preferenceKey,
                                                          final int alertMessage) {
        final Preference pref = handler.findPreference(preferenceKey);
        if (pref == null) {
            return;
        }
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            final Context context = handler.getContext();
            final CharSequence preferenceName = pref.getTitle();
            final String title = context.getString(R.string.earfun_change_confirm_title, preferenceName);
            final String message = context.getString(alertMessage);

            new MaterialAlertDialogBuilder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(handler.getContext().getString(R.string.ok), (dialog, which) -> {
                        if (pref instanceof EditTextPreference) {
                            ((EditTextPreference) pref).setText(newValue.toString());
                        } else if (pref instanceof ListPreference) {
                            ((ListPreference) pref).setValue(newValue.toString());
                        } else if (pref instanceof SwitchPreferenceCompat) {
                            ((SwitchPreferenceCompat) pref).setChecked((Boolean) newValue);
                        } else {
                            LOG.error("Unsupported preference type {} for confirmable handler: {}", pref.getClass(), pref);
                            return;
                        }
                        handler.notifyPreferenceChanged(preferenceKey);
                    })
                    .setNegativeButton(handler.getContext().getString(R.string.Cancel), (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
            return false;
        });
    }

    public static void populateWithBpmRange(final CharSequence prefKey,
                                            final DeviceSpecificSettingsHandler handler,
                                            final int rangeMin,
                                            final int rangeMax) {
        final Preference pref = handler.findPreference(prefKey);
        if (pref == null) {
            return;
        }

        if (rangeMin >= rangeMax) {
            throw new IllegalArgumentException("Invalid range [" + rangeMin + ", " + rangeMax + "]");
        }

        final CharSequence[] entries = new CharSequence[rangeMax - rangeMin + 2];
        final CharSequence[] values = new CharSequence[rangeMax - rangeMin + 2];
        entries[0] = handler.getContext().getString(R.string.off);
        values[0] = "0";

        for (int i = 1, bpm = rangeMin; bpm <= rangeMax; i++, bpm++) {
            entries[i] = handler.getContext().getString(R.string.bpm_value_unit, bpm);
            values[i] = String.valueOf(bpm);
        }

        if (pref instanceof ListPreference) {
            ((ListPreference) pref).setEntries(entries);
            ((ListPreference) pref).setEntryValues(values);
        } else if (pref instanceof MultiSelectListPreference) {
            ((MultiSelectListPreference) pref).setEntries(entries);
            ((MultiSelectListPreference) pref).setEntryValues(values);
        }
    }
}
