/*  Copyright (C) 2026 Davide Gessa

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
package nodomain.freeyourgadget.gadgetbridge.devices.casio.gbd200;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.gbx100.CasioGBX100DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbd200.CasioGBD200DeviceSupport;

public class CasioGBD200DeviceCoordinator extends CasioGBX100DeviceCoordinator {

    @Override
    public GBDevice createDevice(final GBDeviceCandidate candidate, final DeviceType deviceType) {
        final GBDevice device = super.createDevice(candidate, deviceType);
        GBApplication.getDevicePrefs(device).getPreferences().edit()
                .putString(DeviceSettingsPreferenceConst.PREFS_DEVICE_CHARTS_TABS,
                        "activity,activitylist,stepsweek")
                .apply();
        return device;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^CASIO GBD-200.*");
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return CasioGBD200DeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_casiogbd200;
    }

    @Override
    public boolean supportsRecordedActivities(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return (summary, forDetails) -> summary;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_connection_force_legacy_gatt,
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_autolight,
                R.xml.devicesettings_key_vibration,
                R.xml.devicesettings_operating_sounds,
                R.xml.devicesettings_fake_ring_duration,
                R.xml.devicesettings_autoremove_message,
                R.xml.devicesettings_transliteration,
                R.xml.devicesettings_preview_message_in_title,
                R.xml.devicesettings_casio_alert
        };
    }
}
