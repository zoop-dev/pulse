package nodomain.freeyourgadget.gadgetbridge.devices.haylou;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HAYLOU_S35_ANC_EQ_PRESET;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HAYLOU_S35_ANC_GAME_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HAYLOU_S35_ANC_LDAC_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HAYLOU_S35_ANC_MULTIPOINT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HAYLOU_S35_ANC_AUDIO_MODE;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.haylou.HaylouS35AncProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class HaylouS35AncSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private final GBDevice device;

    public HaylouS35AncSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    protected HaylouS35AncSettingsCustomizer(final Parcel in) {
        device = in.readParcelable(HaylouS35AncSettingsCustomizer.class.getClassLoader());
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        // Nothing to do here
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        handler.addPreferenceHandlerFor(PREF_HAYLOU_S35_ANC_AUDIO_MODE);
        handler.addPreferenceHandlerFor(PREF_HAYLOU_S35_ANC_GAME_MODE);
        handler.addPreferenceHandlerFor(PREF_HAYLOU_S35_ANC_LDAC_MODE);
        handler.addPreferenceHandlerFor(PREF_HAYLOU_S35_ANC_MULTIPOINT);
        handler.addPreferenceHandlerFor(PREF_HAYLOU_S35_ANC_EQ_PRESET);
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
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeParcelable(device, flags);
    }

    public static final Creator<HaylouS35AncSettingsCustomizer> CREATOR = new Creator<HaylouS35AncSettingsCustomizer>() {
        @Override
        public HaylouS35AncSettingsCustomizer createFromParcel(final Parcel in) {
            return new HaylouS35AncSettingsCustomizer(in);
        }

        @Override
        public HaylouS35AncSettingsCustomizer[] newArray(final int size) {
            return new HaylouS35AncSettingsCustomizer[size];
        }
    };
}
