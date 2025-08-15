package nodomain.freeyourgadget.gadgetbridge.util.backup

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ZipExportWorker(
    private val mContext: Context,
    workerParams: WorkerParameters
) : Worker(mContext, workerParams), ZipBackupCallback {
    private var success = false

    override fun doWork(): Result {
        val enabled = GBApplication.getPrefs().getBoolean(GBPrefs.AUTO_EXPORT_ZIP_ENABLED, false)
        if (!enabled) {
            LOG.warn("Zip export started, but is disabled")
            // Should not need i18n, this should never happen
            onFailure("Zip export started, but is disabled");
            return Result.failure()
        }

        val dst = GBApplication.getPrefs().getString(GBPrefs.AUTO_EXPORT_ZIP_LOCATION, null)

        LOG.info("Starting zip export, dst={}", dst)

        if (dst == null) {
            LOG.warn("Unable to export zip, export location not set")
            onFailure(mContext.getString(R.string.notif_export_location_not_set));
            broadcastSuccess(false)
            return Result.failure()
        }

        setForegroundAsync(createForegroundInfo())

        ZipBackupExportJob(mContext, this, dst.toUri()).run()

        LOG.info("Zip export completed, success={}", success)

        broadcastSuccess(success)

        return if (success) Result.success() else Result.failure()
    }

    override fun onProgress(progress: Int, message: String?) {
        setProgressAsync(Data.Builder().putInt("progress", progress).build())

        val updatedNotification: Notification =
            NotificationCompat.Builder(mContext, GB.NOTIFICATION_CHANNEL_ID_EXPORT)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(mContext.getString(R.string.backup_restore_exporting))
                .setContentText(message)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        notify(NOTIF_ID_PROGRESS, updatedNotification)
    }

    override fun onSuccess(warnings: String?) {
        success = true
        GBApplication.getPrefs().preferences.edit {
            putLong(GBPrefs.AUTO_EXPORT_ZIP_LAST_EXECUTION, System.currentTimeMillis())
        }
        if (warnings != null) {
            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(mContext, GB.NOTIFICATION_CHANNEL_ID_EXPORT)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(mContext.getString(R.string.backup_restore_export_complete))
                    .setContentText(warnings)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            notify(NOTIF_ID_WARNING, builder.build())
        }
    }

    override fun onFailure(errorMessage: String?) {
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(mContext, GB.NOTIFICATION_CHANNEL_ID_EXPORT)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(mContext.getString(R.string.notif_export_zip_failed_title))
                .setContentText(errorMessage ?: mContext.getString(R.string.unknown_error))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        notify(NOTIF_ID_FAILURE, builder.build())
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification: Notification =
            NotificationCompat.Builder(mContext, GB.NOTIFICATION_CHANNEL_ID_EXPORT)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(mContext.getString(R.string.backup_restore_exporting))
                .setProgress(100, 0, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIF_ID_PROGRESS, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_ID_PROGRESS, notification)
        }
    }

    private fun broadcastSuccess(success: Boolean) {
        if (!GBApplication.getPrefs().getBoolean(GBPrefs.INTENT_API_BROADCAST_EXPORT_ZIP, false)) {
            return
        }

        LOG.info("Broadcasting zip export success={}", success)

        val action: String = if (success) ACTION_ZIP_EXPORT_SUCCESS else ACTION_ZIP_EXPORT_FAIL
        val exportedNotifyIntent = Intent(action)
        mContext.sendBroadcast(exportedNotifyIntent)
    }

    private fun notify(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(mContext).notify(id, notification)
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(ZipExportWorker::class.java)

        private const val NOTIF_ID_PROGRESS: Int = 1001
        private const val NOTIF_ID_FAILURE: Int = 1002
        private const val NOTIF_ID_WARNING: Int = 1003

        const val ACTION_ZIP_EXPORT_SUCCESS: String =
            "nodomain.freeyourgadget.gadgetbridge.action.ZIP_EXPORT_SUCCESS"
        const val ACTION_ZIP_EXPORT_FAIL: String =
            "nodomain.freeyourgadget.gadgetbridge.action.ZIP_EXPORT_FAIL"
    }
}
