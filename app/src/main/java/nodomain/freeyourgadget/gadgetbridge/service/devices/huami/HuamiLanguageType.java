/*  Copyright (C) 2021-2024 Andreas Shimokawa, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import java.util.LinkedHashMap;
import java.util.Map;

public class HuamiLanguageType {
    // Use a LinkedHashMap (sorted), so that when we reverse it we get the first value as key, deterministically
    public static final Map<String, Integer> idLookup = new LinkedHashMap<String, Integer>() {{
        put("zh_CN", 0x00);
        put("zh_TW", 0x01);
        put("zh_HK", 0x01);
        put("en_US", 0x02);
        put("en_GB", 0x02);
        put("en_AU", 0x02);
        put("es_ES", 0x03);
        put("ru_RU", 0x04);
        put("ko_KO", 0x05);
        put("fr_FR", 0x06);
        put("de_DE", 0x07);
        put("de_AT", 0x07);
        put("de_CH", 0x07);
        put("id_ID", 0x08);
        put("pl_PL", 0x09);
        put("it_IT", 0x0a);
        put("ja_JP", 0x0b);
        put("th_TH", 0x0c);
        put("ar_SA", 0x0d);
        put("vi_VN", 0x0e);
        put("pt_PT", 0x0f);
        put("nl_NL", 0x10);
        put("tr_TR", 0x11);
        put("uk_UA", 0x12);
        put("he_IL", 0x13);
        put("pt_BR", 0x14);
        put("ro_RO", 0x15);
        put("cs_CZ", 0x16);
        put("el_GR", 0x17);
        put("sh_SP", 0x18);
        put("ca_ES", 0x19);
        put("fi_FI", 0x1a);
        put("nb_NO", 0x1b);
        put("da_DK", 0x1c);
        put("sv_SE", 0x1d);
        put("hu_HU", 0x1e);
        put("ms_MY", 0x1f);
        // FIXME put("pt_BR", 0x22);
    }};
}
