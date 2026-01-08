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
package nodomain.freeyourgadget.gadgetbridge.activities

import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityDeviceDeleteBinding
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates

class DeviceDeleteActivity : AbstractGBActivity() {
    private lateinit var binding: ActivityDeviceDeleteBinding
    private lateinit var device: GBDevice
    private var deleteFiles by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDeleteBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DEVICE, GBDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DEVICE)
        }!!

        deleteFiles = intent.getBooleanExtra(EXTRA_DELETE_FILES, true)

        title = getString(R.string.controlcenter_delete_device_name, device.aliasOrName)

        binding.deleteDeviceName.text = getString(R.string.controlcenter_delete_device_name, device.aliasOrName)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)

        startDeviceDelete()
    }

    private fun startDeviceDelete() {
        binding.deleteProgressBar.visibility = View.VISIBLE
        binding.deleteProgressBar.keepScreenOn = true

        lifecycleScope.launch {
            val exception = performDeviceDelete()
            handleDeleteResult(exception)
        }
    }

    private suspend fun performDeviceDelete(): Exception? = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()

        try {
            val coordinator = device.deviceCoordinator

            // Delete device and files
            coordinator.deleteDevice(device, deleteFiles)

            // Unpair Bluetooth device
            BondingUtil.Unpair(this@DeviceDeleteActivity, device.address)

            // Remove dynamic shortcut (Android R+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                removeDynamicShortcut(device)
            }
        } catch (ex: Exception) {
            LOG.error("Error deleting device", ex)
            return@withContext ex
        }

        val end = System.currentTimeMillis()

        LOG.debug("Deleting the device took {}ms", end - start)

        if (end - start < 1000L) {
            // Add a small delay to prevent the activity from blinking too fast
            delay(1000L - (end - start))
        }

        return@withContext null
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    fun removeDynamicShortcut(device: GBDevice) {
        val shortcutManager = applicationContext.getSystemService(SHORTCUT_SERVICE) as ShortcutManager

        shortcutManager.removeDynamicShortcuts(mutableListOf<String?>(device.address))
    }

    private fun handleDeleteResult(exception: Exception?) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.deleteProgressBar.keepScreenOn = false
        binding.deleteProgressBar.visibility = View.GONE

        // Refresh device list
        val refreshIntent = Intent(DeviceManager.ACTION_REFRESH_DEVICELIST)
        LocalBroadcastManager.getInstance(this).sendBroadcast(refreshIntent)

        if (exception == null) {
            GB.toast(
                this,
                getString(R.string.device_deleted_successfully),
                Toast.LENGTH_SHORT,
                GB.INFO
            )
            finish()
        } else {
            val errorMsg = getString(
                R.string.error_deleting_device,
                exception.localizedMessage ?: "Unknown error"
            )
            binding.deleteStatusText.text = errorMsg
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DeviceDeleteActivity::class.java)

        const val EXTRA_DEVICE = "device"
        const val EXTRA_DELETE_FILES = "delete_files"
    }
}
