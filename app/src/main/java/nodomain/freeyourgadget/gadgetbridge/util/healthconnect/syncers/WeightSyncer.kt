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
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.WeightSample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.SyncException
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset

private val LOG = LoggerFactory.getLogger("WeightSyncer")

internal object WeightSyncer : HealthConnectSyncer {
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

        if (HealthPermission.getWritePermission(WeightRecord::class) !in grantedPermissions) {
            LOG.info("Skipping Weight sync for device '$deviceName'; WeightRecord permission not granted.")
            return SyncerStatistics(recordType = "Weight")
        }

        val samples = try {
            GBApplication.acquireDB().use { dbInstance ->
                val provider = gbDevice.deviceCoordinator.getWeightSampleProvider(gbDevice, dbInstance.daoSession)

                if (provider == null) {
                    LOG.warn("WeightSampleProvider not found for device '$deviceName'. Skipping Weight sync for slice $sliceStartBoundary to $sliceEndBoundary.")
                    return@use emptyList<WeightSample>()
                }
                provider.getAllSamples(sliceStartBoundary.toEpochMilli(), sliceEndBoundary.toEpochMilli())
            }
        } catch (e: Exception) {
            throw SyncException("Error fetching Weight samples for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.", e)
        }

        LOG.info("Found ${samples.size} Weight samples for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary.")

        if (samples.isEmpty()) {
            LOG.info("No Weight samples to process for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "Weight")
        }

        val recordsToInsert = mutableListOf<Record>()
        var skippedCount = 0
        for (sample in samples) {
            val timestampValue = sample.timestamp
            val weightInKgValue = sample.getWeightKg()

            val timestamp = Instant.ofEpochMilli(timestampValue)

            if (timestamp.isBefore(sliceStartBoundary) || !timestamp.isBefore(sliceEndBoundary)) {
                LOG.debug(
                    "Skipping Weight sample for device '{}' at {} (weight: {}kg) as it's outside the slice {} - {}.",
                    deviceName,
                    timestamp,
                    weightInKgValue,
                    sliceStartBoundary,
                    sliceEndBoundary
                )
                skippedCount++
                continue
            }

            if (weightInKgValue <= 0) {
                LOG.debug(
                    "Skipping Weight sample for device '{}' at {} due to invalid weight: {}kg (must be > 0).",
                    deviceName,
                    timestamp,
                    weightInKgValue
                )
                skippedCount++
                continue
            }

            recordsToInsert.add(
                WeightRecord(
                    time = timestamp,
                    zoneOffset = offset,
                    weight = Mass.kilograms(weightInKgValue.toDouble()),
                    metadata = metadata
                )
            )
        }

        if (recordsToInsert.isEmpty()) {
            LOG.info("No valid Weight records to insert for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary after filtering.")
            return SyncerStatistics(recordsSkipped = skippedCount, recordType = "Weight")
        }

        LOG.info("Attempting to insert ${recordsToInsert.size} WeightRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        HealthConnectUtils.insertRecords(recordsToInsert, healthConnectClient)

        LOG.info("Successfully inserted ${recordsToInsert.size} WeightRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(recordsSynced = recordsToInsert.size, recordsSkipped = skippedCount, recordType = "Weight")
    }
}