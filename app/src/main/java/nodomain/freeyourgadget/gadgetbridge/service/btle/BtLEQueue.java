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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;

/**
 * One queue/thread per connectable device.
 */
@SuppressLint("MissingPermission") // if we're using this, we have bluetooth permissions
public final class BtLEQueue {
    private static final Logger LOG = LoggerFactory.getLogger(BtLEQueue.class);
    private static final byte[] EMPTY = new byte[0];
    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0L);

    private final Object mGattMonitor;
    private final GBDevice mGbDevice;
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattServer mBluetoothGattServer;
    private final Set<? extends BluetoothGattService> mSupportedServerServices;

    private final BlockingQueue<AbstractTransaction> mTransactions;
    private volatile boolean mDisposed;
    private volatile boolean mCrashed;
    private volatile boolean mAbortTransaction;
    private volatile boolean mAbortServerTransaction;
    private volatile boolean mPauseTransaction;

    private final Context mContext;
    private CountDownLatch mWaitForActionResultLatch;
    private CountDownLatch mWaitForServerActionResultLatch;
    private CountDownLatch mConnectionLatch;
    private BluetoothGattCharacteristic mWaitCharacteristic;
    private final InternalGattCallback internalGattCallback;
    private final InternalGattServerCallback internalGattServerCallback;
    private final AbstractBTLEDeviceSupport mDeviceSupport;
    private final boolean mImplicitGattCallbackModify;
    private final boolean mSendWriteRequestResponse;

    private Thread dispatchThread = new Thread("BtLEQueue_" + THREAD_COUNTER.getAndIncrement()) {

        @Override
        public void run() {
            LOG.debug("Queue Dispatch Thread started.");

            while (!mDisposed && !mCrashed) {
                try {
                    AbstractTransaction qTransaction = mTransactions.take();

                    if (!isConnected()) {
                        LOG.debug("not connected, waiting for connection...");
                        // TODO: request connection and initialization from the outside and wait until finished
                        internalGattCallback.reset();

                        // wait until the connection succeeds before running the actions
                        // Note that no automatic connection is performed. This has to be triggered
                        // on the outside typically by the DeviceSupport. The reason is that
                        // devices have different kinds of initializations and this class has no
                        // idea about them.
                        mConnectionLatch = new CountDownLatch(1);
                        mConnectionLatch.await();
                        mConnectionLatch = null;
                    }

                    if(qTransaction instanceof ServerTransaction) {
                        ServerTransaction serverTransaction = (ServerTransaction)qTransaction;
                        internalGattServerCallback.setTransactionGattCallback(serverTransaction.getGattCallback());
                        mAbortServerTransaction = false;

                        for (BtLEServerAction action : serverTransaction.getActions()) {
                            if (mAbortServerTransaction) { // got disconnected
                                LOG.info("Aborting running server transaction");
                                break;
                            }
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("About to run server action: {}", action);
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

                    if(qTransaction instanceof Transaction) {
                        Transaction transaction = (Transaction)qTransaction;
                        LOG.trace("Changing gatt callback for {}? {}", transaction.getTaskName(), transaction.isModifyGattCallback());
                        if (mImplicitGattCallbackModify || transaction.isModifyGattCallback()) {
                            internalGattCallback.setTransactionGattCallback(transaction.getGattCallback());
                        }
                        mAbortTransaction = false;
                        // Run all actions of the transaction until one doesn't succeed
                        for (BtLEAction action : transaction.getActions()) {
                            if (mAbortTransaction) { // got disconnected
                                LOG.info("Aborting running transaction");
                                break;
                            }
                            while ((action instanceof WriteAction) && mPauseTransaction && !mAbortTransaction) {
                              LOG.info("Pausing WriteAction");
                              try {
                                  Thread.sleep(100);
                              } catch (Exception e) {
                                  LOG.info("Exception during pause", e);
                                  break;
                              }
                            }
                            mWaitCharacteristic = action.getCharacteristic();
                            mWaitForActionResultLatch = new CountDownLatch(1);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("About to run action: {}", action);
                            }
                            if (action instanceof GattListenerAction) {
                                // this special action overwrites the transaction gatt listener (if any), it must
                                // always be the last action in the transaction
                                internalGattCallback.setTransactionGattCallback(((GattListenerAction) action).getGattCallback());
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
                    LOG.debug("Thread interrupted");
                } catch (Throwable ex) {
                    LOG.error("Queue Dispatch Thread died", ex);
                    mCrashed = true;
                    mConnectionLatch = null;
                } finally {
                    mWaitForActionResultLatch = null;
                    mWaitCharacteristic = null;
                }
            }
            LOG.info("Queue Dispatch Thread terminated.");
        }
    };

    public BtLEQueue(GBDevice gbDevice, Set<? extends BluetoothGattService> supportedServerServices, AbstractBTLEDeviceSupport deviceSupport) {
        // 1) apply all settings
        mBluetoothAdapter = deviceSupport.getBluetoothAdapter();
        mContext = deviceSupport.getContext();
        mDeviceSupport = deviceSupport;
        mGbDevice = gbDevice;
        mImplicitGattCallbackModify = deviceSupport.getImplicitCallbackModify();
        mPauseTransaction = false;
        mSendWriteRequestResponse = deviceSupport.getSendWriteRequestResponse();
        mSupportedServerServices = supportedServerServices;

        // 2) create new objects
        mGattMonitor = new Object();
        mTransactions = new LinkedBlockingQueue<>();
        internalGattCallback = new InternalGattCallback(deviceSupport);
        internalGattServerCallback = new InternalGattServerCallback(deviceSupport);

        // 3) finally start the dispatch thread
        dispatchThread.start();
    }

    private boolean isConnected() {
        if (mGbDevice.isConnected()) {
            return true;
        }

        LOG.debug("isConnected(): current state = {}", mGbDevice.getState());
        return false;
    }

    /**
     * Connects to the given remote device. Note that this does not perform any device
     * specific initialization. This should be done in the specific {@link DeviceSupport}
     * class.
     *
     * @return <code>true</code> whether the connection attempt was successfully triggered and <code>false</code> if that failed or if there is already a connection
     */
    public boolean connect() {
        mPauseTransaction = false;
        if (isConnected()) {
            LOG.warn("Ignoring connect() because already connected.");
            return false;
        }
        synchronized (mGattMonitor) {
            if (mBluetoothGatt != null) {
                // Tribal knowledge says you're better off not reusing existing BluetoothGatt connections,
                // so create a new one.
                LOG.info("connect() requested -- disconnecting previous connection: {}", mGbDevice.getName());
                disconnect();
            }
        }
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

        synchronized (mGattMonitor) {
            // connectGatt with true doesn't really work ;( too often connection problems
            if (GBApplication.isRunningMarshmallowOrLater()) {
                mBluetoothGatt = remoteDevice.connectGatt(mContext, false, internalGattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mBluetoothGatt = remoteDevice.connectGatt(mContext, false, internalGattCallback);
            }
        }
        boolean result = mBluetoothGatt != null;
        if (result) {
            setDeviceConnectionState(State.CONNECTING);
        }
        return result;
    }

    private void setDeviceConnectionState(final State newState) {
        new Handler(Looper.getMainLooper()).post(() -> {
            LOG.debug("new device connection state: {}", newState);
            mGbDevice.setState(newState);
            mGbDevice.sendDeviceUpdateIntent(mContext, GBDevice.DeviceUpdateSubject.CONNECTION_STATE);
        });
    }

    public void disconnect() {
        synchronized (mGattMonitor) {
            LOG.debug("disconnect()");
            BluetoothGatt gatt = mBluetoothGatt;
            if (gatt != null) {
                mBluetoothGatt = null;
                LOG.info("Disconnecting BtLEQueue from GATT device");
                gatt.disconnect();
                gatt.close();
                setDeviceConnectionState(State.NOT_CONNECTED);
            }
            mPauseTransaction = false;
            BluetoothGattServer gattServer = mBluetoothGattServer;
            if (gattServer != null) {
                mBluetoothGattServer = null;
                gattServer.clearServices();
                gattServer.close();
            }
        }
    }

    private void handleDisconnected(int status) {
        LOG.debug("handleDisconnected: {}", BleNamesResolver.getStatusString(status));
        internalGattCallback.reset();
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
                        setDeviceConnectionState(State.WAITING_FOR_RECONNECT);
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

    public void dispose() {
        if (mDisposed) {
            return;
        }
        mDisposed = true;
//        try {
        disconnect();
        dispatchThread.interrupt();
        dispatchThread = null;
//            dispatchThread.join();
//        } catch (InterruptedException ex) {
//            LOG.error("Exception while disposing BtLEQueue", ex);
//        }
    }

    /**
     * Adds a transaction to the end of the queue.
     *
     * @param transaction
     */
    public void add(Transaction transaction) {
        LOG.debug("about to add: {}", transaction);
        if (!transaction.isEmpty()) {
            mTransactions.add(transaction);
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
     *
     * @param transaction
     */
    public void add(ServerTransaction transaction) {
        LOG.debug("about to add: {}", transaction);
        if(!transaction.isEmpty()) {
            mTransactions.add(transaction);
        }
    }

    /**
     * Adds a transaction to the beginning of the queue.
     * Note that actions of the *currently executing* transaction
     * will still be executed before the given transaction.
     */
    public void insert(Transaction transaction) {
        LOG.debug("about to insert: {}", transaction);
        if (!transaction.isEmpty()) {
            List<AbstractTransaction> tail = new ArrayList<>(mTransactions.size() + 2);
            //mTransactions.drainTo(tail);
            tail.addAll(mTransactions);
            mTransactions.clear();
            mTransactions.add(transaction);
            mTransactions.addAll(tail);
        }
    }

    public void clear() {
        mTransactions.clear();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            LOG.warn("BluetoothGatt is null => no services available.");
            return Collections.emptyList();
        }
        return mBluetoothGatt.getServices();
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

        public InternalGattCallback(GattCallback externalGattCallback) {
            mExternalGattCallback = externalGattCallback;
        }

        public void setTransactionGattCallback(@Nullable GattCallback callback) {
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
            LOG.debug("connection state change, newState: {} {} {}",
                    BleNamesResolver.getStateString(newState), BleNamesResolver.getStatusString(status),
                    BleNamesResolver.getBondStateString(bondState));

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
                callback.onConnectionStateChange(gatt, status, newState);
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
                    handleDisconnected(status);
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    LOG.info("Connecting to GATT server...");
                    setDeviceConnectionState(State.CONNECTING);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (!checkCorrectGattInstance(gatt, "services discovered: " + BleNamesResolver.getStatusString(status))) {
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                final GattCallback callback = getCallbackToUse();
                if (callback != null) {
                    // only propagate the successful event
                    callback.onServicesDiscovered(gatt);
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
            LOG.debug("characteristic write: {} {}", characteristic.getUuid(), BleNamesResolver.getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "characteristic write")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                callback.onCharacteristicWrite(gatt, characteristic, status);
            }
            checkWaitingCharacteristic(characteristic, status);
        }



        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            LOG.debug("mtu changed to {} {}", mtu, BleNamesResolver.getStatusString(status));

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                callback.onMtuChanged(gatt, mtu, status);
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
            LOG.debug(
                    "characteristic read: {} {} {}",
                    characteristic.getUuid(),
                    BleNamesResolver.getStatusString(status),
                    status == BluetoothGatt.GATT_SUCCESS ? ": " + Logging.formatBytes(value) : ""
            );
            if (!checkCorrectGattInstance(gatt, "characteristic read")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onCharacteristicRead(gatt, characteristic, value, status);
                } catch (Throwable ex) {
                    LOG.error("onCharacteristicRead: {}", ex.getMessage(), ex);
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
            LOG.debug("descriptor read: {} {}", descriptor.getUuid(), BleNamesResolver.getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "descriptor read")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onDescriptorRead(gatt, descriptor, status, value);
                } catch (Throwable ex) {
                    LOG.error("onDescriptorRead failed", ex);
                }
            }
            checkWaitingCharacteristic(descriptor.getCharacteristic(), status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            LOG.debug("descriptor write: {} {}", descriptor.getUuid(), BleNamesResolver.getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "descriptor write")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onDescriptorWrite(gatt, descriptor, status);
                } catch (Throwable ex) {
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
                String content = Logging.formatBytes(value);
                LOG.debug("characteristic changed: {} value: {}", characteristic.getUuid(), content);
            }
            if (!checkCorrectGattInstance(gatt, "characteristic changed")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onCharacteristicChanged(gatt, characteristic, value);
                } catch (Throwable ex) {
                    LOG.error("onCharacteristicChanged failed", ex);
                }
            } else {
                LOG.info("No gatt callback registered, ignoring characteristic change");
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            LOG.debug("remote rssi: {} {}", rssi, BleNamesResolver.getStatusString(status));
            if (!checkCorrectGattInstance(gatt, "remote rssi")) {
                return;
            }

            final GattCallback callback = getCallbackToUse();
            if (callback != null) {
                try {
                    callback.onReadRemoteRssi(gatt, rssi, status);
                } catch (Throwable ex) {
                    LOG.error("onReadRemoteRssi failed", ex);
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
            final BluetoothGattCharacteristic waitCharacteristic = BtLEQueue.this.mWaitCharacteristic;
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

        public void reset() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("internal gatt callback set to null");
            }
            mTransactionGattCallback = null;
        }

        /// helper to emulate Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU on older APIs
        private byte[] emulateMemorySafeValue(BluetoothGattCharacteristic characteristic,
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
        private byte[] emulateMemorySafeValue(BluetoothGattDescriptor descriptor,
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

        public InternalGattServerCallback(GattServerCallback externalGattServerCallback) {
            mExternalGattServerCallback = externalGattServerCallback;
        }

        public void setTransactionGattCallback(@Nullable GattServerCallback callback) {
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
                callback.onCharacteristicReadRequest(device, requestId, offset, characteristic);
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
                success = callback.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }
            if (responseNeeded && mSendWriteRequestResponse) {
                mBluetoothGattServer.sendResponse(device, requestId, success ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE, 0, new byte[0]);
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
                callback.onDescriptorReadRequest(device, requestId, offset, descriptor);
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
                success = callback.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }
            if (responseNeeded && mSendWriteRequestResponse) {
                mBluetoothGattServer.sendResponse(device, requestId, success ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE, 0, new byte[0]);
            }
        }
    }
}
