/*  Copyright (C) 2015-2025 Andreas Böhler, Arjan Schrijver, Carsten Pfeiffer,
    Daniel Dakhno, Daniele Gobbetti, Johannes Krude, JohnnySun, José Rebelo,
    Thomas Kuehne

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

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREFS_DEVICE_GATT_SYNCHRONOUS_WRITES;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.CheckInitializedAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * Abstract base class for all devices connected through Bluetooth Low Energy (LE) aka
 * Bluetooth Smart.
 * <p>
 * The connection to the device and all communication is made with a generic {@link BtLEQueue}.
 * Messages to the device are encoded as {@link BtLEAction actions} or {@link BtLEServerAction actions}
 * that are grouped with a {@link Transaction} or {@link ServerTransaction} and sent via {@link BtLEQueue}.
 *
 * @see TransactionBuilder
 * @see BtLEQueue
 */
public abstract class AbstractBTLEMultiDeviceSupport extends AbstractBTLEDeviceSupport {
    private final int deviceCount;
    private final Set<UUID>[] mSupportedServices;
    private final Set<BluetoothGattService>[] mSupportedServerServices;
    private final Logger logger;
    private final List<AbstractBleProfile<?>> mSupportedProfiles = new ArrayList<>();
    //this is common for all BTLE devices. see http://stackoverflow.com/questions/18699251/finding-out-android-bluetooth-le-gatt-profiles
    private final Object characteristicsMonitor = new Object();
    private final int[] mMTUs;
    private final BtLEQueue[] mQueues;
    private final BleIntentApi[] bleApis;
    private final GBDevice[] devices;
    private final Map<UUID, BluetoothGattCharacteristic>[] mAvailableCharacteristics;

    /// used to guard {@link #connect()}, {@link #disconnect()} and {@link #dispose()}
    protected final Object ConnectionMonitor = new Object();

    public AbstractBTLEMultiDeviceSupport(Logger logger, int deviceCount) {
        this.logger = logger;
        this.deviceCount = deviceCount;
        mSupportedServices = new Set[deviceCount];
        mSupportedServerServices = new Set[deviceCount];
        mMTUs = new int[deviceCount];
        for (int i = 0; i < deviceCount; i++) {
            mSupportedServices[i] = new HashSet<>(4);
            mSupportedServerServices[i] = new HashSet<>(4);
            mMTUs[i] = 23;
        }
        mQueues = new BtLEQueue[deviceCount];
        bleApis = new BleIntentApi[deviceCount];
        devices = new GBDevice[deviceCount];
        mAvailableCharacteristics = new Map[deviceCount];
        if (logger == null) {
            throw new IllegalArgumentException("logger must not be null");
        }
    }

    public AbstractBTLEMultiDeviceSupport(Logger logger) {
        this(logger, 1);
    }

    private void validateDeviceIndex(int deviceIdx) {
        if (deviceIdx < 0 || deviceIdx > deviceCount) {
            throw new IllegalArgumentException(
                    "Request device index " + deviceIdx + " doesn't exist.");
        }
    }

    public BtLEQueue getQueue() {
        return getQueue(0);
    }

    @Override
    public BtLEQueue getQueue(int deviceIdx) {
        validateDeviceIndex(deviceIdx);
        return mQueues[deviceIdx];
    }

    public GBDevice getDevice(int deviceIdx) {
        validateDeviceIndex(deviceIdx);
        return devices[deviceIdx];
    }

    private int getDeviceIndexForAddress(String address) {
        // Shortcut for one device.
        if (deviceCount == 1) {
            return 0;
        }

        for (int i = 0; i < deviceCount; i++) {
            if (devices[i] != null && devices[i].getAddress().equals(address)) {
                return i;
            }
        }
        throw new IllegalArgumentException("No sub device with address: " + address);
    }

