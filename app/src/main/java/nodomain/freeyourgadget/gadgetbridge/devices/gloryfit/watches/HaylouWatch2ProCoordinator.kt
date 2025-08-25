/*  Copyright (C) 2025 José Rebelo

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
import java.util.regex.Pattern

class HaylouWatch2ProCoordinator : GloryFitCoordinator() {
    override fun getManufacturer(): String {
        return "Haylou"
    }

    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^HAYLOU Watch 2 Pro$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_haylou_watch_2_pro
    }
}
