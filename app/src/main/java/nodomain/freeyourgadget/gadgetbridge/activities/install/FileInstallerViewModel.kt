package nodomain.freeyourgadget.gadgetbridge.activities.install

import android.net.Uri
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

class FileInstallerViewModel(private val application: GBApplication) : ViewModel() {
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
                        val handler = coordinator.findInstallHandler(uri, application.applicationContext)
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
