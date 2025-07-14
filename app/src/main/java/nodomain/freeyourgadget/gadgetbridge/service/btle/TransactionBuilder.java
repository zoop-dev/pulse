/*  Copyright (C) 2015-2025 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Damien Gaignon, Daniel Dakhno, Daniele Gobbetti, Frank Ertl,
    José Rebelo, Johannes Krude, Thomas Kuehne

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
import android.content.Context;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.function.Predicate;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.BondAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.FunctionAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ReadAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ReadPhyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.RequestConnectionPriorityAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.RequestMtuAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetPreferredPhyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WaitAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;

public class TransactionBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionBuilder.class);

    private final Transaction mTransaction;
    private boolean mQueued;

    TransactionBuilder(String taskName) {
        mTransaction = new Transaction(taskName);
    }

    /// @see ReadAction
    @NonNull
    public TransactionBuilder read(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            LOG.warn("Unable to read characteristic: null");
            return this;
        }
        ReadAction action = new ReadAction(characteristic);
        return add(action);
    }

    /// Use this only if <strong>ALL</strong> conditions are true:
    /// <ol>
    /// <li>characteristic has write type {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE},</li>
    /// <li>custom {@link GattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)}
    /// uses {@link BluetoothGattCharacteristic#getValue()} and</li>
    /// <li>no {@link BluetoothGatt#beginReliableWrite()} was used.</li>
    /// </ol>
    /// @see WriteAction
    @NonNull
    public TransactionBuilder writeLegacy(BluetoothGattCharacteristic characteristic, byte... data) {
        if (characteristic == null) {
            LOG.warn("Unable to write characteristic: null");
            return this;
        }
        WriteAction action = new WriteAction(characteristic, data, true);
        return add(action);
    }

    /// @see WriteAction
    @NonNull
    public TransactionBuilder write(BluetoothGattCharacteristic characteristic, byte... data) {
        if (characteristic == null) {
            LOG.warn("Unable to write characteristic: null");
            return this;
        }
        WriteAction action = new WriteAction(characteristic, data);
        return add(action);
    }

    @NonNull
    public TransactionBuilder writeChunkedData(BluetoothGattCharacteristic characteristic, byte[] data, int chunkSize) {
        for (int start = 0; start < data.length; start += chunkSize) {
            int end = start + chunkSize;
            if (end > data.length) end = data.length;
            WriteAction action = new WriteAction(characteristic, Arrays.copyOfRange(data, start, end));
            add(action);
        }

        return this;
    }

    /// @see RequestMtuAction
    @NonNull
    public TransactionBuilder requestMtu(int mtu){
        return add(
                new RequestMtuAction(mtu)
        );
    }

    /// @see RequestConnectionPriorityAction
    @NonNull
    public TransactionBuilder requestConnectionPriority(int priority){
        return add(
                new RequestConnectionPriorityAction(priority)
        );
    }

    /// @see BondAction
    @NonNull
    public TransactionBuilder bond() {
        BondAction action = new BondAction();
        return add(action);
    }

    /// @see NotifyAction
    @NonNull
    public TransactionBuilder notify(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (characteristic == null) {
            LOG.warn("Unable to notify characteristic: null");
            return this;
        }
        NotifyAction action = createNotifyAction(characteristic, enable);
        return add(action);
    }

    protected NotifyAction createNotifyAction(BluetoothGattCharacteristic characteristic, boolean enable) {
        return new NotifyAction(characteristic, enable);
    }

    /**
     * Causes the queue to sleep for the specified time.
     * Note that this is usually a bad idea, since it will not be able to process messages
     * during that time. It is also likely to cause race conditions.
     * @param millis the number of milliseconds to sleep
     */
    @NonNull
    public TransactionBuilder wait(int millis) {
        WaitAction action = new WaitAction(millis);
        return add(action);
    }

    /// Causes the {@link BtLEQueue} to execute the {@link Predicate} and expect no {@link GattCallback} result.
    /// The {@link Transaction} is aborted if the predicate throws an {@link Exception} or returns {@code false}.
    ///
    /// @see #run(Runnable)
    @NonNull
    public TransactionBuilder run(@NonNull Predicate<? super BluetoothGatt> predicate) {
        BtLEAction action = new FunctionAction(predicate);
        return add(action);
    }

    /// Causes the {@link BtLEQueue} to execute the {@link Runnable} and expect no {@link GattCallback} result.
    /// The {@link Transaction} is aborted if the runnable throws an {@link Exception}.
    ///
    /// @see #run(Predicate)
    @NonNull
    public TransactionBuilder run(@NonNull Runnable runnable) {
        BtLEAction action = new FunctionAction(runnable);
        return add(action);
    }

    @NonNull
    public TransactionBuilder add(@NonNull BtLEAction action) {
        mTransaction.add(action);
        return this;
    }

    /**
     * Sets the device's state and sends {@link GBDevice#ACTION_DEVICE_CHANGED} intent
     */
    @NonNull
    public TransactionBuilder setUpdateState(@NonNull GBDevice device, GBDevice.State state, @NonNull Context context) {
        BtLEAction action = new SetDeviceStateAction(device, state, context);
        return add(action);
    }

    /// updates the progress bar
    /// @see SetProgressAction#SetProgressAction
    @NonNull
    public TransactionBuilder setProgress(@StringRes int textRes, boolean ongoing, int percentage, @NonNull Context context) {
        BtLEAction action = new SetProgressAction(textRes, ongoing, percentage, context);
        return add(action);
    }

    /**
     * Read the current transmitter PHY and receiver PHY of the connection.
     * @see ReadPhyAction
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    public TransactionBuilder readPhy() {
        BtLEAction action = new ReadPhyAction();
        return add(action);
    }

    /**
     * Set the preferred PHY of the connection.
     * @see SetPreferredPhyAction
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    public TransactionBuilder setPreferredPhy(int txPhy, int rxPhy, int phyOptions) {
        BtLEAction action = new SetPreferredPhyAction(txPhy, rxPhy, phyOptions);
        return add(action);
    }

    /// Set the device as busy or not ({@code taskName = 0}).
    /// @see SetDeviceBusyAction#SetDeviceBusyAction
    @NonNull
    public TransactionBuilder setBusyTask(@NonNull final GBDevice device, @StringRes final int taskName,
                                          @NonNull final Context context) {
        BtLEAction action = new SetDeviceBusyAction(device, taskName, context);
        return add(action);
    }

    /**
     * Sets a GattCallback instance that will be called when the transaction is executed,
     * resulting in GattCallback events.
     *
     * @param callback the callback to set, may be null
     */
    public void setCallback(@Nullable GattCallback callback) {
        mTransaction.setCallback(callback);
    }

    public
    @Nullable
    GattCallback getGattCallback() {
        return mTransaction.getGattCallback();
    }

    /**
     * To be used as the final step to execute the transaction by the given queue.
     *
     * @param queue
     */
    public void queue(BtLEQueue queue) {
        if (mQueued) {
            throw new IllegalStateException("This builder had already been queued. You must not reuse it.");
        }
        mQueued = true;
        queue.add(mTransaction);
    }

    public Transaction getTransaction() {
        return mTransaction;
    }

    public String getTaskName() {
        return mTransaction.getTaskName();
    }
}
