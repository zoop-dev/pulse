package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_RIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_RIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_RIGHT;

import android.content.SharedPreferences;

import nodomain.freeyourgadget.gadgetbridge.devices.redmibuds.prefs.Configuration;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class RedmiBuds3ProProtocol extends RedmiBudsProtocol {
    protected RedmiBuds3ProProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public void decodeGetConfig(byte[] configPayload) {
        if (configPayload.length < 3)
            return;

        SharedPreferences preferences = getDevicePrefs().getPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        Configuration.Config config = Configuration.Config.fromCode(configPayload[2]);
        if (config == Configuration.Config.GESTURES) {
            editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_LEFT, Integer.toString(configPayload[4]));
            editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_RIGHT, Integer.toString(configPayload[5]));

            editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_LEFT, Integer.toString(configPayload[7]));
            editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_RIGHT, Integer.toString(configPayload[8]));

            editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_LEFT, Integer.toString(configPayload[10]));
            editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_RIGHT, Integer.toString(configPayload[11]));

            editor.apply();
        } else {
            super.decodeGetConfig(configPayload);
        }
    }
}
