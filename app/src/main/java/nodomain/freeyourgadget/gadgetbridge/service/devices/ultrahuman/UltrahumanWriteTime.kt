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
package nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman

import android.bluetooth.BluetoothGattCharacteristic
import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ConditionalWriteAction
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils

/** calculate date/time on the fly to avoid setting an outdated value */
class UltrahumanWriteTime(characteristic: BluetoothGattCharacteristic) :
    ConditionalWriteAction(characteristic) {
    override fun checkCondition(): ByteArray {
        val epoc: Long = DateTimeUtils.getEpochSeconds()

        val value = byteArrayOf(
            UltrahumanConstants.OPERATION_SETTIME,
            (epoc and 0xff).toByte(),
            ((epoc shr 8) and 0xff).toByte(),
            ((epoc shr 16) and 0xff).toByte(),
            ((epoc shr 24) and 0xff).toByte()
        )
        return value
    }
}