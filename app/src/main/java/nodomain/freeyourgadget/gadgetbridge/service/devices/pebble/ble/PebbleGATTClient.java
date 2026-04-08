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
class PebbleGATTClient extends BluetoothGattCallback implements PairingCallback {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleGATTClient.class);


    private BluetoothGattCharacteristic writeCharacteristics;

    private final Context mContext;
    private final PebbleLESupport mPebbleLESupport;

    private boolean hasConnectivityCharacteristics = false;
    private BluetoothGatt mBluetoothGatt;

    private CountDownLatch mWaitWriteCompleteLatch;

    // Pairing flow strategy
    private PebblePairingFlow mPairingFlow;

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

        if (characteristic.getUuid().equals(PebbleGATTConstants.MTU_CHARACTERISTIC)) {
            int newMTU = characteristic.getIntValue(FORMAT_UINT16, 0);
            LOG.info("Pebble requested MTU: {}", newMTU);
            mPebbleLESupport.setMTU(newMTU);
        } else if (characteristic.getUuid().equals(PebbleGATTConstants.PPOGATT_CHARACTERISTIC_READ)) {
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

            // Delegate to pairing flow if applicable
            if (mPairingFlow != null && mPairingFlow.onCharacteristicRead(gatt, characteristic, characteristic.getValue())) {
                return; // Pairing flow handled it
            }

            // Handle non-pairing characteristics
            if (characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
                int battery_percent = ValueDecoder.decodePercent(characteristic, characteristic.getValue());
                LOG.info("Got battery level through read, is at {}%", battery_percent);
                GBDeviceEventBatteryInfo gbDeviceEventBatteryInfo = new GBDeviceEventBatteryInfo();
                gbDeviceEventBatteryInfo.level = battery_percent;
                gbDeviceEventBatteryInfo.state = BatteryState.BATTERY_NORMAL;
                mPebbleLESupport.getPebbleSupport().evaluateGBDeviceEvent(gbDeviceEventBatteryInfo);
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

        // Delegate to pairing flow if applicable
        if (mPairingFlow != null && mPairingFlow.onCharacteristicWrite(gatt, characteristic, status)) {
            return; // Pairing flow handled it
        }

        // Handle non-pairing characteristics
        if (characteristic.getUuid().equals(PebbleGATTConstants.PPOGATT_CHARACTERISTIC_WRITE)) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                LOG.error("something went wrong when writing to PPoGATT characteristics");
            }
            if (mWaitWriteCompleteLatch != null) {
                mWaitWriteCompleteLatch.countDown();
            } else {
                LOG.warn("mWaitWriteCompleteLatch is null!");
            }
        } else if (characteristic.getUuid().equals(PebbleGATTConstants.MTU_CHARACTERISTIC)) {
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
        if (CHARACTERISTICUUID.equals(PebbleGATTConstants.CONNECTION_PARAMETERS_CHARACTERISTIC)) {
            subscribeToConnectivity(gatt);
        } else if (CHARACTERISTICUUID.equals(PebbleGATTConstants.CONNECTIVITY_CHARACTERISTIC)) {
            subscribeToMTUOrBattery(gatt);
        } else if (CHARACTERISTICUUID.equals(PebbleGATTConstants.MTU_CHARACTERISTIC) || CHARACTERISTICUUID.equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
            if (mPebbleLESupport.clientOnly) {
                subscribeToPPoGATT(gatt);
            } else {
                setMTU(gatt);
            }
        } else if (CHARACTERISTICUUID.equals(PebbleGATTConstants.PPOGATT_CHARACTERISTIC_READ)) {
            setMTU(gatt);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (mPebbleLESupport.isUnexpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onServicesDiscovered() status = {}", status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.error("Service discovery failed with status {}", status);
            return;
        }

        BluetoothGattService pairingService = gatt.getService(PebbleGATTConstants.SERVICE_UUID);
        if (pairingService == null) {
            LOG.error("Pairing service not found");
            return;
        }

        // Detect device characteristics for subscription chain selection
        BluetoothGattCharacteristic connectionParamCharacteristic =
                pairingService.getCharacteristic(PebbleGATTConstants.CONNECTION_PARAMETERS_CHARACTERISTIC);
        hasConnectivityCharacteristics = connectionParamCharacteristic == null;

        if (hasConnectivityCharacteristics) {
            LOG.info("This seems to be an older LE Pebble (Pebble Time), or a 2025 Pebble");
        }

        // Get device model for hardware-based pairing flow detection
        String deviceModel = null;
        try {
            deviceModel = mPebbleLESupport.getPebbleSupport().getDevice().getModel();
        } catch (Exception e) {
            LOG.warn("Could not get device model: {}", e.getMessage());
        }

        // Create appropriate pairing flow
        mPairingFlow = PebblePairingFlowFactory.createPairingFlow(
                mContext, deviceModel, pairingService, mPebbleLESupport.clientOnly, this);

        // Start pairing
        mPairingFlow.startPairing(gatt, pairingService);
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
        BluetoothGattDescriptor descriptor = gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.CONNECTIVITY_CHARACTERISTIC).getDescriptor(PebbleGATTConstants.CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        NotifyAction.writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.setCharacteristicNotification(gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.CONNECTIVITY_CHARACTERISTIC), true);
    }

    private void subscribeToMTU(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.MTU_CHARACTERISTIC);
        if (characteristic != null) {
            LOG.info("subscribing to mtu characteristic");
            BluetoothGattDescriptor descriptor = gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.MTU_CHARACTERISTIC).getDescriptor(PebbleGATTConstants.CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
            NotifyAction.writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.setCharacteristicNotification(gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.MTU_CHARACTERISTIC), true);
        } else {
            LOG.info("Could not find MTU Characteristic. This seems to be a 2025 Pebble");
        }
    }

    private void subscribeToConnectionParams(BluetoothGatt gatt) {
        LOG.info("subscribing to connection parameters characteristic");
        BluetoothGattDescriptor descriptor = gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.CONNECTION_PARAMETERS_CHARACTERISTIC).getDescriptor(PebbleGATTConstants.CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        NotifyAction.writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.setCharacteristicNotification(gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.CONNECTION_PARAMETERS_CHARACTERISTIC), true);
    }

    private void subscribeToMTUOrBattery(BluetoothGatt gatt) {
        // This is dumb, right now there is only one of them present in all pebbles
        BluetoothGattCharacteristic characteristic = gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.MTU_CHARACTERISTIC);
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
            BluetoothGattDescriptor descriptor = gatt.getService(GattService.UUID_SERVICE_BATTERY_SERVICE).getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL).getDescriptor(PebbleGATTConstants.CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
            NotifyAction.writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.setCharacteristicNotification(gatt.getService(GattService.UUID_SERVICE_BATTERY_SERVICE).getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL), true);
        } else {
            LOG.info("Could not find Battery Characteristic. This is normal on pre-2025 pebbles.");
        }
    }

    private void setMTU(BluetoothGatt gatt) {
        LOG.info("setting MTU");
        BluetoothGattCharacteristic characteristic = gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.MTU_CHARACTERISTIC);
        if (characteristic != null) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(PebbleGATTConstants.CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
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
        BluetoothGattDescriptor descriptor = gatt.getService(PebbleGATTConstants.PPOGATT_SERVICE_UUID).getCharacteristic(PebbleGATTConstants.PPOGATT_CHARACTERISTIC_READ).getDescriptor(PebbleGATTConstants.CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        NotifyAction.writeDescriptor(gatt, descriptor, new byte[]{1, 0});
        gatt.setCharacteristicNotification(gatt.getService(PebbleGATTConstants.PPOGATT_SERVICE_UUID).getCharacteristic(PebbleGATTConstants.PPOGATT_CHARACTERISTIC_READ), true);
        writeCharacteristics = gatt.getService(PebbleGATTConstants.PPOGATT_SERVICE_UUID).getCharacteristic(PebbleGATTConstants.PPOGATT_CHARACTERISTIC_WRITE);
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

    // ================== Pairing Callback Implementation ==================

    @Override
    public void onPairingComplete(BluetoothGatt gatt, boolean hasConnectivityCharacteristics) {
        LOG.info("Pairing complete, starting subscription chain (hasConnectivityChar={})", hasConnectivityCharacteristics);
        if (hasConnectivityCharacteristics) {
            subscribeToConnectivity(gatt);
        } else {
            subscribeToConnectionParams(gatt);
        }
    }

    public void close() {
        if (mPairingFlow != null) {
            mPairingFlow.close();
            mPairingFlow = null;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
}
