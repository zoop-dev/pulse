package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import android.util.Pair;

import java.util.UUID;

public class G1Constants {
    public static final UUID UUID_SERVICE_NORDIC_UART =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_NORDIC_UART_TX =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_NORDIC_UART_RX =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final int MTU = 251;
    // The MTU is set to 251, which suggests there should be a larger packet size, however,
    // (excluding headers) the glasses only ever send a payload of 180 bytes.
    // TODO: Try out a larger MTU and a larger packet size to see if these numbers are flexible.
    //       It could be that a 180 byte buffer is allocated in the FW for the payload and sending
    //       more will cause the glasses to crash.
    public static final int MAX_PACKET_SIZE_BYTES = 180;
    // YUCK! The glasses require a constant connection otherwise they will completely shut down.
    // Every 32 seconds (BLE timeout), a packet MUST be sent to the glasses to keep the connection
    // alive. Due to scheduling of background threads in Android, the system can add upwards of 20
    // seconds to the sleep time. There is not a great way around this, so the sleep time is set to
    // 8 seconds. In deep sleep, that means the connection is generally refreshed every 28 seconds.
    // Occasionally, the timeout will be exceeded, in which case we rely on the auto reconnection,
    // however that process requires full initialization of the glasses, and as such it is more
    // expensive from a battery budget than just sending the heartbeat message.
    public static final int HEART_BEAT_BASE_DELAY_MS = 8000;
    public static final int HEART_BEAT_TARGET_DELAY_MS = 25000;
    public static final int HEART_BEAT_MAX_DELAY_MODIFIER_MS = 10000;
    public static final int DEFAULT_COMMAND_TIMEOUT_MS = 5000;
    public static final int DISPLAY_SETTINGS_PREVIEW_DELAY = 5000;
    public static final int DEFAULT_RETRY_COUNT = 5;
    public static final int CASE_BATTERY_INDEX = 2;
    public static final String INTENT_TOGGLE_SILENT_MODE = "nodomain.freeyourgadget.gadgetbridge.evenrealities.silent_mode";
    // The glasses have a filter based on a whitelist of apps and it will only display
    // notifications from apps that are on that list. GadgetBridge already filters the
    // notifications before sending to the glasses and GadgetBridge can work as either a
    // blacklist or a whitelist. To get around this, a fixed application id is used since the
    // glasses don't display it, it doesn't matter to the user experience and it allows all of
    // the notification filtering to happen on the phone side.
    public static final Pair<String, String>
            FIXED_NOTIFICATION_APP_ID = new Pair<>("nodomain.freeyourgadget.gadget", "Name");

    // Only 4 pages with two events each is supported.
    public static final int MAX_CALENDAR_EVENTS = 8;
    // Show calendar events for 5 minutes after they have started.
    public static final int CALENDAR_EVENT_CLEAR_DELAY = 5 * 1000 * 60;

    // Extract the L or R at the end of the device prefix.
    public static Side getSideFromFullName(String deviceName) {
        // Name will be "G1_XX_[L|R]_YYYYY"
        int firstUnderScore = deviceName.indexOf('_');
        if (firstUnderScore < 0) return null;
        int prefixSize = deviceName.indexOf('_', firstUnderScore + 1);
        if (prefixSize < 0) return null;

        char side = deviceName.charAt(prefixSize + 1);
        if (side == 'L' || side == 'R') {
            return side == 'L' ? Side.LEFT : Side.RIGHT;
        }

        return null;
    }

    public static String getNameFromFullName(String deviceName) {
        // Name will be "G1_XX_[L|R]_YYYYY"
        int firstUnderScore = deviceName.indexOf('_');
        if (firstUnderScore < 0) return null;
        int prefixSize = deviceName.indexOf('_', firstUnderScore + 1);
        if (prefixSize < 0) return null;

        return deviceName.substring(0, prefixSize);
    }

    public enum Side {
        INVALID(-1, ""),
        LEFT(0, "left"),
        RIGHT(1, "right");

        private final int deviceIndex;
        private final String name;

        Side(int deviceIndex, String name) {
            this.deviceIndex = deviceIndex;
            this.name = name;
        }

        public int getDeviceIndex() {
            return deviceIndex;
        }

        public static String getIndexKey() {
            return "device_index";
        }

