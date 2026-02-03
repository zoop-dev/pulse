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
import androidx.work.Worker
import androidx.work.WorkerParameters
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EndurainTokenRefreshWorker (
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val LOG: Logger = LoggerFactory.getLogger(EndurainTokenRefreshWorker::class.java)

    override fun doWork(): Result {
        LOG.info("Running Endurain token refresh worker")

        // Refresh tokens if possible
        val tokenManager = EndurainTokenManager(applicationContext)
        val serverUrl = GBApplication.getPrefs().preferences.getString("endurain_server", null)
        if (serverUrl != null && tokenManager.isLoggedIn()) {
            tokenManager.performTokenRefresh(serverUrl) {}
        }

        return Result.success()
    }
}