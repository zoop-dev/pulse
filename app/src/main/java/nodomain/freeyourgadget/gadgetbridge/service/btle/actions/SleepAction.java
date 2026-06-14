/*  Copyright (C) 2015-2026 Carsten Pfeiffer

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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * An action that will cause the queue to {@link Thread#sleep(long) sleep} for the specified time.
 * Note that this is usually a bad idea, since it will not be able to process messages
 * during that time. It is also likely to cause race conditions.
 */
public class SleepAction extends PlainAction {

    private final long mMillis;

    public SleepAction(@IntRange(from = 0L) final long millis) {
        mMillis = millis;
    }

    @Override
    public boolean run(@NonNull final BluetoothGatt gatt) {
        try {
            Thread.sleep(mMillis);
            return true;
        } catch (final InterruptedException e) {
            return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getCreationTime() + " " + getClass().getSimpleName() + " " + mMillis + " ms";
    }
}
