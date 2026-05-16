package nodomain.freeyourgadget.gadgetbridge.devices.soundcore.sport_x20;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_EQUALIZER_BAND8_VALUE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_EQUALIZER_CUSTOM;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_EQUALIZER_PRESET;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_EQUALIZER_RESET;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_ENABLE_PAIRING_MODE;

import android.os.Parcel;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.sport_x20.SoundcoreSportX20Protocol;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SoundcoreSportX20SettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    public static final Creator<SoundcoreSportX20SettingsCustomizer> CREATOR = new Creator<SoundcoreSportX20SettingsCustomizer>() {
        @Override
        public SoundcoreSportX20SettingsCustomizer createFromParcel(final Parcel in) {
            return new SoundcoreSportX20SettingsCustomizer();
        }

        @Override
        public SoundcoreSportX20SettingsCustomizer[] newArray(final int size) {
            return new SoundcoreSportX20SettingsCustomizer[size];
        }
    };

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        if (!PREF_SOUNDCORE_EQUALIZER_PRESET.equals(preference.getKey())) {
            return;
        }

        final Preference equalizerCustom = handler.findPreference(PREF_SOUNDCORE_EQUALIZER_CUSTOM);
        if (equalizerCustom == null) {
            return;
        }

        final String preset = ((ListPreference) preference).getValue();
        equalizerCustom.setEnabled("254".equals(preset));
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Preference pairingMode = handler.findPreference(PREF_SOUNDCORE_ENABLE_PAIRING_MODE);
        if (pairingMode != null) {
            pairingMode.setOnPreferenceClickListener(pref -> {
                handler.notifyPreferenceChanged(PREF_SOUNDCORE_ENABLE_PAIRING_MODE);
                return true;
            });
        }

        final Preference equalizerCustom = handler.findPreference(PREF_SOUNDCORE_EQUALIZER_CUSTOM);
        if (equalizerCustom != null) {
            final String preset = prefs.getString(PREF_SOUNDCORE_EQUALIZER_PRESET, "0");
            equalizerCustom.setEnabled("254".equals(preset));
        }

        final Preference equalizerReset = handler.findPreference(PREF_SOUNDCORE_EQUALIZER_RESET);
        if (equalizerReset != null) {
            equalizerReset.setOnPreferenceClickListener(pref -> resetEqualizer(handler));
        }
    }

    private boolean resetEqualizer(final DeviceSpecificSettingsHandler handler) {
        for (final String key : SoundcoreSportX20Protocol.EQUALIZER_PREFS_VALUE) {
            final SeekBarPreference pref = handler.findPreference(key);
            if (pref != null) {
                pref.setValue(0);
            }
        }

        handler.notifyPreferenceChanged(PREF_SOUNDCORE_EQUALIZER_BAND8_VALUE);
        return true;
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        // No-op.
    }
}
