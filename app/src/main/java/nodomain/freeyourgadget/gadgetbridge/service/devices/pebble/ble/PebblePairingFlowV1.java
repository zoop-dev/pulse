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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;

/**
 * V1 Pebble pairing flow for older BLE-enabled devices (Pebble Time, Pebble Time Round).
 * These devices use a simpler pairing protocol without connectivity status checking
 * or explicit bonding management.
 * <p>
 * This is the original pairing flow from before commit 945e06946.
 */
@SuppressLint("MissingPermission")
class PebblePairingFlowV1 implements PebblePairingFlow {
    private static final Logger LOG = LoggerFactory.getLogger(PebblePairingFlowV1.class);

    private final boolean mClientOnly;
    private final PairingCallback mCallback;

    PebblePairingFlowV1(boolean clientOnly, PairingCallback callback) {
        mClientOnly = clientOnly;
        mCallback = callback;
    }

    @Override
    public void startPairing(BluetoothGatt gatt, BluetoothGattService pairingService) {
        LOG.info("PebblePairingFlowV1: Starting pairing for Pebble Time/Time Round");

        BluetoothGattCharacteristic characteristic =
                pairingService.getCharacteristic(PebbleGATTConstants.PAIRING_TRIGGER_CHARACTERISTIC);

        if (characteristic == null) {
            LOG.error("Pairing trigger characteristic not found");
            // Proceed anyway to subscription chain
            mCallback.onPairingComplete(gatt, false);
            return;
        }

        if ((characteristic.getProperties() & PROPERTY_WRITE) != 0) {
            LOG.info("This seems to be a >=4.0 FW Pebble, writing to pairing trigger");
            // flags:
            // 0 - always 1
            // 1 - unknown
            // 2 - always 0
            // 3 - unknown, set on kitkat (seems to help to get a "better" pairing)
            // 4 - unknown, set on some phones
            byte[] value;
            if (mClientOnly) {
                value = new byte[]{0x11}; // needed in clientOnly mode
            } else {
                value = new byte[]{0x09}; // I just keep this, because it worked
            }
            WriteAction.writeCharacteristic(gatt, characteristic, value);
            // Continue in onCharacteristicWrite()
        } else {
            LOG.info("This seems to be some <4.0 FW Pebble, reading pairing trigger");
            gatt.readCharacteristic(characteristic);
            // Continue in onCharacteristicRead()
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!characteristic.getUuid().equals(PebbleGATTConstants.PAIRING_TRIGGER_CHARACTERISTIC)) {
            return false; // Not handled by this flow
        }

        // Legacy pairing trigger read complete - proceed to subscriptions
        LOG.info("V1 pairing trigger read complete, proceeding to subscriptions");
        proceedAfterPairing(gatt);
        return true;
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!characteristic.getUuid().equals(PebbleGATTConstants.PAIRING_TRIGGER_CHARACTERISTIC)) {
            return false; // Not handled by this flow
        }

        // Legacy pairing trigger write complete - proceed to subscriptions
        LOG.info("V1 pairing trigger write complete (status={}), proceeding to subscriptions", status);
        proceedAfterPairing(gatt);
        return true;
    }

    @Override
    public void close() {
        // No resources to clean up for V1 flow
    }

    /**
     * Continue with the subscription chain after pairing trigger is complete.
     */
    private void proceedAfterPairing(BluetoothGatt gatt) {
        LOG.info("PebblePairingFlowV1: Pairing complete, proceeding to subscriptions");
        mCallback.onPairingComplete(gatt, false); // false = NO hasConnectivityCharacteristics (V1)
    }
}
