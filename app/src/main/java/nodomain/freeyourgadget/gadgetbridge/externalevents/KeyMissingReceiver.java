/*  Copyright (C) 2025 Thomas Kuehne

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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BleNamesResolver;

/// Log why a previously bonded device couldn't provide keys to establish encryption
/// on API 36.1 and higher. On lower API levels this broadcast usually requires
/// Manifest.permission.BLUETOOTH_PRIVILEGED to receive.
public class KeyMissingReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(KeyMissingReceiver.class);

    public static final String ACTION_KEY_MISSING = "android.bluetooth.device.action.KEY_MISSING";
    public static final String EXTRA_BOND_LOSS_REASON = "android.bluetooth.device.extra.BOND_LOSS_REASON";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null){
            LOG.warn("context is null");
        }else if (intent == null){
            LOG.warn("intent is null");
        } else if (ACTION_KEY_MISSING.equals(intent.getAction())) {
            Parcelable device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int reason = intent.getIntExtra(EXTRA_BOND_LOSS_REASON, -1);

            LOG.error("Bluetooth bond failed: KEY_MISSING {} {}",
                    BleNamesResolver.getBondLossReasonString(reason),
                    device);
        } else {
            LOG.warn("received unexpected broadcast action '{}'", intent.getAction());
        }
    }
}
