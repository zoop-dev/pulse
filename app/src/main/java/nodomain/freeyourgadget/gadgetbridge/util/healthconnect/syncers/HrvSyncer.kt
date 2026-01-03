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

import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

internal object HrvSyncer : AbstractTimeSampleSyncer<HrvValueSample, HeartRateVariabilityRmssdRecord>() {
    override val logger: Logger = LoggerFactory.getLogger(HrvSyncer::class.java)
    override val recordClass: KClass<HeartRateVariabilityRmssdRecord> = HeartRateVariabilityRmssdRecord::class

    override fun getSampleProvider(
        gbDevice: GBDevice,
        daoSession: DaoSession
    ): TimeSampleProvider<out HrvValueSample>? {
        return gbDevice.deviceCoordinator.getHrvValueSampleProvider(gbDevice, daoSession)
    }

    override fun convertSample(
        sample: HrvValueSample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): HeartRateVariabilityRmssdRecord? {
        if (sample.value <= 0) {
            logger.debug(
                "Skipping HRV value sample for device '{}' due to non-positive value: {}.",
                deviceName,
                sample.value
            )
            return null
        }

        return HeartRateVariabilityRmssdRecord(
            time = Instant.ofEpochMilli(sample.timestamp),
            zoneOffset = offset,
            heartRateVariabilityMillis = sample.value.toDouble(),
            metadata = metadata
        )
    }
}
