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

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.QuickSettings
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

/**
 * Configuration activity for the Quick Settings tiles.
 *
 * When launched with [EXTRA_TILE_INDEX] (e.g. from [AbstractDeviceTileService] when the tile is
 * unassigned), it jumps directly to the device picker for that tile slot. When launched without
 * the extra (e.g. from the main settings), it shows a list of all [NUM_TILES] slots so the user
 * can configure any of them.
 */
@RequiresApi(Build.VERSION_CODES.N)
class QuickSettingsTilesActivity : AbstractGBActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tileIndex = intent.getIntExtra(EXTRA_TILE_INDEX, -1)
        if (tileIndex in 0 until NUM_TILES) {
            showDevicePicker(tileIndex)
        } else {
            showTileList()
        }
    }

    private fun showTileList() {
        val labels = (0 until NUM_TILES).map { i ->
            val assignment = DeviceTilePrefs.load(i)
            if (assignment != null) {
                val (address, key) = assignment
                val device = GBApplication.app().deviceManager.getDeviceByAddress(address)
                val desc = device?.let { QuickSettings.find(address, key) }
                if (device != null && desc != null) {
                    getString(R.string.qs_tile_slot_assigned, i + 1, device.aliasOrName, getString(desc.title))
                } else {
                    getString(R.string.qs_tile_slot_unassigned, i + 1)
                }
            } else {
                getString(R.string.qs_tile_slot_unassigned, i + 1)
            }
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.qs_tile_activity_title)
            .setItems(labels) { _, which -> showDevicePicker(which) }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun showDevicePicker(tileIndex: Int) {
        val devices = GBApplication.app().deviceManager.devices
            .filter { QuickSettings.listFor(it).isNotEmpty() }
        val deviceLabels = (listOf(getString(R.string.qs_tile_clear)) + devices.map { it.aliasOrName })
            .toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.widget_settings_select_device_title)
            .setItems(deviceLabels) { _, which ->
                if (which == 0) {
                    DeviceTilePrefs.clear(tileIndex)
                    refreshTile(tileIndex)
                    finish()
                } else {
                    showSettingPicker(tileIndex, devices[which - 1])
                }
            }
            .setOnCancelListener {
                // If launched from a tile go back to the OS; if launched from settings go back to the tile list
                if (intent.hasExtra(EXTRA_TILE_INDEX)) finish() else showTileList()
            }
            .show()
    }

    private fun showSettingPicker(tileIndex: Int, device: GBDevice) {
        val settings = QuickSettings.listFor(device)
        val labels = settings.map { getString(it.title) }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(device.aliasOrName)
            .setItems(labels) { _, which ->
                DeviceTilePrefs.save(tileIndex, device.address, settings[which].key)
                refreshTile(tileIndex)
                openTileSettings(tileIndex)
            }
            .setOnCancelListener { showDevicePicker(tileIndex) }
            .show()
    }

    private fun refreshTile(tileIndex: Int) {
        val className = "$packageName.activities.quicksettings.DeviceTileService$tileIndex"
        TileService.requestListeningState(this, ComponentName(packageName, className))
    }

    /**
     * Once a device and setting have been picked, hand off to [QuickSettingsPreferencesActivity]'s
     * sub-screen for this tile so the user can review/finish the setup (e.g. restrict cycle
     * values, allow lock screen access) instead of being left on a blank dialog activity.
     */
    private fun openTileSettings(tileIndex: Int) {
        val intent = Intent(this, QuickSettingsPreferencesActivity::class.java)
            .putExtra(AbstractSettingsActivityV2.EXTRA_PREF_SCREEN, "qs_tile_$tileIndex")
        startActivity(intent)
        finish()
    }

    companion object {
        /** Intent extra: zero-based tile index to configure directly. */
        const val EXTRA_TILE_INDEX = "tile_index"

        /** Total number of preset tile slots. */
        const val NUM_TILES = 10
    }
}
