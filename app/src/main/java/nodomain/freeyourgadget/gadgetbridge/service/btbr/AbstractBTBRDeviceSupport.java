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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BleNamesResolver;

/**
 * Abstract base class for devices connected through a serial protocol, like RFCOMM BT or TCP socket.
 * <p/>
 * The connection to the device and all communication is made with a generic
 * {@link nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread}.
 * Messages to the device are encoded
 * sent via {@link nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread}.
 *
 * @see nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread
 */
public abstract class AbstractBTBRDeviceSupport extends AbstractDeviceSupport implements SocketCallback {

    /// used to guard {@link #connect()}, {@link #disconnect()} and {@link #dispose()}
    protected final Object ConnectionMonitor = new Object();

    private BtBRQueue mQueue;
    private UUID mSupportedService = null;
    private final int mBufferSize;
    private final Logger logger;

    /**
     * @param bufferSize should be larger than the maximum expected message side, or messages might be lost.
     */
    public AbstractBTBRDeviceSupport(Logger logger, final int bufferSize) {
        this.logger = logger;
        this.mBufferSize = bufferSize;
        if (logger == null) {
            throw new IllegalArgumentException("logger must not be null");
        }
    }

    @Override
    public boolean connect() {
        synchronized (ConnectionMonitor) {
            final UUID supportedService = getSupportedService();
            if (supportedService == null) {
                // Before throwing the exception, list the available UUIDs
                final BluetoothDevice btDevice = getBluetoothAdapter().getRemoteDevice(gbDevice.getAddress());
                @SuppressLint("MissingPermission") final ParcelUuid[] uuids = btDevice.getUuids();
                if (uuids == null || uuids.length == 0) {
                    logger.warn("Device provided no UUIDs to connect to: {}", gbDevice);
                } else {
                    for (ParcelUuid uuid : uuids) {
                        logger.debug(
                                "discovered service: {}: {}",
                                BleNamesResolver.resolveServiceName(uuid.toString()),
                                uuid
                        );
                    }
                }

                throw new NullPointerException("No supported service UUID specified");
            }

            if (mQueue == null) {
                mQueue = new BtBRQueue(getBluetoothAdapter(), getDevice(), getContext(), this, supportedService, getBufferSize());
            }
            return mQueue.connect();
        }
    }

    public void disconnect() {
        synchronized (ConnectionMonitor) {
            if (mQueue != null) {
                mQueue.disconnect();
            }
        }
    }

    /**
     * Subclasses should populate the given builder to initialize the device (if necessary). This
     * function might be called multiple times for the same support instance (eg. in the case of a
     * reconnection), and should ensure that any state is also reset as required.
     *
     * @return the same builder as passed as the argument
     */
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        return builder;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            if (mQueue != null) {
                mQueue.dispose();
                mQueue = null;
            }
        }
    }

    public TransactionBuilder createTransactionBuilder(String taskName) {
        return new TransactionBuilder(taskName, this);
    }

    @Override
    public boolean isConnected() {
        // in a multi-threaded environment the queue knows
        // best about the up-to-date connection status
        return (mQueue != null) && mQueue.isConnected();
    }

    BtBRQueue getQueue() {
        return mQueue;
    }

    /**
     * Subclasses should call this method to add services they support.
     * Only supported services will be queried for characteristics.
     *
     * @param aSupportedService the supported service uuid
     */
    protected void addSupportedService(UUID aSupportedService) {
        mSupportedService = aSupportedService;
    }

    protected UUID getSupportedService() {
        return mSupportedService;
    }

    protected int getBufferSize() {
        return mBufferSize;
    }

    /**
     * Utility method that may be used to log incoming messages when we don't know how to deal with them yet.
     */
    public void logMessageContent(byte[] value) {
        logger.info("RECEIVED DATA WITH LENGTH: {}", (value != null) ? value.length : "(null)");
        Logging.logBytes(logger, value);
    }

    @Override
    public void onConnectionEstablished() {
        try {
            initializeDevice(createTransactionBuilder("Initializing device")).queue();
        } catch (final Exception ex) {
            final GBDevice device = getDevice();

            if (device != null) {
                logger.error("Exception raised while initializing device {} (address {}), disconnecting", device.getName(), device.getAddress(), ex);
                device.setState(GBDevice.State.WAITING_FOR_RECONNECT);
                device.sendDeviceUpdateIntent(getContext());
            } else {
                logger.error("Exception raised while initializing unknown device", ex);
            }
        }
    }
}
