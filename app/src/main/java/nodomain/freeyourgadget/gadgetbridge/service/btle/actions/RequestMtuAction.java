/*  Copyright (C) 2019-2024 Andreas Shimokawa, Daniel Dakhno

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

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCallback;

/// Calls {@link BluetoothGatt#requestMtu(int)}. Results are returned to
/// {@link GattCallback#onMtuChanged(BluetoothGatt, int, int)}
public class RequestMtuAction extends BtLEAction {
    private final int mtu;

    public RequestMtuAction(int mtu) {
        super(null);
        this.mtu = mtu;
    }


    @Override
    public boolean expectsResult() {
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean run(BluetoothGatt gatt) {
        return gatt.requestMtu(this.mtu);
    }

    @Override
    public String toString() {
        return getCreationTime() + " " + getClass().getSimpleName() + " mtu=" + mtu;
    }
}
