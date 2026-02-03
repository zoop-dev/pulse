/*  Copyright (C) 2026 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.endurain

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object PeriodicEndurainTokenRefresher {
    private val LOG: Logger = LoggerFactory.getLogger(PeriodicEndurainTokenRefresher::class.java)

    const val TAG_CREATED_AT = "createdAt-"
    const val WORK_TAG = "EndurainTokenRefreshWorker"

    fun scheduleNextExecution(context: Context) {
        try {
            val tokenManager = EndurainTokenManager(context)
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(WORK_TAG)

            if (!tokenManager.isLoggedIn()) {
                LOG.info("Not scheduling {}, no valid refresh token available", this::class.java.simpleName)
                return
            }

            // Runs every 3 days
            val periodicWork = PeriodicWorkRequestBuilder<EndurainTokenRefreshWorker>(
                3, TimeUnit.DAYS
            )
                .addTag(WORK_TAG)
                .addTag("$TAG_CREATED_AT${System.currentTimeMillis()}")
                .build()

            WorkManager.getInstance(context).apply {
                enqueueUniquePeriodicWork(
                    WORK_TAG,
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                    periodicWork
                )
            }
        } catch (e: Exception) {
            LOG.error("Failed to schedule next execution for {}", this::class.java.simpleName, e)
        }
    }
}