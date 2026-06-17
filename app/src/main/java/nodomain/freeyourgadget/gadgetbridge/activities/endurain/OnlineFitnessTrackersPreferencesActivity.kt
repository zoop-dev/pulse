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
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.GB

class OnlineFitnessTrackersPreferencesActivity : AbstractSettingsActivityV2() {
    override fun newFragment(): PreferenceFragmentCompat =
        OnlineFitnessTrackersPreferencesFragment()

    class OnlineFitnessTrackersPreferencesFragment : AbstractPreferenceFragment() {

        private val vm: EndurainSetupViewModel by viewModels()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.online_fitness_trackers_preferences, rootKey)

            updateNetworkWarning()
            wireLoginPreferences()
            wireLogoutPreferences()
            updateStatus()
            updateLogoutPreferenceVisibility()
            setupLoginResultListener()

            // Refresh tokens
            val vm: EndurainSetupViewModel by viewModels()
            val server = GBApplication.getPrefs().preferences.getString("endurain_server", null)
            if (server != null) {
                if (vm.endurainTokenManager.isAccessTokenExpired()) {
                    vm.endurainTokenManager.performTokenRefresh(server) {
                        activity?.runOnUiThread {
                            updateStatus()
                            updateLogoutPreferenceVisibility()
                        }
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
            parentFragmentManager.setFragmentResultListener(
                "wanderer_login_result",
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

        private fun wireLoginPreferences() {
            findPreference<Preference>("pref_key_endurain_log_in")?.setOnPreferenceClickListener {
                EndurainSetupBottomSheet()
                    .show(parentFragmentManager, "endurain_setup")
                true
            }
            findPreference<Preference>("pref_key_wanderer_log_in")?.setOnPreferenceClickListener {
                WandererSetupBottomSheet()
                    .show(parentFragmentManager, "wanderer_setup")
                true
            }
        }

        private fun wireLogoutPreferences() {
            findPreference<Preference>("pref_key_endurain_log_out")?.setOnPreferenceClickListener {
                vm.logout { success ->
                    activity?.runOnUiThread {
                        if (success) {
                            GB.toast(getString(R.string.endurain_logged_out_successfully), Toast.LENGTH_SHORT, GB.INFO)
                            updateStatus()
                            updateLogoutPreferenceVisibility()
                        } else {
                            GB.toast(getString(R.string.endurain_logout_failed), Toast.LENGTH_SHORT, GB.WARN)
                        }
                    }
                }
                true
            }
            findPreference<Preference>("pref_key_wanderer_log_out")?.setOnPreferenceClickListener {
                WandererTokenManager(requireContext()).clearTokens()
                activity?.runOnUiThread {
                    GB.toast(getString(R.string.endurain_logged_out_successfully), Toast.LENGTH_SHORT, GB.INFO)
                    updateStatus()
                    updateLogoutPreferenceVisibility()
                }

                true
            }
        }

        private fun updateLogoutPreferenceVisibility() {
            findPreference<Preference>("pref_key_endurain_log_out")?.isVisible = vm.endurainTokenManager.isLoggedIn()
            findPreference<Preference>("pref_key_endurain_log_in")?.isVisible = !vm.endurainTokenManager.isLoggedIn()
            findPreference<Preference>("pref_key_wanderer_log_out")?.isVisible = WandererTokenManager(requireContext()).isLoggedIn()
            findPreference<Preference>("pref_key_wanderer_log_in")?.isVisible = !WandererTokenManager(requireContext()).isLoggedIn()
        }

        private fun updateStatus() {
            val endurainStatusPref = findPreference<Preference>("pref_key_endurain_status")
            val endurainServer = GBApplication.getPrefs().preferences.getString("endurain_server", null)
            val endurainTokenExpiresAt = DateTimeUtils.parseTimeStamp(vm.endurainTokenManager.getRefreshTokenExpiresAt())
            val wandererStatusPref = findPreference<Preference>("pref_key_wanderer_status")
            val wandererServer = GBApplication.getPrefs().preferences.getString("wanderer_server", null)
            val wandererAPITokenAvailable = WandererTokenManager(requireContext()).isLoggedIn()

            // Update Endurain preferences
            var summaryText = getString(R.string.endurain_not_logged_in_integration_disabled)
            if (vm.endurainTokenManager.isLoggedIn() && endurainServer != null) {
                summaryText =
                    getString(R.string.endurain_logged_in_refresh_token, endurainServer, endurainTokenExpiresAt)
            }
            if (vm.endurainServerVersion != null) {
                summaryText += getString(R.string.endurain_server_version, vm.endurainServerVersion)
            }
            endurainStatusPref?.summary = summaryText

            // Update Wanderer preferences
            summaryText = getString(R.string.endurain_not_logged_in_integration_disabled)
            if (wandererAPITokenAvailable) {
                summaryText =
                    getString(R.string.wanderer_logged_in).format(wandererServer)
            }
            wandererStatusPref?.summary = summaryText
        }
    }
}