/*  Copyright (C) 2023-2025 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import static nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability.COURSE_DOWNLOAD;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.GpxRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminGpxRouteInstallHandler extends GpxRouteInstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GarminGpxRouteInstallHandler.class);

    public GarminGpxRouteInstallHandler(final Uri uri, final Context context) {
        super(uri, context);
    }

    @Override
    protected boolean isCompatible(GBDevice device) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof GarminCoordinator garminCoordinator)) {
            LOG.warn("Coordinator is not a GarminCoordinator: {}", coordinator.getClass());
            return false;
        }
        return garminCoordinator.supports(device, COURSE_DOWNLOAD);
    }
}
