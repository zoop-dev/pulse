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

public enum SurroundMode implements LabeledEntry {
    OFF((byte) 0x00, R.string.sony_surround_mode_off),
    ARENA((byte) 0x02, R.string.sony_surround_mode_arena),
    CLUB((byte) 0x04, R.string.sony_surround_mode_club),
    OUTDOOR_STAGE((byte) 0x01, R.string.sony_surround_mode_outdoor_stage),
    CONCERT_HALL((byte) 0x03, R.string.sony_surround_mode_concert_hall);

    private final byte code;
    @StringRes
    private final int label;

    SurroundMode(final byte code, @StringRes final int label) {
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
            put(DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE, name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static SurroundMode fromPreferences(final SharedPreferences prefs) {
        return SurroundMode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE, "off").toUpperCase());
    }
}
