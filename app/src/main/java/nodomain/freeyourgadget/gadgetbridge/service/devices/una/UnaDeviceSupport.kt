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
package nodomain.freeyourgadget.gadgetbridge.service.devices.una

import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile
import org.slf4j.LoggerFactory
import java.util.GregorianCalendar

/**
 * Connect, battery, firmware/hardware revision (DIS), and time sync (CTS). Activity file sync
 * over the custom File Transfer Service is a separate fast-follow.
 */
class UnaDeviceSupport : AbstractBTLESingleDeviceSupport(LOG) {
    private val deviceInfoProfile: DeviceInfoProfile<UnaDeviceSupport>
    private val batteryInfoProfile: BatteryInfoProfile<UnaDeviceSupport>

    init {
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION)
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE)
        addSupportedService(GattService.UUID_SERVICE_CURRENT_TIME)

        val listener = IntentListener { intent ->
            when (intent.action) {
                DeviceInfoProfile.ACTION_DEVICE_INFO ->
                    handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO)!!)
                BatteryInfoProfile.ACTION_BATTERY_INFO ->
                    handleBatteryInfo(intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO)!!)
            }
        }

        deviceInfoProfile = DeviceInfoProfile(this)
        deviceInfoProfile.addListener(listener)
        addSupportedProfile(deviceInfoProfile)

        batteryInfoProfile = BatteryInfoProfile(this)
        batteryInfoProfile.addListener(listener)
        addSupportedProfile(batteryInfoProfile)
    }

    override fun useAutoConnect(): Boolean = true

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        builder.setDeviceState(GBDevice.State.INITIALIZING)

        deviceInfoProfile.requestDeviceInfo(builder)

        batteryInfoProfile.requestBatteryInfo(builder)
        batteryInfoProfile.enableNotify(builder, true)

        if (GBApplication.getPrefs().syncTime()) {
            writeCurrentTime(builder)
        }

        builder.setDeviceState(GBDevice.State.INITIALIZED)
        return builder
    }

    override fun onSetTime() {
        if (!GBApplication.getPrefs().syncTime()) return
        val builder = createTransactionBuilder("set time")
        writeCurrentTime(builder)
        builder.queue()
    }

    private fun writeCurrentTime(builder: TransactionBuilder) {
        val now = GregorianCalendar()
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME, *BLETypeConversions.calendarToCurrentTime(now, 0))
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_LOCAL_TIME, *BLETypeConversions.calendarToLocalTime(now))
    }

    private fun handleDeviceInfo(info: DeviceInfo) {
        LOG.debug("Device info: {}", info)
        for (event in DeviceInfoProfile.toDeviceEvents(info)) {
            evaluateGBDeviceEvent(event)
        }
    }

    private fun handleBatteryInfo(info: BatteryInfo) {
        LOG.debug("Battery info: {}", info)
        val battery = GBDeviceEventBatteryInfo()
        battery.level = info.percentCharged
        evaluateGBDeviceEvent(battery)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(UnaDeviceSupport::class.java)
    }
}
