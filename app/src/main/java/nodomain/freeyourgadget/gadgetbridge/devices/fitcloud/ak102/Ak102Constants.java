/*  Copyright (C) 2026 Vladimir Tasic

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

package nodomain.freeyourgadget.gadgetbridge.devices.fitcloud.ak102;

import java.util.Locale;
import java.util.UUID;

/*
 * Protocol constants for TopStep FitCloudPro watches (AK102).
 * The wire protocol is a two-layer framing scheme:
 *   - Transport frame: 8-byte header (magic, flags, payload length, CRC-16/ARC,
 *       sequence) followed by the payload.
 *   - Payload: [cmdId][version][keyId][len_hi][len_lo][data...]. A command
 *       is addressed by the (cmdId, keyId) pair; the reply reuses the same cmdId with
 *       keyId + 1.
 */
public final class Ak102Constants {

    // GATT service and characteristics.
    public static final UUID UUID_SERVICE = UUID.fromString("000001ff-3c17-d293-8e48-14fe2e4da212");
    public static final UUID UUID_CHARACTERISTIC_WRITE = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_NOTIFY = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb");

    // Transport header.
    public static final byte FRAME_MAGIC = (byte) 0xAB;
    public static final int HEADER_LENGTH = 8;
    public static final int FLAG_ACK = 0x10;
    public static final int FLAG_ACK_ERROR = 0x20;
    public static final int FLAG_ENCRYPTED = 0x40;
    public static final int MTU_REQUEST = 247;

    // Command groups (cmdId).
    public static final byte CMD_SETTINGS = 2;
    public static final byte CMD_AUTH = 3;
    public static final byte CMD_NOTIFICATION = 4;
    public static final byte CMD_DATA_SYNC = 5;
    public static final byte CMD_CAMERA_MEDIA = 7;
    public static final byte CMD_SPECIAL = 8;

    // Auth keys (cmdId = CMD_AUTH).
    public static final byte KEY_BIND_REQUEST = 17;
    public static final byte KEY_BIND_RESPONSE = 18;
    public static final byte KEY_LOGIN_REQUEST = 19;
    public static final byte KEY_LOGIN_RESPONSE = 20;

    // Settings keys (cmdId = CMD_SETTINGS), phone -> watch.
    public static final byte KEY_SET_TIME = 1;
    public static final byte KEY_SET_ALARMS = 2;
    public static final byte KEY_ALARMS_REQUEST = 3;
    public static final byte KEY_ALARMS_RESPONSE = 4;
    public static final byte KEY_SET_USER_INFO = 5;
    public static final byte KEY_SET_EXERCISE_GOAL = 6;
    public static final byte KEY_BATTERY_REQUEST = 27;
    public static final byte KEY_BATTERY_RESPONSE = 28;
    public static final byte KEY_CONFIG_REQUEST = 32;
    public static final byte KEY_CONFIG_RESPONSE = 33;
    public static final byte KEY_SET_WEATHER = 54;
    public static final byte KEY_SET_LANGUAGE = 58;
    public static final byte KEY_SET_CONTACTS = 84;
    public static final byte KEY_FIND_DEVICE = 59;
    public static final byte KEY_SDK_FUNCTION = 80;
    public static final byte KEY_SET_MUSIC_STATE = (byte) 147;
    public static final byte KEY_SET_MUSIC_INFO = (byte) 148;
    public static final byte KEY_EXERCISE_GOAL_REQUEST = (byte) 153;
    public static final byte KEY_EXERCISE_GOAL_RESPONSE = (byte) 154;
    public static final byte KEY_RESTING_HR_REQUEST = (byte) 225;
    public static final byte KEY_RESTING_HR_RESPONSE = (byte) 226;
    public static final byte KEY_STOP_FIND_DEVICE = (byte) 224;
    // Config-item TLV types (readback blob, section 22). Writes use configWriteKey().
    public static final byte KEY_SET_CONFIG_PAGE = 23;
    public static final byte KEY_SET_CONFIG_FUNCTION = 26;
    public static final byte KEY_SET_CONFIG_HEALTH_MONITOR = 36;
    public static final byte KEY_SET_CONFIG_SEDENTARY = 39;
    public static final byte KEY_SET_CONFIG_DRINK_WATER = 42;
    public static final byte KEY_SET_CONFIG_TURN_WRIST = 45;
    public static final byte KEY_SET_CONFIG_DND = 78;
    public static final byte KEY_SET_CONFIG_SCREEN_VIBRATE = 122;

    // Write key for a config TLV type: type-2, except ScreenVibrate (122->123).
    public static byte configWriteKey(final byte type) {
        return (byte) (type == KEY_SET_CONFIG_SCREEN_VIBRATE ? type + 1 : type - 2);
    }

