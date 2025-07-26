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
package nodomain.freeyourgadget.gadgetbridge.service.devices.generic_hr

import android.content.Intent
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.devices.GenericHeartRateSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate.HeartRateProfile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Calendar

class GenericHeartRateSupport : AbstractBTLESingleDeviceSupport(LOG) {
    private val deviceInfoProfile: DeviceInfoProfile<GenericHeartRateSupport>
    private val batteryInfoProfile: BatteryInfoProfile<GenericHeartRateSupport>
    private val heartRateProfile: HeartRateProfile<GenericHeartRateSupport>

    private val versionCmd = GBDeviceEventVersionInfo()
    private val batteryCmd = GBDeviceEventBatteryInfo()

    init {
        val mListener = IntentListener { intent: Intent? ->
            intent?.action?.let { action ->
                when (action) {
                    DeviceInfoProfile.ACTION_DEVICE_INFO -> {
                        handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO)!!)
                    }

                    BatteryInfoProfile.ACTION_BATTERY_INFO -> {
                        handleBatteryInfo(intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO)!!)
                    }

                    HeartRateProfile.ACTION_HEART_RATE -> {
                        handleHeartRate(intent.getIntExtra(HeartRateProfile.EXTRA_HEART_RATE, -1))
                    }
                }
            }
        }

        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION)
        deviceInfoProfile = DeviceInfoProfile<GenericHeartRateSupport>(this)
        deviceInfoProfile.addListener(mListener)
        addSupportedProfile(deviceInfoProfile)

        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE)
        batteryInfoProfile = BatteryInfoProfile<GenericHeartRateSupport>(this)
        batteryInfoProfile.addListener(mListener)
        addSupportedProfile(batteryInfoProfile)

        addSupportedService(GattService.UUID_SERVICE_HEART_RATE)
        heartRateProfile = HeartRateProfile<GenericHeartRateSupport>(this)
        heartRateProfile.addListener(mListener)
        addSupportedProfile(heartRateProfile)
    }

    override fun useAutoConnect(): Boolean {
        return false
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        builder.setDeviceState(GBDevice.State.INITIALIZING)

        deviceInfoProfile.requestDeviceInfo(builder)
        batteryInfoProfile.requestBatteryInfo(builder)
        batteryInfoProfile.enableNotify(builder, true)
        heartRateProfile.enableNotify(builder, true)

        device.firmwareVersion = "N/A"
        device.firmwareVersion2 = "N/A"

        builder.setDeviceState(GBDevice.State.INITIALIZED)

        return builder
    }

    private fun handleDeviceInfo(deviceInfo: DeviceInfo) {
        LOG.debug("Device info: {}", deviceInfo)

        if (deviceInfo.hardwareRevision != null) {
            versionCmd.hwVersion = deviceInfo.hardwareRevision
        }

        if (deviceInfo.firmwareRevision != null) {
            versionCmd.fwVersion = deviceInfo.firmwareRevision
            versionCmd.fwVersion2 = deviceInfo.softwareRevision
        } else if (deviceInfo.softwareRevision != null) {
            versionCmd.fwVersion = deviceInfo.softwareRevision
        }

        handleGBDeviceEvent(versionCmd)
    }

    private fun handleBatteryInfo(info: BatteryInfo) {
        LOG.debug("Battery info: {}", info)
        batteryCmd.level = info.percentCharged
        handleGBDeviceEvent(batteryCmd)
    }

    private fun handleHeartRate(heartRate: Int) {
        LOG.debug("Heart rate: {}", heartRate)

        if (heartRate <= 0) {
            return
        }

        val timestamp = Calendar.getInstance().getTimeInMillis()
        try {
            GBApplication.acquireDB().use { db ->
                val sampleProvider = GenericHeartRateSampleProvider(device, db.getDaoSession())
                val userId = DBHelper.getUser(db.getDaoSession()).id
                val deviceId = DBHelper.getDevice(device, db.getDaoSession()).id
                val sample = GenericHeartRateSample(timestamp, deviceId, userId, heartRate)
                sampleProvider.addSample(sample)
            }
        } catch (e: Exception) {
            LOG.error("Failed to save heartRate sample", e)
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GenericHeartRateSupport::class.java)
    }
}
