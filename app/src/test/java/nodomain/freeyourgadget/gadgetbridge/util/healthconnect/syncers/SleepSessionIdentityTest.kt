package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

// Guards the stateful sleep-session identity contract that fixes issue #6297: SleepAnalysis is
// stateless and re-segments a night's start earlier as samples arrive. planSleepSessions must pin a
// clientRecordId on first sight and reuse it for any later overlapping detection, so the single HC
// record grows in place instead of orphaning the night. Only new-or-grown sessions are planned
// (written to HC); an unchanged re-detection keeps its row but is skipped. These tests exercise the
// pure decision core directly — no HealthConnectClient or DB mock needed.
class SleepSessionIdentityTest {

    // Deterministic id minter mirroring SleepSyncer's hour-bucketed scheme (without device parts).
    private val mintId: (Instant) -> String = { start ->
        "id-${start.epochSecond / 3600 * 3600}"
    }

    private fun det(start: String, end: String) = DetectedSleepSession(Instant.parse(start), Instant.parse(end))

    private fun plan(existingRows: List<SleepSessionRow>, vararg detected: DetectedSleepSession) =
        SleepSyncer.planSleepSessions(existingRows, detected.toList(), mintId)

    @Test
    fun freshSession_mintsId_plansRecord() {
        val result = plan(emptyList(), det("2026-06-15T07:30:00Z", "2026-06-15T07:30:01Z"))

        assertEquals(1, result.planned.size)
        assertEquals(1, result.rows.size)
        val row = result.rows[0]
        assertEquals(mintId(Instant.parse("2026-06-15T07:30:00Z")), row.clientRecordId)
        assertEquals(row.clientRecordId, result.planned[0].clientRecordId)
    }

    // ★ Regression guard for #6297: the night re-segments earlier across passes, must keep its id.
    @Test
    fun backwardGrowth_reusesFrozenId_growsRecord() {
        // Pass A: a single-sample fragment is all SleepAnalysis can see yet.
        val passA = plan(emptyList(), det("2026-06-15T01:02:00Z", "2026-06-15T01:02:01Z"))
        val frozenId = passA.rows[0].clientRecordId

        // Pass B (seconds later): the fragment has grown into the real night starting 23:08.
        val passB = plan(passA.rows, det("2026-06-14T23:08:00Z", "2026-06-15T07:25:00Z"))

        assertEquals("one record, not a new orphan", 1, passB.rows.size)
        assertEquals("id frozen from first sight", frozenId, passB.rows[0].clientRecordId)
        assertEquals(Instant.parse("2026-06-14T23:08:00Z"), passB.rows[0].startTime)
        assertEquals(Instant.parse("2026-06-15T07:25:00Z"), passB.rows[0].endTime)
        assertEquals("grown session is re-planned", 1, passB.planned.size)
        assertEquals(frozenId, passB.planned[0].clientRecordId)
    }

    @Test
    fun freshSession_advancesCursorToEnd() {
        val end = "2026-06-15T01:00:00Z"
        val result = plan(emptyList(), det("2026-06-14T23:08:00Z", end))
        // Cursor derivation lives in sync(); here we assert the planned span the cursor is taken from.
        assertEquals(Instant.parse(end), result.planned.maxOf { it.end })
    }

    @Test
    fun fragmentOnStartEdge_matchesGrownNight_noOrphan() {
        // First detection is a single-sample fragment sitting exactly on the night's eventual start
        // edge (23:08Z). When the full night 23:08Z->07:25Z is detected next, it must reuse the
        // frozen id, not spawn a second record. (Inclusive overlap; strict would re-orphan here.)
        val frag = plan(emptyList(), det("2026-06-14T23:08:00Z", "2026-06-14T23:08:01Z"))
        val night = plan(frag.rows, det("2026-06-14T23:08:00Z", "2026-06-15T07:25:00Z"))

        assertEquals("one record, not an orphan", 1, night.rows.size)
        assertEquals(frag.rows[0].clientRecordId, night.rows[0].clientRecordId)
        assertEquals(Instant.parse("2026-06-15T07:25:00Z"), night.rows[0].endTime)
    }

