/*  Copyright (C) 2019-2025 Andreas Böhler, Thomas Kuehne

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
import android.bluetooth.BluetoothGattServer;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEServerAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Invokes a response on a given GATT characteristic read.
 */
public class ServerResponseAction extends BtLEServerAction {
    private final byte[] value;
    private final int requestId;
    private final int status;
    private final int offset;

    public ServerResponseAction(BluetoothDevice device, int requestId, int status, int offset, byte[] data) {
        super(device);
        this.value = data;
        this.requestId = requestId;
        this.status = status;
        this.offset = offset;
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean run(BluetoothGattServer server) {
        return server.sendResponse(getDevice(), requestId, status, offset, getValue());
    }

    protected final byte[] getValue() {
        return value;
    }

    @Override
    public boolean expectsResult() {
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " #" + requestId + " - " + GB.hexdump(getValue());
    }
}
