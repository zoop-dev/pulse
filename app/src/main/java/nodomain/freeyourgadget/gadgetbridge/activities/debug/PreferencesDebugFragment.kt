package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.debug.preferences.PreferenceManagerActivity
import nodomain.freeyourgadget.gadgetbridge.prefs.GBPrefsMigrator

class PreferencesDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_preferences, rootKey)

        findPreference<Preference>(PREF_DEBUG_PREFERENCES_VERSION)!!.summary = GBApplication.getPrefs().getString(
            GBPrefsMigrator.PREFS_VERSION,
            getString(R.string.unknown)
        )

        onClick(PREF_DEBUG_GLOBAL_PREFERENCES) {
            editPreferences(
                requireContext().packageName + "_preferences",
                requireContext().getString(R.string.global_preferences)
            )
        }

        val devicePreferences = findPreference<PreferenceCategory>(PREF_HEADER_DEVICE_PREFERENCES)!!
        removeDynamicPrefs(devicePreferences)

        for (device in GBApplication.app().deviceManager.devices.sortedBy { it.aliasOrName }) {
            addDynamicPref(
                devicePreferences,
                device.aliasOrName,
                device.address,
                device.deviceCoordinator.defaultIconResource
            ) {
                editPreferences("devicesettings_" + device.address.toString().uppercase(), device.aliasOrName)
            }
        }
    }

    private fun editPreferences(name: String, title: String) {
        val intent = Intent(requireContext(), PreferenceManagerActivity::class.java)
        intent.putExtra(PreferenceManagerActivity.EXTRA_NAME, name)
        intent.putExtra(PreferenceManagerActivity.EXTRA_TITLE, title)
        requireContext().startActivity(intent)
    }

    companion object {
        private const val PREF_DEBUG_PREFERENCES_VERSION = "pref_debug_preferences_version";
        private const val PREF_DEBUG_GLOBAL_PREFERENCES = "pref_debug_global_preferences";
        private const val PREF_HEADER_DEVICE_PREFERENCES = "pref_header_device_preferences";
    }
}
