/*  Copyright (C) 2026 Ken Blizzard-Caron

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
package nodomain.freeyourgadget.gadgetbridge.devices.ollee

import java.util.UUID

object OlleeConstants {
    // Nordic UART Service
    val UUID_SERVICE_NUS: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    /** app -> watch writes */
    val UUID_CHARACTERISTIC_RX: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    /** watch -> app notifies */
    val UUID_CHARACTERISTIC_TX: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

    const val RESPONSE_TARGET_OFFSET: Int = 0x20

    const val TARGET_SET_CLOCK: Int = 0x23
    const val TARGET_ALARM: Int = 0x25
    const val TARGET_GET_ALARM: Int = 0x2B
    const val TARGET_VERSION: Int = 0x2A
    const val TARGET_WEEKDAYS: Int = 0x34
    const val TARGET_GET_WEEKDAYS: Int = 0x35
    const val TARGET_SET_FACES: Int = 0x36
    const val TARGET_GET_FACES: Int = 0x37

    // Activity records drain
    const val TARGET_ACTIVITY_COUNT: Int = 0x27
    const val TARGET_ACTIVITY_RECORD: Int = 0x28
    const val TARGET_ACTIVITY_COMMIT: Int = 0x2D

    /** Trailing big-endian uint16 millivolt field inside the 0x4A version reply. */
    const val VERSION_REPLY_VOLTAGE_OFFSET: Int = 34

    /** World Time face record ID in the 02 36/37 faces table. */
    const val FACE_ID_WORLD_TIME: Int = 0x06
}
