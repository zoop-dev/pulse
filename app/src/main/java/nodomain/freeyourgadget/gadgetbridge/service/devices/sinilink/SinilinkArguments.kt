package nodomain.freeyourgadget.gadgetbridge.service.devices.sinilink

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.LabeledEntry

enum class SinilinkEqualizer(val code: Int, override val label: Int) : LabeledEntry {
    NORMAL(0x09, R.string.pref_title_equalizer_normal),
    ROCK(0x0a, R.string.nothing_equalizer_rock),
    POP(0x0b, R.string.nothing_equalizer_pop),
    CLASSIC(0x0c, R.string.nothing_equalizer_classical),
    JAZZ(0x0d, R.string.soundcore_equalizer_preset_jazz),
    COUNTRY(0x0e, R.string.equalizer_preset_country),
    ;

    companion object {
        fun fromPreference(value: String): SinilinkEqualizer? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): SinilinkEqualizer? = entries.find { it.code == code }
    }
}

enum class SinilinkPlaybackMode(val code: Int, override val label: Int) : LabeledEntry {
    SINGLE_HEAD(0x10, R.string.sinilink_playback_mode_single_head),
    SINGLE_CYCLE(0x11, R.string.sinilink_playback_mode_single_cycle),
    RANDOM(0x12, R.string.sinilink_playback_mode_random),
    ORDER(0x13, R.string.sinilink_playback_mode_order),
    LIST_CYCLE(0x0f, R.string.sinilink_playback_mode_list_cycle),
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

enum class SinilinkMediaSource(val code: Int, override val label: Int) : LabeledEntry {
    TF(0x03, R.string.media_source_tf),
    USB(0x04, R.string.media_source_usb),
    BLUETOOTH(0x14, R.string.menuitem_bluetooth),
    AUDIO_CARD(0x15, R.string.media_source_audio_card),
    AUX(0x16, R.string.media_source_aux),
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
        fun fromCode(code: Int): SinilinkButton? = entries.find { it.code == code }
    }
}