    @Test
    fun gapSeparatedSessions_stayDistinct() {
        // SleepAnalysis only splits across a wake gap > 1h, so distinct sessions never touch.
        // A nap and a later main sleep with a clear gap must remain two records.
        val first = plan(emptyList(), det("2026-06-14T13:00:00Z", "2026-06-14T14:00:00Z"))
        val second = plan(first.rows, det("2026-06-14T23:08:00Z", "2026-06-15T07:25:00Z"))

        assertEquals("gap-separated sessions must not merge", 2, second.rows.size)
        assertEquals(2, second.rows.map { it.clientRecordId }.toSet().size)
    }

    @Test
    fun multiSessionNight_napAndMainSleep_distinctIds() {
        val result = plan(
            emptyList(),
            det("2026-06-14T13:00:00Z", "2026-06-14T13:30:00Z"), // afternoon nap
            det("2026-06-14T23:00:00Z", "2026-06-15T07:00:00Z")  // main sleep
        )
        assertEquals(2, result.rows.size)
        assertEquals(2, result.rows.map { it.clientRecordId }.toSet().size)
        assertEquals(2, result.planned.size)
    }

    // ★ Skip-if-unchanged: the 24h look-back re-scans an already-synced night every run; an unchanged
    // re-detection must keep its row but not be re-written to HC.
    @Test
    fun unchangedSession_keepsRow_notReplanned() {
        val first = plan(emptyList(), det("2026-06-14T23:00:00Z", "2026-06-15T07:00:00Z"))
        val second = plan(first.rows, det("2026-06-14T23:00:00Z", "2026-06-15T07:00:00Z"))

        assertEquals(1, second.rows.size)
        assertEquals(first.rows[0].clientRecordId, second.rows[0].clientRecordId)
        assertTrue("unchanged span must not be re-planned", second.planned.isEmpty())
    }

    @Test
    fun reDetectedWithinLookback_noNewId() {
        val first = plan(emptyList(), det("2026-06-14T22:00:00Z", "2026-06-15T00:00:00Z"))
        val again = plan(first.rows, det("2026-06-14T22:00:00Z", "2026-06-15T00:00:00Z"))

        assertEquals(1, again.rows.size)
        assertEquals(first.rows[0].clientRecordId, again.rows[0].clientRecordId)
    }

    @Test
    fun prune_dropsRowsBeforeHorizon() {
        val old = SleepSessionRow("old", Instant.parse("2026-06-10T00:00:00Z"), Instant.parse("2026-06-10T06:00:00Z"))
        val recent = SleepSessionRow("recent", Instant.parse("2026-06-14T23:00:00Z"), Instant.parse("2026-06-15T06:00:00Z"))
        val pruneBefore = Instant.parse("2026-06-14T00:00:00Z")

        val keptIds = SleepSyncer.pruneSleepRows(listOf(old, recent), pruneBefore)
            .map { it.clientRecordId }.toSet()

        assertFalse("old row pruned", keptIds.contains("old"))
        assertTrue("recent row kept", keptIds.contains("recent"))
    }

    // ── Issue #6453: full-span session written with truncated stages ──────────────────────────────

    private class MockSample(
        private val ts: Int,
        private val kind: ActivityKind
    ) : ActivitySample {
        override fun getTimestamp() = ts
        override fun getKind() = kind
        override fun getSteps() = 0
        override fun getProvider(): SampleProvider<*>? = null
        override fun getRawKind() = kind.code
        override fun getRawIntensity() = ActivitySample.NOT_MEASURED
        override fun getIntensity() = 0f
        override fun getDistanceCm() = ActivitySample.NOT_MEASURED
        override fun getActiveCalories() = ActivitySample.NOT_MEASURED
        override fun getHeartRate() = ActivitySample.NOT_MEASURED
        override fun setHeartRate(value: Int) {}
    }

    private fun sleep(ts: Int, kind: ActivityKind) = MockSample(ts, kind)

