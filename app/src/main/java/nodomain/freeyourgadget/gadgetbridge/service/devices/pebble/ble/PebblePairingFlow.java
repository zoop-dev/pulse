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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

/**
 * Strategy interface for different Pebble BLE pairing flows.
 * Different Pebble hardware versions require different pairing protocols.
 */
interface PebblePairingFlow {
    /**
     * Start the pairing sequence after service discovery.
     *
     * @param gatt           The GATT connection
     * @param pairingService The Pebble pairing service (0000fed9)
     */
    void startPairing(BluetoothGatt gatt, BluetoothGattService pairingService);

    /**
     * Handle characteristic read callback during pairing.
     *
     * @param gatt           The GATT connection
     * @param characteristic The characteristic that was read
     * @param value          The value that was read
     * @return true if this flow handled the read, false otherwise
     */
    boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value);

    /**
     * Handle characteristic write callback during pairing.
     *
     * @param gatt           The GATT connection
     * @param characteristic The characteristic that was written
     * @param status         The write status (GATT_SUCCESS, etc.)
     * @return true if this flow handled the write, false otherwise
     */
    boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    /**
     * Clean up any resources held by this pairing flow.
     * Called when the GATT client is closed.
     */
    void close();
}