        public String getAddressKey() {
            return name + "_address";
        }

        public String getNameKey() {
            return name + "_name";
        }

        public String getName() {
            return name;
        }
    }

    public static class HardwareDescriptionKey {
        public static final String FRAME_ROUND = "S100";
        public static final String FRAME_SQUARE = "S110";
        public static final String COLOR_GREY = "LAA";
        public static final String COLOR_BROWN = "LBB";
        public static final String COLOR_GREEN = "LCC";
    }

    public static class CommandStatus {
        public static final byte SUCCESS = (byte)0xC9;
        public static final byte FAIL = (byte)0xCA;
        public static final byte DATA_CONTINUES = (byte)0xCB;
    }

    public static class MessageId {
        public static final byte STATUS = (byte)0x22;
        public static final byte AUDIO = (byte)0xF1;
        public static final byte DEBUG = (byte)0xF4;
        public static final byte EVENT = (byte)0xF5;
    };

    public static class CommandId {
        public static final byte ANTI_SHAKE_GET = (byte)0x2A;
        public static final byte BITMAP_SHOW = (byte)0x16;
        public static final byte BITMAP_HIDE = (byte)0x18;
        public static final byte BRIGHTNESS_GET = (byte)0x29;
        public static final byte BRIGHTNESS_SET = (byte)0x01;
        public static final byte DASHBOARD_SET = (byte)0x06;
        public static final byte DASHBOARD_QUICK_NOTE_CONTROL = (byte)0x1E;
        public static final byte DASHBOARD_CALENDAR_NEXT_UP_SET = (byte)0x58;
        public static final byte FILE_UPLOAD = (byte)0x15;
        public static final byte FILE_UPLOAD_COMPLETE = (byte)0x20;
        public static final byte HARDWARE_GET = (byte)0x3F;
        public static final byte HARDWARE_SET = (byte)0x26;
        public static final byte HARDWARE_DISPLAY_GET = (byte)0x3B;
        public static final byte HEAD_UP_ANGLE_GET = (byte)0x32;
        public static final byte HEAD_UP_ANGLE_SET = (byte)0x0B;
        public static final byte HEAD_UP_ACTION_SET = (byte)0x08;
        public static final byte HEAD_UP_CALIBRATION_CONTROL = (byte)0x10;
        public static final byte INFO_BATTERY_AND_FIRMWARE_GET = (byte)0x2C;
        public static final byte INFO_MAC_ADDRESS_GET = (byte)0x2D;
        public static final byte INFO_SERIAL_NUMBER_LENS_GET = (byte)0x33;
        public static final byte INFO_SERIAL_NUMBER_GLASSES_GET = (byte)0x34;
        public static final byte INFO_ESB_CHANNEL_GET = (byte)0x35;
        public static final byte INFO_ESB_NOTIFICATION_COUNT_GET = (byte)0x36;
        public static final byte INFO_TIME_SINCE_BOOT_GET = (byte)0x37;
        public static final byte INFO_BURIED_POINT_GET = (byte)0x3E;
        public static final byte LANGUAGE_SET = (byte)0x3D;
        public static final byte MICROPHONE_SET = (byte)0x0E;
        public static final byte MTU_SET = (byte)0x4D;
        public static final byte NAVIGATION_CONTROL = (byte)0x0A;
        public static final byte NOTIFICATION_APP_LIST_GET = (byte)0x2E;
        public static final byte NOTIFICATION_APP_LIST_SET = (byte)0x04;
        public static final byte NOTIFICATION_APPLE_GET = (byte)0x38;
        public static final byte NOTIFICATION_AUTO_DISPLAY_GET = (byte)0x3C;
        public static final byte NOTIFICATION_AUTO_DISPLAY_SET = (byte)0x4F;
        public static final byte NOTIFICATION_SEND_CONTROL = (byte)0x4B;
        public static final byte NOTIFICATION_CLEAR_CONTROL = (byte)0x4C;
        public static final byte SILENT_MODE_GET = (byte)0x2B;
        public static final byte SILENT_MODE_SET = (byte)0x03;
        public static final byte STATUS_GET = (byte)0x22;
        public static final byte STATUS_RUNNING_APP_GET = (byte)0x39;
        public static final byte SYSTEM_CONTROL = (byte)0x23;
        public static final byte TELEPROMPTER_CONTROL = (byte)0x09;
        public static final byte TELEPROMPTER_SUSPEND = (byte)0x24;
        public static final byte TELEPROMPTER_POSITION_SET = (byte)0x25;
        public static final byte TEXT_SET = (byte)0x4E;
        public static final byte TIMER_CONTROL = (byte)0x07;
        public static final byte TRANSCRIBE_CONTROL = (byte)0x0D;
        public static final byte TRANSLATE_CONTROL = (byte)0x0F;
        public static final byte TUTORIAL_CONTROL = (byte)0x1F;
        public static final byte UNPAIR = (byte)0x47;
        public static final byte UPGRADE_CONTROL = (byte)0x17;
        public static final byte WEAR_DETECTION_GET = (byte)0x3A;
        public static final byte WEAR_DETECTION_SET = (byte)0x27;
        public static final byte UNKNOWN = (byte)0x50;
    };

