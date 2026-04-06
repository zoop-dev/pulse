package nodomain.freeyourgadget.gadgetbridge.devices.redmibuds;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class RedmiBuds8ActiveSettingsCustomizer extends RedmiBudsSettingsCustomizer {

    public RedmiBuds8ActiveSettingsCustomizer(final GBDevice device) {
        super(device);
    }

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, String rootKey) {

        final ListPreference longPressLeft = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT);
        final ListPreference longPressRight = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT);

        final Preference longPressLeftSettings = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_SETTINGS_LEFT);
        final Preference longPressRightSettings = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_SETTINGS_RIGHT);

        final ListPreference equalizerPreset = handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_EQUALIZER_PRESET);

        if (longPressLeft != null) {
            final Preference.OnPreferenceChangeListener longPressLeftButtonListener = (preference, newVal) -> {
                String mode = newVal.toString();
                if (longPressLeftSettings != null) {
                    longPressLeftSettings.setVisible(mode.equals("6"));
                }
                return true;
            };
            longPressLeftButtonListener.onPreferenceChange(longPressLeft, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT, "6"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_LEFT, longPressLeftButtonListener);
        }
        if (longPressRight != null) {
            final Preference.OnPreferenceChangeListener longPressRightButtonListener = (preference, newVal) -> {
                String mode = newVal.toString();
                if (longPressRightSettings != null) {
                    longPressRightSettings.setVisible(mode.equals("6"));
                }
                return true;
            };
            longPressRightButtonListener.onPreferenceChange(longPressRight, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT, "6"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_CONTROL_LONG_TAP_MODE_RIGHT, longPressRightButtonListener);
        }

        if (equalizerPreset != null) {

            final Preference.OnPreferenceChangeListener equalizerPresetListener = (preference, newVal) -> {

                final List<Preference> prefsToDisable = Arrays.asList(
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k),
                        handler.findPreference(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k)
                );

                String mode = newVal.toString();
                for (Preference pref : prefsToDisable) {
                    if (pref != null) {
                        pref.setEnabled(mode.equals("10"));
                    }
                }
                return true;
            };
            equalizerPresetListener.onPreferenceChange(equalizerPreset, prefs.getString(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_EQUALIZER_PRESET, "0"));
            handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_REDMI_BUDS_8_ACTIVE_EQUALIZER_PRESET, equalizerPresetListener);
        }
    }

}
