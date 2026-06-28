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

/**
 * A single option in a [ListSetting].
 *
 * - [Res]  — label resolved from a string resource at render time (compile-time entries,
 *   e.g. from a [LabeledEntry] enum).
 * - [Text] — label supplied as a plain string at runtime (e.g. fetched from the device on
 *   connection and stored in SharedPreferences).
 */
sealed class ListEntry {
    /** The value stored in SharedPreferences when this option is selected. */
    abstract val value: String

    data class Res(override val value: String, @param:StringRes val label: Int) : ListEntry()
    data class Text(override val value: String, val label: String) : ListEntry()
}
