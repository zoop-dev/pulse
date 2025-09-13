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
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
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

    private final AbstractBTLEDeviceSupport mDeviceSupport;
    private final int mDeviceIdx;
    private final Transaction mTransaction;
    private boolean mQueued;

    TransactionBuilder(String taskName, @NonNull AbstractBTLEDeviceSupport deviceSupport,
                       @IntRange(from = 0L) int deviceIdx) {
        mTransaction = new Transaction(taskName);
        mDeviceSupport = deviceSupport;
        mDeviceIdx = deviceIdx;
    }

    /// Invokes a read operation on a given characteristic. The result will be made
    /// available asynchronously through
    /// {@link GattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, byte[], int)}
    /// @see #read(UUID)
    @NonNull
    public TransactionBuilder read(@Nullable BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            LOG.warn("Unable to read characteristic: null");
            return this;
        }
        ReadAction action = new ReadAction(characteristic);
        return add(action);
    }

    /// Invokes a read operation on a given characteristic. The result will be made
    /// available asynchronously through
    /// {@link GattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, byte[], int)}
    /// @see #read(BluetoothGattCharacteristic)
    public TransactionBuilder read(UUID characteristic) {
        BluetoothGattCharacteristic bgc = mDeviceSupport.getCharacteristic(characteristic, mDeviceIdx);
        if (bgc == null) {
            LOG.warn("Unable to read non-existing characteristic: {}", characteristic);
            return this;
        }
        return read(bgc);
    }

    /// Use this only if <strong>ALL</strong> conditions are true:
    /// <ol>
    /// <li>characteristic has write type {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE},</li>
    /// <li>custom {@link GattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)}
    /// uses {@link BluetoothGattCharacteristic#getValue()} and</li>
    /// <li>no {@link BluetoothGatt#beginReliableWrite()} was used.</li>
    /// </ol>
    /// @see #write(UUID, byte...)
    /// @see #write(BluetoothGattCharacteristic, byte...)
    /// @see #writeChunkedData(BluetoothGattCharacteristic, byte[], int)
    @NonNull
    public TransactionBuilder writeLegacy(@Nullable BluetoothGattCharacteristic characteristic, byte... data) {
        if (characteristic == null) {
            LOG.warn("Unable to write characteristic: null");
            return this;
        }

        int maxChunk = getMaxWriteChunk();
        if (data.length > maxChunk) {
            LOG.error("writeLegacy - payload for {} is too long: {} > {}",
                    characteristic.getUuid(), data.length, maxChunk);
            // TODO throw exception after reviewing device specific code (performConnected...)
        }

        WriteAction action = new WriteAction(characteristic, data, true);
        return add(action);
    }

    /// Invokes a write operation on a given characteristic
    /// The result status will be made available asynchronously through
    /// {@link GattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)}
    /// @see #write(UUID, byte...)
    /// @see #writeChunkedData(BluetoothGattCharacteristic, byte[], int)
    /// @see #writeLegacy(BluetoothGattCharacteristic, byte...)
    @NonNull
    public TransactionBuilder write(@Nullable BluetoothGattCharacteristic characteristic, byte... data) {
        if (characteristic == null) {
            LOG.warn("Unable to write characteristic: null");
            return this;
        }

        int maxChunk = getMaxWriteChunk();
        if (data.length > maxChunk) {
            LOG.error("write - payload for {} is too long: {} > {}",
                    characteristic.getUuid(), data.length, maxChunk);
            // TODO throw exception after reviewing device specific code (performConnected...)
        }

        WriteAction action = new WriteAction(characteristic, data);
        return add(action);
    }

    /// Invokes a write operation on a given characteristic
    /// The result status will be made available asynchronously through
    /// {@link GattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)}
    /// @see #write(BluetoothGattCharacteristic, byte...)
    /// @see #writeChunkedData(BluetoothGattCharacteristic, byte[], int)
    /// @see #writeLegacy(BluetoothGattCharacteristic, byte...)
    @NonNull
    public TransactionBuilder write(UUID characteristic, byte... data) {
        BluetoothGattCharacteristic bgc = mDeviceSupport.getCharacteristic(characteristic, mDeviceIdx);
        if (bgc == null) {
            LOG.warn("unable to write to non-existing characteristic: {}", characteristic);
            return this;
        }
        return write(bgc, data);
    }

    /// Invokes one or more write operations on a given characteristic
    /// The result status will be made available asynchronously through
    /// {@link GattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)}
    /// @param requestedChunkLength will be automatically reduced if required for this connection
    /// @see #write(BluetoothGattCharacteristic, byte...)
    /// @see #write(UUID, byte...)
    /// @see #writeLegacy(BluetoothGattCharacteristic, byte...)
    @NonNull
    public TransactionBuilder writeChunkedData(@Nullable BluetoothGattCharacteristic characteristic,
                                               @NonNull byte[] data,
                                               @IntRange(from = 1L) int requestedChunkLength) {
        if (characteristic == null) {
            LOG.warn("Unable to write characteristic: null");
            return this;
        }

        // no larger than requested
        int chunkSize = Math.min(requestedChunkLength, getMaxWriteChunk());

        for (int start = 0; start < data.length; start += chunkSize) {
            int end = start + chunkSize;
            if (end > data.length) end = data.length;
            WriteAction action = new WriteAction(characteristic, Arrays.copyOfRange(data, start, end));
            add(action);
        }

        return this;
    }

    /// the maximum payload length supported for one write action
    @IntRange(from = 20L, to = 512L)
    public int getMaxWriteChunk() {
        int mtu = mDeviceSupport.getMTU(mDeviceIdx);
        return AbstractBTLEDeviceSupport.calcMaxWriteChunk(mtu);
    }

    /// Calls {@link BluetoothGatt#requestMtu(int)}. Results are returned asynchronously through
    /// {@link GattCallback#onMtuChanged(BluetoothGatt, int, int)}
    @NonNull
    public TransactionBuilder requestMtu(@IntRange(from = 23L, to = 517L) int mtu){
        return add(
                new RequestMtuAction(mtu)
        );
    }

    /// Calls {@link BluetoothGatt#requestConnectionPriority(int)}.
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

    /// Enables or disables notifications for a given {@link BluetoothGattCharacteristic}.
    /// The result will be made available asynchronously through
    /// {@link GattCallback#onDescriptorWrite(BluetoothGatt, BluetoothGattDescriptor, int)}.
    @NonNull
    public TransactionBuilder notify(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (characteristic == null) {
            LOG.warn("Unable to notify characteristic: null");
            return this;
        }
        NotifyAction action = new NotifyAction(characteristic, enable);
        return add(action);
    }

    /// Enables or disables notifications for a given {@link BluetoothGattCharacteristic}.
    /// The result will be made available asynchronously through the
    /// {@link GattCallback#onDescriptorWrite(BluetoothGatt, BluetoothGattDescriptor, int)}.
    @NonNull
    public TransactionBuilder notify(UUID characteristic, boolean enable) {
        BluetoothGattCharacteristic chara = mDeviceSupport.getCharacteristic(characteristic, mDeviceIdx);
        if (chara == null) {
            LOG.warn("unable to enable/disable notifications for non-existing characteristic: {}", characteristic);
            return this;
        }
        return notify(chara, enable);
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

    /// Sets the device's state and sends {@link GBDevice#ACTION_DEVICE_CHANGED} intent
    @NonNull
    public TransactionBuilder setDeviceState(GBDevice.State state) {
        BtLEAction action = new SetDeviceStateAction(mDeviceSupport.getDevice(), state, mDeviceSupport.getContext());
        return add(action);
    }

    /// updates the progress bar
    /// @see SetProgressAction#SetProgressAction
    @NonNull
    public TransactionBuilder setProgress(@StringRes int textRes, boolean ongoing, int percentage) {
        BtLEAction action = new SetProgressAction(textRes, ongoing, percentage, mDeviceSupport.getContext());
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
    public TransactionBuilder setBusyTask(@StringRes final int taskName) {
        BtLEAction action = new SetDeviceBusyAction(mDeviceSupport.getDevice(), taskName, mDeviceSupport.getContext());
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

    /// To be used as the final step to execute the transaction by the queue.
    /// @see #queueConnected()
    /// @see #queueImmediately()
    public void queue() {
        if (mQueued) {
            throw new IllegalStateException("This builder had already been queued. You must not reuse it.");
        }
        mQueued = true;
        BtLEQueue queue = mDeviceSupport.getQueue(mDeviceIdx);
        queue.add(mTransaction);
    }

    @VisibleForTesting()
    @NonNull
    public Transaction getTransaction() {
        return mTransaction;
    }

    public String getTaskName() {
        return mTransaction.getTaskName();
    }

    /// Ensures that the device is connected and (only then) performs the actions of the given
    /// transaction builder.
    /// <p>
    /// In contrast to {@code performInitialized(...)}, no initialization sequence is performed
    /// with the device, only the actions of the given builder are executed.
    ///
    /// @throws IOException if unable to connect to the device
    /// @throws IllegalStateException if this builder has already been queued
    /// @see AbstractBTLESingleDeviceSupport#performInitialized(String)
    /// @see AbstractBTLEMultiDeviceSupport#performInitialized(String, int)
    /// @see #queue()
    /// @see #queueImmediately()
    public void queueConnected() throws IOException {
        if (!mDeviceSupport.isConnected()) {
            if (!mDeviceSupport.connect()) {
                throw new IOException("Unable to connect to device: " + mDeviceSupport.getDevice());
            }
        }
        queue();
    }

    /// Performs the actions as soon as possible,
    /// that is, before any other queued transactions, but after the actions
    /// of the currently executing transaction.
    /// @throws IOException if the device isn't connected
    /// @throws IllegalStateException if this builder has already been queued
    /// @see #queue()
    /// @see #queueConnected()
    public void queueImmediately() throws IOException {
        if (!mDeviceSupport.isConnected()) {
            throw new IOException("Not connected to device: " + mDeviceSupport.getDevice());
        }
        if (mQueued) {
            throw new IllegalStateException("This builder had already been queued. You must not reuse it.");
        }
        mQueued = true;
        BtLEQueue queue = mDeviceSupport.getQueue(mDeviceIdx);
        queue.insert(mTransaction);
    }
}
