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

private val LOG = LoggerFactory.getLogger("SleepSyncer")

// Persisted per-night identity. SleepAnalysis re-segments a night's start earlier across syncs, so
// we freeze a clientRecordId on first sight and reuse it for any later time-overlapping detection;
// the HC record then grows in place instead of orphaning the night under a new id.
internal data class SleepSessionRow(
    val clientRecordId: String,
    val startTime: Instant,
    val endTime: Instant
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
    val rows: List<SleepSessionRow>
)

internal data class SleepSyncResult(
    val statistics: SyncerStatistics,
    val rows: List<SleepSessionRow>
)

internal object SleepSyncer {

    /**
     * Pure decision core (no HC/DB/Android deps, unit-testable): overlap-match each detection to a
     * stored row, freeze/reuse its clientRecordId, grow its span. Only new or grown sessions go into
     * `planned`; an unchanged re-detection keeps its row but is skipped so the look-back re-scan
     * doesn't rewrite the same night to HC each sync.
     */
    internal fun planSleepSessions(
        existingRows: List<SleepSessionRow>,
        detected: List<DetectedSleepSession>,
        mintId: (Instant) -> String
    ): SleepSyncPlan {
        val rows = existingRows.toMutableList()
        val used = HashSet<Int>()
        val planned = ArrayList<PlannedSleepRecord>(detected.size)

        for ((i, d) in detected.withIndex()) {
            var matchIdx = -1
            for (idx in rows.indices) {
                if (idx in used) continue
                val r = rows[idx]
                // Inclusive overlap (d.start <= r.end && d.end >= r.start): SleepAnalysis only
                // splits across a >1h wake gap so distinct sessions never touch, and inclusive
                // still matches a fragment sitting exactly on the grown session's start edge.
                if (!d.start.isAfter(r.endTime) && !d.end.isBefore(r.startTime)) {
                    matchIdx = idx
                    break
                }
            }

            if (matchIdx >= 0) {
                val r = rows[matchIdx]
                val start = if (d.start.isBefore(r.startTime)) d.start else r.startTime
                val end = if (d.end.isAfter(r.endTime)) d.end else r.endTime
                used.add(matchIdx)
                val changed = start != r.startTime || end != r.endTime
                if (changed) {
                    rows[matchIdx] = SleepSessionRow(r.clientRecordId, start, end)
                    planned.add(PlannedSleepRecord(i, r.clientRecordId, start, end))
                }
            } else {
                val id = mintId(d.start)
                rows.add(SleepSessionRow(id, d.start, d.end))
                used.add(rows.size - 1)
                planned.add(PlannedSleepRecord(i, id, d.start, d.end))
            }
        }

        return SleepSyncPlan(planned, rows)
    }

    /** Drops rows whose end predates the prune horizon (cursor - look-back): unreachable by any
     *  future scan, so the frozen id is dead weight. */
    internal fun pruneSleepRows(rows: List<SleepSessionRow>, pruneBefore: Instant): List<SleepSessionRow> {
        return rows.filter { it.endTime.isAfter(pruneBefore) }
    }

    /**
     * Pure fetch-window lower bound for SLEEP (no HC/DB/Android deps, unit-testable). SleepAnalysis
     * re-derives stages from the fetched samples alone, so a stored night whose start predates the
     * plain look-back would be re-detected as a clipped tail and overwrite its good HC record with
     * truncated stages (issue #6453). Extend the start back to the earliest stored row that overlaps
     * [baseStart, end] but began before baseStart. Clamped to `floor` so a stale row with a far-past
     * start can't balloon the fetch unboundedly.
     *
     * The clamp assumes a night is shorter than floor-to-baseStart (one look-back, ~24h): SleepAnalysis
     * splits on any wake gap > 1h, so a real session never spans that long. A pathological > 24h
     * session (e.g. a sensor misclassifying continuous wake as sleep) could clamp short of its true
     * start and still write clipped stages — acceptable, as that input is already malformed.
     */
    internal fun sleepQueryStart(
        rows: List<SleepSessionRow>,
        baseStart: Instant,
        end: Instant,
        floor: Instant
    ): Instant {
        val earliestOverlappingStart = rows
            .filter { !it.endTime.isBefore(baseStart) && !it.startTime.isAfter(end) }
            .minOfOrNull { it.startTime }
        val extended = if (earliestOverlappingStart != null && earliestOverlappingStart.isBefore(baseStart)) {
            earliestOverlappingStart
        } else {
            baseStart
        }
        return if (extended.isBefore(floor)) floor else extended
    }

    suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneId,
        grantedPermissions: Set<String>,
        deviceSamples: List<ActivitySample>,
        context: Context,
        existingRows: List<SleepSessionRow>
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

        // No slice-ownership filter: the frozen clientRecordId dedups, so look-back re-discovery
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
            mintId = mintId
        )

        // Version = wall clock so a later run outranks its own earlier write (HC keeps the highest),
        // letting a grown session overwrite the partial it replaces.
        val version = Instant.now().epochSecond

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

        // Plain forward cursor: latest end synced this slice, null to hold when nothing synced.
        // Safe to advance past an open night: it re-enters the 24h look-back next run and
        // overlap-matches its frozen id.
        val cursor = plan.planned.maxOfOrNull { it.end }

        val stats = SyncerStatistics(
            recordsSynced = records.size,
            recordsSkipped = skippedCount,
            recordType = "Sleep",
            latestRecordTimestamp = cursor
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
    internal fun buildSleepStages(
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
                // This is the last stage of this session. End time is one second after the last
                // sample. Must land on a whole second: the per-night registry persists the span as
                // epochSeconds, so a sub-second end (e.g. +1ms) reloads truncated and makes an
                // otherwise-unchanged re-detection compare unequal -> the skip-unchanged guard in
                // planSleepSessions is defeated and the night is needlessly rewritten every sync.
                val lastSampleTimestamp = samplesForThisSession.last().timestamp.toLong()
                stageEndTime = Instant.ofEpochSecond(lastSampleTimestamp).plusSeconds(1)
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
