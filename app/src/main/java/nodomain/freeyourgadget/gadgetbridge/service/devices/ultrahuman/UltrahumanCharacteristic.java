/*  Copyright (C) 2025  Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants;

public enum UltrahumanCharacteristic {
    COMMAND(UltrahumanConstants.UUID_CHARACTERISTIC_COMMAND),
    RESPONSE(UltrahumanConstants.UUID_CHARACTERISTIC_RESPONSE),
    STATE(UltrahumanConstants.UUID_CHARACTERISTIC_STATE),
    DATA(UltrahumanConstants.UUID_CHARACTERISTIC_DATA),
    TODO(UltrahumanConstants.UUID_CHARACTERISTIC_TODO);

    public final UUID uuid;

    UltrahumanCharacteristic(UUID uuid) {
        this.uuid = uuid;
    }
}
