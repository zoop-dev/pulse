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
    public static final int HEART_BEAT_DELAY_MS = 28000;
    public static final int DEFAULT_COMMAND_TIMEOUT_MS = 5000;
    public static final int DISPLAY_SETTINGS_PREVIEW_DELAY = 3000;
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

    // Extract the L or R at the end of the device prefix.
    public static Side getSideFromFullName(String deviceName) {
        int prefixSize = "Even G1_XX_X".length();

        if (deviceName.length() < prefixSize) {
            return null;
        }

        String prefix = deviceName.substring(0, prefixSize);
        char side = prefix.charAt(prefix.length() - 1);
        if (side == 'L' || side == 'R') {
            return side == 'L' ? Side.LEFT : Side.RIGHT;
        }

        return null;
    }

    public static String getNameFromFullName(String deviceName) {
        int prefixSize = "Even G1_XX".length();

        if (deviceName.length() < prefixSize) {
            return null;
        }

        return deviceName.substring(0, prefixSize);
    }

    public enum Side {
        INVALID(-1, ""),
        LEFT(0, "left"),
        RIGHT(1, "right");

        private final int deviceIndex;
        private final String stringPrefix;

        Side(int deviceIndex, String stringPrefix) {
            this.deviceIndex = deviceIndex;
            this.stringPrefix = stringPrefix;
        }

        public int getDeviceIndex() {
            return deviceIndex;
        }

        public static String getIndexKey() {
            return "device_index";
        }

        public String getAddressKey() {
            return stringPrefix + "_address";
        }

        public String getNameKey() {
            return stringPrefix + "_name";
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
        public static final byte FAILED = (byte)0xCA;
        public static final byte DATA_CONTINUES = (byte)0xCA;
        public static final byte SUCCESS = (byte)0xC9;
    }

    // TODO: Lifted these from a different project, some of them are wrong.
    public enum CommandId {
        DASHBOARD_CONFIG((byte) 0x06),
        SYNC_SEQUENCE((byte) 0x22), // 0x05
        DASHBOARD_SHOWN((byte) 0x22), // 0x0A
        SYSTEM((byte) 0x23),
        HEARTBEAT((byte) 0x25),
        BATTERY_LEVEL((byte) 0x2C),
        INIT((byte) 0x4D),
        NOTIFICATION((byte) 0x4B),
        FW_INFO_RESPONSE((byte) 0x6E),
        DEBUG_LOG((byte) 0xF4),
        DEVICE_EVENT((byte) 0xF5),
        GET_SILENT_MODE_SETTINGS((byte) 0x2B), // There is more info in this one
        SET_SILENT_MODE_SETTINGS((byte) 0x03),
        GET_DISPLAY_SETTINGS((byte) 0x3B),
        SET_DISPLAY_SETTINGS((byte) 0x26),
        GET_HEAD_GESTURE_SETTINGS((byte) 0x32),
        SET_HEAD_GESTURE_SETTINGS((byte) 0x0B),
        GET_BRIGHTNESS_SETTINGS((byte) 0x29),
        SET_BRIGHTNESS_SETTINGS((byte) 0x01),
        GET_WEAR_DETECTION_SETTINGS((byte) 0x3A),
        SET_WEAR_DETECTION_SETTINGS((byte) 0x27),
        GET_SERIAL_NUMBER((byte) 0x34),
        GET_NOTIFICATION_DISPLAY_SETTINGS((byte) 0x3C),
        SET_NOTIFICATION_DISPLAY_SETTINGS((byte) 0x4F),
        SET_NOTIFICATION_APP_SETTINGS((byte) 0x04),
        SEND_NOTIFICATION((byte) 0x4B),
        SEND_CLEAR_NOTIFICATION((byte) 0x4C);

        final public byte id;

        CommandId(byte id) {
            this.id = id;
        }
    }

    public static class DashboardConfig {
        public static final byte SUB_COMMAND_SET_TIME_AND_WEATHER = 0x01;
        public static final byte SUB_COMMAND_SET_MODE = 0x06;

        public static final byte MODE_FULL = 0x00;
        public static final byte MODE_DUAL = 0x01;
        public static final byte MODE_MINIMAl = 0x02;

        public static final byte PANE_NOTES = 0x00;
        public static final byte PANE_STOCKS = 0x01;
        public static final byte PANE_NEWS = 0x02;
        public static final byte PANE_CALENDAR = 0x03;
        public static final byte PANE_NAVIGATION = 0x04;
        public static final byte PANE_EMPTY = 0x05;

    }

    public enum SystemSubCommand {
        RESET((byte) 0x72),
        GET_FW_INFO((byte) 0x74),
        SET_DEBUG_LOGGING((byte) 0x6C);

        final public byte id;

        SystemSubCommand(byte id) {
            this.id = id;
        }
    }

    public static class SilentStatus {
        public static final byte ENABLE = 0x0C;
        public static final byte DISABLE = 0x0A;
    }

    public static class DebugLoggingStatus {
        public static final byte ENABLE = 0x00;
        public static final byte DISABLE = (byte)0x31;
    }

    public static class DeviceEventId {
        // Used to indicate a double tap, but it was used to close the dashboard.
        public static final byte DOUBLE_TAP_FOR_EXIT = 0x00;
        public static final byte UNKNOWN_1 = 0x01;
        public static final byte HEAD_UP = 0x02;
        public static final byte HEAD_DOWN = 0x03;
        public static final byte SILENT_MODE_ENABLED = 0x04;
        public static final byte SILENT_MODE_DISABLED = 0x05;
        public static final byte GLASSES_WORN = 0x06;
        public static final byte GLASSES_NOT_WORN_NO_CASE = 0x07;
        public static final byte CASE_LID_OPEN = 0x08;
        // Sent with a payload of 00 or 01 to indicate charging state.
        public static final byte GLASSES_CHARGING = 0x09;
        // Comes with a payload 00 - 64
        public static final byte GLASSES_SIDE_BATTERY_LEVEL = 0x0A;
        public static final byte CASE_LID_CLOSE = 0x0B;
        public static final byte UNKNOWN_4 = 0x0C;
        public static final byte UNKNOWN_5 = 0x0D;
        // Sent with a payload of 00 or 01 to indicate charging state.
        public static final byte CASE_CHARGING = 0x0E;
        // Comes with a payload 00 - 64
        public static final byte CASE_BATTERY_LEVEL = 0x0F;
        public static final byte UNKNOWN_6 = 0x10;
        public static final byte BINDING_SUCCESS = 0x11;
        public static final byte DASHBOARD_SHOW = 0x1E;
        public static final byte DASHBOARD_CLOSE = 0x1F;
        // Used to initiate translate or transcribe in the official app.
        // For us it's strictly a double tap that only sends the event.
        public static final byte DOUBLE_TAP_FOR_ACTION = 0x20;
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
