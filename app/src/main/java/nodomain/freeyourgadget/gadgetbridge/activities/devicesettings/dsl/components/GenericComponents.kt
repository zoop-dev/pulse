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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsScope
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.LabeledEntry
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.Language
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ListEntry
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ListSetting
import nodomain.freeyourgadget.gadgetbridge.util.Prefs

/**
 * Adds a language [ListSetting] with key [DeviceSettingsPreferenceConst.PREF_LANGUAGE]. Pass the
 * languages the device supports; if none are given all known [Language] entries are included.
 */
fun DeviceSettingsScope.languages(vararg supported: Language) {
    val languages = if (supported.isEmpty()) Language.entries else supported.toList()
    items.add(
        ListSetting(
            key = DeviceSettingsPreferenceConst.PREF_LANGUAGE,
            title = R.string.pref_title_language,
            icon = R.drawable.ic_language,
            entries = languages.map { ListEntry.Res(it.code, it.label) },
            defaultValue = Language.AUTO.name.lowercase(),
            connectedOnly = true,
        )
    )
}

inline fun <reified T> DeviceSettingsScope.enumList(
    key: String,
    @StringRes title: Int,
    @DrawableRes icon: Int = 0,
    defaultValue: T,
    dependency: String? = null,
    connectedOnly: Boolean = true,
    noinline filter: ((T) -> Boolean)? = null,
    noinline visibleWhen: ((Prefs) -> Boolean)? = null,
) where T : Enum<T>, T : LabeledEntry {
    val all = enumValues<T>()
    val entries = (if (filter != null) all.filter(filter) else all.toList())
        .map { e -> ListEntry.Res(e.name.lowercase(), e.label) }
    items.add(
        ListSetting(
            key = key,
            title = title,
            icon = icon,
            entries = entries,
            defaultValue = defaultValue.name.lowercase(),
            dependency = dependency,
            connectedOnly = connectedOnly,
            visibleWhen = visibleWhen,
        )
    )
}