    // Round-trip a candidate span through the GreenDAO registry the way sync() does: persist stores
    // Instant.epochSecond, load rebuilds via Instant.ofEpochSecond. If buildSleepStages emits a
    // sub-second last-stage end, the reload truncates it and the span no longer round-trips.
    private fun persistLoad(end: Instant) = Instant.ofEpochSecond(end.epochSecond)

    // ★ Fix A: the last stage must end on a whole second so the persisted span reloads unchanged.
    // Before the fix the last stage ended at +1ms, which epochSecond truncation dropped, so a stored
    // row (whole second) never equalled the re-detected end -> changed=true -> needless rewrite.
    @Test
    fun buildSleepStages_lastStageEnd_survivesEpochSecondRoundTrip() {
        val base = 1_700_000_000
        val stages = SleepSyncer.buildSleepStages(
            listOf(
                sleep(base, ActivityKind.LIGHT_SLEEP),
                sleep(base + 60, ActivityKind.DEEP_SLEEP),
                sleep(base + 120, ActivityKind.LIGHT_SLEEP)
            ),
            "test-device"
        )
        val lastEnd = stages.last().endTime
        assertEquals("last stage end must be a whole second (no sub-second remainder)",
            lastEnd, persistLoad(lastEnd))
    }

    // ★ Fix A: an unchanged re-detection must not be re-planned after the stored row's end has been
    // through the epochSecond round-trip. Guards the round-trip-equality property the +1ms end broke.
    @Test
    fun reDetectedNight_afterEpochSecondRoundTrip_notReplanned() {
        val base = 1_700_000_000
        val samples = listOf(
            sleep(base, ActivityKind.LIGHT_SLEEP),
            sleep(base + 60, ActivityKind.DEEP_SLEEP),
            sleep(base + 120, ActivityKind.LIGHT_SLEEP)
        )
        val stages = SleepSyncer.buildSleepStages(samples, "test-device")
        val detStart = stages.first().startTime
        val detEnd = stages.last().endTime

        // Stored row as it comes back from the DB (epochSecond precision).
        val stored = SleepSessionRow("frozen-id", persistLoad(detStart), persistLoad(detEnd))

        val result = SleepSyncer.planSleepSessions(
            listOf(stored),
            listOf(DetectedSleepSession(detStart, detEnd)),
            mintId
        )
        assertTrue("unchanged re-detection must not be rewritten to HC", result.planned.isEmpty())
    }

    // ★ The exact #6453 trigger, end-to-end: a full night was written, then a later sync fetched only
    // the night's TAIL (its start predates the look-back window). The clipped re-detection has fewer
    // stages; with the +1ms end its end exceeded the epochSecond-truncated stored end, so planSleep-
    // Sessions saw changed=true and rewrote the record with the truncated stage list. With Fix A the
    // tail's end equals the stored end (whole second, end not grown, start not earlier) -> changed=
    // false -> the good multi-stage record is left intact. This is the test that would have caught it.
    @Test
    fun clippedTailReDetection_doesNotOverwriteFullNight() {
        val base = 1_700_000_000
        val fullNight = listOf(
            sleep(base, ActivityKind.LIGHT_SLEEP),
            sleep(base + 60, ActivityKind.DEEP_SLEEP),
            sleep(base + 120, ActivityKind.REM_SLEEP),
            sleep(base + 180, ActivityKind.LIGHT_SLEEP)
        )
        val fullStages = SleepSyncer.buildSleepStages(fullNight, "test-device")
        assertTrue("full night must yield multiple stages", fullStages.size > 1)

        // Stored row = the full night as persisted (epochSecond precision).
        val stored = SleepSessionRow(
            "frozen-id",
            persistLoad(fullStages.first().startTime),
            persistLoad(fullStages.last().endTime)
        )

        // Later sync fetches only the tail (window starts after the night's real start).
        val tail = fullNight.takeLast(2)
        val tailStages = SleepSyncer.buildSleepStages(tail, "test-device")
        assertTrue("tail is genuinely fewer stages", tailStages.size < fullStages.size)

        val result = SleepSyncer.planSleepSessions(
            listOf(stored),
            listOf(DetectedSleepSession(tailStages.first().startTime, tailStages.last().endTime)),
            mintId
        )
        assertTrue(
            "clipped tail must not overwrite the stored full night (would truncate stages)",
            result.planned.isEmpty()
        )
    }

