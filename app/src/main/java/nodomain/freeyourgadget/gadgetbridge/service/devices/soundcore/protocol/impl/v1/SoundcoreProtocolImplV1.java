package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.protocol.impl.v1;

import android.content.SharedPreferences;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.AbstractSoundcoreProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class SoundcoreProtocolImplV1 extends AbstractSoundcoreProtocol {
    public static final short CMD_GET_DEVICE_INFO = (short) 0x0101;
    public static final short CMD_GET_UNKNOWN_DATA_0105 = (short) 0x0105;

    public static final short CMD_NOTIFY_BATTERY_INFO = (short) 0x0301;
    public static final short CMD_NOTIFY_CHARGING_INFO = (short) 0x0401;
    public static final short CMD_NOTIFY_AUDIO_MODE = (short) 0x0106;

    public static final short CMD_SET_AUDIO_MODE = (short) 0x8106;
    public static final short CMD_SET_CONTROL_FUNCTION = (short) 0x8104;
    public static final short CMD_SET_TOUCH_TONE = (short) 0x8301;
    public static final short CMD_SET_AUTO_POWER_OFF = (short) 0x8601;
    public static final short CMD_SET_FIND_DEVICE = (short) 0x8910;
    public static final short CMD_ENABLE_PAIRING_MODE = (short) 0x850b;

    protected SoundcoreProtocolImplV1(final GBDevice device) {
        super(device);
    }

    public byte[] encodeDeviceInfoRequest() {
        return encodeRequest(CMD_GET_DEVICE_INFO);
    }

    @Override
    public byte[] encodeFindDevice(final boolean start) {
        return encodeCommand(CMD_SET_FIND_DEVICE, new byte[]{
                encodeBoolean(start),
                encodeBoolean(start),
                0x00
        });
    }

    protected byte[] encodePairingMode() {
        return encodeCommand(CMD_ENABLE_PAIRING_MODE, new byte[]{0x00, (byte) 0x90});
    }

    protected byte[] encodeControlFunction(final boolean right, final byte action, final byte function) {
        return encodeCommand(CMD_SET_CONTROL_FUNCTION, new byte[]{encodeBoolean(right), action, function});
    }

    /**
     * Encodes the following settings to a payload to set the audio-mode on the headphones:
     * PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL If ANC, Transparent or neither should be active
     * PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING If the strenght of the ANC should be set manual or adaptively according to ambient noise
     * PREF_SONY_AMBIENT_SOUND_LEVEL How strong the ANC should be in manual mode
     * PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE If the Transparency should focus on vocals or should be fully transparent
     * PREF_SOUNDCORE_WIND_NOISE_REDUCTION If Transparency or ANC should reduce Wind Noise
     * @return The payload
    */
    protected byte[] encodeAdvancedAudioMode(final boolean appendStateByte) {
        final Prefs prefs = getDevicePrefs();
        final Byte ambientSoundMode = encodeAmbientSoundMode(prefs.getString(
                DeviceSettingsPreferenceConst.PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL,
                "off"
        ));

        if (ambientSoundMode == null) {
            return null;
        }

        final Byte ancStrength = encodeAncStrength(prefs.getInt(
                DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL,
                0
        ));

        if (ancStrength == null) {
            return null;
        }

        final byte vocalMode = encodeBoolean(prefs.getBoolean(
                DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE,
                false
        ));
        final byte adaptiveAnc = encodeBoolean(prefs.getBoolean(
                DeviceSettingsPreferenceConst.PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING,
                true
        ));
        final byte windNoiseReduction = encodeBoolean(prefs.getBoolean(
                DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WIND_NOISE_REDUCTION,
                false
        ));

        final byte[] payload = new byte[]{
                ambientSoundMode.byteValue(),
                ancStrength.byteValue(),
                vocalMode,
                adaptiveAnc,
                windNoiseReduction
        };

        if (appendStateByte) {
            final byte[] statePayload = Arrays.copyOf(payload, payload.length + 1);
            statePayload[statePayload.length - 1] = 0x01;
            return encodeCommand(CMD_SET_AUDIO_MODE, statePayload);
        }

        return encodeCommand(CMD_SET_AUDIO_MODE, payload);
    }

    protected void decodeAdvancedAudioMode(final byte[] payload) {
        if (payload.length < 5) {
            return;
        }

        final SharedPreferences.Editor editor = getDevicePrefs().getPreferences().edit();

        editor.putString(
                DeviceSettingsPreferenceConst.PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL,
                decodeAmbientSoundMode(payload[0])
        );
        editor.putInt(
                DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL,
                decodeAncStrength(payload[1])
        );
        editor.putBoolean(
                DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE,
                payload[2] == 0x01
        );
        editor.putBoolean(
                DeviceSettingsPreferenceConst.PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING,
                payload[3] == 0x01
        );
        editor.putBoolean(
                DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WIND_NOISE_REDUCTION,
                payload[4] == 0x01
        );
        editor.apply();
    }

    protected Byte encodeAmbientSoundMode(final String ambientMode) {
        switch (ambientMode) {
            case "noise_cancelling":
                return (byte) 0x00;
            case "ambient_sound":
                return (byte) 0x01;
            case "off":
                return (byte) 0x02;
            default:
                return null;
        }
    }

    protected String decodeAmbientSoundMode(final byte ambientMode) {
        switch (ambientMode) {
            case 0x00:
                return "noise_cancelling";
            case 0x01:
                return "ambient_sound";
            case 0x02:
            default:
                return "off";
        }
    }

    protected Byte encodeAncStrength(final int strength) {
        if (strength < 0 || strength > 2) {
            return null;
        }

        return (byte) ((strength + 1) << 4);
    }

    protected int decodeAncStrength(final byte strength) {
        switch (strength & 0x30) {
            case 0x20:
                return 1;
            case 0x30:
                return 2;
            case 0x10:
            default:
                return 0;
        }
    }
}
