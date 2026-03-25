/*  Copyright (C) 2024 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble;

/**
 * Pairing error codes from the Pebble Connectivity characteristic.
 * Based on libpebble3 PairingErrorCode enum.
 */
public enum PairingErrorCode {
    NO_ERROR((byte) 0),
    PASSKEY_ENTRY_FAILED((byte) 1),
    OOB_NOT_AVAILABLE((byte) 2),
    AUTHENTICATION_REQUIREMENTS((byte) 3),
    CONFIRM_VALUE_FAILED((byte) 4),
    PAIRING_NOT_SUPPORTED((byte) 5),
    ENCRYPTION_KEY_SIZE((byte) 6),
    COMMAND_NOT_SUPPORTED((byte) 7),
    UNSPECIFIED_REASON((byte) 8),
    REPEATED_ATTEMPTS((byte) 9),
    INVALID_PARAMETERS((byte) 10),
    DHKEY_CHECK_FAILED((byte) 11),
    NUMERIC_COMPARISON_FAILED((byte) 12),
    BR_EDR_PAIRING_IN_PROGRESS((byte) 13),
    CROSS_TRANSPORT_KEY_DERIVATION_NOT_ALLOWED((byte) 14),
    UNKNOWN_ERROR((byte) -1);

    private final byte value;

    PairingErrorCode(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static PairingErrorCode fromValue(byte value) {
        for (PairingErrorCode code : values()) {
            if (code.value == value) {
                return code;
            }
        }
        return UNKNOWN_ERROR;
    }
}
