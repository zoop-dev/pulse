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

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

internal object ActiveCaloriesSyncer : AbstractActivitySampleSyncer<ActiveCaloriesBurnedRecord>() {
    override val logger: Logger = LoggerFactory.getLogger(StepsSyncer::class.java)
    override val recordClass: KClass<ActiveCaloriesBurnedRecord> = ActiveCaloriesBurnedRecord::class

    override fun convertSample(
        sample: ActivitySample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): ActiveCaloriesBurnedRecord? {
        val caloriesInMinute = sample.activeCalories
        if (caloriesInMinute <= 0) {
            return null
        }

        val endTs = Instant.ofEpochSecond(sample.timestamp.toLong())
        val startTs = endTs.minus(1, ChronoUnit.MINUTES)

        return ActiveCaloriesBurnedRecord(
            startTs,
            offset,
            endTs,
            offset,
            Energy.kilocalories(caloriesInMinute.toDouble()),
            metadata
        )
    }
}
