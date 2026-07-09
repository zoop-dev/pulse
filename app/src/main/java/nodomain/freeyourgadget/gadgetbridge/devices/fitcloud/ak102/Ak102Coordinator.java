/*  Copyright (C) 2026 Vladimir Tasic

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

package nodomain.freeyourgadget.gadgetbridge.devices.fitcloud.ak102;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Ak102ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.fitcloud.ak102.Ak102DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class Ak102Coordinator extends AbstractBLEDeviceCoordinator {

    @Override
    @NonNull
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return Ak102DeviceSupport.class;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^AK102", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public String getManufacturer() {
        return "TopStep";
    }

    @Override
    public int getBondingStyle() {
        // The Android BLE bond already exists from the FitCloudPro pairing; the
        // real handshake happens at the application layer, so avoid re-bonding.
        return BONDING_STYLE_LAZY;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_ak102;
    }

    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miwatch;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityDistance(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActiveCalories(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsDataFetching(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRecordedActivities(@NonNull GBDevice device) {
        // Workout summaries (+ GPX for GPS sports) land in the sports screen.
        return true;
    }

    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        // Summaries are fully built at sync time (summaryData JSON + GPX file);
        // no raw binary to reparse.
        return (summary, forDetails) -> summary;
    }

    @Override
    public boolean supportsSpo2(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device,
                                                                      final DaoSession session) {
        return new Ak102SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(final GBDevice device,
                                                                          final DaoSession session) {
        return new GenericSpo2SampleProvider(device, session);
    }

    @Override
    public boolean supportsStressMeasurement(@NonNull GBDevice device) {
        return supportsWatchFeature(device, Ak102Constants.FEATURE_PRESSURE);
    }

    // Feature check against the device-info blob persisted at connect time.
    private static boolean supportsWatchFeature(final GBDevice device, final int feature) {
        final String hex = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress())
                .getString(Ak102Constants.PREF_DEVICE_INFO, null);
        if (hex == null || hex.isEmpty()) {
            return false;
        }
        return Ak102Constants.isFeatureSupported(GB.hexStringToByteArray(hex), feature);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(final GBDevice device,
                                                                              final DaoSession session) {
        return new GenericStressSampleProvider(device, session);
    }

    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao(@NonNull final DaoSession session) {
        final Map<AbstractDao<?, ?>, Property> map = new HashMap<>(3);
        map.put(session.getAk102ActivitySampleDao(), Ak102ActivitySampleDao.Properties.DeviceId);
        map.put(session.getGenericSpo2SampleDao(), GenericSpo2SampleDao.Properties.DeviceId);
        map.put(session.getGenericStressSampleDao(), GenericStressSampleDao.Properties.DeviceId);
        return map;
    }

    @Override
    public boolean supportsWeather(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public int getAlarmSlotCount(@NonNull GBDevice device) {
        return 8;
    }

    @Override
    public int getContactsSlotCount(@NonNull final GBDevice device) {
        if (!supportsWatchFeature(device, Ak102Constants.FEATURE_CONTACTS)) {
            return 0;
        }
        return supportsWatchFeature(device, Ak102Constants.FEATURE_CONTACTS_100) ? 100 : 10;
    }

    @Override
    public boolean supportsAlarmTitle(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsCalendarEvents(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsAppsManagement(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsScreenshots(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        final List<Integer> settings = new ArrayList<>();
        // Display.
        settings.add(R.xml.devicesettings_timeformat);
        settings.add(R.xml.devicesettings_liftwrist_display);
        if (hasConfigItem(device, Ak102Constants.KEY_SET_CONFIG_SCREEN_VIBRATE)) {
            settings.add(R.xml.devicesettings_screen_timeout);
            settings.add(R.xml.devicesettings_screen_brightness);
            settings.add(R.xml.devicesettings_ak102_vibration);
        }
        if (hasConfigItem(device, Ak102Constants.KEY_SET_CONFIG_PAGE)) {
            settings.add(R.xml.devicesettings_ak102_pages);
        }
        settings.add(R.xml.devicesettings_language_generic);
        // Health.
        settings.add(R.xml.devicesettings_wearlocation);
        settings.add(R.xml.devicesettings_heartrate_automatic_enable);
        settings.add(R.xml.devicesettings_inactivity_dnd);
        settings.add(R.xml.devicesettings_ak102_hydration);
        // Notifications / behavior.
        if (getContactsSlotCount(device) > 0) {
            settings.add(R.xml.devicesettings_contacts);
        }
        // Dual-mode chip: classic side pairs phone-side, no protocol needed.
        settings.add(R.xml.devicesettings_ak102_audio);
        settings.add(R.xml.devicesettings_ak102_dnd);
        settings.add(R.xml.devicesettings_disconnectnotification_noshed);
        if (supportsWatchFeature(device, Ak102Constants.FEATURE_POWER_SAVE)) {
            settings.add(R.xml.devicesettings_power_saving);
        }
        settings.add(R.xml.devicesettings_find_phone);
        final int[] result = new int[settings.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = settings.get(i);
        }
        return result;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new Ak102SettingsCustomizer();
    }

    // True when the last config readback contained the given TLV item.
    private static boolean hasConfigItem(final GBDevice device, final byte type) {
        final String hex = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress())
                .getString(Ak102Constants.PREF_CONFIG_PREFIX + (type & 0xFF), null);
        return hex != null && !hex.isEmpty();
    }
}
