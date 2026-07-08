package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones

import android.app.ProgressDialog
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.enumList
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AutomaticPowerOff
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.ButtonFunctionNcAmbient
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.ButtonModes
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerPreset
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SoundPosition
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatConfig
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SurroundMode
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params.NoiseCancellingOptimizerStatus
import nodomain.freeyourgadget.gadgetbridge.util.Prefs

fun sonyHeadphonesDeviceSettings(
    device: GBDevice,
    capabilities: Set<SonyHeadphonesCapabilities>,
): DeviceSettingsSpec = deviceSettings {

    //
    // Ambient Sound Control
    //

    if (capabilities.contains(SonyHeadphonesCapabilities.AmbientSoundControl) ||
        capabilities.contains(SonyHeadphonesCapabilities.AmbientSoundControl2)
    ) {
        val ambientSoundDefault = if (capabilities.contains(SonyHeadphonesCapabilities.NoNoiseCancelling))
            AmbientSoundControl.Mode.AMBIENT_SOUND
        else
            AmbientSoundControl.Mode.NOISE_CANCELLING

        val isAmbientSoundMode: (Prefs) -> Boolean = { prefs ->
            prefs.getString(
                DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL,
                ambientSoundDefault.name.lowercase()
            ).equals("ambient_sound", ignoreCase = true)
        }

        category(
            key = "pref_header_sony_ambient_sound_control",
            title = R.string.pref_header_sony_ambient_sound_control
        ) {
            enumList<AmbientSoundControl.Mode>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL,
                title = R.string.sony_ambient_sound,
                icon = R.drawable.ic_hearing,
                defaultValue = ambientSoundDefault,
                filter = { mode ->
                    when {
                        capabilities.contains(SonyHeadphonesCapabilities.NoNoiseCancelling) ->
                            mode != AmbientSoundControl.Mode.NOISE_CANCELLING && mode != AmbientSoundControl.Mode.WIND_NOISE_REDUCTION

                        !capabilities.contains(SonyHeadphonesCapabilities.WindNoiseReduction) ->
                            mode != AmbientSoundControl.Mode.WIND_NOISE_REDUCTION

                        else -> true
                    }
                },
            )
            seekbar(
                key = DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL,
                title = R.string.sony_ambient_sound_level,
                icon = R.drawable.ic_volume_up,
                max = 19,
                defaultValue = 0,
                visibleWhen = isAmbientSoundMode,
            )
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE,
                title = R.string.sony_ambient_sound_focus_voice,
                icon = R.drawable.ic_voice,
                defaultValue = true,
                visibleWhen = isAmbientSoundMode,
            )

            //
            // ANC Optimizer
            //

            if (capabilities.contains(SonyHeadphonesCapabilities.AncOptimizer)) {
                var ancOptimizerProgressDialog: ProgressDialog? = null

                category(key = "pref_header_sony_anc_optimizer", title = R.string.pref_header_sony_anc_optimizer) {
                    text(
                        key = DeviceSettingsPreferenceConst.PREF_SONY_NOISE_OPTIMIZER_STATE_PRESSURE,
                        title = R.string.pref_anc_optimizer_state_pressure,
                        icon = R.drawable.ic_pressure,
                        enabled = false,
                        connectedOnly = false,
                    )

                    action(
                        key = "pref_sony_anc_optimizer",
                        title = R.string.sony_anc_optimize_title,
                        summary = R.string.sony_anc_optimize_description,
                        icon = R.drawable.ic_auto_awesome,
                        onClick = { handler ->
                            if (ancOptimizerProgressDialog != null) return@action true
                            val context = handler.context
                            MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.sony_anc_optimize_confirmation_title)
                                .setMessage(R.string.sony_anc_optimize_confirmation_description)
                                .setIcon(R.drawable.ic_hearing)
                                .setPositiveButton(R.string.start) { _, _ ->
                                    handler.notifyPreferenceChanged(DeviceSettingsPreferenceConst.PREF_SONY_NOISE_OPTIMIZER_START)
                                    ancOptimizerProgressDialog = ProgressDialog(context).apply {
                                        setCancelable(false)
                                        setMessage(context.getString(R.string.sony_anc_optimizer_status_starting))
                                        setProgressStyle(ProgressDialog.STYLE_SPINNER)
                                        progress = 0
                                        setButton(
                                            DialogInterface.BUTTON_NEGATIVE,
                                            context.getString(R.string.cancel)
                                        ) { d, _ ->
                                            d.dismiss()
                                            ancOptimizerProgressDialog = null
                                            handler.notifyPreferenceChanged(DeviceSettingsPreferenceConst.PREF_SONY_NOISE_OPTIMIZER_CANCEL)
                                        }
                                        show()
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                            true
                        },
                    )

                    // Hidden preference - status written by the service to drive the progress dialog
                    text(
                        key = DeviceSettingsPreferenceConst.PREF_SONY_NOISE_OPTIMIZER_STATUS,
                        title = R.string.sony_anc_optimize_title,
                        connectedOnly = false,
                        visibleWhen = { false },
                        onSharedPreferenceChanged = { value ->
                            val dialog = ancOptimizerProgressDialog
                            if (dialog != null) {
                                try {
                                    when (val status = NoiseCancellingOptimizerStatus.valueOf(value.uppercase())) {
                                        NoiseCancellingOptimizerStatus.FINISHED,
                                        NoiseCancellingOptimizerStatus.NOT_RUNNING -> {
                                            dialog.dismiss()
                                            ancOptimizerProgressDialog = null
                                        }

                                        else -> dialog.setMessage(status.i18n(dialog.context))
                                    }
                                } catch (_: IllegalArgumentException) {
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    //
    // Adaptive Volume Control
    //

    if (capabilities.contains(SonyHeadphonesCapabilities.AdaptiveVolumeControl)) {
        category(key = "pref_header_sony_sound_control", title = R.string.pref_header_sony_sound_control) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_ADAPTIVE_VOLUME_CONTROL,
                title = R.string.pref_adaptive_volume_control_title,
                summary = R.string.pref_adaptive_volume_control_summary,
                icon = R.drawable.ic_hearing,
                defaultValue = false,
            )
        }
    }

    //
    // Speak to Chat
    //

    if (capabilities.contains(SonyHeadphonesCapabilities.SpeakToChatEnabled)) {
        switchSetting(
            key = DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT,
            title = R.string.sony_speak_to_chat,
            summary = R.string.sony_speak_to_chat_summary,
            icon = R.drawable.ic_voice,
            defaultValue = false,
        )
    } else if (capabilities.contains(SonyHeadphonesCapabilities.SpeakToChatConfig)) {
        screen(
            key = "pref_sony_speak_to_chat_header",
            title = R.string.sony_speak_to_chat,
            summary = R.string.sony_speak_to_chat_summary,
            icon = R.drawable.ic_voice,
        ) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT,
                title = R.string.sony_speak_to_chat,
                summary = R.string.sony_speak_to_chat_summary,
                icon = R.drawable.ic_voice,
                defaultValue = false,
            )
            enumList<SpeakToChatConfig.Sensitivity>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_SENSITIVITY,
                title = R.string.sony_speak_to_chat_sensitivity,
                defaultValue = SpeakToChatConfig.Sensitivity.AUTO,
                dependency = DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT,
            )
            if (capabilities.contains(SonyHeadphonesCapabilities.SpeakToChatFocusOnVoice)) {
                switchSetting(
                    key = DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_FOCUS_ON_VOICE,
                    title = R.string.sony_speak_to_chat_focus_on_voice,
                    icon = R.drawable.ic_voice,
                    dependency = DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT,
                )
            }
            enumList<SpeakToChatConfig.Timeout>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_TIMEOUT,
                title = R.string.sony_speak_to_chat_timeout,
                icon = R.drawable.ic_timer,
                defaultValue = SpeakToChatConfig.Timeout.STANDARD,
                dependency = DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT,
            )
        }
    }

    //
    // Audio settings / Other
    //

    category(key = "pref_header_other", title = R.string.pref_header_other) {
        // When AudioSettingsOnlyOnSbcCodec, equalizer/sound position/surround are only usable
        // when the device is actually using SBC (HD audio forces a non-SBC codec). Use the
        // reported codec pref rather than the HD audio user preference, as the two diverge
        // between user toggling HD and the next reconnect.
        val audioSettingsVisibleWhen: ((Prefs) -> Boolean)? =
            if (capabilities.contains(SonyHeadphonesCapabilities.AudioSettingsOnlyOnSbcCodec)) {
                { prefs ->
                    "sbc".equals(
                        prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_CODEC, "sbc"),
                        ignoreCase = true
                    )
                }
            } else {
                null
            }

        if (capabilities.contains(SonyHeadphonesCapabilities.AudioLDAC)) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_HD,
                title = R.string.sony_audio_hd,
                summary = R.string.sony_audio_hd_summary,
                icon = R.drawable.ic_extension,
                defaultValue = false,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.AudioSettingsOnlyOnSbcCodec)) {
            action(
                key = "pref_sony_warning_sbc_codec_1",
                summary = R.string.sony_warn_sbc_codec,
                icon = R.drawable.ic_warning,
                connectedOnly = false,
                visibleWhen = { prefs -> prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_HD, false) },
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.EqualizerSimple)) {
            enumList<EqualizerPreset>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE,
                title = R.string.sony_equalizer,
                icon = R.drawable.ic_graphic_eq,
                defaultValue = EqualizerPreset.OFF,
                visibleWhen = audioSettingsVisibleWhen,
            )
        } else if (capabilities.contains(SonyHeadphonesCapabilities.EqualizerWithCustomBands)) {
            screen(
                key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER,
                title = R.string.pref_header_equalizer,
                icon = R.drawable.ic_graphic_eq,
                connectedOnly = true,
                visibleWhen = audioSettingsVisibleWhen,
            ) {
                enumList<EqualizerPreset>(
                    key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE,
                    title = R.string.sony_equalizer,
                    icon = R.drawable.ic_graphic_eq,
                    defaultValue = EqualizerPreset.OFF,
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_400,
                    title = R.string.sony_equalizer_band_400,
                    icon = R.drawable.ic_graphic_eq,
                    max = 20,
                    defaultValue = 10
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_1000,
                    title = R.string.sony_equalizer_band_1000,
                    icon = R.drawable.ic_graphic_eq,
                    max = 20,
                    defaultValue = 10
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_2500,
                    title = R.string.sony_equalizer_band_2500,
                    icon = R.drawable.ic_graphic_eq,
                    max = 20,
                    defaultValue = 10
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_6300,
                    title = R.string.sony_equalizer_band_6300,
                    icon = R.drawable.ic_graphic_eq,
                    max = 20,
                    defaultValue = 10
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_16000,
                    title = R.string.sony_equalizer_band_16000,
                    icon = R.drawable.ic_graphic_eq,
                    max = 20,
                    defaultValue = 10
                )
                seekbar(
                    key = DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BASS,
                    title = R.string.sony_equalizer_clear_bass,
                    icon = R.drawable.ic_speaker,
                    max = 20,
                    defaultValue = 10
                )
            }
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.SoundPosition)) {
            enumList<SoundPosition>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_SOUND_POSITION,
                title = R.string.sony_sound_position,
                icon = R.drawable.ic_switch_left,
                defaultValue = SoundPosition.OFF,
                visibleWhen = audioSettingsVisibleWhen,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.SurroundMode)) {
            enumList<SurroundMode>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE,
                title = R.string.sony_surround_mode,
                icon = R.drawable.ic_surround,
                defaultValue = SurroundMode.OFF,
                visibleWhen = audioSettingsVisibleWhen,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.AudioUpsampling)) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_UPSAMPLING,
                title = R.string.sony_audio_upsampling,
                icon = R.drawable.ic_extension,
                defaultValue = false,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.Volume)) {
            seekbar(
                key = DeviceSettingsPreferenceConst.PREF_VOLUME,
                title = R.string.menuitem_volume,
                icon = R.drawable.ic_volume_up,
                max = 30,
                defaultValue = 15,
            )
        }
    }

    //
    // Calls & Notifications
    //

    xmlScreen(
        DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS,
        R.xml.devicesettings_headphones,
        connectedOnly = false,
    )

    //
    // System
    //

    category(key = "pref_header_system", title = R.string.pref_header_system) {
        if (capabilities.contains(SonyHeadphonesCapabilities.ConnectTwoDevices)) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_CONNECT_TWO_DEVICES,
                title = R.string.dual_device_mode_title,
                summary = R.string.dual_device_mode_summary,
                icon = R.drawable.ic_devices_other,
                defaultValue = false,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.WideAreaTap)) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_WIDE_AREA_TAP,
                title = R.string.pref_wide_area_tap_title,
                summary = R.string.pref_wide_area_tap_summary,
                icon = R.drawable.ic_touch,
                defaultValue = false,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.ButtonModesLeftRight)) {
            screen(
                key = "pref_screen_sony_button_mode_help",
                title = R.string.sony_button_mode_help_title,
                summary = R.string.sony_button_mode_help_summary,
                icon = R.drawable.ic_help_outline,
                connectedOnly = false,
            ) {
                category(
                    key = "pref_button_mode_help_ambient_sound_control_header",
                    title = R.string.sony_button_mode_ambient_sound_control
                ) {
                    action(
                        key = "pref_button_mode_help_ambient_sound_single_tap",
                        title = R.string.single_tap,
                        summary = R.string.pref_switch_controls_anc_ambient_off,
                        icon = R.drawable.ic_filter_1,
                        connectedOnly = false
                    )
                    action(
                        key = "pref_button_mode_help_ambient_sound_continue_pressing",
                        title = R.string.continue_pressing,
                        summary = R.string.quick_attention,
                        icon = R.drawable.ic_horizontal_rule,
                        connectedOnly = false
                    )
                }
                category(
                    key = "pref_button_mode_help_playback_control_header",
                    title = R.string.sony_button_mode_playback_control
                ) {
                    action(
                        key = "pref_button_mode_help_playback_control_single_tap",
                        title = R.string.single_tap,
                        summary = R.string.pref_media_playpause,
                        icon = R.drawable.ic_filter_1,
                        connectedOnly = false
                    )
                    action(
                        key = "pref_button_mode_help_playback_control_double_tap",
                        title = R.string.double_tap,
                        summary = R.string.pref_media_next,
                        icon = R.drawable.ic_filter_2,
                        connectedOnly = false
                    )
                    action(
                        key = "pref_button_mode_help_playback_control_triple_tap",
                        title = R.string.triple_tap,
                        summary = R.string.pref_media_previous,
                        icon = R.drawable.ic_filter_3,
                        connectedOnly = false
                    )
                    action(
                        key = "pref_button_mode_help_playback_control_long_press",
                        title = R.string.long_press,
                        summary = R.string.pref_title_touch_voice_assistant,
                        icon = R.drawable.ic_horizontal_rule,
                        connectedOnly = false
                    )
                }
                category(
                    key = "pref_button_mode_help_volume_control_header",
                    title = R.string.sony_button_mode_volume_control
                ) {
                    action(
                        key = "pref_button_mode_help_volume_control_single_tap",
                        title = R.string.single_tap,
                        summary = R.string.pref_media_volumeup,
                        icon = R.drawable.ic_filter_1,
                        connectedOnly = false
                    )
                    action(
                        key = "pref_button_mode_help_volume_control_continue_pressing",
                        title = R.string.continue_pressing,
                        summary = R.string.pref_media_volumedown,
                        icon = R.drawable.ic_horizontal_rule,
                        connectedOnly = false
                    )
                }
            }
            enumList<ButtonModes.Mode>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_MODE_LEFT,
                title = R.string.sony_button_mode_left,
                icon = R.drawable.ic_touch,
                defaultValue = ButtonModes.Mode.OFF,
            )
            enumList<ButtonModes.Mode>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_MODE_RIGHT,
                title = R.string.sony_button_mode_right,
                icon = R.drawable.ic_touch,
                defaultValue = ButtonModes.Mode.OFF,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.AmbientSoundControlButtonMode)) {
            enumList<AmbientSoundControlButtonMode>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE,
                title = R.string.sony_ambient_sound_control_button_modes,
                icon = R.drawable.ic_touch,
                defaultValue = AmbientSoundControlButtonMode.NC_AS_OFF,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.QuickAccess)) {
            enumList<QuickAccess.Mode>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_DOUBLE_TAP,
                title = R.string.sony_quick_access_double_tap,
                icon = R.drawable.ic_filter_2,
                defaultValue = QuickAccess.Mode.OFF,
            )
            enumList<QuickAccess.Mode>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_TRIPLE_TAP,
                title = R.string.sony_quick_access_triple_tap,
                icon = R.drawable.ic_filter_3,
                defaultValue = QuickAccess.Mode.OFF,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.ButtonFunctionNcAmbient)) {
            enumList<ButtonFunctionNcAmbient.Mode>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_FUNCTION_NC_AMBIENT,
                title = R.string.sony_button_function_nc_ambient,
                icon = R.drawable.ic_touch,
                defaultValue = ButtonFunctionNcAmbient.Mode.SWITCH_AMBIENT_SOUND,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.TouchSensorSingle)) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_TOUCH_SENSOR,
                title = R.string.sony_touch_sensor,
                icon = R.drawable.ic_touch,
                defaultValue = true,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.PauseWhenTakenOff)) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_PAUSE_WHEN_TAKEN_OFF,
                title = R.string.sony_pause_when_taken_off,
                icon = R.drawable.ic_pause,
                defaultValue = false,
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.AutomaticPowerOffWhenTakenOff)) {
            enumList<AutomaticPowerOff>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_AUTOMATIC_POWER_OFF,
                title = R.string.sony_automatic_power_off,
                icon = R.drawable.ic_power_settings_new,
                defaultValue = AutomaticPowerOff.OFF,
                filter = { it in setOf(AutomaticPowerOff.OFF, AutomaticPowerOff.WHEN_TAKEN_OFF) },
            )
        }
        if (capabilities.contains(SonyHeadphonesCapabilities.AutomaticPowerOffByTime)) {
            enumList<AutomaticPowerOff>(
                key = DeviceSettingsPreferenceConst.PREF_SONY_AUTOMATIC_POWER_OFF,
                title = R.string.sony_automatic_power_off,
                icon = R.drawable.ic_power_settings_new,
                defaultValue = AutomaticPowerOff.OFF,
                filter = { it != AutomaticPowerOff.WHEN_TAKEN_OFF },
            )
        }

        if (capabilities.contains(SonyHeadphonesCapabilities.VoiceNotifications)) {
            switchSetting(
                key = DeviceSettingsPreferenceConst.PREF_SONY_NOTIFICATION_VOICE_GUIDE,
                title = R.string.sony_notification_voice_guide,
                icon = R.drawable.ic_notifications,
                defaultValue = true,
            )
        }
    }

    //
    // Developer settings
    //

    xmlScreen(
        DeviceSpecificSettingsScreen.DEVELOPER,
        R.xml.devicesettings_override_features,
        R.xml.devicesettings_sony_headphones_protocol_version,
        R.xml.devicesettings_sony_headphones_device_info,
    )
}
