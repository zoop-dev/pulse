package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import java.util.UUID;

public class G1DeviceConstants {
    public static final UUID UUID_SERVICE_NORDIC_UART =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_NORDIC_UART_TX =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_NORDIC_UART_RX =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final int MTU = 251;

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
        LEFT,
        RIGHT;
    }


    // TODO: Lifted these from a different project, some of them are wrong.
    public enum CommandId {
        BATTERY_LEVEL((byte) 0x2C),
        WEATHER_AND_TIME((byte) 0x06),
        START_AI((byte) 0xF5),
        OPEN_MIC((byte) 0x0E),
        MIC_RESPONSE((byte) 0x0E),
        RECEIVE_MIC_DATA((byte) 0xF1),
        INIT((byte) 0x4D),
        HEARTBEAT((byte) 0x25),
        SEND_RESULT((byte) 0x4E),
        QUICK_NOTE((byte) 0x21),
        DASHBOARD((byte) 0x22),
        NOTIFICATION((byte) 0x4B),
        BMP((byte) 0x15),
        FW_INFO_REQUEST((byte) 0x23),
        FW_INFO_RESPONSE((byte) 0x6E),
        CRC((byte) 0x16);

        final public byte id;

        CommandId(byte id) {
            this.id = id;
        }
    }
}
