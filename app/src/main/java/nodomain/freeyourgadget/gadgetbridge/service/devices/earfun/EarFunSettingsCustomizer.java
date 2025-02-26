package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;


import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.*;

import android.os.Parcel;
import android.text.InputFilter;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    final GBDevice device;

    public EarFunSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    public static final Creator<EarFunSettingsCustomizer> CREATOR = new Creator<EarFunSettingsCustomizer>() {
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
        if (preference.getKey().equals(PREF_EARFUN_AMBIENT_SOUND_CONTROL)) {
            ListPreference listPreferenceAmbientSound = handler.findPreference(PREF_EARFUN_AMBIENT_SOUND_CONTROL);
            ListPreference listPreferenceTransparencyMode = handler.findPreference(PREF_EARFUN_TRANSPARENCY_MODE);
            ListPreference listPreferenceAncMode = handler.findPreference(PREF_EARFUN_ANC_MODE);

            if (listPreferenceAmbientSound == null || listPreferenceTransparencyMode == null || listPreferenceAncMode == null) {
                return;
            }

            switch (listPreferenceAmbientSound.getValue()) {
                case "1": // noise cancelling
                    listPreferenceTransparencyMode.setVisible(false);
                    listPreferenceAncMode.setVisible(true);
                    break;
                case "2": // transparency
                    listPreferenceTransparencyMode.setVisible(true);
                    listPreferenceAncMode.setVisible(false);
                    break;
                default:
                    listPreferenceTransparencyMode.setVisible(false);
                    listPreferenceAncMode.setVisible(false);
            }
        }
    }

    @Override
    public void customizeSettings(DeviceSpecificSettingsHandler handler, Prefs prefs, String rootKey) {
        handler.addPreferenceHandlerFor(PREF_EARFUN_DEVICE_NAME);
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

        EditTextPreference editTextDeviceName = handler.findPreference(PREF_EARFUN_DEVICE_NAME);
        if (editTextDeviceName != null) {
            editTextDeviceName.setOnBindEditTextListener(editText -> {
                InputFilter[] filters = new InputFilter[]{new InputFilterLength(25)};
                editText.setFilters(filters);
            });
            editTextDeviceName.setText(device.getName());
        }
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

    private static class InputFilterLength implements InputFilter {
        private final int maxLength;

        public InputFilterLength(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            int keep = maxLength - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                return "";
            } else if (keep >= end - start) {
                return null;
            } else {
                return source.subSequence(start, start + keep);
            }
        }
    }
}
