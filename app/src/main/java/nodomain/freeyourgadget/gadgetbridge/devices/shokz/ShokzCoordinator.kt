package nodomain.freeyourgadget.gadgetbridge.devices.shokz

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.shokz.ShokzSupport

abstract class ShokzCoordinator : AbstractBLClassicDeviceCoordinator() {
    override fun getManufacturer(): String? {
        return "Shokz"
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport?> {
        return ShokzSupport::class.java
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun getDefaultIconResource(): Int {
        // TODO dedicated icon
        return R.drawable.ic_device_headphones
    }

    override fun getSupportedLanguageSettings(device: GBDevice): Array<out String>? {
        return arrayOf(
            "en",
            "zh",
            "ja",
            "ko",
        )
    }

    override fun getDeviceSpecificSettings(device: GBDevice): DeviceSpecificSettings {
        val settings = DeviceSpecificSettings()

        settings.addRootScreen(R.xml.devicesettings_multipoint)
        settings.addRootScreen(R.xml.devicesettings_media_source)
        settings.addRootScreen(R.xml.devicesettings_shokz_equalizer)
        settings.addRootScreen(R.xml.devicesettings_playback_mode)

        settings.addRootScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS)
        settings.addSubScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS, R.xml.devicesettings_shokz_controls)

        settings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS)
        settings.addSubScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS, R.xml.devicesettings_headphones)

        settings.addConnectedPreferences(
            DeviceSettingsPreferenceConst.PREF_MULTIPOINT,
            DeviceSettingsPreferenceConst.PREF_LANGUAGE,
            DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE,
            DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_BLUETOOTH,
            DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_MP3,
            DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE,
            DeviceSpecificSettingsScreen.TOUCH_OPTIONS.key,
            DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_LONG_PRESS_MULTI_FUNCTION,
            DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_SIMULTANEOUS_VOLUME_UP_DOWN,
        )

        return settings
    }

    override fun getDeviceSpecificSettingsCustomizer(device: GBDevice): DeviceSpecificSettingsCustomizer? {
        return ShokzSettingsCustomizer()
    }
}
