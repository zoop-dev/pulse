/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.aawireless;

import android.content.SharedPreferences;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class AAWirelessPrefs extends DevicePrefs {
    // Auto-standby
    public static final String PREF_AUTO_STANDBY_ENABLED = "auto_standby_enabled";
    public static final String PREF_AUTO_STANDBY_DEVICE = "auto_standby_device";

    // Button modes
    public static final String PREF_BUTTON_MODE_SINGLE_CLICK = "button_mode_single_click";
    public static final String PREF_BUTTON_MODE_DOUBLE_CLICK = "button_mode_double_click";
    public static final String BUTTON_MODE_NONE = "none";
    public static final String BUTTON_MODE_NEXT_PHONE = "next_phone";
    public static final String BUTTON_MODE_STANDBY_ON_OFF = "standby_on_off";
    public static final String PREF_HAS_BUTTON = "aawireless_has_button";

    // Advanced settings
    public static final String PREF_DONGLE_MODE = "dongle_mode";
    public static final String PREF_PASSTHROUGH = "passthrough";
    public static final String PREF_AUDIO_STUTTER_FIX = "audio_stutter_fix";
    public static final String AUDIO_STUTTER_FIX_OFF = "off";
    public static final String AUDIO_STUTTER_FIX_LOW = "low";
    public static final String AUDIO_STUTTER_FIX_HIGH = "high";
    public static final String AUDIO_STUTTER_FIX_UNLIMITED = "unlimited";
    public static final String PREF_DPI = "dpi";
    public static final String PREF_DISABLE_MEDIA_SINK = "disable_media_sink";
    public static final String PREF_DISABLE_TTS_SINK = "disable_tts_sink";
    public static final String PREF_REMOVE_TAP_RESTRICTION = "remove_tap_restriction";
    public static final String PREF_VAG_CRASH_FIX = "vag_crash_fix";
    public static final String PREF_START_FIX = "start_fix";
    public static final String PREF_DEVELOPER_MODE = "developer_mode";
    public static final String PREF_AUTO_VIDEO_FOCUS = "auto_video_focus";

    // Phone management
    public static final String PREF_PREFER_LAST_CONNECTED = "prefer_last_connected";
    public static final String PREF_SCREEN_PAIRED_PHONES = "screen_paired_phones";
    public static final String PREF_HEADER_PAIRED_PHONES = "paired_phones_header";
    public static final String PREF_NO_PAIRED_PHONES = "no_paired_phones";
    public static final String PREF_KNOWN_PHONES_COUNT = "aawireless_known_phones_count";
    public static final String PREF_KNOWN_PHONES_MAC = "aawireless_known_phones_mac_";
    public static final String PREF_KNOWN_PHONES_NAME = "aawireless_known_phones_name_";

    // Intents
    public static final String ACTION_PHONE_SWITCH = "aawireless_action_phone_switch";
    public static final String ACTION_PHONE_SORT = "aawireless_action_phone_sort";
    public static final String ACTION_PHONE_DELETE = "aawireless_action_phone_delete";
    public static final String EXTRA_PHONE_MAC = "phone_mac";
    public static final String EXTRA_PHONE_NEW_POSITION = "phone_position";

    public AAWirelessPrefs(final SharedPreferences preferences, final GBDevice gbDevice) {
        super(preferences, gbDevice);
    }

    public String getCountry() {
        return getString(DeviceSettingsPreferenceConst.PREF_COUNTRY, "US");
    }

    public boolean getAutoStandbyEnabled() {
        return getBoolean(PREF_AUTO_STANDBY_ENABLED, false);
    }

    public String getAutoStandbyDevice() {
        return getString(PREF_AUTO_STANDBY_DEVICE, "");
    }

    public String getButtonModeSingleClick() {
        return getString(PREF_BUTTON_MODE_SINGLE_CLICK, "none");
    }

    public String getButtonModeDoubleClick() {
        return getString(PREF_BUTTON_MODE_DOUBLE_CLICK, "none");
    }

    public String getWiFiFrequency() {
        return getString(DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY, "5");
    }

    public int getWiFiChannel() {
        return "5".equals(getWiFiFrequency()) ?
                getInt(DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_5, 0) :
                getInt(DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_2_4, 0);
    }

    public boolean enableDongleMode() {
        return getBoolean(PREF_DONGLE_MODE, false);
    }

    public boolean enablePassthrough() {
        return getBoolean(PREF_PASSTHROUGH, true);
    }

    public String getAudioStutterFix() {
        // off low high unlimited
        return getString(PREF_AUDIO_STUTTER_FIX, "off");
    }

    public int getDpi() {
        // [0-300]
        return getInt(PREF_DPI, 0);
    }

    public boolean disableMediaSink() {
        return getBoolean(PREF_DISABLE_MEDIA_SINK, false);
    }

    public boolean disableTtsSink() {
        return getBoolean(PREF_DISABLE_TTS_SINK, false);
    }

    public boolean removeTapRestriction() {
        return getBoolean(PREF_REMOVE_TAP_RESTRICTION, false);
    }

    public boolean enableVagCrashFix() {
        return getBoolean(PREF_VAG_CRASH_FIX, false);
    }

    public boolean enableStartFix() {
        return getBoolean(PREF_START_FIX, false);
    }

    public boolean enableDeveloperMode() {
        return getBoolean(PREF_DEVELOPER_MODE, false);
    }

    public boolean enableAutoVideoFocus() {
        return getBoolean(PREF_AUTO_VIDEO_FOCUS, false);
    }

    public boolean preferLastConnected() {
        return getBoolean(PREF_PREFER_LAST_CONNECTED, false);
    }

    public int getPairedPhoneCount() {
        return getInt(PREF_KNOWN_PHONES_COUNT, 0);
    }

    public String getPairedPhoneMac(final int position) {
        return getString(PREF_KNOWN_PHONES_MAC + position, "");
    }

    public String getPairedPhoneName(final int position) {
        return getString(PREF_KNOWN_PHONES_NAME + position, "");
    }
}
