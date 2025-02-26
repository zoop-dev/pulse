package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_AMBIENT_SOUND_CONTROL;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_ANC_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_GAME_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_SINGLE_TAP_LEFT_ACTION;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_TRANSPARENCY_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_TRIPPLE_TAP_RIGHT_ACTION;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;

public class EarFunResponseHandlerTest {
    @Test
    public void testHandleBatteryInfo() {
        byte[] payload = new byte[]{0x00, 0x64};
        GBDeviceEvent event = EarFunResponseHandler.handleBatteryInfo(1, payload);
        GBDeviceEventBatteryInfo batteryInfo = (GBDeviceEventBatteryInfo) event;
        assertEquals(1, batteryInfo.batteryIndex);
        assertEquals(100, batteryInfo.level);
        assertEquals(BatteryState.BATTERY_NORMAL, batteryInfo.state);
    }

    @Test
    public void testHandleFirmwareVersionInfo() {
        byte[] payload = "twa200_20250225_3.4.8".getBytes(StandardCharsets.UTF_8);
        GBDeviceEvent event = EarFunResponseHandler.handleFirmwareVersionInfo(payload);
        GBDeviceEventVersionInfo versionInfo = (GBDeviceEventVersionInfo) event;
        assertEquals("3.4.8", versionInfo.fwVersion);
        assertEquals("twa200 20250225", versionInfo.hwVersion);
    }

    @Test
    public void testHandleGameModeInfo() {
        byte[] payload = new byte[]{0x00, 0x01};
        GBDeviceEvent event = EarFunResponseHandler.handleGameModeInfo(payload);
        GBDeviceEventUpdatePreferences updateEvent = (GBDeviceEventUpdatePreferences) event;
        assertTrue(updateEvent.preferences.containsKey(PREF_EARFUN_GAME_MODE));
        assertEquals(updateEvent.preferences.get(PREF_EARFUN_GAME_MODE), true);
    }

    @Test
    public void testHandleAmbientSoundInfo() {
        byte[] payload = new byte[]{0x00, 0x02};
        GBDeviceEvent event = EarFunResponseHandler.handleAmbientSoundInfo(payload);
        GBDeviceEventUpdatePreferences updateEvent = (GBDeviceEventUpdatePreferences) event;
        assertTrue(updateEvent.preferences.containsKey(PREF_EARFUN_AMBIENT_SOUND_CONTROL));
        assertEquals(updateEvent.preferences.get(PREF_EARFUN_AMBIENT_SOUND_CONTROL), "2");
    }

    @Test
    public void testHandleAncModeInfo() {
        byte[] payload = new byte[]{0x00, 0x04};
        GBDeviceEvent event = EarFunResponseHandler.handleAncModeInfo(payload);
        GBDeviceEventUpdatePreferences updateEvent = (GBDeviceEventUpdatePreferences) event;
        assertTrue(updateEvent.preferences.containsKey(PREF_EARFUN_ANC_MODE));
        assertEquals(updateEvent.preferences.get(PREF_EARFUN_ANC_MODE), "4");
    }

    @Test
    public void testHandleTransparencyModeInfo() {
        byte[] payload = new byte[]{0x00, 0x01};
        GBDeviceEvent event = EarFunResponseHandler.handleTransparencyModeInfo(payload);
        GBDeviceEventUpdatePreferences updateEvent = (GBDeviceEventUpdatePreferences) event;
        assertTrue(updateEvent.preferences.containsKey(PREF_EARFUN_TRANSPARENCY_MODE));
        assertEquals(updateEvent.preferences.get(PREF_EARFUN_TRANSPARENCY_MODE), "1");
    }

    @Test
    public void testHandleTouchActionInfo() {
        byte[] payload = ByteBuffer.allocate(18)
                .put((byte) 0x01)
                .putShort((short) 0x0001)
                .putShort((short) 0x0002)
                .putShort((short) 0x0003)
                .putShort((short) 0x0004)
                .putShort((short) 0x0005)
                .putShort((short) 0x0006)
                .putShort((short) 0x0007)
                .putShort((short) 0x0008)
                .array();
        GBDeviceEvent event = EarFunResponseHandler.handleTouchActionInfo(payload);
        GBDeviceEventUpdatePreferences updateEvent = (GBDeviceEventUpdatePreferences) event;
        assertEquals(8, updateEvent.preferences.size());
        assertEquals(updateEvent.preferences.get(PREF_EARFUN_SINGLE_TAP_LEFT_ACTION), "1");
        assertEquals(updateEvent.preferences.get(PREF_EARFUN_TRIPPLE_TAP_RIGHT_ACTION), "6");
    }
}
