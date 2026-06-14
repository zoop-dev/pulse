/*  Copyright (C) 2026 Obside

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.freepro3;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_AMBIENT_SOUND_CONTROL;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_ANC_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_EQUALIZER_PRESET;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_TRANSPARENCY_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Equalizer.TenBandEqualizerPresets;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Equalizer;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunFreePro3SettingsCustomizer extends EarFunSettingsCustomizer {

    @Override
    public void onPreferenceChange(Preference preference, DeviceSpecificSettingsHandler handler) {
        super.onPreferenceChange(preference, handler);
        String key = preference.getKey();
        if (key == null) {
            return;
        }
        switch (key) {
            case PREF_EARFUN_AMBIENT_SOUND_CONTROL:
                onPreferenceChangeAmbientSoundControl(handler);
                break;
            case PREF_EARFUN_EQUALIZER_PRESET:
                onPreferenceChangeEqualizerPreset(handler, Equalizer.TenBandEqualizer, TenBandEqualizerPresets);
                break;
        }
        // if the band sliders match a preset, update the preset list
        if (Equalizer.containsKey(Equalizer.TenBandEqualizer, key)) {
            int equalizerPreset = getSelectedPresetFromEqualizerBands(handler,
                    Equalizer.TenBandEqualizer, TenBandEqualizerPresets);
            ListPreference listPreferenceEqualizerPreset = handler.findPreference(PREF_EARFUN_EQUALIZER_PRESET);
            if (listPreferenceEqualizerPreset != null) {
                listPreferenceEqualizerPreset.setValue(Integer.toString(equalizerPreset));
            }
        }
    }

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, String rootKey) {
        super.customizeSettings(handler, prefs, rootKey);
        initializeEqualizerPresetListPreference(handler, TenBandEqualizerPresets);
    }

    private void onPreferenceChangeAmbientSoundControl(DeviceSpecificSettingsHandler handler) {
        ListPreference listPreferenceAmbientSound = handler.findPreference(PREF_EARFUN_AMBIENT_SOUND_CONTROL);
        ListPreference listPreferenceTransparencyMode = handler.findPreference(PREF_EARFUN_TRANSPARENCY_MODE);
        ListPreference listPreferenceAncMode = handler.findPreference(PREF_EARFUN_ANC_MODE);

        if (listPreferenceAmbientSound == null || listPreferenceTransparencyMode == null || listPreferenceAncMode == null) {
            return;
        }

        switch (listPreferenceAmbientSound.getValue()) {
            case "1": // noise cancelling
                listPreferenceTransparencyMode.setVisible(false);
                listPreferenceAncMode.setVisible(true);
                break;
            case "2": // transparency
                listPreferenceTransparencyMode.setVisible(true);
                listPreferenceAncMode.setVisible(false);
                break;
            default:
                listPreferenceTransparencyMode.setVisible(false);
                listPreferenceAncMode.setVisible(false);
        }
    }

    public EarFunFreePro3SettingsCustomizer(final GBDevice device) {
        super(device);
    }
}
