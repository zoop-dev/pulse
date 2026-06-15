package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

// Guards the stateful sleep-session identity contract that fixes issue #6297: SleepAnalysis is
// stateless and re-segments a night's start earlier as samples arrive. planSleepSessions must pin a
// clientRecordId on first sight and reuse it for any later overlapping detection, and must derive
// the cursor only from completed sessions so an open (still-growing) night is never orphaned by a
// forward-only cursor. These tests exercise the pure decision core directly — no HealthConnectClient
// or DB mock needed, mirroring SyncTimestampAdvancementTest / TemperatureSyncerCursorTest.
class SleepSessionIdentityTest {

    private val now: Instant = Instant.parse("2026-06-15T08:00:00Z")
    private val threshold = 6L

    // Deterministic id minter mirroring SleepSyncer's hour-bucketed scheme (without device parts).
    private val mintId: (Instant) -> String = { start ->
        "id-${start.epochSecond / 3600 * 3600}"
    }

    private fun det(start: String, end: String) = DetectedSleepSession(Instant.parse(start), Instant.parse(end))

    @Test
    fun freshSession_mintsId_insertsOpenRow() {
        // A fresh fragment ending within 6h of now -> still in progress.
        val plan = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(det("2026-06-15T07:30:00Z", "2026-06-15T07:30:01Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )

        assertEquals(1, plan.planned.size)
        assertEquals(1, plan.rows.size)
        val row = plan.rows[0]
        assertEquals(mintId(Instant.parse("2026-06-15T07:30:00Z")), row.clientRecordId)
        assertFalse("ends < 6h before now -> still open", row.finalized)
        // No completed session this pass -> cursor holds (null).
        assertNull(plan.cursor)
    }

    // ★ Regression guard for #6297: the night re-segments earlier across passes, must keep its id.
    @Test
    fun backwardGrowth_reusesFrozenId_growsRecord() {
        // Pass A: a single-sample fragment is all SleepAnalysis can see yet.
        val passA = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(det("2026-06-15T01:02:00Z", "2026-06-15T01:02:01Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        val frozenId = passA.rows[0].clientRecordId

        // Pass B (seconds later): the fragment has grown into the real night starting 23:08.
        val passB = SleepSyncer.planSleepSessions(
            existingRows = passA.rows,
            detected = listOf(det("2026-06-14T23:08:00Z", "2026-06-15T07:25:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )

        assertEquals("one record, not a new orphan", 1, passB.rows.size)
        assertEquals("id frozen from first sight", frozenId, passB.rows[0].clientRecordId)
        assertEquals(Instant.parse("2026-06-14T23:08:00Z"), passB.rows[0].startTime)
        assertEquals(Instant.parse("2026-06-15T07:25:00Z"), passB.rows[0].endTime)
        assertEquals(1, passB.planned.size)
        assertEquals(frozenId, passB.planned[0].clientRecordId)
    }

    // ★ Regression guard for #6297: while a session is open, the cursor must never pass its start.
    @Test
    fun openSession_holdsCursor_null() {
        // 23:08 -> 07:25, ends 07:25 which is < 6h before now (08:00) -> in progress.
        val plan = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(det("2026-06-14T23:08:00Z", "2026-06-15T07:25:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        assertFalse(plan.rows[0].finalized)
        assertNull("open session must not advance the cursor", plan.cursor)
    }

    @Test
    fun completedSession_finalizes_andAdvancesCursorToEnd() {
        // Ends 01:00, which is >6h before now (08:00) -> completed.
        val end = "2026-06-15T01:00:00Z"
        val plan = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(det("2026-06-14T23:08:00Z", end)),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        assertTrue(plan.rows[0].finalized)
        assertEquals(Instant.parse(end), plan.cursor)
    }

    @Test
    fun mixedCompletedAndOpen_cursorIsCompletedEnd_notOpenStart() {
        val completedEnd = "2026-06-15T00:30:00Z" // >6h before now -> completed
        val plan = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(
                det("2026-06-14T22:00:00Z", completedEnd),          // completed nap
                det("2026-06-15T03:00:00Z", "2026-06-15T07:25:00Z") // open main sleep
            ),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        assertEquals(2, plan.rows.size)
        // Cursor = completed end, strictly before the open session's start (03:00).
        assertEquals(Instant.parse(completedEnd), plan.cursor)
        assertTrue(plan.cursor!!.isBefore(Instant.parse("2026-06-15T03:00:00Z")))
    }

    @Test
    fun fragmentOnStartEdge_matchesGrownNight_noOrphan() {
        // First detection is a single-sample fragment sitting exactly on the night's eventual start
        // edge (23:08Z). When the full night 23:08Z->07:25Z is detected next, it must reuse the
        // frozen id, not spawn a second record. (Inclusive overlap; strict would re-orphan here.)
        val frag = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(det("2026-06-14T23:08:00Z", "2026-06-14T23:08:01Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        val night = SleepSyncer.planSleepSessions(
            existingRows = frag.rows,
            detected = listOf(det("2026-06-14T23:08:00Z", "2026-06-15T07:25:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )

        assertEquals("one record, not an orphan", 1, night.rows.size)
        assertEquals(frag.rows[0].clientRecordId, night.rows[0].clientRecordId)
        assertEquals(Instant.parse("2026-06-15T07:25:00Z"), night.rows[0].endTime)
    }

    @Test
    fun gapSeparatedSessions_stayDistinct() {
        // SleepAnalysis only splits across a wake gap > 1h, so distinct sessions never touch.
        // A nap and a later main sleep with a clear gap must remain two records.
        val first = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(det("2026-06-14T13:00:00Z", "2026-06-14T14:00:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        val second = SleepSyncer.planSleepSessions(
            existingRows = first.rows,
            detected = listOf(det("2026-06-14T23:08:00Z", "2026-06-15T07:25:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )

        assertEquals("gap-separated sessions must not merge", 2, second.rows.size)
        assertEquals(2, second.rows.map { it.clientRecordId }.toSet().size)
    }

    @Test
    fun multiSessionNight_napAndMainSleep_distinctIds() {
        val plan = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(
                det("2026-06-14T13:00:00Z", "2026-06-14T13:30:00Z"), // afternoon nap
                det("2026-06-14T23:00:00Z", "2026-06-15T07:00:00Z")  // main sleep
            ),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        assertEquals(2, plan.rows.size)
        assertEquals(2, plan.rows.map { it.clientRecordId }.toSet().size)
    }

    @Test
    fun intraRunRediscovery_isIdempotent() {
        // Same session seen again (e.g. via look-back overlap in a neighbouring slice) -> one row.
        val first = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(det("2026-06-14T23:00:00Z", "2026-06-15T07:00:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        val second = SleepSyncer.planSleepSessions(
            existingRows = first.rows,
            detected = listOf(det("2026-06-14T23:00:00Z", "2026-06-15T07:00:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        assertEquals(1, second.rows.size)
        assertEquals(first.rows[0].clientRecordId, second.rows[0].clientRecordId)
    }

    @Test
    fun finalizedRow_reDetectedWithinLookback_noNewId() {
        val completed = SleepSyncer.planSleepSessions(
            existingRows = emptyList(),
            detected = listOf(det("2026-06-14T22:00:00Z", "2026-06-15T00:00:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        assertTrue(completed.rows[0].finalized)

        val again = SleepSyncer.planSleepSessions(
            existingRows = completed.rows,
            detected = listOf(det("2026-06-14T22:00:00Z", "2026-06-15T00:00:00Z")),
            now = now, thresholdHours = threshold, mintId = mintId
        )
        assertEquals(1, again.rows.size)
        assertEquals(completed.rows[0].clientRecordId, again.rows[0].clientRecordId)
    }

    @Test
    fun prune_dropsRowsBeforeHorizon_regardlessOfFinalized() {
        val finalizedOld = SleepSessionRow("old", Instant.parse("2026-06-10T00:00:00Z"), Instant.parse("2026-06-10T06:00:00Z"), finalized = true)
        val finalizedRecent = SleepSessionRow("recent", Instant.parse("2026-06-14T23:00:00Z"), Instant.parse("2026-06-15T06:00:00Z"), finalized = true)
        // Stale OPEN row (e.g. its samples were deleted): end is old, so it can never be re-detected
        // and must be pruned too, even though it is not finalized.
        val staleOpen = SleepSessionRow("staleOpen", Instant.parse("2026-06-09T00:00:00Z"), Instant.parse("2026-06-09T01:00:00Z"), finalized = false)
        // Genuinely live open row: recent end -> kept.
        val liveOpen = SleepSessionRow("liveOpen", Instant.parse("2026-06-15T03:00:00Z"), Instant.parse("2026-06-15T07:30:00Z"), finalized = false)
        val pruneBefore = Instant.parse("2026-06-14T00:00:00Z")

        val keptIds = SleepSyncer.pruneSleepRows(
            listOf(finalizedOld, finalizedRecent, staleOpen, liveOpen), pruneBefore
        ).map { it.clientRecordId }.toSet()

        assertFalse("old finalized row pruned", keptIds.contains("old"))
        assertFalse("stale open row pruned too", keptIds.contains("staleOpen"))
        assertTrue("recent finalized row kept", keptIds.contains("recent"))
        assertTrue("live open row kept", keptIds.contains("liveOpen"))
    }
}
