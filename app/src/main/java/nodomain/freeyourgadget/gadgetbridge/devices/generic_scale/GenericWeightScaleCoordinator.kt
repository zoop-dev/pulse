/*  Copyright (C) 2025 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices.generic_scale

import de.greenrobot.dao.AbstractDao
import de.greenrobot.dao.Property
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.GenericWeightSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.GenericWeightSampleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate
import nodomain.freeyourgadget.gadgetbridge.model.WeightSample
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService
import nodomain.freeyourgadget.gadgetbridge.service.devices.generic_scale.GenericWeightScaleSupport
import java.util.Collections

class GenericWeightScaleCoordinator : AbstractBLEDeviceCoordinator() {
    override fun getAllDeviceDao(session: DaoSession): MutableMap<AbstractDao<*, *>?, Property?> {
        return Collections.singletonMap(
            session.genericWeightSampleDao, GenericWeightSampleDao.Properties.DeviceId
        )
    }

    override fun getCustomActions(): MutableList<DeviceCardAction?> {
        return Collections.singletonList<DeviceCardAction>(GenericWeightScaleAction())
    }

    override fun getOrderPriority(): Int {
        return Int.MAX_VALUE
    }

    override fun getSupportedDeviceSpecificSettings(device: GBDevice?): IntArray? {
        return intArrayOf(R.xml.devicesettings_weight_scale_unit)
    }

    override fun getWeightSampleProvider(
        device: GBDevice?, session: DaoSession?
    ): TimeSampleProvider<out WeightSample?>? {
        return GenericWeightSampleProvider(device, session)
    }

    override fun isExperimental(): Boolean {
        // needs more testing
        return true
    }

    override fun getManufacturer(): String? {
        return "Generic"
    }

    override fun getDeviceSupportClass(device: GBDevice?): Class<out DeviceSupport?> {
        return GenericWeightScaleSupport::class.java
    }

    override fun getBondingStyle(): Int {
        return BONDING_STYLE_ASK
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_miscale
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_generic_weight_scale
    }

    override fun supports(candidate: GBDeviceCandidate): Boolean {
        return candidate.supportsService(GattService.UUID_SERVICE_WEIGHT_SCALE)
    }

    override fun supportsWeightMeasurement(device: GBDevice): Boolean {
        return true
    }

    override fun supportsActivityTracking(device: GBDevice): Boolean {
        return true
    }

    override fun supportsActivityTabs(device: GBDevice): Boolean {
        return false
    }

    override fun supportsSleepMeasurement(device: GBDevice): Boolean {
        return false
    }

    override fun supportsStepCounter(device: GBDevice): Boolean {
        return false
    }

    override fun supportsSpeedzones(device: GBDevice): Boolean {
        return false
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.SCALE
    }
}