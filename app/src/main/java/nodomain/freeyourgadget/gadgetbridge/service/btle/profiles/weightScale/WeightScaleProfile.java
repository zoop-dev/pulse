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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.weightScale;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/// Supports receiving weights from the standard Bluetooth Weight Scale Service.
/// <ul>
/// <li>org.bluetooth.service.weight_scale / org.bluetooth.characteristic.weight_measurement</li>
/// </ul>
///
/// Weight Scales optionally support {@link #setTime(TransactionBuilder)}.
///
/// @link https://www.bluetooth.com/specifications/specs/weight-scale-service-1-0-1/
/// @see GattService#UUID_SERVICE_WEIGHT_SCALE
/// @see GattCharacteristic#UUID_CHARACTERISTIC_WEIGHT_MEASUREMENT

// TODO also support weight via Body Composition Service:
// org.bluetooth.service.body_composition / org.bluetooth.characteristic.body_composition_measurement
// https://www.bluetooth.com/specifications/specs/body-composition-service-bcs/
// GattService#UUID_SERVICE_BODY_COMPOSITION
public class WeightScaleProfile<T extends AbstractBTLESingleDeviceSupport> extends AbstractBleProfile<T> {
    /// posted whenever a weight is measured
    public static final String ACTION_WEIGHT_MEASUREMENT = BuildConfig.APPLICATION_ID + "_WEIGHT_MEASUREMENT";
    public static final String EXTRA_WEIGHT_MEASUREMENT = "WEIGHT_MEASUREMENT";
    public static final String EXTRA_ADDRESS = "ADDRESS";
    private static final Logger LOG = LoggerFactory.getLogger(WeightScaleProfile.class);

    public WeightScaleProfile(T support) {
        super(support);
    }

    public boolean weightMeasurementIsImperial(int flags) {
        return 0 != (flags & 1);
    }

    @Override
    public void enableNotify(TransactionBuilder builder, boolean enable) {
        builder.notify(GattCharacteristic.UUID_CHARACTERISTIC_WEIGHT_MEASUREMENT, enable);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        return process(characteristic, value);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        return process(characteristic, value);
    }

    private boolean process(BluetoothGattCharacteristic characteristic, byte[] value) {
        UUID uuid = characteristic.getUuid();
        if (GattCharacteristic.UUID_CHARACTERISTIC_WEIGHT_MEASUREMENT.equals(uuid)) {
            return processWeightMeasurement(value);
        }
        return false;
    }

    public boolean processWeightMeasurement(byte[] value) {
        LOG.debug("weight measurement - {}", GB.hexdump(value));
        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() >= 3) {
            WeightScaleMeasurement info = new WeightScaleMeasurement();

            int flags = 0xFF & buffer.get();

            int rawWeight = 0xFFFF & buffer.getShort();
            // 0xFFFF / 65535 is 'measurement unsuccessful'
            if (rawWeight != 0xFFFF) {
                info.setWeightKilogram(decodeWeightMeasurement(rawWeight, flags));
            }

            if (0 == (flags & (1 << 1))) {
                info.setTime(Instant.now());
            } else {
                byte[] rawTimestamp = new byte[7];
                buffer.get(rawTimestamp);
                GregorianCalendar calendar = BLETypeConversions.rawBytesToCalendar(rawTimestamp);
                info.setTime(Instant.ofEpochMilli(calendar.getTimeInMillis()));
            }

            if (0 != (flags & (1 << 2))) {
                int user = 0xFF & buffer.get();
                // 0xFF / 256 is 'unknown user'
                if (user != 0xFF) {
                    info.setUserId(user);
                }
            }

            if (0 != (flags & (1 << 3))) {
                // unitless with a resolution of 0.1
                int rawBmi = 0xFFFF & buffer.getShort();
                info.setBMI((float) (0.1 * rawBmi));

                int rawHeight = 0xFFFF & buffer.getShort();
                if (weightMeasurementIsImperial(flags)) {
                    // inches with a resolution of 0.1
                    info.setHeightMeter((float) (rawHeight * 0.1 * 0.0254));
                } else {
                    // meters with a resolution of 0.001
                    info.setHeightMeter((float) (rawHeight * 0.001));
                }
            }
            LOG.debug("weight measurement - {}", info);
            Intent intent = createIntent(info);
            notify(intent);
        }
        if (buffer.remaining() > 0) {
            LOG.warn("weight measurement - {} undecoded trailing bytes: {}", buffer.remaining(), GB.hexdump(value));
        }
        return true;
    }

    @Nullable
    protected Double decodeWeightMeasurement(int rawWeight, int flags) {
        if (rawWeight == 0xFFFF) {
            // 0xFFFF / 65535 is 'measurement unsuccessful'
            return null;
        }

        if (weightMeasurementIsImperial(flags)) {
            // pounds with a resolution of 0.01
            return rawWeight * 0.01 * 0.45359237;
        } else {
            // kilograms with a resolution of 0.005
            return rawWeight * 0.005;
        }
    }

    public void setTime(@NonNull TransactionBuilder builder) {
        GregorianCalendar now = BLETypeConversions.createCalendar();
        byte[] time = BLETypeConversions.calendarToCurrentTime(now, 0);
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME, time);
    }

    public void readWeight(@NonNull TransactionBuilder builder) {
        builder.read(GattCharacteristic.UUID_CHARACTERISTIC_WEIGHT_MEASUREMENT);
    }

    protected Intent createIntent(final WeightScaleMeasurement measurement) {
        final Intent intent = new Intent(ACTION_WEIGHT_MEASUREMENT);
        intent.putExtra(EXTRA_WEIGHT_MEASUREMENT, measurement);
        return intent;
    }
}
