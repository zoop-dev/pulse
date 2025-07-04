/*  Copyright (C) 2015-2025 Carsten Pfeiffer, Thomas Kuehne

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

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class SetDeviceBusyAction extends PlainAction {
    private final GBDevice device;
    private final Context context;
    @StringRes
    private final int busyTask;

    /**
     * When run, will mark the device as busy (or not busy).
     *
     * @param device  the device to mark
     * @param taskRes the task name to set as busy task, or {@code 0} to mark as not busy
     * @param context
     */
    public SetDeviceBusyAction(@NonNull GBDevice device, @StringRes int taskRes,
                               @NonNull Context context) {
        this.device = device;
        this.busyTask = taskRes;
        this.context = context;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
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
        return getCreationTime() + " " + getClass().getName() + " "
                + (busyTask == 0 ? "<none>" : context.getString(busyTask));
    }
}