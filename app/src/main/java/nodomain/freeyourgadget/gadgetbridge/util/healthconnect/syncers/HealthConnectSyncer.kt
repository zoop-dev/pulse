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
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import java.time.Instant
import java.time.ZoneOffset

/**
 * Statistics returned by a syncer after processing a slice.
 */
data class SyncerStatistics(
    val recordsSynced: Int = 0,
    val recordsSkipped: Int = 0,
    val recordType: String = ""
)

/**
 * Base interface for synchronizing specific data types from Gadgetbridge to Health Connect.
 * This interface defines the common parameters that all syncers need.
 */
internal sealed interface HealthConnectSyncer {
    /**
     * Common parameters for all syncers.
     */
    suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneOffset,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        grantedPermissions: Set<String>
    ): SyncerStatistics
}

/**
 * Interface for syncers that require pre-fetched ActivitySample data.
 * Used by syncers that process activity-based data like steps and heart rate.
 */
internal interface ActivitySampleSyncer {
    suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneOffset,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        grantedPermissions: Set<String>,
        deviceSamples: List<ActivitySample>
    ): SyncerStatistics
}

/**
 * Interface for syncers that require both ActivitySample data and Android Context.
 * Used by syncers that need localized strings or other context-dependent resources.
 */
internal interface ContextualActivitySampleSyncer {
    suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneOffset,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        grantedPermissions: Set<String>,
        deviceSamples: List<ActivitySample>,
        context: Context
    ): SyncerStatistics
}