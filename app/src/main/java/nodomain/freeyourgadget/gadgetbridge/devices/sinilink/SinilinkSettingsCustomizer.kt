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
package nodomain.freeyourgadget.gadgetbridge.devices.sinilink

import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreference.OnBindEditTextListener
import androidx.preference.Preference
import kotlinx.parcelize.Parcelize
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl.PREF_PASSWORD_ENABLED
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl.PREF_SCREEN_PASSWORD
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.util.Prefs


@Parcelize
class SinilinkSettingsCustomizer : DeviceSpecificSettingsCustomizer {
    override fun onPreferenceChange(preference: Preference, handler: DeviceSpecificSettingsHandler
    ) {
    }

    override fun onDeviceChanged(handler: DeviceSpecificSettingsHandler) {

    }

    override fun customizeSettings(
        handler: DeviceSpecificSettingsHandler,
        genericDevicePrefs: Prefs,
        rootKey: String?
    ) {
        // Override the default summary if it is not a band or a watch
        handler.findPreference<Preference>(PREF_SCREEN_PASSWORD)?.summary = null
        handler.findPreference<Preference>(PREF_PASSWORD_ENABLED)?.summary = null

        handler.findPreference<EditTextPreference>(DeviceSettingsPreferenceConst.PREF_DEVICE_NAME)?.let {
            it.setOnBindEditTextListener { editText: EditText? ->
                editText!!.setFilters(arrayOf<InputFilter>(LengthFilter(10)))
            }
        }
    }

    override fun getPreferenceKeysWithSummary(): Set<String> {
        return setOf()
    }

}
