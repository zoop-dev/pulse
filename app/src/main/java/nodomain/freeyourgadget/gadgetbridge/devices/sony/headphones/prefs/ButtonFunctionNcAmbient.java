/*  Copyright (C) 2026

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
import java.util.Map;

import androidx.annotation.StringRes;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.LabeledEntry;

public class ButtonFunctionNcAmbient {
    public enum Mode implements LabeledEntry {
        SWITCH_AMBIENT_SOUND("switch_ambient_sound", 0x00, R.string.sony_button_function_nc_ambient_switch_ambient_sound),
        GOOGLE_ASSISTANT("google_assistant", 0x01, R.string.sony_button_function_nc_ambient_google_assistant),
        ALEXA_ASSISTANT("alexa_assistant", 0x02, R.string.sony_button_function_nc_ambient_alexa_assistant);

        private final String prefValue;
        private final byte code;
        @StringRes
        private final int label;

        Mode(final String prefValue, final int code, @StringRes final int label) {
            this.prefValue = prefValue;
            this.code = (byte) code;
            this.label = label;
        }

        public String getPrefValue() {
            return prefValue;
        }

        public byte getCode() {
            return code;
        }

        @Override
        public int getLabel() {
            return label;
        }

        public static ButtonFunctionNcAmbient.Mode fromCode(final byte b) {
            for (ButtonFunctionNcAmbient.Mode value : ButtonFunctionNcAmbient.Mode.values()) {
                if (value.getCode() == b) {
                    return value;
                }
            }

            return null;
        }

        public static Mode fromPrefValue(final String prefValue) {
            for (final Mode mode : values()) {
                if (mode.prefValue.equals(prefValue)) {
                    return mode;
                }
            }

            return SWITCH_AMBIENT_SOUND;
        }
    }

    private final Mode mode;

    public ButtonFunctionNcAmbient(final Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_FUNCTION_NC_AMBIENT, mode.getPrefValue());
        }};
    }

    public static ButtonFunctionNcAmbient fromPreferences(final SharedPreferences prefs) {
        final String pref = prefs.getString(
                DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_FUNCTION_NC_AMBIENT,
                Mode.SWITCH_AMBIENT_SOUND.getPrefValue()
        );

        return new ButtonFunctionNcAmbient(Mode.fromPrefValue(pref));
    }
}
