/*  Copyright (C) 2015-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Damien Gaignon, Daniel Dakhno, Daniele Gobbetti, José Rebelo,
    Matthieu Baerts, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMisfitSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMorpheuzSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleSupport;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class PebbleCoordinator extends AbstractBLClassicDeviceCoordinator {
    private static final String BG_JS_ENABLED = "pebble_enable_background_javascript";
    private static final boolean BG_JS_ENABLED_DEFAULT = false;

    public PebbleCoordinator() {
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Pebble.*");
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return PebblePairingActivity.class;
    }

    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao(@NonNull final DaoSession session) {
        Map<AbstractDao<?, ?>, Property> map = new HashMap<>(4);
        map.put(session.getPebbleHealthActivitySampleDao(), PebbleHealthActivitySampleDao.Properties.DeviceId);
        map.put(session.getPebbleHealthActivityOverlayDao(), PebbleHealthActivityOverlayDao.Properties.DeviceId);
        map.put(session.getPebbleMisfitSampleDao(), PebbleMisfitSampleDao.Properties.DeviceId);
        map.put(session.getPebbleMorpheuzSampleDao(), PebbleMorpheuzSampleDao.Properties.DeviceId);
        return map;
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        DevicePrefs prefs = GBApplication.getDevicePrefs(device);
        int activityTracker = prefs.getInt("pebble_activitytracker", SampleProvider.PROVIDER_PEBBLE_HEALTH);
        return switch (activityTracker) {
            case SampleProvider.PROVIDER_PEBBLE_MISFIT -> new PebbleMisfitSampleProvider(device, session);
            case SampleProvider.PROVIDER_PEBBLE_MORPHEUZ -> new PebbleMorpheuzSampleProvider(device, session);
            default -> new PebbleHealthSampleProvider(device, session);
        };
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        PBWInstallHandler installHandler = new PBWInstallHandler(uri, context);
        return installHandler.isValid() ? installHandler : null;
    }

    @Override
    public boolean supportsFlashing(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsScreenshots(final GBDevice device) {
        return true;
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return 16;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return PebbleUtils.hasHRM(device.getModel());
    }

    @Override
    public String getManufacturer() {
        return "Pebble";
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity(final GBDevice device) {
        return AppManagerActivity.class;
    }

    @Override
    public File getAppCacheDir() throws IOException {
        return PebbleUtils.getPbwCacheDir();
    }

    @Override
    public String getAppCacheSortFilename() {
        return "pbwcacheorder.txt";
    }

    @Override
    public String getAppFileExtension() {
        return ".pbw";
    }

    @Override
    public boolean supportsAppListFetching(final GBDevice device) {
        if (device.getFirmwareVersion() != null) {
            return PebbleUtils.getFwMajor(device.getFirmwareVersion()) < 3;
        }
        return false;
    }

    @Override
    public boolean supportsAppReordering(final GBDevice device) {
        if (device.getFirmwareVersion() != null) {
            return PebbleUtils.getFwMajor(device.getFirmwareVersion()) >= 3;
        }
        return false;
    }

    @Override
    public boolean supportsCalendarEvents(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather(final GBDevice device) {
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
    public boolean supportsUnicodeEmojis(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        notifications.add(R.xml.devicesettings_autoremove_notifications);
        notifications.add(R.xml.devicesettings_pebble_calls_notifications);
        notifications.add(R.xml.devicesettings_canned_reply_16);
        notifications.add(R.xml.devicesettings_canned_dismisscall_16);
        notifications.add(R.xml.devicesettings_transliteration);

        final List<Integer> calendar = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALENDAR);
        calendar.add(R.xml.devicesettings_sync_calendar);
        calendar.add(R.xml.devicesettings_pebble_calendar);

        final List<Integer> activity = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.ACTIVITY_INFO);
        activity.add(R.xml.devicesettings_pebble_activity);

        final List<Integer> dev = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        dev.add(R.xml.devicesettings_pebble_developer);

        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(GBDevice device) {
        return new PebbleSettingsCustomizer();
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return PebbleSupport.class;
    }


    @Override
    @StringRes
    public int getDeviceNameResource() {
        return R.string.devicetype_pebble;
    }


    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_pebble;
    }

    public boolean isBackgroundJsEnabled(final GBDevice device) {
        DevicePrefs deviceSpecificPreferences = GBApplication.getDevicePrefs(device);
        return deviceSpecificPreferences.getBoolean(BG_JS_ENABLED, BG_JS_ENABLED_DEFAULT);
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
