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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitactive2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AmazfitActive2Coordinator extends ZeppOsCoordinator {
    @Override
    public String getDeviceBluetoothName() {
        return HuamiConst.AMAZFIT_ACTIVE_2_NAME;
    }

    @Override
    public Set<Integer> getDeviceSources() {
        return new HashSet<>(Arrays.asList(
                8913152, // chinese mainland version
                8913153,
                8913155, // chinese mainland version
                8913159
        ));
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_active_2;
    }

    @Override
    public boolean supportsContinuousFindDevice() {
        return true;
    }

    @Override
    public boolean mainMenuHasMoreSection() {
        return true;
    }

    @Override
    public boolean supportsGpxUploads() {
        return true;
    }

    @Override
    public boolean supportsControlCenter() {
        return true;
    }

    @Override
    public boolean supportsScreenshots(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsToDoList() {
        return true;
    }

    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return true;
    }
}
