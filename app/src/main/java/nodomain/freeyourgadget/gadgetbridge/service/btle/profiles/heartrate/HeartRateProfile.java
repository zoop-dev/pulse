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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;

/**
 * https://www.bluetooth.com/specifications/specs/html/?src=HRS_v1.0/out/en/index-en.html
 *
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
        writeToControlPoint(new byte[]{value}, builder);
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

        final long timestamp = System.currentTimeMillis();

        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        final int flag = buf.get();

        final int heartRate;
        if ((flag & 0x01) != 0) {
            heartRate = buf.getShort() & 0xffff;
        } else {
            heartRate = buf.get() & 0xff;
        }

        final SensorContact sensorContact;
        if ((flag & 0x04) != 0) {
            //  Sensor Contact supported
            if ((flag & 0x02) == 0) {
                // Sensor Contact NOT detected - no or poor contact with the skin
                sensorContact = SensorContact.CONTACT_NOT_DETECTED;
            } else {
                sensorContact = SensorContact.CONTACT_DETECTED;
            }
        } else {
            sensorContact = SensorContact.NOT_SUPPORTED;
        }

        // Energy Expended (UINT16, unit: kilo Joules since last reset)
        final int energyExpended;
        if ((flag & 0x08) != 0) {
            energyExpended = buf.getShort() & 0xffff;
        } else {
            energyExpended = -1;
        }

        // RR-Interval (UINT16 array, unit: 1/1024 second)
        final ArrayList<Integer> rrIntervals = new ArrayList<>();
        if ((flag & 0x10) != 0) {
            while (buf.hasRemaining()) {
                rrIntervals.add(((buf.getShort() & 0xffff) * 1000) / 1024);
            }
        }

        LOG.debug(
                "Got heartRate={}, sensorContact={}, energyExpended={}, rrIntervals={}",
                heartRate,
                sensorContact,
                energyExpended,
                rrIntervals
        );

        notify(createIntent(timestamp, heartRate, sensorContact, energyExpended, rrIntervals));

        return true;
    }

    private Intent createIntent(final long timestamp,
                                final int heartRate,
                                final SensorContact sensorContact,
                                final int energyExpended,
                                final ArrayList<Integer> rrIntervals) {
        final Intent intent = new Intent(ACTION_HEART_RATE);
        intent.putExtra(
                EXTRA_HEART_RATE,
                new HeartRate(
                        timestamp,
                        heartRate,
                        sensorContact,
                        energyExpended,
                        rrIntervals
                )
        );
        return intent;
    }
}
