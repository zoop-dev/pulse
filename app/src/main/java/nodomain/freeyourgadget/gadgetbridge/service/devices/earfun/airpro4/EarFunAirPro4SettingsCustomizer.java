package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airpro4;

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

public class EarFunAirPro4SettingsCustomizer extends EarFunSettingsCustomizer {

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

    public EarFunAirPro4SettingsCustomizer(final GBDevice device) {
        super(device);
    }
}
