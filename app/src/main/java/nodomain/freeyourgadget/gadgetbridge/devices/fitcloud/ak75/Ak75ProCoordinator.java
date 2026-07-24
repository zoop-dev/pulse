/*  Copyright (C) 2026 Vladimir Tasic

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

package nodomain.freeyourgadget.gadgetbridge.devices.fitcloud.ak75;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.fitcloud.ak102.Ak102Coordinator;

/// #6446
public class Ak75ProCoordinator extends Ak102Coordinator {

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("AK75 PRO", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_ak75_pro;
    }
}
