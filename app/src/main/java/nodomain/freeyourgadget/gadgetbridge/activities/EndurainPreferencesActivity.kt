/*  Copyright (C) 2025 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.InternetUtils

class EndurainPreferencesActivity : AbstractSettingsActivityV2() {
    override fun newFragment(): PreferenceFragmentCompat {
        return EndurainPreferencesFragment()
    }

    companion object {
        class EndurainPreferencesFragment : AbstractPreferenceFragment() {
            override fun onCreatePreferences(
                savedInstanceState: Bundle?,
                rootKey: String?
            ) {
                setPreferencesFromResource(R.xml.endurain_preferences, rootKey)
                val prefs = GBApplication.getPrefs().preferences

                // Hide network required warning when network is available
                val networkWarning = findPreference<Preference>("pref_key_network_required")
                networkWarning?.isVisible = !GBApplication.hasInternetAccess()

                // Wire up the login button
                val loginPref = findPreference<Preference>("pref_key_log_in")
                loginPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val currentServer = prefs.getString("endurain_server", "https://")
                    val input = TextInputEditText(requireContext()).apply {
                        setText(currentServer)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val layout = TextInputLayout(requireContext()).apply {
                        hint = "Example: https://endurain.example.com"
                        addView(input)
                    }

                    val dialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Endurain server")
                        .setView(layout)
                        .setNegativeButton(R.string.Cancel, null)
                        .setPositiveButton(R.string.ok, null) // ← no listener yet
                        .create()

                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val newUrl = input.text.toString().toUri()
                            val newServer =
                                if (newUrl.scheme == null || newUrl.host == null) {
                                    null
                                } else {
                                    "${newUrl.scheme}://${newUrl.host}"
                                }

                            prefs.edit {
                                putString("endurain_server", newServer)
                            }

                            dialog.dismiss()
                            lifecycleScope.launch(Dispatchers.IO) {
                                doLogin()
                            }
                        }
                    }

                    dialog.show()

                    true
                }

                // TODO: Wire up the logoff button
                val logoffPref = findPreference<Preference>("pref_key_log_out")
                logoffPref?.isVisible = false

                // TODO: Fill the status preference text
                val statusPref = findPreference<Preference>("pref_key_status")
                statusPref?.summary = "Not logged in, integration is disabled"
            }

            private fun doLogin() {
                val prefs = GBApplication.getPrefs().preferences
                val server = prefs.getString("endurain_server", "").orEmpty()
                if (server.isBlank()) return

                val settingsUri = "${server}/api/v1/public/server_settings".toUri()
                val serverSettings = InternetUtils.doJsonRequest(settingsUri)

                if (serverSettings == null) {
                    GB.toast("Endurain server is offline", Toast.LENGTH_LONG, GB.ERROR)
                    return
                }

                val localLogin = serverSettings.optBoolean("local_login_enabled")
                val ssoLogin = serverSettings.optBoolean("sso_enabled")

                GB.toast("Endurain local=$localLogin sso=$ssoLogin", Toast.LENGTH_LONG, GB.INFO)
            }

        }
    }
}