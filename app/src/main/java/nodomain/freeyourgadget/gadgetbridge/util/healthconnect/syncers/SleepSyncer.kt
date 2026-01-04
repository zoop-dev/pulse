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

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset

private val LOG = LoggerFactory.getLogger("SleepSyncer")

internal object SleepSyncer : ContextualActivitySampleSyncer {

    override suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneOffset,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        grantedPermissions: Set<String>,
        deviceSamples: List<ActivitySample>,
        context: Context
    ): SyncerStatistics {

        val deviceName = gbDevice.aliasOrName


        if (HealthPermission.getWritePermission(SleepSessionRecord::class) !in grantedPermissions) {
            LOG.info("Skipping Sleep sync for device '$deviceName'; SleepSessionRecord permission not granted.")
            return SyncerStatistics(recordType = "Sleep")
        }

        if (deviceSamples.isEmpty()) {
            LOG.info("No device samples provided for sleep analysis for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "Sleep")
        }

        val sortedDeviceSamples = deviceSamples.sortedBy { it.timestamp }

        val sleepAnalysis = SleepAnalysis()
        val allIdentifiedSessions = sleepAnalysis.calculateSleepSessions(sortedDeviceSamples)

        if (allIdentifiedSessions.isEmpty()) {
            LOG.info("No sleep sessions identified by SleepAnalysis for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "Sleep")
        }

        LOG.info("SleepAnalysis identified ${allIdentifiedSessions.size} sleep sessions for device '$deviceName'. Filtering by slice: $sliceStartBoundary to $sliceEndBoundary.")

        // Convert all sessions to records, filtering out invalid ones
        val sleepSessionRecordList = allIdentifiedSessions.mapNotNull { analysisSession ->
            sleepSessionToRecord(
                analysisSession = analysisSession,
                sortedDeviceSamples = sortedDeviceSamples,
                sliceStartBoundary = sliceStartBoundary,
                sliceEndBoundary = sliceEndBoundary,
                offset = offset,
                metadata = metadata,
                context = context,
                deviceName = deviceName
            )
        }

        val skippedCount = allIdentifiedSessions.size - sleepSessionRecordList.size

        if (skippedCount > 0) {
            LOG.info("Skipped $skippedCount sleep session(s) for device '$deviceName' (outside slice, no samples, no valid stages, or invalid timings).")
        }

        LOG.info("Finished processing ${sleepSessionRecordList.size} valid sleep session(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")

        if (sleepSessionRecordList.isEmpty()) {
            LOG.info("No valid SleepSessionRecord(s) created for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "Sleep", recordsSkipped = skippedCount)
        }

        LOG.info("Attempting to insert ${sleepSessionRecordList.size} SleepSessionRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        HealthConnectUtils.insertRecords(sleepSessionRecordList, healthConnectClient)
        LOG.info("Successfully inserted SleepSessionRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(recordsSynced = sleepSessionRecordList.size, recordsSkipped = skippedCount, recordType = "Sleep")
    }

    /**
     * Converts a sleep analysis session to a SleepSessionRecord.
     * Returns null if the session is invalid or should be skipped (e.g., outside slice, no valid stages).
     */
    private fun sleepSessionToRecord(
        analysisSession: SleepAnalysis.SleepSession,
        sortedDeviceSamples: List<ActivitySample>,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        context: Context,
        deviceName: String
    ): SleepSessionRecord? {
        // Get session boundaries from SleepAnalysis (timestamps of first and last sample in the session)
        val sessionBoundaryStart = analysisSession.sleepStart.toInstant()
        val sessionBoundaryEndInclusive = analysisSession.sleepEnd.toInstant()

        // Check if session overlaps with the current slice (inclusive boundaries [sliceStart, sliceEnd])
        // Skip if: sessionStart > sliceEnd OR sessionEnd < sliceStart
        if (sessionBoundaryStart.isAfter(sliceEndBoundary) || sessionBoundaryEndInclusive.isBefore(sliceStartBoundary)) {
            LOG.debug(
                "Skipping sleep session (identified by SleepAnalysis) for device '{}' (Timeframe: {} to {}) as it does not overlap with current slice ({} to {}).",
                deviceName,
                sessionBoundaryStart,
                sessionBoundaryEndInclusive,
                sliceStartBoundary,
                sliceEndBoundary
            )
            return null
        }

        // Filter the original (sorted) device samples that fall within this specific session's timeframe
        val samplesForThisSession = sortedDeviceSamples.filter {
            val sampleEpochSeconds = it.timestamp.toLong()
            sampleEpochSeconds >= (analysisSession.sleepStart.time / 1000L) &&
                    sampleEpochSeconds <= (analysisSession.sleepEnd.time / 1000L)
        }

        if (samplesForThisSession.isEmpty()) {
            LOG.debug(
                "Skipping session from SleepAnalysis for device '{}' as no samples were found in the original list for its timeframe ({} to {}).",
                deviceName,
                sessionBoundaryStart,
                sessionBoundaryEndInclusive
            )
            return null
        }

        val nominalSessionStart = samplesForThisSession.first().timestamp.toLong().let { Instant.ofEpochSecond(it) }
        val nominalSessionEnd = samplesForThisSession.last().timestamp.toLong().let { Instant.ofEpochSecond(it) }

        LOG.info("Processing sleep session (identified by SleepAnalysis) for device '$deviceName' (Nominal sample range: $nominalSessionStart to $nominalSessionEnd) as it overlaps with slice $sliceStartBoundary to $sliceEndBoundary.")

        // Build sleep stages from samples
        val stages = buildSleepStages(samplesForThisSession, deviceName)

        if (stages.isEmpty()) {
            LOG.warn("No valid sleep stages derived for session (Nominal range: $nominalSessionStart to $nominalSessionEnd, identified by SleepAnalysis) for device '$deviceName'. Skipping this session.")
            return null
        }

        val recordFinalStartTime = stages.first().startTime
        val recordFinalEndTime = stages.last().endTime

        if (!recordFinalEndTime.isAfter(recordFinalStartTime)) {
            LOG.warn("Skipping sleep session for device '$deviceName' due to invalid overall stage timings after processing (End: $recordFinalEndTime, Start: $recordFinalStartTime). Stages: ${stages.size}")
            return null
        }

        LOG.info("Prepared SleepSessionRecord for device '$deviceName' (Session: $recordFinalStartTime to $recordFinalEndTime). Stages: ${stages.size}")

        return SleepSessionRecord(
            startTime = recordFinalStartTime,
            startZoneOffset = offset,
            endTime = recordFinalEndTime,
            endZoneOffset = offset,
            title = context.getString(nodomain.freeyourgadget.gadgetbridge.R.string.health_connect_sleep_session_title, deviceName),
            notes = context.getString(nodomain.freeyourgadget.gadgetbridge.R.string.health_connect_sleep_session_notes, deviceName),
            stages = stages,
            metadata = metadata
        )
    }

    /**
     * Builds sleep stages from activity samples by grouping consecutive samples of the same type.
     */
    private fun buildSleepStages(
        samplesForThisSession: List<ActivitySample>,
        deviceName: String
    ): List<SleepSessionRecord.Stage> {
        val stages = mutableListOf<SleepSessionRecord.Stage>()
        var currentIndex = 0

        while (currentIndex < samplesForThisSession.size) {
            val firstSampleOfStage = samplesForThisSession[currentIndex]
            val stageType = mapActivityKindToSleepStage(firstSampleOfStage.kind)

            if (stageType == SleepSessionRecord.STAGE_TYPE_UNKNOWN) {
                currentIndex++
                continue
            }

            val stageStartTime = Instant.ofEpochSecond(firstSampleOfStage.timestamp.toLong())
            var nextDifferentSampleIndex = currentIndex + 1
            while (nextDifferentSampleIndex < samplesForThisSession.size &&
                mapActivityKindToSleepStage(samplesForThisSession[nextDifferentSampleIndex].kind) == stageType) {
                nextDifferentSampleIndex++
            }

            val stageEndTime: Instant
            if (nextDifferentSampleIndex < samplesForThisSession.size) {
                // Stage ends when the next, different sample begins
                stageEndTime = Instant.ofEpochSecond(samplesForThisSession[nextDifferentSampleIndex].timestamp.toLong())
            } else {
                // This is the last stage of this session. End time is slightly after the last sample's timestamp.
                val lastSampleTimestamp = samplesForThisSession.last().timestamp.toLong()
                val provisionalEnd = Instant.ofEpochSecond(lastSampleTimestamp)
                // Ensure endTime is exclusive and after startTime
                stageEndTime = if (provisionalEnd.plusMillis(1).isAfter(stageStartTime)) {
                    provisionalEnd.plusMillis(1)
                } else {
                    stageStartTime.plusSeconds(1) // Fallback for very short/single sample stages
                }
            }

            if (stageEndTime.isAfter(stageStartTime)) {
                stages.add(SleepSessionRecord.Stage(stageStartTime, stageEndTime, stageType))
            } else {
                LOG.trace(
                    "Skipping zero or negative duration stage for device '{}' at {} (type {}), proposed end {}.",
                    deviceName,
                    stageStartTime,
                    stageType,
                    stageEndTime
                )
            }
            currentIndex = nextDifferentSampleIndex
        }

        return stages
    }

    private fun mapActivityKindToSleepStage(activityKind: ActivityKind): Int {
        return when (activityKind) {
            ActivityKind.DEEP_SLEEP -> SleepSessionRecord.STAGE_TYPE_DEEP
            ActivityKind.LIGHT_SLEEP -> SleepSessionRecord.STAGE_TYPE_LIGHT
            ActivityKind.REM_SLEEP -> SleepSessionRecord.STAGE_TYPE_REM
            ActivityKind.AWAKE_SLEEP -> SleepSessionRecord.STAGE_TYPE_AWAKE
            else -> SleepSessionRecord.STAGE_TYPE_UNKNOWN
        }
    }
}
