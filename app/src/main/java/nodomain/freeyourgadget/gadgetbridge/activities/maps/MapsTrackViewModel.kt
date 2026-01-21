package nodomain.freeyourgadget.gadgetbridge.activities.maps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate
import org.slf4j.LoggerFactory

class MapsTrackViewModel : ViewModel() {
    private val _trackPoints = MutableLiveData<List<GPSCoordinate>>()
    val trackPoints: LiveData<List<GPSCoordinate>> = _trackPoints

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<Exception?>()
    val error: LiveData<Exception?> = _error

    fun loadTrackData(baseActivitySummary: BaseActivitySummary, gbDevice: GBDevice) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val points = fetchTrackPoints(baseActivitySummary, gbDevice)
                if (points.isNotEmpty()) {
                    _trackPoints.postValue(points)
                } else {
                    LOG.warn("No track points found for: ${baseActivitySummary.name}")
                    _trackPoints.postValue(emptyList())
                }
            } catch (e: Exception) {
                LOG.error("Error loading track points", e)
                _error.postValue(e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun fetchTrackPoints(
        baseActivitySummary: BaseActivitySummary,
        gbDevice: GBDevice
    ): List<GPSCoordinate> {
        val activityTrackProvider =
            gbDevice.deviceCoordinator.getActivityTrackProvider(gbDevice, GBApplication.getContext())
        return withContext(Dispatchers.IO) {
            val activityTrack = activityTrackProvider
                .getActivityTrack(baseActivitySummary) ?: return@withContext listOf()
            return@withContext activityTrack.allPoints.mapNotNull { it.location }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MapsTrackViewModel::class.java)
    }
}
