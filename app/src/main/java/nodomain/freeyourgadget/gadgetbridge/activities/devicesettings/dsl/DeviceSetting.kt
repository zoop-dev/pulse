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
@file:Suppress("ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD")

package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.util.Prefs

/**
 * Sealed hierarchy describing a single device setting node. Nodes are purely declarative -
 * they carry no Android context and can be built by a coordinator and re-evaluated cheaply.
 *
 * Common semantics:
 *  - [key]          - SharedPreferences key (or navigation screen key for screen nodes).
 *  - [visibleWhen]  - predicate evaluated against current SharedPreferences; null means always
 *    visible. Re-evaluated after any preference in the screen changes.
 *  - [connectedOnly] - preference is disabled when the device is not connected.
 */
sealed class DeviceSetting {
    abstract val key: String
    abstract val visibleWhen: ((Prefs) -> Boolean)?
    abstract val connectedOnly: Boolean
}

/**
 * A [DeviceSetting] that contains child settings. Both [ScreenSetting] and [CategorySetting]
 * are groups; tree-walking code can match on this type to recurse without caring which kind.
 */
sealed class GroupSetting : DeviceSetting() {
    abstract val children: List<DeviceSetting>
}

/**
 * A navigable sub-screen whose children are rendered programmatically when entered.
 * In the root preference list this appears as a single tappable row (title + icon).
 */
data class ScreenSetting(
    override val key: String,
    @StringRes val title: Int,
    @StringRes val summary: Int = 0,
    @DrawableRes val icon: Int = 0,
    override val visibleWhen: ((Prefs) -> Boolean)? = null,
    override val connectedOnly: Boolean = true,
    override val children: List<DeviceSetting> = emptyList(),
) : GroupSetting()

/**
 * A switch boolean setting, equivalent to SwitchPreferenceCompat.
 * [summaryOn] and [summaryOff] show different text depending on the checked state; [summary] shows
 * static text regardless. If none are set, no summary is displayed.
 */
data class SwitchSetting(
    override val key: String,
    @StringRes val title: Int,
    @StringRes val summary: Int = 0,
    @StringRes val summaryOn: Int = 0,
    @StringRes val summaryOff: Int = 0,
    @DrawableRes val icon: Int = 0,
    val defaultValue: Boolean = false,
    val dependency: String? = null,
    override val visibleWhen: ((Prefs) -> Boolean)? = null,
    override val connectedOnly: Boolean = true,
) : DeviceSetting()

/**
 * A list setting, equivalent to ListPreference. Exactly one entry source must be provided:
 *  - [entriesProvider]: evaluated from SharedPreferences at render time and on every refresh -
 *    use this for presets fetched from the device at connection.
 *  - [entries]: static list built at DSL construction time (e.g. from a [LabeledEntry] enum).
 *  - [entriesRes] + [entryValuesRes]: legacy resource arrays, for devices not yet migrated to
 *    programmatic entries.
 *
 * If [summary] is set it overrides the default behaviour of showing the selected entry as summary.
 */
data class ListSetting(
    override val key: String,
    @StringRes val title: Int,
    @StringRes val summary: Int = 0,
    @DrawableRes val icon: Int = 0,
    @ArrayRes val entriesRes: Int = 0,
    @ArrayRes val entryValuesRes: Int = 0,
    val entries: List<ListEntry> = emptyList(),
    val entriesProvider: ((Prefs) -> List<ListEntry>)? = null,
    val defaultValue: String = "",
    val dependency: String? = null,
    override val visibleWhen: ((Prefs) -> Boolean)? = null,
    override val connectedOnly: Boolean = true,
) : DeviceSetting()

/** A seek bar setting, equivalent to SeekBarPreference. */
data class SeekBarSetting(
    override val key: String,
    @StringRes val title: Int,
    @StringRes val summary: Int = 0,
    @DrawableRes val icon: Int = 0,
    val max: Int,
    val defaultValue: Int,
    val showValue: Boolean = true,
    val dependency: String? = null,
    override val visibleWhen: ((Prefs) -> Boolean)? = null,
    override val connectedOnly: Boolean = true,
) : DeviceSetting()

/** A preference category header, equivalent to PreferenceCategory. Children are rendered inside the group. */
data class CategorySetting(
    override val key: String,
    @StringRes val title: Int,
    override val children: List<DeviceSetting> = emptyList(),
    override val visibleWhen: ((Prefs) -> Boolean)? = null,
    override val connectedOnly: Boolean = false,
) : GroupSetting()

/**
 * A free text setting, equivalent to EditTextPreference.
 * If [summary] is set it overrides the default behaviour of showing the current value as summary.
 * Set [enabled] to false to make the field read-only (displayed but not editable).
 */
data class TextSetting(
    override val key: String,
    @StringRes val title: Int,
    @StringRes val summary: Int = 0,
    @DrawableRes val icon: Int = 0,
    val defaultValue: String = "",
    val maxLength: Int? = null,
    val inputType: Int = InputType.TYPE_CLASS_TEXT,
    val dependency: String? = null,
    override val visibleWhen: ((Prefs) -> Boolean)? = null,
    override val connectedOnly: Boolean = true,
    val enabled: Boolean = true,
    val onSharedPreferenceChanged: ((String) -> Unit)? = null,
    val onBindEditText: ((EditText) -> Unit)? = null,
    /** When non-null, shown as the summary when no value is set. */
    val defaultSummary: ((Context) -> String)? = null,
    /** When non-zero, wraps the current value in this format string for the summary. */
    @StringRes val summaryTemplate: Int = 0,
) : DeviceSetting()

/**
 * A non-persistent action preference. [onClick] receives the [DeviceSpecificSettingsHandler] so
 * it can launch activities or invoke device-specific operations.
 */
data class ActionSetting(
    override val key: String,
    @StringRes val title: Int = 0,
    @StringRes val summary: Int = 0,
    @DrawableRes val icon: Int = 0,
    override val visibleWhen: ((Prefs) -> Boolean)? = null,
    override val connectedOnly: Boolean = true,
    val onClick: ((DeviceSpecificSettingsHandler) -> Boolean)? = null,
) : DeviceSetting()

/**
 * Legacy wrapper for an existing DeviceSpecificSettingsScreen and its XML sub-screens, so
 * that a migrating coordinator can delegate remaining XML screens while providing model nodes for
 * others.
 */
data class XmlScreenSetting(
    val screen: DeviceSpecificSettingsScreen,
    val subScreens: List<Int> = emptyList(),
    override val connectedOnly: Boolean = true,
    /** Keys of preferences inside the sub-screen XML that should also be disabled when disconnected. */
    val childConnectedKeys: List<String> = emptyList(),
    override val visibleWhen: ((Prefs) -> Boolean)? = null,
) : DeviceSetting() {
    override val key: String get() = screen.key
}
