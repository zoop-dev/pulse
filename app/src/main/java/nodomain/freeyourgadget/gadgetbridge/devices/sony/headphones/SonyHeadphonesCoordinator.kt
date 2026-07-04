/*  Copyright (C) 2021-2024 Damien Gaignon, Daniel Dakhno, José Rebelo,
    Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones

import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.SonyHeadphonesSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class SonyHeadphonesCoordinator : AbstractBLClassicDeviceCoordinator() {
    override fun getManufacturer(): String {
        return "Sony"
    }

    override fun getDeviceSpecificSettingsCustomizer(device: GBDevice): DeviceSpecificSettingsCustomizer {
        return SonyHeadphonesSettingsCustomizer(device)
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun supportsPowerOff(device: GBDevice): Boolean {
        return getCapabilities(device).contains(SonyHeadphonesCapabilities.PowerOffFromPhone)
    }

    override fun getBatteryCount(device: GBDevice): Int {
        val capabilities = getCapabilities(device)

        if (capabilities.contains(SonyHeadphonesCapabilities.BatterySingle)) {
            if (capabilities.contains(SonyHeadphonesCapabilities.BatteryDual) ||
                capabilities.contains(SonyHeadphonesCapabilities.BatteryDual2)
            ) {
                LOG.error("A device can't have both single and dual battery")
                return 0
            } else if (capabilities.contains(SonyHeadphonesCapabilities.BatteryCase)) {
                LOG.error("Devices with single battery + case are not supported by the protocol")
                return 0
            }
        }

        var batteryCount = 0

        if (capabilities.contains(SonyHeadphonesCapabilities.BatterySingle)) {
            batteryCount += 1
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.BatteryCase)) {
            batteryCount += 1
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.BatteryDual) ||
            capabilities.contains(SonyHeadphonesCapabilities.BatteryDual2)
        ) {
            batteryCount += 2
        }

        return batteryCount
    }

    override fun getBatteryConfig(device: GBDevice): Array<BatteryConfig> {
        val batteries: MutableList<BatteryConfig> = ArrayList(3)

        if (defaultCapabilities.contains(SonyHeadphonesCapabilities.BatterySingle)) {
            batteries.add(
                BatteryConfig(
                    batteries.size,
                    GBDevice.BATTERY_ICON_DEFAULT.toInt(),
                    GBDevice.BATTERY_LABEL_DEFAULT.toInt(),
                    BATTERY_DEFAULT_LOW_THRESHOLD,
                    BATTERY_DEFAULT_FULL_THRESHOLD
                )
            )
        }

        if (defaultCapabilities.contains(SonyHeadphonesCapabilities.BatteryCase)) {
            batteries.add(
                BatteryConfig(
                    batteries.size,
                    R.drawable.ic_tws_case,
                    R.string.battery_case,
                    BATTERY_DEFAULT_LOW_THRESHOLD,
                    BATTERY_DEFAULT_FULL_THRESHOLD
                )
            )
        }

        if (defaultCapabilities.contains(SonyHeadphonesCapabilities.BatteryDual) ||
            defaultCapabilities.contains(SonyHeadphonesCapabilities.BatteryDual2)
        ) {
            batteries.add(
                BatteryConfig(
                    batteries.size,
                    R.drawable.ic_galaxy_buds_l,
                    R.string.left_earbud,
                    BATTERY_DEFAULT_LOW_THRESHOLD,
                    BATTERY_DEFAULT_FULL_THRESHOLD
                )
            )
            batteries.add(
                BatteryConfig(
                    batteries.size,
                    R.drawable.ic_galaxy_buds_r,
                    R.string.right_earbud,
                    BATTERY_DEFAULT_LOW_THRESHOLD,
                    BATTERY_DEFAULT_FULL_THRESHOLD
                )
            )
        }

        return batteries.toTypedArray()
    }

    override fun getDeviceSettings(device: GBDevice): DeviceSettingsSpec =
        sonyHeadphonesDeviceSettings(device, getCapabilities(device))

    open val defaultCapabilities: Set<SonyHeadphonesCapabilities>
        get() = mutableSetOf()

    open fun getCapabilities(device: GBDevice): Set<SonyHeadphonesCapabilities> {
        val devicePrefs = GBApplication.getDevicePrefs(device)
        val overrideFeatures =
            devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_OVERRIDE_FEATURES_ENABLED, false)
        if (overrideFeatures) {
            val stringList = devicePrefs.getStringSet(
                DeviceSettingsPreferenceConst.PREF_OVERRIDE_FEATURES_LIST,
                mutableSetOf<String>()
            )

            return stringList.mapTo(mutableSetOf()) { SonyHeadphonesCapabilities.valueOf(it) }
        }
        return this.defaultCapabilities
    }

    open fun preferServiceV2(): Boolean {
        return false
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport> {
        return SonyHeadphonesSupport::class.java
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_sony_overhead
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(SonyHeadphonesCoordinator::class.java)

        private const val BATTERY_DEFAULT_LOW_THRESHOLD = 20
        private const val BATTERY_DEFAULT_FULL_THRESHOLD = 100
    }
}
