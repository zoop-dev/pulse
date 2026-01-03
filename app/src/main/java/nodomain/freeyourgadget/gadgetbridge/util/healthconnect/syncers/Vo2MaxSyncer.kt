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

import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

internal object Vo2MaxSyncer : AbstractTimeSampleSyncer<Vo2MaxSample, Vo2MaxRecord>() {
    override val logger: Logger = LoggerFactory.getLogger(Vo2MaxSyncer::class.java)
    override val recordClass: KClass<Vo2MaxRecord> = Vo2MaxRecord::class

    override fun getSampleProvider(
        gbDevice: GBDevice,
        daoSession: DaoSession
    ): TimeSampleProvider<out Vo2MaxSample>? {
        return gbDevice.deviceCoordinator.getVo2MaxSampleProvider(gbDevice, daoSession)
    }

    override fun convertSample(
        sample: Vo2MaxSample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): Vo2MaxRecord? {
        if (sample.value <= 0) {
            logger.debug("Skipping VO2Max sample for device '$deviceName' due to invalid value: ${sample.value}.")
            return null
        }

        return Vo2MaxRecord(
            time = Instant.ofEpochMilli(sample.timestamp),
            zoneOffset = offset,
            vo2MillilitersPerMinuteKilogram = sample.value.toDouble(),
            measurementMethod = Vo2MaxRecord.MEASUREMENT_METHOD_OTHER,
            metadata = metadata
        )
    }
}