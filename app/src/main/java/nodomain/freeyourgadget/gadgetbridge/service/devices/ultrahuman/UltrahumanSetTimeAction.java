/*  Copyright (C) 2025  Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

/// calculate date/time on the fly to avoid setting an outdated value
class UltrahumanSetTimeAction extends WriteAction {
    UltrahumanSetTimeAction(BluetoothGattCharacteristic characteristic) {
        super(characteristic, null);
    }

    @Override
    protected boolean writeValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        final Calendar calendar = DateTimeUtils.getCalendarUTC();
        final long millis = calendar.getTimeInMillis();
        final long epoc = Math.round(millis / 1000.0d);

        byte[] command = new byte[]{
                UltrahumanConstants.OPERATION_SETTIME,
                (byte) (epoc & 0xff),
                (byte) ((epoc >> 8) & 0xff),
                (byte) ((epoc >> 16) & 0xff),
                (byte) ((epoc >> 24) & 0xff)
        };
        return super.writeValue(gatt, characteristic, command);
    }
}
