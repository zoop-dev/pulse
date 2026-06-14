/*  Copyright (C) 2015-2026 Carsten Pfeiffer, Uwe Hermann, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

/**
 * The Bluedroid implementation only allows performing one GATT request at a time.
 * As they are asynchronous anyway, we encapsulate every GATT request (read and write)
 * inside a runnable action.
 * <p>
 * These actions are then executed one after another, ensuring that every action's result
 * has been posted before invoking the next action.
 * </p>
 */
public abstract class BtLEAction {
    private final BluetoothGattCharacteristic characteristic;
    private final long creationTimestamp;

    public BtLEAction(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
        creationTimestamp = System.currentTimeMillis();
    }

    /**
     * Returns true if this action expects an (async) result which must
     * be waited for, before continuing with other actions.
     * <p>
     * This is needed because the current Bluedroid stack can only deal
     * with one single bluetooth operation at a time.
     * </p>
     */
    public abstract boolean expectsResult();

    /**
     * Executes this action, e.g. reads or write a {@link GattCharacteristic}.
     *
     * @return {@code true} if the action was successful, {@code false} otherwise
     */
    public abstract boolean run(@NonNull BluetoothGatt gatt);

    /**
     * Returns the GATT characteristic being read/written/...
     *
     * @return the GATT characteristic, or {@code null}
     */
    @Nullable
    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    @NonNull
    protected String getCreationTime() {
        return DateTimeUtils.formatLocalTime(creationTimestamp);
    }

    @NonNull
    public String toString() {
        BluetoothGattCharacteristic characteristic = getCharacteristic();
        String uuid = characteristic == null ? "(null)" : characteristic.getUuid().toString();
        return getCreationTime() + " " + getClass().getSimpleName() + " on characteristic " + uuid;
    }
}