    public static class DashboardMode {
        public static final byte FULL = 0x00;
        public static final byte DUAL = 0x01;
        public static final byte MINIMAl = 0x02;
    }

    public static class DashboardPaneMode {
        public static final byte QUICK_NOTES = 0x00;
        public static final byte STOCKS = 0x01;
        public static final byte NEWS = 0x02;
        public static final byte CALENDAR = 0x03;
        public static final byte MAP = 0x04;
        public static final byte EMPTY = 0x05;
    }

    public static class DashboardSetSubcommand {
        public static final byte TIME_AND_WEATHER = 0x01;
        public static final byte WEATHER = 0x02;
        public static final byte CALENDAR = 0x03;
        public static final byte STOCKS = 0x04;
        public static final byte NEWS = 0x05;
        public static final byte MODE = 0x06;
        public static final byte MAP = 0x07;
    }

    public static class DashboardQuickNoteSubcommand {
        public static final byte AUDIO_METADATA_GET = 0x01;
        public static final byte AUDIO_FILE_GET = 0x02;
        // This one has additional subcommands to delete, update or add a note.
        public static final byte NOTE_TEXT_EDIT = 0x03;
        public static final byte AUDIO_FILE_DELETE = 0x04;
        // Does this delete the metadata?
        public static final byte AUDIO_RECORD_DELETE = 0x05;
        // Set or clear a checkmark in front of the entry.
        public static final byte NOTE_STATUS_EDIT = 0x07;
        public static final byte NOTE_ADD = 0x08;
        public static final byte UNKNOWN = 0x09;
        public static final byte NOTE_STATUS_EDIT_2 = 0x0A;
    }

    public static class HardwareSubcommand {
//        public static final byte UNKNOWN = 0x01;
        public static final byte DISPLAY = 0x02;
//        public static final byte MIC_MUTEX_RELEASE = 0x03;
        public static final byte LUM_GEAR = 0x04;
        public static final byte DOUBLE_TAP_ACTION = 0x05;
        public static final byte LUM_COEFFICIENT = 0x06;
        public static final byte LONG_PRESS_ACTION = 0x07;
        public static final byte HEAD_UP_MIC_ACTIVATION = 0x08;
    }

    public static class SystemSubcommand {
        public static final byte DEBUG_LOGGING_SET = 0x6C;
        public static final byte REBOOT = 0x72;
        public static final byte FIRMWARE_BUILD_STRING_GET = 0x74;
    }

    public static final byte SYSTEM_FIRMWARE_BUILD_STRING_PREFIX = 0x6E;

    public static class LanguageId {
        public static final byte CHINESE = 0x01;
        public static final byte ENGLISH = 0x02;
        public static final byte JAPANESE = 0x03;
        public static final byte KOREAN = 0x04;
        public static final byte FRENCH = 0x05;
        public static final byte GERMAN = 0x06;
        public static final byte SPANISH = 0x07;
        public static final byte ITALIAN = 0x0E;
    }

    public static class NavigationSubcommand {
        public static final byte INIT = 0x00;
        public static final byte TRIP_STATUS = 0x01;
        public static final byte MAP_OVERVIEW = 0x02;
        public static final byte PANORAMIC_MAP = 0x03;
        public static final byte SYNC = 0x04;
        public static final byte EXIT = 0x05;
        public static final byte ARRIVED = 0x06;
    }

