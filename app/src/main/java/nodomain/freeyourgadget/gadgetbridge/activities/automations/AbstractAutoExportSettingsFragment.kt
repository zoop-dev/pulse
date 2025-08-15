/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.automations

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import nodomain.freeyourgadget.gadgetbridge.util.PeriodicExporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.TimeUnit

abstract class AbstractAutoExportSettingsFragment(
    val exporter: PeriodicExporter,
) : AbstractPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.auto_export_settings, rootKey)

        val keyPrefix = exporter.getKeyPrefix()
        addPrefixToPreferenceKeys(preferenceScreen, keyPrefix)

        val prefKeyEnabled = keyPrefix + GBPrefs.AUTO_EXPORT_ENABLED
        val prefKeyLocation = keyPrefix + GBPrefs.AUTO_EXPORT_LOCATION
        val prefKeyInterval = keyPrefix + GBPrefs.AUTO_EXPORT_INTERVAL
        val prefKeyStartTime = keyPrefix + "auto_export_start_time"
        val prefKeyRunNow = keyPrefix + "auto_export_run_now"

        val gbPrefs = GBApplication.getPrefs()

        val exportLocationPicker = registerForActivityResult<Intent?, ActivityResult?>(
            StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? ->
                if (result!!.resultCode != Activity.RESULT_OK) {
                    return@ActivityResultCallback
                }
                val uri = result.data?.data
                if (uri == null) {
                    LOG.error("Got no uri")
                    return@ActivityResultCallback
                }
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                gbPrefs.preferences.edit {
                    putString(prefKeyLocation, uri.toString())
                }
                val summary = resolveLocationSummary(
                    requireContext(),
                    gbPrefs.getString(prefKeyLocation, "")
                )
                findPreference<Preference>(prefKeyLocation)?.setSummary(summary)
            }
        )

        setInputTypeFor(prefKeyInterval, InputType.TYPE_CLASS_NUMBER)

        val prefExportLocation = findPreference<Preference>(prefKeyLocation)
        if (prefExportLocation != null) {
            prefExportLocation.setOnPreferenceClickListener {
                val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                i.setType(exporter.getFileMimeType())
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.putExtra(Intent.EXTRA_TITLE, "Gadgetbridge.${exporter.getFileExtension()}")
                i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val title: String = requireContext().applicationContext.getString(R.string.choose_auto_export_location)
                exportLocationPicker.launch(Intent.createChooser(i, title))

                true
            }
            prefExportLocation.setSummary(
                resolveLocationSummary(
                    requireContext(),
                    gbPrefs.getString(prefKeyLocation, "")
                )
            )
        }

        val prefExportInterval = findPreference<Preference>(prefKeyInterval)
        if (prefExportInterval != null) {
            prefExportInterval.setOnPreferenceChangeListener { preference: Preference?, autoExportInterval: Any? ->
                val summary = String.format(
                    requireContext().applicationContext.getString(R.string.pref_summary_auto_export_interval),
                    (autoExportInterval as String?)!!.toInt()
                )
                prefExportInterval.setSummary(summary)
                scheduleNextExecutionDelayed()
                true
            }
            val autoExportInterval = gbPrefs.getInt(prefKeyInterval, 0)
            val summary = String.format(
                requireContext().applicationContext.getString(R.string.pref_summary_auto_export_interval),
                autoExportInterval
            )
            prefExportInterval.setSummary(summary)
        }

        val prefExportEnabled = findPreference<Preference>(prefKeyEnabled)
        prefExportEnabled?.setOnPreferenceChangeListener { preference: Preference?, autoExportEnabled: Any? ->
            scheduleNextExecutionDelayed()
            true
        }

        findPreference<Preference>(prefKeyRunNow)?.setOnPreferenceClickListener {
            exporter.executeNow()
            true
        }

        val prefStartTime = findPreference<Preference>(prefKeyStartTime)
        prefStartTime?.setOnPreferenceChangeListener { preference: Preference?, startFrom: Any? ->
            scheduleNextExecutionDelayed()
            true
        }
    }

    /**
     * Update the scheduled execution after a slight delay to allow for preferences to be persisted.
     */
    private fun scheduleNextExecutionDelayed() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(200)
            exporter.scheduleNextExecution(requireContext())
        }
    }

    override fun onStart() {
        super.onStart()

        val workManager = WorkManager.getInstance(requireContext())

        val gbPrefs = GBApplication.getPrefs()
        val prefKeyStatus = exporter.getKeyPrefix() + "auto_export_status"
        val prefKeyLastExecution = exporter.getKeyPrefix() + GBPrefs.AUTO_EXPORT_LAST_EXECUTION
        val prefKeyNextExecution = exporter.getKeyPrefix() + GBPrefs.AUTO_EXPORT_NEXT_EXECUTION
        val prefStatus = findPreference<Preference>(prefKeyStatus)
        val prefLastExecution = findPreference<Preference>(prefKeyLastExecution)
        val prefNextExecution = findPreference<Preference>(prefKeyNextExecution)
        workManager.getWorkInfosByTagLiveData(exporter.getWorkTag()).observe(viewLifecycleOwner) { workInfos ->
            LOG.debug("Got update for {} workInfos", workInfos.size)

            val sortedWorkInfos = workInfos.sortedBy { workInfo ->
                workInfo.tags
                    .find { it.startsWith(PeriodicExporter.TAG_CREATED_AT) }
                    ?.removePrefix(PeriodicExporter.TAG_CREATED_AT)
                    ?.toLongOrNull()
                    ?: 0L
            }

            val lastExecution = sortedWorkInfos.findLast { workInfo -> workInfo.state != WorkInfo.State.ENQUEUED }
            val nextExecution = sortedWorkInfos.findLast { workInfo -> workInfo.state == WorkInfo.State.ENQUEUED }

            if (lastExecution != null) {
                prefStatus?.summary = when (lastExecution.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = lastExecution.progress.getInt("progress", -1)
                        if (progress >= 0) {
                            requireContext().getString(
                                R.string.work_info_running_percentage,
                                getString(R.string.work_info_status_running),
                                progress
                            )
                        } else {
                            getString(R.string.work_info_status_running)
                        }
                    }

                    WorkInfo.State.ENQUEUED -> requireContext().getString(R.string.work_info_status_enqueued)
                    WorkInfo.State.SUCCEEDED -> requireContext().getString(R.string.work_info_status_succeeded)
                    WorkInfo.State.FAILED -> requireContext().getString(R.string.work_info_status_failed)
                    WorkInfo.State.BLOCKED -> requireContext().getString(R.string.work_info_status_blocked)
                    WorkInfo.State.CANCELLED -> requireContext().getString(R.string.work_info_status_cancelled)
                }
            } else {
                prefStatus?.summary = requireContext().getString(R.string.unknown)
            }

            // We need to persist and fetch the timestamp from preferences, since the WorkInfo will not contain it
            val lastAutoExportTimestamp: Long = gbPrefs.getLong(prefKeyLastExecution, 0)
            if (lastAutoExportTimestamp > 0) {
                prefLastExecution?.summary = formatDateWithDiff(Date(lastAutoExportTimestamp))
            } else {
                prefLastExecution?.summary = getString(R.string.unknown)
            }

            if (nextExecution != null) {
                prefNextExecution?.summary = formatDateWithDiff(Date(nextExecution.nextScheduleTimeMillis))
            }
        }
    }

    /**
     * Add a prefix to the key and dependency of all preferences in a group.
     */
    private fun addPrefixToPreferenceKeys(prefGroup: PreferenceGroup, prefix: String) {
        for (i in 0 until prefGroup.preferenceCount) {
            val pref = prefGroup.getPreference(i)

            if (pref is PreferenceGroup) {
                addPrefixToPreferenceKeys(pref, prefix)
            }

            if (!pref.key.isNullOrBlank() && !pref.key.startsWith(prefix)) {
                pref.key = prefix + pref.key
            }
            if (!pref.dependency.isNullOrBlank() && !pref.dependency?.startsWith(prefix)!!) {
                pref.dependency = prefix + pref.dependency
            }
        }
    }

    /**
     * Format a date, including the difference to the current time.
     */
    private fun formatDateWithDiff(date: Date): String {
        val diffMillis = System.currentTimeMillis() - date.time
        return if (diffMillis > 0) {
            requireContext().getString(
                R.string.datetime_in_the_past,
                DateTimeUtils.formatDateTime(date),
                DateTimeUtils.formatDurationHoursMinutes(diffMillis, TimeUnit.MILLISECONDS)
            )
        } else {
            requireContext().getString(
                R.string.datetime_in_the_future,
                DateTimeUtils.formatDateTime(date),
                DateTimeUtils.formatDurationHoursMinutes(-diffMillis, TimeUnit.MILLISECONDS)
            )
        }
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(AbstractAutoExportSettingsFragment::class.java)

        /**
         * Either returns the file path of the selected document, or the display name, or an error string
         */
        fun resolveLocationSummary(context: Context, uriString: String): String {
            if (uriString == "") {
                return ""
            }
            val uri = uriString.toUri()
            try {
                return AndroidUtils.getFilePath(context.applicationContext, uri)
            } catch (e: IllegalArgumentException) {
                LOG.warn("getAutoExportLocationSummary 1", e)
                try {
                    context.contentResolver.query(
                        uri,
                        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                        null, null, null, null
                    ).use { cursor ->
                        if (cursor != null && cursor.moveToFirst()) {
                            return cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                        }
                    }
                } catch (e2: Exception) {
                    LOG.warn("getAutoExportLocationSummary 2", e2)
                }
            }
            return context.getString(R.string.activity_db_management_autoexport_location)
        }
    }
}
