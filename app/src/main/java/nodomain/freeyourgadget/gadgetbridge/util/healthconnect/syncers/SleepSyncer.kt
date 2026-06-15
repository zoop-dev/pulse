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
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val LOG = LoggerFactory.getLogger("SleepSyncer")

private const val IN_PROGRESS_THRESHOLD_HOURS = 6L

// Persisted identity of a sleep session, decoupled from the GreenDAO entity (which stores epoch
// seconds). SleepAnalysis is stateless and re-segments as samples arrive, so a night's start can
// move earlier across syncs. We pin a clientRecordId on first sight and reuse it for any later
// detection that overlaps in time, so the single Health Connect record grows in place instead of
// spawning a new id (the old behaviour, which orphaned the night and left junk fragments).
internal data class SleepSessionRow(
    val clientRecordId: String,
    val startTime: Instant,
    val endTime: Instant,
    val finalized: Boolean
)

internal data class DetectedSleepSession(
    val start: Instant,
    val end: Instant
)

internal data class PlannedSleepRecord(
    val detectedIndex: Int,
    val clientRecordId: String,
    val start: Instant,
    val end: Instant
)

internal data class SleepSyncPlan(
    val planned: List<PlannedSleepRecord>,
    val rows: List<SleepSessionRow>,
    val cursor: Instant?
)

internal data class SleepSyncResult(
    val statistics: SyncerStatistics,
    val rows: List<SleepSessionRow>
)

internal object SleepSyncer {

    /**
     * Pure decision core: matches freshly detected sessions against the persisted registry by
     * temporal overlap, freezing the clientRecordId on the matched (or newly minted) row, and
     * derives the sync cursor. No HC / DB / Android dependencies, so it is unit-testable.
     *
     * Cursor contract: the cursor is the latest END of a *completed* session processed this pass,
     * or null if none completed. Sessions are sequential and non-overlapping and the in-progress
     * one is always the most recent, so max(completed.end) <= any open session's start. Returning
     * that (or null to hold) feeds the orchestrator's forward-only advance without ever stranding
     * an open session: its earlier samples stay inside the 24h look-back window on the next run.
     */
    internal fun planSleepSessions(
        existingRows: List<SleepSessionRow>,
        detected: List<DetectedSleepSession>,
        now: Instant,
        thresholdHours: Long,
        mintId: (Instant) -> String
    ): SleepSyncPlan {
        val cutoff = now.minus(thresholdHours, ChronoUnit.HOURS)
        val rows = existingRows.toMutableList()
        val used = HashSet<Int>()
        val planned = ArrayList<PlannedSleepRecord>(detected.size)

        for ((i, d) in detected.withIndex()) {
            var matchIdx = -1
            for (idx in rows.indices) {
                if (idx in used) continue
                val r = rows[idx]
                // Inclusive overlap: d.start <= r.end && d.end >= r.start. SleepAnalysis splits
                // sessions only across a wake gap > 1h, so distinct sessions are never exactly
                // adjacent; inclusive overlap therefore won't merge them, and it still matches a
                // single-sample fragment sitting exactly on the grown session's start edge (which
                // strict overlap would miss, re-orphaning the night).
                if (!d.start.isAfter(r.endTime) && !d.end.isBefore(r.startTime)) {
                    matchIdx = idx
                    break
                }
            }

            val id: String
            val start: Instant
            val end: Instant
            if (matchIdx >= 0) {
                val r = rows[matchIdx]
                start = if (d.start.isBefore(r.startTime)) d.start else r.startTime
                end = if (d.end.isAfter(r.endTime)) d.end else r.endTime
                val finalized = r.finalized || !end.isAfter(cutoff)
                rows[matchIdx] = SleepSessionRow(r.clientRecordId, start, end, finalized)
                used.add(matchIdx)
                id = r.clientRecordId
            } else {
                id = mintId(d.start)
                start = d.start
                end = d.end
                val finalized = !end.isAfter(cutoff)
                rows.add(SleepSessionRow(id, start, end, finalized))
                used.add(rows.size - 1)
            }
            planned.add(PlannedSleepRecord(i, id, start, end))
        }

        var cursor: Instant? = null
        for (idx in used) {
            val r = rows[idx]
            if (r.finalized && (cursor == null || r.endTime.isAfter(cursor))) {
                cursor = r.endTime
            }
        }

        return SleepSyncPlan(planned, rows, cursor)
    }

    /** Drops any row whose end is at or before the prune horizon (cursor - look-back): it can no
     *  longer fall inside any future scan window, so it can never be re-detected, grown, or
     *  finalized again and its frozen id is dead weight. This covers stale OPEN rows too (e.g. an
     *  in-progress session whose samples were deleted, which would otherwise never be pruned). A
     *  genuinely in-progress session is safe: its end is within the 6h threshold of now, while the
     *  horizon is cursor - 24h <= now - 24h, so a live session's end is always after the horizon. */
    internal fun pruneSleepRows(rows: List<SleepSessionRow>, pruneBefore: Instant): List<SleepSessionRow> {
        return rows.filter { it.endTime.isAfter(pruneBefore) }
    }

    suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneId,
        grantedPermissions: Set<String>,
        deviceSamples: List<ActivitySample>,
        context: Context,
        existingRows: List<SleepSessionRow>,
        now: Instant
    ): SleepSyncResult {

        val deviceName = gbDevice.aliasOrName

        if (HealthPermission.getWritePermission(SleepSessionRecord::class) !in grantedPermissions) {
            LOG.info("Skipping Sleep sync for device '$deviceName'; SleepSessionRecord permission not granted.")
            return SleepSyncResult(SyncerStatistics(recordType = "Sleep"), existingRows)
        }

        val device = metadata.device
        if (device == null) {
            LOG.warn("Skipping Sleep sync for device '$deviceName'; no Health Connect device metadata.")
            return SleepSyncResult(SyncerStatistics(recordType = "Sleep"), existingRows)
        }

        if (deviceSamples.isEmpty()) {
            LOG.info("No device samples provided for sleep analysis for device '$deviceName'.")
            return SleepSyncResult(SyncerStatistics(recordType = "Sleep"), existingRows)
        }

        val sortedDeviceSamples = deviceSamples.sortedBy { it.timestamp }
        val allIdentifiedSessions = SleepAnalysis().calculateSleepSessions(sortedDeviceSamples)

        if (allIdentifiedSessions.isEmpty()) {
            LOG.info("No sleep sessions identified by SleepAnalysis for device '$deviceName'.")
            return SleepSyncResult(SyncerStatistics(recordType = "Sleep"), existingRows)
        }

        // Build a record candidate (stage list + its true span) per identified session. No slice
        // ownership filter: a session is no longer tied to the slice its start falls in. Dedup is
        // now provided by the frozen clientRecordId, so re-discovery across the look-back overlap
        // upserts the same record instead of duplicating.
        val candidates = allIdentifiedSessions.mapNotNull { buildCandidate(it, sortedDeviceSamples, deviceName) }
        val skippedCount = allIdentifiedSessions.size - candidates.size
        if (skippedCount > 0) {
            LOG.info("Skipped $skippedCount sleep session(s) for device '$deviceName' (no samples, no valid stages, or invalid timings).")
        }

        if (candidates.isEmpty()) {
            LOG.info("No valid sleep sessions to sync for device '$deviceName'.")
            return SleepSyncResult(SyncerStatistics(recordType = "Sleep", recordsSkipped = skippedCount), existingRows)
        }

        val mintId: (Instant) -> String = { start ->
            val startHourEpoch = start.epochSecond / 3600 * 3600
            "gb-sleep-${device.manufacturer ?: "unknown"}-${device.model ?: "unknown"}-$startHourEpoch"
        }

        val plan = planSleepSessions(
            existingRows = existingRows,
            detected = candidates.map { it.detected },
            now = now,
            thresholdHours = IN_PROGRESS_THRESHOLD_HOURS,
            mintId = mintId
        )

        // clientRecordVersion is the sync run's wall clock so a later run always outranks the value
        // it previously wrote for this id (HC keeps the highest version on conflict), letting a
        // grown session overwrite the partial record it replaces.
        val version = now.epochSecond

        val records = plan.planned.map { p ->
            val candidate = candidates[p.detectedIndex]
            LOG.info("Prepared SleepSessionRecord for device '$deviceName' (Session: ${p.start} to ${p.end}, id=${p.clientRecordId}). Stages: ${candidate.stages.size}")
            SleepSessionRecord(
                startTime = p.start,
                startZoneOffset = offset.rules.getOffset(p.start),
                endTime = p.end,
                endZoneOffset = offset.rules.getOffset(p.end),
                title = context.getString(nodomain.freeyourgadget.gadgetbridge.R.string.health_connect_sleep_session_title, deviceName),
                notes = context.getString(nodomain.freeyourgadget.gadgetbridge.R.string.health_connect_sleep_session_notes, deviceName),
                stages = candidate.stages,
                metadata = Metadata.autoRecorded(
                    clientRecordId = p.clientRecordId,
                    clientRecordVersion = version,
                    device = device
                )
            )
        }

        LOG.info("Attempting to insert ${records.size} SleepSessionRecord(s) for device '$deviceName'.")
        HealthConnectUtils.insertRecords(records, healthConnectClient)
        LOG.info("Successfully inserted SleepSessionRecord(s) for device '$deviceName'.")

        val stats = SyncerStatistics(
            recordsSynced = records.size,
            recordsSkipped = skippedCount,
            recordType = "Sleep",
            latestRecordTimestamp = plan.cursor
        )
        return SleepSyncResult(stats, plan.rows)
    }

    private data class SleepCandidate(
        val detected: DetectedSleepSession,
        val stages: List<SleepSessionRecord.Stage>
    )

    private fun buildCandidate(
        analysisSession: SleepAnalysis.SleepSession,
        sortedDeviceSamples: List<ActivitySample>,
        deviceName: String
    ): SleepCandidate? {
        val sessionBoundaryStart = analysisSession.sleepStart.toInstant()
        val sessionBoundaryEndInclusive = analysisSession.sleepEnd.toInstant()

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

        val stages = buildSleepStages(samplesForThisSession, deviceName)

        if (stages.isEmpty()) {
            LOG.warn("No valid sleep stages derived for session ({} to {}, identified by SleepAnalysis) for device '$deviceName'. Skipping this session.", sessionBoundaryStart, sessionBoundaryEndInclusive)
            return null
        }

        val recordFinalStartTime = stages.first().startTime
        val recordFinalEndTime = stages.last().endTime

        if (!recordFinalEndTime.isAfter(recordFinalStartTime)) {
            LOG.warn("Skipping sleep session for device '$deviceName' due to invalid overall stage timings after processing (End: $recordFinalEndTime, Start: $recordFinalStartTime). Stages: ${stages.size}")
            return null
        }

        return SleepCandidate(DetectedSleepSession(recordFinalStartTime, recordFinalEndTime), stages)
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
