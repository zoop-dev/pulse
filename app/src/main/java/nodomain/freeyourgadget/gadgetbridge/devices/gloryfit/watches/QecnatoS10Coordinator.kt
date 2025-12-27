/*  Copyright (C) 2025 Jonathan Albrieux

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
package nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.watches

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.GloryFitCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.regex.Pattern

class QecnatoS10Coordinator : GloryFitCoordinator() {
    override fun getManufacturer(): String {
        return "Qecnato"
    }

    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^S10$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_qecnato_s10
    }

    override fun getBondingStyle(): Int {
        return BONDING_STYLE_NONE
    }

    override fun getAlarmSlotCount(device: GBDevice): Int {
        return 0
    }
}
