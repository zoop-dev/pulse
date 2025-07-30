package nodomain.freeyourgadget.gadgetbridge.service.devices.shokz

/// Naming: LONG_PRESS_MULTI_FUNCTION __ SIMULTANEOUS_VOLUME_UP_DOWN
enum class ShokzControls(val code: Int) {
    ASSISTANT__MEDIA_SOURCE(0x01),
    MEDIA_SOURCE__ASSISTANT(0x02),
    MEDIA_SOURCE__MEDIA_SOURCE(0x03),
    ASSISTANT__ASSISTANT(0x04),
    ;

    companion object {
        fun fromPreference(value: String): ShokzControls? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): ShokzControls? = entries.find { it.code == code }
    }
}

enum class ShokzEqualizer(val code: Int) {
    STANDARD(0x01),
    VOCAL(0x02),
    SWIMMING(0x07),
    ;

    companion object {
        fun fromPreference(value: String): ShokzEqualizer? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): ShokzEqualizer? = entries.find { it.code == code }
    }
}

enum class ShokzMp3PlaybackMode(val code: Int) {
    NORMAL(0x00),
    SHUFFLE(0x01),
    REPEAT(0x02),
    ;

    companion object {
        fun fromPreference(value: String): ShokzMp3PlaybackMode? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): ShokzMp3PlaybackMode? = entries.find { it.code == code }
    }
}

enum class ShokzMediaSource(val code: Int) {
    BLUETOOTH(0x00),
    MP3(0x01),
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
        fun fromPreference(value: String): ShokzPlaybackStatus? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): ShokzPlaybackStatus? = entries.find { it.code == code }
    }
}

enum class ShokzLanguage(val locale: String, val code: Int) {
    ENGLISH("en", 0x00),
    CHINESE("zh", 0x01),
    JAPANESE("ja", 0x02),
    KOREAN("ko", 0x03),
    ;

    companion object {
        fun fromLocale(locale: String): ShokzLanguage? = entries.find { it.locale == locale }
        fun fromCode(code: Int): ShokzLanguage? = entries.find { it.code == code }
    }
}
