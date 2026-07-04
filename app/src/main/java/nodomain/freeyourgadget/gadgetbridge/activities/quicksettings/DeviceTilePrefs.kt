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
package nodomain.freeyourgadget.gadgetbridge.activities.quicksettings

import androidx.core.content.edit
import nodomain.freeyourgadget.gadgetbridge.GBApplication

/**
 * Persists the tile-index → (deviceAddress, settingKey) assignments and optional cycle-value
 * restrictions for the Quick Settings preset tiles, stored in the default
 * [android.content.SharedPreferences].
 *
 * Keys are of the form `qs_tile_<index>_*` so they coexist safely with other app preferences.
 */
object DeviceTilePrefs {
    private fun addressKey(index: Int) = "qs_tile_${index}_address"
    private fun settingKey(index: Int) = "qs_tile_${index}_key"
    private fun cycleKey(index: Int) = "qs_tile_${index}_cycle"
    private fun lockscreenKey(index: Int) = "qs_tile_${index}_lockscreen"

    /**
     * Saves the device/setting assignment for tile [tileIndex].
     * Overwrites any existing assignment for that index.
     */
    fun save(tileIndex: Int, deviceAddress: String, settingKey: String) {
        GBApplication.getPrefs().preferences.edit {
            putString(addressKey(tileIndex), deviceAddress)
            putString(settingKey(tileIndex), settingKey)
        }
    }

    /**
     * Returns the (deviceAddress, settingKey) pair stored for [tileIndex], or null if the tile
     * has not been assigned.
     */
    fun load(tileIndex: Int): Pair<String, String>? {
        val prefs = GBApplication.getPrefs()
        val address = prefs.getString(addressKey(tileIndex), null) ?: return null
        val key = prefs.getString(settingKey(tileIndex), null) ?: return null
        return address to key
    }

    /** Removes the assignment and cycle-value restriction for tile [tileIndex]. */
    fun clear(tileIndex: Int) {
        GBApplication.getPrefs().preferences.edit {
            remove(addressKey(tileIndex))
            remove(settingKey(tileIndex))
            remove(cycleKey(tileIndex))
        }
    }

    /** Returns whether tile [tileIndex] may be toggled from the lock screen (default: true). */
    fun loadLockScreen(tileIndex: Int): Boolean {
        return GBApplication.getPrefs().getBoolean(lockscreenKey(tileIndex), true)
    }

    /** Persists the lock-screen toggle permission for tile [tileIndex]. */
    fun saveLockScreen(tileIndex: Int, allowed: Boolean) {
        GBApplication.getPrefs().preferences.edit {
            putBoolean(lockscreenKey(tileIndex), allowed)
        }
    }

    /**
     * Saves the ordered subset of preference values the tile should cycle through. An empty list means
     * "all values" (no restriction). Values are newline-separated strings.
     */
    fun saveCycleValues(tileIndex: Int, values: List<String>) {
        GBApplication.getPrefs().preferences.edit {
            putString(cycleKey(tileIndex), values.joinToString("\n"))
        }
    }

    /**
     * Returns the stored cycle-through values for tile [tileIndex]. An empty list means "all
     * values"; a non-empty list restricts cycling to those values (in their declared order).
     */
    fun loadCycleValues(tileIndex: Int): List<String> {
        @Suppress("DEPRECATION")
        val raw = GBApplication.getPrefs()
            .getString(cycleKey(tileIndex), null)
            .takeUnless { it.isNullOrEmpty() } ?: return emptyList()
        return raw.split("\n")
    }
}
