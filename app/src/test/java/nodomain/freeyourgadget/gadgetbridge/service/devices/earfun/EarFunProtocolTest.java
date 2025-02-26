package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_GAME_MODE;

public class EarFunProtocolTest {

    private EarFunProtocol protocol;
    private GBDevice device;

    @Before
    public void setUp() {
        protocol = new EarFunProtocol(device);
    }

    @Test
    public void testDecodeResponse() {
        byte[] data = {(byte) 0xFF, 0x03, 0x00, 0x02, 0x00, 0x0A, 0x03, 0x07, 0x00, 0x64};

        GBDeviceEvent[] events = protocol.decodeResponse(data);

        assertNotNull(events);
        assertEquals(1, events.length);
        assertTrue(events[0] instanceof GBDeviceEventBatteryInfo);
        GBDeviceEventBatteryInfo batteryInfo = (GBDeviceEventBatteryInfo) events[0];
        assertEquals(1, batteryInfo.batteryIndex);
        assertEquals(100, batteryInfo.level);
    }
}
