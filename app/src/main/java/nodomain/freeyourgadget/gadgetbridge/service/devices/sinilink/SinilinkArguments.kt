package nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink

enum class SinilinkEqualizer(val code: Int) {
    NORMAL(0x09),
    ROCK(0x0a),
    POP(0x0b),
    CLASSIC(0x0c),
    JAZZ(0x0d),
    COUNTRY(0x0e),
    ;

    companion object {
        fun fromPreference(value: String): SinilinkEqualizer? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): SinilinkEqualizer? = entries.find { it.code == code }
    }
}

enum class SinilinkPlaybackMode(val code: Int) {
    SINGLE_HEAD(0x10),
    SINGLE_CYCLE(0x11),
    RANDOM(0x12),
    ORDER(0x13),
    LIST_CYCLE(0x0f),
    ;

    companion object {
        fun fromPreference(value: String): SinilinkPlaybackMode? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): SinilinkPlaybackMode? = entries.find { it.code == code }
    }
}

enum class SinilinkPlaybackState(val code: Int) {
    IDLE(0x01),
    PLAYING(0x02),
    ;

    companion object {
        fun fromPreference(value: String): SinilinkPlaybackState? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): SinilinkPlaybackState? = entries.find { it.code == code }
    }
}

enum class SinilinkMediaSource(val code: Int) {
    TF(0x03),
    USB(0x04),
    BLUETOOTH(0x14),
    AUDIO_CARD(0x15),
    AUX(0x16),
    ;

    companion object {
        fun fromPreference(value: String): SinilinkMediaSource? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): SinilinkMediaSource? = entries.find { it.code == code }
    }
}

enum class SinilinkButton(val code: Int) {
    PLAY_PAUSE(0x01),
    PREVIOUS(0x07),
    NEXT(0x08),
    ;

    companion object {
        fun fromPreference(value: String): SinilinkButton? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): SinilinkButton? = entries.find { it.code == code }
    }
}
