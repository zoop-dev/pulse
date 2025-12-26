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
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.SyncException
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset

private val LOG = LoggerFactory.getLogger("HrvSyncer")

internal object HrvSyncer : HealthConnectSyncer {

    private fun createHrvRecordsFromSamples(
        valueSamples: List<HrvValueSample>,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): List<Record> {
        if (valueSamples.isEmpty()) {
            LOG.info("No HRV value samples provided to create records for device '$deviceName'.")
            return emptyList()
        }

        LOG.info("Processing ${valueSamples.size} HRV value samples for device '$deviceName'.")
        val records = mutableListOf<Record>()
        for (sample in valueSamples) {
            val timestamp = Instant.ofEpochMilli(sample.timestamp)

            if (sample.value <= 0) {
                LOG.debug(
                    "Skipping HRV value sample for device '{}' at {} due to non-positive value: {}.",
                    deviceName,
                    timestamp,
                    sample.value
                )
                continue
            }

            if (timestamp.isBefore(sliceStartBoundary) || !timestamp.isBefore(sliceEndBoundary)) {
                LOG.debug(
                    "Skipping HRV value sample for device '{}' at {} (value: {}) as it's outside the slice {} - {}.",
                    deviceName,
                    timestamp,
                    sample.value,
                    sliceStartBoundary,
                    sliceEndBoundary
                )
                continue
            }

            records.add(
                HeartRateVariabilityRmssdRecord(
                    time = timestamp,
                    zoneOffset = offset,
                    heartRateVariabilityMillis = sample.value.toDouble(),
                    metadata = metadata
                )
            )
        }
        return records
    }

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

        if (HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class) !in grantedPermissions) {
            LOG.info("Skipping HRV sync for device '$deviceName'; HeartRateVariabilityRmssdRecord permission not granted.")
            return SyncerStatistics(recordType = "HRV")
        }

        val coordinator = gbDevice.deviceCoordinator
        val valueSamples: List<HrvValueSample> = try {
            GBApplication.acquireDB().use { dbInstance ->
                val valueProvider = coordinator.getHrvValueSampleProvider(gbDevice, dbInstance.daoSession)
                if (valueProvider == null) {
                    LOG.info("HRV Value Provider not available for device '$deviceName'.")
                    return SyncerStatistics(recordType = "HRV")
                }
                valueProvider.getAllSamples(sliceStartBoundary.toEpochMilli(), sliceEndBoundary.toEpochMilli())
            }
        } catch (e: Exception) {
            throw SyncException("Error fetching HRV value samples for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.", e)
        }

        if (valueSamples.isEmpty()) {
            LOG.info("No HRV value samples found by provider for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "HRV")
        }

        val recordsToInsert = createHrvRecordsFromSamples(
            valueSamples,
            sliceStartBoundary,
            sliceEndBoundary,
            offset,
            metadata,
            deviceName
        )

        if (recordsToInsert.isEmpty()) {
            LOG.info("No valid HRV records to insert for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary after processing samples.")
            return SyncerStatistics(recordType = "HRV")
        }

        LOG.info("Attempting to insert ${recordsToInsert.size} HeartRateVariabilityRmssdRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        HealthConnectUtils.insertRecords(recordsToInsert, healthConnectClient)

        LOG.info("Successfully inserted ${recordsToInsert.size} HeartRateVariabilityRmssdRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(recordsSynced = recordsToInsert.size, recordType = "HRV")
    }
}
