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
 * <p/>
 * The connection to the device and all communication is made with a generic {@link BtLEQueue}.
 * Messages to the device are encoded as {@link BtLEAction actions} or {@link BtLEServerAction actions}
 * that are grouped with a {@link Transaction} or {@link ServerTransaction} and sent via {@link BtLEQueue}.
 *
 * @see TransactionBuilder
 * @see BtLEQueue
 */
public abstract class AbstractBTLESingleDeviceSupport extends AbstractBTLEDeviceSupport {
    private int mMTU = 23;
    private BtLEQueue mQueue;
    private Map<UUID, BluetoothGattCharacteristic> mAvailableCharacteristics;
    private final Set<UUID> mSupportedServices = new HashSet<>(4);
    private final Set<BluetoothGattService> mSupportedServerServices = new HashSet<>(4);
    private final Logger logger;

    private final List<AbstractBleProfile<?>> mSupportedProfiles = new ArrayList<>();
    private final Object characteristicsMonitor = new Object();

    /// used to guard {@link #connect()}, {@link #disconnect()} and {@link #dispose()}
    protected final Object ConnectionMonitor = new Object();

    private BleIntentApi bleApi = null;

    public AbstractBTLESingleDeviceSupport(Logger logger) {
        this.logger = logger;
        if (logger == null) {
            throw new IllegalArgumentException("logger must not be null");
        }
    }

    /// Device specific code usually should {@code synchronize()} on {@link #ConnectionMonitor}.
    /// @see AbstractBTLEDeviceSupport#connect()
    @CallSuper
    @Override
    public boolean connect() {
        synchronized (ConnectionMonitor) {
            if (mQueue == null) {
                mQueue = new BtLEQueue(getDevice(), mSupportedServerServices, this);
            }

            return mQueue.connect();
        }
    }

    /// Disconnects, but doesn't dispose.
    /// <p>
    /// Device specific code usually should {@code synchronize()} on {@link #ConnectionMonitor}.
    /// </p>
    @CallSuper
    public void disconnect() {
        synchronized (ConnectionMonitor) {
            if (mQueue != null) {
                mQueue.disconnect();
            }
        }
    }

    public BleIntentApi getBleApi() {
        return bleApi;
    }

    @Override
    public void onSendConfiguration(String config) {
        super.onSendConfiguration(config);

        if (bleApi != null) {
            bleApi.onSendConfiguration(config);
        }
    }

    @CallSuper
    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        super.setContext(gbDevice, btAdapter, context);

