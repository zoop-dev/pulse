package nodomain.freeyourgadget.gadgetbridge.activities.workouts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.activities.maps.MapsSettingsFragment
import nodomain.freeyourgadget.gadgetbridge.activities.maps.MapsTrackActivity
import nodomain.freeyourgadget.gadgetbridge.activities.maps.MapsTrackViewModel
import nodomain.freeyourgadget.gadgetbridge.databinding.FragmentWorkoutGpsBinding
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate
import nodomain.freeyourgadget.gadgetbridge.util.maps.MapsManager
import org.slf4j.LoggerFactory
import java.io.File


class WorkoutGpsFragment : Fragment() {
    private val viewModel: MapsTrackViewModel by viewModels()
    private lateinit var binding: FragmentWorkoutGpsBinding

    private lateinit var mapsManager: MapsManager
    private var mapValid = true

    private var inputFile: File? = null

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (MapsSettingsFragment.ACTION_SETTING_CHANGE == intent.action) {
                if (!isResumed) {
                    mapValid = false
                } else {
                    mapsManager.reload()
                }
                return
            }

            LOG.warn("Unknown action {}", intent.action)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWorkoutGpsBinding.inflate(inflater, container, false)

        binding.mapView.setBuiltInZoomControls(false)

        mapsManager = MapsManager(requireContext(), binding.mapView)
        mapsManager.loadMaps()
        if (mapsManager.isMapLoaded) {
            binding.missingMapsWarning.visibility = View.GONE
        }

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mReceiver, IntentFilter().apply {
            addAction(MapsSettingsFragment.ACTION_SETTING_CHANGE)
        })

        inputFile?.let { viewModel.loadTrackData(it) }

        observeViewModel()

        return binding.root
    }

    private fun observeViewModel() {
        viewModel.trackPoints.observe(viewLifecycleOwner) { points ->
            if (points.isNotEmpty()) {
                lifecycleScope.launch {
                    drawTrack(points)
                }
            } else {
                LOG.warn("No track points to display or file was empty.")
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it.localizedMessage ?: it.javaClass.simpleName)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mapValid) {
            mapsManager.reload()
            mapValid = true
        }
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mReceiver)
        mapsManager.onDestroy()
        super.onDestroyView()
    }

    fun setTrackData(inputFile: File?) {
        this.inputFile = inputFile
        if (inputFile != null) {
            viewModel.loadTrackData(inputFile)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.mapView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        binding.errorMessage.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingSpinner.visibility = View.GONE
        binding.mapView.visibility = View.INVISIBLE
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }

    private suspend fun drawTrack(trackPoints: List<GPSCoordinate>) {
        withContext(Dispatchers.Main) {
            try {
                mapsManager.setTrack(trackPoints)

                setupMapTouchListener()
            } catch (e: Exception) {
                LOG.error("Error drawing track", e)
                showError(e.localizedMessage ?: e.message ?: "Error drawing track")
            }
        }
    }

    private fun setupMapTouchListener() {
        binding.mapView.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var pressTime = 0L
            private var isDrag = false

            private val TAP_THRESHOLD = 10
            private val LONG_PRESS_TIME = 300

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDrag = false
                        startX = event.x
                        startY = event.y
                        pressTime = System.currentTimeMillis()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (kotlin.math.abs(startX - event.x) > TAP_THRESHOLD ||
                            kotlin.math.abs(startY - event.y) > TAP_THRESHOLD
                        ) {
                            isDrag = true
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!isDrag && System.currentTimeMillis() - pressTime < LONG_PRESS_TIME) {
                            openMapActivity()
                        }
                    }
                }
                return true
            }

            private fun openMapActivity() {
                val intent = Intent(getContext(), MapsTrackActivity::class.java)
                intent.putExtra("file", inputFile)
                startActivity(intent)
            }
        })
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WorkoutGpsFragment::class.java)
    }
}
