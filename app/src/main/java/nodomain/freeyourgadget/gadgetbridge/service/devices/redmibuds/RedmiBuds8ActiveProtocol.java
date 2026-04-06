package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds;

// EQ_PRESET
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_EQUALIZER_PRESET;

// EQ_CURVE
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k;

// DOUBLE_CONNECTION
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_DOUBLE_CONNECTION;

// ADAPTIVE_SOUND
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_ADAPTIVE_SOUND;

// SINGLE_TAP
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_SINGLE_TAP_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_SINGLE_TAP_RIGHT;
// DOUBLE_TAP
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_DOUBLE_TAP_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_DOUBLE_TAP_RIGHT;
// TRIPLE_TAP
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_TRIPLE_TAP_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_TRIPLE_TAP_RIGHT;
// LONG_TAP
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.prefs.Configuration;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.prefs.Gestures.InteractionType;
import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.prefs.Gestures.Position;
import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.prefs.Configuration.Config;

public class RedmiBuds8ActiveProtocol extends RedmiBudsProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(RedmiBuds8ActiveProtocol.class);

    protected RedmiBuds8ActiveProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        switch (config) {
            // GESTURE
            case PREF_REDMI_BUDS_8_ACTIVE_CONTROL_SINGLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.SINGLE, Position.LEFT);
            case PREF_REDMI_BUDS_8_ACTIVE_CONTROL_SINGLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.SINGLE, Position.RIGHT);
            case PREF_REDMI_BUDS_8_ACTIVE_CONTROL_DOUBLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.DOUBLE, Position.LEFT);
            case PREF_REDMI_BUDS_8_ACTIVE_CONTROL_DOUBLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.DOUBLE, Position.RIGHT);
            case PREF_REDMI_BUDS_8_ACTIVE_CONTROL_TRIPLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.TRIPLE, Position.LEFT);
            case PREF_REDMI_BUDS_8_ACTIVE_CONTROL_TRIPLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.TRIPLE, Position.RIGHT);
            case PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT:
                return encodeSetGesture(config, InteractionType.LONG, Position.LEFT);
            case PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT:
                return encodeSetGesture(config, InteractionType.LONG, Position.RIGHT);
            // EQ_PRESET
            case PREF_REDMI_BUDS_8_ACTIVE_EQUALIZER_PRESET:
                return encodeSetIntegerConfig(config, Config.EQ_PRESET);
            // EQ_CURVE
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k:
                return encodeSetCustomEqualizer();
            // DOUBLE_CONNECTION
            case PREF_REDMI_BUDS_5_PRO_DOUBLE_CONNECTION:
                return encodeSetBooleanConfig(config, Config.DOUBLE_CONNECTION);
            // ADAPTIVE_SOUND
            case PREF_REDMI_BUDS_5_PRO_ADAPTIVE_SOUND:
                return encodeSetBooleanConfig(config, Config.ADAPTIVE_SOUND);

            default:
                LOG.debug("Unsupported config: {}", config);
                return null;        
        }
    }

    @Override
    public void decodeGetConfig(byte[] configPayload) {
        if (configPayload.length < 3)
            return;

        SharedPreferences preferences = getDevicePrefs().getPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        Configuration.Config config = Configuration.Config.fromCode(configPayload[2]);

        switch (config) {
            case GESTURES:
                // SINGLE_TAP
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_CONTROL_SINGLE_TAP_LEFT, Integer.toString(configPayload[4]));
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_CONTROL_SINGLE_TAP_RIGHT, Integer.toString(configPayload[5]));
                // DOUBLE_TAP
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_CONTROL_DOUBLE_TAP_LEFT, Integer.toString(configPayload[7]));
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_CONTROL_DOUBLE_TAP_RIGHT, Integer.toString(configPayload[8]));
                // TRIPLE_TAP
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_CONTROL_TRIPLE_TAP_LEFT, Integer.toString(configPayload[10]));
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_CONTROL_TRIPLE_TAP_RIGHT, Integer.toString(configPayload[11]));
                // LONG_TAP
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT, Integer.toString(configPayload[13]));
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT, Integer.toString(configPayload[14]));
                break;
            case EQ_PRESET:
                editor.putString(PREF_REDMI_BUDS_8_ACTIVE_EQUALIZER_PRESET, Integer.toString(configPayload[3]));
                break;
            case EQ_CURVE:
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62, Integer.toString(configPayload[12] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125, Integer.toString(configPayload[15] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250, Integer.toString(configPayload[18] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500, Integer.toString(configPayload[21] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k, Integer.toString(configPayload[24] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k, Integer.toString(configPayload[27] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k, Integer.toString(configPayload[30] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k, Integer.toString(configPayload[33] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k, Integer.toString(configPayload[36] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k, Integer.toString(configPayload[39] & 0xFF));
                break;
            case DOUBLE_CONNECTION:
                editor.putBoolean(PREF_REDMI_BUDS_5_PRO_DOUBLE_CONNECTION, configPayload[3] == 0x01);
                break;
            case ADAPTIVE_SOUND:
                editor.putBoolean(PREF_REDMI_BUDS_5_PRO_ADAPTIVE_SOUND, configPayload[3] == 0x01);
                break;

            default:
                LOG.debug("Unhandled device update: {}", hexdump(configPayload));
        }
        editor.apply();
    }

}
