package nodomain.freeyourgadget.gadgetbridge.service.devices.pixel;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class PixelBudsAProtocol extends GBDeviceProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(PixelBudsAProtocol.class);

    private static final int battery_case = 0;
    private static final int battery_earphone_left = 1;
    private static final int battery_earphone_right = 2;

    protected PixelBudsAProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        List<GBDeviceEvent> devEvts = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(responseData);

        while (buf.hasRemaining()) { // one response can contain multiple commands
            int cmd = buf.getShort(); // first 2 bytes define what the payload contains
            int size = buf.getShort(); // next 2 bytes define how many bytes belong to this command
            byte[] payload = new byte[size];
            buf.get(payload); // next `size` bytes

            devEvts.add(buildVersionInfo("unknown", "unknown", "unknown"));

            if (cmd == 0x0303) { // Battery Update
                int charge_left = payload[0];
                int charge_right = payload[1];
                int charge_case = payload[2];

                if (charge_case >= 0) { // is -1 when no bud is in case
                    devEvts.add(buildBatteryInfo(battery_case, charge_case));
                }
                if (charge_left >= 0) { // is -28 when bud is in case
                    devEvts.add(buildBatteryInfo(battery_earphone_left, charge_left));
                }
                if (charge_right >= 0) { // is -28 when bud is in case
                    devEvts.add(buildBatteryInfo(battery_earphone_right, charge_right));
                }
            } else {
                LOG.debug("Unknown incoming message - dump: " + hexdump(responseData));
            }
        }
        return devEvts.toArray(new GBDeviceEvent[devEvts.size()]);
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

}
