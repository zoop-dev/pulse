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

import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

internal object RestingHeartRateSyncer : AbstractTimeSampleSyncer<HeartRateSample, RestingHeartRateRecord>() {
    override val logger: Logger = LoggerFactory.getLogger(RestingHeartRateSyncer::class.java)
    override val recordClass: KClass<RestingHeartRateRecord> = RestingHeartRateRecord::class

    override fun getSampleProvider(
        gbDevice: GBDevice,
        daoSession: DaoSession
    ): TimeSampleProvider<out HeartRateSample>? {
        return gbDevice.deviceCoordinator.getHeartRateRestingSampleProvider(gbDevice, daoSession)
    }

    override fun convertSample(
        sample: HeartRateSample,
        offset: ZoneOffset,
        metadata: Metadata,
        deviceName: String
    ): RestingHeartRateRecord? {
        if (sample.heartRate !in 20..250) {
            return null
        }

        return RestingHeartRateRecord(
            time = Instant.ofEpochMilli(sample.timestamp),
            zoneOffset = offset,
            beatsPerMinute = sample.heartRate.toLong(),
            metadata = metadata
        )
    }
}
