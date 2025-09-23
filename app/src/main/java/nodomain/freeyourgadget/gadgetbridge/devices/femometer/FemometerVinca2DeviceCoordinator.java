/*  Copyright (C) 2023-2024 Alicia Hormann

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
package nodomain.freeyourgadget.gadgetbridge.devices.femometer;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.FemometerVinca2TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.FemometerVinca2TemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.femometer.FemometerVinca2DeviceSupport;

public class FemometerVinca2DeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public String getManufacturer() {
        // Actual manufacturer is Joytech Healthcare
        return "Femometer";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return FemometerVinca2DeviceSupport.class;
    }

    @Override
    public TimeSampleProvider<FemometerVinca2TemperatureSample> getTemperatureSampleProvider(GBDevice device, DaoSession session) {
        return new FemometerVinca2SampleProvider(device, session);
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("BM-Vinca2");
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_femometer_vinca2;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_thermometer;
    }

    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao(@NonNull final DaoSession session) {
        Map<AbstractDao<?, ?>, Property> map = new HashMap<>(1);
        map.put(session.getFemometerVinca2TemperatureSampleDao(), FemometerVinca2TemperatureSampleDao.Properties.DeviceId);
        return map;
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_NONE;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        return 1;
    }

    @Override
    public boolean supportsTemperatureMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
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

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_volume,
                R.xml.devicesettings_femometer,
                R.xml.devicesettings_temperature_scale_cf,
        };
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.THERMOMETER;
    }
}
