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
import android.companion.CompanionDeviceManager;
import android.companion.CompanionDeviceService;
import android.companion.DevicePresenceEvent;
import android.net.MacAddress;
import android.os.Build;

import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothConnectReceiver;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;


/**
 * Potentially {@link BluetoothConnectReceiver#observedDevice(String) reconnects} observed
 * companion devices. Android API documentation:
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
            LOG.debug("observed device {} via onDeviceAppeared(old {})", address, Build.VERSION.SDK_INT);
            BluetoothConnectReceiver.observedDevice(address);
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
            MacAddress mac = associationInfo.getDeviceMacAddress();
            if (mac != null) {
                String address = mac.toString();
                LOG.debug("observed device {} via onDeviceAppeared(new {})", address, Build.VERSION.SDK_INT);
                BluetoothConnectReceiver.observedDevice(address);
            }
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

        switch (code) {
            case DevicePresenceEvent.EVENT_BLE_APPEARED:
            case DevicePresenceEvent.EVENT_BT_CONNECTED:
            case DevicePresenceEvent.EVENT_SELF_MANAGED_APPEARED:
                CompanionDeviceManager manager = BondingUtil.getCompanionDeviceManager(getBaseContext());
                if (manager == null) {
                    LOG.error("CompanionDeviceManager is null");
                    return;
                }
                for (final AssociationInfo info : manager.getMyAssociations()) {
                    if (info.getId() == event.getAssociationId()) {
                        MacAddress mac = info.getDeviceMacAddress();
                        if (mac != null) {
                            String address = mac.toString();
                            LOG.debug("observed device {} via {}", address, type);
                            BluetoothConnectReceiver.observedDevice(address);
                            return;
                        }
                    }
                }
                LOG.warn("no matching AssociationInfo for {}", event);
                break;
            default:
                LOG.debug("onDevicePresenceEvent {} association:{}", type, event.getAssociationId());
        }
    }
}
