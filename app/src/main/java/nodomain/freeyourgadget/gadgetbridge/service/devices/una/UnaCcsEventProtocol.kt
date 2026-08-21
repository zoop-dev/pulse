/*  Copyright (C) 2026 Toby Murray

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.una

import nodomain.freeyourgadget.gadgetbridge.devices.una.UnaConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** [appId] is the id the recording app has in the watch's own `/Apps/app_list.json`. */
data class UnaActivitySaved(val appId: Long) {
    val appIdHex: String get() = String.format("%016X", appId)
}

/** Wire encoding for the events the watch pushes on the CCS event characteristic. */
object UnaCcsEventProtocol {
    private const val ACTIVITY_SAVED_SIZE = 10

    /** Parses one event notification, or null for anything this does not act on. */
    fun parseActivitySaved(data: ByteArray): UnaActivitySaved? {
        if (data.size < ACTIVITY_SAVED_SIZE) return null
        if ((data[0].toInt() and 0xFF) != UnaConstants.EVENT_ACTIVITY_SAVED) return null
        val appId = ByteBuffer.wrap(data, 2, 8).order(ByteOrder.LITTLE_ENDIAN).long
        return UnaActivitySaved(appId)
    }
}
