/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.aawireless

import android.text.InputType
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.aawireless.AAWirelessPairedPhonesActivity
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ListEntry
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.aawireless.AAWirelessPrefs
import nodomain.freeyourgadget.gadgetbridge.service.devices.aawireless.AAWirelessSupport
import nodomain.freeyourgadget.gadgetbridge.util.preferences.CountryCodeTextWatcher
import nodomain.freeyourgadget.gadgetbridge.util.preferences.MinMaxTextWatcher
import java.util.regex.Pattern

class AAWirelessCoordinator : AbstractBLClassicDeviceCoordinator() {
    protected override fun getSupportedDeviceName(): Pattern {
        // AAW 1 - AAWireless-xUkL1YH0
        // AAW 2 - Normal mode: AAWireless-12345abc
        // AAW 2 - Dongle mode: AndroidAuto-AAW12345abc
        return Pattern.compile("^(AAWireless-|AndroidAuto-AAW)[0-9a-zA-Z]+$")
    }

    override fun getManufacturer(): String {
        return "AAWireless"
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport> {
        return AAWirelessSupport::class.java
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_aawireless
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_car
    }

    override fun getBatteryCount(device: GBDevice): Int {
        return 0
    }

    override fun getBatteryConfig(device: GBDevice): Array<BatteryConfig> {
        return arrayOf()
    }

    override fun getDeviceSettings(device: GBDevice): DeviceSettingsSpec = deviceSettings {
        externalSettings(
            key = AAWirelessPrefs.PREF_SCREEN_PAIRED_PHONES,
            title = R.string.paired_phones,
            icon = R.drawable.ic_smartphone,
            connectedOnly = false,
            activityClass = AAWirelessPairedPhonesActivity::class.java,
        )

        text(
            key = DeviceSettingsPreferenceConst.PREF_COUNTRY,
            title = R.string.country,
            icon = R.drawable.ic_language,
            defaultValue = "US",
            onBindEditText = { editText ->
                editText.addTextChangedListener(CountryCodeTextWatcher(editText))
                editText.setSelection(editText.text?.length ?: 0)
            },
        )

        screen(
            key = "screen_button_actions",
            title = R.string.mi2_prefs_button_actions,
            summary = R.string.mi2_prefs_button_actions_summary,
            icon = R.drawable.ic_touch,
            visibleWhen = { it.getBoolean(AAWirelessPrefs.PREF_HAS_BUTTON, false) },
        ) {
            list(
                key = AAWirelessPrefs.PREF_BUTTON_MODE_SINGLE_CLICK,
                title = R.string.single_click,
                icon = R.drawable.ic_filter_1,
                defaultValue = AAWirelessPrefs.BUTTON_MODE_NONE,
                entriesProvider = { buttonModeEntries(device) },
            )
            list(
                key = AAWirelessPrefs.PREF_BUTTON_MODE_DOUBLE_CLICK,
                title = R.string.double_click,
                icon = R.drawable.ic_filter_2,
                defaultValue = AAWirelessPrefs.BUTTON_MODE_NONE,
                entriesProvider = { buttonModeEntries(device) },
            )
            action(
                key = "button_mode_long_press_2s",
                title = R.string.long_press_2_seconds,
                summary = R.string.pairing_mode,
                icon = R.drawable.ic_horizontal_rule,
                enabled = false,
                connectedOnly = false,
            )
            action(
                key = "button_mode_long_press_10s",
                title = R.string.long_press_10_seconds,
                summary = R.string.factory_reset,
                icon = R.drawable.ic_horizontal_rule,
                enabled = false,
                connectedOnly = false,
            )
        }

        screen(
            key = DeviceSpecificSettingsScreen.CONNECTION.key,
            title = R.string.pref_header_connection,
            icon = R.drawable.ic_mtu,
        ) {
            list(
                key = DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY,
                title = R.string.wifi_frequency,
                icon = R.drawable.ic_wifi_tethering,
                entriesRes = R.array.wifi_frequency_names,
                entryValuesRes = R.array.wifi_frequency_values,
                defaultValue = "5",
            )
            list(
                key = DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_2_4,
                title = R.string.wifi_channel_2_4_ghz,
                icon = R.drawable.ic_frequency,
                entriesRes = R.array.wifi_channels_2_4_ghz,
                entryValuesRes = R.array.wifi_channels_2_4_ghz,
                visibleWhen = { "2.4" == it.getString(DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY, "5") },
            )
            list(
                key = DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_5,
                title = R.string.wifi_channel_5_ghz,
                icon = R.drawable.ic_frequency,
                entriesRes = R.array.wifi_channels_5_ghz,
                entryValuesRes = R.array.wifi_channels_5_ghz,
                visibleWhen = { "5" == it.getString(DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY, "5") },
            )
        }

        screen(
            key = "screen_aawireless_advanced_settings",
            title = R.string.advanced_settings,
            icon = R.drawable.ic_warning,
        ) {
            switchSetting(
                key = AAWirelessPrefs.PREF_DONGLE_MODE,
                title = R.string.pref_aawireless_dongle_mode_title,
                summary = R.string.pref_aawireless_dongle_mode_summary,
                icon = R.drawable.ic_usb_dongle,
                defaultValue = false,
                confirmationMessage = R.string.pref_aawireless_dongle_confirmation,
            )
            switchSetting(
                key = AAWirelessPrefs.PREF_PASSTHROUGH,
                title = R.string.pref_aawireless_passthrough_title,
                summary = R.string.pref_aawireless_passthrough_summary,
                icon = R.drawable.ic_arrow_depth,
                defaultValue = true,
                disableDependentsState = true,
            )
            list(
                key = AAWirelessPrefs.PREF_AUDIO_STUTTER_FIX,
                title = R.string.pref_aawireless_audio_stutter_fix_title,
                summary = R.string.pref_aawireless_audio_stutter_fix_summary,
                icon = R.drawable.ic_volume_up,
                entriesRes = R.array.aawireless_audio_stutter_fix_names,
                entryValuesRes = R.array.aawireless_audio_stutter_fix_values,
                defaultValue = AAWirelessPrefs.AUDIO_STUTTER_FIX_OFF,
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
            )
            text(
                key = AAWirelessPrefs.PREF_DPI,
                title = R.string.dpi_dots_per_inch,
                summary = R.string.pref_aawireless_dpi_summary,
                icon = R.drawable.ic_font_size,
                defaultValue = "0",
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
                onBindEditText = { editText ->
                    editText.inputType = InputType.TYPE_CLASS_NUMBER
                    editText.addTextChangedListener(MinMaxTextWatcher(editText, 0, 300, false))
                    editText.setSelection(editText.text?.length ?: 0)
                },
            )
            switchSetting(
                key = AAWirelessPrefs.PREF_DISABLE_MEDIA_SINK,
                title = R.string.pref_aawireless_disable_media_sink_title,
                summary = R.string.pref_aawireless_disable_media_sink_summary,
                icon = R.drawable.ic_music_note,
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
            )
            switchSetting(
                key = AAWirelessPrefs.PREF_DISABLE_TTS_SINK,
                title = R.string.pref_aawireless_disable_tts_sink_title,
                summary = R.string.pref_aawireless_disable_tts_sink_summary,
                icon = R.drawable.ic_voice,
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
            )
            switchSetting(
                key = AAWirelessPrefs.PREF_REMOVE_TAP_RESTRICTION,
                title = R.string.pref_aawireless_remove_tap_restriction_title,
                summary = R.string.pref_aawireless_remove_tap_restriction_summary,
                icon = R.drawable.ic_touch,
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
            )
            switchSetting(
                key = AAWirelessPrefs.PREF_VAG_CRASH_FIX,
                title = R.string.pref_aawireless_vag_crash_fix_title,
                summary = R.string.pref_aawireless_vag_crash_fix_summary,
                icon = R.drawable.ic_extension,
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
            )
            switchSetting(
                key = AAWirelessPrefs.PREF_START_FIX,
                title = R.string.pref_aawireless_start_fix_title,
                summary = R.string.pref_aawireless_start_fix_summary,
                icon = R.drawable.ic_extension,
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
            )
            switchSetting(
                key = AAWirelessPrefs.PREF_DEVELOPER_MODE,
                title = R.string.developer_mode,
                summary = R.string.pref_aawireless_developer_mode_summary,
                icon = R.drawable.ic_developer_mode,
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
            )
            switchSetting(
                key = AAWirelessPrefs.PREF_AUTO_VIDEO_FOCUS,
                title = R.string.pref_aawireless_auto_video_focus_title,
                summary = R.string.pref_aawireless_auto_video_focus_summary,
                icon = R.drawable.ic_focus,
                dependency = AAWirelessPrefs.PREF_PASSTHROUGH,
            )
        }
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.UNKNOWN
    }

    companion object {
        private fun buttonModeEntries(device: GBDevice): List<ListEntry> {
            val prefs = AAWirelessPrefs(GBApplication.getDevicePrefs(device).preferences, device)
            val entries = mutableListOf<ListEntry>(
                ListEntry.Res(AAWirelessPrefs.BUTTON_MODE_NONE, R.string.none),
                ListEntry.Res(AAWirelessPrefs.BUTTON_MODE_NEXT_PHONE, R.string.switch_to_next_phone),
                ListEntry.Res(AAWirelessPrefs.BUTTON_MODE_STANDBY_ON_OFF, R.string.toggle_standby_on_off),
            )
            val context = GBApplication.getContext()
            for (i in 0 until prefs.pairedPhoneCount) {
                entries.add(
                    ListEntry.Text(
                        prefs.getPairedPhoneMac(i),
                        context.getString(R.string.switch_to_phone, prefs.getPairedPhoneName(i)),
                    )
                )
            }
            return entries
        }
    }
}
