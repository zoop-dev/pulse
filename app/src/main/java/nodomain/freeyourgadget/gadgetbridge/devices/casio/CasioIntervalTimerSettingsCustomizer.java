/*  Copyright (C) 2026 Gadgetbridge contributors
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package nodomain.freeyourgadget.gadgetbridge.devices.casio;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.casio.CasioIntervalTimerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CasioIntervalTimerSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private final GBDevice device;

    public CasioIntervalTimerSettingsCustomizer(final GBDevice device) {
        this.device = device;
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Preference pref = handler.findPreference("pref_casio_interval_timer");
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(handler.getContext(), CasioIntervalTimerActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                handler.getContext().startActivity(intent);
                return true;
            });
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
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(device, flags);
    }

    public static final Creator<CasioIntervalTimerSettingsCustomizer> CREATOR = new Creator<>() {
        @Override
        public CasioIntervalTimerSettingsCustomizer createFromParcel(final Parcel in) {
            final GBDevice device = in.readParcelable(CasioIntervalTimerSettingsCustomizer.class.getClassLoader());
            return new CasioIntervalTimerSettingsCustomizer(device);
        }

        @Override
        public CasioIntervalTimerSettingsCustomizer[] newArray(final int size) {
            return new CasioIntervalTimerSettingsCustomizer[size];
        }
    };
}