        if(BleIntentApi.isEnabled(gbDevice)) {
            bleApi = new BleIntentApi(this, 0);
            bleApi.handleBLEApiPrefs();
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

    /// Device specific code usually should {@code synchronize()} on {@link #ConnectionMonitor}.
    /// @see AbstractBTLEDeviceSupport#dispose()
    @CallSuper
    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            if (mQueue != null) {
                mQueue.dispose();
                mQueue = null;
            }

            if (bleApi != null) {
                bleApi.dispose();
            }
        }
    }

    public TransactionBuilder createTransactionBuilder(String taskName) {
        return new TransactionBuilder(taskName, this, 0);
    }

    @Override
    public boolean isConnected(){
        // in a multi-threaded environment the queue knows
        // best about the up-to-date connection status
        return (mQueue != null) && mQueue.isConnected();
    }

    /**
     * Send commands like this to the device:
     * <p>
     * <code>performInitialized("sms notification").write(someCharacteristic, someByteArray).queue();</code>
     * </p>
     * This will asynchronously
     * <ul>
     * <li>connect to the device (if necessary)</li>
     * <li>initialize the device (if necessary)</li>
     * <li>execute the commands collected with the returned transaction builder</li>
     * </ul>
     *
     * @see TransactionBuilder#queueConnected()
     * @see #initializeDevice(TransactionBuilder)
     */
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        if (!isConnected()) {
            logger.debug("Connecting to device for {}", taskName);
            if (!connect()) {
                throw new IOException("1: Unable to connect to device: " + getDevice());
            }
        }
        if (!isInitialized()) {
            logger.debug("Initializing device for {}", taskName);
            // first, add a transaction that performs device initialization
            TransactionBuilder builder = createTransactionBuilder("Initialize device");
            builder.add(new CheckInitializedAction(gbDevice));
            initializeDevice(builder);
            builder.queue();
        }
        return createTransactionBuilder(taskName);
    }

    public ServerTransactionBuilder createServerTransactionBuilder(String taskName) {
        return new ServerTransactionBuilder(taskName);
    }

    public ServerTransactionBuilder performServer(String taskName) throws IOException {
        if (!isConnected()) {
            if(!connect()) {
                throw new IOException("1: Unable to connect to device: " + getDevice());
            }
        }
        return createServerTransactionBuilder(taskName);
    }

    public BtLEQueue getQueue() {
        return mQueue;
    }

    @Override
    BtLEQueue getQueue(int deviceIdx){
        if(deviceIdx != 0){
            throw new IllegalArgumentException("deviceIdx is " + deviceIdx);
        }
        return getQueue();
    }

    /**
     * Subclasses should call this method to add services they support.
     * Only supported services will be queried for characteristics.
     *
     * @param aSupportedService supported service uuid
     * @see #getCharacteristic(UUID)
     */
    protected void addSupportedService(UUID aSupportedService) {
        mSupportedServices.add(aSupportedService);
    }

    protected void addSupportedProfile(AbstractBleProfile<?> profile) {
        mSupportedProfiles.add(profile);
    }

    /**
     * Subclasses should call this method to add server services they support.
     */
    protected void addSupportedServerService(BluetoothGattService service) {
        mSupportedServerServices.add(service);
    }

    /**
     * Returns the characteristic matching the given UUID. Only characteristics
     * are returned whose service is marked as supported.
     *
     * @param uuid characteristic uuid
     * @return the characteristic for the given UUID or <code>null</code>
     * @see #addSupportedService(UUID)
     */
    @Nullable
    public BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        synchronized (characteristicsMonitor) {
            if (mAvailableCharacteristics == null) {
                return null;
            }
            return mAvailableCharacteristics.get(uuid);
        }
    }

    @Nullable
    @Override
    BluetoothGattCharacteristic getCharacteristic(UUID uuid, int deviceIdx){
        if(deviceIdx != 0){
            throw new IllegalArgumentException("deviceIdx is " + deviceIdx);
        }
        return getCharacteristic(uuid);
    }

    private void gattServicesDiscovered(List<BluetoothGattService> discoveredGattServices) {
        if (discoveredGattServices == null) {
            logger.warn("No gatt services discovered: null!");
            return;
        }
        Set<UUID> supportedServices = getSupportedServices();
        Map<UUID, BluetoothGattCharacteristic> newCharacteristics = new HashMap<>();

        final Prefs devicePrefs = GBApplication.getDevicePrefs(getDevice());
        final boolean prefs_device_gatt_synchronous_writes = devicePrefs.getBoolean(PREFS_DEVICE_GATT_SYNCHRONOUS_WRITES, false);

        for (BluetoothGattService service : discoveredGattServices) {
            if(bleApi != null) {
                bleApi.addService(service);
            }

            if (supportedServices.contains(service.getUuid())) {
                logger.debug("discovered supported service: {}: {}", BleNamesResolver.resolveServiceName(service.getUuid().toString()), service.getUuid());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                if (characteristics == null || characteristics.isEmpty()) {
                    logger.warn("Supported LE service {} did not return any characteristics", service.getUuid());
                    continue;
                }
                HashMap<UUID, BluetoothGattCharacteristic> intmAvailableCharacteristics = new HashMap<>(characteristics.size());
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    intmAvailableCharacteristics.put(characteristic.getUuid(), characteristic);
                    logger.info("    characteristic: {}: {} ({})", BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()), characteristic.getUuid(), BleNamesResolver.getCharacteristicPropertyString(characteristic.getProperties()));

                    if (prefs_device_gatt_synchronous_writes && characteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
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
                    mAvailableCharacteristics = newCharacteristics;
                }
            } else {
                logger.debug("discovered unsupported service: {}: {}", BleNamesResolver.resolveServiceName(service.getUuid().toString()), service.getUuid());
            }
        }
    }

    protected Set<UUID> getSupportedServices() {
        return mSupportedServices;
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
        gattServicesDiscovered(gatt.getServices());

        if (getDevice().getState().compareTo(GBDevice.State.INITIALIZING) >= 0) {
            logger.warn("Services discovered, but device state is already " + getDevice().getState() + " for device: " + getDevice() + ", so ignoring");
            return;
        }
        TransactionBuilder builder = createTransactionBuilder("Initializing device");

        if(bleApi != null) {
            bleApi.initializeDevice(builder);
        }

        initializeDevice(builder);

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
        if(bleApi != null && status == BluetoothGatt.GATT_SUCCESS) {
            bleApi.onCharacteristicChanged(characteristic, value);
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
    public boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, byte[] value) {
        for (AbstractBleProfile<?> profile : mSupportedProfiles) {
            if (profile.onDescriptorRead(gatt, descriptor, status, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
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
        if(bleApi != null) {
            bleApi.onCharacteristicChanged(characteristic, value);
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

    @CallSuper
    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            this.mMTU = mtu;
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {

    }

    @Override
    public boolean onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    public boolean onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        return false;
    }

    @Override
    public boolean onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        return false;
    }

    @Override
    public boolean onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
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
        logger.warn("onServiceChanged is NOT supported by AbstractBTLESingleDeviceSupport");
    }

    /**
     * Get the current MTU, or the minimum 23 if unknown
     */
    public int getMTU() {
        return mMTU;
    }

    @Override
    int getMTU(int deviceIdx) {
        if(deviceIdx != 0){
            throw new IllegalArgumentException("deviceIdx is " + deviceIdx);
        }
        return getMTU();
    }
}
