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

public enum SoundPosition implements LabeledEntry {
    OFF((byte) 0x00, R.string.sony_sound_position_off),
    FRONT((byte) 0x03, R.string.sony_sound_position_front),
    FRONT_LEFT((byte) 0x01, R.string.sony_sound_position_front_left),
    FRONT_RIGHT((byte) 0x02, R.string.sony_sound_position_front_right),
    REAR_LEFT((byte) 0x11, R.string.sony_sound_position_rear_left),
    REAR_RIGHT((byte) 0x12, R.string.sony_sound_position_rear_right);

    private final byte code;
    @StringRes
    private final int label;

    SoundPosition(final byte code, @StringRes final int label) {
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
            put(DeviceSettingsPreferenceConst.PREF_SONY_SOUND_POSITION, name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static SoundPosition fromPreferences(final SharedPreferences prefs) {
        return SoundPosition.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_SOUND_POSITION, "off").toUpperCase());
    }
}
