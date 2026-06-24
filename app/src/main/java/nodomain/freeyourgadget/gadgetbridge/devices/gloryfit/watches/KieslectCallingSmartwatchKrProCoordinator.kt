/*  Copyright (C) 2026 José Rebelo

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

/// #5560
class KieslectCallingSmartwatchKrProCoordinator : GloryFitCoordinator() {
    override fun getManufacturer(): String {
        return "Kieslect"
    }

    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^Watch Kr Pro-[0-9A-F]{4}$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_kieslect_calling_smartwatch_kr_pro
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_miwatch
    }

    override fun getBondingStyle(): Int {
        return BONDING_STYLE_NONE
    }

    override fun getAlarmSlotCount(device: GBDevice): Int {
        // #5560 - alarms from app do not work
        return 0
    }
}
