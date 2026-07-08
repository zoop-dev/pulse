/*  Copyright (C) 2026 David Giron

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

public class VoiceAssistant {
    public enum Mode implements LabeledEntry {
        DO_NOT_USE((byte) 0xff, R.string.off),
        MOBILE_DEVICE((byte) 0x30, R.string.sony_voice_assistant_mobile_device),
        DIGITAL_ASSISTANT_GOOGLE((byte) 0x31, R.string.sony_button_function_nc_ambient_google_assistant),
        AMAZON_ALEXA((byte) 0x32, R.string.sony_button_function_nc_ambient_alexa_assistant),
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
            for (final Mode value : Mode.values()) {
                if (value.getCode() == code) {
                    return value;
                }
            }
            return null;
        }
    }

    private final Mode mode;

    public VoiceAssistant(final Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_VOICE_ASSISTANT_FUNCTION,
                    mode.name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static VoiceAssistant fromPreferences(final SharedPreferences prefs) {
        final String value = prefs.getString(
                DeviceSettingsPreferenceConst.PREF_SONY_VOICE_ASSISTANT_FUNCTION,
                Mode.DO_NOT_USE.name().toLowerCase(Locale.getDefault())
        );
        try {
            return new VoiceAssistant(Mode.valueOf(value.toUpperCase(Locale.getDefault())));
        } catch (final IllegalArgumentException e) {
            return new VoiceAssistant(Mode.DO_NOT_USE);
        }
    }
}
