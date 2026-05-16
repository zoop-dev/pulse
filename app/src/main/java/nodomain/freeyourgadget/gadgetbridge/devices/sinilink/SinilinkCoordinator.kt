package nodomain.freeyourgadget.gadgetbridge.devices.sinilink

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink.SinilinkButton
import nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink.SinilinkPlaybackState
import nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink.SinilinkSupport
import java.util.regex.Pattern

class SinilinkCoordinator : AbstractBLEDeviceCoordinator() {
    override fun getSupportedDeviceName(): Pattern? {
        return Pattern.compile("^Sinilink-APP$")
    }

    override fun getManufacturer(): String {
        return "Xinyi Electronics"
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport?> {
        return SinilinkSupport::class.java
    }

    override fun getBondingStyle(): Int {
        // Does not seem to be needed?
        return BONDING_STYLE_ASK
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_sinilink
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_speaker
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind? {
        return DeviceCoordinator.DeviceKind.SPEAKER
    }

    override fun getBatteryCount(device: GBDevice): Int {
        return 0
    }

    override fun getDeviceSpecificSettings(device: GBDevice): DeviceSpecificSettings {
        val settings = DeviceSpecificSettings()

        settings.addRootScreen(R.xml.devicesettings_sinilink)
        settings.addRootScreen(R.xml.devicesettings_device_name)
        settings.addRootScreen(R.xml.devicesettings_password)

        settings.addConnectedPreferences(
            DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE,
            DeviceSettingsPreferenceConst.PREF_HEADPHONES_EQUALIZER,
            DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE,
            DeviceSettingsPreferenceConst.PREF_VOLUME,
            DeviceSettingsPreferenceConst.PREF_PROMPT_TONE,
            DeviceSettingsPreferenceConst.PREF_DEVICE_NAME,
            PasswordCapabilityImpl.PREF_SCREEN_PASSWORD,
            PasswordCapabilityImpl.PREF_PASSWORD_ENABLED,
            PasswordCapabilityImpl.PREF_PASSWORD,
        )

        return settings
    }

    override fun getDeviceSpecificSettingsCustomizer(device: GBDevice): DeviceSpecificSettingsCustomizer {
        return SinilinkSettingsCustomizer()
    }

    override fun getPasswordCapability(): PasswordCapabilityImpl.Mode {
        return PasswordCapabilityImpl.Mode.VISIBLE_NUMBERS_4_DIGITS_0_TO_9
    }

    override fun getCustomActions(): List<DeviceCardAction> {
        return DEVICE_CARD_ACTIONS
    }

    companion object {
        private val DEVICE_CARD_ACTIONS = listOf(
            // previous
            object : DeviceCardAction {
                override fun getIcon(device: GBDevice): Int {
                    return R.drawable.ic_skip_previous
                }

                override fun getDescription(device: GBDevice, context: Context): String {
                    return context.getString(R.string.pref_media_previous)
                }

                override fun onClick(device: GBDevice, context: Context) {
                    GBApplication.deviceService(device).onSendConfiguration(SinilinkButton.PREVIOUS.name)
                }
            },

            // play / pause
            object : DeviceCardAction {
                override fun getIcon(device: GBDevice): Int {
                    return when (SinilinkPlaybackState.fromPreference(device.getExtraInfo("playback_state") as? String ?: return R.drawable.ic_play)) {
                        SinilinkPlaybackState.PLAYING -> R.drawable.ic_pause
                        else -> R.drawable.ic_play
                    }
                }

                override fun getDescription(device: GBDevice, context: Context): String {
                    return context.getString(R.string.moondrop_touch_action_play_pause)
                }

                override fun onClick(device: GBDevice, context: Context) {
                    GBApplication.deviceService(device).onSendConfiguration(SinilinkButton.PLAY_PAUSE.name)
                }
            },

            // next
            object : DeviceCardAction {
                override fun getIcon(device: GBDevice): Int {
                    return R.drawable.ic_skip_next
                }

                override fun getDescription(device: GBDevice, context: Context): String {
                    return context.getString(R.string.pref_media_next)
                }

                override fun onClick(device: GBDevice, context: Context) {
                    GBApplication.deviceService(device).onSendConfiguration(SinilinkButton.NEXT.name)
                }
            }
        )
    }
}
