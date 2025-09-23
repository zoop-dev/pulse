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
package nodomain.freeyourgadget.gadgetbridge.devices.gloryfit

import android.content.Context
import android.content.Intent
import de.greenrobot.dao.AbstractDao
import de.greenrobot.dao.Property
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.CameraActivity
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability.MeasurementInterval
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSpo2SampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSampleDao
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSleepStageSampleDao
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSpo2SampleDao
import nodomain.freeyourgadget.gadgetbridge.entities.GloryFitStepsSampleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.gloryfit.GloryFitLanguage
import nodomain.freeyourgadget.gadgetbridge.service.devices.gloryfit.GloryFitSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationCount
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationKind

abstract class GloryFitCoordinator : AbstractBLEDeviceCoordinator() {
    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_amazfit_bip
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.WATCH
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport> {
        return GloryFitSupport::class.java
    }

    override fun getAllDeviceDao(session: DaoSession): MutableMap<AbstractDao<*, *>, Property> {
        return object : HashMap<AbstractDao<*, *>, Property>() {
            init {
                put(session.gloryFitStepsSampleDao, GloryFitStepsSampleDao.Properties.DeviceId)
                put(session.genericSleepStageSampleDao, GenericSleepStageSampleDao.Properties.DeviceId)
                put(session.genericHeartRateSampleDao, GenericHeartRateSampleDao.Properties.DeviceId)
                put(session.genericSpo2SampleDao, GenericSpo2SampleDao.Properties.DeviceId)
            }
        }
    }

    override fun getSampleProvider(
        device: GBDevice,
        session: DaoSession
    ): SampleProvider<out AbstractActivitySample>? {
        return GloryFitActivitySampleProvider(device, session)
    }

    override fun getSpo2SampleProvider(
        device: GBDevice,
        session: DaoSession
    ): TimeSampleProvider<out Spo2Sample>? {
        return GenericSpo2SampleProvider(device, session)
    }

    override fun getAlarmSlotCount(device: GBDevice): Int {
        return 3
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun supportsActivityDataFetching(device: GBDevice): Boolean {
        return true
    }

    override fun supportsActivityTracking(device: GBDevice): Boolean {
        return true
    }

    override fun supportsActiveCalories(device: GBDevice): Boolean {
        // TODO it does not, but we could try and match their formula in the samples
        return false
    }

    override fun supportsSpo2(device: GBDevice): Boolean {
        return true
    }

    override fun supportsMusicInfo(device: GBDevice): Boolean {
        // Not info, but supports music control
        return true
    }

    override fun getCannedRepliesSlotCount(device: GBDevice): Int {
        return 8
    }

    override fun getContactsSlotCount(device: GBDevice): Int {
        return 100
    }

    override fun supportsHeartRateMeasurement(device: GBDevice): Boolean {
        return true
    }

    override fun supportsManualHeartRateMeasurement(device: GBDevice): Boolean {
        return false // TODO supportsManualHeartRateMeasurement
    }

    override fun supportsRealtimeData(device: GBDevice): Boolean {
        // TODO it does
        return false
    }

    override fun supportsRemSleep(device: GBDevice): Boolean {
        return true
    }

    override fun supportsAwakeSleep(device: GBDevice): Boolean {
        return true
    }

    override fun supportsWeather(device: GBDevice): Boolean {
        // TODO it does
        return false
    }

    override fun supportsFindDevice(device: GBDevice): Boolean {
        return true
    }

    override fun supportsUnicodeEmojis(device: GBDevice): Boolean {
        // Official app seems to just remove them outright
        return false
    }

    override fun addBatteryPollingSettings(): Boolean {
        // It only sends proactive updates during charging
        return true
    }

    override fun supportsNotificationVibrationPatterns(device: GBDevice): Boolean {
        return true
    }

    override fun supportsNotificationVibrationRepetitionPatterns(device: GBDevice): Boolean {
        return true
    }

    override fun getNotificationVibrationPatterns(): Array<AbstractNotificationPattern?> {
        return arrayOf(
            VibrationKind.NONE,
            VibrationKind.BASIC,
        )
    }

    override fun getNotificationVibrationRepetitionPatterns(): Array<AbstractNotificationPattern?> {
        return arrayOf(
            VibrationCount.ONCE,
            VibrationCount.TWICE,
            VibrationCount.THREE,
            VibrationCount.FOUR,
        )
    }

    override fun getDeviceSpecificSettings(device: GBDevice): DeviceSpecificSettings {
        val deviceSpecificSettings = DeviceSpecificSettings()

        val dateTime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME)
        dateTime.add(R.xml.devicesettings_timeformat)

        val display = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY)
        display.add(R.xml.devicesettings_liftwrist_display_noshed)

        val health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH)
        health.add(R.xml.devicesettings_heartrate_automatic_enable)
        health.add(R.xml.devicesettings_heartrate_alerts)
        if (supportsSpo2(device)) {
            health.add(R.xml.devicesettings_spo2)
        }
        health.add(R.xml.devicesettings_inactivity_dnd)

        val notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS)
        notifications.add(R.xml.devicesettings_transliteration)
        if (getContactsSlotCount(device) > 0) {
            notifications.add(R.xml.devicesettings_contacts)
        }
        notifications.add(R.xml.devicesettings_header_notifications)
        notifications.add(R.xml.devicesettings_send_app_notifications)
        notifications.add(R.xml.devicesettings_per_app_notifications)
        notifications.add(R.xml.devicesettings_header_phone_calls)
        notifications.add(R.xml.devicesettings_reject_call_method)
        if (getCannedRepliesSlotCount(device) > 0) {
            notifications.add(R.xml.devicesettings_sms_quick_reply)
            notifications.add(R.xml.devicesettings_canned_reply_16)
        }

        return deviceSpecificSettings
    }

    override fun getDeviceSpecificSettingsCustomizer(device: GBDevice): DeviceSpecificSettingsCustomizer? {
        return GloryFitSettingsCustomizer()
    }

    override fun getSupportedLanguageSettings(device: GBDevice): Array<String> {
        // TODO fetch languages from device
        return arrayOf("auto") + GloryFitLanguage.entries.map { it.locale }
    }

    override fun getHeartRateMeasurementIntervals(): List<MeasurementInterval?> {
        // actually on/off
        return listOf<MeasurementInterval?>(
            MeasurementInterval.OFF,
            MeasurementInterval.SMART
        )
    }

    override fun getCustomActions(): List<DeviceCardAction> {
        if (!CameraActivity.supportsCamera()) {
            return emptyList()
        }

        return listOf(
            object : DeviceCardAction {
                override fun getIcon(device: GBDevice): Int {
                    return R.drawable.ic_camera_remote
                }

                override fun getDescription(device: GBDevice, context: Context): String? {
                    return context.getString(R.string.open_camera)
                }

                override fun onClick(device: GBDevice, context: Context) {
                    val cameraIntent = Intent(context, CameraActivity::class.java)
                    cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    cameraIntent.putExtra(
                        CameraActivity.intentExtraEvent,
                        GBDeviceEventCameraRemote.eventToInt(GBDeviceEventCameraRemote.Event.OPEN_CAMERA)
                    )
                    context.startActivity(cameraIntent)
                }
            }
        )
    }
}
