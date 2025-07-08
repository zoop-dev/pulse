/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.straps;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AmazfitHelioStrapCoordinator extends ZeppOsCoordinator {
    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public List<String> getDeviceBluetoothNames() {
        return Collections.singletonList("Amazfit Helio Strap"); // no mac address at the end
    }

    @Override
    public Set<Integer> getDeviceSources() {
        return Collections.emptySet(); // TODO are there?
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_helio_strap;
    }

    @Override
    public boolean supportsScreenshots(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return false;
    }

    @Override
    public boolean supportsMusicInfo() {
        return false;
    }

    @Override
    public boolean supportsHrvMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public int getWorldClocksSlotCount() {
        return 0;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return 0;
    }

    @Override
    public boolean supportsTemperatureMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsContinuousTemperature(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAgpsUpdates() {
        return false;
    }
}
