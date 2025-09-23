/*  Copyright (C) 2023-2025 José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MijiaLywsdHistoricSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MijiaLywsdRealtimeSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.mijia_lywsd.MijiaLywsdSupport;

public abstract class AbstractMijiaLywsdCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supportsActivityDataFetching(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public String getManufacturer() {
        return "Xiaomi";
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new MijiaLywsdSettingsCustomizer();
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return MijiaLywsdSupport.class;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_mijia_lywsd,
                R.xml.devicesettings_temperature_scale_cf,
        };
    }

    @Override
    public TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(final GBDevice device, final DaoSession session) {
        return new MijiaLywsdRealtimeSampleProvider(device, session);
    }

    @Override
    public boolean supportsTemperatureMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsContinuousTemperature(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return false; // FIXME: Enable this once temperature fetching is enabled
    }

    @Override
    public boolean supportsSleepMeasurement(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsStepCounter(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsSpeedzones(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsActivityTabs(@NonNull GBDevice device) {
        return false;
    }

    public abstract boolean supportsSetTime();

    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao(@NonNull final DaoSession session) {
        Map<AbstractDao<?, ?>, Property> map = new HashMap<>(2);
        map.put(session.getMijiaLywsdHistoricSampleDao(), MijiaLywsdHistoricSampleDao.Properties.DeviceId);
        map.put(session.getMijiaLywsdRealtimeSampleDao(), MijiaLywsdRealtimeSampleDao.Properties.DeviceId);
        return map;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return supportsSetTime() ? DeviceKind.SMART_CLOCK : DeviceKind.THERMOMETER;
    }
}
