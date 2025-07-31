/*  Copyright (C) 2018-2024 Carsten Pfeiffer, Felix Konstantin Maurer,
    Ganblejs, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.database

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object PeriodicExporter {
    private val LOG: Logger = LoggerFactory.getLogger(PeriodicExporter::class.java)

    private const val TAG = "exporter_db"

    @JvmStatic
    fun enablePeriodicExport(context: Context) {
        val prefs: Prefs = GBApplication.getPrefs()
        val autoExportScheduled = GBApplication.app().autoExportScheduledTimestamp
        val autoExportEnabled = prefs.getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false)
        val autoExportInterval = prefs.getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0)
        scheduleAlarm(context, autoExportInterval, autoExportEnabled && autoExportScheduled == 0L)
    }

    @JvmStatic
    fun scheduleAlarm(
        context: Context,
        autoExportInterval: Int,
        autoExportEnabled: Boolean
    ) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG)

        if (!autoExportEnabled) {
            LOG.info("Not scheduling periodic export, either already scheduled or not enabled")
            return
        }

        if (autoExportInterval == 0) {
            LOG.info("Not scheduling periodic export, interval set to 0")
            return
        }

        LOG.info("Scheduling periodic export for {}h in the future", autoExportInterval)

        val exportPeriodMillis = autoExportInterval * 60 * 60 * 1000
        GBApplication.app().autoExportScheduledTimestamp = System.currentTimeMillis() + exportPeriodMillis

        val exportRequest = PeriodicWorkRequestBuilder<DatabaseExportWorker>(
            autoExportInterval.toLong(),
            TimeUnit.HOURS
        ).addTag(TAG).build()

        workManager.enqueue(exportRequest)
    }

    @JvmStatic
    fun trigger() {
        val workManager = WorkManager.getInstance(GBApplication.getContext())
        val exportRequest = OneTimeWorkRequest.Builder(DatabaseExportWorker::class.java)
                .addTag(TAG)
                .build()
        workManager.enqueue(exportRequest)
    }
}
