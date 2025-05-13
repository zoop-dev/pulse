/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Uwe Hermann, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothStatusCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BleNamesResolver;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;

/**
 * Invokes a write operation on a given GATT characteristic.
 * The result status will be made available asynchronously through the
 * {@link BluetoothGattCallback}
 */
public class WriteAction extends BtLEAction {
    private static final Logger LOG = LoggerFactory.getLogger(WriteAction.class);

    private final byte[] value;

    public WriteAction(BluetoothGattCharacteristic characteristic, byte[] value) {
        super(characteristic);
        this.value = value;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = getCharacteristic();
        int properties = characteristic.getProperties();
        //TODO: expectsResult should return false if PROPERTY_WRITE_NO_RESPONSE is true, but this leads to timing issues
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0 || ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0)) {
            return writeValue(gatt, characteristic, value);
        }

        LOG.error("WriteAction for non-writeable characteristic {}", characteristic.getUuid());
        return false;
    }

    protected boolean writeValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        return writeCharacteristic(gatt,characteristic,value);
    }

    /// shared write implementation that can be used without a BtLEAction
    @SuppressLint("MissingPermission")
    public static boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("writing to characteristic: {} : {}", characteristic.getUuid(), Logging.formatBytes(value));
        }

        if (GBApplication.isRunningTiramisuOrLater()) {
            // use API introduced in SDK level 33 to catch exceptions and more specific errors
            try {
                final int status = gatt.writeCharacteristic(characteristic, value, characteristic.getWriteType());
                if (status == BluetoothStatusCodes.SUCCESS) {
                    return true;
                }
                LOG.error("writing to characteristic {} failed: BluetoothStatusCode={}", characteristic.getUuid(), BleNamesResolver.getBluetoothStatusString(status));
            } catch (Exception e) {
                LOG.error("writing to characteristic {} failed with ", characteristic.getUuid(), e);
            }
            return false;
        }

        if (characteristic.setValue(value)) {
            return gatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    public final byte[] getValue() {
        return value;
    }

    @Override
    public boolean expectsResult() {
        return true;
    }
}
