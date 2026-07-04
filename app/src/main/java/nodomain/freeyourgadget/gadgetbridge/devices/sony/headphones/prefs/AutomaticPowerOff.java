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

public enum AutomaticPowerOff implements LabeledEntry {
    OFF(new byte[]{(byte) 0x11, (byte) 0x00}, R.string.sony_automatic_power_off_off),
    AFTER_5_MIN(new byte[]{(byte) 0x00, (byte) 0x00}, R.string.sony_automatic_power_off_5_min),
    AFTER_30_MIN(new byte[]{(byte) 0x01, (byte) 0x01}, R.string.sony_automatic_power_off_30_min),
    AFTER_1_HOUR(new byte[]{(byte) 0x02, (byte) 0x02}, R.string.sony_automatic_power_off_1_hour),
    AFTER_3_HOUR(new byte[]{(byte) 0x03, (byte) 0x03}, R.string.sony_automatic_power_off_3_hour),
    WHEN_TAKEN_OFF(new byte[]{(byte) 0x10, (byte) 0x00}, R.string.sony_automatic_power_off_when_taken_off);

    private final byte[] code;
    @StringRes
    private final int label;

    AutomaticPowerOff(final byte[] code, @StringRes final int label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public int getLabel() {
        return label;
    }

    public byte[] getCode() {
        return this.code;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_AUTOMATIC_POWER_OFF, name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static AutomaticPowerOff fromCode(final byte b1, final byte b2) {
        for (AutomaticPowerOff value : AutomaticPowerOff.values()) {
            if (value.getCode()[0] == b1 && value.getCode()[1] == b2) {
                return value;
            }
        }

        return null;
    }

    public static AutomaticPowerOff fromPreferences(final SharedPreferences prefs) {
        return AutomaticPowerOff.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_AUTOMATIC_POWER_OFF, "off").toUpperCase());
    }
}
