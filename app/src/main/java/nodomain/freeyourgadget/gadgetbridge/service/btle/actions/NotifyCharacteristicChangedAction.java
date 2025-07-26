/*  Copyright (C) 2023-2025 Frank Ertl, Thomas Kuehne

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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothStatusCodes;
import android.os.Build;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BleNamesResolver;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEServerAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class NotifyCharacteristicChangedAction extends BtLEServerAction {
    private static final Logger LOG = LoggerFactory.getLogger(NotifyCharacteristicChangedAction.class);

    private final BluetoothGattCharacteristic characteristic;
    private final byte[] value;

    public NotifyCharacteristicChangedAction(@NonNull BluetoothDevice device,
                                             @NonNull BluetoothGattCharacteristic characteristic,
                                             byte[] value) {
        super(device);
        this.characteristic = characteristic;
        this.value = value;
    }

    @Override
    public boolean expectsResult() {
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean run(BluetoothGattServer server) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int status = server.notifyCharacteristicChanged(getDevice(), characteristic, false, value);
            if (status == BluetoothStatusCodes.SUCCESS) {
                return true;
            }
            LOG.error("notifyCharacteristicChanged {} failed: {}",
                    characteristic.getUuid(), BleNamesResolver.getBluetoothStatusString(status));
            return false;
        }

        if (characteristic.setValue(value)) {
            if (server.notifyCharacteristicChanged(getDevice(), characteristic, false)) {
                return true;
            }
            LOG.error("notifyCharacteristicChanged {} failed", characteristic.getUuid());
        } else {
            LOG.error("setting value of characteristic {} failed", characteristic.getUuid());
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        String uuid = characteristic == null ? "(null)" : characteristic.getUuid().toString();
        return super.toString() + " " + uuid + " - " + GB.hexdump(value);
    }
}
