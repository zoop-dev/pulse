package nodomain.freeyourgadget.gadgetbridge.database

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import androidx.core.net.toUri

class DatabaseExportWorker(
    private val mContext: Context,
    workerParams: WorkerParameters
) : Worker(mContext, workerParams) {
    override fun doWork(): Result {
        val dst = GBApplication.getPrefs().getString(GBPrefs.AUTO_EXPORT_LOCATION, null)

        LOG.info("Starting DB export, dst={}", dst)

        if (dst == null) {
            LOG.warn("Unable to export DB, export location not set")
            broadcastSuccess(false)
            return Result.failure()
        }

        try {
            GBApplication.acquireDB().use { dbHandler ->
                val helper = DBHelper(mContext)
                mContext.contentResolver.openOutputStream(dst.toUri()).use { out ->
                    helper.exportDB(dbHandler, out)
                }
            }

            GBApplication.app().lastAutoExportTimestamp = System.currentTimeMillis()
        } catch (e: Exception) {
            GB.updateExportFailedNotification(mContext.getString(R.string.notif_export_failed_title), mContext)
            LOG.error("Exception while exporting DB", e)
            broadcastSuccess(false)
            return Result.failure()
        }

        LOG.info("DB export completed")

        broadcastSuccess(true)

        return Result.success()
    }

    private fun broadcastSuccess(success: Boolean) {
        if (!GBApplication.getPrefs().getBoolean("intent_api_broadcast_export", false)) {
            return
        }

        LOG.info("Broadcasting database export success={}", success)

        val action: String = if (success) ACTION_DATABASE_EXPORT_SUCCESS else ACTION_DATABASE_EXPORT_FAIL
        val exportedNotifyIntent = Intent(action)
        mContext.sendBroadcast(exportedNotifyIntent)
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DatabaseExportWorker::class.java)

        const val ACTION_DATABASE_EXPORT_SUCCESS: String =
            "nodomain.freeyourgadget.gadgetbridge.action.DATABASE_EXPORT_SUCCESS"
        const val ACTION_DATABASE_EXPORT_FAIL: String =
            "nodomain.freeyourgadget.gadgetbridge.action.DATABASE_EXPORT_FAIL"
    }
}
