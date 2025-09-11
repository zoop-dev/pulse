/*  Copyright (C) 2016-2025 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.heart_rate.xml
 * @see GattService#UUID_SERVICE_HEART_RATE
 */
public class HeartRateProfile<T extends AbstractBTLESingleDeviceSupport> extends AbstractBleProfile<T> {
    private static final Logger LOG = LoggerFactory.getLogger(HeartRateProfile.class);

    private static final String ACTION_PREFIX = HeartRateProfile.class.getName() + "_";
    public static final String ACTION_HEART_RATE = ACTION_PREFIX + "HEART_RATE";
    public static final String EXTRA_HEART_RATE = "HEART_RATE";

    /**
     * Returned when a request to the heart rate control point is not supported by the device
     */
    public static final int ERR_CONTROL_POINT_NOT_SUPPORTED = 0x80;

    public HeartRateProfile(T support) {
        super(support);
    }

    public void resetEnergyExpended(TransactionBuilder builder) {
        writeToControlPoint((byte) 0x01, builder);
    }

    protected void writeToControlPoint(byte value, TransactionBuilder builder) {
        writeToControlPoint(new byte[] { value }, builder);
    }

    protected void writeToControlPoint(byte[] value, TransactionBuilder builder) {
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, value);
    }

    public void requestBodySensorLocation(TransactionBuilder builder) {

    }

    @Override
    public void enableNotify(TransactionBuilder builder, boolean enable) {
        builder.notify(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT, enable);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            return false;
        }

        final int flag = value[0];
        final int heartRate;
        if ((flag & 0x01) != 0) {
            heartRate = BLETypeConversions.toUint16(value, 1);
        } else {
            heartRate = BLETypeConversions.toUnsigned(value, 1);
        }

        if ((flag & 0x04) != 0){
            //  Sensor Contact supported
            if ((flag & 0x02) == 0){
                // Sensor Contact NOT detected - no or poor contact with the skin
                LOG.debug("Got poor contact heartRate: {}", heartRate);
                return true;
            }
        }
        if ((flag & 0x08) != 0){
            // TODO: Energy Expended present (UINT16, unit: kilo Joules since last reset)
        }
        if ((flag & 0x10) != 0){
            // TODO: RR-Interval present (UINT16 array, unit: 1/1024 second)
        }

        LOG.debug("Got heartRate: {}", heartRate);

        notify(createIntent(heartRate));

        return true;
    }

    private Intent createIntent(final int heartRate) {
        final Intent intent = new Intent(ACTION_HEART_RATE);
        intent.putExtra(EXTRA_HEART_RATE, heartRate);
        return intent;
    }
}
