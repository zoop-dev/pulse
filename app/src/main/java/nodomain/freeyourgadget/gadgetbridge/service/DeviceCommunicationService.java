/*  Copyright (C) 2015-2026 Andreas Böhler, Andreas Shimokawa, Arjan
    Schrijver, Avamander, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti,
    Daniel Hauck, Davis Mosenkovs, Dikay900, Dmitriy Bogdanov, Frank Slezak,
    Gabriele Monaco, Gordon Williams, ivanovlev, João Paulo Barraca, José
    Rebelo, Julien Pivotto, Kasha, keeshii, Martin, Martin Braun, Matthieu
    Baerts, mvn23, NekoBox, Nephiel, Petr Vaněk, Sebastian Kranz, Sergey
    Trofimov, Steffen Liebergeld, Taavi Eomäe, TylerWilliamson, Uwe Hermann,
    Yoran Vulker, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_STRESS_TEST_CONNECT_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_STRESS_TEST_CONNECT_PARALLEL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_STRESS_TEST_DISPOSE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_DELETE_NOTIFICATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_DISCONNECT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_NOTIFICATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_CONNECT_FIRST_TIME;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_ID;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothConnectReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLEScanService;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.AutoConnectIntervalReceiver;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class DeviceCommunicationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static class DeviceStruct {
        private GBDevice device;
        private DeviceCoordinator coordinator;
        private DeviceSupport deviceSupport;

        public GBDevice getDevice() {
            return device;
        }

        public void setDevice(GBDevice device) {
            this.device = device;
        }

        public DeviceCoordinator getCoordinator() {
            return coordinator;
        }

        public void setCoordinator(DeviceCoordinator coordinator) {
            this.coordinator = coordinator;
        }

        public DeviceSupport getDeviceSupport() {
            return deviceSupport;
        }

        public void setDeviceSupport(DeviceSupport deviceSupport) {
            this.deviceSupport = deviceSupport;
        }
    }

    public static class DeviceNotFoundException extends GBException {
        private final String address;

        public DeviceNotFoundException(GBDevice device) {
            this.address = device.getAddress();
        }

        public DeviceNotFoundException(String address) {
            this.address = address;
        }

        @Nullable
        @Override
        public String getMessage() {
            return String.format("device %s not found cached", address);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DeviceCommunicationService.class);

    private DeviceSupportFactory mFactory;
    private final ArrayList<DeviceStruct> deviceStructs = new ArrayList<>(1);
    private final HashMap<String, ArrayList<Intent>> cachedNotifications = new HashMap<>();
    private DeviceReceiversManager deviceReceiversManager;

    private BluetoothConnectReceiver mBlueToothConnectReceiver = null;
    private AutoConnectIntervalReceiver mAutoConnectIntervalReceiver = null;

    private final HashMap<String, Long> deviceLastScannedTimestamps = new HashMap<>();

    private final int NOTIFICATIONS_CACHE_MAX = 10;  // maximum amount of notifications to cache per device while disconnected
    private boolean allowBluetoothIntentApi = false;
    private boolean reconnectViaScan = GBPrefs.RECONNECT_SCAN_DEFAULT;

    private final String API_LEGACY_COMMAND_BLUETOOTH_CONNECT = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_CONNECT";
    private final String API_LEGACY_COMMAND_BLUETOOTH_DISCONNECT = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_DISCONNECT";
    private final String API_LEGACY_ACTION_DEVICE_CONNECTED = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_CONNECTED";
    private final String API_LEGACY_ACTION_DEVICE_SCANNED = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_SCANNED";

    private void sendDeviceAPIBroadcast(String address, String action) {
        if (!allowBluetoothIntentApi) {
            LOG.debug("not sending API event due to settings");
            return;
        }
        Intent intent = new Intent(action);
        intent.putExtra("EXTRA_DEVICE_ADDRESS", address);

        sendBroadcast(intent);
    }

    private void sendDeviceConnectedBroadcast(String address) {
        sendDeviceAPIBroadcast(address, API_LEGACY_ACTION_DEVICE_CONNECTED);
    }

    BroadcastReceiver bluetoothCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!allowBluetoothIntentApi) {
                LOG.error("Connection API not allowed in settings");
                return;
            }
            Bundle extras = intent.getExtras();
            if (extras == null) {
                LOG.error("no extras provided in Intent");
                return;
            }
            final String action = intent.getAction();
            if (action == null) {
                LOG.error("Action for bluetooth command is null");
                return;
            }
            String address = extras.getString("EXTRA_DEVICE_ADDRESS", "");
            if (address.isEmpty()) {
                LOG.error("no bluetooth address provided in Intent");
                return;
            }
            GBDevice targetDevice = GBApplication.app()
                    .getDeviceManager()
                    .getDeviceByAddress(address);

            if (targetDevice == null) {
                LOG.error("device {} not registered", address);
                return;
            }

            switch (action) {
                case API_LEGACY_COMMAND_BLUETOOTH_CONNECT:
                    if (isDeviceConnected(address)) {
                        LOG.info("device {} already connected", address);
                        sendDeviceConnectedBroadcast(address);

                        return;
                    }

                    LOG.info("connecting to {}", address);

                    GBApplication.deviceService(targetDevice).connect();
                    break;
                case API_LEGACY_COMMAND_BLUETOOTH_DISCONNECT:
                    LOG.info("disconnecting from {}", address);

                    GBApplication.deviceService(targetDevice).disconnect();
                    break;
            }
        }
    };

    public static boolean isRunning(final Context context) {
        final ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DeviceCommunicationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public DeviceCommunicationService() {

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (device == null) {
                    // Should never happen
                    LOG.error("Got ACTION_DEVICE_CHANGED without device");
                    return;
                }

                // create a new instance of the changed devices coordinator, in case it's capabilities changed
                DeviceStruct cachedStruct = getDeviceStructOrNull(device);
                if (cachedStruct != null) {
                    cachedStruct.setDevice(device);
                    DeviceCoordinator newCoordinator = device.getDeviceCoordinator();
                    cachedStruct.setCoordinator(newCoordinator);
                }
                updateReceiversState();

                GBDevice.DeviceUpdateSubject subject = (GBDevice.DeviceUpdateSubject) intent.getSerializableExtra(GBDevice.EXTRA_UPDATE_SUBJECT);

                if (subject == GBDevice.DeviceUpdateSubject.DEVICE_STATE && device.isInitialized()) {
                    //Store the time the device was initialized for optional sorting in Control Center
                    GBApplication.getDeviceSpecificSharedPrefs(device.getAddress())
                            .edit()
                            .putLong(GBPrefs.LAST_CONNECTED_TS, System.currentTimeMillis())
                            .apply();
                    sendDeviceConnectedBroadcast(device.getAddress());
                    sendCachedNotifications(device);
                } else if (subject == GBDevice.DeviceUpdateSubject.DEVICE_STATE && (device.getState() == GBDevice.State.SCANNED)) {
                    sendDeviceAPIBroadcast(device.getAddress(), API_LEGACY_ACTION_DEVICE_SCANNED);
                }
            } else if (BLEScanService.EVENT_DEVICE_FOUND.equals(action)) {
                String deviceAddress = intent.getStringExtra(BLEScanService.EXTRA_DEVICE_ADDRESS);

                GBDevice target = GBApplication
                        .app()
                        .getDeviceManager()
                        .getDeviceByAddress(deviceAddress);

                if (target == null) {
                    LOG.error("onReceive: device not found");
                    return;
                }

                if (!target.getDeviceCoordinator().isConnectable()) {
                    int actualRSSI = intent.getIntExtra(BLEScanService.EXTRA_RSSI, 0);
                    Prefs prefs = new Prefs(
                            GBApplication.getDeviceSpecificSharedPrefs(target.getAddress())
                    );
                    long timeoutSeconds = prefs.getLong("devicesetting_scannable_debounce", 60);
                    long minimumUnseenSeconds = prefs.getLong("devicesetting_scannable_unseen", 0);
                    int thresholdRSSI = prefs.getInt("devicesetting_scannable_rssi", -100);

                    if (actualRSSI < thresholdRSSI) {
                        LOG.debug("ignoring {} since RSSI is too low ({} < {})", deviceAddress, actualRSSI, thresholdRSSI);
                        return;
                    }

                    Long lastSeenTimestamp = deviceLastScannedTimestamps.get(deviceAddress);
                    deviceLastScannedTimestamps.put(deviceAddress, System.currentTimeMillis());

                    if (lastSeenTimestamp != null) {
                        long secondsSince = (System.currentTimeMillis() - lastSeenTimestamp) / 1000;
                        if (secondsSince < minimumUnseenSeconds) {
                            LOG.debug("ignoring {}, since only {} seconds passed (< {})", deviceAddress, secondsSince, minimumUnseenSeconds);
                            return;
                        }
                    }

                    target.setUpdateState(GBDevice.State.SCANNED, DeviceCommunicationService.this);
                    new Handler().postDelayed(() -> {
                        if (target.getState() != GBDevice.State.SCANNED) {
                            return;
                        }
                        deviceLastScannedTimestamps.put(target.getAddress(), System.currentTimeMillis());
                        target.setUpdateState(GBDevice.State.WAITING_FOR_SCAN, DeviceCommunicationService.this);
                    }, timeoutSeconds * 1000);
                    return;
                }

                connectToDevice(target, false);
            }
        }
    };

    private void updateReceiversState() {
        boolean enableReceivers = false;
        boolean anyDeviceInitialized = false;
        List<GBDevice> devicesWithCalendar = new ArrayList<>();

        DeviceReceiversManager.FeatureSet features = new DeviceReceiversManager.FeatureSet();

        for (DeviceStruct struct : deviceStructs) {
            final GBDevice device = struct.getDevice();
            DeviceSupport deviceSupport = struct.getDeviceSupport();
            if ((deviceSupport != null && deviceSupport.useAutoConnect()) || isDeviceInitialized(device)) {
                enableReceivers = true;
            }
            if (isDeviceInitialized(device)) {
                anyDeviceInitialized = true;
            }

            DeviceCoordinator coordinator = struct.getCoordinator();
            if (coordinator != null) {
                features.logicalOr(coordinator, device);
                if (coordinator.supportsCalendarEvents(device)) {
                    devicesWithCalendar.add(device);
                }
            }
        }
        deviceReceiversManager.setReceiversEnableState(enableReceivers, anyDeviceInitialized, features, devicesWithCalendar);
    }

    private void registerInternalReceivers() {
        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        localFilter.addAction(BLEScanService.EVENT_DEVICE_FOUND);
        //noinspection deprecation
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, localFilter);
    }

    private void registerExternalReceivers() {
        mBlueToothConnectReceiver = new BluetoothConnectReceiver(this);
        ContextCompat.registerReceiver(this, mBlueToothConnectReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED), ContextCompat.RECEIVER_EXPORTED);

        mAutoConnectIntervalReceiver = new AutoConnectIntervalReceiver(this);
        ContextCompat.registerReceiver(this, mAutoConnectIntervalReceiver, new IntentFilter("GB_RECONNECT"), ContextCompat.RECEIVER_EXPORTED);

        IntentFilter bluetoothCommandFilter = new IntentFilter();
        bluetoothCommandFilter.addAction(API_LEGACY_COMMAND_BLUETOOTH_CONNECT);
        bluetoothCommandFilter.addAction(API_LEGACY_COMMAND_BLUETOOTH_DISCONNECT);
        ContextCompat.registerReceiver(this, bluetoothCommandReceiver, bluetoothCommandFilter, ContextCompat.RECEIVER_EXPORTED);

        deviceReceiversManager.registerReceivers();
    }

    @Override
    public void onCreate() {
        LOG.debug("DeviceCommunicationService is being created");
        super.onCreate();
        mFactory = new DeviceSupportFactory(this);
        deviceReceiversManager = new DeviceReceiversManager(this);

        registerInternalReceivers();
        registerExternalReceivers();

        if (hasPrefs()) {
            getPrefs().getPreferences().registerOnSharedPreferenceChangeListener(this);
            allowBluetoothIntentApi = getPrefs().getBoolean(GBPrefs.PREF_ALLOW_INTENT_API, false);
            reconnectViaScan = getPrefs().getAutoReconnectByScan();
        }

        startForeground();
        if (reconnectViaScan) {
            scanAllDevices();

            Intent scanServiceIntent = new Intent(this, BLEScanService.class);
            startService(scanServiceIntent);
        }
    }

    private void scanAllDevices() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice device : devices) {
            if (!device.getDeviceCoordinator().getConnectionType().usesBluetoothLE()) {
                continue;
            }
            if (device.getState() != GBDevice.State.NOT_CONNECTED) {
                continue;
            }
            boolean shouldAutoConnect = getPrefs().getAutoReconnect(device);
            if (!shouldAutoConnect) {
                continue;
            }
            createDeviceStruct(device);
            device.setUpdateState(GBDevice.State.WAITING_FOR_SCAN, this);
        }
    }

    private DeviceStruct createDeviceStruct(GBDevice target) {
        DeviceStruct registeredStruct = new DeviceStruct();
        registeredStruct.setDevice(target);
        registeredStruct.setCoordinator(target.getDeviceCoordinator());
        deviceStructs.add(registeredStruct);
        return registeredStruct;
    }

    private void connectToDevice(@Nullable final GBDevice device, final boolean firstTime) {
        final List<GBDevice> gbDevs = new ArrayList<>(2);
        final boolean fromExtra;

        final GBPrefs prefs = getPrefs();

        if (device != null) {
            gbDevs.add(device);
            fromExtra = true;
        } else {
            fromExtra = false;
            List<GBDevice> gbAllDevs = GBApplication.app().getDeviceManager().getDevices();

            if (gbAllDevs != null && !gbAllDevs.isEmpty()) {
                if (prefs.getBoolean(GBPrefs.RECONNECT_ONLY_TO_CONNECTED, true)) {
                    Set<String> lastDeviceAddresses = prefs.getStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, Collections.emptySet());

                    if (lastDeviceAddresses != null && !lastDeviceAddresses.isEmpty()) {
                        for (final GBDevice gbDev : gbAllDevs) {
                            // TODO volatile address
                            if (lastDeviceAddresses.contains(gbDev.getAddress())) {
                                gbDevs.add(gbDev);
                            }
                        }
                    }
                } else {
                    gbDevs.addAll(gbAllDevs);
                }
            }
        }

        if (gbDevs.isEmpty()) {
            LOG.warn("No devices to connect to");
            return;
        }

        for (final GBDevice gbDevice : gbDevs) {
            final String deviceAddress = gbDevice.getAddress();

            LOG.debug("Will attempt to connect to {}", gbDevice);

            if (!gbDevice.getDeviceCoordinator().isConnectable()) {
                // we cannot connect to beacons, skip this device
                LOG.debug("connectToDevice - {} isn't connectable", deviceAddress);
                if (fromExtra) {
                    GB.toast("Cannot connect to Scannable Device", Toast.LENGTH_SHORT, GB.INFO);
                }
                continue;
            }

            final boolean autoReconnect;
            if (prefs != null && prefs.getPreferences() != null) {
                autoReconnect = prefs.getAutoReconnect(gbDevice);
                if (!fromExtra && !autoReconnect) {
                    LOG.debug("connectToDevice - {} neither from extra nor auto reconnect (1)",
                            deviceAddress);
                    continue;
                }

                final Set<String> lastDeviceAddresses = new HashSet<>(prefs.getStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, Collections.emptySet()));

                if (!lastDeviceAddresses.contains(deviceAddress)) {
                    lastDeviceAddresses.add(deviceAddress);
                    prefs.getPreferences().edit().putStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, lastDeviceAddresses).apply();
                }
            } else {
                autoReconnect = GBPrefs.AUTO_RECONNECT_DEFAULT;
            }

            if (!fromExtra && !autoReconnect) {
                LOG.debug("connectToDevice - {} neither from extra nor auto reconnect (2)",
                        deviceAddress);
                continue;
            }

            DeviceStruct registeredStruct = getDeviceStructOrNull(gbDevice);
            if (registeredStruct == null) {
                LOG.debug("connectToDevice - {} create new device struct", deviceAddress);
                registeredStruct = createDeviceStruct(gbDevice);
            }

            try {
                DeviceSupport deviceSupport = registeredStruct.getDeviceSupport();

                if (deviceSupport != null) {
                    if (deviceSupport.isConnected()) {
                        LOG.debug("connectToDevice - {} device support is already connected",
                                deviceAddress);
                        continue;
                    }

                    if (deviceSupport.isConnecting()) {
                        LOG.debug("connectToDevice - {} device support is already isConnecting",
                                deviceAddress);
                        continue;
                    }
                }

                if (deviceSupport != null && !deviceSupport.canReconnect()) {
                    try {
                        LOG.debug("connectToDevice - {} dispose device support", deviceAddress);
                        if (BuildConfig.DEBUG) {
                            stressTestDispose(deviceSupport);
                        } else {
                            deviceSupport.dispose();
                        }
                    } catch (final Exception e) {
                        LOG.error("connectToDevice - {} failed to dispose device support",
                                deviceAddress, e);
                    }
                    deviceSupport = null;
                }

                final boolean createSupport = (deviceSupport == null);
                if (createSupport) {
                    LOG.debug("connectToDevice - create new device support for {} ({})", deviceAddress, gbDevice.getType());
                    deviceSupport = mFactory.createDeviceSupport(gbDevice);
                    LOG.debug("connectToDevice - created {} for {}", deviceSupport != null ? deviceSupport.getClass().getSimpleName() : "(null)", deviceAddress);
                    registeredStruct.setDeviceSupport(deviceSupport);
                }

                if (deviceSupport != null) {
                    try {
                        final boolean connected;
                        if (firstTime) {
                            connected = deviceSupport.connectFirstTime();
                        } else {
                            deviceSupport.setAutoReconnect(autoReconnect);
                            deviceSupport.setScanReconnect(reconnectViaScan);
                            if (BuildConfig.DEBUG) {
                                connected = stressTestConnect(deviceSupport);
                            } else {
                                connected = deviceSupport.connect();
                            }
                        }
                        LOG.debug("connectToDevice - {} connected:{} firstTime:{}", deviceAddress,
                                connected, firstTime);
                    } catch (Exception e) {
                        try {
                            deviceSupport.dispose();
                        } catch (Exception ignored) {
                        }
                        registeredStruct.setDeviceSupport(null);
                        throw e;
                    }
                } else {
                    // no device found, check transport availability and warn
                    final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    if (adapter == null) {
                        GB.toast(this, getString(R.string.bluetooth_is_not_supported_), Toast.LENGTH_SHORT, GB.WARN);
                    } else if (!adapter.isEnabled()) {
                        GB.toast(this, getString(R.string.bluetooth_is_disabled_), Toast.LENGTH_SHORT, GB.WARN);
                    } else {
                        GB.toast(this, getString(R.string.cannot_connect, "Can't create device support"), Toast.LENGTH_SHORT, GB.ERROR);
                    }
                }
            } catch (Exception e) {
                LOG.warn("exception in connectToDevice for {}", deviceAddress, e);
                GB.toast(this, getString(R.string.cannot_connect, e.getLocalizedMessage()), Toast.LENGTH_SHORT, GB.ERROR, e);
            }

            registeredStruct.getDevice().sendDeviceUpdateIntent(this);
        }
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            LOG.info("no intent");
            return START_STICKY;
        }

        String action = intent.getAction();

        if (action == null) {
            LOG.info("no action");
            return START_STICKY;
        }

        // when we get past this, we should have valid mDeviceSupport and mGBDevice instances

        GBDevice targetDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);

        LOG.debug("Service startcommand: {}{}", action, targetDevice != null ? " (" + targetDevice.getAddress() + ")" : "");

        switch (action) {
            case ACTION_CONNECT:
                boolean firstTime = intent.getBooleanExtra(EXTRA_CONNECT_FIRST_TIME, false);
                connectToDevice(targetDevice, firstTime);
                break;
            default:
                ArrayList<GBDevice> targetedDevices = new ArrayList<>();
                if (targetDevice != null) {
                    targetedDevices.add(targetDevice);
                } else {
                    for (GBDevice device : getGBDevices()) {
                        if (isDeviceInitialized(device)) {
                            targetedDevices.add(device);
                        } else if (isDeviceReconnecting(device) && action.equals(ACTION_NOTIFICATION) && GBApplication.getPrefs().getBoolean("notification_cache_while_disconnected", false)) {
                            if (!cachedNotifications.containsKey(device.getAddress())) {
                                cachedNotifications.put(device.getAddress(), new ArrayList<>());
                            }
                            ArrayList<Intent> notifCache = cachedNotifications.get(device.getAddress());
                            notifCache.add(intent);
                            if (notifCache.size() > NOTIFICATIONS_CACHE_MAX) {
                                // remove the oldest notification if the maximum is reached
                                notifCache.remove(0);
                            }
                        } else if (action.equals(ACTION_DELETE_NOTIFICATION)) {
                            ArrayList<Intent> notifCache = cachedNotifications.get(device.getAddress());
                            if (notifCache != null) {
                                int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
                                ArrayList<Intent> toRemove = new ArrayList<>();
                                for (Intent cached : notifCache) {
                                    if (notifId == cached.getIntExtra(EXTRA_NOTIFICATION_ID, -1)) {
                                        toRemove.add(cached);
                                    }
                                }
                                notifCache.removeAll(toRemove);
                            }
                        } else if (action.equals(ACTION_DISCONNECT) && device.getState() != GBDevice.State.NOT_CONNECTED) {
                            targetedDevices.add(device);
                        }
                    }
                }
                for (GBDevice device1 : targetedDevices) {
                    try {
                        handleAction(intent, action, device1);
                    } catch (DeviceNotFoundException e) {
                        LOG.warn("exception in onStartCommand", e);
                    } catch (Exception e) {
                        LOG.error("An exception was raised while handling the action {} for the device {}: ", action, device1, e);
                    }
                }
                break;
        }
        return START_STICKY;
    }

    private void handleAction(Intent intent, String action, GBDevice device) throws DeviceNotFoundException {
        if (ACTION_DISCONNECT.equals(intent.getAction())) {
            try {
                removeDeviceSupport(device);
            } catch (DeviceNotFoundException e) {
                LOG.error("Trying to disconnect unknown device: ", e);
            }
            device.setState(GBDevice.State.NOT_CONNECTED);
            device.sendDeviceUpdateIntent(this);
            updateReceiversState();
            return;
        }

        DeviceActionHandler.handle(
                device,
                getDeviceSupport(device),
                this,
                intent,
                action
        );
    }

    private void removeDeviceSupport(GBDevice device) throws DeviceNotFoundException {
        DeviceStruct struct = getDeviceStruct(device);
        DeviceSupport support = struct.getDeviceSupport();
        if (support != null) {
            if (BuildConfig.DEBUG) {
                stressTestDispose(support);
            } else {
                support.dispose();
            }
        }
        struct.setDeviceSupport(null);
    }

    private DeviceStruct getDeviceStructOrNull(GBDevice device) {
        DeviceStruct deviceStruct = null;
        try {
            deviceStruct = getDeviceStruct(device);
        } catch (DeviceNotFoundException e) {
            LOG.warn("exception in getDeviceStructOrNull", e);
        }
        return deviceStruct;
    }

    public DeviceStruct getDeviceStruct(GBDevice device) throws DeviceNotFoundException {
        if (device == null) {
            throw new DeviceNotFoundException("null");
        }
        for (DeviceStruct struct : deviceStructs) {
            if (struct.getDevice().equals(device)) {
                return struct;
            }
        }
        throw new DeviceNotFoundException(device);
    }

    public GBDevice getDeviceByAddress(String deviceAddress) throws DeviceNotFoundException {
        if (deviceAddress == null) {
            throw new DeviceNotFoundException(deviceAddress);
        }
        for (DeviceStruct struct : deviceStructs) {
            if (struct.getDevice().getAddress().equals(deviceAddress)) {
                return struct.getDevice();
            }
        }
        throw new DeviceNotFoundException(deviceAddress);
    }

    public GBDevice getDeviceByAddressOrNull(String deviceAddress) {
        GBDevice device = null;
        try {
            device = getDeviceByAddress(deviceAddress);
        } catch (DeviceNotFoundException e) {
            LOG.warn("exception in getDeviceByAddressOrNull", e);
        }
        return device;
    }

    private DeviceSupport getDeviceSupport(GBDevice device) throws DeviceNotFoundException {
        if (device == null) {
            throw new DeviceNotFoundException("null");
        }
        for (DeviceStruct struct : deviceStructs) {
            if (struct.getDevice().equals(device)) {
                DeviceSupport support = struct.getDeviceSupport();
                if (support == null)
                    throw new DeviceNotFoundException(device);

                return support;
            }
        }
        throw new DeviceNotFoundException(device);
    }

    private void startForeground() {
        GB.createNotificationChannels(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED)
                return;

            ServiceCompat.startForeground(this, GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            ServiceCompat.startForeground(this, GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this), 0);
        }
    }

    private boolean isDeviceConnected(String deviceAddress) {
        for (DeviceStruct struct : deviceStructs) {
            if (struct.getDevice().getAddress().compareToIgnoreCase(deviceAddress) == 0) {
                return struct.getDevice().isConnected();
            }
        }
        return false;
    }

    private boolean isDeviceInitialized(GBDevice device) {
        return isDeviceInitialized(device.getAddress());
    }

    private boolean isDeviceInitialized(String deviceAddress) {
        for (DeviceStruct struct : deviceStructs) {
            if (struct.getDevice().getAddress().compareToIgnoreCase(deviceAddress) == 0) {
                return struct.getDevice().isInitialized();
            }
        }
        return false;
    }

    private boolean isDeviceReconnecting(GBDevice device) {
        if ((device = getDeviceByAddressOrNull(device.getAddress())) != null) {
            return device.getState().equalsOrHigherThan(GBDevice.State.NOT_CONNECTED);
        }
        return false;
    }

    private void sendCachedNotifications(GBDevice device) {
        ArrayList<Intent> notifCache = cachedNotifications.get(device.getAddress());
        if (notifCache == null) return;
        try {
            while (!notifCache.isEmpty()) {
                handleAction(notifCache.remove(0), ACTION_NOTIFICATION, device);
            }
        } catch (DeviceNotFoundException e) {
            LOG.error("Error while sending cached notifications to {}", device.getAliasOrName(), e);
        }
    }

    @Override
    public void onDestroy() {
        if (hasPrefs()) {
            getPrefs().getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        LOG.debug("DeviceCommunicationService is being destroyed");
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        deviceReceiversManager.onDestroy();

        unregisterReceiver(mBlueToothConnectReceiver);
        mBlueToothConnectReceiver = null;

        unregisterReceiver(mAutoConnectIntervalReceiver);
        mAutoConnectIntervalReceiver.destroy();
        mAutoConnectIntervalReceiver = null;

        for (GBDevice device : getGBDevices()) {
            try {
                removeDeviceSupport(device);
            } catch (DeviceNotFoundException e) {
                LOG.warn("exception in onDestroy", e);
            }
        }
        GB.removeNotification(GB.NOTIFICATION_ID, this); // need to do this because the updated notification won't be cancelled when service stops

        unregisterReceiver(bluetoothCommandReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;

        switch (key) {
            case GBPrefs.DEVICE_AUTO_RECONNECT -> {
                for (DeviceStruct deviceStruct : deviceStructs) {
                    boolean autoReconnect = getPrefs().getAutoReconnect(deviceStruct.getDevice());
                    deviceStruct.getDeviceSupport().setAutoReconnect(autoReconnect);
                }
            }
            case GBPrefs.CHART_MAX_HEART_RATE, GBPrefs.CHART_MIN_HEART_RATE ->
                    HeartRateUtils.getInstance().updateCachedHeartRatePreferences();
            case GBPrefs.PREF_ALLOW_INTENT_API -> {
                allowBluetoothIntentApi = sharedPreferences.getBoolean(GBPrefs.PREF_ALLOW_INTENT_API, false);
                LOG.info("allowBluetoothIntentApi changed to {}", allowBluetoothIntentApi);
            }
        }

        deviceReceiversManager.onSharedPreferenceChanged(sharedPreferences, key);
    }

    protected boolean hasPrefs() {
        return getPrefs().getPreferences() != null;
    }

    public GBPrefs getPrefs() {
        return GBApplication.getPrefs();
    }

    public GBDevice[] getGBDevices() {
        GBDevice[] devices = new GBDevice[deviceStructs.size()];
        for (int i = 0; i < devices.length; i++) {
            devices[i] = deviceStructs.get(i).getDevice();
        }
        return devices;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void onTimeout(int startId) {
        LOG.info("onTimeout startId={}", startId);
        super.onTimeout(startId);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public void onTimeout(int startId, int fgsType) {
        LOG.info("onTimeout startId={} fgsType={}", startId, fgsType);
        super.onTimeout(startId, fgsType);
    }

    private static void stressTestDispose(DeviceSupport deviceSupport) {
        SharedPreferences pref = GBApplication.getDeviceSpecificSharedPrefs(deviceSupport.getDevice().getAddress());
        final boolean extra = pref.getBoolean(PREF_DEVICE_STRESS_TEST_DISPOSE, false);

        LOG.debug("stress test - dispose(normal)");
        deviceSupport.dispose();
        if (extra) {
            LOG.debug("stress test - dispose(extra)");
            deviceSupport.dispose();
        }
    }

    private static boolean stressTestConnect(DeviceSupport deviceSupport) {
        SharedPreferences pref = GBApplication.getDeviceSpecificSharedPrefs(deviceSupport.getDevice().getAddress());

        final int extras = pref.getInt(PREF_DEVICE_STRESS_TEST_CONNECT_COUNT, 0);
        final boolean parallel = pref.getBoolean(PREF_DEVICE_STRESS_TEST_CONNECT_PARALLEL, false);

        LOG.debug("stress test - connect() extras:{} parallel:{}", extras, parallel);

        StressTestConnect[] testers = new StressTestConnect[extras + 1];
        for (int i = 0; i < testers.length; i++) {
            testers[i] = new StressTestConnect(i, deviceSupport);
        }

        if (parallel) {
            // parallel calls
            for (int i = 0; i < testers.length; i++) {
                testers[i].start();
            }
            for (int i = testers.length - 1; 0 <= i; i--) {
                // reverse order to increase chances that something "interesting" happens
                try {
                    testers[i].join(0L);
                } catch (final Throwable t) {
                    LOG.debug("stress test - connect(#{}) join exception", i, t);
                }
            }
            LOG.debug("stress test - connect() parallel => {}", testers[0].result);
            return testers[0].result;
        } else {
            // serial calls
            for (int i = 0; i < testers.length; i++) {
                testers[i].run();
            }
            LOG.debug("stress test - connect() serial => {}", testers[0].result);
            return testers[0].result;
        }
    }

    private static class StressTestConnect extends Thread {
        private final int i;
        private final DeviceSupport support;
        boolean result;

        StressTestConnect(int index, DeviceSupport deviceSupport) {
            i = index;
            support = deviceSupport;
        }

        @Override
        public void run() {
            LOG.debug("stress test - connect(#{})", i);
            try {
                result = support.connect();
                LOG.debug("stress test - connect(#{}) => {}", i, result);
            } catch (Throwable t) {
                LOG.error("stress test - connect(#{}) exception", i, t);
            }
        }
    }
}
