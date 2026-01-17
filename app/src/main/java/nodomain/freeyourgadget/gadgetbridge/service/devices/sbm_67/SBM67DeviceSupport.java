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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sbm_67;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.sbm_67.SBM67BloodPressureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sbm_67.SBM67Coordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.SBM67BloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SBM67DeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(nodomain.freeyourgadget.gadgetbridge.service.devices.sbm_67.SBM67DeviceSupport.class);

    private final DeviceInfoProfile<SBM67DeviceSupport> deviceInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();

    private List<SBM67BloodPressureSample> samples;

    public SBM67DeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_BLOOD_PRESSURE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);

        samples = new ArrayList<>();

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        IntentListener mListener = intent -> {
            String s = intent.getAction();
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(s)) {
                DeviceInfo deviceInfo = intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO);
                if (deviceInfo != null) {
                    handleDeviceInfo(deviceInfo);
                }
            }
        };
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        BroadcastReceiver commandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                    GBDevice.DeviceUpdateSubject subject = (GBDevice.DeviceUpdateSubject) intent.getSerializableExtra(GBDevice.EXTRA_UPDATE_SUBJECT);

                    if (GBDevice.DeviceUpdateSubject.CONNECTION_STATE.equals(subject) &&
                            GBDevice.State.NOT_CONNECTED.equals(gbDevice.getState()) &&
                            !samples.isEmpty()) {
                        // This particular device disconnects automatically once the transfer is finished, hence this method of persisting the samples works, but it's far from ideal
                        persistSamples();
                        samples.clear();
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(GBApplication.getContext()).registerReceiver(commandReceiver, commandFilter);
    }

    /**
     * Subclasses should populate the given builder to initialize the device (if necessary).
     *
     * @param builder
     * @return the same builder as passed as the argument
     */
    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {

        samples = new ArrayList<>();

        deviceInfoProfile.requestDeviceInfo(builder);

        builder.setCallback(this);

        // mark the device as initializing
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        builder.notify(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT), true);

        // mark the device as initialized
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        return builder;
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {

        if (GattCharacteristic.UUID_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT.equals(characteristic.getUuid())) {
            ByteBuffer incoming = ByteBuffer.wrap(value);
            incoming.order(ByteOrder.LITTLE_ENDIAN);

            final SBM67BloodPressureSample bloodPressureSample = new SBM67BloodPressureSample();

            byte unk = incoming.get();
            short syst = incoming.getShort();//
            short dia = incoming.getShort();//
            if ((unk & 1) != 0) {
                bloodPressureSample.setBpSystolic((int) ((int) syst * 7.50061683d));
                bloodPressureSample.setBpDiastolic((int) ((int) dia * 7.50061683d));
            } else {
                bloodPressureSample.setBpSystolic((int) syst);
                bloodPressureSample.setBpDiastolic((int) dia);
            }
            short map = incoming.getShort(); //mean arterial pressure
            bloodPressureSample.setMeanArterialPressure((int) map);

            if ((unk & 2) != 0) {
                short year = incoming.getShort();
                byte month = incoming.get();
                byte day = incoming.get();

                byte hour = incoming.get();
                byte minute = incoming.get();
                byte second = incoming.get();

                Calendar c = GregorianCalendar.getInstance();
                c.set(year, month - 1, day, hour, minute, second);

                bloodPressureSample.setTimestamp(c.getTime().getTime());
            }
            byte pulse = incoming.get();//
            bloodPressureSample.setPulse((int) pulse & 0xff);
            incoming.get();//skip
            byte user = incoming.get();//
            bloodPressureSample.setUserIndex((int) user);

            byte status = incoming.get();//
            bloodPressureSample.setReadingStatus((int) status);
            bloodPressureSample.setHeartRhythmDisorder((status & 4) != 0);
            bloodPressureSample.setRestingIndicator((status & 64) != 0);

            samples.add(bloodPressureSample);
            return true;
        }

        return super.onCharacteristicChanged(gatt, characteristic, value);
    }


    protected void persistSamples() {

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final SBM67Coordinator coordinator = (SBM67Coordinator) getDevice().getDeviceCoordinator();
            final SBM67BloodPressureSampleProvider sampleProvider = coordinator.getBloodPressureSampleProvider(getDevice(), session);

            for (final SBM67BloodPressureSample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} blood pressure samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving blood pressure samples", Toast.LENGTH_LONG, GB.ERROR, e);
            return;
        }

        return;
    }

    /**
     * Returns true if a connection attempt shall be made automatically whenever
     * needed (e.g. when a notification shall be sent to the device while not connected.
     */
    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