    // Bluetooth Classic call-audio preference keys (phone-side bond, no protocol).
    public static final String PREF_AUDIO_AUTO_CONNECT = "ak102_audio_auto_connect";
    public static final String PREF_AUDIO_PAIR = "ak102_audio_pair";
    public static final String PREF_AUDIO_UNPAIR = "ak102_audio_unpair";

    // PageConfig (item 23) multiselect of flag indices.
    public static final String PREF_DISPLAY_ITEMS = "ak102_display_items";
    // Power save (gate feature 302; 5B schedule payload with feature 303).
    public static final byte KEY_POWER_SAVE_WRITE = (byte) 169;

    // Data-sync keys (cmdId = CMD_DATA_SYNC).
    public static final byte KEY_SYNC_REQUEST = 1;
    public static final byte KEY_SYNC_START_ACK = 7;
    public static final byte KEY_SYNC_FINISH = 8;
    public static final byte KEY_SYNC_ACK = 32;
    public static final byte KEY_SYNC_CHUNK = 48;
    public static final byte KEY_TODAY_TOTAL_REQUEST = 33;
    public static final byte KEY_TODAY_TOTAL_RESPONSE = 34;
    public static final byte KEY_REALTIME_REQUEST = 35;
    public static final byte KEY_REALTIME_RESPONSE = 36;
    public static final byte KEY_REALTIME_TEMP_ERROR = 37;

    // Special-data keys (cmdId = CMD_SPECIAL).
    public static final byte KEY_SET_VOLUME = 77;

    // Sync data types (payload of KEY_SYNC_REQUEST, keyData[0]).
    public static final int SYNC_TYPE_STEP = 1;
    public static final int SYNC_TYPE_SLEEP = 2;
    public static final int SYNC_TYPE_HEART_RATE = 3;
    public static final int SYNC_TYPE_OXYGEN = 4;
    public static final int SYNC_TYPE_GPS = 10;
    public static final int SYNC_TYPE_SPORT = 16;
    public static final int SYNC_TYPE_PRESSURE = 18;
    public static final int SYNC_TYPE_HEART_RATE_MEASURE = 131;
    public static final int SYNC_TYPE_OXYGEN_MEASURE = 132;
    public static final int SYNC_TYPE_PRESSURE_MEASURE = 146;

    // Realtime health types (bitmask, KEY_REALTIME_REQUEST bytes 0..1).
    public static final int REALTIME_HEART_RATE = 1;
    public static final int REALTIME_OXYGEN = 2;
    public static final int REALTIME_PRESSURE = 64;

    // FunctionConfig (item type 26) flag indices (descending bit scheme).
    public static final int FUNCTION_FLAG_WEAR_WAY = 0;
    public static final int FUNCTION_FLAG_TIME_FORMAT = 2;
    public static final int FUNCTION_FLAG_LENGTH_UNIT = 3;
    public static final int FUNCTION_FLAG_TEMPERATURE_UNIT = 4;
    public static final int FUNCTION_FLAG_WEATHER_DISPLAY = 5;
    public static final int FUNCTION_FLAG_DISCONNECT_REMINDER = 6;

    // Settings keys, watch -> phone pushes.
    public static final byte KEY_PUSH_FIND_PHONE = 60;
    public static final byte KEY_PUSH_CONFIG_CHANGED = 81;
    public static final byte KEY_PUSH_ALARM_CHANGED = 109;
    public static final byte KEY_PUSH_SCHEDULE_CHANGED = 110;
    public static final byte KEY_PUSH_MUSIC_REFRESH = (byte) 146;
    public static final byte KEY_PUSH_QUICK_REPLY_SMS = (byte) 149;
    public static final byte KEY_PUSH_QUICK_REPLY_FREE = (byte) 190;
    public static final byte KEY_PUSH_SILENT_MODE = 120;
    public static final byte KEY_PUSH_BATTERY_CHANGED = (byte) 172;
    public static final byte KEY_PUSH_STOP_FIND_PHONE = 124;
    public static final byte KEY_PUSH_AUDIO_DEVICE_MAC = (byte) 132;
    public static final byte KEY_PUSH_SOS = (byte) 135;
    public static final byte KEY_PUSH_TYPED_EVENT = (byte) 202;

