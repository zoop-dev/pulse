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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import android.content.Context;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetProgressAction;

public class ZeppOsBtbrTransactionBuilder implements ZeppOsTransactionBuilder {
    private final ZeppOsBtbrSupport mSupport;
    private final TransactionBuilder mBuilder;

    public ZeppOsBtbrTransactionBuilder(final ZeppOsBtbrSupport mSupport, final String taskName) {
        this.mSupport = mSupport;
        this.mBuilder = new TransactionBuilder(taskName);
    }

    public ZeppOsBtbrTransactionBuilder(final ZeppOsBtbrSupport mSupport, final TransactionBuilder builder) {
        this.mSupport = mSupport;
        this.mBuilder = builder;
    }

    @Override
    public void setProgress(final String text, final boolean ongoing, final int percentage, final Context context) {
        mBuilder.add(new SetProgressAction(text, ongoing, percentage, context));
    }

    @Override
    public void setDeviceState(final GBDevice device, final GBDevice.State deviceState, final Context context) {
        mBuilder.setUpdateState(device, deviceState, context);
    }

    @Override
    public void setBusy(final GBDevice device, final String string, final Context context) {
        mBuilder.add(new SetDeviceBusyAction(device, string, context));
    }

    @Override
    public void notify(final UUID characteristic, final boolean enable) {
        // Nothing to do
    }

    @Override
    public void write(final UUID characteristic, final byte[] arr) {
        mSupport.write(mBuilder, characteristic, arr, false);
    }

    @Override
    public void queue(final ZeppOsSupport support) {
        mBuilder.queue(mSupport.getQueue());
    }
}