    public static class TextDisplayStyle {
//        public static final byte UNKNOWN = 0x00;
//        public static final byte UNKNOWN = 0x10;
//        public static final byte UNKNOWN = 0x20;
        public static final byte AI_DISPLAY_AUTO_SCROLL = 0x30;
        public static final byte AI_DISPLAY_COMPLETE = 0x40;
        public static final byte AI_DISPLAY_MANUAL_SCROLL = 0x50;
        public static final byte AI_NETWORK_ERROR = 0x60;
        public static final byte TEXT_ONY = 0x70;
    }

    public static class SilentStatus {
        public static final byte ENABLE = 0x0C;
        public static final byte DISABLE = 0x0A;
    }

    public static class DebugLoggingStatus {
        public static final byte ENABLE = 0x00;
        public static final byte DISABLE = (byte)0x31;
    }

    public static class EventId {
        // Used to indicate a double tap, but it was used to close the dashboard.
        public static final byte ACTION_DOUBLE_TAP_FOR_EXIT = 0x00;
        public static final byte ACTION_SINGLE_TAP = 0x01;
        public static final byte ACTION_HEAD_UP = 0x02;
        public static final byte ACTION_HEAD_DOWN = 0x03;
        public static final byte ACTION_SILENT_MODE_ENABLED = 0x04;
        public static final byte ACTION_SILENT_MODE_DISABLED = 0x05;
        public static final byte STATE_WORN = 0x06;
        public static final byte STATE_NOT_WORN_NO_CASE = 0x07;
        public static final byte STATE_IN_CASE_LID_OPEN = 0x08;
        // Sent with a payload of 00 or 01 to indicate charging state.
        public static final byte STATE_CHARGING = 0x09;
        // Comes with a payload 00 - 64
        public static final byte INFO_BATTERY_LEVEL = 0x0A;
        public static final byte STATE_IN_CASE_LID_CLOSED = 0x0B;
        public static final byte UNKNOWN_4 = 0x0C;
        public static final byte UNKNOWN_5 = 0x0D;
        // Sent with a payload of 00 or 01 to indicate charging state.
        public static final byte STATE_CASE_CHARGING = 0x0E;
        // Comes with a payload 00 - 64
        public static final byte INFO_CASE_BATTERY_LEVEL = 0x0F;
        public static final byte UNKNOWN_6 = 0x10;
        public static final byte ACTION_BINDING_SUCCESS = 0x11;
        // ACTION_LONG_PRESS_HELD will be sent when the long press is started, ACTION_LONG_PRESS and
        // ACTION_LONG_PRESS_RELEASED will be sent on release.
        public static final byte ACTION_LONG_PRESS = 0x12;
        public static final byte ACTION_LONG_PRESS_HELD = 0x17;
        public static final byte ACTION_LONG_PRESS_RELEASED = 0x18;
        public static final byte ACTION_DOUBLE_TAP_DASHBOARD_SHOW = 0x1E;
        public static final byte ACTION_DOUBLE_TAP_DASHBOARD_CLOSE = 0x1F;
        // Used to initiate translate or transcribe in the official app.
        // For us it's strictly a double tap that only sends the event.
        public static final byte ACTION_DOUBLE_TAP = 0x20;
    }

    public static class TemperatureUnit {
        public static final byte CELSIUS = 0x00;
        public static final byte FAHRENHEIT = 0x01;
    }

    public static class TimeFormat {
        public static final byte TWELVE_HOUR = 0x00;
        public static final byte TWENTY_FOUR_HOUR = 0x01;
    }

    public static class WeatherId {
        public static final byte NONE = 0x00;
        public static final byte NIGHT = 0x01;
        public static final byte CLOUDS = 0x02;
        public static final byte DRIZZLE = 0x03;
        public static final byte HEAVY_DRIZZLE = 0x04;
        public static final byte RAIN = 0x05;
        public static final byte HEAVY_RAIN = 0x06;
        public static final byte THUNDER = 0x07;
        public static final byte THUNDERSTORM = 0x08;
        public static final byte SNOW = 0x09;
        public static final byte MIST = 0x0A;
        public static final byte FOG = 0x0B;
        public static final byte SAND = 0x0C;
        public static final byte SQUALLS = 0x0D;
        public static final byte TORNADO = 0x0E;
        public static final byte FREEZING_RAIN = 0x0F;
        public static final byte SUNNY = 0x10;
    }

