/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.samples.ColmiActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.samples.ColmiHrvSummarySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.samples.ColmiHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.samples.ColmiSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.samples.ColmiStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.samples.ColmiTemperatureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHeartRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHrvSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHrvValueSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepSessionSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.yawell.ring.YawellRingDeviceSupport;

public abstract class AbstractYawellRingCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao( @NonNull final DaoSession session) {
        return new HashMap<>() {{
            put(session.getColmiActivitySampleDao(), ColmiActivitySampleDao.Properties.DeviceId);
            put(session.getColmiHeartRateSampleDao(), ColmiHeartRateSampleDao.Properties.DeviceId);
            put(session.getColmiSpo2SampleDao(), ColmiSpo2SampleDao.Properties.DeviceId);
            put(session.getColmiStressSampleDao(), ColmiStressSampleDao.Properties.DeviceId);
            put(session.getColmiSleepSessionSampleDao(), ColmiSleepSessionSampleDao.Properties.DeviceId);
            put(session.getColmiSleepStageSampleDao(), ColmiSleepStageSampleDao.Properties.DeviceId);
            put(session.getColmiHrvSummarySampleDao(), ColmiHrvSummarySampleDao.Properties.DeviceId);
            put(session.getColmiHrvValueSampleDao(), ColmiHrvValueSampleDao.Properties.DeviceId);
        }};
    }

    @Override
    public String getManufacturer() {
        return "Colmi";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return YawellRingDeviceSupport.class;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_smartring;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supportsPowerOff(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsStressMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRemSleep(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAwakeSleep(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHrvMeasurement(final GBDevice device) {
        return true;
    }

    public boolean hasDisplay() {
        return false;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new ColmiActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new ColmiSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(GBDevice device, DaoSession session) {
        return new ColmiStressSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HrvSummarySample> getHrvSummarySampleProvider(GBDevice device, DaoSession session) {
        return new ColmiHrvSummarySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HrvValueSample> getHrvValueSampleProvider(final GBDevice device, final DaoSession session) {
        return new ColmiHrvValueSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(GBDevice device, DaoSession session) {
        return new ColmiTemperatureSampleProvider(device, session);
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.MINUTES_5,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_15,
                HeartRateCapability.MeasurementInterval.MINUTES_30,
                HeartRateCapability.MeasurementInterval.MINUTES_45,
                HeartRateCapability.MeasurementInterval.HOUR_1
        );
    }

    @Override
    public int[] getStressRanges() {
        // 1-29 = relaxed
        // 30-59 = normal
        // 60-79 = medium
        // 80-99 = high
        return new int[]{1, 30, 60, 80};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        final List<Integer> health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH);
        health.add(R.xml.devicesettings_colmi_r0x);
        if (supportsContinuousTemperature(device)) {
            health.add(R.xml.devicesettings_temperature_automatic_enable);
        }
        if (hasDisplay()) {
            final List<Integer> display = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY);
            display.add(R.xml.devicesettings_colmi_r0x_display);
        }
        return deviceSpecificSettings;
    }

    @Override
    public int getLiveActivityFragmentPulseInterval() {
        return 2000;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.RING;
    }
}
