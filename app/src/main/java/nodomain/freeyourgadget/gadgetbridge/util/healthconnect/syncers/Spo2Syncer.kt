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

import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Percentage
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

internal object Spo2Syncer : AbstractTimeSampleSyncer<Spo2Sample, OxygenSaturationRecord>() {
    override val logger: Logger = LoggerFactory.getLogger(Spo2Syncer::class.java)
    override val recordClass: KClass<OxygenSaturationRecord> = OxygenSaturationRecord::class

    override fun getSampleProvider(
        gbDevice: GBDevice,
        daoSession: DaoSession
    ): TimeSampleProvider<out Spo2Sample>? {
        return gbDevice.deviceCoordinator.getSpo2SampleProvider(gbDevice, daoSession)
    }

    override fun convertSample(
        sample: Spo2Sample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): OxygenSaturationRecord? {
        val spo2AsDouble = sample.spo2.toDouble()

        if (spo2AsDouble <= 0 || spo2AsDouble > 100) {
            logger.debug(
                "Skipping SpO2 sample for device '{}' with invalid value ({}). Valid range: >0 and <=100.",
                deviceName,
                spo2AsDouble
            )
            return null
        }

        // Create appropriate metadata based on measurement type
        val sampleMetadata = when (sample.type) {
            Spo2Sample.Type.MANUAL -> Metadata.activelyRecorded(metadata.device!!)
            Spo2Sample.Type.AUTOMATIC -> Metadata.autoRecorded(metadata.device!!)
            Spo2Sample.Type.UNKNOWN -> Metadata.unknownRecordingMethod(metadata.device!!)
        }

        return OxygenSaturationRecord(
            time = Instant.ofEpochMilli(sample.timestamp),
            zoneOffset = offset,
            percentage = Percentage(spo2AsDouble),
            metadata = sampleMetadata
        )
    }
}
