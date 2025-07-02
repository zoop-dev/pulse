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

package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCallback;


/// Calls {@link BluetoothGatt#setPreferredPhy(int, int, int)}.  The result will be made available
/// asynchronously through {@link GattCallback#onPhyUpdate(BluetoothGatt, int, int, int)}
@RequiresApi(api = Build.VERSION_CODES.O)
public class SetPreferredPhyAction extends BtLEAction {
    private static final Logger LOG = LoggerFactory.getLogger(SetPreferredPhyAction.class);

    private final int mTxPhy;
    private final int mRxPhy;
    private final int mPhyOptions;

    public SetPreferredPhyAction(int txPhy, int rxPhy, int phyOptions) {
        super(null);
        mTxPhy = txPhy;
        mRxPhy = rxPhy;
        mPhyOptions = phyOptions;
    }

    /// {@link GattCallback#onPhyUpdate(BluetoothGatt, int, int, int)} is also triggered by device
    /// activity so can't use {@link BtLEQueue#mWaitForActionResultLatch} to wait for results
    @Override
    public boolean expectsResult() {
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean run(BluetoothGatt gatt) {
        try {
            gatt.setPreferredPhy(mTxPhy, mRxPhy, mPhyOptions);
            return true;
        } catch (final Throwable ex) {
            LOG.warn("BluetoothGatt.setPreferredPhy({}, {}, {}) failed", mTxPhy, mRxPhy,
                    mPhyOptions, ex);
            return false;
        }
    }

    @Override
    public String toString() {
        return getCreationTime() + " " + getClass().getSimpleName() + " tx=" + mTxPhy
                + " rx=" + mRxPhy + " opt=" + mPhyOptions;
    }
}
