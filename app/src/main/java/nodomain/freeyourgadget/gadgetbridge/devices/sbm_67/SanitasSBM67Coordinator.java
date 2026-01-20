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

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class SanitasSBM67Coordinator extends AbstractSBM67Coordinator {
    @Nullable
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^BPM Smart$");
    }

    @Override
    public String getManufacturer() {
        return "Sanitas";
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sanitas_sbm_67;
    }

    @Override
    public int getBondingStyle() {
        // Sanitas one does not show a pin, and would pair with 0000, but it's not needed
        return BONDING_STYLE_NONE;
    }
}
