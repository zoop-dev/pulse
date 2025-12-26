/*  Copyright (C) 2025 Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.activities.preferences

import androidx.health.connect.client.HealthConnectClient
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(HealthConnectPreferencesActivity::class.java)

/**
 * Helper suspend function to get granted Health Connect permissions.
 *
 * @param client The HealthConnectClient instance.
 * @return A Set of permission strings if successful, or null if an error occurs.
 */
suspend fun getGrantedHealthConnectPermissions(client: HealthConnectClient): Set<String>? {
    return try {
        client.permissionController.getGrantedPermissions()
    } catch (e: Exception) {
        LOG.error("Error getting Health Connect permissions in Kotlin helper", e)
        null // Return null to indicate an error
    }
}
