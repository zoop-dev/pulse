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
package nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands;

import java.lang.Iterable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.Nullable;

public enum AncConfigValue {
    OFF(0x01, "0"),
    TRANSPARENCY(0x02, "2"),
    ON(0x08, "1");
    ;

    private final int code;
    private final String prefId;

    AncConfigValue(final int code, final String prefId) {
        this.code = code;
        this.prefId = prefId;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public static AncConfigValue fromCode(final int code) {
        for (final AncConfigValue param : AncConfigValue.values()) {
            if (param.code == code) {
                return param;
            }
        }

        return null;
    }

    public String getPrefId() {
        return prefId;
    }

    @Nullable
    public static AncConfigValue fromPrefId(final String prefId) {
        for (final AncConfigValue param : AncConfigValue.values()) {
            if (prefId.equals(param.prefId)) {
                return param;
            }
        }

        return null;
    }

    public static int toMask(Iterable<AncConfigValue> modes) {
        int mask = 0;
        for (AncConfigValue mode : modes) {
            mask |= mode.getCode();
        }
        return mask;
    }

    public static EnumSet<AncConfigValue> fromMask(int mask) {
        EnumSet<AncConfigValue> modes = EnumSet.noneOf(AncConfigValue.class);
        for (AncConfigValue mode : AncConfigValue.values()) {
            if ((mask & mode.getCode()) == mode.getCode()) {
                modes.add(mode);
            }
        }

        return modes;
    }

    public static Set<String> toPrefIds(Iterable<AncConfigValue> modes) {
        Set<String> prefIds = new HashSet<>();
        for (AncConfigValue mode : modes) {
            prefIds.add(mode.getPrefId());
        }
        return prefIds;
    }

    public static EnumSet<AncConfigValue> fromPrefIds(Iterable<String> prefIds) {
        EnumSet<AncConfigValue> modes = EnumSet.noneOf(AncConfigValue.class);
        for (String prefId : prefIds) {
            AncConfigValue mode = fromPrefId(prefId);
            if (mode != null) {
                modes.add(mode);
            }
        }
        return modes;
    }
}
