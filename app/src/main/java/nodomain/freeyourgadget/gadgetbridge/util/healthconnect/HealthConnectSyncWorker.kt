/*  Copyright (C) 2025-2026 Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import org.slf4j.LoggerFactory
import java.util.function.BiConsumer

class HealthConnectSyncWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        private val LOG = LoggerFactory.getLogger(HealthConnectSyncWorker::class.java)
        private const val NOTIFICATION_ID = 123
        const val INPUT_DEVICE_ADDRESS = "device_address"
    }

    override suspend fun doWork(): Result {
        LOG.info("Health Connect sync worker started")

        return try {
            performSync()
        } catch (e: Exception) {
            LOG.error("Health Connect sync worker failed", e)
            Result.failure()
        }
    }

    private suspend fun performSync(): Result {
        val prefs = GBApplication.getPrefs()

        if (!prefs.getBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false)) {
            LOG.info("Health Connect is disabled, aborting sync.")
            return Result.failure()
        }

        setForeground(createForegroundInfo())
        setProgress(
            Data.Builder().putString("progress", applicationContext.getString(R.string.health_connect_syncing)).build()
        )

        val healthConnectClient = HealthConnectClientProvider.healthConnectInit(applicationContext)
        if (healthConnectClient == null) {
            LOG.error("SyncWorker: HealthConnectClient is null, cannot perform HC sync")
            return Result.success()
        }

        performHealthConnectSync(healthConnectClient)
        LOG.info("Health Connect sync worker finished successfully.")
        return Result.success()
    }

    private suspend fun performHealthConnectSync(healthConnectClient: HealthConnectClient) {
        LOG.info("SyncWorker: Starting HC data sync")

        // Extract device address from input data, if provided
        val deviceAddress = inputData.getString(INPUT_DEVICE_ADDRESS)
        if (!deviceAddress.isNullOrEmpty()) {
            LOG.info("SyncWorker: Syncing specific device: {}", deviceAddress)
        } else {
            LOG.info("SyncWorker: Syncing all selected devices")
        }

        val summaryCallback = BiConsumer<String, Boolean> { summary, inProgress ->
            if (!inProgress) {
                val saved = GBApplication.getPrefs().preferences.edit()
                    .putString(GBPrefs.HEALTH_CONNECT_SYNC_STATUS, summary)
                    .commit()
                if (!saved) {
                    LOG.warn("Failed to save final sync status to SharedPreferences")
                }
            }
            setProgressAsync(Data.Builder().putString("progress", summary).build())
        }

        HealthConnectUtils().healthConnectDataSync(
            applicationContext,
            healthConnectClient,
            summaryCallback,
            this,
            deviceAddress  // Pass the device address (null if not provided)
        )
        LOG.info("SyncWorker: HC data sync completed")
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val context = applicationContext
        val channelId = GB.NOTIFICATION_CHANNEL_ID_HEALTH_CONNECT_SYNC
        val title = context.getString(R.string.health_connect_sync_notification_title)
        val message = context.getString(R.string.health_connect_sync_notification_message)

        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
