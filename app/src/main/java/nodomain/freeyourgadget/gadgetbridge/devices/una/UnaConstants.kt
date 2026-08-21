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
    // FTS uses Adafruit's published 0xFEBB service UUID, but UNA's own characteristic numbering
    // and protocol. adaf0002 carries requests and responses for every FTS command; adaf0001 is a
    // uint32 protocol version (4 = classic, 5 = fast-transfer extensions) that nothing here needs.
    // Wire format: UNA's Docs/BLE-File-Transfer-Service.md.
    val UUID_SERVICE_FTS: UUID = UUID.fromString("0000febb-0000-1000-8000-00805f9b34fb")
    val UUID_CHARACTERISTIC_FTS: UUID = UUID.fromString("adaf0002-4669-6c65-5472-616e73666572")

    // Directory listing: 0x50 request, 0x51 streamed response (one notification per entry).
    const val CMD_LIST_DIR: Int = 0x50
    const val RESP_LIST_ENTRY: Int = 0x51

    const val CMD_READ: Int = 0x10
    const val RESP_READ_CHUNK: Int = 0x11
    const val CMD_READ_PACING: Int = 0x12

    const val READ_WINDOW_SIZE: Int = 4096

    // Without this the link can sit on the 23-byte default, roughly tenfold more round trips.
    const val MTU_REQUEST: Int = 247

    // CCS multiplexes small phone/watch commands by leading opcode byte, same pattern as FTS, and
    // is not covered by UNA's published BLE docs. Two commands are used here: daily health for one
    // calendar day, and 60 per-minute heart rates for one hour. CCS also carries find-phone, reset,
    // EPO and firmware update, none of which are implemented.
    //
    // -0001- takes commands and answers them. -0002- is notify-only and carries watch to phone
    // events.
    val UUID_SERVICE_CCS: UUID = UUID.fromString("554e4100-a2cf-4df8-0000-7e1e48595106")
    val UUID_CHARACTERISTIC_CCS_COMMAND: UUID = UUID.fromString("554e4100-a2cf-4df8-0001-7e1e48595106")
    val UUID_CHARACTERISTIC_CCS_EVENT: UUID = UUID.fromString("554e4100-a2cf-4df8-0002-7e1e48595106")

    const val EVENT_ACTIVITY_SAVED: Int = 0x01

    const val CMD_DAILY_HEALTH: Int = 0x10
    const val CMD_HOURLY_HR: Int = 0x14

    // Byte 1 of a CCS response, shared by both commands. Any other value is an error status
    // rather than a payload.
    const val RESP_STATUS_OK: Int = 0x01
}
