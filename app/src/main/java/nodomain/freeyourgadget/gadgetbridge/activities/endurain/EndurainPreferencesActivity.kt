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
import android.widget.Toast
import androidx.fragment.app.viewModels
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

        private val vm: EndurainSetupViewModel by viewModels()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.endurain_preferences, rootKey)

            updateNetworkWarning()
            wireLoginPreference()
            wireLogoutPreference()
            updateStatus()
            updateLogoutPreferenceVisibility()
            setupLoginResultListener()

            // Refresh auth token
            val vm: EndurainSetupViewModel by viewModels()
            val server = GBApplication.getPrefs().preferences.getString("endurain_server", null)
            if (server != null) {
                vm.performTokenRefresh(server) {
                    activity?.runOnUiThread {
                        updateStatus()
                        updateLogoutPreferenceVisibility()
                    }
                }
                vm.fetchServerVersion(server) {
                    activity?.runOnUiThread {
                        updateStatus()
                    }
                }
            }
        }

        private fun setupLoginResultListener() {
            parentFragmentManager.setFragmentResultListener(
                "endurain_login_result",
                this
            ) { _, bundle ->
                val success = bundle.getBoolean("success", false)
                if (success) {
                    updateStatus()
                    updateLogoutPreferenceVisibility()
                }
            }
        }

        override fun onResume() {
            super.onResume()
            updateStatus()
            updateLogoutPreferenceVisibility()
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

        private fun wireLogoutPreference() {
            findPreference<Preference>("pref_key_log_out")?.setOnPreferenceClickListener {
                performLogout()
                true
            }
        }

        private fun performLogout() {
            vm.logout { success ->
                activity?.runOnUiThread {
                    if (success) {
                        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                        updateStatus()
                        updateLogoutPreferenceVisibility()
                    } else {
                        Toast.makeText(requireContext(), "Logout failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun updateLogoutPreferenceVisibility() {
            findPreference<Preference>("pref_key_log_out")?.isVisible = vm.isLoggedIn()
            findPreference<Preference>("pref_key_log_in")?.isVisible = !vm.isLoggedIn()
        }

        private fun updateStatus() {
            val statusPref = findPreference<Preference>("pref_key_status")
            val server = GBApplication.getPrefs().preferences.getString("endurain_server", null)
            val tokenExpiresAt = vm.getTokenExpiresAt()

            var summaryText = "Not logged in, integration is disabled"
            if (vm.isLoggedIn() && server != null) {
                summaryText = "Logged in to $server\nAuth token expires: $tokenExpiresAt"
            }
            if (vm.serverVersion != null) {
                summaryText += "\nServer version: ${vm.serverVersion}"
            }
            statusPref?.summary = summaryText
        }
    }
}