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
package nodomain.freeyourgadget.gadgetbridge.devices.gloryfitpro

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.gloryfitpro.GloryFitProSupport

/**
 * Base coordinator for watches speaking the "GloryFit Pro" BLE dialect (com.yc.gloryfitpro).
 * Concrete per-model coordinators (e.g. [nodomain.freeyourgadget.gadgetbridge.devices.gloryfitpro.watches.DM58Coordinator])
 * only supply the advertised-name pattern, manufacturer and display name. MVP: connect + firmware + time.
 */
abstract class GloryFitProCoordinator : AbstractBLEDeviceCoordinator() {
    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_amazfit_bip
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.WATCH
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport> {
        return GloryFitProSupport::class.java
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun getDeviceSpecificSettings(device: GBDevice): DeviceSpecificSettings {
        val settings = DeviceSpecificSettings()
        val notifications = settings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS)
        notifications.add(R.xml.devicesettings_header_notifications)
        notifications.add(R.xml.devicesettings_send_app_notifications)
        notifications.add(R.xml.devicesettings_per_app_notifications)
        return settings
    }
}
