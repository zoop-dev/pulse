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
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.SyncException
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset

private val LOG = LoggerFactory.getLogger("Vo2MaxSyncer")

internal object Vo2MaxSyncer : HealthConnectSyncer {
    override suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneOffset,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        grantedPermissions: Set<String>
    ): SyncerStatistics {
        val deviceName = gbDevice.aliasOrName

        if (HealthPermission.getWritePermission(Vo2MaxRecord::class) !in grantedPermissions) {
            LOG.info("Skipping VO2Max sync for device '$deviceName'; Vo2MaxRecord permission not granted.")
            return SyncerStatistics(recordType = "VO2Max")
        }

        val samples = try {
            GBApplication.acquireDB().use { dbInstance ->
                val provider = gbDevice.deviceCoordinator.getVo2MaxSampleProvider(gbDevice, dbInstance.daoSession)

                if (provider == null) {
                    LOG.warn("Vo2MaxSampleProvider not found for device '$deviceName'. Skipping VO2Max sync for slice $sliceStartBoundary to $sliceEndBoundary.")
                    return@use emptyList<Vo2MaxSample>()
                }
                provider.getAllSamples(sliceStartBoundary.toEpochMilli(), sliceEndBoundary.toEpochMilli())
            }
        } catch (e: Exception) {
            throw SyncException("Error fetching VO2Max samples for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.", e)
        }

        if (samples.isEmpty()) {
            LOG.info("No VO2Max samples found for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "VO2Max")
        }

        LOG.info("Found ${samples.size} VO2Max samples for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary.")

        val recordsToInsert = mutableListOf<Record>()
        var skippedCount = 0
        for (sample in samples) {
            val timestamp = Instant.ofEpochMilli(sample.timestamp)

            if (sample.value <= 0) {
                LOG.warn("Skipping VO2Max sample for device '$deviceName' at $timestamp due to invalid value: ${sample.value}.")
                skippedCount++
                continue
            }

            if (!timestamp.isBefore(sliceStartBoundary) && timestamp.isBefore(sliceEndBoundary)) {
                recordsToInsert.add(
                    Vo2MaxRecord(
                        time = timestamp,
                        zoneOffset = offset,
                        vo2MillilitersPerMinuteKilogram = sample.value.toDouble(),
                        measurementMethod = Vo2MaxRecord.MEASUREMENT_METHOD_OTHER,
                        metadata = metadata
                    )
                )
            } else {
                LOG.debug(
                    "Skipping VO2Max sample for device '{}' at {} as it's outside the slice {} - {}",
                    deviceName,
                    timestamp,
                    sliceStartBoundary,
                    sliceEndBoundary
                )
                skippedCount++
            }
        }

        if (recordsToInsert.isEmpty()) {
            LOG.info("No valid VO2Max records to insert for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary after filtering.")
            return SyncerStatistics(recordsSkipped = skippedCount, recordType = "VO2Max")
        }

        LOG.info("Attempting to insert ${recordsToInsert.size} Vo2MaxRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        HealthConnectUtils.insertRecords(recordsToInsert, healthConnectClient)

        LOG.info("Successfully inserted ${recordsToInsert.size} Vo2MaxRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(recordsSynced = recordsToInsert.size, recordsSkipped = skippedCount, recordType = "VO2Max")
    }
}