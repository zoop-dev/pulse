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

    /** Finds a [ScreenSetting] by its key, searching the full tree. */
    fun findScreen(key: String): ScreenSetting? = findScreen(key, items)

    private fun findScreen(key: String, nodes: List<DeviceSetting>): ScreenSetting? {
        for (node in nodes) {
            if (node is ScreenSetting && node.key == key) return node
            if (node is GroupSetting) {
                val inner = findScreen(key, node.children)
                if (inner != null) return inner
            }
        }
        return null
    }

    /**
     * Returns all preference keys managed by this spec.
     */
    fun collectAllKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        items.forEach { collectKeys(it, keys) }
        return keys
    }

    private fun collectKeys(node: DeviceSetting, out: MutableSet<String>) {
        val k = node.key
        if (k.isNotEmpty()) out.add(k)
        if (node is GroupSetting) node.children.forEach { collectKeys(it, out) }
    }

    /**
     * Returns all preference keys that should be disabled when the device is not connected.
     * Includes keys from [XmlScreenSetting] navigation entries and their declared child keys.
     */
    fun collectConnectedKeys(): Array<String> {
        val keys = mutableListOf<String>()
        items.forEach { collectConnectedKeys(it, keys) }
        return keys.toTypedArray()
    }

    private fun collectConnectedKeys(node: DeviceSetting, out: MutableList<String>) {
        if (node.connectedOnly) out.add(node.key)
        when (node) {
            is GroupSetting -> node.children.forEach { collectConnectedKeys(it, out) }
            is XmlScreenSetting -> out.addAll(node.childConnectedKeys)
            else -> {}
        }
    }

    /**
     * Returns the key of the top-level [ScreenSetting] that (transitively) contains [prefKey],
     * or null if [prefKey] is itself a top-level item or is not found in the spec.
     */
    fun findTopScreenKeyForPreference(prefKey: String): String? {
        for (item in items) {
            if (item.key == prefKey) return null
            if (item is ScreenSetting && screenContainsKey(item, prefKey)) return item.key
        }
        return null
    }

    private fun screenContainsKey(screen: ScreenSetting, prefKey: String): Boolean =
        screen.children.any { it.key == prefKey || (it is ScreenSetting && screenContainsKey(it, prefKey)) }
}
