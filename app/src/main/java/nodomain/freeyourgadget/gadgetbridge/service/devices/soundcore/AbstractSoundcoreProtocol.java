package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore;

import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public abstract class AbstractSoundcoreProtocol extends GBDeviceProtocol {

    protected AbstractSoundcoreProtocol(GBDevice device) {
        super(device);
    }

    protected GBDeviceEventBatteryInfo buildBatteryInfo(int batteryIndex, int level) {
        GBDeviceEventBatteryInfo info = new GBDeviceEventBatteryInfo();
        info.batteryIndex = batteryIndex;
        info.level = level;
        return info;
    }

    protected GBDeviceEventVersionInfo buildVersionInfo(String firmware1, String firmware2, String serialNumber) {
        GBDeviceEventVersionInfo info = new GBDeviceEventVersionInfo();
        info.hwVersion = serialNumber;
        info.fwVersion = firmware1;
        info.fwVersion2 = firmware2;
        return info;
    }

    protected String readString(byte[] data, int position, int size) {
        if (position + size > data.length) throw new IllegalStateException();
        return new String(data, position, size, StandardCharsets.UTF_8);
    }

    protected byte encodeBoolean(boolean bool) {
        if (bool) return 0x01;
        else return 0x00;
    }

    public byte[] encodeDeviceInfoRequest() {
        return new SoundcorePacket((short) 0x0101).encode();
    }

}
