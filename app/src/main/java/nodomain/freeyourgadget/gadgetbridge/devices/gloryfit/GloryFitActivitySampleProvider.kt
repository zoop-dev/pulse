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
package nodomain.freeyourgadget.gadgetbridge.devices.gloryfit

import nodomain.freeyourgadget.gadgetbridge.devices.GenericHeartRateSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSleepStageSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.GenericActivitySample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.util.RangeMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class GloryFitActivitySampleProvider(device: GBDevice, session: DaoSession) :
    SampleProvider<GenericActivitySample> {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GloryFitActivitySampleProvider::class.java)
    }

    private val stepsProvider: GloryFitStepsSampleProvider = GloryFitStepsSampleProvider(device, session)
    private val heartRateProvider: GenericHeartRateSampleProvider = GenericHeartRateSampleProvider(device, session)
    private val sleepStagesProvider: GenericSleepStageSampleProvider = GenericSleepStageSampleProvider(device, session)

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
        val byTimestamp: MutableMap<Int, GenericActivitySample> = mutableMapOf()
        val ret: MutableList<GenericActivitySample> = mutableListOf()

        val stepsSamples = stepsProvider.getAllSamples(timestampFrom * 1000L - 2 * 86400L, timestampTo * 1000L)
        for (stepsSample in stepsSamples) {
            val activitySample = GenericActivitySample()
            activitySample.provider = this
            activitySample.timestamp = (stepsSample.timestamp / 1000L).toInt()
            activitySample.steps = stepsSample.totalSteps
            ret.add(activitySample)
            byTimestamp.put(activitySample.timestamp, activitySample)
        }
        val hrSamples = heartRateProvider.getAllSamples(timestampFrom * 1000L - 2 * 86400L, timestampTo * 1000L)
        for (hrSample in hrSamples) {
            val timestamp = (hrSample.timestamp / 1000L).toInt()
            if (byTimestamp.contains(timestamp)) {
                byTimestamp[timestamp]!!.heartRate = hrSample.heartRate
            } else {
                val activitySample = GenericActivitySample()
                activitySample.provider = this
                activitySample.timestamp = timestamp
                activitySample.heartRate = hrSample.heartRate
                ret.add(activitySample)
                byTimestamp.put(activitySample.timestamp, activitySample)
            }
        }

        // TODO fill gaps?

        overlaySleep(ret, timestampFrom, timestampTo)

        return ret
            .filter { sample -> sample.timestamp in timestampFrom..timestampTo }
            .sortedBy { sample -> sample.timestamp }
            .toMutableList()
    }

    override fun getAllActivitySamplesHighRes(
        timestampFrom: Int,
        timestampTo: Int
    ): MutableList<GenericActivitySample> {
        return getAllActivitySamples(timestampFrom, timestampTo)
    }

    override fun hasHighResData(): Boolean {
        return false
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
        // TODO getLatestActivitySample
        LOG.warn("getLatestActivitySample not implemented");
        return null
    }

    override fun getLatestActivitySample(until: Int): GenericActivitySample? {
        // TODO getLatestActivitySample
        LOG.warn("getLatestActivitySample(until) not implemented");
        return null
    }

    override fun getFirstActivitySample(): GenericActivitySample? {
        // TODO getFirstActivitySample
        LOG.warn("getFirstActivitySample not implemented");
        return null
    }

    fun overlaySleep(samples: MutableList<GenericActivitySample>, timestampFrom: Int, timestampTo: Int) {
        val stagesMap = RangeMap<Long, ActivityKind?>(RangeMap.Mode.LOWER_BOUND)

        // Retrieve the last stage before this time range, as the user could have been asleep during
        // the range transition
        val lastSleepStageBeforeRange = sleepStagesProvider.getLastSampleBefore(timestampFrom * 1000L)

        if (lastSleepStageBeforeRange != null) {
            LOG.debug(
                "Last sleep stage before range: ts={}, stage={}",
                lastSleepStageBeforeRange.timestamp,
                lastSleepStageBeforeRange.stage
            )
            stagesMap.put(
                lastSleepStageBeforeRange.timestamp,
                sleepStageToActivityKind(lastSleepStageBeforeRange.stage)
            )
            stagesMap.put(
                lastSleepStageBeforeRange.timestamp + lastSleepStageBeforeRange.duration * 60 * 1000L,
                ActivityKind.UNKNOWN
            )
        }

        // Retrieve all sleep stage samples during the range
        val sleepStagesInRange = sleepStagesProvider.getAllSamples(
            timestampFrom * 1000L,
            timestampTo * 1000L
        )

        if (!sleepStagesInRange.isEmpty()) {
            // We got actual sleep stages
            LOG.debug(
                "Found {} sleep stage samples between {} and {}",
                sleepStagesInRange.size,
                timestampFrom,
                timestampTo
            )

            for (stageSample in sleepStagesInRange) {
                stagesMap.put(
                    stageSample!!.timestamp,
                    sleepStageToActivityKind(stageSample.stage)
                )
                stagesMap.put(
                    stageSample.timestamp + stageSample.duration * 60 * 1000L,
                    ActivityKind.UNKNOWN
                )
            }
        }

        if (!stagesMap.isEmpty) {
            LOG.debug(
                "Found {} sleep stage samples between {} and {}",
                stagesMap.size(),
                timestampFrom,
                timestampTo
            )

            for (sample in samples) {
                val ts = sample.timestamp * 1000L
                val sleepType = stagesMap.get(ts)
                if (sleepType != null && sleepType != ActivityKind.UNKNOWN) {
                    sample.rawKind = sleepType.code
                    sample.rawIntensity = ActivitySample.NOT_MEASURED
                }
            }
        }
    }

    fun sleepStageToActivityKind(stage: Int): ActivityKind {
        return when (stage) {
            1 -> ActivityKind.DEEP_SLEEP
            2 -> ActivityKind.LIGHT_SLEEP
            3 -> ActivityKind.AWAKE_SLEEP
            4 -> ActivityKind.REM_SLEEP
            else -> ActivityKind.UNKNOWN
        }
    }
}
