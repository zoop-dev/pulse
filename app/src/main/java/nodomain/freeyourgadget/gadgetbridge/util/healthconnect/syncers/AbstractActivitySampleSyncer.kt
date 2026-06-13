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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

internal abstract class AbstractActivitySampleSyncer<TRecord : Record> : ActivitySampleSyncer {
    protected abstract val logger: Logger
    protected abstract val recordClass: KClass<TRecord>

    // Re-emit samples this far before the slice start. The ACTIVITY cursor is shared across
    // steps/calories/distance and advances to the furthest delivered; when step or distance detail
    // trails the calorie summary, those minutes would be clipped and lost. The per-minute
    // clientRecordId makes the re-emitted overlap an upsert. Heart rate is a series keyed on its
    // start time, does not extend this base, and keeps strict boundaries.
    protected open val lateSampleLookback: Duration = Duration.ofHours(1)

    internal fun isWithinSlice(
        endTs: Instant,
        startTs: Instant,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant
    ): Boolean {
        val effectiveStart = sliceStartBoundary.minus(lateSampleLookback)
        return !endTs.isBefore(effectiveStart) && !startTs.isAfter(sliceEndBoundary)
    }

    internal abstract fun convertSample(
        sample: ActivitySample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String,
        version: Long
    ): TRecord?

    override suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneId,
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

        // clientRecordVersion: every record in this slice shares the run's wall-clock so a later
        // sync always outranks the value it previously wrote for a minute. HC keeps the highest
        // version on a clientRecordId collision, so the freshest (most complete) re-read wins even
        // when a value is revised downward. Must not be the metric value itself: a downward
        // correction would then carry a lower version and be silently ignored.
        val recordVersion = System.currentTimeMillis()

        val recordsToInsert = mutableListOf<Record>()
        var skippedCount = 0
        var latestSyncedTimestamp: Instant? = null
        relevantSamples.forEach { currentSample ->
            val endTs = Instant.ofEpochSecond(currentSample.timestamp.toLong())
            val startTs = endTs.minus(1, ChronoUnit.MINUTES)

            if (!isWithinSlice(endTs, startTs, sliceStartBoundary, sliceEndBoundary)) {
                logger.trace(
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

            val record = convertSample(sample = currentSample, offset.rules.getOffset(endTs), metadata, deviceName, recordVersion)
            if (record == null) {
                skippedCount++
                return@forEach
            }
            recordsToInsert.add(record)
            if (latestSyncedTimestamp == null || endTs.isAfter(latestSyncedTimestamp)) {
                latestSyncedTimestamp = endTs
            }
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
            recordType = recordTypeName,
            latestRecordTimestamp = latestSyncedTimestamp
        )
    }
}
