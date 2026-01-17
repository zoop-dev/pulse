package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.CameraActivity
import nodomain.freeyourgadget.gadgetbridge.activities.PermissionsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.welcome.WelcomeActivity
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.DeviceTypeDialog
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.Boolean
import kotlin.Any
import kotlin.String
import kotlin.let

class MainDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_main, rootKey)

        onClick(PREF_DEBUG_ADD_TEST_DEVICE) {
            DeviceTypeDialog(requireActivity(), R.string.add_test_device, null).show { address, deviceType ->
                DeviceTypeDialog.createTestDevice(requireContext(), deviceType, address, null)
            }
        }

        onClick(PREF_DEBUG_SHARE_LOGS) { showLogSharingWarning() }
        findPreference<SwitchPreferenceCompat>(LOG_TO_FILE)?.let {
            it.setOnPreferenceChangeListener { _: Preference?, newVal: Any? ->
                val doEnable = Boolean.TRUE == newVal
                try {
                    if (doEnable) {
                        FileUtils.getExternalFilesDir() // ensures that it is created
                    }
                    GBApplication.setupLogging(doEnable)
                } catch (ex: IOException) {
                    GB.toast(
                        requireContext().applicationContext,
                        getString(R.string.error_creating_directory_for_logfiles, ex.localizedMessage),
                        Toast.LENGTH_LONG,
                        GB.ERROR,
                        ex
                    )
                }
                true
            }

            // If we didn't manage to initialize file logging, disable the preference
            if (!GBApplication.getLogging().isFileLoggerInitialized) {
                it.isEnabled = false
                it.setSummary(R.string.pref_write_logfiles_not_available)
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            findPreference<Preference>(PREF_DEBUG_COMPANION_DEVICES)?.isVisible = false
        }

        onClick(PREF_DEBUG_ACTIVITY_CAMERA) {
            val intent = Intent(requireContext().applicationContext, CameraActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(
                    CameraActivity.intentExtraEvent,
                    GBDeviceEventCameraRemote.eventToInt(GBDeviceEventCameraRemote.Event.OPEN_CAMERA)
                )
            }
            requireContext().startActivity(intent)
        }

        onClick(PREF_DEBUG_ACTIVITY_WELCOME) {
            requireContext().startActivity(Intent(requireContext().applicationContext, WelcomeActivity::class.java))
        }

        onClick(PREF_DEBUG_ACTIVITY_PERMISSIONS) {
            requireContext().startActivity(Intent(requireContext().applicationContext, PermissionsActivity::class.java))
        }
    }

    private fun showLogSharingWarning() {
        MaterialAlertDialogBuilder(requireContext())
            .setCancelable(true)
            .setTitle(R.string.warning)
            .setMessage(R.string.share_log_warning)
            .setPositiveButton(R.string.ok) { _, _ -> shareLog() }
            .setNegativeButton(R.string.Cancel) { _, _ -> }
            .show()
    }

    private fun shareLog() {
        val fileName = GBApplication.getLogPath()
        if (fileName == null || fileName.isEmpty()) {
            return
        }

        // Flush the logs, so that we ensure latest lines are also there
        GBApplication.getLogging().setImmediateFlush(true)
        LOG.debug("Flushing logs before sharing")
        GBApplication.getLogging().setImmediateFlush(false)

        val logFile = File(fileName)
        if (!logFile.exists()) {
            GB.toast("File does not exist", Toast.LENGTH_LONG, GB.INFO)
            return
        }

        val providerUri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().applicationContext.packageName + ".screenshot_provider",
            logFile
        )

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        emailIntent.setType("text/plain")
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Gadgetbridge log file")
        emailIntent.putExtra(Intent.EXTRA_STREAM, providerUri)
        startActivity(Intent.createChooser(emailIntent, "Share File"))
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(MainDebugFragment::class.java)

        private const val PREF_DEBUG_ADD_TEST_DEVICE = "pref_debug_add_test_device"
        private const val PREF_HEADER_LOGS = "pref_header_logs"
        private const val LOG_TO_FILE = "log_to_file"
        private const val PREF_DEBUG_SHARE_LOGS = "pref_debug_share_logs"
        private const val PREF_HEADER_DEBUG = "pref_header_debug"
        private const val PREF_DEBUG_NOTIFICATIONS = "pref_debug_notifications"
        private const val PREF_DEBUG_CALLS = "pref_debug_calls"
        private const val PREF_DEBUG_MUSIC = "pref_debug_music"
        private const val PREF_DEBUG_WEATHER = "pref_debug_weather"
        private const val PREF_DEBUG_DATABASE = "pref_debug_database"
        private const val PREF_DEBUG_PREFERENCES = "pref_debug_preferences"
        private const val PREF_DEBUG_COMPANION_DEVICES = "pref_debug_companion_devices"
        private const val PREF_DEBUG_WIDGETS = "pref_debug_widgets"
        private const val PREF_DEBUG_LOCATION = "pref_debug_location"
        private const val PREF_DEBUG_ACTIVITY_LEGACY = "pref_debug_activity_legacy"
        private const val PREF_HEADER_ACTIVITIES = "pref_header_activities"
        private const val PREF_DEBUG_ACTIVITY_CAMERA = "pref_debug_activity_camera"
        private const val PREF_DEBUG_ACTIVITY_WELCOME = "pref_debug_activity_welcome"
        private const val PREF_DEBUG_ACTIVITY_PERMISSIONS = "pref_debug_activity_permissions"
    }
}
