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

import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

internal object DistanceSyncer : AbstractActivitySampleSyncer<DistanceRecord>() {
    override val logger: Logger = LoggerFactory.getLogger(DistanceSyncer::class.java)
    override val recordClass: KClass<DistanceRecord> = DistanceRecord::class

    override fun convertSample(
        sample: ActivitySample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): DistanceRecord? {
        val distanceCm = sample.distanceCm
        if (distanceCm <= 0 || distanceCm == ActivitySample.NOT_MEASURED) {
            return null
        }

        val endTs = Instant.ofEpochSecond(sample.timestamp.toLong())
        val startTs = endTs.minus(1, ChronoUnit.MINUTES)

        return DistanceRecord(
            startTime = startTs,
            startZoneOffset = offset,
            endTime = endTs,
            endZoneOffset = offset,
            distance = Length.meters(distanceCm / 100.0),
            metadata = metadata
        )
    }
}
