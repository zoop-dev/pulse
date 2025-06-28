/*  Copyright (C) 2016-2025 Andreas Shimokawa, Daniel Dakhno, José Rebelo, Thomas Kuehne

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

import static nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State.WAITING_FOR_RECONNECT;
import static nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State.WAITING_FOR_SCAN;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class BluetoothConnectReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(BluetoothConnectReceiver.class);

    public BluetoothConnectReceiver(final DeviceCommunicationService ignored) {
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (!action.equals(BluetoothDevice.ACTION_ACL_CONNECTED) || !intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
            return;
        }

        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            LOG.error("Got no device for {}", action);
            return;
        }

        final String address = device.getAddress();
        LOG.debug("observed device {} via ACL_CONNECTED", address);

       observedDevice(address);
    }

    public static void observedDevice(String address) {
        final DeviceManager manager = GBApplication.app().getDeviceManager();
        final GBDevice gbDevice = manager.getDeviceByAddress(address);
        if (gbDevice == null) {
            LOG.debug("observed non-GB device {}", address);
            return;
        }

        final GBDevice.State state = gbDevice.getState();
        if (state == WAITING_FOR_RECONNECT || state == WAITING_FOR_SCAN) {
            LOG.debug("re-connecting to observed device due to state {}", state);
        } else {
            final SharedPreferences pref = GBApplication.getDeviceSpecificSharedPrefs(address);
            if (pref == null) {
                LOG.warn("no preferences found for connecting device {}", address);
                return;
            }

            if (pref.getBoolean(GBPrefs.DEVICE_CONNECT_BACK, false)) {
                LOG.debug("re-connecting to observed device due DEVICE_CONNECT_BACK preference");
            } else {
                LOG.info("ignoring observed device {} {}", address, state);
                return;
            }
        }

        GBApplication.deviceService(gbDevice).connect();
    }
}
