/*  Copyright (C) 2025 Ilya Nikitenkov

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */

package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_ANC_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_AUDIOMODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_VOICE_BOOST;

import android.os.Parcel;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SetAudioModeRequest;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class HuaweiFreebudsSettingsCustomizer implements DeviceSpecificSettingsCustomizer {

    final GBDevice device;

    public HuaweiFreebudsSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, Prefs prefs, final String rootKey) {
        // Show ANC modes control if audio mode is noise cancelling and show Voice Boost control in Transparency mode
        final ListPreference audioModeControl = handler.findPreference(PREF_HUAWEI_FREEBUDS_AUDIOMODE);
        if (audioModeControl != null) {
            final Preference ancMode = handler.findPreference(PREF_HUAWEI_FREEBUDS_ANC_MODE);
            final Preference voiceBoost = handler.findPreference(PREF_HUAWEI_FREEBUDS_VOICE_BOOST);

            final Preference.OnPreferenceChangeListener audioModePrefListener = (preference, newVal) -> {
                boolean isNoiseCancellationEnabled = SetAudioModeRequest.AudioMode.ANC.name().toLowerCase(Locale.getDefault()).equals(newVal);
                boolean isTransparencyEnabled = SetAudioModeRequest.AudioMode.TRANSPARENCY.name().toLowerCase(Locale.getDefault()).equals(newVal);
                if (ancMode != null)
                    ancMode.setVisible(isNoiseCancellationEnabled);
                if (voiceBoost != null)
                    voiceBoost.setVisible(isTransparencyEnabled);

                return true;
            };
            audioModePrefListener.onPreferenceChange(audioModeControl, audioModeControl.getValue());
            handler.addPreferenceHandlerFor(PREF_HUAWEI_FREEBUDS_AUDIOMODE, audioModePrefListener);
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<HuaweiFreebudsSettingsCustomizer> CREATOR = new Creator<>() {
        @Override
        public HuaweiFreebudsSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(HuaweiFreebudsSettingsCustomizer.class.getClassLoader());
            return new HuaweiFreebudsSettingsCustomizer(device);
        }

        @Override
        public HuaweiFreebudsSettingsCustomizer[] newArray(final int size) {
            return new HuaweiFreebudsSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(device, 0);
    }
}