    // ── Fix B: fetch-window lower bound covers overlapping stored nights ──────────────────────────

    private val hour = 3600L
    private fun t(s: String) = Instant.parse(s)

    // ★ Fix B: a stored night beginning before the plain look-back must pull the fetch start back to
    // its start, so SleepAnalysis re-derives the full stage list instead of a clipped tail (#6453).
    // Timeline mirrors issue #6453: cursor at 07-19T07:38 -> baseStart = cursor-24h = 07-18T07:38,
    // floor = baseStart-24h = 07-17T07:38. The lost night ran 07-17T23:20 -> 07-18T07:46.
    @Test
    fun sleepQueryStart_extendsBackToOverlappingRowStart() {
        val baseStart = t("2026-07-18T07:38:00Z")
        val end = t("2026-07-19T22:00:00Z")
        val floor = baseStart.minusSeconds(24 * hour)
        val row = SleepSessionRow("id", t("2026-07-17T23:20:00Z"), t("2026-07-18T07:46:00Z"))

        val start = SleepSyncer.sleepQueryStart(listOf(row), baseStart, end, floor)
        assertEquals(row.startTime, start)
    }

    @Test
    fun sleepQueryStart_noOverlap_keepsBaseStart() {
        val baseStart = t("2026-07-18T07:38:00Z")
        val end = t("2026-07-19T22:00:00Z")
        val floor = baseStart.minusSeconds(24 * hour)
        // Row ends before the window -> not overlapping.
        val row = SleepSessionRow("id", t("2026-07-10T00:00:00Z"), t("2026-07-10T06:00:00Z"))

        val start = SleepSyncer.sleepQueryStart(listOf(row), baseStart, end, floor)
        assertEquals(baseStart, start)
    }

    @Test
    fun sleepQueryStart_rowStartAfterBase_keepsBaseStart() {
        val baseStart = t("2026-07-18T07:38:00Z")
        val end = t("2026-07-19T22:00:00Z")
        val floor = baseStart.minusSeconds(24 * hour)
        // Overlaps the window but starts after baseStart -> no extension needed.
        val row = SleepSessionRow("id", t("2026-07-18T23:00:00Z"), t("2026-07-19T06:00:00Z"))

        val start = SleepSyncer.sleepQueryStart(listOf(row), baseStart, end, floor)
        assertEquals(baseStart, start)
    }

    @Test
    fun sleepQueryStart_clampsToFloor() {
        val baseStart = t("2026-07-18T07:38:00Z")
        val end = t("2026-07-19T22:00:00Z")
        val floor = baseStart.minusSeconds(24 * hour)
        // Stale row starting before the floor must not balloon the fetch past the floor.
        val row = SleepSessionRow("id", t("2026-01-01T00:00:00Z"), t("2026-07-18T08:00:00Z"))

        val start = SleepSyncer.sleepQueryStart(listOf(row), baseStart, end, floor)
        assertEquals(floor, start)
    }

    @Test
    fun sleepQueryStart_picksEarliestOfMultipleOverlaps() {
        val baseStart = t("2026-07-18T07:38:00Z")
        val end = t("2026-07-19T22:00:00Z")
        val floor = baseStart.minusSeconds(24 * hour)
        val rowA = SleepSessionRow("a", t("2026-07-17T23:00:00Z"), t("2026-07-18T07:45:00Z"))
        val rowB = SleepSessionRow("b", t("2026-07-17T22:00:00Z"), t("2026-07-18T07:40:00Z"))

        val start = SleepSyncer.sleepQueryStart(listOf(rowA, rowB), baseStart, end, floor)
        assertEquals(rowB.startTime, start)
    }

