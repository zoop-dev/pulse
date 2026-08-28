/*  Copyright (C) 2026 David Giron

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
package nodomain.freeyourgadget.gadgetbridge.devices.jabra

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ListEntry
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.deviceName
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.multipointPairing
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.deviceCardAction
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.jabra.JabraEvolve255Support
import java.util.regex.Pattern

class JabraEvolve255Coordinator : AbstractBLClassicDeviceCoordinator() {

    override fun getSupportedDeviceName(): Pattern {
        return Pattern.compile("Jabra Evolve2 55")
    }

    override fun getManufacturer(): String {
        return "Jabra"
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport?> {
        return JabraEvolve255Support::class.java
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_jabra_evolve2_55
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_headphones
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.HEADPHONES
    }

    override fun getBatteryConfig(device: GBDevice): Array<BatteryConfig> {
        return arrayOf(
            BatteryConfig(
                0,
                GBDevice.BATTERY_ICON_DEFAULT.toInt(),
                GBDevice.BATTERY_LABEL_DEFAULT.toInt(),
                15,
                100
            )
        )
    }

    override fun getBondingStyle(): Int {
        return BONDING_STYLE_LAZY
    }

    override fun getCustomActions(): List<DeviceCardAction> {
        return DEVICE_CARD_ACTIONS
    }

    override fun getDeviceSettings(device: GBDevice): DeviceSettingsSpec = deviceSettings {
        switchSetting(
            key = DeviceSettingsPreferenceConst.PREF_ACTIVE_NOISE_CANCELLING_TOGGLE,
            title = R.string.prefs_active_noise_cancelling,
            summary = R.string.prefs_active_noise_cancelling_summary,
            icon = R.drawable.ic_hearing,
            defaultValue = false,
        )
        switchSetting(
            key = DeviceSettingsPreferenceConst.PREF_BUSYLIGHT,
            title = R.string.prefs_busylight,
            summary = R.string.prefs_busylight_summary,
            icon = R.drawable.ic_dnd,
            defaultValue = false,
        )
        switchSetting(
            key = DeviceSettingsPreferenceConst.PREF_BUSYLIGHT_ON_CALL,
            title = R.string.prefs_busylight_on_call,
            summary = R.string.prefs_busylight_on_call_summary,
            icon = R.drawable.ic_dnd,
            defaultValue = false,
        )
        switchSetting(
            key = DeviceSettingsPreferenceConst.PREF_JABRA_BUTTON_SOUNDS,
            title = R.string.prefs_jabra_button_sounds,
            summary = R.string.prefs_jabra_button_sounds_summary,
            icon = R.drawable.ic_volume_up,
            defaultValue = false,
        )
        list(
            key = DeviceSettingsPreferenceConst.PREF_JABRA_SLEEP_MODE,
            title = R.string.soundcore_auto_power_off_title,
            icon = R.drawable.ic_timer,
            entries = listOf(
                ListEntry.Res("15", R.string.minutes_15),
                ListEntry.Res("30", R.string.minutes_30),
                ListEntry.Res("60", R.string.minutes_60),
                ListEntry.Res("120", R.string.minutes_120),
                ListEntry.Res("240", R.string.minutes_240),
                ListEntry.Res("360", R.string.minutes_360),
                ListEntry.Res("480", R.string.minutes_480),
                ListEntry.Res("0", R.string.off),
            ),
            defaultValue = "15",
        )
        list(
            key = DeviceSettingsPreferenceConst.PREF_JABRA_HEADSET_GUIDANCE,
            title = R.string.prefs_jabra_headset_guidance,
            icon = R.drawable.ic_voice,
            entries = listOf(
                ListEntry.Res(JabraEvolve255Support.GUIDANCE_VOICE, R.string.prefs_jabra_guidance_voice),
                ListEntry.Res(JabraEvolve255Support.GUIDANCE_TONE, R.string.prefs_jabra_guidance_tone),
                ListEntry.Res(JabraEvolve255Support.GUIDANCE_NONE, R.string.none),
            ),
            defaultValue = JabraEvolve255Support.GUIDANCE_VOICE,
        )
        list(
            key = DeviceSettingsPreferenceConst.PREF_JABRA_BOOM_ARM_GUIDANCE,
            title = R.string.prefs_jabra_boom_arm_guidance,
            icon = R.drawable.ic_microphone,
            entries = listOf(
                ListEntry.Res(JabraEvolve255Support.GUIDANCE_VOICE, R.string.prefs_jabra_guidance_voice),
                ListEntry.Res(JabraEvolve255Support.GUIDANCE_TONE, R.string.prefs_jabra_guidance_tone),
                ListEntry.Res(JabraEvolve255Support.GUIDANCE_NONE, R.string.none),
            ),
            defaultValue = JabraEvolve255Support.GUIDANCE_TONE,
        )

        list(
            key = DeviceSettingsPreferenceConst.PREF_JABRA_VOICE_ASSISTANT,
            title = R.string.sony_voice_assistant_function,
            icon = R.drawable.ic_voice,
            entries = listOf(
                ListEntry.Res(JabraEvolve255Support.VOICE_ASSISTANT_MOBILE, R.string.sony_voice_assistant_mobile_device),
                ListEntry.Res(JabraEvolve255Support.VOICE_ASSISTANT_ALEXA, R.string.menuitem_alexa),
            ),
            defaultValue = JabraEvolve255Support.VOICE_ASSISTANT_MOBILE,
        )

        screen(
            key = "jabra_equalizer_screen",
            title = R.string.prefs_equalizer,
            icon = R.drawable.ic_equalizer,
        ) {
            list(
                key = DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER,
                title = R.string.prefs_equalizer,
                icon = R.drawable.ic_equalizer,
                entries = listOf(
                    ListEntry.Res(JabraEvolve255Support.EQ_NEUTRAL, R.string.off),
                    ListEntry.Res(JabraEvolve255Support.EQ_SPEECH, R.string.sony_equalizer_preset_speech),
                    ListEntry.Res(JabraEvolve255Support.EQ_BASS, R.string.pref_title_equalizer_bass_boost),
                    ListEntry.Res(JabraEvolve255Support.EQ_TREBLE, R.string.pref_title_equalizer_trebble),
                    ListEntry.Res(JabraEvolve255Support.EQ_SMOOTH, R.string.pref_title_equalizer_smooth),
                    ListEntry.Res(JabraEvolve255Support.EQ_ENERGIZE, R.string.pref_title_equalizer_energize),
                    ListEntry.Res(JabraEvolve255Support.EQ_CUSTOM, R.string.nothing_equalizer_custom),
                ),
                defaultValue = JabraEvolve255Support.EQ_NEUTRAL,
            )
            category(
                key = "jabra_equalizer_bands",
                title = R.string.prefs_equalizer,
                visibleWhen = { prefs ->
                    prefs.getString(DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER, JabraEvolve255Support.EQ_NEUTRAL) == JabraEvolve255Support.EQ_CUSTOM
                },
            ) {
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND1,
                    title = R.string.jabra_equalizer_band_60,
                    icon = R.drawable.ic_graphic_eq,
                    max = 12,
                    defaultValue = 6,
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND2,
                    title = R.string.redmi_buds_5_pro_equalizer_band_250,
                    icon = R.drawable.ic_graphic_eq,
                    max = 12,
                    defaultValue = 6,
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND3,
                    title = R.string.redmi_buds_5_pro_equalizer_band_1k,
                    icon = R.drawable.ic_graphic_eq,
                    max = 12,
                    defaultValue = 6,
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND4,
                    title = R.string.redmi_buds_5_pro_equalizer_band_4k,
                    icon = R.drawable.ic_graphic_eq,
                    max = 12,
                    defaultValue = 6,
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND5,
                    title = R.string.jabra_equalizer_band_7600,
                    icon = R.drawable.ic_graphic_eq,
                    max = 12,
                    defaultValue = 6,
                )
            }
        }

        screen(
            key = DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS.key,
            title = R.string.pref_header_calls_and_notifications,
            icon = R.drawable.ic_notifications,
        ) {
            category(
                key = "incoming_calls",
                title = R.string.prefs_incoming_calls,
            ) {
                switchSetting(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_ANSWER_CALL_BOOM_ARM,
                    title = R.string.prefs_jabra_answer_call_boom_arm,
                    summary = R.string.prefs_jabra_answer_call_boom_arm_summary,
                    defaultValue = false,
                )
                switchSetting(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_AUTO_REJECT_CALL,
                    title = R.string.prefs_jabra_auto_reject_call,
                    summary = R.string.prefs_jabra_auto_reject_call_summary,
                    defaultValue = false,
                )
            }
            category(
                key = "mute_unmute",
                title = R.string.prefs_jabra_mute_unmute,
            ) {
                switchSetting(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_MUTE_MIC_BOOM_ARM,
                    title = R.string.prefs_jabra_mute_mic_boom_arm,
                    summary = R.string.prefs_jabra_mute_mic_boom_arm_summary,
                    defaultValue = false,
                )
                switchSetting(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_MUTE_REMINDER,
                    title = R.string.prefs_jabra_mute_reminder,
                    summary = R.string.prefs_jabra_mute_reminder_summary,
                    defaultValue = false,
                )
            }
            category(
                key = "active_calls",
                title = R.string.prefs_active_calls,
            ) {
                switchSetting(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_SIDETONE,
                    title = R.string.prefs_jabra_sidetone,
                    summary = R.string.prefs_jabra_sidetone_summary,
                    defaultValue = false,
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_SIDETONE_VOLUME,
                    title = R.string.pref_title_touch_volume,
                    max = 5,
                    defaultValue = 3,
                    dependency = DeviceSettingsPreferenceConst.PREF_JABRA_SIDETONE,
                    visibleWhen = { prefs -> prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_JABRA_SIDETONE, false) },
                )
            }
            category(
                key = "call_quality",
                title = R.string.prefs_call_quality,
            ) {
                list(
                    key = DeviceSettingsPreferenceConst.PREF_JABRA_CALL_AUDIO_EQ,
                    title = R.string.prefs_jabra_call_audio_eq,
                    entries = listOf(
                        ListEntry.Res(JabraEvolve255Support.CALL_AUDIO_EQ_NEUTRAL, R.string.pref_title_equalizer_normal),
                        ListEntry.Res(JabraEvolve255Support.CALL_AUDIO_EQ_TREBLE, R.string.pref_title_equalizer_trebble),
                        ListEntry.Res(JabraEvolve255Support.CALL_AUDIO_EQ_BASS, R.string.pref_title_equalizer_bass_boost),
                    ),
                    defaultValue = JabraEvolve255Support.CALL_AUDIO_EQ_NEUTRAL,
                )
            }
        }
        screen(
            key = DeviceSpecificSettingsScreen.CONNECTION.key,
            title = R.string.pref_header_connection,
            icon = R.drawable.ic_mtu,
        ) {
            deviceName()
            multipointPairing()
        }
    }

    companion object {
        private val DEVICE_CARD_ACTIONS = listOf(
            deviceCardAction {
                icon = { device ->
                    val state = GBApplication.getDevicePrefs(device)
                        .getBoolean(DeviceSettingsPreferenceConst.PREF_ACTIVE_NOISE_CANCELLING_TOGGLE, false)
                    if (state) R.drawable.ic_hearing else R.drawable.ic_hearing_disabled
                }
                description = { _, context -> context.getString(R.string.prefs_active_noise_cancelling) }
                onClick = { device, context ->
                    val prefs = GBApplication.getDeviceSpecificSharedPrefs(device.address)
                    val newState = !prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_ACTIVE_NOISE_CANCELLING_TOGGLE, false)
                    prefs.edit().putBoolean(DeviceSettingsPreferenceConst.PREF_ACTIVE_NOISE_CANCELLING_TOGGLE, newState).apply()
                    GBApplication.deviceService(device).onSendConfiguration(DeviceSettingsPreferenceConst.PREF_ACTIVE_NOISE_CANCELLING_TOGGLE)
                }
            },
            deviceCardAction {
                icon = { device ->
                    val state = GBApplication.getDevicePrefs(device)
                        .getBoolean(DeviceSettingsPreferenceConst.PREF_BUSYLIGHT, false)
                    if (state) R.drawable.ic_dnd else R.drawable.ic_dnd_disabled
                }
                description = { _, context -> context.getString(R.string.prefs_busylight) }
                onClick = { device, context ->
                    val prefs = GBApplication.getDeviceSpecificSharedPrefs(device.address)
                    val newState = !prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_BUSYLIGHT, false)
                    prefs.edit().putBoolean(DeviceSettingsPreferenceConst.PREF_BUSYLIGHT, newState).apply()
                    GBApplication.deviceService(device).onSendConfiguration(DeviceSettingsPreferenceConst.PREF_BUSYLIGHT)
                }
            },
        )
    }
}