    /// Device specific code usually should {@code synchronize()} on {@link #ConnectionMonitor}.
    /// @see AbstractBTLEDeviceSupport#connect()
    @CallSuper
    @Override
    public boolean connect() {
        // Connect to the queue for each device.
        synchronized (ConnectionMonitor) {
            for (int i = 0; i < deviceCount; i++) {
                if (mQueues[i] == null && devices[i] != null) {
                    mQueues[i] = new BtLEQueue(devices[i], mSupportedServerServices[i], this);
                }

                if (mQueues[i] != null && !mQueues[i].connect()) {
                    return false;
                }
            }
        }
        return true;
    }

    /// Disconnects, but doesn't dispose.
    /// <p>
    /// Device specific code usually should {@code synchronize()} on {@link #ConnectionMonitor}.
    /// </p>
    @CallSuper
    public void disconnect() {
        synchronized (ConnectionMonitor) {
            for (BtLEQueue queue : mQueues) {
                if (queue != null) {
                    queue.disconnect();
                }
            }
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        for (BleIntentApi bleApi : bleApis) {
            if (bleApi != null) {
                bleApi.onSendConfiguration(config);
            }
        }
    }

    public void setDevice(GBDevice device, int deviceIdx) {
        validateDeviceIndex(deviceIdx);
        devices[deviceIdx] = device;
    }

    @Override
    public void setContext(GBDevice device, BluetoothAdapter btAdapter, Context context) {
        super.setContext(device, btAdapter, context);

        // Device 0 should be the parent device.
        devices[0] = device;
        for (int i = 0; i < deviceCount; i++) {
            if (devices[i] != null && BleIntentApi.isEnabled(device)) {
                bleApis[i] = new BleIntentApi(this, i);
                bleApis[i].handleBLEApiPrefs();
            }
        }
    }

    /**
     * Subclasses should populate the given builder to initialize the device (if necessary).
     *
     * @return the same builder as passed as the argument
     */
    protected TransactionBuilder initializeDevice(TransactionBuilder builder, int deviceIdx) {
        return builder;
    }

    /// Device specific code usually should {@code synchronize()} on {@link #ConnectionMonitor}.
    /// @see AbstractBTLEDeviceSupport#dispose()
    @CallSuper
    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            for (int i = 0; i < deviceCount; i++) {
                if (mQueues[i] != null) {
                    mQueues[i].dispose();
                    mQueues[i] = null;
                }
                if (bleApis[i] != null) {
                    bleApis[i].dispose();
                }
            }
        }
    }

    public TransactionBuilder createTransactionBuilder(String taskName, int deviceIdx) {
        return new TransactionBuilder(taskName + "_" + deviceIdx, this, deviceIdx);
    }

    public ServerTransactionBuilder createServerTransactionBuilder(String taskName) {
        return new ServerTransactionBuilder(taskName);
    }

    /**
     * Send commands like this to the device:
     * <p>
     * <code>performInitialized("sms notification").write(someCharacteristic, someByteArray).queue(getQueue());</code>
     * </p>
     * This will asynchronously
     * <ul>
     * <li>connect to the device (if necessary)</li>
     * <li>initialize the device (if necessary)</li>
     * <li>execute the commands collected with the returned transaction builder</li>
     * </ul>
     *
     * @see TransactionBuilder#queueConnected()
     * @see #initializeDevice(TransactionBuilder, int)
     */
    public TransactionBuilder performInitialized(String taskName, int deviceIdx)
            throws IOException {
        if (devices[deviceIdx] == null) {
            throw new IllegalArgumentException(
                    "Requested device index " + deviceIdx + " doesn't exist.");
        }
        if (!isConnected()) {
            logger.debug("Connecting to device for {}", taskName);
            if (!connect()) {
                throw new IOException("1: Unable to connect to device: " + getDevice(deviceIdx));
            }
        }
        if (!devices[deviceIdx].isInitialized()) {
            logger.debug("Initializing device for {}", taskName);
            // first, add a transaction that performs device initialization
            TransactionBuilder builder = createTransactionBuilder("Initialize device", deviceIdx);
            builder.add(new CheckInitializedAction(devices[deviceIdx]));
            initializeDevice(builder, deviceIdx);
            builder.queue();
        }
        return createTransactionBuilder(taskName, deviceIdx);
    }

    /**
     * Subclasses should call this method to add services they support.
     * Only supported services will be queried for characteristics.
     *
     * @param aSupportedService supported service uuid
     * @see #getCharacteristic(UUID, int)
     */
    protected void addSupportedService(UUID aSupportedService, int deviceIdx) {
        validateDeviceIndex(deviceIdx);
        mSupportedServices[deviceIdx].add(aSupportedService);
    }

    protected void addSupportedProfile(AbstractBleProfile<?> profile) {
        mSupportedProfiles.add(profile);
    }

    /**
     * Subclasses should call this method to add server services they support.
     */
    protected void addSupportedServerService(BluetoothGattService service, int deviceIdx) {
        validateDeviceIndex(deviceIdx);
        mSupportedServerServices[deviceIdx].add(service);
    }

    /**
     * Returns the characteristic matching the given UUID. Only characteristics
     * are returned whose service is marked as supported.
     *
     * @param uuid characteristic uuid
     * @return the characteristic for the given UUID or <code>null</code>
     * @see #addSupportedService(UUID, int)
     */
    @Override
    @Nullable
    public BluetoothGattCharacteristic getCharacteristic(UUID uuid, int deviceIdx) {
        validateDeviceIndex(deviceIdx);

        synchronized (characteristicsMonitor) {
            if (mAvailableCharacteristics[deviceIdx] == null) {
                return null;
            }
            return mAvailableCharacteristics[deviceIdx].get(uuid);
        }
    }

    private void gattServicesDiscovered(List<BluetoothGattService> discoveredGattServices,
                                        int deviceIdx) {
        if (discoveredGattServices == null) {
            logger.warn("No gatt services discovered: null!");
            return;
        }

        final Prefs devicePrefs = GBApplication.getDevicePrefs(getDevice());
        final boolean prefs_device_ble_synchronous_writes = devicePrefs.getBoolean(PREFS_DEVICE_GATT_SYNCHRONOUS_WRITES, false);

        Set<UUID> supportedServices = getSupportedServices(deviceIdx);
        Map<UUID, BluetoothGattCharacteristic> newCharacteristics = new HashMap<>();
        for (BluetoothGattService service : discoveredGattServices) {
            for (BleIntentApi bleApi : bleApis) {
                if (bleApi != null) {
                    bleApi.addService(service);
                }
            }

            if (supportedServices.contains(service.getUuid())) {
                logger.debug("discovered supported service: {}: {}",
                             BleNamesResolver.resolveServiceName(service.getUuid().toString()),
                             service.getUuid());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                if (characteristics == null || characteristics.isEmpty()) {
                    logger.warn("Supported LE service {} did not return any characteristics",
                                service.getUuid());
                    continue;
                }
                HashMap<UUID, BluetoothGattCharacteristic> intmAvailableCharacteristics =
                        new HashMap<>(characteristics.size());
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    intmAvailableCharacteristics.put(characteristic.getUuid(), characteristic);
                    logger.info("    characteristic: {}: {} ({})",
                                BleNamesResolver.resolveCharacteristicName(
                                        characteristic.getUuid().toString()),
                                characteristic.getUuid(),
                                BleNamesResolver.getCharacteristicPropertyString(
                                        characteristic.getProperties()));

                    if (prefs_device_ble_synchronous_writes && characteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                        if (0 != (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE)) {
                            // if both PROPERTY_WRITE and PROPERTY_WRITE_NO_RESPONSE are set Android
                            // defaults to WRITE_TYPE_NO_RESPONSE and then calls onCharacteristicWrite
                            // when the write has been queued on the mobile but potentially not yet
                            // actually completely reached the gadget
                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            logger.debug("changed WriteType of characteristic {} to synchronous", characteristic.getUuid());
                        }
                    }
                }
                newCharacteristics.putAll(intmAvailableCharacteristics);

                synchronized (characteristicsMonitor) {
                    mAvailableCharacteristics[deviceIdx] = newCharacteristics;
                }
            } else {
                logger.debug("discovered unsupported service: {}: {}",
                             BleNamesResolver.resolveServiceName(service.getUuid().toString()),
                             service.getUuid());
            }
        }
    }

    protected Set<UUID> getSupportedServices(int deviceIdx) {
        validateDeviceIndex(deviceIdx);
        return mSupportedServices[deviceIdx];
    }

    /**
     * Utility method that may be used to log incoming messages when we don't know how to deal with them yet.
     */
    public void logMessageContent(byte[] value) {
        logger.info("RECEIVED DATA WITH LENGTH: {}", (value != null) ? value.length : "(null)");
        Logging.logBytes(logger, value);
    }

    // default implementations of event handler methods (gatt callbacks)
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            profile.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        int deviceIdx = getDeviceIndexForAddress(gatt.getDevice().getAddress());
        gattServicesDiscovered(gatt.getServices(), deviceIdx);

        if (getDevice(deviceIdx).getState().compareTo(GBDevice.State.INITIALIZING) >= 0) {
            logger.warn(
                    "Services discovered, but device {} ({}) is already in state {}, so ignoring",
                    getDevice(deviceIdx), deviceIdx, getDevice(deviceIdx).getState());
            return;
        }
        TransactionBuilder builder = createTransactionBuilder("Initializing device", deviceIdx);

        if (bleApis[deviceIdx] != null) {
            bleApis[deviceIdx].initializeDevice(builder);
        }

        initializeDevice(builder, deviceIdx);

        boolean lowPower = getDevicePrefs().getConnectionPriorityLowPower();
        // have to explicitly request normal ("balanced") as some Android devices remember the last
        // request. Else low power would become a set once option.
        builder.requestConnectionPriority(lowPower ? BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER : BluetoothGatt.CONNECTION_PRIORITY_BALANCED);

        builder.queue();
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, byte[] value,
                                        int status) {

        int deviceIdx = getDeviceIndexForAddress(gatt.getDevice().getAddress());
        if (bleApis[deviceIdx] != null) {
            bleApis[deviceIdx].onCharacteristicChanged(characteristic, value);
        }

        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onCharacteristicRead(gatt, characteristic, value, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onCharacteristicWrite(gatt, characteristic, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                    int status, byte[] value) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onDescriptorRead(gatt, descriptor, status, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onDescriptorWrite(gatt, descriptor, status)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic, byte[] value) {
        int deviceIdx = getDeviceIndexForAddress(gatt.getDevice().getAddress());
        if (bleApis[deviceIdx] != null) {
            bleApis[deviceIdx].onCharacteristicChanged(characteristic, value);
        }

        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onCharacteristicChanged(gatt, characteristic, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            profile.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            int deviceIdx = getDeviceIndexForAddress(gatt.getDevice().getAddress());
            mMTUs[deviceIdx] = mtu;
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {}

    @Override
    public boolean onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                               BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    public boolean onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                BluetoothGattCharacteristic characteristic,
                                                boolean preparedWrite, boolean responseNeeded,
                                                int offset, byte[] value) {
        return false;
    }

    @Override
    public boolean onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                           BluetoothGattDescriptor descriptor) {
        return false;
    }

    @Override
    public boolean onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                            BluetoothGattDescriptor descriptor,
                                            boolean preparedWrite, boolean responseNeeded,
                                            int offset, byte[] value) {
        return false;
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
    }

    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status){
    }

    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status){
    }

    @Override
    public void onServiceChanged(@NonNull BluetoothGatt gatt){
        logger.warn("onServiceChanged is NOT supported by AbstractBTLEMultiDeviceSupport");
    }

    /**
     * Get the current MTU, or the minimum 23 if unknown
     */
    @Override
    public int getMTU(int deviceIdx) {
        validateDeviceIndex(deviceIdx);
        return mMTUs[deviceIdx];
    }
}
