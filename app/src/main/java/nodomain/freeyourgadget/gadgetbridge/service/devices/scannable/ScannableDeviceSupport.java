/*  Copyright (C) 2023-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.scannable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;

public class ScannableDeviceSupport extends AbstractDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ScannableDeviceSupport.class);

    @Override
    public boolean connect() {
        LOG.error("Attempting to connect to a scannable device - this should never happen");
        return false;
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
