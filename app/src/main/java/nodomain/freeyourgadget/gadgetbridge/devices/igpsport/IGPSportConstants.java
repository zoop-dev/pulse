package nodomain.freeyourgadget.gadgetbridge.devices.igpsport;

import java.util.UUID;

public class IGPSportConstants {

    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_FIRST_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_FIRST_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_FIRST_RX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_SECOND_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca8e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_SECOND_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca8e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_SECOND_RX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca8e");

    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_THIRD_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca7e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_THIRD_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca7e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_THIRD_RX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca7e");

    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_FORTH_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca6e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_FOURTH_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca6e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_FOURTH_RX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca6e");


    public static final byte DATA_HEADER = (byte) 0x01;


    public static final byte[] DATA_TEMPLATE = {
            (byte) DATA_HEADER, // header
            (byte) 0xFF, // main service byte
            (byte) 0xFF, // second service byte
            (byte) 0xFF, // byte3 - FF in common operations, has some value in files operations
            (byte) 0xFF, // main command byte
            (byte) 0xFF, // second common byte
            (byte) 0xFF, // always FF
            (byte) 0x00, //payload size high byte
            (byte) 0xFF, //payload size low byte
            (byte) 0xFF, // crc8 for ayload
            (byte) 0x01, //always 0x01 in common packets
            (byte) 0xFF, //reserved padding
            (byte) 0xFF, //reserved padding
            (byte) 0xFF, //reserved padding
            (byte) 0xFF, //reserved padding
            (byte) 0xFF, //reserved padding
            (byte) 0xFF, //reserved padding
            (byte) 0xFF, //reserved padding
            (byte) 0xFF, //reserved padding
            (byte) 0xFF, //header crc8
            //protobuf data payload
    };

}
