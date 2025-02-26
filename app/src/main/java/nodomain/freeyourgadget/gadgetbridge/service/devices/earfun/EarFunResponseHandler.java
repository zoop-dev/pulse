package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_AMBIENT_SOUND_CONTROL;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_ANC_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_GAME_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_TRANSPARENCY_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Interactions.interactionPrefs;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;

public class EarFunResponseHandler {

    public static GBDeviceEvent handleBatteryInfo(int index, byte[] payload) {
        int batteryLevel = payload[1];
        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
        batteryInfo.batteryIndex = index;
        batteryInfo.level = (batteryLevel > 0) ? batteryLevel : GBDevice.BATTERY_UNKNOWN;
        batteryInfo.state = (batteryLevel > 0) ? BatteryState.BATTERY_NORMAL : BatteryState.UNKNOWN;
        return batteryInfo;
    }

    public static GBDeviceEvent handleFirmwareVersionInfo(byte[] payload) {
        final GBDeviceEventVersionInfo versionInfo = new GBDeviceEventVersionInfo();
        String[] versionParts = (new String(payload, StandardCharsets.UTF_8)).split("_");
        versionInfo.fwVersion = versionParts[versionParts.length - 1];
        if (versionParts.length > 1) {
            versionInfo.hwVersion = String.join(" ", Arrays.copyOf(versionParts, versionParts.length - 1));
        } else {
            versionInfo.hwVersion = GBApplication.getContext().getString(R.string.n_a);
        }
        return versionInfo;
    }

    public static GBDeviceEvent handleGameModeInfo(byte[] payload) {
        boolean gameMode = payload[1] == 0x01;
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_GAME_MODE, gameMode);
    }

    public static GBDeviceEvent handleAmbientSoundInfo(byte[] payload) {
        String ambientSoundMode = Integer.toString(payload[1]);
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_AMBIENT_SOUND_CONTROL, ambientSoundMode);
    }

    public static GBDeviceEvent handleAncModeInfo(byte[] payload) {
        String ancMode = Integer.toString(payload[1]);
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_ANC_MODE, ancMode);
    }

    public static GBDeviceEvent handleTransparencyModeInfo(byte[] payload) {
        String transparencyMode = Integer.toString(payload[1]);
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_TRANSPARENCY_MODE, transparencyMode);
    }

    public static GBDeviceEvent handleTouchActionInfo(byte[] payload) {
        GBDeviceEventUpdatePreferences updateEvent = new GBDeviceEventUpdatePreferences();
        ByteBuffer buf = ByteBuffer.wrap(payload);
        buf.get();
        Arrays.stream(interactionPrefs).forEach(interactionType -> {
            // only use the last byte - the first byte initially contains some non zero data
            int action = buf.getShort() & 0xFF;
            updateEvent.preferences.put(interactionType, Integer.toString(action));
        });
        return updateEvent;
    }
}
