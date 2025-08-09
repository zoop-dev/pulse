/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.ValueDecoder;

public class BatteryInfoProfile<T extends AbstractBTLESingleDeviceSupport> extends AbstractBleProfile {
    private static final Logger LOG = LoggerFactory.getLogger(BatteryInfoProfile.class);

    private static final String ACTION_PREFIX = BatteryInfoProfile.class.getName() + "_";

    public static final String ACTION_BATTERY_INFO = ACTION_PREFIX + "BATTERY_INFO";
    public static final String EXTRA_BATTERY_INFO = "BATTERY_INFO";

    public static final UUID SERVICE_UUID = GattService.UUID_SERVICE_BATTERY_SERVICE;

    public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL = GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL;
    private final BatteryInfo batteryInfo = new BatteryInfo();

    public BatteryInfoProfile(T support) {
        super(support);
    }

    public void requestBatteryInfo(TransactionBuilder builder) {
        builder.read(UUID_CHARACTERISTIC_BATTERY_LEVEL);
    }

    @Override
    public void enableNotify(TransactionBuilder builder, boolean enable) {
        builder.notify(UUID_CHARACTERISTIC_BATTERY_LEVEL, enable);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        UUID charUuid = characteristic.getUuid();
        if (charUuid.equals(UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleBatteryLevel(gatt, characteristic, value);
                return true;
            } else {
                LOG.warn("error reading characteristic: {}", GattCharacteristic.toString(characteristic));
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        return onCharacteristicRead(gatt, characteristic, value, BluetoothGatt.GATT_SUCCESS);
    }

    private void handleBatteryLevel(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        int percent = ValueDecoder.decodePercent(characteristic, value);
        batteryInfo.setPercentCharged(percent);

        notify(createIntent(batteryInfo));
    }

    private Intent createIntent(BatteryInfo batteryInfo) {
        Intent intent = new Intent(ACTION_BATTERY_INFO);
        intent.putExtra(EXTRA_BATTERY_INFO, batteryInfo);
        return intent;
    }
}