    // Typed events carried in KEY_PUSH_TYPED_EVENT keyData[0].
    public static final int EVENT_BATTERY = 1;
    public static final int EVENT_SPORT_FINISH = 2;
    public static final int EVENT_DIAL_SWITCH = 3;
    public static final int EVENT_HEART_RATE_MEASURE = 4;
    public static final int EVENT_STOP_FIND_DEVICE = 5;
    public static final int EVENT_SPORT_STATE = 6;
    public static final int EVENT_STOP_MEASURE = 7;
    public static final int EVENT_DELETE_DIAL = 8;
    public static final int EVENT_LOW_BATTERY = 9;

    // Telephony keys (cmdId = CMD_NOTIFICATION), watch -> phone.
    public static final byte KEY_PUSH_CALL_HANG_UP = 32;
    public static final byte KEY_PUSH_CALL_ACCEPT = 70;

    // Camera/media keys (cmdId = CMD_CAMERA_MEDIA).
    public static final byte KEY_PUSH_CAMERA_SHOOT = 1;
    public static final byte KEY_CAMERA_STATUS = 2;
    public static final byte KEY_PUSH_MEDIA_CONTROL = 3;
    public static final byte KEY_PUSH_CAMERA_OPEN = 4;
    public static final byte KEY_PUSH_CAMERA_EXIT = 5;

    // Media control subtypes (KEY_PUSH_MEDIA_CONTROL keyData[0]).
    public static final int MEDIA_PLAY_PAUSE_MAX = 2;
    public static final int MEDIA_NEXT = 3;
    public static final int MEDIA_PREVIOUS = 4;
    public static final int MEDIA_VOLUME_UP = 5;
    public static final int MEDIA_VOLUME_DOWN = 6;

    // Notification types (cmdId = CMD_NOTIFICATION, keyId = type).
    public static final byte NOTIFICATION_TELEPHONY_INCOMING = 1;
    public static final byte NOTIFICATION_TELEPHONY_ANSWERED = 2;
    public static final byte NOTIFICATION_TELEPHONY_REJECTED = 3;
    public static final byte NOTIFICATION_SMS = 4;
    public static final byte NOTIFICATION_QQ = 5;
    public static final byte NOTIFICATION_WECHAT = 6;
    public static final byte NOTIFICATION_FACEBOOK = 7;
    public static final byte NOTIFICATION_TWITTER = 8;
    public static final byte NOTIFICATION_LINKEDIN = 9;
    public static final byte NOTIFICATION_INSTAGRAM = 10;
    public static final byte NOTIFICATION_PINTEREST = 11;
    public static final byte NOTIFICATION_WHATSAPP = 12;
    public static final byte NOTIFICATION_LINE = 13;
    public static final byte NOTIFICATION_FACEBOOK_MESSENGER = 14;
    public static final byte NOTIFICATION_KAKAO = 15;
    public static final byte NOTIFICATION_SKYPE = 16;
    public static final byte NOTIFICATION_EMAIL = 17;
    public static final byte NOTIFICATION_TELEGRAM = 18;
    public static final byte NOTIFICATION_VIBER = 19;
    public static final byte NOTIFICATION_CALENDAR = 20;
    public static final byte NOTIFICATION_SNAPCHAT = 21;
    public static final byte NOTIFICATION_YOUTUBE = 34;
    public static final byte NOTIFICATION_GMAIL = 38;
    public static final byte NOTIFICATION_OUTLOOK = 39;

    // Auth result codes (first byte of the auth response payload).
    public static final byte AUTH_OK = 0;
    public static final byte AUTH_NEED_BIND = 1;
    public static final byte AUTH_BIND_CANCELLED = 2;
    public static final byte AUTH_BIND_TIMEOUT = 3;
    public static final byte AUTH_SHOULD_RESET = 5;

    // Config blob (KEY_CONFIG_RESPONSE) TLV item type carrying the device info.
    public static final int CONFIG_ITEM_DEVICE_INFO = 17;
    public static final int DEVICE_INFO_MIN_LENGTH = 38;

