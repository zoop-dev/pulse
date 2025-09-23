/*  Copyright (C) 2025 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin.gps;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminInReachMini2 extends GarminCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_inReach_mini_2;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^inReach Mini 2$");
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching(@NonNull final GBDevice device) {
        // for gps tracks
        return true;
    }

    @Override
    public boolean supportsActivityTracks(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsOSBatteryLevel(@NonNull GBDevice device){
        // uses standard Bluetooth battery level updates
        // Garmin's RemoteDeviceBatteryStatusRequest isn't supported
        return true;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.UNKNOWN;
    }
}
