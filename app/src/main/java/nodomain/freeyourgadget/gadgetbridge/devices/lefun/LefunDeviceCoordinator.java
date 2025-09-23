/*  Copyright (C) 2020-2025 Damien Gaignon, Daniel Dakhno, José Rebelo,
    Petr Vaněk, Yukai Li, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.LefunActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.LefunBiometricSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.LefunSleepSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;

import static nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants.ADVERTISEMENT_NAME;
import static nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants.MANUFACTURER_NAME;
import static nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants.NUM_ALARM_SLOTS;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Device coordinator for Lefun band
 */
public class LefunDeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supports(@NonNull GBDeviceCandidate candidate) {
        final Pattern supportedDeviceName = getSupportedDeviceName();
        if (supportedDeviceName != null) {
            return supportedDeviceName.matcher(candidate.getName()).matches();
        }

        // There's a bunch of other names other than "Lefun", but let's just focus on one for now.
        if (ADVERTISEMENT_NAME.equals(candidate.getName())) {
            // The device does not advertise service UUIDs, so can't check whether it supports
            // the proper service. We can check that it doesn't advertise any services, though.
            // We're actually supposed to check for presence of the string "TJDR" within the
            // manufacturer specific data, which consists of the device's MAC address and said
            // string. But we're not being given it, so *shrug*.
            if (candidate.getServiceUuids().length == 0) {
                return true;
            }
        }

        return false;
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
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new LefunSampleProvider(device, session);
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return NUM_ALARM_SLOTS;
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return MANUFACTURER_NAME;
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        return new BatteryConfig[]{
                new BatteryConfig(
                        0,
                        GBDevice.BATTERY_ICON_DEFAULT,
                        GBDevice.BATTERY_LABEL_DEFAULT,
                        15,
                        100
                )
        };
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_liftwrist_display_noshed,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_antilost,
                R.xml.devicesettings_inactivity,
                R.xml.devicesettings_hydration_reminder,
                R.xml.devicesettings_lefun_interface_language,
                R.xml.devicesettings_transliteration
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return LefunDeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_lefun;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_h30_h10;
    }

    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao(@NonNull final DaoSession session) {
        Map<AbstractDao<?, ?>, Property> map = new HashMap<>(3);
        map.put(session.getLefunActivitySampleDao(), LefunActivitySampleDao.Properties.DeviceId);
        map.put(session.getLefunBiometricSampleDao(), LefunBiometricSampleDao.Properties.DeviceId);
        map.put(session.getLefunSleepSampleDao(), LefunSleepSampleDao.Properties.DeviceId);
        return map;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.FITNESS_BAND;
    }
}
