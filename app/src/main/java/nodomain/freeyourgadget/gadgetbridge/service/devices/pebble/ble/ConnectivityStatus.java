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
 * Parses the Pebble Connectivity characteristic to determine pairing state.
 * Based on libpebble3 ConnectivityWatcher.kt
 *
 * The connectivity characteristic contains status flags indicating the current
 * connection and pairing state from the watch's perspective.
 */
public class ConnectivityStatus {
    public final boolean connected;
    public final boolean paired;
    public final boolean encrypted;
    public final boolean hasBondedGateway;
    public final boolean supportsPinningWithoutSlaveSecurity;
    public final boolean hasRemoteAttemptedToUseStalePairing;
    public final PairingErrorCode pairingErrorCode;

    /**
     * Parse a connectivity characteristic value.
     *
     * @param characteristicValue Raw bytes from the connectivity characteristic.
     *                            Byte 0: Status flags
     *                            Byte 3: Pairing error code (if present)
     */
    public ConnectivityStatus(byte[] characteristicValue) {
        if (characteristicValue == null || characteristicValue.length == 0) {
            // Default to safe values if no data
            connected = false;
            paired = false;
            encrypted = false;
            hasBondedGateway = false;
            supportsPinningWithoutSlaveSecurity = false;
            hasRemoteAttemptedToUseStalePairing = false;
            pairingErrorCode = PairingErrorCode.UNKNOWN_ERROR;
            return;
        }

        byte flags = characteristicValue[0];
        connected = (flags & 0b000001) != 0;
        paired = (flags & 0b000010) != 0;
        encrypted = (flags & 0b000100) != 0;
        hasBondedGateway = (flags & 0b001000) != 0;
        supportsPinningWithoutSlaveSecurity = (flags & 0b010000) != 0;
        hasRemoteAttemptedToUseStalePairing = (flags & 0b100000) != 0;

        if (characteristicValue.length > 3) {
            pairingErrorCode = PairingErrorCode.fromValue(characteristicValue[3]);
        } else {
            pairingErrorCode = PairingErrorCode.NO_ERROR;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "ConnectivityStatus{connected=%b, paired=%b, encrypted=%b, " +
                        "hasBondedGateway=%b, supportsPinning=%b, stalePairing=%b, errorCode=%s}",
                connected, paired, encrypted, hasBondedGateway,
                supportsPinningWithoutSlaveSecurity, hasRemoteAttemptedToUseStalePairing,
                pairingErrorCode);
    }
}
