/*  Copyright (C) 2023-2024 Andreas Shimokawa, Arjan Schrijver, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreference;
import com.mobeta.android.dslv.DragSortListPreference;
import com.mobeta.android.dslv.DragSortListPreferenceFragment;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.XDatePreference;
import nodomain.freeyourgadget.gadgetbridge.util.XDatePreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreference;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialEditTextPreferenceDialogFragment;
import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialListPreferenceDialogFragment;
import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialMultiSelectListPreferenceDialogFragment;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.MinMaxTextWatcher;

public abstract class AbstractPreferenceFragment extends PreferenceFragmentCompat implements MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPreferenceFragment.class);

    private final SharedPreferencesChangeHandler sharedPreferencesChangeHandler = new SharedPreferencesChangeHandler();

    public static final String FRAGMENT_TAG = "preference_fragment";

    private SearchConfiguration mSearchConfiguration;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SearchPreference searchPreference = findPreference("searchPreference");
        if (searchPreference != null) {
            mSearchConfiguration = searchPreference.getSearchConfiguration();
            mSearchConfiguration.setActivity((AppCompatActivity) requireActivity());
            mSearchConfiguration.setHistoryId(requireActivity().getClass().getName());
        }
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        menu.clear();
        if (mSearchConfiguration != null) {
            inflater.inflate(R.menu.menu_preferences, menu);
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.preferences_search) {
            final SearchPreference searchPreference = findPreference("searchPreference");
            if (searchPreference != null) {
                //searchPreference.setVisible(true);
                mSearchConfiguration.showSearchFragment();
                return true;
            }
        }

        return false;
    }

    @Override
    public void onStart() {
        super.onStart();

        final SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

        reloadPreferences(sharedPreferences, getPreferenceScreen());

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesChangeHandler);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateActionBarTitle();
    }

    private void updateActionBarTitle() {
        try {
            CharSequence title = getPreferenceScreen().getTitle();
            if (StringUtils.isBlank(title)) {
                title = requireActivity().getTitle();
            }
            ((AbstractSettingsActivityV2) requireActivity()).setActionBarTitle(title);
        } catch (final Exception e) {
            LOG.error("Failed to update action bar title", e);
        }
    }

    @Override
    public void onStop() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(sharedPreferencesChangeHandler);

        super.onStop();
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull final Preference preference) {
        DialogFragment dialogFragment;
        if (preference instanceof XTimePreference) {
            dialogFragment = new XTimePreferenceFragment();
        } else if (preference instanceof XDatePreference) {
            dialogFragment = new XDatePreferenceFragment();
        } else if (preference instanceof DragSortListPreference) {
            dialogFragment = new DragSortListPreferenceFragment();
        } else if (preference instanceof EditTextPreference) {
            dialogFragment = MaterialEditTextPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof ListPreference) {
            dialogFragment = MaterialListPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof MultiSelectListPreference) {
            dialogFragment = MaterialMultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        final Bundle bundle = new Bundle(1);
        bundle.putString("key", preference.getKey());
        dialogFragment.setArguments(bundle);
        dialogFragment.setTargetFragment(this, 0);
        if (getFragmentManager() != null) {
            dialogFragment.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
        }
    }

    /**
     * Keys of preferences which should print its values as a summary below the preference name.
     */
    protected Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    protected void onSharedPreferenceChanged(final Preference preference) {
        // Nothing to do
    }

    public void setInputTypeFor(final String preferenceKey, final int editTypeFlags) {
        final EditTextPreference textPreference = findPreference(preferenceKey);
        if (textPreference != null) {
            textPreference.setOnBindEditTextListener(editText -> editText.setInputType(editTypeFlags));
        }
    }

    public void setNumericInputTypeWithRangeFor(final String preferenceKey, int min, int max, boolean allowEmpty) {
        final EditTextPreference textPreference = findPreference(preferenceKey);
        if (textPreference != null) {
            textPreference.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.addTextChangedListener(new MinMaxTextWatcher(editText, min, max, allowEmpty));
                editText.setSelection(editText.getText().length());
            });
        }
    }

    protected void index(@XmlRes final int preferencesResId) {
        index(preferencesResId, 0);
    }

    protected void index(@XmlRes final int preferencesResId, final int breadcrumb) {
        if (mSearchConfiguration == null) {
            final SearchPreference searchPreference = findPreference("searchPreference");
            if (searchPreference != null) {
                mSearchConfiguration = searchPreference.getSearchConfiguration();
                mSearchConfiguration.setActivity((AppCompatActivity) requireActivity());
                mSearchConfiguration.setHistoryId(requireActivity().getClass().getName());
                mSearchConfiguration.setBreadcrumbsEnabled(true);
            }
        }

        if (mSearchConfiguration != null) {
            final SearchConfiguration.SearchIndexItem indexItem = mSearchConfiguration.index(preferencesResId);
            if (breadcrumb != 0) {
                indexItem.addBreadcrumb(breadcrumb);
            }
        }
    }

    /**
     * Reload the preferences in the current screen. This is needed when the user enters or exists a PreferenceScreen,
     * otherwise the settings won't be reloaded by the {@link SharedPreferencesChangeHandler}, as the preferences return
     * null, since they're not visible.
     *
     * @param sharedPreferences the {@link SharedPreferences} instance
     * @param preferenceGroup   the {@link PreferenceGroup} for which preferences will be reloaded
     */
    private void reloadPreferences(final SharedPreferences sharedPreferences, final PreferenceGroup preferenceGroup) {
        if (preferenceGroup == null) {
            return;
        }

        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            final Preference preference = preferenceGroup.getPreference(i);

            LOG.trace("Reloading {}", preference.getKey());

            if (preference instanceof PreferenceCategory) {
                reloadPreferences(sharedPreferences, (PreferenceCategory) preference);
                continue;
            }

            sharedPreferencesChangeHandler.onSharedPreferenceChanged(sharedPreferences, preference.getKey());
        }
    }

    /**
     * Handler for preference changes, update UI accordingly (if device updates the preferences).
     */
    private class SharedPreferencesChangeHandler implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
            LOG.trace("Preference changed: {}", key);

            final Prefs prefs = new Prefs(sharedPreferences);

            if (key == null) {
                LOG.warn("Preference null, ignoring");
                return;
            }

            final Preference preference = findPreference(key);
            if (preference == null) {
                LOG.warn("Preference {} not found", key);

                return;
            }

            if (preference instanceof SeekBarPreference seekBarPreference) {
                seekBarPreference.setValue(prefs.getInt(key, seekBarPreference.getValue()));
            } else if (preference instanceof SwitchPreferenceCompat switchPreference) {
                switchPreference.setChecked(prefs.getBoolean(key, switchPreference.isChecked()));
            } else if (preference instanceof ListPreference listPreference) {
                listPreference.setValue(prefs.getString(key, listPreference.getValue()));
            } else if (preference instanceof EditTextPreference editTextPreference) {
                editTextPreference.setText(prefs.getString(key, editTextPreference.getText()));
            } else if (preference instanceof XTimePreference xTimePreference) {
                final LocalTime localTime = prefs.getLocalTime(key, xTimePreference.getPrefValue());
                xTimePreference.setValue(localTime.getHour(), localTime.getMinute());
            } else if (preference instanceof XDatePreference xDatePreference) {
                final LocalDate localDate = prefs.getLocalDate(key, xDatePreference.getPrefValue());
                xDatePreference.setValue(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
            } else if (preference instanceof PreferenceScreen || Preference.class.equals(preference.getClass())) {
                LOG.trace("Unknown preference class {} for {}, ignoring", preference.getClass(), key);
            } else {
                LOG.warn("Unknown preference class {} for {}, ignoring", preference.getClass(), key);
            }

            if (getPreferenceKeysWithSummary().contains(key)) {
                final String summary;

                // For multi select preferences, let's set the summary to the values, comma-delimited
                if (preference instanceof MultiSelectListPreference multiSelectListPreference) {
                    final Set<String> prefSetValue = prefs.getStringSet(key, Collections.emptySet());
                    if (prefSetValue.isEmpty()) {
                        summary = requireContext().getString(R.string.not_set);
                    } else {
                        final CharSequence[] entries = multiSelectListPreference.getEntries();
                        final CharSequence[] entryValues = multiSelectListPreference.getEntryValues();
                        final List<String> translatedEntries = new ArrayList<>();
                        for (int i = 0; i < entryValues.length; i++) {
                            if (prefSetValue.contains(entryValues[i].toString())) {
                                translatedEntries.add(entries[i].toString());
                            }
                        }
                        summary = TextUtils.join(", ", translatedEntries);
                    }
                } else {
                    summary = prefs.getString(key, preference.getSummary() != null ? preference.getSummary().toString() : "");
                }

                if (preference.getSummaryProvider() == null) {
                    try {
                        preference.setSummary(summary);
                    } catch (final Exception e) {
                        LOG.error("Failed to set preference summary for {}", key, e);
                    }
                }
            }

            AbstractPreferenceFragment.this.onSharedPreferenceChanged(preference);
        }
    }
}
