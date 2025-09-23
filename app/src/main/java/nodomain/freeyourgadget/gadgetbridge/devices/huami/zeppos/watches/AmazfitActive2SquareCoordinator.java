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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AmazfitActive2SquareCoordinator extends ZeppOsCoordinator {
    @Override
    public boolean isExperimental() {
        // untested
        return true;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.BOTH;
    }

    @Override
    public List<String> getDeviceBluetoothNames() {
        return Arrays.asList(
                "Active 2 (Square)", // never seen, assumption following the Round one
                "Active 2 NFC (Square)", // never seen, assumption following the Round one
                "Active 2 Square", // never seen
                "Active 2 NFC Square" // #5056
        );
    }

    @Override
    public Set<Integer> getDeviceSources() {
        return new HashSet<>(Arrays.asList(
                // Old values removed from the website?
                //9830656, // chinese mainland version
                //9830657,
                //9830659,
                10223872, // chinese mainland version
                10223873,
                10223875
        ));
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_active_2_square;
    }

    @Override
    public boolean mainMenuHasMoreSection() {
        return false;
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
    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return true;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
