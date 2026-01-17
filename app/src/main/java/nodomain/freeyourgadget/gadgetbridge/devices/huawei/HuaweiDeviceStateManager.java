/*  Copyright (C) 2025 Damien José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiDeviceStateManager {
    private static final Map<String, HuaweiState> STATES = new ConcurrentHashMap<>();

    public static HuaweiState get(final GBDevice device) {
        return get(device.getAddress());
    }

    public static HuaweiState get(final String macAddress) {
        return STATES.computeIfAbsent(macAddress, HuaweiState::new);
    }
}
