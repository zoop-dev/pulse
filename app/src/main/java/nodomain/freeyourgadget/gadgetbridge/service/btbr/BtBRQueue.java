/*  Copyright (C) 2022-2024 Damien Gaignon, José Rebelo

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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public final class BtBRQueue {
    private static final Logger LOG = LoggerFactory.getLogger(BtBRQueue.class);
    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0L);
    public static final int HANDLER_SUBJECT_CONNECT = 0;
    public static final int HANDLER_SUBJECT_PERFORM_TRANSACTION = 1;

    private final BluetoothAdapter mBtAdapter;
    private BluetoothSocket mBtSocket = null;
    private final GBDevice mGbDevice;
    private final SocketCallback mCallback;
    private final UUID mService;

    private final AtomicBoolean mDisposed;

    private final Context mContext;
    private final int mBufferSize;

    private final Handler mWriteHandler;
    private final HandlerThread mWriteHandlerThread = new HandlerThread("BtBRQueue_write_" + THREAD_COUNTER.getAndIncrement(), Process.THREAD_PRIORITY_BACKGROUND);

    private Thread readThread;

    private Thread createReadThread() {
        return new Thread("BtBRQueue_read_" + THREAD_COUNTER.getAndIncrement()) {
            @Override
            public void run() {
                LOG.debug("started thread {}", getName());
                final byte[] buffer = new byte[mBufferSize];
                int nRead;

                LOG.debug("Read thread started, entering loop");

                while (!mDisposed.get()) {
                    try {
                        if (mBtSocket == null)
                            throw new IOException("mBtSocket was null");

                        nRead = mBtSocket.getInputStream().read(buffer);

                        // safety measure
                        if (nRead == -1) {
                            throw new IOException("End of stream");
                        }
                    } catch (IOException ex) {
                        LOG.error("IO exception while reading message from socket, breaking out of read thread", ex);
                        break;
                    }

                    LOG.debug("Received {} bytes: {}", nRead, GB.hexdump(buffer, 0, nRead));

                    try {
                        mCallback.onSocketRead(Arrays.copyOf(buffer, nRead));
                    } catch (Throwable ex) {
                        LOG.error("Failed to process received bytes in onSocketRead callback: ", ex);
                    }
                }

                cleanup();

                if (mDisposed.get() || !GBApplication.getPrefs().getAutoReconnect(mGbDevice)) {
                    LOG.debug("Exited read thread loop, disconnecting");
                    mGbDevice.setUpdateState(GBDevice.State.NOT_CONNECTED, mContext);
                } else {
                    LOG.debug("Exited read thread loop, will wait for reconnect");
                    mGbDevice.setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, mContext);
                }

                LOG.debug("finished thread {}", getName());
            }
        };
    }

    public BtBRQueue(BluetoothAdapter btAdapter, GBDevice gbDevice, Context context, SocketCallback socketCallback, @NonNull UUID supportedService, int bufferSize) {
        mBtAdapter = btAdapter;
        mGbDevice = gbDevice;
        mContext = context;
        mCallback = socketCallback;
        mService = supportedService;
        mBufferSize = bufferSize;
        mDisposed = new AtomicBoolean(false);

        mWriteHandlerThread.start();

        new Handler(mWriteHandlerThread.getLooper()).post(()
                -> LOG.debug("started thread {}", Thread.currentThread().getName()));

        LOG.debug("Write handler thread is prepared, creating write handler");
        mWriteHandler = new Handler(mWriteHandlerThread.getLooper()) {
            @SuppressLint("MissingPermission")
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case HANDLER_SUBJECT_CONNECT: {
                        if (mBtSocket == null) {
                            LOG.error("Got request to connect to RFCOMM socket, but it is null");
                            if (!GBApplication.getPrefs().getAutoReconnect(mGbDevice)) {
                                mGbDevice.setUpdateState(GBDevice.State.NOT_CONNECTED, mContext);
                            } else {
                                mGbDevice.setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, mContext);
                            }
                            return;
                        }

                        try {
                            mBtSocket.connect();

                            LOG.info("Connected to RFCOMM socket for {}", mGbDevice.getName());
                            setDeviceConnectionState(GBDevice.State.CONNECTED);

                            if (readThread == null || !readThread.isAlive()) {
                                readThread = createReadThread();
                            }

                            // now that connect has been created, start the threads
                            readThread.start();
                            onConnectionEstablished();
                        } catch (IOException e) {
                            LOG.error("IO exception while establishing socket connection: ", e);

                            cleanup();

                            if (!GBApplication.getPrefs().getAutoReconnect(mGbDevice)) {
                                mGbDevice.setUpdateState(GBDevice.State.NOT_CONNECTED, mContext);
                            } else {
                                mGbDevice.setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, mContext);
                            }
                        } catch (SecurityException e) {
                            LOG.error("Security exception while establishing socket connection: ", e);

                            cleanup();

                            if (!GBApplication.getPrefs().getAutoReconnect(mGbDevice)) {
                                mGbDevice.setUpdateState(GBDevice.State.NOT_CONNECTED, mContext);
                            } else {
                                mGbDevice.setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, mContext);
                            }
                        }

                        return;
                    }
                    case HANDLER_SUBJECT_PERFORM_TRANSACTION: {
                        try {
                            if (!isConnected()) {
                                LOG.debug("Not connected, updating device state to WAITING_FOR_RECONNECT");
                                setDeviceConnectionState(GBDevice.State.WAITING_FOR_RECONNECT);
                                return;
                            }

                            if (!(msg.obj instanceof Transaction transaction)) {
                                LOG.error("msg.obj is not an instance of Transaction");
                                return;
                            }

                            for (BtBRAction action : transaction.getActions()) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("About to run action: {}", action);
                                }

                                if (action.run(mBtSocket)) {
                                    LOG.debug("Action ok: {}", action);
                                } else {
                                    LOG.error("Action returned false, cancelling further actions in transaction: {}", action);
                                    break;
                                }
                            }
                        } catch (Throwable ex) {
                            LOG.error("IO Write Thread died: ", ex);
                        }

                        return;
                    }
                }

                LOG.warn("Unhandled write handler message {}", msg.what);
            }
        };
    }

    /**
     * Connects to the given remote device. Note that this does not perform any device
     * specific initialization. This should be done in the specific {@link DeviceSupport}
     * class.
     *
     * @return <code>true</code> whether the connection attempt was successfully triggered and <code>false</code> if that failed or if there is already a connection
     */
    @SuppressLint("MissingPermission")
    public boolean connect() {
        final GBDevice.State state = mGbDevice.getState();
        if (state.equalsOrHigherThan(GBDevice.State.CONNECTING)) {
            LOG.warn("connect - ignored, state is {}", state);
            return false;
        } else if (mBtSocket != null) {
            LOG.warn("connect - ignored, mBtSocket isn't null");
            return false;
        } else if (mDisposed.get()) {
            LOG.error("connect - ignored, this BtBRQueue has already been disposed");
            return false;
        }

        LOG.info("Attempting to connect to {} ({})", mGbDevice.getName(), mGbDevice.getAddress());

        // stop discovery before connection is made
        mBtAdapter.cancelDiscovery();

        // revert to original state upon exception
        GBDevice.State originalState = mGbDevice.getState();
        setDeviceConnectionState(GBDevice.State.CONNECTING);

        try {
            BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(mGbDevice.getAddress());
            mBtSocket = btDevice.createRfcommSocketToServiceRecord(mService);
        } catch (IOException e) {
            LOG.error("Unable to connect to RFCOMM endpoint: ", e);
            setDeviceConnectionState(originalState);
            cleanup();
            return false;
        }

        LOG.debug("Socket created, connecting in handler");
        mWriteHandler.sendMessageAtFrontOfQueue(mWriteHandler.obtainMessage(HANDLER_SUBJECT_CONNECT));
        return true;
    }

    private void onConnectionEstablished() {
        mCallback.onConnectionEstablished();
    }

    public void disconnect() {
        if (mWriteHandlerThread.isAlive()) {
            mWriteHandlerThread.quit();
            LOG.debug("finished thread {}", mWriteHandlerThread.getName());
        }

        if (mBtSocket != null && mBtSocket.isConnected()) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                LOG.error("IO exception while closing socket in disconnect(): ", e);
            }
        }

        mBtSocket = null;
        setDeviceConnectionState(GBDevice.State.NOT_CONNECTED);
    }

    /**
     * Check whether a connection to the device exists and whether a socket connection has been
     * initialized and connected
     * @return true if the Bluetooth device is connected and the socket is ready, false otherwise
     */
    boolean isConnected() {
        return mGbDevice.isConnected() &&
                mBtSocket != null &&
                mBtSocket.isConnected();
    }

    /**
     * Add a finalized {@link Transaction} to the write handler's queue
     *
     * @param transaction The transaction to be run in the handler thread's looper
     */
    public void add(Transaction transaction) {
        LOG.debug("Adding transaction to looper message queue: {}", transaction);

        if (!transaction.isEmpty()) {
            mWriteHandler.obtainMessage(HANDLER_SUBJECT_PERFORM_TRANSACTION, transaction).sendToTarget();
        }
    }

    private void setDeviceConnectionState(GBDevice.State newState) {
        LOG.debug("New device connection state: {}", newState);
        mGbDevice.setState(newState);
        mGbDevice.sendDeviceUpdateIntent(mContext, GBDevice.DeviceUpdateSubject.CONNECTION_STATE);
    }

    private void cleanup() {
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (final IOException ignored) {
            }
            mBtSocket = null;
        }
    }

    public void dispose() {
        if (mDisposed.getAndSet(true)) {
            LOG.warn("dispose() was called repeatedly");
            return;
        }

        disconnect();

        if (readThread != null && readThread.isAlive()) {
            readThread.interrupt();
            readThread = null;
        }
    }
}
