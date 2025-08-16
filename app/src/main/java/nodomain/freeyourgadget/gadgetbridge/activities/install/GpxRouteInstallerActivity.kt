package nodomain.freeyourgadget.gadgetbridge.activities.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.launch
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.adapter.ItemWithDetailsAdapter
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityInstallerGpxBinding
import nodomain.freeyourgadget.gadgetbridge.devices.GpxRouteInstallHandler
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile
import nodomain.freeyourgadget.gadgetbridge.util.maps.MapsManager

class GpxRouteInstallerActivity : AbstractGBActivity(), InstallActivity {
    companion object {
        private const val ITEM_DETAILS = "details"
    }

    private lateinit var binding: ActivityInstallerGpxBinding
    private lateinit var device: GBDevice
    private lateinit var installHandler: GpxRouteInstallHandler
    private lateinit var currentUri: Uri
    private var mapsManager: MapsManager? = null

    private var mayConnect: Boolean = false
    private var finished: Boolean = false

    private val items: ArrayList<ItemWithDetails> = ArrayList()
    private lateinit var itemAdapter: ItemWithDetailsAdapter

    private var details: ArrayList<ItemWithDetails> = ArrayList()
    private lateinit var detailsAdapter: ItemWithDetailsAdapter

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GBDevice.ACTION_DEVICE_CHANGED -> {
                    val changedDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(GBDevice.EXTRA_DEVICE, GBDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(GBDevice.EXTRA_DEVICE)
                    }

                    if (changedDevice == null || changedDevice != device) {
                        return;
                    }
                    if (finished) return
                    refreshBusyState(device)
                    if (!device.isInitialized) {
                        setInstallEnabled(false)
                        if (mayConnect) {
                            GB.toast(
                                this@GpxRouteInstallerActivity,
                                getString(R.string.connecting),
                                Toast.LENGTH_SHORT,
                                GB.INFO
                            )
                            connect()
                        } else {
                            setInfoText(
                                getString(
                                    R.string.fwappinstaller_connection_state,
                                    device.getStateString(context)
                                )
                            )
                        }
                    } else {
                        validateInstallation()
                    }
                }

                GB.ACTION_SET_PROGRESS_BAR -> {
                    if (intent.hasExtra(GB.PROGRESS_BAR_INDETERMINATE)) {
                        setProgressIndeterminate(intent.getBooleanExtra(GB.PROGRESS_BAR_INDETERMINATE, false))
                    }
                    if (intent.hasExtra(GB.PROGRESS_BAR_PROGRESS)) {
                        setProgressIndeterminate(false)
                        setProgressBarProgress(intent.getIntExtra(GB.PROGRESS_BAR_PROGRESS, 0))
                    }
                }

