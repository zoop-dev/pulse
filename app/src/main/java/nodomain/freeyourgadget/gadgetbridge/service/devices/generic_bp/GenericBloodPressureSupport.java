/*  Copyright (C) 2023 Daniele Gobbetti

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.generic_bp;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.sbm_67.GenericBloodPressureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericBloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.bloodpressure.BloodPressureMeasurement;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.bloodpressure.BloodPressureProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GenericBloodPressureSupport extends AbstractBTLESingleDeviceSupport implements IntentListener {
    private static final Logger LOG = LoggerFactory.getLogger(GenericBloodPressureSupport.class);

    private final DeviceInfoProfile<GenericBloodPressureSupport> deviceInfoProfile;
    private final BloodPressureProfile<GenericBloodPressureSupport> bloodPressureProfile;
    private final BroadcastReceiver commandReceiver;

    private final List<BloodPressureMeasurement> measurements = new ArrayList<>();

    public GenericBloodPressureSupport() {
        super(LOG);

        addSupportedService(GattService.UUID_SERVICE_BLOOD_PRESSURE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(this);
        addSupportedProfile(deviceInfoProfile);

        bloodPressureProfile = new BloodPressureProfile<>(this);
        bloodPressureProfile.addListener(this);
        addSupportedProfile(bloodPressureProfile);

        commandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                    final GBDevice.DeviceUpdateSubject subject = (GBDevice.DeviceUpdateSubject) intent.getSerializableExtra(GBDevice.EXTRA_UPDATE_SUBJECT);

                    if (GBDevice.DeviceUpdateSubject.CONNECTION_STATE.equals(subject) &&
                            GBDevice.State.NOT_CONNECTED.equals(gbDevice.getState()) &&
                            !measurements.isEmpty()) {
                        // This particular device disconnects automatically once the transfer is finished, hence this method of persisting the samples works, but it's far from ideal
                        persistMeasurements();
                        measurements.clear();
                    }
                }
            }
        };

        final IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(GBApplication.getContext()).registerReceiver(commandReceiver, commandFilter);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        measurements.clear();

        // mark the device as initializing
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        // request device info
        deviceInfoProfile.requestDeviceInfo(builder);

        // enable blood pressure notifications
        bloodPressureProfile.enableNotify(builder, true);

        // mark the device as initialized
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    @CallSuper
    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            try {
                LocalBroadcastManager.getInstance(GBApplication.getContext()).unregisterReceiver(commandReceiver);
            } catch (final Exception e) {
                LOG.error("Failed to unregister receiver", e);
            }

            super.dispose();
        }
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic,
                                           final byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        LOG.warn("Unhandled characteristic changed: {} {}", characteristic.getUuid(), GB.hexdump(value));

        return false;
    }

    protected void persistMeasurements() {
        if (measurements.isEmpty()) {
            LOG.debug("No measurements to persist");
            return;
        }

        LOG.debug("Will persist {} blood pressure measurements", measurements.size());

        final List<GenericBloodPressureSample> samples = measurements.stream().map(measurement -> {
            final GenericBloodPressureSample sample = new GenericBloodPressureSample();
            sample.setTimestamp(measurement.getTimestamp());
            sample.setBpSystolic(measurement.getSystolicMmHg());
            sample.setBpDiastolic(measurement.getDiastolicMmHg());
            sample.setMeanArterialPressure(measurement.getMeanArterialPressure());
            sample.setPulseRate(measurement.getPulseRate());
            sample.setUserIndex(measurement.getUserId());
            sample.setMeasurementStatus(measurement.getMeasurementStatus());
            return sample;
        }).collect(Collectors.toList());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GenericBloodPressureSampleProvider sampleProvider = new GenericBloodPressureSampleProvider(getDevice(), session);
            sampleProvider.persistForDevice(getContext(), getDevice(), samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving blood pressure samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }

        GB.signalActivityDataFinish(getDevice());
    }

    @Override
    public void notify(final Intent intent) {
        if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(intent.getAction())) {
            final DeviceInfo deviceInfo = intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO);
            if (deviceInfo != null) {
                handleDeviceInfo(deviceInfo);
            }
        }
        if (BloodPressureProfile.ACTION_BLOOD_PRESSURE.equals(intent.getAction())) {
            final BloodPressureMeasurement measurement = intent.getParcelableExtra(BloodPressureProfile.EXTRA_BLOOD_PRESSURE);
            if (measurement != null) {
                // If we're not marked as busy yet, do it now
                if (!getDevice().isBusy()) {
                    getDevice().setBusyTask(R.string.busy_task_fetch_blood_pressure_data, getContext());
                    getDevice().sendDeviceUpdateIntent(getContext());
                }

                handleBloodPressureMeasurement(measurement);
            }
        }
    }

    private void handleDeviceInfo(final nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    private void handleBloodPressureMeasurement(final BloodPressureMeasurement measurement) {
        measurements.add(measurement);
    }
}
