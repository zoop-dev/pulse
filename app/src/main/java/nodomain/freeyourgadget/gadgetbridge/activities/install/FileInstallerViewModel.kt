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

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import org.slf4j.LoggerFactory

sealed class InstallDeviceUiState {
    object Loading : InstallDeviceUiState()
    data class Success(val results: List<Pair<GBDevice, InstallHandler>>) : InstallDeviceUiState()
    object NoUri : InstallDeviceUiState()
    data class Error(val message: String) : InstallDeviceUiState()
}

class FileInstallerViewModel(private val application: GBApplication, private val fwOptions: Bundle) : ViewModel() {
    private val _uiState = MutableLiveData<InstallDeviceUiState>()
    val uiState: LiveData<InstallDeviceUiState> = _uiState

    fun findCompatibleDevices(uri: Uri?) {
        if (uri == null) {
            _uiState.value = InstallDeviceUiState.NoUri
            return
        }

        _uiState.value = InstallDeviceUiState.Loading
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                val compatibleDevices = mutableListOf<Pair<GBDevice, InstallHandler>>()
                for (device in getAllDeviceTypesConnectedFirst()) {
                    val coordinator = device.deviceCoordinator
                    try {
                        val handler = coordinator.findInstallHandler(uri, fwOptions, application.applicationContext)
                        if (handler != null) {
                            LOG.debug(
                                "Found compatible install handler {} for {}",
                                handler.javaClass.simpleName,
                                device
                            )
                            compatibleDevices.add(Pair(device, handler))
                        }
                    } catch (e: Exception) {
                        LOG.error("Error finding install handler for $device", e)
                    }
                }
                compatibleDevices
            }

            if (results.isNotEmpty()) {
                _uiState.value = InstallDeviceUiState.Success(results)
            } else {
                _uiState.value = InstallDeviceUiState.Error(
                    application.getString(R.string.fwinstaller_no_compatible_device_found, uri)
                )
            }
        }
    }

    private fun getAllDeviceTypesConnectedFirst(): List<GBDevice> {
        return application.deviceManager.devices
            .sortedWith { d1, d2 ->
                when {
                    d1.isConnected && !d2.isConnected -> -1
                    d2.isConnected && !d1.isConnected -> 1
                    else -> d1.aliasOrName.compareTo(d2.aliasOrName)
                }
            }
            .toList()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FileInstallerViewModel::class.java)
    }
}
