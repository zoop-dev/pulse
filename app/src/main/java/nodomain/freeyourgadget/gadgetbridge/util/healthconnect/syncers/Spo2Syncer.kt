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
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.records.metadata.Device
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractSpo2Sample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.SyncException
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset

private val LOG = LoggerFactory.getLogger("Spo2Syncer")

internal object Spo2Syncer : HealthConnectSyncer {
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

        if (HealthPermission.getWritePermission(OxygenSaturationRecord::class) !in grantedPermissions) {
            LOG.info("Skipping SpO2 sync for device '$deviceName'; OxygenSaturationRecord permission not granted.")
            return SyncerStatistics(recordType = "SpO2")
        }

        // Create Device object for metadata
        val deviceCoordinator = gbDevice.deviceCoordinator
        val device = Device(
            type = when (deviceCoordinator.getDeviceKind(gbDevice)) {
                nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.DeviceKind.WATCH -> Device.TYPE_WATCH
                nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.DeviceKind.PHONE -> Device.TYPE_PHONE
                nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.DeviceKind.SCALE -> Device.TYPE_SCALE
                nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.DeviceKind.RING -> Device.TYPE_RING
                nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.DeviceKind.HEAD_MOUNTED -> Device.TYPE_HEAD_MOUNTED
                nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.DeviceKind.FITNESS_BAND -> Device.TYPE_FITNESS_BAND
                nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.DeviceKind.CHEST_STRAP -> Device.TYPE_CHEST_STRAP
                nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator.DeviceKind.SMART_DISPLAY -> Device.TYPE_SMART_DISPLAY
                else -> Device.TYPE_UNKNOWN
            },
            manufacturer = deviceCoordinator.manufacturer,
            model = gbDevice.model
        )

        val samples = try {
            GBApplication.acquireDB().use { dbInstance ->
                val provider = gbDevice.deviceCoordinator.getSpo2SampleProvider(gbDevice, dbInstance.daoSession)
                if (provider == null) {
                    LOG.warn("Spo2SampleProvider not found for device '$deviceName'. Skipping SpO2 sync for slice $sliceStartBoundary to $sliceEndBoundary.")
                    return@use emptyList<AbstractSpo2Sample>()
                }
                provider.getAllSamples(sliceStartBoundary.toEpochMilli(), sliceEndBoundary.toEpochMilli())
            }
        } catch (e: Exception) {
            throw SyncException("Error fetching SpO2 samples for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.", e)
        }

        LOG.info("Found ${samples.size} SpO2 samples for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary.")

        if (samples.isEmpty()) {
            LOG.info("No SpO2 samples to process for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "SpO2")
        }

        val recordsToInsert = mutableListOf<Record>()
        var skippedCount = 0
        for (sample in samples) {
            val timestamp = Instant.ofEpochMilli(sample.timestamp)
            val spo2Value = sample.spo2
            val spo2AsDouble = spo2Value.toDouble()

            if (timestamp.isBefore(sliceStartBoundary) || !timestamp.isBefore(sliceEndBoundary)) {
                LOG.debug(
                    "Skipping SpO2 sample for device '{}' at {} (value: {}) as it's outside the slice {} - {}.",
                    deviceName,
                    timestamp,
                    spo2AsDouble,
                    sliceStartBoundary,
                    sliceEndBoundary
                )
                skippedCount++
                continue
            }

            if (spo2AsDouble <= 0 || spo2AsDouble > 100) {
                LOG.debug(
                    "Skipping SpO2 sample for device '{}' at {} with invalid value ({}). Valid range: >0 and <=100.",
                    deviceName,
                    timestamp,
                    spo2AsDouble
                )
                skippedCount++
                continue
            }

            // Create appropriate metadata based on measurement type
            val sampleMetadata = when (sample.type) {
                Spo2Sample.Type.MANUAL -> {
                    LOG.trace("SpO2 sample at {} for device '{}' is manually recorded", timestamp, deviceName)
                    Metadata.activelyRecorded(device)
                }
                Spo2Sample.Type.AUTOMATIC -> {
                    LOG.trace("SpO2 sample at {} for device '{}' is automatically recorded", timestamp, deviceName)
                    Metadata.autoRecorded(device)
                }
                Spo2Sample.Type.UNKNOWN -> {
                    LOG.trace("SpO2 sample at {} for device '{}' has unknown type, using autoRecorded", timestamp, deviceName)
                    Metadata.autoRecorded(device)
                }
            }

            recordsToInsert.add(
                OxygenSaturationRecord(
                    time = timestamp,
                    zoneOffset = offset,
                    percentage = Percentage(spo2AsDouble),
                    metadata = sampleMetadata
                )
            )
        }

        if (recordsToInsert.isEmpty()) {
            LOG.info("No valid SpO2 records to insert for device '$deviceName' in slice $sliceStartBoundary to $sliceEndBoundary after filtering.")
            return SyncerStatistics(recordsSkipped = skippedCount, recordType = "SpO2")
        }

        LOG.info("Attempting to insert ${recordsToInsert.size} OxygenSaturationRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        HealthConnectUtils.insertRecords(recordsToInsert, healthConnectClient)

        LOG.info("Successfully inserted ${recordsToInsert.size} OxygenSaturationRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(recordsSynced = recordsToInsert.size, recordsSkipped = skippedCount, recordType = "SpO2")
    }
}