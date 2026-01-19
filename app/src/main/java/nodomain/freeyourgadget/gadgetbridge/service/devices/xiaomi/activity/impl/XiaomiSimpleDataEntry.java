/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public class XiaomiSimpleDataEntry {
    private final String key;
    private final String unit;
    private final double multiplier;
    private final Getter getter;

    public XiaomiSimpleDataEntry(final String key, final String unit, final Getter getter) {
        this(key, unit, getter, 1);
    }

    public XiaomiSimpleDataEntry(final String key, final String unit, final Getter getter, final double multiplier) {
        this.key = key;
        this.unit = unit;
        this.getter = getter;
        this.multiplier = multiplier;
    }

    public String getKey() {
        return key;
    }

    public String getUnit() {
        return unit;
    }

    public Number get(final ByteBuffer buf) {
        final Number value = getter.get(buf);
        if (value == null) {
            return value;
        }
        if (multiplier != 1) {
            return value.doubleValue() * multiplier;
        }
        return value;
    }

    public interface Getter {
        Number get(ByteBuffer buf);
    }

    @NonNull
    @Override
    public String toString() {
        return "XiaomiSimpleDataEntry{" +
                "key='" + key + '\'' +
                ", unit='" + unit + '\'' +
                '}';
    }
}
