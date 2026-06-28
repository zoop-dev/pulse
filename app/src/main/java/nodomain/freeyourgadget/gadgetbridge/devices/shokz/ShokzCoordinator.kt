package nodomain.freeyourgadget.gadgetbridge.devices.shokz

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsSpec
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.Language
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.enumList
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.equalizerPreset
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.languages
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components.multipointPairing
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.deviceSettings
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.shokz.ShokzEqualizer
import nodomain.freeyourgadget.gadgetbridge.service.devices.shokz.ShokzMediaSource
import nodomain.freeyourgadget.gadgetbridge.service.devices.shokz.ShokzMp3PlaybackMode
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

    override fun getDeviceSettings(device: GBDevice): DeviceSettingsSpec = deviceSettings {
        multipointPairing()
        languages(Language.EN, Language.ZH, Language.JA, Language.KO)
        enumList<ShokzMediaSource>(
            key = DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE,
            title = R.string.media_source,
            icon = R.drawable.ic_music_note,
            defaultValue = ShokzMediaSource.BLUETOOTH,
        )

        // Equalizer presets - same enum, filtered to entries valid for each media source
        equalizerPreset<ShokzEqualizer>(
            key = DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_BLUETOOTH,
            title = R.string.sony_equalizer,
            defaultValue = ShokzEqualizer.STANDARD,
            filter = { ShokzMediaSource.BLUETOOTH in it.sources },
            visibleWhen = { prefs ->
                ShokzMediaSource.fromPreference(
                    prefs.getString(DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE, "")
                ) == ShokzMediaSource.BLUETOOTH
            },
        )
        equalizerPreset<ShokzEqualizer>(
            key = DeviceSettingsPreferenceConst.PREF_SHOKZ_EQUALIZER_MP3,
            title = R.string.sony_equalizer,
            defaultValue = ShokzEqualizer.STANDARD,
            filter = { ShokzMediaSource.MP3 in it.sources },
            visibleWhen = { prefs ->
                ShokzMediaSource.fromPreference(
                    prefs.getString(DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE, "")
                ) == ShokzMediaSource.MP3
            },
        )

        enumList<ShokzMp3PlaybackMode>(
            key = DeviceSettingsPreferenceConst.PREF_MEDIA_PLAYBACK_MODE,
            title = R.string.media_playback_mode,
            icon = R.drawable.ic_play,
            defaultValue = ShokzMp3PlaybackMode.NORMAL,
            visibleWhen = { prefs ->
                ShokzMediaSource.fromPreference(
                    prefs.getString(DeviceSettingsPreferenceConst.PREF_MEDIA_SOURCE, "")
                ) == ShokzMediaSource.MP3
            },
        )
        xmlScreen(
            DeviceSpecificSettingsScreen.TOUCH_OPTIONS,
            R.xml.devicesettings_shokz_controls,
            childConnectedKeys = listOf(
                DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_LONG_PRESS_MULTI_FUNCTION,
                DeviceSettingsPreferenceConst.PREF_SHOKZ_CONTROLS_SIMULTANEOUS_VOLUME_UP_DOWN,
            ),
        )
        xmlScreen(
            DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS,
            R.xml.devicesettings_headphones,
            connectedOnly = false,
        )
    }
}