    @Test
    fun sleepQueryStart_noRows_keepsBaseStart() {
        val baseStart = t("2026-07-18T07:38:00Z")
        val end = t("2026-07-19T22:00:00Z")
        val floor = baseStart.minusSeconds(24 * hour)

        assertEquals(baseStart, SleepSyncer.sleepQueryStart(emptyList(), baseStart, end, floor))
    }

    @Test
    fun sleepQueryStart_rowEndsExactlyAtBaseStart_stillOverlaps() {
        val baseStart = t("2026-07-18T07:38:00Z")
        val end = t("2026-07-19T22:00:00Z")
        val floor = baseStart.minusSeconds(24 * hour)
        // Row ends exactly at baseStart: the predicate is inclusive (!endTime.isBefore(baseStart)),
        // so it counts as overlapping and its earlier start extends the window.
        val row = SleepSessionRow("id", t("2026-07-18T00:00:00Z"), baseStart)

        assertEquals(row.startTime, SleepSyncer.sleepQueryStart(listOf(row), baseStart, end, floor))
    }

    // Mirror the real sync path buildCandidate/sync take: fetch filters device samples to the query
    // window, SleepAnalysis re-derives sessions from that window, then each session's stages are
    // rebuilt from the samples inside its detected [sleepStart, sleepEnd] span. Returns total stages.
    private fun stagesViaPipeline(allSamples: List<MockSample>, windowStart: Instant, windowEnd: Instant): Int {
        val fetched = allSamples.filter {
            it.timestamp >= windowStart.epochSecond && it.timestamp <= windowEnd.epochSecond
        }
        return SleepAnalysis().calculateSleepSessions(fetched).sumOf { session ->
            val forSession = fetched.filter {
                it.timestamp >= session.sleepStart.time / 1000L && it.timestamp <= session.sleepEnd.time / 1000L
            }
            SleepSyncer.buildSleepStages(forSession, "test-device").size
        }
    }

    // ★ Fix B, load-bearing wiring: proves the whole reason sleepQueryStart exists — that widening
    // the fetch back to the stored row's start makes the REAL SleepAnalysis re-derive the full night
    // (full stage list) instead of a clipped tail. Drives SleepAnalysis itself, not just
    // buildSleepStages, so a future boundary/merge change in SleepAnalysis can't silently pass.
    @Test
    fun widerFetch_reDerivesFullNight_viaSleepAnalysis() {
        // Night of distinct 5-min stages (SleepAnalysis needs span & duration > MIN_SESSION_LENGTH).
        val nightStart = t("2026-07-17T23:20:00Z").epochSecond.toInt()
        val step = 5 * 60
        val samples = listOf(
            sleep(nightStart, ActivityKind.LIGHT_SLEEP),
            sleep(nightStart + step, ActivityKind.DEEP_SLEEP),
            sleep(nightStart + 2 * step, ActivityKind.REM_SLEEP),
            sleep(nightStart + 3 * step, ActivityKind.LIGHT_SLEEP),
            sleep(nightStart + 4 * step, ActivityKind.DEEP_SLEEP)
        )
        val nightEnd = t("2026-07-17T23:20:00Z").plusSeconds(4L * step)
        val row = SleepSessionRow("id", t("2026-07-17T23:20:00Z"), nightEnd)
        val end = t("2026-07-18T22:00:00Z")
        // Plain look-back lands mid-night, clipping the head; the stored row pulls it back to nightStart.
        val baseStart = t("2026-07-17T23:20:00Z").plusSeconds(2L * step)
        val floor = baseStart.minusSeconds(24 * hour)

        val clipped = stagesViaPipeline(samples, baseStart, end)
        val widened = stagesViaPipeline(samples, SleepSyncer.sleepQueryStart(listOf(row), baseStart, end, floor), end)

        assertTrue("clipped fetch drops head stages ($clipped)", clipped < widened)
        assertEquals("widened fetch recovers every stage", 5, widened)
    }
}
