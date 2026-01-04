/*  Copyright (C) 2026 José Rebelo

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
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.TimeSample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.SyncException
import org.slf4j.Logger
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

/**
 * A base class for synchronizing generic Gadgetbridge TimeSample samples to Health Connect.
 * This class provides common logic for fetching samples and converting them to records.
 *
 * Note: We need recordClass as an abstract property due to JVM type erasure - generic type
 * information is not available at runtime, so we can't automatically derive TRecord.
 */
internal abstract class AbstractTimeSampleSyncer<TSample : TimeSample, TRecord : Record> : HealthConnectSyncer {
    protected abstract val logger: Logger
    protected abstract val recordClass: KClass<TRecord>

    protected abstract fun getSampleProvider(
        gbDevice: GBDevice,
        daoSession: DaoSession
    ): TimeSampleProvider<out TSample>?

    protected abstract fun convertSample(
        sample: TSample,
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
        grantedPermissions: Set<String>
    ): SyncerStatistics {
        val deviceName = gbDevice.aliasOrName
        val recordTypeName = recordClass.simpleName ?: "Unknown"

        // Check permissions
        if (HealthPermission.getWritePermission(recordClass) !in grantedPermissions) {
            logger.info("Skipping $recordTypeName sync for device '$deviceName'; $recordTypeName permission not granted.")
            return SyncerStatistics(recordType = recordTypeName)
        }

        // Fetch samples
        val samples: List<TSample> = try {
            GBApplication.acquireDB().use { dbInstance ->
                val provider = getSampleProvider(gbDevice, dbInstance.daoSession)
                if (provider == null) {
                    logger.info("$recordTypeName sample provider not available for device '$deviceName'.")
                    return SyncerStatistics(recordType = recordTypeName)
                }
                provider.getAllSamples(sliceStartBoundary.toEpochMilli(), sliceEndBoundary.toEpochMilli())
            }
        } catch (e: Exception) {
            throw SyncException(
                "Error fetching $recordTypeName samples for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.",
                e
            )
        }

        if (samples.isEmpty()) {
            logger.info("No $recordTypeName samples found by provider for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = recordTypeName)
        }

        logger.info("Processing ${samples.size} $recordTypeName samples for device '$deviceName'.")

        // Convert samples to records
        var skippedCount = 0

        val recordsToInsert = samples.filter {
            val timestamp = Instant.ofEpochMilli(it.timestamp)
            // Use inclusive boundaries [sliceStart, sliceEnd] to ensure last sample is included
            if (timestamp.isBefore(sliceStartBoundary) || timestamp.isAfter(sliceEndBoundary)) {
                logger.debug(
                    "Skipping sample for at {} as it's outside the slice {} - {}.",
                    timestamp,
                    sliceStartBoundary,
                    sliceEndBoundary
                )
                return@filter false
            }
            return@filter true
        }.mapNotNull {
            convertSample(
                it,
                offset,
                metadata,
                deviceName
            ) ?: run {
                skippedCount++
                null
            }
        }

        if (recordsToInsert.isEmpty()) {
            logger.info("No valid $recordTypeName records to insert for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary after processing samples.")
            return SyncerStatistics(recordsSkipped = skippedCount, recordType = recordTypeName)
        }

        // Insert records
        logger.info("Attempting to insert ${recordsToInsert.size} $recordTypeName(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        HealthConnectUtils.insertRecords(recordsToInsert, healthConnectClient)

        logger.info("Successfully inserted ${recordsToInsert.size} $recordTypeName(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(
            recordsSynced = recordsToInsert.size,
            recordsSkipped = skippedCount,
            recordType = recordTypeName
        )
    }
}
