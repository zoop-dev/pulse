/*  Copyright (C) 2024-2025 José Rebelo, a0z

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
package nodomain.freeyourgadget.gadgetbridge.activities.maps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.MenuProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityMapsTrackBinding
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.maps.MapsManager
import org.slf4j.LoggerFactory
import java.io.File

class MapsTrackActivity : AbstractGBActivity(), MenuProvider {
    private lateinit var binding: ActivityMapsTrackBinding
    private val viewModel: MapsTrackViewModel by viewModels()

    private lateinit var mapsManager: MapsManager
    private var mapValid = true

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (MapsSettingsFragment.ACTION_SETTING_CHANGE == intent.action) {
                mapValid = false
                return
            }

            LOG.warn("Unknown action: {}", intent.action)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = intent.extras?.getSerializable("file") as? File
        if (file == null) {
            GB.toast(this, "No file provided", Toast.LENGTH_LONG, GB.ERROR)
            finish()
            return
        }

        addMenuProvider(this)

        mapsManager = MapsManager(this, binding.mapView)
        mapsManager.loadMaps()

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, IntentFilter().apply {
            addAction(MapsSettingsFragment.ACTION_SETTING_CHANGE)
        })

        viewModel.loadTrackData(file)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.trackPoints.observe(this) { points ->
            if (points.isNotEmpty()) {
                mapsManager.setTrack(points)
            } else {
                LOG.warn("No track points to display or file was empty.")
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // TODO loading spinner?
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                GB.toast(this, it.localizedMessage, Toast.LENGTH_LONG, GB.ERROR)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver)
        mapsManager.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (!mapValid) {
            mapsManager.reload()
            mapValid = true
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.maps_track_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.maps_settings -> {
                val enableIntent = Intent(this, MapsSettingsActivity::class.java)
                startActivity(enableIntent)
                true
            }

            else -> false
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MapsTrackActivity::class.java)
    }
}
