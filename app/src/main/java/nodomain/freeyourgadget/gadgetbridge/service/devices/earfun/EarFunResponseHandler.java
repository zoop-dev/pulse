package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_AMBIENT_SOUND_CONTROL;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_ANC_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_AUDIO_CODEC;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_CONNECT_TWO_DEVICES_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_GAME_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_ADVANCED_AUDIO_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_IN_EAR_DETECTION_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_MICROPHONE_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_FIND_DEVICE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_TOUCH_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_TRANSPARENCY_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.PREF_EARFUN_VOICE_PROMPT_VOLUME;
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

    public static GBDeviceEvent handleInEarDetectionModeInfo(byte[] payload) {
        // "0" means in ear detection is enabled
        boolean inEarDetectionMode = payload[1] == 0x00;
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_IN_EAR_DETECTION_MODE, inEarDetectionMode);
    }

    public static GBDeviceEvent handleTouchModeInfo(byte[] payload) {
        // "0" means touch is enabled
        boolean touchMode = payload[1] == 0x00;
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_TOUCH_MODE, touchMode);
    }

    public static GBDeviceEvent handleConnectTwoDevicesModeInfo(byte[] payload) {
        boolean touchMode = payload[1] == 0x01;
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_CONNECT_TWO_DEVICES_MODE, touchMode);
    }

    public static GBDeviceEvent handleAdvancedAudioModeInfo(byte[] payload) {
        // 00 = Google Fast Pair, 01 = LE Audio
        String advancedAudioMode = Integer.toString(payload[1]);
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_ADVANCED_AUDIO_MODE, advancedAudioMode);
    }

    public static GBDeviceEvent handleAudioCodecInfo(byte[] payload) {
        String audioCodec = Integer.toString(payload[1]);
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_AUDIO_CODEC, audioCodec);
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

    public static GBDeviceEvent handleMicrophoneModeInfo(byte[] payload) {
        String microphoneMode = Integer.toString(payload[1]);
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_MICROPHONE_MODE, microphoneMode);
    }

    public static GBDeviceEvent handleFindDeviceInfo(byte[] payload) {
        String findDeviceMode = Integer.toString(payload[1]);
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_FIND_DEVICE, findDeviceMode);
    }

    public static GBDeviceEvent handleVoicePromptVolumeInfo(byte[] payload) {
        // the volume has a scale from 4 to 0, where 0 is the highest volume and 4 is off,
        // to make it nicer for a slider, we reverse it
        int voicePromptVolume = payload[1];
        voicePromptVolume = Math.max(0, 4 - voicePromptVolume);
        return new GBDeviceEventUpdatePreferences(PREF_EARFUN_VOICE_PROMPT_VOLUME, voicePromptVolume);
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
