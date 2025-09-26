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
package nodomain.freeyourgadget.gadgetbridge.util.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Random
import kotlin.math.roundToInt

/**
 * A helper class to keep a transfer notification updated, with utility functions to handle throttling and progress.
 * <p>
 * It is assumed that a chunk is in the same unit as the total progress, and not included in it until the chunk finishes.
 * At any point, the progress percentage of the notification corresponds to (totalProgress + chunkProgress) / totalSize.
 */
class GBProgressNotification(
    private val context: Context,
    private val channelId: String
) {
    private val notificationId: Int = Random().nextInt(100000)

    @StringRes
    private var titleRes = 0

    @StringRes
    private var textRes = 0

    private var visible = false

    private var lastNotificationUpdateTs: Long = -1

    private var totalProgress: Long = 0
    private var totalSize: Long = 0
    private var chunkProgress: Long = 0

    /**
     * Starts the transfer notification, resetting all progress and size to zero.
     */
    fun start(
        @StringRes titleRes: Int,
        @StringRes textRes: Int,
        totalSize: Long = 0
    ) {
        this.titleRes = titleRes
        this.textRes = textRes
        this.chunkProgress = 0
        this.totalProgress = 0
        this.totalSize = totalSize

        LOG.debug(
            "Started notification id={}, title={}",
            notificationId,
            if (titleRes != 0) context.getString(titleRes) else "0"
        )

        refresh(true)
    }

    fun setChunkProgress(chunkProgress: Long) {
        LOG.debug("setChunkProgress id={}: {}", notificationId, chunkProgress)
        this.chunkProgress = chunkProgress
        refresh(false)
    }

    fun setTotalProgress(totalProgress: Long) {
        LOG.debug("setTotalProgress id={}: {}", notificationId, totalProgress)
        this.chunkProgress = 0
        this.totalProgress = totalProgress
        refresh(false)
    }

    fun setTotalSize(totalSize: Long) {
        LOG.debug("setTotalSize id={}: {}", notificationId, totalSize)
        this.totalSize = totalSize
        refresh(false)
    }

    fun incrementTotalProgress(inc: Long) {
        LOG.debug("incrementTotalProgress id={}: {}", notificationId, inc)
        this.chunkProgress = 0
        this.totalProgress += inc
        refresh(false)
    }

    fun incrementTotalSize(inc: Long) {
        LOG.debug("incrementTotalSize id={}: {}", notificationId, inc)
        this.totalSize += inc
        refresh(false)
    }

    fun setProgress(chunkProgress: Long, totalProgress: Long) {
        LOG.debug("setProgress id={}: {} {}", notificationId, chunkProgress, totalProgress)
        this.chunkProgress = chunkProgress
        this.totalProgress = totalProgress
        refresh(false)
    }

    fun getProgressPercentage(): Int {
        if (totalSize == 0L) {
            return 0
        }

        return (((totalProgress + chunkProgress) * 100f) / totalSize).roundToInt()
    }

    fun finish() {
        this.visible = false
        this.chunkProgress = 0
        this.totalProgress = 0
        this.totalSize = 0

        LOG.debug("Finishing notification id={}", notificationId)

        GB.removeNotification(notificationId, context)
    }

    private fun refresh(force: Boolean) {
        val percentage = getProgressPercentage()

        if (visible) {
            val now = System.currentTimeMillis()
            if (now - lastNotificationUpdateTs < MIN_TIME_BETWEEN_UPDATES && !force) {
                LOG.trace("Throttling notification update")
                return
            }

            lastNotificationUpdateTs = now
        }

        LOG.debug(
            "Updating notification, progress=({}+{})/{}, percentage={}",
            totalProgress,
            chunkProgress,
            totalSize,
            percentage
        )

        visible = true

        val title: CharSequence = if (titleRes != 0) context.getString(titleRes) else "Unknown transfer"
        val text = if (textRes != 0) {
            context.getString(textRes)
        } else if (totalSize != 0L) {
            context.getString(
                R.string.work_info_running_percentage,
                context.getString(R.string.busy_task_progress_int, totalProgress + chunkProgress, totalSize),
                percentage
            )
        } else {
            ""
        }

        update(
            title = title,
            text = text,
            ongoing = true,
            percentage = percentage
        )
    }

    private fun update(
        title: CharSequence,
        text: CharSequence,
        ongoing: Boolean,
        percentage: Int
    ) {
        if (percentage >= 100) {
            GB.removeNotification(notificationId, context)
        } else {
            val notification: Notification = createProgressNotification(
                title, text, ongoing, percentage, channelId, context
            )
            GB.notify(notificationId, notification, context)
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GBProgressNotification::class.java)

        const val MIN_TIME_BETWEEN_UPDATES = 1000L

        private fun createProgressNotification(
            title: CharSequence?,
            text: CharSequence?,
            ongoing: Boolean,
            percentage: Int,
            channelId: String,
            context: Context
        ): Notification {
            val notificationIntent = Intent(context, ControlCenterv2::class.java)
            notificationIntent.setPackage(BuildConfig.APPLICATION_ID)
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val nb = NotificationCompat.Builder(context, channelId)
                .setTicker(title ?: context.getString(R.string.app_name))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(title ?: context.getString(R.string.app_name))
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(percentage > 0 && percentage < 100)
                .setOngoing(ongoing)

            if (ongoing) {
                nb.setProgress(100, percentage, percentage == 0)
                nb.setSmallIcon(android.R.drawable.stat_sys_download)
            } else {
                nb.setProgress(0, 0, false)
                nb.setSmallIcon(android.R.drawable.stat_sys_download_done)
            }

            return nb.build()
        }
    }
}
