/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import android.content.Context;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GBDeviceEventVersionInfo extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventVersionInfo.class);

    public String fwVersion = "N/A";
    public String fwVersion2 = null;
    public String hwVersion = "N/A";

    public GBDeviceEventVersionInfo() {
        if (GBApplication.getContext() != null) {
            // Only get from context if there is one (eg. not in unit tests)
            this.fwVersion = GBApplication.getContext().getString(R.string.n_a);
            this.hwVersion = GBApplication.getContext().getString(R.string.n_a);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + "fwVersion: " + fwVersion + "; fwVersion2: " + fwVersion2 + "; hwVersion: " + hwVersion;
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        LOG.info("Got event for VERSION_INFO: {}", this);
        if (device == null) {
            return;
        }
        if (fwVersion != null) {
            device.setFirmwareVersion(fwVersion);
        }
        if (fwVersion2 != null) {
            device.setFirmwareVersion2(fwVersion2);
        }
        if (hwVersion != null) {
            device.setModel(hwVersion);
        }
        device.sendDeviceUpdateIntent(context);
    }
}
