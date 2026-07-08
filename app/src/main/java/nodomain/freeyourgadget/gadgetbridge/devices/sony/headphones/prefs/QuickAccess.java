/*  Copyright (C) 2022-2024 José Rebelo

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

public class QuickAccess {
    public enum Mode implements LabeledEntry {
        OFF((byte) 0x00, R.string.off),
        SPOTIFY((byte) 0x01, R.string.pref_title_touch_spotify),
        VOICE_ASSISTANT((byte) 0x02, R.string.pref_title_touch_voice_assistant),
        ;

        private final byte code;
        @StringRes
        private final int label;

        Mode(final byte code, @StringRes final int label) {
            this.code = code;
            this.label = label;
        }

        public byte getCode() {
            return this.code;
        }

        @Override
        public int getLabel() {
            return label;
        }

        public static Mode fromCode(final byte code) {
            for (Mode value : Mode.values()) {
                if (value.getCode() == code) {
                    return value;
                }
            }

            return null;
        }
    }

    final Mode doubleTap;
    final Mode tripleTap;

    public QuickAccess(final Mode doubleTap, final Mode tripleTap) {
        this.doubleTap = doubleTap;
        this.tripleTap = tripleTap;
    }

    public Mode getModeDoubleTap() {
        return doubleTap;
    }

    public Mode getModeTripleTap() {
        return tripleTap;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_DOUBLE_TAP, doubleTap.name().toLowerCase(Locale.getDefault()));
            put(DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_TRIPLE_TAP, tripleTap.name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static QuickAccess fromPreferences(final SharedPreferences prefs) {
        return new QuickAccess(
                Mode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_DOUBLE_TAP, "off").toUpperCase()),
                Mode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_TRIPLE_TAP, "off").toUpperCase())
        );
    }
}
