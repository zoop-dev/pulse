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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.QuickSettingType
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.QuickSettings
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import org.slf4j.LoggerFactory

/**
 * Base [TileService] for Gadgetbridge preset Quick Settings tiles. Each concrete subclass
 * represents one numbered tile slot and identifies itself via [tileIndex].
 *
 * When the tile is assigned (via [DeviceTilePrefs]), tapping it headlessly applies the stored
 * setting through [QuickSettings.apply]. When unassigned, tapping opens
 * [QuickSettingsTilesActivity] so the user can configure this tile slot.
 */
@RequiresApi(Build.VERSION_CODES.N)
abstract class AbstractDeviceTileService : TileService() {

    private var deviceAddress: String? = null

    /** Returns the zero-based index identifying this tile slot (0–9). */
    abstract fun tileIndex(): Int

    // Refreshes the tile whenever device connection state changes while the panel is open.
    private val deviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val changedDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(GBDevice.EXTRA_DEVICE, GBDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(GBDevice.EXTRA_DEVICE)
            }

            if (changedDevice == null || changedDevice.address != deviceAddress) {
                return
            }

            refreshTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        @Suppress("DEPRECATION")
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(deviceStateReceiver, IntentFilter(GBDevice.ACTION_DEVICE_CHANGED))
        refreshTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        @Suppress("DEPRECATION")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceStateReceiver)
    }

    override fun onClick() {
        LOG.debug("Tile {} got clicked", tileIndex())

        val assignment = DeviceTilePrefs.load(tileIndex())
        if (assignment == null) {
            launchConfigActivity()
            return
        }
        val (address, key) = assignment
        val device = GBApplication.app().deviceManager.getDeviceByAddress(address)
        val descriptor = device?.let { QuickSettings.find(address, key) }
        if (device == null || descriptor == null) {
            LOG.warn("Tile {} assignment ({}::{}) no longer valid - opening config", tileIndex(), address, key)
            launchConfigActivity()
            return
        }
        val applyAction = {
            val cycleValues = DeviceTilePrefs.loadCycleValues(tileIndex())
            QuickSettings.apply(this, device, descriptor, cycleValues)
            refreshTile()
        }
        if (isLocked && !DeviceTilePrefs.loadLockScreen(tileIndex())) {
            unlockAndRun(applyAction)
        } else {
            applyAction()
        }
    }

    private fun refreshTile() {
        LOG.debug("Refreshing tile {}", tileIndex())
        val tile = qsTile ?: return
        val assignment = DeviceTilePrefs.load(tileIndex())
        if (assignment == null) {
            deviceAddress = null
            tile.label = getString(R.string.qs_tile_not_assigned)
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = ""
            }
        } else {
            val (address, key) = assignment
            deviceAddress = address
            val device = GBApplication.app().deviceManager.getDeviceByAddress(address)
            val descriptor = device?.let { QuickSettings.find(address, key) }
            if (device == null || descriptor == null) {
                tile.label = getString(R.string.qs_tile_not_assigned)
                tile.state = Tile.STATE_INACTIVE
            } else {
                tile.label = getString(descriptor.title)
                tile.icon = Icon.createWithResource(this, descriptor.icon)
                when (descriptor.type) {
                    QuickSettingType.TOGGLE -> {
                        val value = QuickSettings.currentBool(address, key)
                        tile.state = if (value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            tile.subtitle = device.aliasOrName
                        }
                    }

                    QuickSettingType.LIST -> {
                        tile.state = Tile.STATE_ACTIVE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            tile.subtitle = QuickSettings.currentLabel(this, device, key)
                                ?: device.aliasOrName
                        }
                    }
                }
                if (!device.isInitialized) {
                    tile.state = Tile.STATE_UNAVAILABLE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = getString(R.string.qs_tile_not_connected, device.aliasOrName)
                    }
                }
            }
        }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchConfigActivity() {
        val intent = Intent(this, QuickSettingsTilesActivity::class.java)
            .putExtra(QuickSettingsTilesActivity.EXTRA_TILE_INDEX, tileIndex())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this,
                tileIndex(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AbstractDeviceTileService::class.java)
    }
}
