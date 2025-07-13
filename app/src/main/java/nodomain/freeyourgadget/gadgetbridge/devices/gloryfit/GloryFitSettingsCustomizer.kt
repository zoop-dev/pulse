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
package nodomain.freeyourgadget.gadgetbridge.devices.gloryfit

import androidx.preference.Preference
import kotlinx.parcelize.Parcelize
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler
import nodomain.freeyourgadget.gadgetbridge.util.Prefs

@Parcelize
class GloryFitSettingsCustomizer : DeviceSpecificSettingsCustomizer {
    override fun onPreferenceChange(
        preference: Preference,
        handler: DeviceSpecificSettingsHandler
    ) {
    }

    override fun onDeviceChanged(handler: DeviceSpecificSettingsHandler) {
    }

    override fun customizeSettings(
        handler: DeviceSpecificSettingsHandler,
        genericDevicePrefs: Prefs,
        rootKey: String?
    ) {
        DeviceSettingsUtils.populateWithBpmRange(
            DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD,
            handler,
            100,
            200
        )

        DeviceSettingsUtils.populateWithBpmRange(
            DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD,
            handler,
            40,
            100
        )

        // Inactivity reminders
        val dndEnabled = handler.findPreference<Preference>(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND)
        dndEnabled?.summary = handler.context.getString(R.string.mi2_prefs_inactivity_warnings_dnd_lunch_break_summary)
        val dndStart = handler.findPreference<Preference>(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_START)
        dndStart?.isVisible = false
        val dndEnd = handler.findPreference<Preference>(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_END)
        dndEnd?.isVisible = false
    }

    override fun getPreferenceKeysWithSummary(): Set<String> {
        return setOf()
    }
}
