/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BleNamesResolver;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class BluetoothStateChangeReceiver extends BroadcastReceiver {
    //the following constants are annotated as systemApi, hence cannot be referenced directly
    //they are reported as working in https://community.home-assistant.io/t/bluetooth-battery-levels-android/661525
    public static final String ANDROID_BLUETOOTH_DEVICE_EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL";
    public static final String ANDROID_BLUETOOTH_DEVICE_ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";
    private static final Logger LOG = LoggerFactory.getLogger(BluetoothStateChangeReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            final int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
            LOG.info("bluetooth adapter state changed from {} to {}",
                    BleNamesResolver.getAdapterStateString(prevState),
                    BleNamesResolver.getAdapterStateString(state));

            if (state == BluetoothAdapter.STATE_ON) {
                final Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);

                final GBPrefs prefs = GBApplication.getPrefs();
                if (!DeviceCommunicationService.isRunning(context) && !prefs.getAutoStart()) {
                    // Prevent starting the service if it isn't yet running
                    LOG.debug("DeviceCommunicationService not running, ignoring bluetooth on");
                    return;
                }

                if (!prefs.getBoolean(GBPrefs.AUTO_CONNECT_BLUETOOTH, false)) {
                    return;
                }

                GBApplication.deviceService().connect();
            } else if (state == BluetoothAdapter.STATE_OFF) {
                if (!DeviceCommunicationService.isRunning(context)) {
                    // Prevent starting the service if it isn't yet running
                    LOG.debug("DeviceCommunicationService not running, ignoring bluetooth off");
                    return;
                }
                GBApplication.deviceService().disconnect();
            }
        } else if (ANDROID_BLUETOOTH_DEVICE_ACTION_BATTERY_LEVEL_CHANGED.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) {
                LOG.error("Got no device for {}", action);
                return;
            }

            final GBDevice gbDevice = GBApplication.app().getDeviceManager().getDeviceByAddress(device.getAddress());
            if (gbDevice == null) {
                LOG.info("Bluetooth device {} unknown", device.getAddress());
                return;
            }

            if (!gbDevice.isConnected()) {
                LOG.info("gbDevice {} not connected, ignoring incoming battery information.", gbDevice);
                return;
            }

            if (!gbDevice.getDeviceCoordinator().supportsOSBatteryLevel(gbDevice)) {
                LOG.info("gbDevice {} does not support OS battery provided levels, ignoring incoming battery information.", gbDevice);
                return;
            }

            final int batteryLevel = intent.getIntExtra(ANDROID_BLUETOOTH_DEVICE_EXTRA_BATTERY_LEVEL, -1);

            final GBDeviceEventBatteryInfo eventBatteryInfo = new GBDeviceEventBatteryInfo();
            eventBatteryInfo.state = batteryLevel == -1 ? BatteryState.UNKNOWN : BatteryState.BATTERY_NORMAL;
            eventBatteryInfo.level = batteryLevel;
            eventBatteryInfo.evaluate(context, gbDevice);
        }
    }
}
