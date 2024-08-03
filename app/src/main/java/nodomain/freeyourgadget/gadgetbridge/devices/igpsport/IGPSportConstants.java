package nodomain.freeyourgadget.gadgetbridge.devices.igpsport;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;

public class IGPSportConstants {

    public static final UUID UUID_IGPSPORT_SERVICE_BATTERY = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_IGPSPORT_BATTERY_INFO = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_IGPSPORT_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_CONTROL = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_IGPSPORT_CHARACTERISTIC_REPORT = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
}
