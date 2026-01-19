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
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

private val LOG = LoggerFactory.getLogger("HeartRateSyncer")

internal object HeartRateSyncer : ActivitySampleSyncer {
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

        val deviceName = gbDevice.aliasOrName // Use Gadgetbridge device name for logging

        // 1. Permission Check
        if (HealthPermission.getWritePermission(HeartRateRecord::class) !in grantedPermissions) {
            LOG.info("Skipping Heart Rate sync for device '$deviceName'; HeartRateRecord permission not granted.")
            return SyncerStatistics(recordType = "HeartRate")
        }

        // 2. Relevant Input Data Check
        val validHRSamples = deviceSamples
            .filter { it.heartRate in 20..250 }
            .sortedBy { it.timestamp }

        if (validHRSamples.isEmpty()) {
            LOG.info("No valid heart rate samples found for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "HeartRate")
        }

        LOG.info("Processing ${validHRSamples.size} valid heart rate samples for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")

        val heartRateRecordList = mutableListOf<Record>()
        val currentHcSamples = mutableListOf<HeartRateRecord.Sample>()
        var previousSampleTimestamp: Instant? = null
        var skippedCount = 0

        for (gbSample in validHRSamples) {
            val currentSampleTimestamp = Instant.ofEpochSecond(gbSample.timestamp.toLong())

            // Use inclusive boundaries [sliceStart, sliceEnd] for the slice
            if (currentSampleTimestamp.isBefore(sliceStartBoundary) || currentSampleTimestamp.isAfter(sliceEndBoundary)) {
                skippedCount++
                continue
            }

            previousSampleTimestamp?.let { prevTs ->
                val newDay = ZonedDateTime.ofInstant(prevTs, offset).toLocalDate() != ZonedDateTime.ofInstant(currentSampleTimestamp, offset).toLocalDate()
                val gapTooLong = currentSampleTimestamp.epochSecond - prevTs.epochSecond > 15 * 60 // 15 min gap
                val samplesFull = currentHcSamples.size >= HealthConnectUtils.MAX_SAMPLES_PER_HEART_RATE_RECORD // Use a defined constant

                if (newDay || gapTooLong || samplesFull) {
                    if (currentHcSamples.isNotEmpty()) {
                        val recordStartTime = currentHcSamples.first().time
                        var recordEndTime = currentHcSamples.last().time
                        
                        if (recordEndTime == recordStartTime) { // Ensure duration is positive for HC
                            recordEndTime = recordEndTime.plusSeconds(1)
                        }

                        if (recordEndTime.isAfter(recordStartTime)) { // Should always be true after adjustment if currentHcSamples is not empty
                            LOG.debug(
                                "Creating HeartRateRecord for device '{}' from {} to {} with {} samples.",
                                deviceName,
                                recordStartTime,
                                recordEndTime,
                                currentHcSamples.size
                            )
                            heartRateRecordList.add(HeartRateRecord(recordStartTime, offset, recordEndTime, offset, ArrayList(currentHcSamples), metadata))
                        } else {
                             LOG.warn("Skipping HeartRateRecord for device '$deviceName' from $recordStartTime to $recordEndTime due to invalid duration even after adjustment.")
                        }
                    }
                    currentHcSamples.clear()
                }
            }
            currentHcSamples.add(HeartRateRecord.Sample(currentSampleTimestamp, gbSample.heartRate.toLong()))
            previousSampleTimestamp = currentSampleTimestamp
        }

        if (currentHcSamples.isNotEmpty()) {
            val recordStartTime = currentHcSamples.first().time
            var recordEndTime = currentHcSamples.last().time
            if (recordEndTime == recordStartTime) { // Ensure duration is positive for HC
                recordEndTime = recordEndTime.plusSeconds(1)
            }

            if (recordEndTime.isAfter(recordStartTime)) {
                LOG.debug(
                    "Creating final HeartRateRecord for device '{}' from {} to {} with {} samples.",
                    deviceName,
                    recordStartTime,
                    recordEndTime,
                    currentHcSamples.size
                )
                heartRateRecordList.add(HeartRateRecord(recordStartTime, offset, recordEndTime, offset, ArrayList(currentHcSamples), metadata))
            } else {
                LOG.warn("Skipping final HeartRateRecord for device '$deviceName' from $recordStartTime to $recordEndTime due to invalid duration even after adjustment.")
            }
        }

        if (heartRateRecordList.isEmpty()) {
            LOG.info("No valid HeartRateRecord(s) created for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary after processing ${validHRSamples.size} samples.")
            return SyncerStatistics(recordsSkipped = skippedCount, recordType = "HeartRate")
        }

        LOG.info("Attempting to insert ${heartRateRecordList.size} HeartRateRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        for (chunk in heartRateRecordList.chunked(HealthConnectUtils.CHUNK_SIZE)) {
            HealthConnectUtils.insertRecords(chunk, healthConnectClient)
        }

        LOG.info("Successfully inserted ${heartRateRecordList.size} HeartRateRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(recordsSynced = heartRateRecordList.size, recordsSkipped = skippedCount, recordType = "HeartRate")
    }
}