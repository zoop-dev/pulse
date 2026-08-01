/*  Copyright (C) 2026 Toby Murray

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
package nodomain.freeyourgadget.gadgetbridge.devices.una

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.una.UnaDeviceSupport
import java.util.regex.Pattern

/**
 * Connect, battery, firmware/hardware revision, and time sync via standard SIG GATT services.
 * Activity-file sync over the custom File Transfer Service is a separate fast-follow.
 */
class UnaDeviceCoordinator : AbstractBLEDeviceCoordinator() {
    override fun getSupportedDeviceName(): Pattern {
        // Digit count isn't a confirmed fixed width; unconstrained to avoid missing real units.
        return Pattern.compile("^UNA Watch \\d+$")
    }

    override fun getManufacturer(): String = "UNA"

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind =
        DeviceCoordinator.DeviceKind.WATCH

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport> =
        UnaDeviceSupport::class.java

    override fun getDeviceNameResource(): Int = R.string.devicetype_una_watch

    override fun getDefaultIconResource(): Int = R.drawable.ic_device_default

    // No app-layer secret; standard BLE bonding is the entire security gate on this firmware.
    override fun getBondingStyle(): Int = BONDING_STYLE_BOND
}
