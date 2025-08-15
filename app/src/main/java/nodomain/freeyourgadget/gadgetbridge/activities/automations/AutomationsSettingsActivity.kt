/*  Copyright (C) 2025 José Rebelo

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

import android.os.Bundle
import android.text.InputType
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs

class AutomationsSettingsActivity : AbstractSettingsActivityV2() {
    override fun newFragment(): PreferenceFragmentCompat {
        return AutomationsSettingsFragment()
    }

    companion object {
        class AutomationsSettingsFragment : AbstractPreferenceFragment() {
            override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
                setPreferencesFromResource(R.xml.automations_settings, rootKey)

                setInputTypeFor(GBPrefs.PREF_AUTO_FETCH_INTERVAL_LIMIT, InputType.TYPE_CLASS_NUMBER)

                val prefAutoFetchInterval = findPreference<Preference>(GBPrefs.PREF_AUTO_FETCH_INTERVAL_LIMIT)
                if (prefAutoFetchInterval != null) {
                    prefAutoFetchInterval.setOnPreferenceChangeListener { preference: Preference?, autoFetchInterval: Any? ->
                        val summary = String.format(
                            requireContext().applicationContext.getString(R.string.pref_auto_fetch_limit_fetches_summary),
                            (autoFetchInterval as String?)!!.toInt()
                        )
                        preference!!.setSummary(summary)
                        true
                    }

                    val autoFetchInterval = GBApplication.getPrefs().getInt(GBPrefs.PREF_AUTO_FETCH_INTERVAL_LIMIT, 0)
                    val summary = String.format(
                        requireContext().applicationContext.getString(R.string.pref_auto_fetch_limit_fetches_summary),
                        autoFetchInterval
                    )
                    prefAutoFetchInterval.setSummary(summary)
                }
            }
        }
    }
}
