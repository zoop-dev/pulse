/*  Copyright (C) 2015-2025 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Cre3per, Daniel Dakhno, Daniele Gobbetti, Gordon Williams, José
    Rebelo, Sergey Trofimov, Taavi Eomäe, Uwe Hermann, Yoran Vulker, Thomas Kuehne

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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBExceptionHandler;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * One queue/thread per connectable device.
 */
@SuppressLint("MissingPermission") // if we're using this, we have bluetooth permissions
public final class BtLEQueue implements Thread.UncaughtExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BtLEQueue.class);
    private static final byte[] EMPTY = new byte[0];
    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0L);

    private final Object mGattMonitor;
    private final GBDevice mGbDevice;
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattServer mBluetoothGattServer;
    private final Set<? extends BluetoothGattService> mSupportedServerServices;

    private final BlockingDeque<AbstractTransaction> mTransactions;
    private final AtomicBoolean mDisposed;
    private volatile boolean mAbortTransaction;
    private volatile boolean mAbortServerTransaction;
    private volatile boolean mPauseTransaction;

    private final Context mContext;
    private CountDownLatch mWaitForActionResultLatch;
    private CountDownLatch mWaitForServerActionResultLatch;
    private CountDownLatch mConnectionLatch;
    private BluetoothGattCharacteristic mWaitCharacteristic;
    private final NoThrowBluetoothGattCallback<InternalGattCallback> internalGattCallback;
    private final InternalGattServerCallback internalGattServerCallback;
    private final AbstractBTLEDeviceSupport mDeviceSupport;
    private final boolean mImplicitGattCallbackModify;
    private final boolean mSendWriteRequestResponse;

    private final boolean connectionForceLegacyGatt;
    private final Thread mDispatchThread;
    private final HandlerThread mReceiverThread;
    private final Handler mReceiverHandler;

    private class DispatchRunnable implements Runnable {
        @Override
        public void run() {
            LOG.debug("started thread {}", Thread.currentThread().getName());
            boolean crashed = false;

            while (!mDisposed.get() && !crashed) {
                try {
                    AbstractTransaction qTransaction = mTransactions.takeFirst();

                    if (!isConnected()) {
                        LOG.debug("not connected, waiting for connection...");
                        // TODO: request connection and initialization from the outside and wait until finished
                        internalGattCallback.Delegate.reset();

                        // wait until the connection succeeds before running the actions
                        // Note that no automatic connection is performed. This has to be triggered
                        // on the outside typically by the DeviceSupport. The reason is that
                        // devices have different kinds of initializations and this class has no
                        // idea about them.
                        mConnectionLatch = new CountDownLatch(1);
                        mConnectionLatch.await();
                        mConnectionLatch = null;
                    }

                    if (qTransaction instanceof final ServerTransaction serverTransaction) {
                        internalGattServerCallback.setTransactionGattCallback(serverTransaction.getGattCallback());
                        mAbortServerTransaction = false;

                        for (final BtLEServerAction action : serverTransaction.getActions()) {
                            if (mAbortServerTransaction) { // got disconnected
                                LOG.info("Aborting running server transaction");
                                break;
                            }
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("execute server: {}", action);
                            }
                            if (action.run(mBluetoothGattServer)) {
                                // check again, maybe due to some condition, action did not need to write, so we can't wait
                                boolean waitForResult = action.expectsResult();
                                if (waitForResult) {
                                    mWaitForServerActionResultLatch.await();
                                    mWaitForServerActionResultLatch = null;
                                    if (mAbortServerTransaction) {
                                        break;
                                    }
                                }
                            } else {
                                LOG.error("Server action returned false: {}", action);
                                break; // abort the transaction
                            }
                        }
                    }

                    if (qTransaction instanceof final Transaction transaction) {
                        LOG.trace("Changing gatt callback for {}? {}", transaction.getTaskName(), transaction.isModifyGattCallback());
                        if (mImplicitGattCallbackModify || transaction.isModifyGattCallback()) {
                            internalGattCallback.Delegate.setTransactionGattCallback(transaction.getGattCallback());
                        }
                        mAbortTransaction = false;
                        // Run all actions of the transaction until one doesn't succeed
                        for (final BtLEAction action : transaction.getActions()) {
                            if (mAbortTransaction) { // got disconnected
                                LOG.info("Aborting running transaction");
                                break;
                            }
                            while ((action instanceof WriteAction) && mPauseTransaction && !mAbortTransaction) {
                              LOG.info("Pausing WriteAction");
                              try {
                                  Thread.sleep(100L);
                              } catch (Exception e) {
                                  LOG.info("Exception during pause", e);
                                  break;
                              }
                            }
                            mWaitCharacteristic = action.getCharacteristic();
                            mWaitForActionResultLatch = new CountDownLatch(1);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("execute: {}", action);
                            }
                            if (action instanceof final GattListenerAction listenerAction) {
                                // this special action overwrites the transaction gatt listener (if any), it must
                                // always be the last action in the transaction
                                internalGattCallback.Delegate.setTransactionGattCallback(listenerAction.getGattCallback());
                            }
                            if (action.run(mBluetoothGatt)) {
                                // check again, maybe due to some condition, action did not need to write, so we can't wait
                                boolean waitForResult = action.expectsResult();
                                if (waitForResult) {
                                    mWaitForActionResultLatch.await();
                                    mWaitForActionResultLatch = null;
                                    if (mAbortTransaction) {
                                        break;
                                    }
                                }
                            } else {
                                LOG.error("Action returned false: {}", action);
                                break; // abort the transaction
                            }
                        }
                    }
                } catch (InterruptedException ignored) {
                    mConnectionLatch = null;
                    LOG.debug("Queue Dispatch Thread interrupted");
                } catch (Throwable ex) {
                    LOG.error("Queue Dispatch Thread died", ex);
                    crashed = true;
                    mConnectionLatch = null;
                } finally {
                    mWaitForActionResultLatch = null;
                    mWaitCharacteristic = null;
                }
            }
            LOG.debug("finished thread {}", Thread.currentThread().getName());
        }
    };

    BtLEQueue(GBDevice gbDevice, Set<? extends BluetoothGattService> supportedServerServices, AbstractBTLEDeviceSupport deviceSupport) {
        // 1) apply all settings
        mBluetoothAdapter = deviceSupport.getBluetoothAdapter();
        mContext = deviceSupport.getContext();
        mDeviceSupport = deviceSupport;
        mGbDevice = gbDevice;
        mImplicitGattCallbackModify = deviceSupport.getImplicitCallbackModify();
        mPauseTransaction = false;
        mSendWriteRequestResponse = deviceSupport.getSendWriteRequestResponse();
        mSupportedServerServices = supportedServerServices;
        // #5414 - some older android versions misbehave with the new constructor
        connectionForceLegacyGatt = deviceSupport.getDevicePrefs().getConnectionForceLegacyGatt();

        long threadIdx = THREAD_COUNTER.getAndIncrement();

        // 2) create new objects
        mDisposed = new AtomicBoolean(false);
        mGattMonitor = new Object();
        mTransactions = new LinkedBlockingDeque<>();
        internalGattCallback = new NoThrowBluetoothGattCallback<>(new InternalGattCallback(deviceSupport));
        internalGattServerCallback = new InternalGattServerCallback(deviceSupport);
        mDispatchThread = new Thread(new DispatchRunnable(), "BtLEQueue_" + threadIdx + "_out");
        mDispatchThread.setUncaughtExceptionHandler(this);

        // 3) start the thread
        mDispatchThread.start();

        // 4) handler thread ensure serial processing and informative thread name in the log
        if(GBApplication.isRunningOreoOrLater() && !connectionForceLegacyGatt){
            mReceiverThread = new HandlerThread("BtLEQueue_" + threadIdx + "_in");
            mReceiverThread.setUncaughtExceptionHandler(this);
            mReceiverThread.start();
            mReceiverHandler = new Handler(mReceiverThread.getLooper());
            mReceiverHandler.post(() -> LOG.debug("started thread {}", Thread.currentThread().getName()));
        } else {
            mReceiverThread = null;
            mReceiverHandler = null;
        }
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        LOG.error("exception in {}", t.getName(), e);

        // TODO implement actual exception handling for mDispatchThread and mReceiverThread
        new GBExceptionHandler(null, true).uncaughtException(t, e);
    }

    boolean isConnected() {
        State state = mGbDevice.getState();
        boolean gatt = (mBluetoothGatt != null);
        boolean dispatch = mDispatchThread.isAlive();
        boolean receiver = (mReceiverThread == null || mReceiverThread.isAlive());
        if (state.equalsOrHigherThan(State.CONNECTED) && gatt && dispatch && receiver) {
            return true;
        }
        LOG.debug("not connected: state={} gatt={} dispatch={} receiver={}", state, gatt, dispatch, receiver);
        return false;
    }

    /**
     * Connects to the given remote device. Note that this does not perform any device
     * specific initialization. This should be done in the specific {@link DeviceSupport}
     * class.
     *
     * @return <code>true</code> whether the connection attempt was successfully triggered and <code>false</code> if that failed or if there is already a connection
     */
    boolean connect() {
        synchronized (mGattMonitor) {
            State state = mGbDevice.getState();
            if (state.equalsOrHigherThan(State.CONNECTING)) {
                LOG.warn("connect - ignored, state is {}", state);
                return false;
            } else if (mBluetoothGatt != null) {
                LOG.warn("connect - ignored, mBluetoothGatt isn't null");
                return false;
            } else if (mDisposed.get()) {
                LOG.error("connect - queue has already been disposed");
                String message = mContext.getString(R.string.error_queue_is_dead);
                throw new IllegalStateException(message);
            } else if (!mDispatchThread.isAlive()) {
                LOG.error("connect - mDispatchThread {} is dead", mDispatchThread.getName());
                String message = mContext.getString(R.string.error_sender_is_dead);
                throw new IllegalStateException(message);
            } else if (mReceiverThread != null && !mReceiverThread.isAlive()) {
                LOG.error("connect - mReceiverThread {} is dead", mReceiverThread.getName());
                String message = mContext.getString(R.string.error_receiver_is_dead);
                throw new IllegalStateException(message);
            }

            if (connectImp()) {
                setDeviceConnectionState(State.CONNECTING);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean connectImp() {
        mPauseTransaction = false;

        LOG.info("Attempting to connect to {}", mGbDevice.getName());
        mBluetoothAdapter.cancelDiscovery();
        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mGbDevice.getAddress());
        if(!mSupportedServerServices.isEmpty()) {
            BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                LOG.error("Error getting bluetoothManager");
                return false;
            }
            mBluetoothGattServer = bluetoothManager.openGattServer(mContext, internalGattServerCallback);
            if (mBluetoothGattServer == null) {
                LOG.error("Error opening Gatt Server");
                return false;
            }
            for(BluetoothGattService service : mSupportedServerServices) {
                mBluetoothGattServer.addService(service);
            }
        }


        // connectGatt with true doesn't really work ;( too often connection problems
        if (GBApplication.isRunningOreoOrLater() && !connectionForceLegacyGatt) {
            mBluetoothGatt = remoteDevice.connectGatt(mContext, false,
                    internalGattCallback, BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_CODED_MASK, mReceiverHandler);
        } else {
            mBluetoothGatt = remoteDevice.connectGatt(mContext, false,
                    internalGattCallback, BluetoothDevice.TRANSPORT_LE);
        }

        return mBluetoothGatt != null;
    }

    private void setDeviceConnectionState(final State newState) {
        LOG.debug("new device connection state: {}", newState);
        mGbDevice.setState(newState);
        mGbDevice.sendDeviceUpdateIntent(mContext, GBDevice.DeviceUpdateSubject.CONNECTION_STATE);
    }

    void disconnect() {
        synchronized (mGattMonitor) {
            BluetoothGatt gatt = mBluetoothGatt;
            if (gatt != null) {
                mBluetoothGatt = null;
                LOG.info("disconnecting BluetoothGatt");
                gatt.disconnect();
                gatt.close();
            }
            mPauseTransaction = false;
            BluetoothGattServer gattServer = mBluetoothGattServer;
            if (gattServer != null) {
                mBluetoothGattServer = null;
                LOG.info("disconnecting BluetoothGattServer");
                gattServer.clearServices();
                gattServer.close();
            }

            if (mGbDevice.getState() != State.NOT_CONNECTED) {
                setDeviceConnectionState(State.NOT_CONNECTED);
            }
        }
    }

    private void handleDisconnected(int status) {
        LOG.debug("handleDisconnected: {}", BleNamesResolver.getStatusString(status));
        internalGattCallback.Delegate.reset();
        mTransactions.clear();
        mPauseTransaction = false;
        mAbortTransaction = true;
        mAbortServerTransaction = true;
        final CountDownLatch clientLatch = mWaitForActionResultLatch;
        if (clientLatch != null) {
            clientLatch.countDown();
        }
        final CountDownLatch serverLatch = mWaitForServerActionResultLatch;
        if (serverLatch != null) {
            serverLatch.countDown();
        }

        boolean forceDisconnect;
        switch(status){
            case 0x81: // 0x81 129 GATT_INTERNAL_ERROR
            case 0x85: // 0x85 133 GATT_ERROR
                // Bluetooth stack has a fundamental problem:
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION:
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                // a Bluetooth bonding / pairing issue
                // some devices report AUTHORIZATION instead of TIMEOUT during connection setup
            case BluetoothGatt.GATT_CONNECTION_TIMEOUT:
                forceDisconnect = true;
                break;
            default:
                forceDisconnect = false;
        }

        if (forceDisconnect) {
            LOG.warn("unhealthy disconnect {} {}", mBluetoothGatt.getDevice().getAddress(),
                    BleNamesResolver.getStatusString(status));
        } else if (mBluetoothGatt != null) {
            // try to reconnect immediately
            if (mDeviceSupport.getAutoReconnect()) {
                if (mDeviceSupport.getScanReconnect()) {
                    // connect() would first disconnect() anyway
                    forceDisconnect = true;
                } else {
                    LOG.info("enabling automatic immediate BLE reconnection");
                    mPauseTransaction = false;
                    if (mBluetoothGatt.connect()) {
                        setDeviceConnectionState(State.CONNECTING);
                    } else {
                        forceDisconnect = true;
                    }
                }
            } else {
                forceDisconnect = true;
            }
        }

        if (forceDisconnect) {
            disconnect();
        }

        if (mBluetoothGatt == null) {
            if (mDeviceSupport.getAutoReconnect()) {
                // don't reconnect immediately to give the Bluetooth stack some time to settle down
                // use BluetoothConnectReceiver or AutoConnectIntervalReceiver instead
                if (mDeviceSupport.getScanReconnect()) {
                    LOG.info("waiting for BLE scan before attempting reconnection");
                    setDeviceConnectionState(State.WAITING_FOR_SCAN);
                } else {
                    LOG.info("enabling automatic delayed BLE reconnection");
                    setDeviceConnectionState(State.WAITING_FOR_RECONNECT);
                }
            } else if (!forceDisconnect) {
                setDeviceConnectionState(State.NOT_CONNECTED);
            }
        }
    }

    public void setPaused(boolean paused) {
      mPauseTransaction = paused;
    }

    void dispose() {
        if (mDisposed.getAndSet(true)) {
            LOG.warn("dispose() was called repeatedly");
            return;
        }

        disconnect();

        if (mReceiverThread != null && mReceiverThread.isAlive()) {
            mReceiverHandler.post(() -> {
                LOG.debug("finish thread {}", Thread.currentThread().getName());
                mReceiverThread.quitSafely();
            });
        }

        if (mDispatchThread != null) {
            mDispatchThread.interrupt();
        }
    }

    /**
     * Adds a transaction to the end of the queue.
     *
     * @param transaction
     */
    void add(Transaction transaction) {
        LOG.debug("add: {}", transaction);
        if (!transaction.isEmpty()) {
            mTransactions.addLast(transaction);
        }
    }

    /**
     * Aborts the currently running transaction
     */
    public void abortCurrentTransaction() {
        mAbortTransaction = true;
        final CountDownLatch latch = mWaitForActionResultLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    /**
     * Adds a serverTransaction to the end of the queue
     */
    void add(ServerTransaction transaction) {
        LOG.debug("add server: {}", transaction);
        if(!transaction.isEmpty()) {
            mTransactions.addLast(transaction);
        }
    }

    /**
     * Adds a transaction to the beginning of the queue.
     * Note that actions of the *currently executing* transaction
     * will still be executed before the given transaction.
     */
    void insert(Transaction transaction) {
        LOG.debug("about to insert: {}", transaction);
        if (!transaction.isEmpty()) {
            mTransactions.addFirst(transaction);
        }
    }

    public void clear() {
        mTransactions.clear();
    }

    /** @noinspection BooleanMethodIsAlwaysInverted*/
    private boolean checkCorrectGattInstance(BluetoothGatt gatt, String where) {
        if (gatt != mBluetoothGatt && mBluetoothGatt != null) {
            LOG.warn("Ignoring event from wrong BluetoothGatt instance: {}; {}", where, gatt);
            return false;
        }
        return true;
    }

    /** @noinspection BooleanMethodIsAlwaysInverted*/
    private boolean checkCorrectBluetoothDevice(BluetoothDevice device) {
        //BluetoothDevice clientDevice = mBluetoothAdapter.getRemoteDevice(mGbDevice.getAddress());

        if(!device.getAddress().equals(mGbDevice.getAddress())) { // != clientDevice && clientDevice != null) {
            LOG.warn("Ignoring request from wrong Bluetooth device: {}", device.getAddress());
            return false;
        }
        return true;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final class InternalGattCallback extends BluetoothGattCallback {
        private
        @Nullable
        GattCallback mTransactionGattCallback;
        private final GattCallback mExternalGattCallback;

        InternalGattCallback(GattCallback externalGattCallback) {
            mExternalGattCallback = externalGattCallback;
        }

        void setTransactionGattCallback(@Nullable GattCallback callback) {
            mTransactionGattCallback = callback;
        }

        private GattCallback getCallbackToUse() {
            final GattCallback callback = mTransactionGattCallback;
            if (callback != null) {
                return callback;
            }
            return mExternalGattCallback;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            final int bondState = gatt.getDevice().getBondState();
            LOG.debug("connection state changed: {} {} {}",
                    BleNamesResolver.getStateString(newState), BleNamesResolver.getStatusString(status),
                    BleNamesResolver.getBondStateString(bondState));

            if (!checkCorrectGattInstance(gatt, "onConnectionStateChange")) {
                return;
            }

            synchronized (mGattMonitor) {
                if (mBluetoothGatt == null) {
                    mBluetoothGatt = gatt;
                }
            }

            if (!checkCorrectGattInstance(gatt, "connection state event")) {
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                LOG.warn("connection state event with error status {}", BleNamesResolver.getStatusString(status));
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onConnectionStateChange(gatt, status, newState);
                } catch (Exception ex) {
                    LOG.error("onConnectionStateChange failed", ex);
                }
            }

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    LOG.info("Connected to GATT server.");
                    setDeviceConnectionState(State.CONNECTED);

                    // discover services in the main thread (appears to fix Samsung connection problems)
                    final long delayMillis = mDeviceSupport.getServiceDiscoveryDelay(bondState != BluetoothDevice.BOND_NONE);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothGatt bluetoothGatt = mBluetoothGatt;
                            if (bluetoothGatt == null) {
                                return;
                            }
                            List<BluetoothGattService> services = bluetoothGatt.getServices();
                            if (services != null && !services.isEmpty()) {
                                LOG.info("Using cached services, skipping discovery");
                                onServicesDiscovered(bluetoothGatt, BluetoothGatt.GATT_SUCCESS);
                            } else {
                                LOG.debug("discoverServices");
                                bluetoothGatt.discoverServices();
                            }
                        }
                    }, delayMillis);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    LOG.info("Disconnected from GATT server.");
                    synchronized (mGattMonitor) {
                        handleDisconnected(status);
                    }
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    LOG.info("Connecting to GATT server...");
                    setDeviceConnectionState(State.CONNECTING);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LOG.debug("services discovered: {}", BleNamesResolver.getStatusString(status));

            if (!checkCorrectGattInstance(gatt, "onServicesDiscovered")) {
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                final GattCallback callback = getCallbackToUse();
                if (callback != null) {
                    // only propagate the successful event
                    try {
                        callback.onServicesDiscovered(gatt);
                    } catch (Exception ex) {
                        LOG.error("onServicesDiscovered failed", ex);
                    }
                }
                final CountDownLatch latch = mConnectionLatch;
                if (latch != null) {
                    latch.countDown();
                }
            } else {
                LOG.warn("onServicesDiscovered received: {}", BleNamesResolver.getStatusString(status));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LOG.debug("characteristic written: {} {}", characteristic.getUuid(), BleNamesResolver.getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "characteristic write")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onCharacteristicWrite(gatt, characteristic, status);
                } catch (Exception ex) {
                    LOG.error("onCharacteristicWrite failed", ex);
                }
            }
            checkWaitingCharacteristic(characteristic, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            LOG.debug("mtu changed to {} {}", mtu, BleNamesResolver.getStatusString(status));

            if (!checkCorrectGattInstance(gatt, "onMtuChanged")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onMtuChanged(gatt, mtu, status);
                } catch (Exception ex) {
                    LOG.error("onMtuChanged failed", ex);
                }
            }

            final CountDownLatch latch = mWaitForActionResultLatch;
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            byte[] value = emulateMemorySafeValue(characteristic, status);
            onCharacteristicRead(gatt, characteristic, value, status);
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         @NonNull byte[] value, int status) {
            if (LOG.isDebugEnabled()) {
                String content = GB.hexdump(value);
                LOG.debug(
                        "characteristic read: {} {} - {}", characteristic.getUuid(),
                        BleNamesResolver.getStatusString(status), content
                );
            }

            if (!checkCorrectGattInstance(gatt, "onCharacteristicRead")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onCharacteristicRead(gatt, characteristic, value, status);
                } catch (Exception ex) {
                    LOG.error("onCharacteristicRead failed", ex);
                }
            }
            checkWaitingCharacteristic(characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            byte[] value = emulateMemorySafeValue(descriptor, status);
            onDescriptorRead(gatt, descriptor, status, value);
        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            if (LOG.isDebugEnabled()) {
                String content = GB.hexdump(value);
                LOG.debug("descriptor read: {} {} - {}", descriptor.getUuid(),
                        BleNamesResolver.getStatusString(status), content);
            }

            if (!checkCorrectGattInstance(gatt, "onDescriptorRead")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onDescriptorRead(gatt, descriptor, status, value);
                } catch (Exception ex) {
                    LOG.error("onDescriptorRead failed", ex);
                }
            }
            checkWaitingCharacteristic(descriptor.getCharacteristic(), status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            LOG.debug("descriptor written: {} {}", descriptor.getUuid(), BleNamesResolver.getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "descriptor write")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onDescriptorWrite(gatt, descriptor, status);
                } catch (Exception ex) {
                    LOG.error("onDescriptorWrite failed", ex);
                }
            }
            checkWaitingCharacteristic(descriptor.getCharacteristic(), status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] value = emulateMemorySafeValue(characteristic, BluetoothGatt.GATT_SUCCESS);
            onCharacteristicChanged(gatt, characteristic, value);
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
                                            @NonNull BluetoothGattCharacteristic characteristic,
                                            @NonNull byte[] value) {
            if (LOG.isDebugEnabled()) {
                String content = GB.hexdump(value);
                LOG.debug("characteristic changed: {} - {}", characteristic.getUuid(), content);
            }
            if (!checkCorrectGattInstance(gatt, "characteristic changed")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onCharacteristicChanged(gatt, characteristic, value);
                } catch (Exception ex) {
                    LOG.error("onCharacteristicChanged failed", ex);
                }
            } else {
                LOG.info("No gatt callback registered, ignoring characteristic change");
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            LOG.debug("read remote rssi: {} {}", rssi, BleNamesResolver.getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "remote rssi")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onReadRemoteRssi(gatt, rssi, status);
                } catch (Exception ex) {
                    LOG.error("onReadRemoteRssi failed", ex);
                }
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            LOG.debug("reliable write completed: {}", BleNamesResolver.getStatusString(status));

            if (!checkCorrectGattInstance(gatt, "onReliableWriteCompleted")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onReliableWriteCompleted(gatt, status);
                } catch (Exception ex) {
                    LOG.error("onReliableWriteCompleted failed", ex);
                }
            }

            final CountDownLatch latch = mWaitForActionResultLatch;
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        @RequiresApi(Build.VERSION_CODES.O)
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            LOG.debug("phy read: tx={} rx={} {}", txPhy, rxPhy,
                    BleNamesResolver.getStatusString(status));

            if (!checkCorrectGattInstance(gatt, "onPhyRead")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onPhyRead(gatt, txPhy, rxPhy, status);
                } catch (Exception ex) {
                    LOG.error("onPhyRead failed", ex);
                }
            }

            final CountDownLatch latch = mWaitForActionResultLatch;
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        @RequiresApi(Build.VERSION_CODES.O)
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            LOG.debug("phy updated: tx={} rx={} {}", txPhy, rxPhy,
                    BleNamesResolver.getStatusString(status));

            if (!checkCorrectGattInstance(gatt, "onPhyUpdate")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onPhyUpdate(gatt, txPhy, rxPhy, status);
                } catch (Exception ex) {
                    LOG.error("onPhyUpdate failed", ex);
                }
            }

            // not all updates are triggered by GB:
            // can't use mWaitForActionResultLatch here
        }

        @Override
        @RequiresApi(Build.VERSION_CODES.S)
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            LOG.debug("service changed");

            if (!checkCorrectGattInstance(gatt, "onServiceChanged")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onServiceChanged(gatt);
                } catch (Exception ex) {
                    LOG.error("onServiceChanged failed", ex);
                }
            }
        }

        private void checkWaitingCharacteristic(BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (characteristic != null) {
                    LOG.debug("failed btle action, aborting transaction: {} {}", characteristic.getUuid(), BleNamesResolver.getStatusString(status));
                }
                mAbortTransaction = true;
            }
            final BluetoothGattCharacteristic waitCharacteristic = mWaitCharacteristic;
            if (characteristic != null && waitCharacteristic != null && characteristic.getUuid().equals(waitCharacteristic.getUuid())) {
                final CountDownLatch resultLatch = mWaitForActionResultLatch;
                if (resultLatch != null) {
                    resultLatch.countDown();
                }
            } else {
                if (waitCharacteristic != null) {
                    LOG.error(
                            "checkWaitingCharacteristic: mismatched characteristic received: {}",
                            (characteristic != null && characteristic.getUuid() != null) ? characteristic.getUuid().toString() : "(null)"
                    );
                }
            }
        }

        void reset() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("internal gatt callback set to null");
            }
            mTransactionGattCallback = null;
        }

        /// helper to emulate Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU on older APIs
        private static byte[] emulateMemorySafeValue(BluetoothGattCharacteristic characteristic,
                                              int status){
            if(status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                if (value != null) {
                    return value.clone();
                }
            }
            return EMPTY;
        }

        /// helper to emulate Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU on older APIs
        private static byte[] emulateMemorySafeValue(BluetoothGattDescriptor descriptor,
                                              int status){
            if(status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = descriptor.getValue();
                if (value != null) {
                    return value.clone();
                }
            }
            return EMPTY;
        }
    }

    // Implements callback methods for GATT server events that the app cares about.  For example,
    // connection change and read/write requests.
    private final class InternalGattServerCallback extends BluetoothGattServerCallback {
        private
        @Nullable
        GattServerCallback mTransactionGattCallback;
        private final GattServerCallback mExternalGattServerCallback;

        InternalGattServerCallback(GattServerCallback externalGattServerCallback) {
            mExternalGattServerCallback = externalGattServerCallback;
        }

        void setTransactionGattCallback(@Nullable GattServerCallback callback) {
            mTransactionGattCallback = callback;
        }

        private GattServerCallback getCallbackToUse() {
            final GattServerCallback callback = mTransactionGattCallback;
            if (callback != null) {
                return callback;
            }
            return mExternalGattServerCallback;
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            LOG.debug("gatt server connection state change, newState: {} {}", newState, BleNamesResolver.getStatusString(status));

            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                LOG.warn("gatt server connection state event with error status {}", BleNamesResolver.getStatusString(status));
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }
            LOG.debug("characteristic read request: {} characteristic: {}", device.getAddress(), characteristic.getUuid());
            final GattServerCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                } catch (Exception ex) {
                    LOG.error("onCharacteristicReadRequest failed", ex);
                }
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }
            LOG.debug("characteristic write request: {} characteristic: {}", device.getAddress(), characteristic.getUuid());
            boolean success = false;
            final GattServerCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    success = callback.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                } catch (Exception ex) {
                    LOG.error("onCharacteristicWriteRequest failed", ex);
                }
            }
            if (responseNeeded && mSendWriteRequestResponse) {
                mBluetoothGattServer.sendResponse(device, requestId, success ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE, 0, EMPTY);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }
            LOG.debug("onDescriptorReadRequest: {}", device.getAddress());
            final GattServerCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onDescriptorReadRequest(device, requestId, offset, descriptor);
                } catch (Exception ex) {
                    LOG.error("onDescriptorReadRequest failed", ex);
                }
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if(!checkCorrectBluetoothDevice(device)) {
                return;
            }
            LOG.debug("onDescriptorWriteRequest: {}", device.getAddress());
            boolean success = false;
            final GattServerCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    success = callback.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                } catch (Exception ex) {
                    LOG.error("onDescriptorWriteRequest failed", ex);
                }
            }
            if (responseNeeded && mSendWriteRequestResponse) {
                mBluetoothGattServer.sendResponse(device, requestId, success ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE, 0, EMPTY);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            LOG.debug("server.onServiceAdded {} {}", service.getUuid(), service.getInstanceId());
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            LOG.debug("server.onExecuteWrite {} {}", requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            LOG.debug("server.onNotificationSent {}",
                    BleNamesResolver.getStatusString(status));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            LOG.debug("server.onMtuChanged mtu={}", mtu);
        }

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            LOG.debug("server.onPhyUpdate tx={} rx={} {}", txPhy, rxPhy,
                    BleNamesResolver.getStatusString(status));
        }

        @Override
        public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            LOG.debug("server.onPhyRead tx={} rx={} {}", txPhy, rxPhy,
                    BleNamesResolver.getStatusString(status));
        }
    }
}
