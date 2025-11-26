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

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils.PACKAGE_INTERNET_HELPER

class InternetHelperPreferencesActivity : AbstractSettingsActivityV2() {
    override fun newFragment(): PreferenceFragmentCompat? {
        return InternetHelperPreferencesFragment()
    }

    companion object {
        class InternetHelperPreferencesFragment : AbstractPreferenceFragment() {
            override fun onCreatePreferences(
                savedInstanceState: Bundle?,
                rootKey: String?
            ) {
                setPreferencesFromResource(R.xml.internethelper_preferences, rootKey)
                val installWarning =
                    findPreference<Preference>("pref_key_internethelper_not_installed")
                if (AndroidUtils.isPackageInstalled(PACKAGE_INTERNET_HELPER)) {
                    installWarning?.isVisible = false
                } else {
                    installWarning?.setOnPreferenceClickListener {
                        val startIntent = Intent(Intent.ACTION_VIEW)
                        startIntent.data =
                            "https://codeberg.org/Freeyourgadget/Internethelper/releases".toUri()
                        startActivity(startIntent)
                        true
                    }
                }
            }
        }
    }
}