    public static byte fromOpenWeatherCondition(int openWeatherMapCondition) {
        // http://openweathermap.org/weather-conditions
        switch (openWeatherMapCondition) {
            //Group 2xx: Thunderstorm
            case 200:  //thunderstorm with light rain:
            case 201:  //thunderstorm with rain:
            case 202:  //thunderstorm with heavy rain:
            case 210:  //light thunderstorm::
            case 211:  //thunderstorm:
            case 230:  //thunderstorm with light drizzle:
            case 231:  //thunderstorm with drizzle:
            case 232:  //thunderstorm with heavy drizzle:
            case 212:  //heavy thunderstorm:
            case 221:  //ragged thunderstorm:
                return WeatherId.THUNDERSTORM;
            //Group 3xx: Drizzle
            case 300:  //light intensity drizzle:
            case 301:  //drizzle:
            case 310:  //light intensity drizzle rain:
                return WeatherId.DRIZZLE;
            case 302:  //heavy intensity drizzle:
            case 311:  //drizzle rain:
            case 312:  //heavy intensity drizzle rain:
            case 313:  //shower rain and drizzle:
            case 314:  //heavy shower rain and drizzle:
            case 321:  //shower drizzle:
                return WeatherId.HEAVY_DRIZZLE;
            //Group 5xx: Rain
            case 500:  //light rain:
            case 501:  //moderate rain:
                return WeatherId.RAIN;
            case 502:  //heavy intensity rain:
            case 503:  //very heavy rain:
            case 504:  //extreme rain:
            case 511:  //freezing rain:
            case 520:  //light intensity shower rain:
            case 521:  //shower rain:
            case 522:  //heavy intensity shower rain:
            case 531:  //ragged shower rain:
                return WeatherId.HEAVY_RAIN;
            //Group 6xx: Snow
            case 600:  //light snow:
            case 601:  //snow:
            case 602:  //heavy snow:
                return WeatherId.SNOW;
            case 611:  //sleet:
            case 612:  //shower sleet:
            case 615:  //light rain and snow:
            case 616:  //rain and snow:
            case 620:  //light shower snow:
            case 621:  //shower snow:
            case 622:  //heavy shower snow:
                return WeatherId.FREEZING_RAIN;
            //Group 7xx: Atmosphere
            case 701:  //mist:
                return WeatherId.MIST;
            case 711:  //smoke:
                return WeatherId.FOG;
            case 721:  //haze:
                return WeatherId.MIST;
            case 731:  //sandcase  dust whirls:
                return WeatherId.SAND;
            case 741:  //fog:
                return WeatherId.FOG;
            case 751:  //sand:
            case 761:  //dust:
            case 762:  //volcanic ash:
                return WeatherId.SAND;
            case 771:  //squalls:
                return WeatherId.SQUALLS;
            case 781:  //tornado:
            case 900:  //tornado
                return WeatherId.TORNADO;
            //Group 800: Clear
            case 800:  //clear sky:
                return WeatherId.SUNNY;
            //Group 80x: Clouds
            case 801:  //few clouds:
            case 802:  //scattered clouds:
            case 803:  //broken clouds:
            case 804:  //overcast clouds:
                return WeatherId.CLOUDS;
            //Group 90x: Extreme
            case 903:  //cold
                return WeatherId.SNOW;
            case 904:  //hot
                return WeatherId.SUNNY;
            case 905:  //windy
                return WeatherId.NONE;
            case 906:  //hail
                return WeatherId.THUNDERSTORM;
            //Group 9xx: Additional
            case 951:  //calm
                return WeatherId.SUNNY;
            case 952:  //light breeze
            case 953:  //gentle breeze
            case 954:  //moderate breeze
            case 955:  //fresh breeze
            case 956:  //strong breeze
            case 957:  //high windcase  near gale
            case 958:  //gale
                return WeatherId.SQUALLS;
            case 901:  //tropical storm
            case 959:  //severe gale
            case 960:  //storm
            case 961:  //violent storm
            case 902:  //hurricane
            case 962:  //hurricane
                return WeatherId.TORNADO;
            default:
                return WeatherId.SUNNY;
        }

    }
}
