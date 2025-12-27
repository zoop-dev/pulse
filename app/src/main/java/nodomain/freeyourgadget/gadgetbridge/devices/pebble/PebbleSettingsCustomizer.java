package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.os.Parcel;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class PebbleSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    public static final Creator<PebbleSettingsCustomizer> CREATOR = new Creator<PebbleSettingsCustomizer>() {
        @Override
        public PebbleSettingsCustomizer createFromParcel(final Parcel in) {
            return new PebbleSettingsCustomizer();
        }

        @Override
        public PebbleSettingsCustomizer[] newArray(final int size) {
            return new PebbleSettingsCustomizer[size];
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(PebbleSettingsCustomizer.class);
    private final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final EditTextPreference pref = handler.findPreference("pebble_mtu_limit");
        if (pref != null) {
            pref.setOnBindEditTextListener(p -> {
                p.setInputType(InputType.TYPE_CLASS_NUMBER);
                p.setSelection(p.getText().length());
            });
        }

        final Preference appUpdatePref = handler.findPreference("pebble_enable_finding_app_updates");
        if (appUpdatePref != null && !GBApplication.hasInternetAccess()) {
            appUpdatePref.setEnabled(false);
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
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }
}
