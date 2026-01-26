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

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import org.slf4j.Logger
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

internal abstract class AbstractActivitySampleSyncer<TRecord : Record> : ActivitySampleSyncer {
    protected abstract val logger: Logger
    protected abstract val recordClass: KClass<TRecord>

    protected abstract fun convertSample(
        sample: ActivitySample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): TRecord?

    override suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneOffset,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        grantedPermissions: Set<String>,
        deviceSamples: List<ActivitySample>
    ): SyncerStatistics {
        val deviceName = gbDevice.aliasOrName
        val recordTypeName = recordClass.simpleName ?: "Unknown"

        // 1. Permission Check
        if (HealthPermission.getWritePermission(recordClass) !in grantedPermissions) {
            logger.info("Skipping $recordTypeName sync for device '$deviceName'; $recordTypeName permission not granted.")
            return SyncerStatistics(recordType = recordTypeName)
        }

        // 2. Relevant Input Data Check
        val relevantSamples = deviceSamples.sortedBy { it.timestamp }
        if (relevantSamples.isEmpty()) {
            logger.info("No relevant step samples (>0) for device '$deviceName' in the provided deviceSamples for slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = recordTypeName)
        }

        logger.info("Processing ${relevantSamples.size} samples for steps for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")

        val recordsToInsert = mutableListOf<Record>()
        var skippedCount = 0
        relevantSamples.forEach { currentSample ->
            val endTs = Instant.ofEpochSecond(currentSample.timestamp.toLong())
            val startTs = endTs.minus(1, ChronoUnit.MINUTES)

            // Use inclusive boundaries [sliceStart, sliceEnd] for the slice
            // Overlap check: interval overlaps if endTs > sliceStart AND startTs <= sliceEnd
            if (endTs.isBefore(sliceStartBoundary) || startTs.isAfter(sliceEndBoundary)) {
                logger.debug(
                    "Skipping {} for device '{}' for sample at {} (interval {} to {}) as its interval is outside the slice {} - {}.",
                    recordTypeName,
                    deviceName,
                    endTs,
                    startTs,
                    endTs,
                    sliceStartBoundary,
                    sliceEndBoundary
                )
                return@forEach
            }

            val record = convertSample(sample = currentSample, offset, metadata, deviceName)
            if (record == null) {
                skippedCount++
                return@forEach
            }
            recordsToInsert.add(record)
        }

        // 3. No Valid Records to Insert
        if (recordsToInsert.isEmpty()) {
            logger.info("No valid $recordTypeName created for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary after processing ${relevantSamples.size} samples.")
            return SyncerStatistics(recordsSkipped = skippedCount, recordType = recordTypeName)
        }

        // 4. Insertion (with chunking)
        logger.info("Attempting to insert ${recordsToInsert.size} $recordTypeName(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        for (chunk in recordsToInsert.chunked(HealthConnectUtils.CHUNK_SIZE)) {
            HealthConnectUtils.insertRecords(chunk, healthConnectClient)
        }

        logger.info("Successfully inserted ${recordsToInsert.size} $recordTypeName(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(
            recordsSynced = recordsToInsert.size,
            recordsSkipped = skippedCount,
            recordType = recordTypeName
        )
    }
}
