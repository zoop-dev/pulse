package nodomain.freeyourgadget.gadgetbridge.service.devices.shokz

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.LabeledEntry
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.Language

/// Naming: LONG_PRESS_MULTI_FUNCTION __ SIMULTANEOUS_VOLUME_UP_DOWN
enum class ShokzControls(val code: Int) {
    ASSISTANT__MEDIA_SOURCE(0x01),
    MEDIA_SOURCE__ASSISTANT(0x02),
    MEDIA_SOURCE__MEDIA_SOURCE(0x03),
    ASSISTANT__ASSISTANT(0x04),
    ;

    companion object {
        fun fromCode(code: Int): ShokzControls? = entries.find { it.code == code }
    }
}

enum class ShokzEqualizer(
    val code: Int,
    override val label: Int,
    val sources: Set<ShokzMediaSource>
) : LabeledEntry {
    STANDARD(0x01, R.string.equalizer_preset_standard, setOf(ShokzMediaSource.BLUETOOTH, ShokzMediaSource.MP3)),
    VOCAL(0x02, R.string.sony_equalizer_preset_vocal, setOf(ShokzMediaSource.BLUETOOTH)),
    SWIMMING(0x07, R.string.Swimming, setOf(ShokzMediaSource.MP3)),
    ;

    companion object {
        fun fromPreference(value: String): ShokzEqualizer? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): ShokzEqualizer? = entries.find { it.code == code }
    }
}

enum class ShokzMp3PlaybackMode(val code: Int, override val label: Int) : LabeledEntry {
    NORMAL(0x00, R.string.media_playback_mode_normal),
    SHUFFLE(0x01, R.string.media_playback_mode_shuffle),
    REPEAT(0x02, R.string.media_playback_mode_repeat),
    ;

    companion object {
        fun fromPreference(value: String): ShokzMp3PlaybackMode? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): ShokzMp3PlaybackMode? = entries.find { it.code == code }
    }
}

enum class ShokzMediaSource(val code: Int, override val label: Int) : LabeledEntry {
    BLUETOOTH(0x00, R.string.menuitem_bluetooth),
    MP3(0x01, R.string.mp3),
    ;

    companion object {
        fun fromPreference(value: String): ShokzMediaSource? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): ShokzMediaSource? = entries.find { it.code == code }
    }
}

enum class ShokzPlaybackStatus(val code: Int) {
    PAUSED(0x00),
    PLAYING(0xff),
    ;

    companion object {
        fun fromCode(code: Int): ShokzPlaybackStatus? = entries.find { it.code == code }
    }
}

enum class ShokzLanguage(val language: Language, val code: Int) {
    ENGLISH(Language.EN, 0x00),
    CHINESE(Language.ZH, 0x01),
    JAPANESE(Language.JA, 0x02),
    KOREAN(Language.KO, 0x03),
    ;

    companion object {
        fun fromLanguage(language: Language): ShokzLanguage? = entries.find { it.language == language }
        fun fromCode(code: Int): ShokzLanguage? = entries.find { it.code == code }
    }
}
