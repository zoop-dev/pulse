/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.automations

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import org.slf4j.LoggerFactory

class AutoExportGpxSettingsActivity : AbstractSettingsActivityV2() {
    override fun newFragment(): PreferenceFragmentCompat {
        return AutoExportGpxSettingsFragment()
    }

    companion object {
        class AutoExportGpxSettingsFragment : AbstractPreferenceFragment() {
            companion object {
                private val LOG = LoggerFactory.getLogger(AutoExportGpxSettingsFragment::class.java)
            }

            override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
                setPreferencesFromResource(R.xml.auto_export_gpx_settings, rootKey)

                setupCustomDirectoryPreference()
                setupDeviceSelection()
            }

            private fun setupCustomDirectoryPreference() {
                val gbPrefs = GBApplication.getPrefs()

                val customDirectoryPicker = registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result: ActivityResult? ->
                    if (result?.resultCode != RESULT_OK) {
                        return@registerForActivityResult
                    }
                    val uri = result.data?.data
                    LOG.info("Got {} for gpx export directory", uri)
                    if (uri == null) {
                        return@registerForActivityResult
                    }

                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    gbPrefs.preferences.edit {
                        putString(GBPrefs.AUTO_EXPORT_GPX_DIRECTORY, uri.toString())
                    }

                    updateCustomDirectorySummary(uri.toString())
                }

                val customDirPref = findPreference<Preference>(GBPrefs.AUTO_EXPORT_GPX_DIRECTORY)
                customDirPref?.setOnPreferenceClickListener {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    customDirectoryPicker.launch(intent)
                    true
                }

                val currentUri = gbPrefs.getString(GBPrefs.AUTO_EXPORT_GPX_DIRECTORY, "")
                updateCustomDirectorySummary(currentUri)
            }

            private fun updateCustomDirectorySummary(uriString: String) {
                val customDirPref = findPreference<Preference>(GBPrefs.AUTO_EXPORT_GPX_DIRECTORY)
                if (uriString.isEmpty()) {
                    customDirPref?.summary = getString(R.string.not_set)
                } else {
                    customDirPref?.summary = uriString
                }
            }

            private fun setupDeviceSelection() {
                val selectedDevicesPref =
                    findPreference<MultiSelectListPreference>(GBPrefs.AUTO_EXPORT_GPX_SELECTED_DEVICES)

                // Populate device list
                val devices = GBApplication.app().deviceManager.devices
                    .filter { it.deviceCoordinator.supportsRecordedActivities(it) }
                val deviceAddresses = devices.map { it.address }
                val deviceNames = devices.map { it.aliasOrName }

                selectedDevicesPref?.entryValues = deviceAddresses.toTypedArray()
                selectedDevicesPref?.entries = deviceNames.toTypedArray()
            }
        }
    }
}
