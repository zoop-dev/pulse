/*  Copyright (C) 2023 Daniele Gobbetti

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.sbm_67;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.generic_bp.GenericBloodPressureSupport;

/**
 * SBM67 devices seem to be sold under multiple brands, with slightly different bluetooth names and bonding behaviors.
 */
public abstract class AbstractSBM67Coordinator extends AbstractBLEDeviceCoordinator {
    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(GBDevice device) {
        return GenericBloodPressureSupport.class;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.BLOOD_PRESSURE_METER;
    }

    @Override
    public int getBatteryCount(GBDevice device) {
        return 0; // it does not report battery %
    }

    @Override
    public boolean suggestUnbindBeforePair() {
        // Works just fine if already paired
        return false;
    }

    @Override
    public boolean supportsBloodPressureMeasurement(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public GenericBloodPressureSampleProvider getBloodPressureSampleProvider(final GBDevice device, final DaoSession session) {
        return new GenericBloodPressureSampleProvider(device, session);
    }
}
