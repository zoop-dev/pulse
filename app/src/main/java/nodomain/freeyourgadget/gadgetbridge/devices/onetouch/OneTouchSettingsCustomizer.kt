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
package nodomain.freeyourgadget.gadgetbridge.devices.onetouch

import androidx.preference.Preference
import kotlinx.parcelize.Parcelize
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler
import nodomain.freeyourgadget.gadgetbridge.util.Prefs

@Parcelize
class OneTouchSettingsCustomizer : DeviceSpecificSettingsCustomizer {
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
        // FIXME: Read-only for now
        handler.findPreference<Preference>(DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_LOW)?.isSelectable = false
        handler.findPreference<Preference>(DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_HIGH)?.isSelectable = false

        DeviceSettingsUtils.populateWithRange(
            DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_LOW,
            handler,
            60,
            110,
            R.string.glucose_value_mg_dl,
            false
        )

        DeviceSettingsUtils.populateWithRange(
            DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_HIGH,
            handler,
            90,
            300,
            R.string.glucose_value_mg_dl,
            false
        )
    }

    override fun getPreferenceKeysWithSummary(): Set<String> {
        return setOf()
    }
}
