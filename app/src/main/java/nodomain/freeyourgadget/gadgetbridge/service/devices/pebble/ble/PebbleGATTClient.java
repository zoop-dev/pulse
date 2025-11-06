/*  Copyright (C) 2016-2024 Andreas Shimokawa, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.ValueDecoder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

@SuppressLint("MissingPermission")
class PebbleGATTClient extends BluetoothGattCallback {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleGATTClient.class);

    private static final UUID SERVICE_UUID = UUID.fromString("0000fed9-0000-1000-8000-00805f9b34fb");
    private static final UUID CONNECTIVITY_CHARACTERISTIC = UUID.fromString("00000001-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID PAIRING_TRIGGER_CHARACTERISTIC = UUID.fromString("00000002-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID MTU_CHARACTERISTIC = UUID.fromString("00000003-328e-0fbb-c642-1aa6699bdada");
    private static final UUID CONNECTION_PARAMETERS_CHARACTERISTIC = UUID.fromString("00000005-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID CHARACTERISTIC_CONFIGURATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //PPoGATT service (Pebble side)
    private static final UUID PPOGATT_SERVICE_UUID = UUID.fromString("30000003-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID PPOGATT_CHARACTERISTIC_READ = UUID.fromString("30000004-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID PPOGATT_CHARACTERISTIC_WRITE = UUID.fromString("30000006-328E-0FBB-C642-1AA6699BDADA");

    private BluetoothGattCharacteristic writeCharacteristics;

    private final Context mContext;
    private final PebbleLESupport mPebbleLESupport;

    private boolean hasConnectivityCharacteristics = false;
    private final boolean doPairing = true;
    private BluetoothGatt mBluetoothGatt;

    private CountDownLatch mWaitWriteCompleteLatch;

    PebbleGATTClient(PebbleLESupport pebbleLESupport, Context context, BluetoothDevice btDevice) {
        mContext = context;
        mPebbleLESupport = pebbleLESupport;
        connectToPebble(btDevice);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (mPebbleLESupport.isUnexpectedDevice(gatt.getDevice())) {
            return;
        }

        if (characteristic.getUuid().equals(MTU_CHARACTERISTIC)) {
            int newMTU = characteristic.getIntValue(FORMAT_UINT16, 0);
            LOG.info("Pebble requested MTU: {}", newMTU);
            mPebbleLESupport.setMTU(newMTU);
        } else if (characteristic.getUuid().equals(PPOGATT_CHARACTERISTIC_READ)) {
            mPebbleLESupport.handlePPoGATTPacket(characteristic.getValue().clone());
        } else if (characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
            int battery_percent = ValueDecoder.decodePercent(characteristic, characteristic.getValue());
            LOG.info("Got battery level through notification, is at {}%", battery_percent);
        } else {
            LOG.info("onCharacteristicChanged() {} {}", characteristic.getUuid().toString(), GB.hexdump(characteristic.getValue(), 0, -1));
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (mPebbleLESupport.isUnexpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onCharacteristicRead() status = {}", status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            LOG.info("onCharacteristicRead() {} {}", characteristic.getUuid().toString(), GB.hexdump(characteristic.getValue(), 0, -1));
            if (characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
                int battery_percent = ValueDecoder.decodePercent(characteristic, characteristic.getValue());
                LOG.info("Got battery level through read, is at {}%", battery_percent);
                GBDeviceEventBatteryInfo gbDeviceEventBatteryInfo = new GBDeviceEventBatteryInfo();
                gbDeviceEventBatteryInfo.level = battery_percent;
                gbDeviceEventBatteryInfo.state = BatteryState.BATTERY_NORMAL;
                mPebbleLESupport.getPebbleSupport().evaluateGBDeviceEvent(gbDeviceEventBatteryInfo);
            } else if ((characteristic.getUuid().equals(PAIRING_TRIGGER_CHARACTERISTIC))) {
                // this is just a hack to force sequential ble commands for initialization
                // kind of event driven
                // And this never happens when not READING the pairing trigger which is only done for old pebbles running fw 3.x
                if (hasConnectivityCharacteristics) {
                    subscribeToConnectivity(gatt);
                } else {
                    subscribeToConnectionParams(gatt);
                }
            }
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (mPebbleLESupport.isUnexpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onConnectionStateChange() status = {} newState = {}", status, newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            LOG.info("calling discoverServices()");
            gatt.discoverServices();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            mPebbleLESupport.close();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (mPebbleLESupport.isUnexpectedDevice(gatt.getDevice())) {
            return;
        }
        if (characteristic.getUuid().equals(PPOGATT_CHARACTERISTIC_WRITE)) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                LOG.error("something went wrong when writing to PPoGATT characteristics");
            }
            if (mWaitWriteCompleteLatch != null) {
                mWaitWriteCompleteLatch.countDown();
            } else {
                LOG.warn("mWaitWriteCompleteLatch is null!");
            }
        } else if (characteristic.getUuid().equals(PAIRING_TRIGGER_CHARACTERISTIC) || characteristic.getUuid().equals(CONNECTIVITY_CHARACTERISTIC)) {
            //mBtDevice.createBond(); // did not work when last tried

            if (hasConnectivityCharacteristics) {
                subscribeToConnectivity(gatt);
            } else {
                subscribeToConnectionParams(gatt);
            }
        } else if (characteristic.getUuid().equals(MTU_CHARACTERISTIC)) {
            gatt.requestMtu(339);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor bluetoothGattDescriptor, int status) {
        if (mPebbleLESupport.isUnexpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onDescriptorWrite() status={}", status);

        UUID CHARACTERISTICUUID = bluetoothGattDescriptor.getCharacteristic().getUuid();

        // this is just a hack to force sequential ble commands for initialization
        // kind of event driven
        if (CHARACTERISTICUUID.equals(CONNECTION_PARAMETERS_CHARACTERISTIC)) {
            subscribeToConnectivity(gatt);
        } else if (CHARACTERISTICUUID.equals(CONNECTIVITY_CHARACTERISTIC)) {
            subscribeToMTUOrBattery(gatt);
        } else if (CHARACTERISTICUUID.equals(MTU_CHARACTERISTIC) || CHARACTERISTICUUID.equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
            if (mPebbleLESupport.clientOnly) {
                subscribeToPPoGATT(gatt);
            } else {
                setMTU(gatt);
            }
        } else if (CHARACTERISTICUUID.equals(PPOGATT_CHARACTERISTIC_READ)) {
            setMTU(gatt);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (mPebbleLESupport.isUnexpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onServicesDiscovered() status = {}", status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BluetoothGattCharacteristic connectionParamCharacteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTION_PARAMETERS_CHARACTERISTIC);
            hasConnectivityCharacteristics = connectionParamCharacteristic == null;

            if (hasConnectivityCharacteristics) {
                LOG.info("This seems to be an older le enabled Pebble (Pebble Time), or a 2025 Pebble");
            }

            if (doPairing) {
                BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(PAIRING_TRIGGER_CHARACTERISTIC);
                if ((characteristic.getProperties() & PROPERTY_WRITE) != 0) {
                    LOG.info("This seems to be a >=4.0 FW Pebble, writing to pairing trigger");
                    // flags:
                    // 0 - always 1
                    // 1 - unknown
                    // 2 - always 0
                    // 3 - unknown, set on kitkat (seems to help to get a "better" pairing)
                    // 4 - unknown, set on some phones
                    byte[] value;
                    if (mPebbleLESupport.clientOnly) {
                        value = new byte[]{0x11}; // needed in clientOnly mode (TODO: try 0x19)
                    } else {
                        value = new byte[]{0x09}; // I just keep this, because it worked
                    }
                    WriteAction.writeCharacteristic(gatt, characteristic, value);
                } else {
                    LOG.info("This seems to be some <4.0 FW Pebble, reading pairing trigger");
                    gatt.readCharacteristic(characteristic);
                }
            } else {
                if (hasConnectivityCharacteristics) {
                    subscribeToConnectivity(gatt);
                } else {
                    subscribeToConnectionParams(gatt);
                }
            }
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            LOG.info("MTU changed to {}", mtu);
            mPebbleLESupport.setMTU(mtu);
        }
    }

    public void readBatteryCharacteristic() {
        BluetoothGattService serivce = mBluetoothGatt.getService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        if (serivce == null)
            return;

        BluetoothGattCharacteristic characteristic = serivce.getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL);
        if (characteristic == null)
            return;

        mBluetoothGatt.readCharacteristic(characteristic);
    }

    private void connectToPebble(BluetoothDevice btDevice) {
        if (mBluetoothGatt != null) {
            this.close();
        }
        mBluetoothGatt = btDevice.connectGatt(mContext, false, this);
    }

    private void subscribeToConnectivity(BluetoothGatt gatt) {
        LOG.info("subscribing to connectivity characteristic");
        BluetoothGattDescriptor descriptor = gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTIVITY_CHARACTERISTIC).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        NotifyAction.writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.setCharacteristicNotification(gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTIVITY_CHARACTERISTIC), true);
    }

    private void subscribeToMTU(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(MTU_CHARACTERISTIC);
        if (characteristic != null) {
            LOG.info("subscribing to mtu characteristic");
            BluetoothGattDescriptor descriptor = gatt.getService(SERVICE_UUID).getCharacteristic(MTU_CHARACTERISTIC).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
            NotifyAction.writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.setCharacteristicNotification(gatt.getService(SERVICE_UUID).getCharacteristic(MTU_CHARACTERISTIC), true);
        } else {
            LOG.info("Could not find MTU Characteristic. This seems to be a 2025 Pebble");
        }
    }

    private void subscribeToConnectionParams(BluetoothGatt gatt) {
        LOG.info("subscribing to connection parameters characteristic");
        BluetoothGattDescriptor descriptor = gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTION_PARAMETERS_CHARACTERISTIC).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        NotifyAction.writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.setCharacteristicNotification(gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTION_PARAMETERS_CHARACTERISTIC), true);
    }

    private void subscribeToMTUOrBattery(BluetoothGatt gatt) {
        // This is dumb, right now there is only one of them present in all pebbles
        BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(MTU_CHARACTERISTIC);
        if (characteristic != null) {
            subscribeToMTU(gatt);
        } else {
            subscribeToBattery(gatt);
        }
    }

    private void subscribeToBattery(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = gatt.getService(GattService.UUID_SERVICE_BATTERY_SERVICE).getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL);
        if (characteristic != null) {
            LOG.info("subscribing to battery characteristic");
            BluetoothGattDescriptor descriptor = gatt.getService(GattService.UUID_SERVICE_BATTERY_SERVICE).getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
            NotifyAction.writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.setCharacteristicNotification(gatt.getService(GattService.UUID_SERVICE_BATTERY_SERVICE).getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL), true);
        } else {
            LOG.info("Could not find Battery Characteristic. This is normal on pre-2025 pebbles.");
        }
    }

    private void setMTU(BluetoothGatt gatt) {
        LOG.info("setting MTU");
        BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(MTU_CHARACTERISTIC);
        if (characteristic != null) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
            descriptor.setValue(new byte[]{0x0b, 0x01}); // unknown
            // descriptor is not wrote back to the device, but the characteristic is.
            // Reason is unclear but writing back the descriptor instead of the characteristic breaks the connection.
            WriteAction.writeCharacteristic(gatt, characteristic, characteristic.getValue());
        } else {
            gatt.requestMtu(339);
        }
    }

    private void subscribeToPPoGATT(BluetoothGatt gatt) {
        LOG.info("subscribing to PPoGATT read characteristic");
        BluetoothGattDescriptor descriptor = gatt.getService(PPOGATT_SERVICE_UUID).getCharacteristic(PPOGATT_CHARACTERISTIC_READ).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        NotifyAction.writeDescriptor(gatt, descriptor, new byte[]{1, 0});
        gatt.setCharacteristicNotification(gatt.getService(PPOGATT_SERVICE_UUID).getCharacteristic(PPOGATT_CHARACTERISTIC_READ), true);
        writeCharacteristics = gatt.getService(PPOGATT_SERVICE_UUID).getCharacteristic(PPOGATT_CHARACTERISTIC_WRITE);
    }

    synchronized void sendDataToPebble(byte[] data) {
        mWaitWriteCompleteLatch = new CountDownLatch(1);

        boolean success = WriteAction.writeCharacteristic(mBluetoothGatt, writeCharacteristics, data.clone());
        if (!success) {
            LOG.error("could not send data to pebble (error writing characteristic)");
        } else {
            try {
                mWaitWriteCompleteLatch.await();
            } catch (InterruptedException e) {
                LOG.warn("interrupted while waiting for write complete latch");
            }
        }
        mWaitWriteCompleteLatch = null;
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
}
