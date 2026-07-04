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

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import androidx.core.content.edit

/** The type of headless interaction available for a [QuickSettingDescriptor]. */
enum class QuickSettingType { TOGGLE, LIST }

/**
 * Describes a single device setting that can be operated headlessly, without opening the device
 * settings UI: either toggled on/off (TOGGLE) or cycled to the next value (LIST).
 *
 * Instances are obtained from [QuickSettings.listFor] / [QuickSettings.listAll].
 */
data class QuickSettingDescriptor(
    val deviceAddress: String,
    val deviceName: String,
    val key: String,
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val type: QuickSettingType,
)

/**
 * Utilities for enumerating and headlessly applying device settings declared via the
 * [DeviceSettingsSpec] DSL.
 *
 * Only [SwitchSetting] ([QuickSettingType.TOGGLE]) and [ListSetting] ([QuickSettingType.LIST])
 * nodes are included. Seekbar, text, action and XML-screen nodes are excluded.
 */
object QuickSettings {

    /** Separator used in [controlId] to combine device address and preference key. */
    const val CONTROL_ID_SEPARATOR = "::"

    /**
     * Returns all [QuickSettingDescriptor]s for [device] by recursively walking the coordinator's
     * [DeviceSettingsSpec]. Nodes whose [DeviceSetting.visibleWhen] evaluates to false against the
     * current device SharedPreferences are excluded. Returns an empty list for devices without a
     * DSL spec.
     */
    fun listFor(device: GBDevice): List<QuickSettingDescriptor> {
        val spec = device.deviceCoordinator.getDeviceSettings(device) ?: return emptyList()
        val prefs = Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.address))
        val result = mutableListOf<QuickSettingDescriptor>()
        collectNodes(device, spec.items, prefs, result)
        return result
    }

    private fun collectNodes(
        device: GBDevice,
        items: List<DeviceSetting>,
        prefs: Prefs,
        out: MutableList<QuickSettingDescriptor>,
    ) {
        for (setting in items) {
            when (setting) {
                is GroupSetting -> collectNodes(device, setting.children, prefs, out)
                is SwitchSetting -> out.add(
                    QuickSettingDescriptor(
                        deviceAddress = device.address,
                        deviceName = device.aliasOrName,
                        key = setting.key,
                        title = setting.title,
                        icon = setting.icon,
                        type = QuickSettingType.TOGGLE,
                    )
                )

                is ListSetting -> out.add(
                    QuickSettingDescriptor(
                        deviceAddress = device.address,
                        deviceName = device.aliasOrName,
                        key = setting.key,
                        title = setting.title,
                        icon = setting.icon,
                        type = QuickSettingType.LIST,
                    )
                )

                // CategorySetting, TextSetting, ActionSetting, XmlScreenSetting excluded
                else -> {}
            }
        }
    }

    /** Returns descriptors for all paired devices that expose any DSL settings. */
    fun listAll(): List<QuickSettingDescriptor> =
        GBApplication.app().deviceManager.devices.flatMap { listFor(it) }

    /** Finds a single [QuickSettingDescriptor] by device address and preference key. */
    fun find(address: String, key: String): QuickSettingDescriptor? {
        val device = GBApplication.app().deviceManager.getDeviceByAddress(address) ?: return null
        return listFor(device).firstOrNull { it.key == key }
    }

    /** Returns the current stored boolean value for a TOGGLE setting (defaults to false). */
    fun currentBool(address: String, key: String): Boolean =
        GBApplication.getDeviceSpecificSharedPrefs(address).getBoolean(key, false)

    /**
     * Returns the resolved display label for the current LIST value, or null if it cannot be
     * determined (unknown device, no DSL spec, or unresolvable entries).
     */
    fun currentLabel(context: Context, device: GBDevice, key: String): String? {
        val spec = device.deviceCoordinator.getDeviceSettings(device) ?: return null
        val setting = findListSetting(spec.items, key) ?: return null
        val sp = GBApplication.getDeviceSpecificSharedPrefs(device.address)
        val currentValue = sp.getString(key, setting.defaultValue) ?: setting.defaultValue
        val entries = resolveEntries(context, setting, Prefs(sp))
        return entries.firstOrNull { it.value == currentValue }?.let { entry ->
            when (entry) {
                is ListEntry.Res -> context.getString(entry.label)
                is ListEntry.Text -> entry.label
            }
        }
    }

    /**
     * Applies the headless setting change for [descriptor]:
     * - **TOGGLE**: flips the stored boolean.
     * - **LIST**: advances to the next [ListEntry] value (wrapping around).
     *   If [cycleValues] is non-empty, only those values participate in the cycle (values not
     *   present in the full entry list are silently skipped).
     *
     * After both, onSendConfiguration is called.
     *
     * @return the new boolean state for [QuickSettingType.TOGGLE], or null for [QuickSettingType.LIST].
     */
    fun apply(
        context: Context,
        device: GBDevice,
        descriptor: QuickSettingDescriptor,
        cycleValues: List<String> = emptyList(),
    ): Boolean? {
        val devicePrefs = GBApplication.getDeviceSpecificSharedPrefs(device.address)
        return when (descriptor.type) {
            QuickSettingType.TOGGLE -> {
                val newValue = !devicePrefs.getBoolean(descriptor.key, false)
                devicePrefs.edit { putBoolean(descriptor.key, newValue) }
                GBApplication.deviceService(device).onSendConfiguration(descriptor.key)
                newValue
            }

            QuickSettingType.LIST -> {
                val spec = device.deviceCoordinator.getDeviceSettings(device)
                val setting = spec?.let { findListSetting(it.items, descriptor.key) }
                if (setting != null) {
                    val allEntries = resolveEntries(context, setting, Prefs(devicePrefs))
                    // Restrict to allowed cycle values if configured, preserving declaration order
                    val entries = if (cycleValues.isEmpty()) allEntries
                    else allEntries.filter { it.value in cycleValues }
                        .takeUnless { it.isEmpty() } ?: allEntries
                    if (entries.isNotEmpty()) {
                        val current = devicePrefs.getString(descriptor.key, setting.defaultValue) ?: ""
                        val idx = entries.indexOfFirst { it.value == current }.coerceAtLeast(0)
                        devicePrefs.edit { putString(descriptor.key, entries[(idx + 1) % entries.size].value) }
                        GBApplication.deviceService(device).onSendConfiguration(descriptor.key)
                    }
                }
                null
            }
        }
    }

    /**
     * Returns the [ListSetting] for [key] within [device]'s spec, or null if the device has no
     * DSL spec or the key does not correspond to a list setting.
     */
    fun getListSetting(device: GBDevice, key: String): ListSetting? {
        val spec = device.deviceCoordinator.getDeviceSettings(device) ?: return null
        return findListSetting(spec.items, key)
    }

    private fun findListSetting(items: List<DeviceSetting>, key: String): ListSetting? {
        for (item in items) {
            when (item) {
                is ListSetting -> if (item.key == key) return item
                is GroupSetting -> findListSetting(item.children, key)?.let { return it }
                else -> {}
            }
        }
        return null
    }

    /**
     * Resolves the concrete entry list for a [ListSetting], mirroring the logic in
     * [DeviceSettingRenderer]: prefers [ListSetting.entriesProvider], then [ListSetting.entries],
     * then falls back to the legacy array resources.
     */
    fun resolveEntries(context: Context, setting: ListSetting, prefs: Prefs): List<ListEntry> =
        when {
            setting.entriesProvider != null -> setting.entriesProvider.invoke(prefs)
            setting.entries.isNotEmpty() -> setting.entries
            setting.entriesRes != 0 -> {
                val labels = context.resources.getStringArray(setting.entriesRes)
                val values = context.resources.getStringArray(setting.entryValuesRes)
                labels.zip(values).map { (label, value) -> ListEntry.Text(value, label) }
            }

            else -> emptyList()
        }

    /** Encodes a Device Controls control ID from a device address and preference key. */
    fun controlId(address: String, key: String): String = "$address$CONTROL_ID_SEPARATOR$key"

    /**
     * Decodes a Device Controls control ID into (deviceAddress, preferenceKey), or null if the
     * format is unrecognized.
     */
    fun parseControlId(controlId: String): Pair<String, String>? {
        val idx = controlId.indexOf(CONTROL_ID_SEPARATOR)
        if (idx < 0) return null
        return controlId.substring(0, idx) to controlId.substring(idx + CONTROL_ID_SEPARATOR.length)
    }
}
