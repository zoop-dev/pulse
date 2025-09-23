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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin.hrm;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupportHrm;

public class GarminHrmProPlusCoordinator extends GarminCoordinator {
    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_lovetoy;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.CHEST_STRAP;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_hrm_pro_plus;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return GarminSupportHrm.class;
    }

    @Override
    public Pattern getSupportedDeviceName() {
        return Pattern.compile("^HRMPro[+]:\\d+$");
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public boolean suggestUnbindBeforePair() {
        // Not needed
        return false;
    }

    @Override
    public boolean supportsActivityDataFetching(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRealtimeData(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsSleepMeasurement(@NonNull final GBDevice device){
        return false;
    }
}
