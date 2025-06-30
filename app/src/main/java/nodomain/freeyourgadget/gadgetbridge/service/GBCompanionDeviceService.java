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

package nodomain.freeyourgadget.gadgetbridge.service;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceService;
import android.companion.DevicePresenceEvent;
import android.os.Build;

import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothConnectReceiver;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.AutoConnectIntervalReceiver;


/**
 * For now this service only ensures that GB is less likely to get killed.
 * See {@link #maybeConnect}. Android API documentation:
 * <blockquote>
 * The system binding CompanionDeviceService elevates the priority of the process that the service
 * is running in, and thus may prevent the Low-memory killer from killing the process at expense of
 * other processes with lower priority.
 * </blockquote>
 */
@RequiresApi(Build.VERSION_CODES.S)
public class GBCompanionDeviceService extends CompanionDeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(GBCompanionDeviceService.class);

    @DeprecatedSinceApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onDeviceAppeared(@NonNull String address) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            LOG.debug("onDeviceAppeared address:{}", address);
            maybeConnect();
        }
    }

    @DeprecatedSinceApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onDeviceDisappeared(@NonNull String address) {
        // nop - the super is abstract in older versions
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @DeprecatedSinceApi(api = Build.VERSION_CODES.BAKLAVA)
    @Override
    public void onDeviceAppeared(@NonNull AssociationInfo associationInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            LOG.debug("onDeviceAppeared association:{}", associationInfo.getDeviceMacAddress());
            maybeConnect();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @DeprecatedSinceApi(api = Build.VERSION_CODES.BAKLAVA)
    @Override
    public void onDeviceDisappeared(@NonNull AssociationInfo associationInfo) {
        // nop - the super is abstract in older versions
    }

    @RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
    @Override
    public void onDevicePresenceEvent(@NonNull DevicePresenceEvent event) {
        final int code = event.getEvent();

        final String type = switch (code) {
            case DevicePresenceEvent.EVENT_BLE_APPEARED -> "EVENT_BLE_APPEARED";
            case DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> "EVENT_BLE_DISAPPEARED";
            case DevicePresenceEvent.EVENT_BT_CONNECTED -> "EVENT_BT_CONNECTED";
            case DevicePresenceEvent.EVENT_BT_DISCONNECTED -> "EVENT_BT_DISCONNECTED";
            case DevicePresenceEvent.EVENT_SELF_MANAGED_APPEARED -> "EVENT_SELF_MANAGED_APPEARED";
            case DevicePresenceEvent.EVENT_SELF_MANAGED_DISAPPEARED ->
                    "EVENT_SELF_MANAGED_DISAPPEARED";
            default -> Integer.toString(code);
        };
        LOG.debug("onDevicePresenceEvent {} association:{}", type, event.getAssociationId());

        switch (code) {
            case DevicePresenceEvent.EVENT_BLE_APPEARED:
            case DevicePresenceEvent.EVENT_BT_CONNECTED:
            case DevicePresenceEvent.EVENT_SELF_MANAGED_APPEARED:
                maybeConnect();
                break;
        }
    }

    /**
     * FIXME {@link DeviceCommunicationService#connectToDevice} has to play nicer with
     *  concurrent reconnect triggers before code can be added here
     *  <ol>
     *  <li>{@link AutoConnectIntervalReceiver}</li>
     *  <li>{@link BluetoothConnectReceiver}</li>
     *  <li>{@link DeviceService#ACTION_CONNECT}</li>
     *  <li>device specific - e.g. {@link BtLEQueue#maybeReconnect}'s {@code mBluetoothGatt.connect}</li>
     *  </ol>
     */
    private void maybeConnect() {
        // TODO check DEVICE_CONNECT_BACK, WAITING_FOR_RECONNECT and WAITING_FOR_SCAN
        //  and conditionally try to establish a connection
    }
}
