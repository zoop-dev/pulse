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
}
