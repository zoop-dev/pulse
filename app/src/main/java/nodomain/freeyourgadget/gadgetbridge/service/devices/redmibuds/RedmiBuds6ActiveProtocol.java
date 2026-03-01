package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds;

// EQ
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_EQUALIZER_PRESET;
// SINGLE_TAP
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_CONTROL_SINGLE_TAP_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_CONTROL_SINGLE_TAP_RIGHT;
// DOUBLE_TAP
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_CONTROL_DOUBLE_TAP_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_CONTROL_DOUBLE_TAP_RIGHT;
// TRIPLE_TAP
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_CONTROL_TRIPLE_TAP_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_CONTROL_TRIPLE_TAP_RIGHT;
// LONG_TAP
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_6_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT;

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

public class RedmiBuds6ActiveProtocol extends RedmiBudsProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(RedmiBuds6ActiveProtocol.class);

    protected RedmiBuds6ActiveProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        switch (config) {
            case PREF_REDMI_BUDS_6_ACTIVE_CONTROL_SINGLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.SINGLE, Position.LEFT);
            case PREF_REDMI_BUDS_6_ACTIVE_CONTROL_SINGLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.SINGLE, Position.RIGHT);
            case PREF_REDMI_BUDS_6_ACTIVE_CONTROL_DOUBLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.DOUBLE, Position.LEFT);
            case PREF_REDMI_BUDS_6_ACTIVE_CONTROL_DOUBLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.DOUBLE, Position.RIGHT);
            case PREF_REDMI_BUDS_6_ACTIVE_CONTROL_TRIPLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.TRIPLE, Position.LEFT);
            case PREF_REDMI_BUDS_6_ACTIVE_CONTROL_TRIPLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.TRIPLE, Position.RIGHT);
            case PREF_REDMI_BUDS_6_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT:
                return encodeSetGesture(config, InteractionType.LONG, Position.LEFT);
            case PREF_REDMI_BUDS_6_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT:
                return encodeSetGesture(config, InteractionType.LONG, Position.RIGHT);

            case PREF_REDMI_BUDS_6_ACTIVE_EQUALIZER_PRESET:
                return encodeSetIntegerConfig(config, Config.EQ_PRESET);

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
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_CONTROL_SINGLE_TAP_LEFT, Integer.toString(configPayload[4]));
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_CONTROL_SINGLE_TAP_RIGHT, Integer.toString(configPayload[5]));
                // DOUBLE_TAP
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_CONTROL_DOUBLE_TAP_LEFT, Integer.toString(configPayload[7]));
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_CONTROL_DOUBLE_TAP_RIGHT, Integer.toString(configPayload[8]));
                // TRIPLE_TAP
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_CONTROL_TRIPLE_TAP_LEFT, Integer.toString(configPayload[10]));
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_CONTROL_TRIPLE_TAP_RIGHT, Integer.toString(configPayload[11]));
                // LONG_TAP
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT, Integer.toString(configPayload[13]));
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT, Integer.toString(configPayload[14]));
                break;
            case EQ_PRESET:
                editor.putString(PREF_REDMI_BUDS_6_ACTIVE_EQUALIZER_PRESET, Integer.toString(configPayload[3]));
                break;
 
            default:
                LOG.debug("Unhandled device update: {}", hexdump(configPayload));
        }
        editor.apply();
    }

}
