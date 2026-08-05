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

import androidx.core.content.edit
import de.greenrobot.dao.AbstractDao
import de.greenrobot.dao.Property
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.UnaDailySampleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.una.UnaDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import java.util.regex.Pattern

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

    override fun getDefaultIconResource(): Int = R.drawable.ic_device_miwatch

    // No app-layer secret; standard BLE bonding is the entire security gate on this firmware.
    override fun getBondingStyle(): Int = BONDING_STYLE_BOND

    override fun createDevice(candidate: GBDeviceCandidate, deviceType: DeviceType): GBDevice {
        val gbDevice = super.createDevice(candidate, deviceType)
        GBApplication.getDevicePrefs(gbDevice).preferences.edit {
            putBoolean(DeviceSettingsPreferenceConst.PREF_CONNECTION_PRIORITY_LOW_POWER, true)
            putBoolean(GBPrefs.DEVICE_CONNECT_BACK, true)
            putBoolean(GBPrefs.DEVICE_AUTO_RECONNECT, true)
        }
        return gbDevice
    }

    override fun supportsDataFetching(device: GBDevice): Boolean = true

    override fun supportsRecordedActivities(device: GBDevice): Boolean = true

    // Daily steps/active-minutes/heart-rate totals, from the watch's own CCS DailyHealth
    // request -- one synthetic sample burst per day, not a real per-minute stream.
    override fun supportsActivityTracking(device: GBDevice): Boolean = true

    override fun getSampleProvider(device: GBDevice, session: DaoSession): SampleProvider<out AbstractActivitySample> =
        UnaDailySampleProvider(device, session)

    override fun getAllDeviceDao(session: DaoSession): MutableMap<AbstractDao<*, *>, Property> {
        return object : HashMap<AbstractDao<*, *>, Property>() {
            init {
                put(session.unaDailySampleDao, UnaDailySampleDao.Properties.DeviceId)
            }
        }
    }
}
