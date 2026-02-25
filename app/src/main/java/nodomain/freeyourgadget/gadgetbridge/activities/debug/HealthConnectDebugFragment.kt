package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.preferences.HealthConnectPreferencesActivity.HealthConnectPreferencesFragment
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.entities.HealthConnectSyncStateDao
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.GadgetbridgeDataExporter
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectSyncWorker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class HealthConnectDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setupPreferences()
    }

    private fun setupPreferences() {
        setPreferencesFromResource(R.xml.debug_preferences_health_connect, null)

        onClick(PREF_DEBUG_MANUAL_SYNC_TRIGGER) {
            val syncRequest = OneTimeWorkRequest.Builder(HealthConnectSyncWorker::class.java)
                .addTag(HealthConnectPreferencesFragment.HEALTH_CONNECT_SYNC_WORKER_TAG)
                .build()
            WorkManager.getInstance(requireContext()).enqueueUniqueWork(
                "HealthConnectSyncWorker_Debug",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        }

        onClick(PREF_DEBUG_RESET_HC_SYNC_STATE) {
            MaterialAlertDialogBuilder(requireActivity())
                .setCancelable(true)
                .setTitle("Reset HC sync state")
                .setMessage("Reset Health Connect sync state? This will allow all data to be re-synced.")
                .setPositiveButton(R.string.ok) { _, _ ->
                    try {
                        GBApplication.acquireDB().use { db ->
                            db.daoSession.healthConnectSyncStateDao.deleteAll()
                            GB.toast("Health Connect sync state reset successfully", Toast.LENGTH_SHORT, GB.INFO)
                        }
                    } catch (e: Exception) {
                        GB.toast("Failed to reset Health Connect sync state", Toast.LENGTH_LONG, GB.ERROR, e)
                    }
                    reloadSyncStates()
                }
                .setNegativeButton(R.string.Cancel) { _, _ -> }
                .show()
        }

        onClick(PREF_DEBUG_EXPORT_GB_DATA) {
            runOnDebugDevices("Export data for device", forceDialog = true) { gbDevice ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                DatePickerDialog(
                    requireActivity(),
                    { _: DatePicker?, year: Int, month: Int, day: Int ->
                        val startDate = LocalDate.of(year, month + 1, day)
                        val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                        val endInstant = Instant.now()
                        try {
                            val file = GadgetbridgeDataExporter.export(requireContext(), gbDevice, startInstant, endInstant)
                            GB.toast("Exported to ${file.name}", Toast.LENGTH_LONG, GB.INFO)
                        } catch (e: Exception) {
                            GB.toast("Export failed: ${e.message}", Toast.LENGTH_LONG, GB.ERROR, e)
                        }
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        reloadSyncStates()
    }

    private fun reloadSyncStates() {
        removeDynamicPrefs(preferenceScreen)

        GBApplication.app().deviceManager.devices
            .sortedBy { it.aliasOrName }
            .forEach { gbDevice ->
                val syncStates = GBApplication.acquireDB().use { db ->
                    val deviceFromDb = DBHelper.getDevice(gbDevice, db.daoSession)
                    return@use db.daoSession.healthConnectSyncStateDao.queryBuilder()
                        .where(HealthConnectSyncStateDao.Properties.DeviceId.eq(deviceFromDb.id))
                        .orderAsc(HealthConnectSyncStateDao.Properties.DataType)
                        .list()
                }

                if (syncStates.isEmpty()) {
                    return@forEach
                }

                addDynamicCategory(gbDevice.aliasOrName)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                for (state in syncStates) {
                    val zonedDateTime = Instant.ofEpochMilli(state.lastSyncTimestamp * 1000L)
                        .atZone(ZoneId.systemDefault())

                    addDynamicPref(
                        preferenceScreen,
                        state.dataType,
                        zonedDateTime.format(formatter)
                    ) {
                        DatePickerDialog(
                            requireActivity(),
                            { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                                val date = DateTimeUtils.dayStart(year, monthOfYear, dayOfMonth)

                                GBApplication.acquireDB().use { db ->
                                    val syncStateDao = db.daoSession.healthConnectSyncStateDao
                                    state.lastSyncTimestamp = date.time / 1000

                                    LOG.info(
                                        "Updating Health Connect sync state for device {}, data type {} to timestamp: {}",
                                        gbDevice.aliasOrName,
                                        state.dataType,
                                        date
                                    )
                                    syncStateDao.insertOrReplace(state)
                                }

                                reloadSyncStates()
                            },
                            zonedDateTime.year,
                            zonedDateTime.monthValue - 1,
                            zonedDateTime.dayOfMonth
                        ).show()
                    }
                }
            }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(HealthConnectDebugFragment::class.java)
        private const val PREF_DEBUG_MANUAL_SYNC_TRIGGER = "pref_debug_manual_sync_trigger"
        private const val PREF_DEBUG_RESET_HC_SYNC_STATE = "pref_debug_reset_hc_sync_state"
        private const val PREF_DEBUG_EXPORT_GB_DATA = "pref_debug_export_gb_data"
    }
}
