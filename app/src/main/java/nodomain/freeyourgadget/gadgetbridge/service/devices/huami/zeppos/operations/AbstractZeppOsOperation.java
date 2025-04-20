/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations;

import android.content.Context;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

/**
 * This is a simplified version of the AbstractBTLEOperation, in order to allow for simpler
 * Zepp OS btrfcomm support without a major refactor. It should not be used for new operations.
 */
@Deprecated
public abstract class AbstractZeppOsOperation<T extends ZeppOsSupport> {
    private final T mSupport;
    protected OperationStatus operationStatus = OperationStatus.INITIAL;
    private String name;

    protected AbstractZeppOsOperation(T support) {
        mSupport = support;
    }

    public final void perform() throws IOException {
        operationStatus = OperationStatus.RUNNING;
        doPerform();
    }

    protected abstract void doPerform() throws IOException;

    protected void operationFinished() {
    }

    protected Context getContext() {
        return mSupport.getContext();
    }

    protected GBDevice getDevice() {
        return mSupport.getDevice();
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (name != null) {
            return name;
        }
        String busyTask = getDevice().getBusyTask();
        if (busyTask != null) {
            return busyTask;
        }
        return getClass().getSimpleName();
    }

    protected void unsetBusy() {
        if (getDevice().isBusy()) {
            getDevice().unsetBusyTask();
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    public boolean isOperationRunning() {
        return operationStatus == OperationStatus.RUNNING;
    }

    public boolean isOperationFinished() {
        return operationStatus == OperationStatus.FINISHED;
    }

    public T getSupport() {
        return mSupport;
    }
}
