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
package nodomain.freeyourgadget.gadgetbridge.devices.una

import java.util.UUID

object UnaConstants {
    // File Transfer Service (FTS): a small Adafruit-style pair (service UUID matches Adafruit's
    // real published BLE File Transfer Service, 0xFEBB, but the characteristic-level protocol is
    // UNA's own, not Adafruit's). adaf0002 carries both the request writes and the response
    // notifications for every FTS sub-protocol below; adaf0001 exists but is unused here.
    val UUID_SERVICE_FTS: UUID = UUID.fromString("0000febb-0000-1000-8000-00805f9b34fb")
    val UUID_CHARACTERISTIC_FTS: UUID = UUID.fromString("adaf0002-4669-6c65-5472-616e73666572")

    // Directory listing: 0x50 request, 0x51 streamed response (one notification per entry).
    const val CMD_LIST_DIR: Int = 0x50
    const val RESP_LIST_ENTRY: Int = 0x51

    // Whole-file read: 0x10 request (path + offset) per chunk, 0x11 response. The real app was
    // observed switching to a leaner 0x12 opcode (no path) for chunks after the first, but that's
    // never been directly validated here -- repeating 0x10 with the full path is the form that's
    // actually been tested end-to-end, and the firmware accepts it for every chunk.
    const val CMD_READ: Int = 0x10
    const val RESP_READ_CHUNK: Int = 0x11

    // Largest chunk to request, verified against a watch at MTU 220. The usable size depends on
    // the negotiated MTU, so UnaDeviceSupport derives it per connection.
    const val MAX_READ_CHUNK_SIZE: Int = 200

    // Without this the link can sit on the 23-byte default, roughly tenfold more round trips.
    const val MTU_REQUEST: Int = 247

    // CCS (Custom Command Service): a grab-bag of small phone<->watch commands, multiplexed by
    // leading opcode byte on one characteristic, same pattern as FTS. Only the daily-health request
    // (steps/floors/active-minutes/resting-HR/average-HR for one calendar day) is used here; CCS
    // also reportedly carries find-phone, reset, and firmware-update commands, none of which are
    // implemented or needed by this device support.
    val UUID_SERVICE_CCS: UUID = UUID.fromString("554e4100-a2cf-4df8-0000-7e1e48595106")
    val UUID_CHARACTERISTIC_DAILY_HEALTH: UUID = UUID.fromString("554e4100-a2cf-4df8-0001-7e1e48595106")

    const val CMD_DAILY_HEALTH: Int = 0x10
    // Not a separate opcode -- byte 1 of the response, alongside the same leading opcode byte
    // as the request (see UnaDailyHealthProtocol.parseResponse).
    const val RESP_DAILY_HEALTH: Int = 0x01
}
