package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi_scooters

import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.enumList
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.GenericTemperatureSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters.XiaomiScooterEnergyRecovery
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters.XiaomiScooterSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters.XiaomiScooterTirePressureInterval
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters.XiaomiScooterUnits

abstract class XiaomiScooterCoordinator : AbstractBLEDeviceCoordinator() {
    override fun isExperimental(): Boolean {
        // Not yet extensively tested
        // Sometimes the scooter dashboard would freeze during development, although that
        // has not happened in a long time.
        return true
    }

    override fun getManufacturer(): String {
        return "Xiaomi"
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport?> {
        return XiaomiScooterSupport::class.java
    }

    override fun createDevice(candidate: GBDeviceCandidate, deviceType: DeviceType): GBDevice? {
        val gbDevice = super.createDevice(candidate, deviceType)
        gbDevice?.alias = GBApplication.getContext().getString(deviceNameResource)
        return gbDevice
    }

    override fun getBondingStyle(): Int {
        return BONDING_STYLE_NONE
    }

    override fun requiresAuthKey(): Boolean {
        return true
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_scooter
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.SCOOTER
    }

    override fun validateAuthKey(authKey: String): Boolean {
        val authKeyBytes: ByteArray = authKey.trim().toByteArray()
        return authKeyBytes.size == 64 || (authKey.trim().startsWith("0x") && authKeyBytes.size == 66)
    }

    override fun getAuthHelp(): String? {
        return "https://gadgetbridge.org/gadgets/scooters/xiaomi/#auth-key"
    }

    override fun getSupportedDeviceSpecificAuthenticationSettings(): IntArray {
        return intArrayOf(R.xml.devicesettings_pairingkey)
    }

    override fun supportsFindDevice(device: GBDevice): Boolean {
        return true
    }

    override fun supportsRecordedActivities(device: GBDevice): Boolean {
        return true
    }

    override fun supportsTemperatureMeasurement(device: GBDevice): Boolean {
        return true
    }

    override fun supportsContinuousTemperature(device: GBDevice): Boolean {
        return true
    }

    override fun getTemperatureSampleProvider(
        device: GBDevice,
        session: DaoSession
    ): TimeSampleProvider<out TemperatureSample?> {
        return GenericTemperatureSampleProvider(device, session)
    }

    override fun getDeviceSettings(device: GBDevice): DeviceSettingsSpec = deviceSettings {
        switchSetting(
            key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_MOTOR_LOCKED,
            title = R.string.scooter_lock_motor_title,
            summary = R.string.scooter_lock_motor_summary,
            icon = R.drawable.ic_lock_open,
        )
        // TODO ride mode
        //enumList<XiaomiScooterRideMode>(
        //    key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_RIDE_MODE,
        //    title = R.string.xiaomi_scooter_ride_mode,
        //    defaultValue = XiaomiScooterRideMode.STANDARD,
        //    icon = R.drawable.ic_speed,
        //)
        // TODO scooter temperature
        //text(
        //    key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SCOOTER_TEMP,
        //    title = R.string.xiaomi_scooter_scooter_temp,
        //    enabled = false,
        //)
        enumList<XiaomiScooterUnits>(
            key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SPEED_UNIT,
            title = R.string.pref_title_unit_system,
            defaultValue = XiaomiScooterUnits.KMH,
            icon = R.drawable.ic_straighten,
        )

        screen(
            key = "xiaomi_scooter_lights",
            title = R.string.supercars_lights_label,
            icon = R.drawable.ic_wb_sunny,
        ) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TAILLIGHT_ALWAYS_ON,
                title = R.string.xiaomi_scooter_taillight_always_on,
                icon = R.drawable.ic_wb_sunny,
            )
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_AUTO_LIGHTS,
                title = R.string.xiaomi_scooter_auto_lights_title,
                summary = R.string.xiaomi_scooter_auto_lights_summary,
                icon = R.drawable.ic_light_mode_auto,
            )
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_AMBIENT_LIGHT,
                title = R.string.xiaomi_scooter_ambient_light_title,
                summary = R.string.xiaomi_scooter_ambient_light_summary,
                icon = R.drawable.ic_fluorescent,
            )
        }

        screen(
            key = "xiaomi_scooter_assisted_driving",
            title = R.string.xiaomi_scooter_category_assisted_driving,
            icon = R.drawable.ic_activity_driving,
        ) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SMART_ENERGY_RECOVERY,
                title = R.string.xiaomi_scooter_smart_energy_recovery_title,
                summary = R.string.xiaomi_scooter_smart_energy_recovery_summary,
                icon = R.drawable.ic_auto_awesome
            )
            enumList<XiaomiScooterEnergyRecovery>(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_ENERGY_RECOVERY_INTENSITY,
                title = R.string.xiaomi_scooter_energy_recovery_level,
                defaultValue = XiaomiScooterEnergyRecovery.MEDIUM,
                icon = R.drawable.ic_bolt,
            )
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SLOPE_PARKING,
                title = R.string.xiaomi_scooter_slope_parking_title,
                summary = R.string.xiaomi_scooter_slope_parking_summary,
                icon = R.drawable.ic_moving,
            )
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TCS_ANTI_SLIP,
                title = R.string.xiaomi_scooter_tcs_anti_slip_title,
                summary = R.string.xiaomi_scooter_tcs_anti_slip_summary,
                icon = R.drawable.ic_tcs_anti_slip,
            )
        }

        screen(
            key = "xiaomi_scooter_tire_pressure",
            title = R.string.xiaomi_scooter_tire_pressure_title,
            icon = R.drawable.ic_tire_repair,
        ) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_ENABLED,
                title = R.string.xiaomi_scooter_tire_pressure_enabled_title,
                summary = R.string.xiaomi_scooter_tire_pressure_enabled_summary,
                defaultValue = true,
                icon = R.drawable.ic_tire_repair,
            )
            enumList<XiaomiScooterTirePressureInterval>(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_INTERVAL_DAYS,
                title = R.string.xiaomi_scooter_tire_pressure_interval_title,
                defaultValue = XiaomiScooterTirePressureInterval.DAYS_30,
                dependency = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_ENABLED,
                icon = R.drawable.ic_calendar_from,
            )
            info(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_REMAINING_DAYS,
                title = R.string.xiaomi_scooter_tire_pressure_remaining_title,
                icon = R.drawable.ic_timer,
                dependency = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_ENABLED,
            )
            action(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_RESET,
                title = R.string.xiaomi_scooter_tire_pressure_reset_title,
                summary = R.string.xiaomi_scooter_tire_pressure_reset_summary,
                icon = R.drawable.ic_refresh,
                dependency = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_ENABLED,
                confirmationMessage = R.string.xiaomi_scooter_tire_pressure_reset_confirmation,
                onClick = { handler ->
                    handler.notifyPreferenceChanged(DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_RESET)
                    true
                },
            )
        }

        screen(
            key = DeviceSpecificSettingsScreen.CONNECTION.key,
            title = R.string.pref_header_connection,
            icon = R.drawable.ic_mtu,
        ) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_KEEP_BLUETOOTH_ON,
                title = R.string.xiaomi_scooter_keep_bluetooth_on_title,
                summary = R.string.xiaomi_scooter_keep_bluetooth_on_summary,
                icon = R.drawable.ic_bluetooth_searching,
            )
        }

        xmlScreen(
            DeviceSpecificSettingsScreen.BATTERY,
            R.xml.devicesettings_xiaomi_scooter_battery,
            connectedOnly = false,
        )
    }
}
