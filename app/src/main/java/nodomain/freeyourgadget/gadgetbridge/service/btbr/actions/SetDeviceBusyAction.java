/*  Copyright (C) 2022-2025 Damien Gaignon, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.btbr.actions;

import android.bluetooth.BluetoothSocket;
import android.content.Context;

import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class SetDeviceBusyAction extends PlainAction {
    private final GBDevice device;
    private final Context context;
    private final @StringRes int busyTask;

    /**
     * When run, will mark the device as busy (or not busy).
     *
     * @param device   the device to mark
     * @param busyTask string resource for the busy task name, or {@code 0} to mark as not busy
     * @param context
     */
    public SetDeviceBusyAction(GBDevice device, @StringRes int busyTask, Context context) {
        this.device = device;
        this.busyTask = busyTask;
        this.context = context;
    }

    @Override
    public boolean run(BluetoothSocket socket) {
        if (busyTask == 0) {
            device.unsetBusyTask();
        } else {
            device.setBusyTask(busyTask, context);
        }
        device.sendDeviceUpdateIntent(context);

        return true;
    }

    @Override
    public String toString() {
        return getCreationTime() + ": " + getClass().getName() + ": "
                + (busyTask == 0 ? "<none>" : context.getString(busyTask));

    }
}
