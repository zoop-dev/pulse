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
package nodomain.freeyourgadget.gadgetbridge.service.btbr;

import android.bluetooth.BluetoothSocket;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.function.Predicate;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.FunctionAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.WaitAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.WriteAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.SetDeviceBusyAction;

public class TransactionBuilder {
    private final AbstractBTBRDeviceSupport mDeviceSupport;
    private final Transaction mTransaction;
    private boolean mQueued;

    TransactionBuilder(String taskName, @NonNull AbstractBTBRDeviceSupport deviceSupport) {
        mTransaction = new Transaction(taskName);
        mDeviceSupport = deviceSupport;
    }

    @NonNull
    public TransactionBuilder write(byte... data) {
        WriteAction action = new WriteAction(data);
        return add(action);
    }


    /**
     * Causes the queue to sleep for the specified time.
     * Note that this is usually a bad idea, since it will not be able to process messages
     * during that time. It is also likely to cause race conditions.
     * @param millis the number of milliseconds to sleep
     * @see Thread#sleep(long)
     */
    @NonNull
    public TransactionBuilder wait(@IntRange(from = 0L) int millis) {
        WaitAction action = new WaitAction(millis);
        return add(action);
    }

    /// Causes the {@link BtBRQueue} to execute the {@link Predicate} and expect no {@link SocketCallback} result.
    /// The {@link Transaction} is aborted if the predicate throws an {@link Exception} or returns {@code false}.
    ///
    /// @see #run(Runnable)
    @NonNull
    public TransactionBuilder run(@NonNull Predicate<? super BluetoothSocket> predicate) {
        BtBRAction action = new FunctionAction(predicate);
        return add(action);
    }

    /// Causes the {@link BtBRQueue} to execute the {@link Runnable} and expect no {@link SocketCallback} result.
    /// The {@link Transaction} is aborted if the runnable throws an {@link Exception}.
    ///
    /// @see #run(Predicate)
    @NonNull
    public TransactionBuilder run(@NonNull Runnable runnable) {
        BtBRAction action = new FunctionAction(runnable);
        return add(action);
    }

    @NonNull
    public TransactionBuilder add(@NonNull BtBRAction action) {
        mTransaction.add(action);
        return this;
    }

    /// Sets the device's state and sends an {@link GBDevice#ACTION_DEVICE_CHANGED} intent
    @NonNull
    public TransactionBuilder setDeviceState(GBDevice.State state) {
        BtBRAction action = new SetDeviceStateAction(mDeviceSupport.getDevice(), state, mDeviceSupport.getContext());
        return add(action);
    }

    /// updates the progress bar
    /// @see SetProgressAction#SetProgressAction
    @NonNull
    public TransactionBuilder setProgress(@StringRes int textRes, boolean ongoing, int percentage) {
        BtBRAction action = new SetProgressAction(textRes, ongoing, percentage, mDeviceSupport.getContext());
        return add(action);
    }

    /// Set the device as busy or not ({@code taskName = 0}).
    /// @see SetDeviceBusyAction#SetDeviceBusyAction
    @NonNull
    public TransactionBuilder setBusyTask(@StringRes final int taskName) {
        BtBRAction action = new SetDeviceBusyAction(mDeviceSupport.getDevice(), taskName, mDeviceSupport.getContext());
        return add(action);
    }

    /**
     * Sets a SocketCallback instance that will be called when the transaction is executed,
     * resulting in SocketCallback events.
     *
     * @param callback the callback to set, may be null
     */
    public void setCallback(@Nullable SocketCallback callback) {
        mTransaction.setCallback(callback);
    }

    /**
     * To be used as the final step to execute the transaction by the queue.
     * @throws IllegalStateException if this builder has already been queued
     * @see #queueConnected()
     */
    public void queue() {
        if (mQueued) {
            throw new IllegalStateException("This builder had already been queued. You must not reuse it.");
        }
        mQueued = true;
        BtBRQueue queue = mDeviceSupport.getQueue();
        queue.add(mTransaction);
    }

    @VisibleForTesting
    @NonNull
    public Transaction getTransaction() {
        return mTransaction;
    }

    public String getTaskName() {
        return mTransaction.getTaskName();
    }

    /// Ensures that the device is connected and (only then) performs the actions of the given
    /// transaction builder.
    ///
    /// @throws IOException if unable to connect to the device
    /// @throws IllegalStateException if this builder has already been queued
    /// @see #queue()
    public void queueConnected() throws IOException {
        if (!mDeviceSupport.isConnected()) {
            if (!mDeviceSupport.connect()) {
                throw new IOException("Unable to connect to device: " + mDeviceSupport.getDevice());
            }
        }
        queue();
    }
}
