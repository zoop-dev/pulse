/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.gloryfit

enum class GloryFitLanguage(val locale: String, val code: Byte) {
    CHINESE_SIMPLIFIED("zh_CN", 0x01),
    CHINESE_TRADITIONAL("zh_TW", 0x17),
    ENGLISH("en", 0x02),
    KOREAN("ko", 0x03),
    JAPANESE("ja", 0x04),
    GERMAN("de", 0x05),
    SPANISH("es", 0x06),
    FRENCH("fr", 0x07),
    ITALIAN("it", 0x08),
    PORTUGUESE("pt", 0x09),
    ARABIC("ar", 0x0a),
    POLISH("pl", 0x0d),
    RUSSIAN("ru", 0x0e),
    DUTCH("nl", 0x0f),
    TURKISH("tr", 0x10),
    BENGALI("bn", 0x11),
    INDONESIAN("id", 0x13),
    CZECH("cs", 0x16),
    HEBREW("he", 0x18),
    THAI("th", 0x15),
    PERSIAN("fa", 0x28),
    VIETNAMESE("vi", 0x63),
    ;

    companion object {
        fun fromLocale(locale: String): GloryFitLanguage? {
            return entries.find { language -> language.locale == locale }
                // Fallback - attempt to find the next closest one
                ?: entries.find { language -> language.locale.substring(0, 2) == locale.substring(0, 2) }
        }
    }
}