    // Device feature bits (device info blob). Features in the negated list are
    // supported when their bit is cleared.
    public static final int FEATURE_HEART_RATE = 0;
    public static final int FEATURE_OXYGEN = 1;
    public static final int FEATURE_TEMPERATURE = 10;
    public static final int FEATURE_PRESSURE = 13;
    public static final int FEATURE_WEATHER = 4;
    public static final int FEATURE_SPORT = 6;
    public static final int FEATURE_DYNAMIC_HEART_RATE = 9;
    public static final int FEATURE_MEASURE_DATA_SYNCABLE = 22;
    public static final int FEATURE_GPS = 24;
    public static final int FEATURE_LONG_NOTIFICATION = 267;
    public static final int FEATURE_CONTACTS = 271;
    public static final int FEATURE_CONTACTS_100 = 525;
    public static final int FEATURE_HEALTH_MONITOR_INTERVAL = 274;
    public static final int FEATURE_POWER_SAVE = 302;
    public static final int FEATURE_POWER_SAVE_SCHEDULE = 303;
    public static final int FEATURE_NEW_SLEEP_PROTOCOL = 268;
    public static final int FEATURE_WEATHER_FORECAST = 276;
    public static final int FEATURE_STEP_EXTRA = 259;
    public static final int FEATURE_SLEEP_NAP = 522;
    public static final int FEATURE_SLEEP_REM = 523;
    public static final int FEATURE_ACTIVITY_SPORT_DURATION = 539;
    public static final int FEATURE_HEART_RATE_RESTING = 540;
    public static final int FEATURE_STOP_FIND_DEVICE = 537;
    public static final int FEATURE_SPORT_ITEM_EXTRA = 546;
    public static final int FEATURE_NEW_SPORT_DETAIL = 558;
    public static final int FEATURE_SYNC_VOLUME_INFO = 572;
    public static final int FEATURE_MUSIC_INFO = 597;
    public static final int[] NEGATED_FEATURES = {18, 23, 256, 294, 582, 592, 593, 597, 602, 604};

    // Device-specific pref holding the raw device-info blob as hex.
    public static final String PREF_DEVICE_INFO = "ak102_device_info";
    // Device-specific pref prefix for cached config items (hex), by TLV type.
    public static final String PREF_CONFIG_PREFIX = "ak102_cfg_";

    // ISO-639 language -> watch language byte (LanguageUtil scheme; 0 = default).
    public static byte languageByte(final String language) {
        if (language == null || language.isEmpty() || "auto".equals(language)) {
            return 0;
        }
        final String lang = language.split("[_-]")[0].toLowerCase(Locale.ROOT);
        if ("zh".equals(lang)) {
            // language handling for Chinese script in AK102
            for (final String part : language.toLowerCase(Locale.ROOT).split("[_-]")) {
                if ("hant".equals(part) || "tw".equals(part) || "hk".equals(part)
                        || "mo".equals(part)) {
                    return 2;
                }
                if ("hans".equals(part)) {
                    return 1;
                }
            }
            return 1;
        }
        final String[] codes = {
                "", "", "", "en", "de", "ru", "es", "pt", "fr", "ja",          // 0..9
                "ar", "nl", "it", "bn", "hr", "cs", "da", "el", "he", "hi",    // 10..19
                "hu", "in", "ko", "ms", "fa", "pl", "ro", "sr", "sv", "th",    // 20..29
                "tr", "ur", "vi", "ca", "lv", "lt", "nb", "sk", "sl", "bg",    // 30..39
                "uk", "tl", "fi", "af", "rm", "my", "km", "am", "be", "et",    // 40..49
                "sw", "zu", "az", "hy", "ka", "lo", "mn", "ne", "kk", "gl",    // 50..59
                "is", "kn", "ky", "ml", "mr", "ta", "mk", "te", "uz", "eu",    // 60..69
                "si", "sq",                                                    // 70..71
        };
        final String iso = "id".equals(lang) ? "in" : lang; // Java legacy code
        for (int i = 3; i < codes.length; i++) {
            if (codes[i].equals(iso)) {
                return (byte) i;
            }
        }
        return 0;
    }

    // Length in bytes of the auth user-id payload (base36 token, zero padded).
    public static final int AUTH_USER_ID_LENGTH = 32;
    public static final int AUTH_USER_ID_MAX = 28;

    // Mirrors FcDeviceInfo.isSupportFeature: three bit regions plus a negated list.
    public static boolean isFeatureSupported(final byte[] info, final int feature) {
        boolean bit = false;
        if (info != null) {
            if (feature < 256) {
                bit = feature <= 31 && flagDescending(info, 9, feature);
            } else if (feature < 512) {
                bit = (feature - 256) <= 47 && flagDescending(info, 37, feature - 256);
            } else if (info.length >= 40) {
                final int index = feature - 512;
                final int count = info[38] & 0xFF;
                final int byteIndex = index / 8;
                if (byteIndex < count && 39 + byteIndex < info.length) {
                    bit = (info[39 + byteIndex] & (1 << (index % 8))) != 0;
                }
            }
        }
        for (final int negated : NEGATED_FEATURES) {
            if (feature == negated) {
                return !bit;
            }
        }
        return bit;
    }

    private static boolean flagDescending(final byte[] data, final int offset, final int index) {
        final int byteIndex = offset - index / 8;
        return byteIndex >= 0 && byteIndex < data.length && (data[byteIndex] & (1 << (index % 8))) != 0;
    }

    private Ak102Constants() {
    }
}
