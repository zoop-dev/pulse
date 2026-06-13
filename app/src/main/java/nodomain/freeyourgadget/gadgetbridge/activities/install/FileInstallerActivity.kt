/*  Copyright (C) 2025-2026 José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.activities.install

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.adapter.DeviceInstallAdapter
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityFileInstallerBinding
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_OPTIONS
import org.slf4j.LoggerFactory

class FileInstallerActivity : AppCompatActivity() {
    private val viewModel: FileInstallerViewModel by viewModels {
        FileInstallerViewModelFactory(GBApplication.app(),
            intent.getParcelableExtra(EXTRA_OPTIONS) ?: Bundle.EMPTY
        )
    }

    private lateinit var binding: ActivityFileInstallerBinding
    private lateinit var deviceAdapter: DeviceInstallAdapter

    private val handlers: MutableMap<GBDevice, InstallHandler> = mutableMapOf()
    private var currentUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileInstallerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUri = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)

        // Set up the RecyclerView
        deviceAdapter = DeviceInstallAdapter { selectedDevice ->
            launchInstallActivity(selectedDevice, handlers[selectedDevice], currentUri)
        }
        binding.recyclerViewDevices.apply {
            adapter = deviceAdapter
            layoutManager = LinearLayoutManager(this@FileInstallerActivity)
        }

        viewModel.findCompatibleDevices(currentUri)
        viewModel.uiState.observe(this) { state ->
            updateUi(state)
        }
    }

    private fun updateUi(state: InstallDeviceUiState) {
        // Hide everything initially, then show the relevant one
        binding.fileInstallerLoadingProgress.visibility = View.GONE
        binding.recyclerViewDevices.visibility = View.GONE
        binding.textInstallerStatus.visibility = View.GONE

        when (state) {
            is InstallDeviceUiState.Loading -> {
                binding.fileInstallerLoadingProgress.visibility = View.VISIBLE
                LOG.debug("Loading compatible devices...")
            }
            is InstallDeviceUiState.Success -> {
                if (state.results.isEmpty()) { // Ideally this should be caught by Error state
                    binding.textInstallerStatus.text = getString(R.string.fwinstaller_no_compatible_device_found, currentUri)
                    binding.textInstallerStatus.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewDevices.visibility = View.VISIBLE
                    handlers.clear()
                    for (pair in state.results) {
                        handlers.put(pair.first, pair.second)
                    }
                    deviceAdapter.submitList(state.results.map { pair -> pair.first })
                    if (state.results.size == 1) {
                        // Single device was found - start the install activity directly
                        val (device, handler) = state.results[0]
                        launchInstallActivity(device, handler, currentUri)
                    }
                }
            }
            is InstallDeviceUiState.Error -> {
                binding.textInstallerStatus.text = state.message
                binding.textInstallerStatus.visibility = View.VISIBLE
                LOG.error("Error: ${state.message}")
            }
            is InstallDeviceUiState.NoUri -> {
                // Should never happen
                @SuppressLint("SetTextI18n")
                binding.textInstallerStatus.text = "No file URI provided to install."
                binding.textInstallerStatus.visibility = View.VISIBLE
                LOG.warn("No URI provided.")
            }
        }
    }

    private fun launchInstallActivity(selectedDevice: GBDevice, installHandler: InstallHandler?, uri: Uri?) {
        if (uri == null) {
            Toast.makeText(this, "Cannot install without a file URI.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (installHandler == null) {
            // This should never happen
            Toast.makeText(this, "Failed to find installHandler", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        LOG.info(
            "Launching install activity {} for {}",
            installHandler.installActivity.simpleName,
            selectedDevice
        )

        val intent = Intent(this, installHandler.installActivity).apply {
            setDataAndType(uri, contentResolver.getType(uri))
            putExtra(GBDevice.EXTRA_DEVICE, selectedDevice)
            setAction(Intent.ACTION_VIEW)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_TASK_ON_HOME or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val installAppBundle = getIntent().getParcelableExtra<Bundle?>(EXTRA_OPTIONS)
        if (installAppBundle != null) {
            intent.putExtra(EXTRA_OPTIONS, installAppBundle)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FileInstallerActivity::class.java)
    }
}


// FileInstallerViewModelFactory remains the same
class FileInstallerViewModelFactory(
    private val application: GBApplication,
    private val fwOptions: Bundle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileInstallerViewModel::class.java)) {
            return FileInstallerViewModel(application, fwOptions) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
