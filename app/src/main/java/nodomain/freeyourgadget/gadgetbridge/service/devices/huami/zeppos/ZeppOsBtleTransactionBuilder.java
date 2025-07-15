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

import androidx.annotation.StringRes;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public class ZeppOsBtleTransactionBuilder implements ZeppOsTransactionBuilder {
    private final ZeppOsBtleSupport mSupport;
    private final TransactionBuilder mBuilder;

    public ZeppOsBtleTransactionBuilder(final ZeppOsBtleSupport mSupport, final String taskName) {
        this.mSupport = mSupport;
        this.mBuilder = mSupport.createTransactionBuilder(taskName);
    }

    public ZeppOsBtleTransactionBuilder(final ZeppOsBtleSupport mSupport, final TransactionBuilder builder) {
        this.mSupport = mSupport;
        this.mBuilder = builder;
    }

    @Override
    public void setProgress(@StringRes final int textRes, final boolean ongoing, final int percentage) {
        mBuilder.setProgress(textRes, ongoing, percentage);
    }

    @Override
    public void setDeviceState(final GBDevice.State deviceState) {
        mBuilder.setDeviceState(deviceState);
    }

    @Override
    public void setBusy(@StringRes final int stringRes) {
        mBuilder.setBusyTask(stringRes);
    }

    @Override
    public void notify(final UUID characteristic, final boolean enable) {
        mBuilder.notify(characteristic, enable);
    }

    @Override
    public void write(final UUID characteristic, final byte[] arr) {
        mBuilder.write(characteristic, arr);
    }

    @Override
    public void queue() {
        mBuilder.queue();
    }

    public TransactionBuilder getTransactionBuilder() {
        return mBuilder;
    }
}
