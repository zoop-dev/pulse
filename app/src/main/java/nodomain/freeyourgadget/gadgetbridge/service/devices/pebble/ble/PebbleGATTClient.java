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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

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

    // New pairing state management
    private ConnectivityStatus mConnectivityStatus;
    private CountDownLatch mBondingLatch;
    private BroadcastReceiver mBondingReceiver;
    private static final long BONDING_TIMEOUT_MS = 60000; // 60 seconds

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

            if (characteristic.getUuid().equals(CONNECTIVITY_CHARACTERISTIC)) {
                // This is required for Pebble Time 2 (EMERY) pairing
                handleConnectivityRead(gatt, characteristic.getValue());
            } else if (characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
                int battery_percent = ValueDecoder.decodePercent(characteristic, characteristic.getValue());
                LOG.info("Got battery level through read, is at {}%", battery_percent);
                GBDeviceEventBatteryInfo gbDeviceEventBatteryInfo = new GBDeviceEventBatteryInfo();
                gbDeviceEventBatteryInfo.level = battery_percent;
                gbDeviceEventBatteryInfo.state = BatteryState.BATTERY_NORMAL;
                mPebbleLESupport.getPebbleSupport().evaluateGBDeviceEvent(gbDeviceEventBatteryInfo);
            } else if (characteristic.getUuid().equals(PAIRING_TRIGGER_CHARACTERISTIC)) {
                // Legacy path: this is just a hack to force sequential ble commands for initialization
                // Only happens when READING the pairing trigger for old pebbles running fw 3.x
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
        } else if (characteristic.getUuid().equals(PAIRING_TRIGGER_CHARACTERISTIC)) {
            // Pairing trigger written - now initiate Bluetooth bonding
            // This is required for Pebble Time 2 (EMERY) pairing
            LOG.info("Pairing trigger written successfully (status={}), initiating bond", status);
            initiateBluetoothBond(gatt);
        } else if (characteristic.getUuid().equals(CONNECTIVITY_CHARACTERISTIC)) {
            // Legacy path for connectivity characteristic write
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
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.error("Service discovery failed with status {}", status);
            return;
        }

        BluetoothGattService pairingService = gatt.getService(SERVICE_UUID);
        if (pairingService == null) {
            LOG.error("Pairing service not found");
            return;
        }

        BluetoothGattCharacteristic connectionParamCharacteristic =
                pairingService.getCharacteristic(CONNECTION_PARAMETERS_CHARACTERISTIC);
        hasConnectivityCharacteristics = connectionParamCharacteristic == null;

        if (hasConnectivityCharacteristics) {
            LOG.info("This seems to be an older LE Pebble (Pebble Time), or a 2025 Pebble");
        }

        if (doPairing) {
            // Read Connectivity characteristic FIRST to determine pairing state
            // This is required for Pebble Time 2 (EMERY) pairing
            BluetoothGattCharacteristic connectivityChar =
                    pairingService.getCharacteristic(CONNECTIVITY_CHARACTERISTIC);
            if (connectivityChar != null) {
                LOG.info("Reading connectivity characteristic to check pairing state");
                gatt.readCharacteristic(connectivityChar);
                // Continue in onCharacteristicRead() -> handleConnectivityRead()
            } else {
                // Fallback for very old firmware - use legacy method
                LOG.warn("Connectivity characteristic not found, using legacy pairing");
                proceedWithLegacyPairing(gatt);
            }
        } else {
            if (hasConnectivityCharacteristics) {
                subscribeToConnectivity(gatt);
            } else {
                subscribeToConnectionParams(gatt);
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

    // ================== New Pairing Methods (based on libpebble3) ==================

    /**
     * Handle the connectivity characteristic read to determine pairing state.
     * Based on libpebble3 ConnectivityWatcher and PebblePairing.
     */
    private void handleConnectivityRead(BluetoothGatt gatt, byte[] value) {
        mConnectivityStatus = new ConnectivityStatus(value);
        LOG.info("Connectivity status: {}", mConnectivityStatus);

        BluetoothDevice device = gatt.getDevice();
        int bondState = device.getBondState();
        boolean phoneBonded = bondState == BluetoothDevice.BOND_BONDED;
        LOG.info("Phone bond state: {} (BONDED={})", bondState, BluetoothDevice.BOND_BONDED);

        // If BOTH sides are already paired, skip pairing trigger and bonding entirely
        if (mConnectivityStatus.paired && phoneBonded) {
            LOG.info("Already paired on both sides - skipping pairing trigger, proceeding with subscriptions");
            proceedAfterPairing(gatt);
            return;
        }

        LOG.info("Proceeding with pairing trigger write (watch.paired={}, phone.bonded={})",
                mConnectivityStatus.paired, phoneBonded);

        BluetoothGattCharacteristic pairingTrigger =
                gatt.getService(SERVICE_UUID).getCharacteristic(PAIRING_TRIGGER_CHARACTERISTIC);

        if (pairingTrigger == null) {
            LOG.error("Pairing trigger characteristic not found");
            proceedAfterPairing(gatt);
            return;
        }

        if ((pairingTrigger.getProperties() & PROPERTY_WRITE) != 0) {
            LOG.info("Writing pairing trigger for Pebble Time 2 / modern Pebble");

            // Use the original working values, but we'll add createBond() after
            // 0x09 = pinAddress(1) + autoAccept(8) - worked for normal mode
            // 0x11 = pinAddress(1) + watchAsServer(16) - for clientOnly mode
            byte[] triggerValue;
            if (mPebbleLESupport.clientOnly) {
                triggerValue = new byte[]{0x11};
                LOG.info("Using clientOnly pairing trigger: 0x11");
            } else {
                triggerValue = new byte[]{0x09};
                LOG.info("Using normal pairing trigger: 0x09");
            }
            WriteAction.writeCharacteristic(gatt, pairingTrigger, triggerValue);
            // Continue in onCharacteristicWrite() -> initiateBluetoothBond()
        } else {
            // Old firmware - just read the characteristic (legacy path)
            LOG.info("Pairing trigger not writable, using legacy read");
            gatt.readCharacteristic(pairingTrigger);
        }
    }

    /**
     * Initiate Bluetooth bonding after writing the pairing trigger.
     * Based on libpebble3 PebblePairing.requestBlePairing().
     */
    private void initiateBluetoothBond(BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        int currentBondState = device.getBondState();
        LOG.info("Current bond state before pairing: {} (NONE={}, BONDING={}, BONDED={})",
                currentBondState, BluetoothDevice.BOND_NONE,
                BluetoothDevice.BOND_BONDING, BluetoothDevice.BOND_BONDED);

        // If already bonded, just proceed - don't try to re-bond
        if (currentBondState == BluetoothDevice.BOND_BONDED) {
            LOG.info("Device already bonded, proceeding with subscriptions");
            proceedAfterPairing(gatt);
            return;
        }

        LOG.info("Initiating Bluetooth bond with {}ms timeout", BONDING_TIMEOUT_MS);

        // Register bond state receiver
        mBondingLatch = new CountDownLatch(1);
        mBondingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice bondDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bondDevice == null || !bondDevice.getAddress().equals(device.getAddress())) {
                    return;
                }
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                LOG.info("Bond state changed: {}", bondState);

                if (bondState == BluetoothDevice.BOND_BONDED) {
                    LOG.info("Bonding succeeded!");
                    mBondingLatch.countDown();
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    int reason = intent.getIntExtra("android.bluetooth.device.extra.REASON", -1);
                    LOG.error("Bonding failed with reason: {}", reason);
                    mBondingLatch.countDown();
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        ContextCompat.registerReceiver(mContext, mBondingReceiver, filter, ContextCompat.RECEIVER_EXPORTED);

        // Start bonding on background thread to avoid blocking GATT callbacks
        new Thread(() -> {
            try {
                LOG.info("Calling createBond() on device {}", device.getAddress());
                boolean bondInitiated = device.createBond();
                LOG.info("createBond() returned: {}", bondInitiated);

                if (!bondInitiated) {
                    LOG.error("Failed to initiate bonding - createBond() returned false");
                    LOG.info("Current bond state after failed createBond: {}", device.getBondState());
                    cleanupBondingReceiver();
                    // Still proceed to try connection
                    new Handler(Looper.getMainLooper()).post(() -> proceedAfterPairing(gatt));
                    return;
                }

                LOG.info("Waiting for bond state change (timeout: {}ms)...", BONDING_TIMEOUT_MS);
                boolean bonded = mBondingLatch.await(BONDING_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                int finalBondState = device.getBondState();
                LOG.info("Bond wait completed: latch={}, finalState={}", bonded, finalBondState);

                if (bonded && finalBondState == BluetoothDevice.BOND_BONDED) {
                    LOG.info("Bonding completed successfully!");
                } else {
                    LOG.warn("Bonding timed out or failed (latch={}, state={}), proceeding anyway",
                            bonded, finalBondState);
                }
            } catch (InterruptedException e) {
                LOG.error("Bonding interrupted", e);
            } catch (SecurityException e) {
                LOG.error("SecurityException during createBond - missing BLUETOOTH_CONNECT permission?", e);
            } catch (Exception e) {
                LOG.error("Unexpected error during bonding", e);
            } finally {
                cleanupBondingReceiver();
                // Continue with subscription chain on main thread
                new Handler(Looper.getMainLooper()).post(() -> proceedAfterPairing(gatt));
            }
        }).start();
    }

    /**
     * Clean up the bonding broadcast receiver.
     */
    private void cleanupBondingReceiver() {
        if (mBondingReceiver != null) {
            try {
                mContext.unregisterReceiver(mBondingReceiver);
            } catch (Exception e) {
                LOG.warn("Error unregistering bonding receiver: {}", e.getMessage());
            }
            mBondingReceiver = null;
        }
    }

    /**
     * Continue with the subscription chain after pairing is complete or skipped.
     */
    private void proceedAfterPairing(BluetoothGatt gatt) {
        LOG.info("Proceeding after pairing to subscription chain");
        if (hasConnectivityCharacteristics) {
            subscribeToConnectivity(gatt);
        } else {
            subscribeToConnectionParams(gatt);
        }
    }

    /**
     * Legacy pairing path for backward compatibility with older firmwares.
     */
    private void proceedWithLegacyPairing(BluetoothGatt gatt) {
        LOG.info("Using legacy pairing method");
        BluetoothGattCharacteristic characteristic =
                gatt.getService(SERVICE_UUID).getCharacteristic(PAIRING_TRIGGER_CHARACTERISTIC);
        if ((characteristic.getProperties() & PROPERTY_WRITE) != 0) {
            byte[] value = mPebbleLESupport.clientOnly ? new byte[]{0x11} : new byte[]{0x09};
            WriteAction.writeCharacteristic(gatt, characteristic, value);
        } else {
            gatt.readCharacteristic(characteristic);
        }
    }

    /**
     * Determines if we need to pair based on connectivity status and bond state.
     * Based on libpebble3 PebbleBle.kt pairing logic.
     */
    private boolean shouldPair(ConnectivityStatus status, boolean phoneBonded) {
        if (status.paired && phoneBonded) {
            LOG.info("Already paired on both sides, skipping pairing");
            return false;
        }
        if (status.paired && !phoneBonded) {
            LOG.info("Watch thinks it's paired, phone does not - need to re-pair");
            return true;
        }
        if (!status.paired && phoneBonded) {
            LOG.info("Phone thinks it's paired, watch does not - need to re-pair");
            return true;
        }
        LOG.info("Neither side paired - need full pairing");
        return true;
    }

    /**
     * Builds the pairing trigger value with proper bit flags.
     * Based on libpebble3 PebblePairing.makePairingTriggerValue()
     *
     * Bit 0: pinAddress - pin BLE address to prevent MAC rotation
     * Bit 1: noSecurityRequest - if true, watch won't request security (phone will via createBond)
     * Bit 2: forceSecurityRequest - if true, watch will request security (inverse of bit 1)
     * Bit 3: autoAcceptFuturePairing
     * Bit 4: watchAsGattServer - for reversed PPoG mode
     *
     * When phone calls createBond(), set noSecurityRequest=true so watch doesn't also request.
     */
    private byte buildPairingTriggerValue(boolean pinAddress, boolean noSecurityRequest,
                                          boolean autoAcceptFuture, boolean watchAsGattServer) {
        byte value = 0;
        if (pinAddress) value |= 0b00001;          // Bit 0
        if (noSecurityRequest) value |= 0b00010;   // Bit 1
        if (!noSecurityRequest) value |= 0b00100;  // Bit 2: force security (only if bit 1 is 0)
        if (autoAcceptFuture) value |= 0b01000;    // Bit 3
        if (watchAsGattServer) value |= 0b10000;   // Bit 4
        LOG.debug("buildPairingTriggerValue: pin={}, noSec={}, auto={}, server={} -> 0x{}",
                pinAddress, noSecurityRequest, autoAcceptFuture, watchAsGattServer,
                String.format("%02X", value));
        return value;
    }

    public void close() {
        cleanupBondingReceiver();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
}
