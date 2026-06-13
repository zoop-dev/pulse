package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.SoundcorePacket;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.protocol.impl.v1.SoundcoreProtocolImplV1;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SoundcoreLibertyProtocol extends SoundcoreProtocolImplV1 {

    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreLibertyProtocol.class);

    private static final short CMD_GET_UNKNOWN_DATA_8D01 = (short) 0x8d01;
    private static final short CMD_GET_UNKNOWN_DATA_8205 = (short) 0x8205;
    private static final short CMD_SET_AMBIENT_SOUND_CONTROL_BUTTON_MODE = (short) 0x8206;
    private static final short CMD_SET_TOUCH_LOCK = (short) 0x8304;
    private static final short CMD_SET_WEARING_DETECTION = (short) 0x8101;
    private static final short CMD_SET_WEARING_TONE = (short) 0x8c01;

    private static final int battery_case = 0;
    private static final int battery_earphone_left = 1;
    private static final int battery_earphone_right = 2;

    protected SoundcoreLibertyProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        SoundcorePacket packet = decodePacket(responseData);

        if (packet == null)
            return null;

        List<GBDeviceEvent> devEvts = new ArrayList<>();
        short cmd = packet.getCommand();
        byte[] payload = packet.getPayload();

        if (cmd == CMD_GET_DEVICE_INFO) {
            int batteryLeft = payload[2] * 20;
            int batteryRight = payload[3] * 20;

            String firmware1 = readString(payload, 6, 5);
            String firmware2 = readString(payload, 11, 5);
            String serialNumber = readString(payload, 16, 16);

            // todo: Initializing Battery for battery_case not implemented
            devEvts.add(buildBatteryInfo(battery_earphone_left, batteryLeft));
            devEvts.add(buildBatteryInfo(battery_earphone_right, batteryRight));
            devEvts.add(buildVersionInfo(firmware1, firmware2, serialNumber));
        } else if (cmd == CMD_GET_UNKNOWN_DATA_8D01) {
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        } else if (cmd == CMD_GET_UNKNOWN_DATA_8205) {
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        } else if (cmd == CMD_GET_UNKNOWN_DATA_0105) {
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        } else if (cmd == CMD_NOTIFY_AUDIO_MODE) {
            decodeAdvancedAudioMode(payload);
        } else if (cmd == CMD_NOTIFY_BATTERY_INFO) {
            int batteryLeft = payload[0] * 20;
            int batteryRight = payload[1] * 20;
            int batteryCase = payload[2] * 20;

            devEvts.add(buildBatteryInfo(battery_case, batteryCase));
            devEvts.add(buildBatteryInfo(battery_earphone_left, batteryLeft));
            devEvts.add(buildBatteryInfo(battery_earphone_right, batteryRight));
        } else {
            // see https://github.com/gmallios/SoundcoreManager/blob/master/soundcore-lib/src/models/packet_kind.rs
            // for a mapping for other soundcore devices (similar protocol?)
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        }
        return devEvts.toArray(new GBDeviceEvent[devEvts.size()]);
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        Prefs prefs = getDevicePrefs();
        String pref_string;

        switch (config) {
            // Ambient Sound Modes
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL:
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WIND_NOISE_REDUCTION:
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE:
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING:
            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL:
                return encodeAdvancedAudioMode(true);

            // Control
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_DISABLED:
                return encodeControlTouchLock(TapAction.SINGLE_TAP, prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_DISABLED, false));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_DISABLED:
                return encodeControlTouchLock(TapAction.DOUBLE_TAP, prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_DISABLED, false));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_DISABLED:
                return encodeControlTouchLock(TapAction.TRIPLE_TAP, prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_DISABLED, false));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_DISABLED:
                return encodeControlTouchLock(TapAction.LONG_PRESS, prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_DISABLED, false));

            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_LEFT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_LEFT, "");
                return encodeControlFunction(TapAction.SINGLE_TAP, false, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_RIGHT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_RIGHT, "");
                return encodeControlFunction(TapAction.SINGLE_TAP, true, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_LEFT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_LEFT, "");
                return encodeControlFunction(TapAction.DOUBLE_TAP, false, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_RIGHT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_RIGHT, "");
                return encodeControlFunction(TapAction.DOUBLE_TAP, true, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_LEFT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_LEFT, "");
                return encodeControlFunction(TapAction.TRIPLE_TAP, false, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_RIGHT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_RIGHT, "");
                return encodeControlFunction(TapAction.TRIPLE_TAP, true, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_LEFT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_LEFT, "");
                return encodeControlFunction(TapAction.LONG_PRESS, false, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_RIGHT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_RIGHT, "");
                return encodeControlFunction(TapAction.LONG_PRESS, true, TapFunction.valueOf(pref_string));

            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE:
                AmbientSoundControlButtonMode modes = AmbientSoundControlButtonMode.fromPreferences(prefs.getPreferences());
                switch (modes) {
                    case NC_AS_OFF:
                        return encodeControlAmbientMode(true, true, true);
                    case NC_AS:
                        return encodeControlAmbientMode(true, true, false);
                    case NC_OFF:
                        return encodeControlAmbientMode(true, false, true);
                    case AS_OFF:
                        return encodeControlAmbientMode(false, true, true);
                }

            // Miscellaneous Settings
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WEARING_DETECTION:
                boolean wearingDetection = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WEARING_DETECTION, false);
                return encodeBooleanCommand(CMD_SET_WEARING_DETECTION, wearingDetection);
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WEARING_TONE:
                boolean wearingTone = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WEARING_TONE, false);
                return encodeBooleanCommand(CMD_SET_WEARING_TONE, wearingTone);
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TOUCH_TONE:
                boolean touchTone = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TOUCH_TONE, false);
                return encodeBooleanCommand(CMD_SET_TOUCH_TONE, touchTone);
            default:
                LOG.debug("Unsupported CONFIG: " + config);
        }

        return super.encodeSendConfiguration(config);
    }

    byte[] encodeMysteryDataRequest1() {
        byte[] payload = new byte[]{0x00};
        return encodeCommand(CMD_GET_UNKNOWN_DATA_8D01, payload);
    }
    byte[] encodeMysteryDataRequest2() {
        return encodeRequest(CMD_GET_UNKNOWN_DATA_0105);
    }
    byte[] encodeMysteryDataRequest3() {
        byte[] payload = new byte[]{0x00};
        return encodeCommand(CMD_GET_UNKNOWN_DATA_8205, payload);
    }

    /**
     * Enables or disables a tap-action
     * @param action The byte that encodes the action (single/double/triple or long tap)
     * @param disabled If the action should be enabled or disabled
     * @return
     */
    private byte[] encodeControlTouchLock(TapAction action, boolean disabled) {
        boolean enabled = !disabled;
        byte enabled_byte;
        byte[] payload;
        switch (action) {
            case SINGLE_TAP:
            case TRIPLE_TAP:
                enabled_byte = encodeBoolean(enabled);
                break;
            case DOUBLE_TAP:
            case LONG_PRESS:
                enabled_byte = enabled?(byte) 0x11: (byte) 0x10;
                break;
            default:
                LOG.error("Invalid Tap action");
                return null;
        }
        payload = new byte[]{0x00, action.getCode(), enabled_byte};
        return encodeCommand(CMD_SET_TOUCH_LOCK, payload);
    }

    /**
     * Assigns a function (eg play/pause) to an action (eg single tap on right bud)
     * @param action The byte that encodes the action (single/double/triple or long tap)
     * @param right  If the right or left earbud is meant
     * @param function The byte that encodes the triggered function (eg play/pause)
     * @return The encoded message
     */
    private byte[] encodeControlFunction(TapAction action, boolean right, TapFunction function) {
        byte function_byte;
        switch (action) {
            case SINGLE_TAP:
            case DOUBLE_TAP:
                function_byte = (byte) (16*6 + function.getCode());
                break;
            case TRIPLE_TAP:
                function_byte = (byte) (16*4 + function.getCode());
                break;
            case LONG_PRESS:
                function_byte = (byte) (16*5 + function.getCode());
                break;
            default:
                LOG.error("Invalid Tap action");
                return null;
        }
        return encodeControlFunction(right, action.getCode(), function_byte);
    }

    /**
     * Encodes between which Audio Modes a tap should switch, if it is set to switch the Audio Mode.
     * Zb ANC -> -> Transparency -> Normal -> ANC -> ....
     */
    private byte[] encodeControlAmbientMode(boolean anc, boolean transparency, boolean normal) {
        // Original app does not allow only one true flag. Unsure if Earbuds accept this state.
        byte ambientModes = (byte) (4 * (normal?1:0) + 2 * (transparency?1:0) + (anc?1:0));
        return encodeCommand(CMD_SET_AMBIENT_SOUND_CONTROL_BUTTON_MODE, new byte[] {ambientModes});
    }

}
