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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractBluetoothDeviceSupport;
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
public abstract class AbstractBTBRDeviceSupport extends AbstractBluetoothDeviceSupport implements SocketCallback {

    /**
     * "No explicit RFCOMM channel". Matches what
     * {@link BluetoothDevice#createRfcommSocketToServiceRecord} uses internally, and is the default
     * of both {@link #getRfcommChannel()} (primary socket resolved through SDP) and
     * {@link TransactionBuilder#setChannel(int)} (transaction goes to the primary socket).
     */
    public static final int RFCOMM_CHANNEL_UNSPECIFIED = -1;

    /// used to guard {@link #connect()}, {@link #disconnect()} and {@link #dispose()}
    protected final Object ConnectionMonitor = new Object();

    private BtBRQueue mQueue;
    /// Secondary ("aux") RFCOMM sockets keyed by channel number, opened on demand via
    /// {@link #openAuxChannel(int)}. Empty for the common single-socket case.
    private final Map<Integer, BtBRQueue> mAuxQueues = new ConcurrentHashMap<>();
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
                final ParcelUuid[] uuids = getBluetoothDeviceUuids();
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
                mQueue = new BtBRQueue(
                        getBluetoothAdapter(),
                        getDevice(),
                        getContext(),
                        this,
                        supportedService,
                        getBufferSize(),
                        getConnectDelayMillis(),
                        getRfcommChannel()
                );
            }
            return mQueue.connect();
        }
    }

    @SuppressLint("MissingPermission")
    protected ParcelUuid[] getBluetoothDeviceUuids() {
        final BluetoothDevice btDevice = getBluetoothAdapter().getRemoteDevice(gbDevice.getAddress());
        return btDevice.getUuids();
    }

    public void disconnect() {
        synchronized (ConnectionMonitor) {
            closeAllAuxChannels();
            if (mQueue != null) {
                mQueue.disconnect();
            }
        }
    }

    /**
     * Opens a secondary ("aux") RFCOMM socket on the given channel, in addition to the primary
     * connection. Aux sockets share this support's {@link #onSocketRead(byte[])} sink but never
     * affect the primary device connection state or re-run device initialization. Writes are
     * routed to an aux socket by setting the target channel on the transaction
     * ({@link TransactionBuilder#setChannel(int)}); {@link #RFCOMM_CHANNEL_UNSPECIFIED} and the
     * primary socket's own {@link #getRfcommChannel()} both mean the primary socket.
     *
     * @return true if the aux connection attempt was successfully triggered
     */
    public boolean openAuxChannel(final int channel) {
        synchronized (ConnectionMonitor) {
            if (channel <= 0 || channel == getRfcommChannel()) {
                logger.warn("openAuxChannel - ignored, invalid aux channel {} (primary channel is {})",
                        channel, getRfcommChannel());
                return false;
            }
            final UUID supportedService = getSupportedService();
            if (supportedService == null) {
                logger.warn("openAuxChannel - ignored, no supported service UUID");
                return false;
            }
            if (mAuxQueues.containsKey(channel)) {
                logger.debug("openAuxChannel - channel {} already open", channel);
                return true;
            }
            final BtBRQueue auxQueue = new BtBRQueue(
                    getBluetoothAdapter(),
                    getDevice(),
                    getContext(),
                    this,
                    supportedService,
                    getBufferSize(),
                    getConnectDelayMillis(),
                    channel,
                    true
            );
            mAuxQueues.put(channel, auxQueue);
            return auxQueue.connect();
        }
    }

    /// Closes and disposes the aux socket previously opened on the given channel, if any.
    public void closeAuxChannel(final int channel) {
        synchronized (ConnectionMonitor) {
            final BtBRQueue auxQueue = mAuxQueues.remove(channel);
            if (auxQueue != null) {
                auxQueue.dispose();
            }
        }
    }

    private void closeAllAuxChannels() {
        synchronized (ConnectionMonitor) {
            for (final BtBRQueue auxQueue : mAuxQueues.values()) {
                auxQueue.dispose();
            }
            mAuxQueues.clear();
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
            closeAllAuxChannels();
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
        // in a multithreaded environment the queue knows
        // best about the up-to-date connection status
        return (mQueue != null) && mQueue.isConnected();
    }

    BtBRQueue getQueue() {
        return mQueue;
    }

    /// Returns the queue for the given RFCOMM channel: an aux queue when the channel is an explicit
    /// one that is not the primary socket's ({@link #getRfcommChannel()}) and that aux socket is
    /// open, otherwise the primary queue. Callers can route unconditionally; an unavailable aux
    /// channel transparently falls back to the primary socket.
    BtBRQueue getQueue(final int channel) {
        // A negative channel is "unspecified" and always means the primary socket, as does the
        // primary socket's own channel for devices that pin it to a fixed number.
        if (channel >= 0 && channel != getRfcommChannel()) {
            final BtBRQueue auxQueue = mAuxQueues.get(channel);
            // Only route to the aux socket once it is actually connected; until then transactions
            // transparently use the primary socket so nothing is dropped while it comes up.
            if (auxQueue != null && auxQueue.isConnected()) {
                return auxQueue;
            }
        }
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

    /**
     * Subclasses can override this to specify a fixed RFCOMM channel number for the primary
     * socket. If {@link #RFCOMM_CHANNEL_UNSPECIFIED} (default), the service UUID is used for SDP
     * resolution.
     */
    protected int getRfcommChannel() {
        return RFCOMM_CHANNEL_UNSPECIFIED;
    }


    protected int getBufferSize() {
        return mBufferSize;
    }

    /**
     * Some devices fail to connect to the btrfcomm socket if we connect too fast. Increase this delay
     * to wait a few milliseconds.
     */
    protected int getConnectDelayMillis() {
        return 0;
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
