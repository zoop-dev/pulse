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
package nodomain.freeyourgadget.gadgetbridge.devices.shokz

import androidx.preference.Preference
import kotlinx.parcelize.Parcelize
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler
import nodomain.freeyourgadget.gadgetbridge.service.devices.shokz.ShokzMediaSource
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import org.slf4j.LoggerFactory

@Parcelize
class ShokzSettingsCustomizer : DeviceSpecificSettingsCustomizer {
    override fun onPreferenceChange(
        preference: Preference,
        handler: DeviceSpecificSettingsHandler
    ) {
    }

    override fun onDeviceChanged(handler: DeviceSpecificSettingsHandler) {
        hideEqualizers(handler, GBApplication.getDevicePrefs(handler.device))
    }

    override fun customizeSettings(
        handler: DeviceSpecificSettingsHandler,
        genericDevicePrefs: Prefs,
        rootKey: String?
    ) {
        hideEqualizers(handler, genericDevicePrefs)
    }

    private fun hideEqualizers(
        handler: DeviceSpecificSettingsHandler,
        devicePrefs: Prefs
    ) {
        val mediaSourceValue: String =
            devicePrefs.getString(DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE, "")
        val mediaSource = ShokzMediaSource.fromPreference(mediaSourceValue) ?: run {
            LOG.warn("Unknown media source {}", mediaSourceValue)
            null
        }

        handler.findPreference<Preference>(DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_BLUETOOTH)?.isVisible =
            mediaSource == ShokzMediaSource.BLUETOOTH
        handler.findPreference<Preference>(DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_MP3)?.isVisible =
            mediaSource == ShokzMediaSource.MP3
        handler.findPreference<Preference>(DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE)?.isVisible =
            mediaSource == ShokzMediaSource.MP3
    }

    override fun getPreferenceKeysWithSummary(): Set<String> {
        return setOf()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ShokzSettingsCustomizer::class.java)
    }
}
