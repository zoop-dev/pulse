/*  Copyright (C) 2024-2025 Daniel Dakhno, José Rebelo, LLan, Thomas Kuehne

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

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREFS_KEY_DEVICE_BLE_API_CHARACTERISTIC;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREFS_KEY_DEVICE_BLE_API_DEVICE_NOTIFY;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREFS_KEY_DEVICE_BLE_API_DEVICE_READ_WRITE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREFS_KEY_DEVICE_BLE_API_DEVICE_STATE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREFS_KEY_DEVICE_BLE_API_PACKAGE;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class BleIntentApi {
    final Logger logger;

    private boolean intentApiEnabledDeviceState = false;
    private boolean intentApiEnabledReadWrite= false;
    private boolean intentApiEnabledNotifications= false;
    private List<String> intentApiCharacteristicFilter = new ArrayList<>();
    private String intentApiPackage = "";
    private boolean intentApiCharacteristicReceiverRegistered = false;
    private boolean intentApiDeviceStateReceiverRegistered = false;
    private String lastReportedState = null;

    private final HashMap<String, BluetoothGattCharacteristic> characteristics = new HashMap<>();

    public static final String BLE_API_COMMAND_READ = "nodomain.freeyourgadget.gadgetbridge.ble_api.commands.CHARACTERISTIC_READ";
    public static final String BLE_API_COMMAND_WRITE = "nodomain.freeyourgadget.gadgetbridge.ble_api.commands.CHARACTERISTIC_WRITE";
    public static final String BLE_API_EVENT_CHARACTERISTIC_CHANGED = "nodomain.freeyourgadget.gadgetbridge.ble_api.events.CHARACTERISTIC_CHANGED";

    private final AbstractBTLEDeviceSupport deviceSupport;
    private final int deviceIdx;

    BroadcastReceiver intentApiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            boolean isWrite = BLE_API_COMMAND_WRITE.equals(action);

            boolean isRead = BLE_API_COMMAND_READ.equals(action);

            if((!isWrite) && (!isRead)) {
                return;
            }

            if (!concernsThisDevice(intent)) {
                return;
            }

            if(!getDevice().getState().equalsOrHigherThan(GBDevice.State.INITIALIZED)) {
                logger.error(String.format("BLE API: Device %s not initialized.", getDevice()));
                return;
            }

            String uuid = intent.getStringExtra("EXTRA_CHARACTERISTIC_UUID");
            if (StringUtils.isNullOrEmpty(uuid)) {
                logger.error("BLE API: missing EXTRA_CHARACTERISTIC_UUID");
                return;
            }

            String hexData = intent.getStringExtra("EXTRA_PAYLOAD");
            if (isWrite && (hexData == null)) {
                logger.error("BLE API: missing EXTRA_PAYLOAD");
                return;
            }

            BluetoothGattCharacteristic characteristic = characteristics.get(uuid);

            if(characteristic == null) {
                logger.error("Characteristic {} not found", uuid);
                return;
            }

            if(isWrite) {
                new TransactionBuilder("BLE API write", deviceSupport, deviceIdx)
                        .write(characteristic, StringUtils.hexToBytes(hexData))
                        .queue();
                return;
            }

            if(isRead) {
                new TransactionBuilder("BLE API read", deviceSupport, deviceIdx)
                        .read(characteristic)
                        .queue();
                return;
            }
        }
    };

    public static boolean isEnabled(GBDevice device) {
        Prefs devicePrefs = GBApplication.getDevicePrefs(device);

        boolean intentApiEnabledReadWrite = devicePrefs.getBoolean(PREFS_KEY_DEVICE_BLE_API_DEVICE_READ_WRITE, false);
        boolean intentApiEnabledNotifications = devicePrefs.getBoolean(PREFS_KEY_DEVICE_BLE_API_DEVICE_NOTIFY, false);
        boolean intentApiEnabledDeviceState = devicePrefs.getBoolean(PREFS_KEY_DEVICE_BLE_API_DEVICE_STATE, false);

        return intentApiEnabledReadWrite | intentApiEnabledNotifications | intentApiEnabledDeviceState;
    }

    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value) {
        if(!intentApiEnabledNotifications) {
            return;
        }
        if(!intentApiCharacteristicFilter.isEmpty() && !intentApiCharacteristicFilter.contains(characteristic.getUuid().toString())) {
            return;
        }
        Intent intent = getBleApiIntent(BLE_API_EVENT_CHARACTERISTIC_CHANGED);
        if(!StringUtils.isNullOrEmpty(intentApiPackage)) {
            intent.setPackage(intentApiPackage);
        }
        intent.putExtra("EXTRA_CHARACTERISTIC", characteristic.getUuid().toString());
        intent.putExtra("EXTRA_PAYLOAD", StringUtils.bytesToHex(value));

        getContext().sendBroadcast(intent);
    }

    public void initializeDevice(TransactionBuilder builder) {
        if(intentApiEnabledNotifications) {
            for (BluetoothGattCharacteristic characteristic : characteristics.values()) {
                if (intentApiCharacteristicFilter.isEmpty()) {
                    builder.notify(characteristic, true);
                    logger.info("Subscribed to {}", characteristic.getUuid());
                }
                else if(intentApiCharacteristicFilter.contains(characteristic.getUuid().toString())) {
                    builder.notify(characteristic, true);
                    logger.info("Subscribed to filtered {}", characteristic.getUuid());
                }
            }
        }
    }

    public void dispose() {
        registerBleApiCharacteristicReceivers(false);
    }

    public void onSendConfiguration(String config) {
        if(StringUtils.isNullOrEmpty(config)) {
            return;
        }
        if(config.startsWith("prefs_device_ble_api_")) {
            // could subscribe here, but there is more setup to do than that...
            // handleBLEApiPrefs();
            GB.toast(
                    getContext().getString(R.string.toast_setting_requires_reconnect),
                    Toast.LENGTH_SHORT,
                    GB.INFO
            );
        };
    }

    public Context getContext() {
        return deviceSupport.getContext();
    }

    public BtLEQueue getQueue() {
        return deviceSupport.getQueue(deviceIdx);
    }

    private void registerBleApiCharacteristicReceivers(boolean enable){
        if(enable == intentApiCharacteristicReceiverRegistered) {
            return;
        }

        if(enable){
            IntentFilter filter = new IntentFilter();
            filter.addAction(BLE_API_COMMAND_READ);
            filter.addAction(BLE_API_COMMAND_WRITE);

            ContextCompat.registerReceiver(
                    getContext(),
                    intentApiReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
            );
        }else{
            getContext().unregisterReceiver(intentApiReceiver);
        }
        intentApiCharacteristicReceiverRegistered = intentApiEnabledReadWrite;
    }

    public void addService(BluetoothGattService service) {
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            this.characteristics.put(characteristic.getUuid().toString(), characteristic);
        }
    }

    public void handleBLEApiPrefs(){
        Prefs devicePrefs = GBApplication.getDevicePrefs(getDevice());
        this.intentApiEnabledReadWrite = devicePrefs.getBoolean(PREFS_KEY_DEVICE_BLE_API_DEVICE_READ_WRITE, false);
        this.intentApiEnabledNotifications = devicePrefs.getBoolean(PREFS_KEY_DEVICE_BLE_API_DEVICE_NOTIFY, false);
        this.intentApiEnabledDeviceState = devicePrefs.getBoolean(PREFS_KEY_DEVICE_BLE_API_DEVICE_STATE, false);
        // Also trim whitespace from the list of UUIDs
        this.intentApiCharacteristicFilter = devicePrefs.getList(PREFS_KEY_DEVICE_BLE_API_CHARACTERISTIC, Collections.emptyList()).stream().map(String::trim).collect(Collectors.toList());
        this.intentApiPackage = devicePrefs.getString(PREFS_KEY_DEVICE_BLE_API_PACKAGE, "");

        registerBleApiCharacteristicReceivers(this.intentApiEnabledReadWrite);
    }

    public static Intent getBleApiIntent(String deviceAddress, String action) {
        Intent updateIntent = new Intent(action);
        updateIntent.putExtra("EXTRA_DEVICE_ADDRESS", deviceAddress);
        return updateIntent;
    }

    private Intent getBleApiIntent(String action) {
        return getBleApiIntent(getDevice().getAddress(), action);
    }

    public BleIntentApi(AbstractBTLEDeviceSupport deviceSupport, int deviceIdx) {
        this.deviceSupport = deviceSupport;
        this.deviceIdx = deviceIdx;
        this.logger = LoggerFactory.getLogger(BleIntentApi.class);
    }

    public GBDevice getDevice() {
        return deviceSupport.getDevice();
    }

    private boolean concernsThisDevice(Intent intent) {
        String deviceAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS");
        if (StringUtils.isNullOrEmpty(deviceAddress)) {
            logger.error("BLE API: missing EXTRA_DEVICE_ADDRESS");
            return false;
        }
        return deviceAddress.equalsIgnoreCase(getDevice().getAddress());
    }
}
