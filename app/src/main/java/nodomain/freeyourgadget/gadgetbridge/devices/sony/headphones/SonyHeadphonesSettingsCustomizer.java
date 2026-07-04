/*  Copyright (C) 2021-2024 Arjan Schrijver, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OVERRIDE_FEATURES_LIST;

import android.os.Parcel;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SonyHeadphonesSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    final GBDevice device;

    public SonyHeadphonesSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, Prefs prefs, final String rootKey) {
        final SonyHeadphonesCoordinator coordinator = (SonyHeadphonesCoordinator) device.getDeviceCoordinator();

        // Override features
        final MultiSelectListPreference overrideFeaturesList = handler.findPreference(PREF_OVERRIDE_FEATURES_LIST);
        if (overrideFeaturesList != null) {
            final Set<SonyHeadphonesCapabilities> defaultCapabilities = coordinator.getDefaultCapabilities();

            final CharSequence[] entries = new CharSequence[SonyHeadphonesCapabilities.values().length];
            final CharSequence[] values = new CharSequence[SonyHeadphonesCapabilities.values().length];
            int i = 0;
            for (SonyHeadphonesCapabilities capability : SonyHeadphonesCapabilities.values()) {
                if (defaultCapabilities.contains(capability)) {
                    entries[i] = "*" + capability.name();
                    values[i] = capability.name();
                    i++;
                }
            }
            for (SonyHeadphonesCapabilities capability : SonyHeadphonesCapabilities.values()) {
                if (!defaultCapabilities.contains(capability)) {
                    entries[i] = capability.name();
                    values[i] = capability.name();
                    i++;
                }
            }
            overrideFeaturesList.setEntries(entries);
            overrideFeaturesList.setEntryValues(values);

            overrideFeaturesList.setOnPreferenceClickListener(preference -> {
                device.sendDeviceUpdateIntent(handler.getContext());
                return false;
            });
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<SonyHeadphonesSettingsCustomizer> CREATOR = new Creator<>() {
        @Override
        public SonyHeadphonesSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(SonyHeadphonesSettingsCustomizer.class.getClassLoader());
            return new SonyHeadphonesSettingsCustomizer(device);
        }

        @Override
        public SonyHeadphonesSettingsCustomizer[] newArray(final int size) {
            return new SonyHeadphonesSettingsCustomizer[size];
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
