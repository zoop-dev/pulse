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

/**
 * The programmatic settings model returned by a coordinator's
 * [nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.getDeviceSettings].
 *
 * Not Parcelable (nodes can carry lambdas); rebuilt cheaply from the coordinator on demand.
 */
class DeviceSettingsSpec(val items: List<DeviceSetting>) {

    /** Finds a [ScreenSetting] by its key, searching only the top-level items. */
    fun findScreen(key: String): ScreenSetting? =
        items.filterIsInstance<ScreenSetting>().firstOrNull { it.key == key }

    /**
     * Returns all top-level preference keys managed by this spec.
     */
    fun collectAllKeys(): Set<String> = items.mapNotNull { it.key.ifEmpty { null } }.toHashSet()

    /**
     * Returns all preference keys that should be disabled when the device is not connected.
     * Includes keys from [XmlScreenSetting] navigation entries and their declared child keys.
     */
    fun collectConnectedKeys(): Array<String> {
        val keys = mutableListOf<String>()
        for (item in items) {
            collectConnectedKeys(item, keys)
        }
        return keys.toTypedArray()
    }

    private fun collectConnectedKeys(node: DeviceSetting, out: MutableList<String>) {
        if (node.connectedOnly) out.add(node.key)
        when (node) {
            is ScreenSetting -> node.children.forEach { collectConnectedKeys(it, out) }
            is XmlScreenSetting -> out.addAll(node.childConnectedKeys)
            else -> {}
        }
    }
}
