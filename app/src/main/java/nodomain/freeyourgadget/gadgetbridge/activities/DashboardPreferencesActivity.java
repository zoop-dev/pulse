/*  Copyright (C) 2024 Arjan Schrijver

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import nodomain.freeyourgadget.gadgetbridge.R;

public class DashboardPreferencesActivity extends AbstractSettingsActivityV2 {
    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new DashboardPreferencesFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static class DashboardPreferencesFragment extends AbstractPreferenceFragment {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.dashboard_preferences, rootKey);

            // Open the drag-and-drop layout editor (same as the Today edit pencil).
            final Preference editLayout = findPreference("pref_dashboard_edit_layout");
            if (editLayout != null) {
                editLayout.setOnPreferenceClickListener(preference -> {
                    startActivity(new Intent(requireContext(), PulseDashboardEditActivity.class));
                    return true;
                });
            }

            // Hero ring metric — re-render the dashboard when changed.
            final Preference ringMetric = findPreference("pulse_ring_metric");
            if (ringMetric != null) {
                ringMetric.setOnPreferenceChangeListener((preference, newVal) -> {
                    sendDashboardConfigChangedIntent();
                    return true;
                });
            }
        }

        /**
         * Signal dashboard that its config has changed
         */
        private void sendDashboardConfigChangedIntent() {
            Intent intent = new Intent();
            intent.setAction(DashboardFragment.ACTION_CONFIG_CHANGE);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        }
    }
}
