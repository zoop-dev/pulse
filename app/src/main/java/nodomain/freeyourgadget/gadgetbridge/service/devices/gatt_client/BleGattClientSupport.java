/*  Copyright (C) 2024 Daniel Dakhno

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.gatt_client;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public class BleGattClientSupport extends AbstractBTLESingleDeviceSupport {
    public static final Logger logger = LoggerFactory.getLogger(BleGattClientSupport.class);

    public BleGattClientSupport() {
        super(logger);

        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        if(characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL) && status == BluetoothGatt.GATT_SUCCESS) {
            GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
            batteryInfo.level = value[0];
            handleGBDeviceEvent(batteryInfo);
        }else if(characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING) && status == BluetoothGatt.GATT_SUCCESS) {
            String firmwareVersion = BLETypeConversions.getStringValue(value,0);
            getDevice().setFirmwareVersion(firmwareVersion);
            getDevice().sendDeviceUpdateIntent(getContext());
        }
        return super.onCharacteristicRead(gatt, characteristic, value, status);
    }

    void readCharacteristicIfAvailable(UUID characteristicUUID, TransactionBuilder builder) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(characteristicUUID);
        if(characteristic == null) {
            return;
        }

        logger.debug("found characteristic {}", characteristicUUID);
        builder.read(characteristic);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);

        readCharacteristicIfAvailable(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL, builder);
        readCharacteristicIfAvailable(GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING, builder);

        builder.setDeviceState(GBDevice.State.INITIALIZED);

        return builder;
    }
}
