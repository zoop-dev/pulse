/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.generic_hr

import nodomain.freeyourgadget.gadgetbridge.devices.GenericHeartRateSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.GenericActivitySample
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind

open class GenericHeartRateActivitySampleProvider(device: GBDevice, session: DaoSession) :
    SampleProvider<GenericActivitySample> {

    private val heartRateProvider: GenericHeartRateSampleProvider = GenericHeartRateSampleProvider(device, session)

    override fun normalizeType(rawType: Int): ActivityKind {
        return ActivityKind.fromCode(rawType)
    }

    override fun toRawActivityKind(activityKind: ActivityKind): Int {
        return activityKind.code
    }

    override fun normalizeIntensity(rawIntensity: Int): Float {
        return rawIntensity.toFloat()
    }

    override fun getAllActivitySamples(timestampFrom: Int, timestampTo: Int): MutableList<GenericActivitySample> {
        return heartRateProvider.getAllSamples(timestampFrom * 1000L, timestampTo * 1000L)
            .map { hrSample -> toGenericActivitySample(hrSample) }
            .toMutableList()
    }

    override fun getAllActivitySamplesHighRes(
        timestampFrom: Int,
        timestampTo: Int
    ): MutableList<GenericActivitySample> {
        return getAllActivitySamples(timestampFrom, timestampTo)
    }

    override fun hasHighResData(): Boolean {
        return true
    }

    override fun getActivitySamples(timestampFrom: Int, timestampTo: Int): MutableList<GenericActivitySample> {
        return getAllActivitySamples(timestampFrom, timestampTo)
            .filter { sample -> sample.kind == ActivityKind.ACTIVITY }
            .toMutableList()
    }

    override fun addGBActivitySample(activitySample: GenericActivitySample) {
        throw UnsupportedOperationException("Read-only sample provider")
    }

    override fun addGBActivitySamples(activitySamples: Array<GenericActivitySample>) {
        throw UnsupportedOperationException("Read-only sample provider")
    }

    override fun createActivitySample(): GenericActivitySample {
        return GenericActivitySample()
    }

    override fun getLatestActivitySample(): GenericActivitySample? {
        return heartRateProvider.latestSample?.let { hrSample -> toGenericActivitySample(hrSample) }
    }

    override fun getLatestActivitySample(until: Int): GenericActivitySample? {
        return heartRateProvider.getLatestSample(until * 1000L)?.let { hrSample -> toGenericActivitySample(hrSample) }
    }

    override fun getFirstActivitySample(): GenericActivitySample? {
        return heartRateProvider.firstSample?.let { hrSample -> toGenericActivitySample(hrSample) }
    }

    private fun toGenericActivitySample(hrSample: GenericHeartRateSample): GenericActivitySample {
        val activitySample = GenericActivitySample()
        activitySample.provider = this
        activitySample.timestamp = (hrSample.timestamp / 1000L).toInt()
        activitySample.heartRate = hrSample.heartRate
        return activitySample
    }
}
