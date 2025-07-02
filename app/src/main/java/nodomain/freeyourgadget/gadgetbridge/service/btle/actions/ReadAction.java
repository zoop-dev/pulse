/*  Copyright (C) 2015-2024 Carsten Pfeiffer, Daniele Gobbetti

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
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCallback;

/**
 * Invokes a read operation on a given {@link BluetoothGattCharacteristic}.
 * The result will be made available asynchronously through
 * {@link GattCallback#onCharacteristicRead}
 */
public class ReadAction extends BtLEAction {
    private static final Logger LOG = LoggerFactory.getLogger(ReadAction.class);

    public ReadAction(BluetoothGattCharacteristic characteristic) {
        super(characteristic);
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean run(BluetoothGatt gatt) {
        int properties = getCharacteristic().getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            return gatt.readCharacteristic(getCharacteristic());
        }
        LOG.error("ReadAction for non-readable characteristic {}", getCharacteristic().getUuid());
        return false;
    }

    @Override
    public boolean expectsResult() {
        return true;
    }

    @Override
    public String toString() {
        BluetoothGattCharacteristic characteristic = getCharacteristic();
        String uuid = characteristic == null ? "(null)" : characteristic.getUuid().toString();
        return getCreationTime() + " " + getClass().getSimpleName() + " " + uuid;
    }
}
