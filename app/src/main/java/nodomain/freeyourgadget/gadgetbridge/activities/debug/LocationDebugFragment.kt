package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.os.Bundle
import android.os.Handler
import androidx.preference.Preference
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LocationDebugFragment : AbstractDebugFragment() {
    private val handler = Handler()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_location, rootKey)

        onClick(PREF_DEBUG_OPENTRACKS_START) { OpenTracksController.startRecording(requireContext()) }
        onClick(PREF_DEBUG_OPENTRACKS_STOP) { OpenTracksController.stopRecording(requireContext()) }
        onClick(PREF_DEBUG_GPS_LISTENER_STOP) { GBLocationService.stop(requireContext(), null) }

        refreshOpenTracksStatus()
        scheduleRefresh()
    }

    override fun onStart() {
        super.onStart()
        refreshOpenTracksStatus()
        scheduleRefresh()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
    }

    private fun scheduleRefresh() {
        handler.postDelayed({ refreshOpenTracksStatus(); scheduleRefresh() }, 1000)
    }

    private fun refreshOpenTracksStatus() {
        val openTracksObserver = GBApplication.app().openTracksObserver
        LOG.debug("Refreshing OpenTracks status - {}", openTracksObserver != null)

        val pref = findPreference<Preference>(PREF_DEBUG_OPENTRACKS_STATUS)
        if (openTracksObserver == null) {
            pref?.summary = "Not running"
            return
        } else {
            val timeSecs = openTracksObserver.getTimeMillisChange() / 1000
            val distanceCM = openTracksObserver.getDistanceMeterChange() * 100
            pref?.summary = "TimeSec: $timeSecs, distanceCM $distanceCM"
        }
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(LocationDebugFragment::class.java)

        private const val PREF_HEADER_LOCATION_OPENTRACKS = "pref_header_location_opentracks"
        private const val PREF_DEBUG_OPENTRACKS_STATUS = "pref_debug_opentracks_status"
        private const val PREF_DEBUG_OPENTRACKS_START = "pref_debug_opentracks_start"
        private const val PREF_DEBUG_OPENTRACKS_STOP = "pref_debug_opentracks_stop"
        private const val PREF_HEADER_LOCATION_GPS_LISTENER = "pref_header_location_gps_listener"
        private const val PREF_DEBUG_GPS_LISTENER_STOP = "pref_debug_gps_listener_stop"
    }
}
