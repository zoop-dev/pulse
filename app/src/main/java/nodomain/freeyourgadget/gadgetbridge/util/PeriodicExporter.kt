package nodomain.freeyourgadget.gadgetbridge.util

import android.content.Context
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit


abstract class PeriodicExporter {
    abstract fun getWorkerClass(): Class<out ListenableWorker>
    abstract fun getKeyPrefix(): String
    abstract fun getFileMimeType(): String
    abstract fun getFileExtension(): String

    fun scheduleNextExecution(context: Context) {
        try {
            val keyPrefix = getKeyPrefix()
            val prefKeyEnabled = keyPrefix + GBPrefs.AUTO_EXPORT_ENABLED
            val prefKeyInterval = keyPrefix + GBPrefs.AUTO_EXPORT_INTERVAL
            val prefKeyNextExecution = keyPrefix + GBPrefs.AUTO_EXPORT_NEXT_EXECUTION
            val prefKeyStartTime = keyPrefix + "auto_export_start_time"

            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(getWorkTag())

            val prefs: Prefs = GBApplication.getPrefs()

            val autoExportEnabled = prefs.getBoolean(prefKeyEnabled, false)
            if (!autoExportEnabled) {
                LOG.info("Not scheduling {} - not enabled", getWorkerClass().simpleName)
                return
            }

            val autoExportInterval = prefs.getInt(prefKeyInterval, 0)
            if (autoExportInterval == 0) {
                LOG.info("Not scheduling {}, interval set to 0", getWorkerClass().simpleName)
                return
            }
            val exportPeriodMillis = autoExportInterval * 60 * 60 * 1000L

            val startTime = prefs.getLocalTime(prefKeyStartTime, "00:00")
            val nextExecution = nextExecution(startTime, autoExportInterval.toLong())
            val initialDelayMillis = nextExecution.toInstant().toEpochMilli() - System.currentTimeMillis()
            LOG.info(
                "Scheduling {} for {}h in the future from {} ({}ms)",
                getWorkerClass().simpleName,
                autoExportInterval,
                startTime,
                initialDelayMillis
            )

            prefs.preferences.edit {
                putLong(prefKeyNextExecution, System.currentTimeMillis() + exportPeriodMillis)
            }

            val exportRequest = PeriodicWorkRequest.Builder(
                getWorkerClass(),
                autoExportInterval.toLong(),
                TimeUnit.HOURS
            ).setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .addTag(getWorkTag())
                .addTag("$TAG_CREATED_AT${System.currentTimeMillis()}")
                .build()

            workManager.enqueueUniquePeriodicWork(
                getWorkTag(),
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                exportRequest
            )
        } catch (e: Exception) {
            LOG.error("Failed to schedule next execution for {}", getWorkerClass().simpleName, e)
        }
    }

    private fun nextExecution(anchor: LocalTime, periodHours: Long): ZonedDateTime {
        val now = ZonedDateTime.now(ZoneId.systemDefault())

        val candidate = now.with(anchor)

        // If today's anchor is after now, it's the first execution
        if (candidate.isAfter(now)) {
            return candidate
        }

        // Otherwise, find how many periods have passed since anchor
        val elapsedMillis = ChronoUnit.MILLIS.between(candidate, now)
        val periodMillis = Duration.ofHours(periodHours).toMillis()

        val periodsPassed = (elapsedMillis / periodMillis) + 1  // +1 = next occurrence
        return candidate.plus(periodsPassed * periodHours, ChronoUnit.HOURS)
    }

    fun executeNow() {
        val workManager = WorkManager.getInstance(GBApplication.getContext())
        val exportRequest = OneTimeWorkRequest.Builder(getWorkerClass())
            .addTag(getWorkTag())
            .addTag("$TAG_CREATED_AT${System.currentTimeMillis()}")
            .build()
        workManager.enqueue(exportRequest)
    }

    fun getWorkTag(): String {
        return "${getKeyPrefix()}exporter_worker"
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(PeriodicExporter::class.java)

        const val TAG_CREATED_AT = "createdAt-"
    }
}
