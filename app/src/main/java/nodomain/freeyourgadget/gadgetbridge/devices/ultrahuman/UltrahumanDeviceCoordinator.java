/*  Copyright (C) 2025  Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericTemperatureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.samples.UltrahumanActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHrvValueSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.UltrahumanActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.UltrahumanDeviceStateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanDeviceSupport;

public class UltrahumanDeviceCoordinator extends AbstractBLEDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(UltrahumanDeviceCoordinator.class);

    @Override
    public List<DeviceCardAction> getCustomActions() {
        // TODO - use an airplane icon instead
        DeviceCardAction airplaneMode = new UltrahumanDeviceCardAction(R.drawable.ic_circle, R.string.ultrahuman_airplane_mode_title, R.string.ultrahuman_airplane_mode_question, UltrahumanConstants.ACTION_AIRPLANE_MODE);

        return Collections.singletonList(airplaneMode);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        final Long deviceId = device.getId();

        final Map<AbstractDao<?, ?>, Property> daoMap = new HashMap<AbstractDao<?, ?>, Property>() {{
            put(session.getGenericHeartRateSampleDao(), GenericHeartRateSampleDao.Properties.DeviceId);
            put(session.getGenericHrvValueSampleDao(), GenericHrvValueSampleDao.Properties.DeviceId);
            put(session.getGenericSpo2SampleDao(), GenericSpo2SampleDao.Properties.DeviceId);
            put(session.getGenericStressSampleDao(), GenericStressSampleDao.Properties.DeviceId);
            put(session.getGenericTemperatureSampleDao(), GenericTemperatureSampleDao.Properties.DeviceId);
            put(session.getUltrahumanActivitySampleDao(), UltrahumanActivitySampleDao.Properties.DeviceId);
            put(session.getUltrahumanDeviceStateSampleDao(), UltrahumanDeviceStateSampleDao.Properties.DeviceId);
        }};

        for (final Map.Entry<AbstractDao<?, ?>, Property> e : daoMap.entrySet()) {
            e.getKey().queryBuilder()
                    .where(e.getValue().eq(deviceId))
                    .buildDelete().executeDeleteWithoutDetachingEntities();
        }
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_smartring;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_smartring_disabled;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_ultrahuma_ring_air;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return UltrahumanDeviceSupport.class;
    }

    @Override
    public String getManufacturer() {
        return "Ultrahuman Healthcare Pvt Ltd";
    }

    @Override
    public TimeSampleProvider<? extends HrvValueSample> getHrvValueSampleProvider(GBDevice device, DaoSession session) {
        return new GenericHrvValueSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateMaxSampleProvider(GBDevice device, DaoSession session) {
        return new GenericHeartRateSampleProvider(device, session);
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device, final DaoSession session) {
        return new UltrahumanActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new GenericSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(GBDevice device, DaoSession session) {
        return new GenericStressSampleProvider(device, session);
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("UH_[0-9A-F]{12}");
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_time_sync
        };
    }

    @Override
    public TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(GBDevice device, DaoSession session) {
        return new GenericTemperatureSampleProvider(device, session);
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsContinuousTemperature() {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHrvMeasurement() {
        // TODO - needs getHrvSummarySampleProvider in addition to the implemented getHrvValueSampleProvider
        return false;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsSleepMeasurement() {
        return false;
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsStepCounter() {
        return true;
    }

    @Override
    public boolean supportsTemperatureMeasurement() {
        return true;
    }

    @Override
    public boolean supportsSpeedzones() {
        return false;
    }

    @Override
    public boolean supportsStressMeasurement() {
        return true;
    }
}
