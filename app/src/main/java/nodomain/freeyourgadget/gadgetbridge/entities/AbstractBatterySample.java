/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.entities;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public abstract class AbstractBatterySample {
    /// Unix timestamp in milliseconds
    public abstract long getTimestamp();

    public abstract void setTimestamp(long timestamp);

    public abstract long getDeviceId();

    public abstract void setDeviceId(long deviceId);

    public abstract void setDevice(final Device device);

    public abstract int getBatteryIndex();

    public abstract void setBatteryIndex(int batteryIndex);

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "timestamp=" + DateTimeUtils.formatDateTime(DateTimeUtils.parseTimestampMillis(getTimestamp())) +
                ", deviceId=" + getDeviceId() +
                ", batteryIndex=" + getBatteryIndex() +
                "}";
    }
}
