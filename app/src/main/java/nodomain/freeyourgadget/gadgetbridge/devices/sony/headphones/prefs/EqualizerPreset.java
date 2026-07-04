/*  Copyright (C) 2021-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.LabeledEntry;

public enum EqualizerPreset implements LabeledEntry {
    OFF((byte) 0x00, R.string.sony_equalizer_preset_off),
    BRIGHT((byte) 0x10, R.string.sony_equalizer_preset_bright),
    EXCITED((byte) 0x11, R.string.sony_equalizer_preset_excited),
    MELLOW((byte) 0x12, R.string.sony_equalizer_preset_mellow),
    RELAXED((byte) 0x13, R.string.sony_equalizer_preset_relaxed),
    VOCAL((byte) 0x14, R.string.sony_equalizer_preset_vocal),
    TREBLE_BOOST((byte) 0x15, R.string.sony_equalizer_preset_treble_boost),
    BASS_BOOST((byte) 0x16, R.string.sony_equalizer_preset_bass_boost),
    SPEECH((byte) 0x17, R.string.sony_equalizer_preset_speech),
    MANUAL((byte) 0xa0, R.string.sony_equalizer_preset_manual),
    CUSTOM_1((byte) 0xa1, R.string.sony_equalizer_preset_custom_1),
    CUSTOM_2((byte) 0xa2, R.string.sony_equalizer_preset_custom_2);

    private final byte code;
    @StringRes
    private final int label;

    EqualizerPreset(final byte code, @StringRes final int label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public int getLabel() {
        return label;
    }

    public byte getCode() {
        return this.code;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE, name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static EqualizerPreset fromCode(final byte code) {
        for (EqualizerPreset value : EqualizerPreset.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }

        return null;
    }

    public static EqualizerPreset fromPreferences(final SharedPreferences prefs) {
        return EqualizerPreset.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE, "off").toUpperCase());
    }
}
