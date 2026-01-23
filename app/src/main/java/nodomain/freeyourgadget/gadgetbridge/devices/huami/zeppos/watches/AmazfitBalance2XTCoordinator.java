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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AmazfitBalance2XTCoordinator extends ZeppOsCoordinator {
    @Override
    public boolean isExperimental() {
        // untested
        return true;
    }

    @Override
    public List<String> getDeviceBluetoothNames() {
        return Collections.singletonList("Amazfit Balance 2 XT");
    }

    @Override
    public Set<Integer> getDeviceSources() {
        return new HashSet<>(Arrays.asList(
                10486016,
                10486017,
                10486019
        ));
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_balance_2_xt;
    }

    @Override
    public boolean mainMenuHasMoreSection() {
        return true;
    }

    @Override
    public boolean supportsGpxUploads(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsControlCenter() {
        return true;
    }

    @Override
    public boolean supportsToDoList() {
        return true;
    }

    @Override
    public boolean supportsWifiHotspot(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFtpServer(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return true;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
