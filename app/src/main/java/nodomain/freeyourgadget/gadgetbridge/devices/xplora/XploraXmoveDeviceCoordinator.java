/*
Copyright (C) 2024 enoint

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
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package nodomain.freeyourgadget.gadgetbridge.devices.xplora;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.hama.fit6900.HamaFit6900DeviceCoordinator;

public class XploraXmoveDeviceCoordinator extends HamaFit6900DeviceCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^XMOVE$");
    }

    @Override
    public String getManufacturer() {
        return "Xplora";
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_xplora_xmove;
    }
}
