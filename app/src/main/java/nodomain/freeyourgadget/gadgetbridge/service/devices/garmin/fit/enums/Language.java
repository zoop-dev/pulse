package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import androidx.annotation.Nullable;

public enum Language {
    english(0),
    french(1),
    italian(2),
    german(3),
    spanish(4),
    croatian(5),
    czech(6),
    danish(7),
    dutch(8),
    finnish(9),
    greek(10),
    hungarian(11),
    norwegian(12),
    polish(13),
    portuguese(14),
    slovakian(15),
    slovenian(16),
    swedish(17),
    russian(18),
    turkish(19),
    latvian(20),
    ukrainian(21),
    arabic(22),
    farsi(23),
    bulgarian(24),
    romanian(25),
    chinese(26),
    japanese(27),
    korean(28),
    taiwanese(29),
    thai(30),
    hebrew(31),
    brazilian_portuguese(32),
    indonesian(33),
    malaysian(34),
    vietnamese(35),
    burmese(36),
    mongolian(37),
    estonian(38),
    lithuanian(39),
    ;

    public final int id;

    Language(final int i) {
        id = i;
    }
}
