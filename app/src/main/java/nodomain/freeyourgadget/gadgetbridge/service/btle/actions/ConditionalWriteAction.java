/*  Copyright (C) 2016-2025 Andreas Shimokawa, Carsten Pfeiffer, Thomas Kuehne

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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public abstract class ConditionalWriteAction extends WriteAction {
    private boolean actualGenerated;
    private byte[] actual;

    public ConditionalWriteAction(BluetoothGattCharacteristic characteristic) {
        super(characteristic, null);
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        byte[] value = getValue();
        if (value != null) {
            return super.run(gatt);
        }
        return true;
    }

    @Override
    public final byte[] getValue() {
        if (!actualGenerated) {
            actual = checkCondition();
            actualGenerated = true;
        }
        return actual;
    }

    /**
     * Checks the condition whether the write shall happen or not.
     * Returns the actual value to be written or null in case nothing shall be written.
     * <p/>
     * Note that returning null will not cause {@link #run()} to return false, in other words,
     * the rest of the queue will still be executed.
     *
     * @return the value to be written or null to not write anything
     */
    protected abstract byte[] checkCondition();
}
