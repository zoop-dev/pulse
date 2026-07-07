/*  Copyright (C) 2026 ExploWare

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
package nodomain.freeyourgadget.gadgetbridge.devices.gloryfitpro.watches

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfitpro.GloryFitProCoordinator
import java.util.regex.Pattern

class DM58Coordinator : GloryFitProCoordinator() {
    override fun getManufacturer(): String {
        return "Unknown"
    }

    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^DM58$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_dm58
    }
}
