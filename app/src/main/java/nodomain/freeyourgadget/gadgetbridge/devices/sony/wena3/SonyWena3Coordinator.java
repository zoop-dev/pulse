/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3;

import android.bluetooth.le.ScanFilter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3BehaviorSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3CaloriesSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3EnergySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3HeartRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3StressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3Vo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.SonyWena3DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.LedColor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationCount;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationKind;

public class SonyWena3Coordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public String getManufacturer() {
        return "Sony";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return SonyWena3DeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sony_wena3;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(GBDevice device) {
        return new SonyWena3SettingsCustomizer();
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceName(SonyWena3Constants.BT_DEVICE_NAME);
        ArrayList<ScanFilter> result = new ArrayList<>();
        result.add(builder.build());
        return result;
    }

    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao(@NonNull final DaoSession session) {
        Map<AbstractDao<?, ?>, Property> map = new HashMap<>(7);
        map.put(session.getWena3HeartRateSampleDao(), Wena3HeartRateSampleDao.Properties.DeviceId);
        map.put(session.getWena3BehaviorSampleDao(), Wena3BehaviorSampleDao.Properties.DeviceId);
        map.put(session.getWena3CaloriesSampleDao(), Wena3CaloriesSampleDao.Properties.DeviceId);
        map.put(session.getWena3EnergySampleDao(), Wena3EnergySampleDao.Properties.DeviceId);
        map.put(session.getWena3ActivitySampleDao(), Wena3ActivitySampleDao.Properties.DeviceId);
        map.put(session.getWena3Vo2SampleDao(), Wena3Vo2SampleDao.Properties.DeviceId);
        map.put(session.getWena3StressSampleDao(), Wena3StressSampleDao.Properties.DeviceId);
        return map;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(SonyWena3Constants.BT_DEVICE_NAME);
    }
    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_BOND;
    }

    @Override
    public boolean supportsCalendarEvents(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsStressMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(GBDevice device, DaoSession session) {
        return new SonyWena3StressSampleProvider(device, session);
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new SonyWena3ActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingSampleProvider(GBDevice device, DaoSession session) {
        return new SonyWena3HeartRateSampleProvider(device, session);
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return SonyWena3Constants.ALARM_SLOTS;
    }

    @Override
    public boolean supportsSmartWakeup(@NonNull GBDevice device, int position) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "en_US",
                "ja_JP"
        };
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_notifications_enable,
                R.xml.devicesettings_sync_calendar,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_donotdisturb_no_auto,
                R.xml.devicesettings_wena3_auto_power_off,
                R.xml.devicesettings_goal_notification,
                R.xml.devicesettings_wena3,
        };
    }


    @Override
    public boolean supportsNotificationVibrationPatterns(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsNotificationVibrationRepetitionPatterns(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsNotificationLedPatterns(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public AbstractNotificationPattern[] getNotificationVibrationPatterns() {
        return new AbstractNotificationPattern[] {
                VibrationKind.NONE, VibrationKind.BASIC,
                VibrationKind.CONTINUOUS, VibrationKind.RAPID,
                VibrationKind.TRIPLE, VibrationKind.STEP_UP, VibrationKind.STEP_DOWN,
                VibrationKind.WARNING, VibrationKind.SIREN, VibrationKind.SHORT
        };
    }

    @Override
    public AbstractNotificationPattern[] getNotificationVibrationRepetitionPatterns() {
        return new AbstractNotificationPattern[] {
                VibrationCount.ONCE, VibrationCount.TWICE, VibrationCount.THREE, VibrationCount.FOUR,
                VibrationCount.INDEFINITE
        };
    }

    @Override
    public AbstractNotificationPattern[] getNotificationLedPatterns() {
        return new AbstractNotificationPattern[] {
                LedColor.NONE, LedColor.RED, LedColor.YELLOW, LedColor.GREEN,
                LedColor.CYAN, LedColor.BLUE, LedColor.PURPLE, LedColor.WHITE
        };
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
