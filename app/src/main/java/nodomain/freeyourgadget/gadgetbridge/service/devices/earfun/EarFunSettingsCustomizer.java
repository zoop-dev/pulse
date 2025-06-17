package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;


import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.*;

import android.content.Context;
import android.os.Parcel;
import android.text.InputFilter;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Equalizer;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    final GBDevice device;

    public EarFunSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    public static final Creator<EarFunSettingsCustomizer> CREATOR = new Creator<>() {
        @Override
        public EarFunSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(EarFunSettingsCustomizer.class.getClassLoader());
            return new EarFunSettingsCustomizer(device);
        }

        @Override
        public EarFunSettingsCustomizer[] newArray(final int size) {
            return new EarFunSettingsCustomizer[size];
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(EarFunSettingsCustomizer.class);

    @Override
    public void onPreferenceChange(Preference preference, DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, String rootKey) {
        DeviceSettingsUtils.addConfirmablePreferenceHandlerFor(handler, PREF_EARFUN_DEVICE_NAME, R.string.earfun_change_device_name_confirm_message);
        handler.addPreferenceHandlerFor(PREF_EARFUN_AMBIENT_SOUND_CONTROL);
        handler.addPreferenceHandlerFor(PREF_EARFUN_TRANSPARENCY_MODE);
        handler.addPreferenceHandlerFor(PREF_EARFUN_ANC_MODE);
        handler.addPreferenceHandlerFor(PREF_EARFUN_SINGLE_TAP_LEFT_ACTION);
        handler.addPreferenceHandlerFor(PREF_EARFUN_SINGLE_TAP_RIGHT_ACTION);
        handler.addPreferenceHandlerFor(PREF_EARFUN_DOUBLE_TAP_LEFT_ACTION);
        handler.addPreferenceHandlerFor(PREF_EARFUN_DOUBLE_TAP_RIGHT_ACTION);
        handler.addPreferenceHandlerFor(PREF_EARFUN_TRIPPLE_TAP_LEFT_ACTION);
        handler.addPreferenceHandlerFor(PREF_EARFUN_TRIPPLE_TAP_RIGHT_ACTION);
        handler.addPreferenceHandlerFor(PREF_EARFUN_LONG_TAP_LEFT_ACTION);
        handler.addPreferenceHandlerFor(PREF_EARFUN_LONG_TAP_RIGHT_ACTION);
        handler.addPreferenceHandlerFor(PREF_EARFUN_GAME_MODE);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_31_5);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_63);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_125);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_180);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_250);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_500);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_1000);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_2000);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_4000);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_8000);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_15000);
        handler.addPreferenceHandlerFor(PREF_EARFUN_EQUALIZER_BAND_16000);
        handler.addPreferenceHandlerFor(PREF_EARFUN_IN_EAR_DETECTION_MODE);
        handler.addPreferenceHandlerFor(PREF_EARFUN_TOUCH_MODE);
        DeviceSettingsUtils.addConfirmablePreferenceHandlerFor(handler, PREF_EARFUN_CONNECT_TWO_DEVICES_MODE, R.string.earfun_reboot_confirm_message);
        DeviceSettingsUtils.addConfirmablePreferenceHandlerFor(handler, PREF_EARFUN_ADVANCED_AUDIO_MODE, R.string.earfun_reboot_confirm_message);
        DeviceSettingsUtils.addConfirmablePreferenceHandlerFor(handler, PREF_EARFUN_MICROPHONE_MODE, R.string.earfun_reboot_confirm_message);
        handler.addPreferenceHandlerFor(PREF_EARFUN_FIND_DEVICE);
        handler.addPreferenceHandlerFor(PREF_EARFUN_VOICE_PROMPT_VOLUME);
        DeviceSettingsUtils.addConfirmablePreferenceHandlerFor(handler, PREF_EARFUN_AUDIO_CODEC, R.string.earfun_reboot_confirm_message);

        EditTextPreference editTextDeviceName = handler.findPreference(PREF_EARFUN_DEVICE_NAME);
        if (editTextDeviceName != null) {
            editTextDeviceName.setOnBindEditTextListener(editText -> {
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(25)});
            });
            editTextDeviceName.setText(device.getName());
        }
    }

    protected void initializeEqualizerPresetListPreference(DeviceSpecificSettingsHandler handler,
                                                           Equalizer.EqualizerPreset[] equalizerPresets) {
        ListPreference equalizerPresetListPreference = handler.findPreference(PREF_EARFUN_EQUALIZER_PRESET);
        if (equalizerPresetListPreference != null) {
            List<CharSequence> entries = Arrays.stream(equalizerPresets)
                    .map(preset -> localizedPresetName(preset, handler.getContext())).collect(Collectors.toList());
            // add an additional element for user set custom band adjustments
            entries.add(handler.getContext().getString(R.string.redmi_buds_5_pro_equalizer_preset_custom));

            CharSequence[] entryValues = IntStream.rangeClosed(0, equalizerPresets.length)
                    .mapToObj(Integer::toString).toArray(String[]::new);

            equalizerPresetListPreference.setEntries(entries.toArray(new CharSequence[0]));
            equalizerPresetListPreference.setEntryValues(entryValues);
        }
    }

    private String localizedPresetName(Equalizer.EqualizerPreset preset, Context context) {
        if (preset.getLocalizedPresetName() != -1) {
            return context.getString(preset.getLocalizedPresetName());
        } else {
            return preset.getPresetName();
        }
    }

    protected static void onPreferenceChangeEqualizerPreset(DeviceSpecificSettingsHandler handler,
                                                            Equalizer.BandConfig[] equalizerBands,
                                                            Equalizer.EqualizerPreset[] equalizerPresets) {
        ListPreference listPreferenceEqualizerPreset = handler.findPreference(PREF_EARFUN_EQUALIZER_PRESET);
        if (listPreferenceEqualizerPreset == null) {
            return;
        }
        try {
            int selectedOption = Integer.parseInt(listPreferenceEqualizerPreset.getValue());
            if (selectedOption >= equalizerPresets.length || selectedOption < 0) {
                return;
            }
            Equalizer.EqualizerPreset preset = equalizerPresets[selectedOption];

            IntStream.range(0, preset.getSettings().length).forEach(index -> {
                String key = equalizerBands[index].getKey();
                if (key == null) {
                    return;
                }
                SeekBarPreference seekBarPreferenceEqualizerBand = handler.findPreference(key);
                if (seekBarPreferenceEqualizerBand == null) {
                    return;
                }
                int gain = (int) Math.round(preset.getSettings()[index]);
                seekBarPreferenceEqualizerBand.setValue(gain);
                // call the change listener after setting last band to send new values to the device
                if (index == preset.getSettings().length - 1) {
                    seekBarPreferenceEqualizerBand.callChangeListener(gain);
                }
            });
        } catch (NumberFormatException ignored) {
        }
    }

    protected static int getSelectedPresetFromEqualizerBands(DeviceSpecificSettingsHandler handler,
                                                             Equalizer.BandConfig[] equalizerBands,
                                                             Equalizer.EqualizerPreset[] equalizerPresets) {
        double[] equalizerConfig = Arrays.stream(equalizerBands)
                .filter(bandConfig -> bandConfig.getKey() != null)
                .map(bandConfig -> {
                    SeekBarPreference bandSeekBarPreference = handler.findPreference(bandConfig.getKey());
                    return bandSeekBarPreference.getValue();
                })
                .mapToDouble(Integer::doubleValue)
                .toArray();

        return IntStream.range(0, equalizerPresets.length)
                .filter(i -> Arrays.equals(equalizerPresets[i].getSettings(), equalizerConfig))
                .findFirst()
                // if filter settings do not match a preset, select the "custom" preset
                .orElse(equalizerPresets.length);
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
    }
}
