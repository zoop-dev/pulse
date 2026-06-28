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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl

import androidx.annotation.StringRes
import nodomain.freeyourgadget.gadgetbridge.R

/**
 * All device language options available in Gadgetbridge. Each constant carries the exact
 * preference [code] stored in SharedPreferences and a human-readable [label] resource.
 */
enum class Language(val code: String, @StringRes val label: Int) {
    AUTO("auto", R.string.automatic),
    ZH_CN("zh_CN", R.string.simplified_chinese),
    ZH_TW("zh_TW", R.string.traditional_chinese),
    EN_AU("en_AU", R.string.english_au),
    EN_CA("en_CA", R.string.english_ca),
    EN_GB("en_GB", R.string.english_gb),
    EN_IN("en_IN", R.string.english_in),
    EN_US("en_US", R.string.english_us),
    ES_ES("es_ES", R.string.spanish_es),
    ES_MX("es_MX", R.string.spanish_mx),
    ES_US("es_US", R.string.spanish_us),
    DE_DE("de_DE", R.string.german),
    IT_IT("it_IT", R.string.italian),
    FR_CA("fr_CA", R.string.french_ca),
    FR_FR("fr_FR", R.string.french_fr),
    PT_BR("pt_BR", R.string.portuguese_br),
    PT_PT("pt_PT", R.string.portuguese_pt),
    NL_NL("nl_NL", R.string.dutch),
    PL_PL("pl_PL", R.string.polish),
    CS_CZ("cs_CZ", R.string.czesh),
    TR_TR("tr_TR", R.string.turkish),
    EL_GR("el_GR", R.string.greek),
    RU_RU("ru_RU", R.string.russian),
    UK_UA("uk_UA", R.string.ukrainian),
    AR_SA("ar_SA", R.string.arabic),
    ID_ID("id_ID", R.string.indonesian),
    TH_TH("th_TH", R.string.thai),
    VI_VN("vi_VN", R.string.vietnamese),
    JA_JP("ja_JP", R.string.japanese),
    KO_KO("ko_KO", R.string.korean),
    HE_IL("he_IL", R.string.hebrew),
    DA_DK("da_DK", R.string.danish),
    NB_NO("nb_NO", R.string.norwegian_bokmal),
    RO_RO("ro_RO", R.string.romanian),
    SV_SE("sv_SE", R.string.swedish),
    SH_SP("sh_SP", R.string.serbian_latin),
    CA_ES("ca_ES", R.string.catalan),
    FI_FI("fi_FI", R.string.finnish),
    HU_HU("hu_HU", R.string.hungarian),
    MS_MY("ms_MY", R.string.bahasa_melayu),
    FA_IR("fa_IR", R.string.persian),

    // simplified versions, without country suffix
    ZH("zh", R.string.chinese),
    EN("en", R.string.english),
    KO("ko", R.string.korean),
    JA("ja", R.string.japanese),
    DE("de", R.string.german),
    ES("es", R.string.spanish),
    FR("fr", R.string.french),
    IT("it", R.string.italian),
    PT("pt", R.string.portuguese),
    AR("ar", R.string.arabic),
    PL("pl", R.string.polish),
    RU("ru", R.string.russian),
    NL("nl", R.string.dutch),
    TR("tr", R.string.turkish),
    BN("bn", R.string.bengali),
    ID("id", R.string.indonesian),
    CS("cs", R.string.czech),
    HE("he", R.string.hebrew),
    TH("th", R.string.thai),
    FA("fa", R.string.persian),
    VI("vi", R.string.vietnamese),
}
