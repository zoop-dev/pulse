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

package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;

public abstract class AbstractBTLEDeviceSupport extends AbstractDeviceSupport
        implements GattCallback, GattServerCallback {
    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb";

    /**
     * Returns whether the gatt callback should be implicitly set to the one on the transaction,
     * even if it was not set directly on the transaction. If true, the gatt callback will always
     * be set to the one in the transaction, even if null and not explicitly set to null.
     * See <a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/2912">#2912</a> for
     * more information. This is false by default, but we are making it configurable to avoid breaking
     * older devices that rely on this behavior, so all older devices got this overridden to true.
     */
    public boolean getImplicitCallbackModify() {
        return false;
    }

    /**
     * Whether to send a write request response to the device, if requested. The standard actually
     * expects this to happen, but Gadgetbridge did not originally support it. This is set to true
     * on all older devices that were not confirmed to handle the response well after this was introduced.
     * <p>
     * See also: <a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/2831#issuecomment-941568">#2831#issuecomment-941568</a>
     *
     * @return whether to send write request responses, if a response is requested
     */
    public boolean getSendWriteRequestResponse() {
        return true;
    }

    /**
     * {@link BluetoothProfile#STATE_CONNECTED} is posted after the connection has been opened but
     * BEFORE encryption was established and Service Changed Indication processed.
     * Android will NOT notify the application when that is completed. GB has to wait before calling
     * {@link BluetoothGatt#getServices} and {@link BluetoothGatt#discoverServices} or risk
     * outdated services from Android or connection termination due to concurrent GATT requests.
     * <p>
     * The general Android implementation changed significantly at least in Android 8 and 15.
     * Some vendors also heavily customize Bluetooth so use a longer, conservative delay.
     *
     * @return milliseconds
     */
    public long getServiceDiscoveryDelay(boolean bonded) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return bonded ? 1600L : 300L;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return 300L;
        } else {
            return bonded ? 1000L : 300L;
        }
    }

    abstract BtLEQueue getQueue(int deviceIdx);

    @Nullable
    abstract BluetoothGattCharacteristic getCharacteristic(UUID uuid, int deviceIdx);

    abstract int getMTU(int deviceIdx);

    /// the maximum payload length supported for one write action
    @IntRange(from = 20L, to = 512L)
    public static int calcMaxWriteChunk(int mtu) {
        // the minimum MTU is 23 (Bluetooth spec)
        int safeMtu = Math.max(23, mtu);

        // GATT_MAX_ATTR_LEN: no larger than 512 (Bluetooth spec)
        // MTU: overhead of simple write must be supported. Some other operations like
        //      ATT_PREPARE_WRITE_REQ have even larger overhead so the max BLE MTU is larger than 512+3
        return Math.min(512, safeMtu - 3);
    }
}