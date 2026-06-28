package nodomain.freeyourgadget.gadgetbridge.devices.sinilink

import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.deviceName
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.enumList
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.equalizerPreset
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.PasswordMode
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.passwordScreen
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.volume
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.deviceCardAction
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink.SinilinkButton
import nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink.SinilinkEqualizer
import nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink.SinilinkMediaSource
import nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink.SinilinkPlaybackMode
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

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.SPEAKER
    }

    override fun getBatteryCount(device: GBDevice): Int {
        return 0
    }

    override fun getDeviceSettings(device: GBDevice): DeviceSettingsSpec = deviceSettings {
        enumList<SinilinkMediaSource>(
            key = DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE,
            title = R.string.media_source,
            icon = R.drawable.ic_music_note,
            defaultValue = SinilinkMediaSource.BLUETOOTH,
        )
        equalizerPreset<SinilinkEqualizer>(
            defaultValue = SinilinkEqualizer.NORMAL,
        )
        enumList<SinilinkPlaybackMode>(
            key = DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE,
            title = R.string.media_playback_mode,
            icon = R.drawable.ic_play,
            defaultValue = SinilinkPlaybackMode.LIST_CYCLE,
        )
        volume(max = 30, defaultValue = 15)
        switchSetting(
            key = DeviceSettingsPreferenceConst.PREF_PROMPT_TONE,
            title = R.string.sinilink_prompt_tone,
            icon = R.drawable.ic_notifications,
            defaultValue = true,
        )
        deviceName(maxLength = 10)
        passwordScreen(PasswordMode.VISIBLE_NUMBERS_4_DIGITS_0_TO_9)
    }

    override fun getCustomActions(): List<DeviceCardAction> {
        return DEVICE_CARD_ACTIONS
    }

    companion object {
        private val DEVICE_CARD_ACTIONS = listOf(
            DeviceCardAction.forConfiguration(
                R.drawable.ic_skip_previous,
                R.string.pref_media_previous,
                SinilinkButton.PREVIOUS.name
            ),

            deviceCardAction {
                icon = { device ->
                    val state = device.getExtraInfo("playback_state") as? String
                    if (state != null && SinilinkPlaybackState.fromPreference(state) == SinilinkPlaybackState.PLAYING) {
                        R.drawable.ic_pause
                    } else {
                        R.drawable.ic_play
                    }
                }
                description = { _, context -> context.getString(R.string.moondrop_touch_action_play_pause) }
                onClick = { device, _ ->
                    GBApplication.deviceService(device).onSendConfiguration(SinilinkButton.PLAY_PAUSE.name)
                }
            },

            DeviceCardAction.forConfiguration(
                R.drawable.ic_skip_next,
                R.string.pref_media_next,
                SinilinkButton.NEXT.name
            ),
        )
    }
}
