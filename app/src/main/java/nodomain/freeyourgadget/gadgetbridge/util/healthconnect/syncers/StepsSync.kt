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
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

private val LOG = LoggerFactory.getLogger("StepsSyncer")

internal object StepsSyncer : ActivitySampleSyncer {
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

        // 1. Permission Check
        if (HealthPermission.getWritePermission(StepsRecord::class) !in grantedPermissions) {
            LOG.info("Skipping Steps sync for device '$deviceName'; StepsRecord permission not granted.")
            return SyncerStatistics(recordType = "Steps")
        }

        // 2. Relevant Input Data Check
        val relevantSamples = deviceSamples.filter { it.steps > 0 }.sortedBy { it.timestamp }

        if (relevantSamples.isEmpty()) {
            LOG.info("No relevant step samples (>0) for device '$deviceName' in the provided deviceSamples for slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "Steps")
        }

        LOG.info("Processing ${relevantSamples.size} samples for steps for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")

        val stepsRecordList = mutableListOf<Record>()
        var skippedCount = 0
        relevantSamples.forEach { currentSample ->
            val stepsInMinute = currentSample.steps.toLong()
            if (stepsInMinute > 0) {
                val endTs = Instant.ofEpochSecond(currentSample.timestamp.toLong())
                val startTs = endTs.minus(1, ChronoUnit.MINUTES)

                // Ensure the record's interval [startTs, endTs) overlaps with the slice [sliceStartBoundary, sliceEndBoundary)
                if (endTs.isAfter(sliceStartBoundary) && startTs.isBefore(sliceEndBoundary)) {
                    stepsRecordList.add(StepsRecord(startTs, offset, endTs, offset, stepsInMinute, metadata))
                } else {
                    skippedCount++
                    LOG.debug(
                        "Skipping steps for device '{}' for sample at {} (interval {} to {}) as its interval is outside the slice {} - {}.",
                        deviceName,
                        endTs,
                        startTs,
                        endTs,
                        sliceStartBoundary,
                        sliceEndBoundary
                    )
                }
            }
        }

        // 3. No Valid Records to Insert
        if (stepsRecordList.isEmpty()) {
            LOG.info("No valid StepsRecord created for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary after processing ${relevantSamples.size} samples.")
            return SyncerStatistics(recordsSkipped = skippedCount, recordType = "Steps")
        }

        // 4. Insertion (with chunking)
        LOG.info("Attempting to insert ${stepsRecordList.size} StepsRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        for (chunk in stepsRecordList.chunked(HealthConnectUtils.CHUNK_SIZE)) {
            HealthConnectUtils.insertRecords(chunk, healthConnectClient)
        }

        LOG.info("Successfully inserted ${stepsRecordList.size} StepsRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(recordsSynced = stepsRecordList.size, recordsSkipped = skippedCount, recordType = "Steps")
    }
}
