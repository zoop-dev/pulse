package nodomain.freeyourgadget.gadgetbridge.activities.maps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParseException
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class MapsTrackViewModel : ViewModel() {
    private val _trackPoints = MutableLiveData<List<GPSCoordinate>>()
    val trackPoints: LiveData<List<GPSCoordinate>> = _trackPoints

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<Exception?>()
    val error: LiveData<Exception?> = _error

    fun loadTrackData(trackFile: File) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val points = fetchTrackPoints(trackFile)
                if (points.isNotEmpty()) {
                    _trackPoints.postValue(points)
                } else {
                    LOG.warn("No track points found in file: ${trackFile.name}")
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

    private suspend fun fetchTrackPoints(trackFile: File): List<GPSCoordinate> {
        return withContext(Dispatchers.IO) {
            getActivityPoints(trackFile).mapNotNull { it.location }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MapsTrackViewModel::class.java)

        fun getActivityPoints(trackFile: File): List<ActivityPoint> {
            try {
                when {
                    trackFile.name.endsWith(".gpx") -> {
                        FileInputStream(trackFile).use { inputStream ->
                            val gpxParser = GpxParser(inputStream)
                            return gpxParser.gpxFile.activityPoints
                        }
                    }

                    trackFile.name.endsWith(".fit") -> {
                        val fitFile = FitFile.parseIncoming(trackFile)
                        return fitFile.records
                            .filterIsInstance<FitRecord>()
                            .map { it.toActivityPoint() }
                    }

                    else -> {
                        LOG.warn("Unknown file type: ${trackFile.name}")
                    }
                }
            } catch (e: IOException) {
                LOG.error("Failed to open $trackFile", e)
            } catch (e: GpxParseException) {
                LOG.error("Failed to parse gpx file", e)
            } catch (e: Exception) {
                LOG.error("Failed to parse fit file", e)
            }

            return emptyList()
        }
    }
}
