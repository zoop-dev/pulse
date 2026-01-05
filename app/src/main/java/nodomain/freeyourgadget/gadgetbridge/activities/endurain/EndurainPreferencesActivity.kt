/*  Copyright (C) 2026 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.endurain

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2

class EndurainPreferencesActivity : AbstractSettingsActivityV2() {

    override fun newFragment(): PreferenceFragmentCompat =
        EndurainPreferencesFragment()

    class EndurainPreferencesFragment : AbstractPreferenceFragment() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.endurain_preferences, rootKey)

            updateNetworkWarning()
            wireLoginPreference()
            hideLogoffPreference()
            updateStatus()
        }

        private fun updateNetworkWarning() {
            findPreference<Preference>("pref_key_network_required")?.isVisible =
                !GBApplication.hasInternetAccess()
        }

        private fun wireLoginPreference() {
            findPreference<Preference>("pref_key_log_in")?.setOnPreferenceClickListener {
                EndurainSetupBottomSheet()
                    .show(parentFragmentManager, "endurain_setup")
                true
            }
        }

        private fun hideLogoffPreference() {
            findPreference<Preference>("pref_key_log_out")?.isVisible = false
        }

        private fun updateStatus() {
            findPreference<Preference>("pref_key_status")?.summary =
                "Not logged in, integration is disabled"
        }
    }
}