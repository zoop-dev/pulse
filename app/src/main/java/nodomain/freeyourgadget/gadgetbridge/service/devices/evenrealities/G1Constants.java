package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import java.util.UUID;

public class G1Constants {
    public static final UUID UUID_SERVICE_NORDIC_UART =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_NORDIC_UART_TX =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_NORDIC_UART_RX =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final int MTU = 251;
    public static final int HEART_BEAT_DELAY_MS = 28000;
    public static final int DEFAULT_COMMAND_TIMEOUT_MS = 5000;
    public static final int DISPLAY_SETTINGS_PREVIEW_DELAY = 3000;
    public static final int DEFAULT_RETRY_COUNT = 5;
    public static final String INTENT_TOGGLE_SILENT_MODE = "nodomain.freeyourgadget.gadgetbridge.evenrealities.silent_mode";

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

    public static class CommandStatus {
        public static final byte FAILED = (byte)0xCA;
        public static final byte DATA_CONTINUES = (byte)0xCA;
        public static final byte SUCCESS = (byte)0xC9;
    }

    // TODO: Lifted these from a different project, some of them are wrong.
    public enum CommandId {
        NOTIFICATION_CONFIG((byte) 0x04),
        DASHBOARD_CONFIG((byte) 0x06),
        DASHBOARD((byte) 0x22),
        FW_INFO_REQUEST((byte) 0x23),
        HEARTBEAT((byte) 0x25),
        BATTERY_LEVEL((byte) 0x2C),
        INIT((byte) 0x4D),
        NOTIFICATION((byte) 0x4B),
        FW_INFO_RESPONSE((byte) 0x6E),
        DEVICE_ACTION((byte) 0xF5),
        GET_SILENT_MODE_SETTINGS((byte) 0x2B), // There is more info in this one
        SET_SILENT_MODE_SETTINGS((byte) 0x03),
        GET_DISPLAY_SETTINGS((byte) 0x3B),
        SET_DISPLAY_SETTINGS((byte) 0x26),
        GET_HEAD_GESTURE_SETTINGS((byte) 0x32),
        SET_HEAD_GESTURE_SETTINGS((byte) 0x0B),
        GET_BRIGHTNESS_SETTINGS((byte) 0x29),
        SET_BRIGHTNESS_SETTINGS((byte) 0x01),
        GET_WEAR_DETECTION_SETTINGS((byte) 0x3A),
        SET_WEAR_DETECTION_SETTINGS((byte) 0x27);

        final public byte id;

        CommandId(byte id) {
            this.id = id;
        }
    }

    public enum DashboardConfigSubCommand {
        SET_MODE((byte) 0x07),
        UNKNOWN_1((byte) 0x0C),
        SET_TIME_AND_WEATHER((byte) 0x15),
        // Not sure why they use this one sometimes.
        SET_TIME_AND_WEATHER_ALSO((byte) 0x16);

        final public byte id;

        DashboardConfigSubCommand(byte id) {
            this.id = id;
        }
    }

    public static class SilentStatus {
        public static final byte ENABLE = 0x0C;
        public static final byte DISABLE = 0x0A;
    }
}