                GB.ACTION_SET_PROGRESS_TEXT -> {
                    intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE)?.let { setProgressText(it) }
                }

                GB.ACTION_SET_INFO_TEXT -> {
                    intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE)?.let { setInfoText(it) }
                }

                GB.ACTION_DISPLAY_MESSAGE -> {
                    val message = intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE)
                    val severity = intent.getIntExtra(GB.DISPLAY_MESSAGE_SEVERITY, GB.INFO)
                    if (message != null) {
                        addMessage(message, severity)
                    }
                }

                GB.ACTION_SET_FINISHED -> {
                    finished = true
                    setProgressBarVisibility(false)
                    setInstallEnabled(false)
                    setCloseEnabled(true)
                }
            }
        }
    }

    private fun refreshBusyState(dev: GBDevice) {
        if (dev.isConnecting || dev.isBusy) {
            binding.installProgressBar.visibility = View.VISIBLE
        } else {
            val wasBusy = binding.installProgressBar.visibility != View.GONE
            if (wasBusy) {
                binding.installProgressBar.visibility = View.GONE
                // done!
            }
        }
    }

    private fun setProgressIndeterminate(indeterminate: Boolean) {
        binding.installProgressBar.visibility = View.VISIBLE
        binding.installProgressBar.isIndeterminate = indeterminate
    }

    private fun setProgressBarProgress(progress: Int) {
        binding.installProgressBar.progress = progress
    }

    fun setProgressText(text: CharSequence) {
        binding.installProgressText.visibility = View.VISIBLE
        binding.installProgressText.text = text
    }

    private fun connect() {
        mayConnect = false // only do that once per #onCreate
        GBApplication.deviceService(device)?.connect()
    }

    private fun validateInstallation() {
        installHandler.validateInstallation(this, device)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallerGpxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intentDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(GBDevice.EXTRA_DEVICE, GBDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(GBDevice.EXTRA_DEVICE)
        }
        if (intentDevice == null) {
            GB.toast(this, "No device provided to GpxRouteInstallerActivity", Toast.LENGTH_LONG, GB.ERROR)
            finish()
            return
        }
        device = intentDevice

        details = if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelableArrayList(ITEM_DETAILS, ItemWithDetails::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelableArrayList(ITEM_DETAILS)
            } ?: ArrayList()
        } else {
            ArrayList()
        }

        val intentUri = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
        if (intentUri == null) {
            GB.toast(this, "No URI provided to GpxRouteInstallerActivity", Toast.LENGTH_LONG, GB.ERROR)
            finish()
            return
        }
        currentUri = intentUri

        val deviceInstallHandler = device.deviceCoordinator.findInstallHandler(currentUri, this)
        if (deviceInstallHandler == null) {
            // Should never happen if we got here
            GB.toast(this, getString(R.string.installer_activity_unable_to_find_handler), Toast.LENGTH_LONG, GB.ERROR)
            finish()
            return
        }

        val gpxRouteInstallHandler = deviceInstallHandler as? GpxRouteInstallHandler
        if (gpxRouteInstallHandler == null) {
            GB.toast(this, "Install handler is not GpxRouteInstallHandler", Toast.LENGTH_LONG, GB.ERROR)
            finish()
            return
        }
        installHandler = deviceInstallHandler

        val gpxFile = gpxRouteInstallHandler.gpxFile
        if (gpxFile == null) {
            GB.toast(this, "No gpx file!", Toast.LENGTH_LONG, GB.ERROR)
            finish()
            return
        }

        mayConnect = true
        itemAdapter = ItemWithDetailsAdapter(this, items)
        binding.itemListView.adapter = itemAdapter
        detailsAdapter = ItemWithDetailsAdapter(this, details).apply {
            size = ItemWithDetailsAdapter.SIZE_SMALL
        }
        binding.detailsListView.adapter = detailsAdapter

        setInstallEnabled(false)
        val filter = IntentFilter().apply {
            addAction(GBDevice.ACTION_DEVICE_CHANGED)
            addAction(GB.ACTION_DISPLAY_MESSAGE)
            addAction(GB.ACTION_SET_PROGRESS_BAR)
            addAction(GB.ACTION_SET_PROGRESS_TEXT)
            addAction(GB.ACTION_SET_INFO_TEXT)
            addAction(GB.ACTION_SET_FINISHED)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        binding.gpxMapView.setBuiltInZoomControls(false)
        binding.gpxMapView.setOnTouchListener { _, _ -> true } // ignore touch
        mapsManager = MapsManager(this, binding.gpxMapView).apply {
            loadMaps()
        }
        loadMapTrack(gpxFile)

        binding.trackNameEditText.setText(gpxRouteInstallHandler.name)

        binding.installButton.setOnClickListener {
            val trackName = binding.trackNameEditText.text?.toString()?.trim() ?: ""
            if (trackName.isEmpty()) {
                binding.trackNameInputLayout.error = getString(R.string.track_name_required)
                return@setOnClickListener
            } else {
                binding.trackNameInputLayout.error = null
            }

            setInstallEnabled(false)
            installHandler.onStartInstall(device)

            val bundle = Bundle().apply {
                putString(GpxRouteInstallHandler.EXTRA_TRACK_NAME, trackName)
            }
            GBApplication.deviceService(device)?.onInstallApp(currentUri, bundle)
        }

        binding.closeButton.setOnClickListener { finish() }

        setInfoText(getString(R.string.installer_activity_wait_while_determining_status))

        if (!device.isConnected) {
            if (mayConnect) {
                connect()
            }
        } else {
            GBApplication.deviceService(device)?.requestDeviceInfo()
        }
    }

    private fun loadMapTrack(gpxFile: GpxFile) {
        lifecycleScope.launch {
            val points = gpxFile.activityPoints.mapNotNull { it.location }

            if (points.isNotEmpty()) {
                mapsManager?.setTrack(points)
            } else {
                binding.gpxMapView.visibility = View.GONE
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(ITEM_DETAILS, details)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        mapsManager?.onDestroy()
        super.onDestroy()
    }

    override fun setInfoText(text: String?) {
        binding.infoTextView.text = text
    }

    override fun setPreview(bitmap: Bitmap?) {
        binding.previewImage.setImageBitmap(bitmap)
        binding.previewImage.visibility = if (bitmap == null) View.GONE else View.VISIBLE
    }

    override fun getInfoText(): CharSequence? = binding.infoTextView.text

    override fun setInstallEnabled(enable: Boolean) {
        val isEnabled = device.isConnected && enable
        binding.installButton.isEnabled = isEnabled
        binding.installButton.visibility = if (isEnabled) View.VISIBLE else View.GONE
        binding.gpxMapView.visibility = if (isEnabled) View.VISIBLE else View.GONE
        binding.trackNameInputLayout.visibility = if (isEnabled) View.VISIBLE else View.GONE
        if (isEnabled) {
            setCloseEnabled(false)
        }
    }

    override fun setCloseEnabled(enable: Boolean) {
        binding.closeButton.isEnabled = enable
        binding.closeButton.visibility = if (enable) View.VISIBLE else View.GONE
    }

    override fun clearInstallItems() {
        items.clear()
        itemAdapter.notifyDataSetChanged()
    }

    override fun setInstallItem(item: ItemWithDetails) {
        items.clear()
        items.add(item)
        itemAdapter.notifyDataSetChanged()
    }

    private fun addMessage(message: String, severity: Int) {
        details.add(GenericItem(message))
        detailsAdapter.notifyDataSetChanged()
    }
}
