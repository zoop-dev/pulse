/*  Copyright (C) 2024 Gadgetbridge contributors

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

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
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

import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;

/**
 * V2 Pebble pairing flow for BLE-only devices (Pebble 2, Time 2, 2 Duo).
 * These devices require reading connectivity status, writing a pairing trigger,
 * and initiating Bluetooth bonding before the connection can complete.
 * <p>
 * Based on the PebbleOS open source implementation and libpebble3.
 */
@SuppressLint("MissingPermission")
class PebblePairingFlowV2 implements PebblePairingFlow {
    private static final Logger LOG = LoggerFactory.getLogger(PebblePairingFlowV2.class);

    private static final long BONDING_TIMEOUT_MS = 60000; // 60 seconds

    private final Context mContext;
    private final boolean mClientOnly;
    private final PairingCallback mCallback;

    private ConnectivityStatus mConnectivityStatus;
    private CountDownLatch mBondingLatch;
    private BroadcastReceiver mBondingReceiver;

    PebblePairingFlowV2(Context context, boolean clientOnly, PairingCallback callback) {
        mContext = context;
        mClientOnly = clientOnly;
        mCallback = callback;
    }

    @Override
    public void startPairing(BluetoothGatt gatt, BluetoothGattService pairingService) {
        LOG.info("PebblePairingFlowV2: Starting pairing for Pebble 2/Time 2/2 Duo");

        // Read Connectivity characteristic FIRST to determine pairing state
        BluetoothGattCharacteristic connectivityChar =
                pairingService.getCharacteristic(PebbleGATTConstants.CONNECTIVITY_CHARACTERISTIC);

        if (connectivityChar != null) {
            LOG.info("Reading connectivity characteristic to check pairing state");
            gatt.readCharacteristic(connectivityChar);
            // Continue in onCharacteristicRead() -> handleConnectivityRead()
        } else {
            LOG.error("Connectivity characteristic not found - cannot proceed with modern pairing");
            // Proceed anyway to subscription chain
            mCallback.onPairingComplete(gatt, true);
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!characteristic.getUuid().equals(PebbleGATTConstants.CONNECTIVITY_CHARACTERISTIC)) {
            return false; // Not handled by this flow
        }

        handleConnectivityRead(gatt, value);
        return true;
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!characteristic.getUuid().equals(PebbleGATTConstants.PAIRING_TRIGGER_CHARACTERISTIC)) {
            return false; // Not handled by this flow
        }

        // Pairing trigger written - now initiate Bluetooth bonding
        LOG.info("Pairing trigger written successfully (status={}), initiating bond", status);
        initiateBluetoothBond(gatt);
        return true;
    }

    @Override
    public void close() {
        cleanupBondingReceiver();
    }

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
                gatt.getService(PebbleGATTConstants.SERVICE_UUID).getCharacteristic(PebbleGATTConstants.PAIRING_TRIGGER_CHARACTERISTIC);

        if (pairingTrigger == null) {
            LOG.error("Pairing trigger characteristic not found");
            proceedAfterPairing(gatt);
            return;
        }

        if ((pairingTrigger.getProperties() & PROPERTY_WRITE) != 0) {
            LOG.info("Writing pairing trigger for Pebble Time 2 / modern Pebble");

            // Use the original working values
            // 0x09 = pinAddress(1) + autoAccept(8) - worked for normal mode
            // 0x11 = pinAddress(1) + watchAsServer(16) - for clientOnly mode
            byte[] triggerValue;
            if (mClientOnly) {
                triggerValue = new byte[]{0x11};
                LOG.info("Using clientOnly pairing trigger: 0x11");
            } else {
                triggerValue = new byte[]{0x09};
                LOG.info("Using normal pairing trigger: 0x09");
            }
            WriteAction.writeCharacteristic(gatt, pairingTrigger, triggerValue);
            // Continue in onCharacteristicWrite() -> initiateBluetoothBond()
        } else {
            // Old firmware - just read the characteristic (legacy path - shouldn't happen in modern flow)
            LOG.warn("Pairing trigger not writable in modern flow - unexpected");
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
        LOG.info("PebblePairingFlowV2: Pairing complete, proceeding to subscriptions");
        mCallback.onPairingComplete(gatt, true); // true = hasConnectivityCharacteristics
    }
}
