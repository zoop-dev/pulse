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

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;

public class ButtonFunctionNcAmbient {
    public enum Mode {
        SWITCH_AMBIENT_SOUND("switch_ambient_sound", 0x00),
        GOOGLE_ASSISTANT("google_assistant", 0x01),
        ALEXA_ASSISTANT("alexa_assistant", 0x02);

        private final String prefValue;
        private final byte code;

        Mode(final String prefValue, final int code) {
            this.prefValue = prefValue;
            this.code = (byte) code;
        }

        public String getPrefValue() {
            return prefValue;
        }

        public byte getCode() {
            return code;
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
        return new HashMap<String, Object>() {{
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
