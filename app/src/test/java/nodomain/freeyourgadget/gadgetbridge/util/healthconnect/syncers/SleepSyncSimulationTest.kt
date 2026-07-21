package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Illustrative model (NOT a production-faithful harness: HC is a map, sync ranges are synthetic) of
 * the sync loop over issue #6453's night timings, to document repair behavior: reset-without-fix is
 * not durable, and the fix repairs a corrupted night iff its registry row is still present (else a
 * reset is needed). The fixed path drives the REAL production cores planSleepSessions /
 * buildSleepStages / sleepQueryStart / pruneSleepRows; only the old +1ms stage-end is reproduced
 * locally (buildStagesLegacy) to model pre-fix behavior.
 */
class SleepSyncSimulationTest {

    private class MockSample(private val ts: Int, private val kind: ActivityKind) : ActivitySample {
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

    private val DAY = 24 * 3600L
    private val LOOKBACK = 24 * 3600L
    private val LOOKFWD = 12 * 3600L

    private data class HcRecord(val stageCount: Int, val version: Long)

    private fun ep(s: String) = Instant.parse(s).epochSecond

    // Exact #6453 nights. n0 is the reported July-18 night; n2 is a short morning session ending
    // ~23h52m after n0 ends, so a later cursor at n2.end makes cursor-24h land inside n0.
    private val n0s = ep("2026-07-17T23:20:00Z"); private val n0e = ep("2026-07-18T07:46:00Z")
    private val n1s = ep("2026-07-18T23:22:00Z"); private val n1e = ep("2026-07-19T03:18:00Z")
    private val n2s = ep("2026-07-19T05:34:00Z"); private val n2e = ep("2026-07-19T07:38:00Z")

    // A night of distinct 20-min stages spanning [start,end], + a trailing wake marker (>1h later)
    // so SleepAnalysis closes the session.
    private fun night(startEpoch: Long, endEpoch: Long): List<MockSample> {
        val kinds = listOf(ActivityKind.LIGHT_SLEEP, ActivityKind.DEEP_SLEEP, ActivityKind.REM_SLEEP)
        val out = mutableListOf<MockSample>()
        var t = startEpoch; var i = 0
        while (t < endEpoch) { out.add(MockSample(t.toInt(), kinds[i % 3])); t += 20 * 60; i++ }
        out.add(MockSample(endEpoch.toInt(), kinds[i % 3]))
        out.add(MockSample((endEpoch + 2 * 3600).toInt(), ActivityKind.ACTIVITY))
        return out
    }

    private val allSamples: List<MockSample> by lazy {
        (night(n0s, n0e) + night(n1s, n1e) + night(n2s, n2e)).sortedBy { it.timestamp }
    }
    private fun idOf(nightStart: Long) = "gb-sleep-${nightStart / 3600 * 3600}"

    // Expected full stage count for n0, from the production builder over n0's own sleep samples.
    private val expectedFullN0: Int by lazy {
        SleepSyncer.buildSleepStages(night(n0s, n0e).filter { it.timestamp <= n0e }, "test").size
    }

    // Reproduces the pre-fix +1ms last-stage end. Only for modeling old behavior; the fixed path
    // uses production SleepSyncer.buildSleepStages so a +1s regression there would fail these tests.
    private fun buildStagesLegacy(samples: List<ActivitySample>): List<Pair<Instant, Instant>> {
        val stages = mutableListOf<Pair<Instant, Instant>>()
        var i = 0
        fun kindOf(s: ActivitySample) = when (s.kind) {
            ActivityKind.DEEP_SLEEP, ActivityKind.LIGHT_SLEEP, ActivityKind.REM_SLEEP, ActivityKind.AWAKE_SLEEP -> s.kind
            else -> null
        }
        while (i < samples.size) {
            val type = kindOf(samples[i]); if (type == null) { i++; continue }
            val start = Instant.ofEpochSecond(samples[i].timestamp.toLong())
            var j = i + 1
            while (j < samples.size && kindOf(samples[j]) == type) j++
            val end = if (j < samples.size) Instant.ofEpochSecond(samples[j].timestamp.toLong())
                      else Instant.ofEpochSecond(samples.last().timestamp.toLong()).plusMillis(1)
            if (end.isAfter(start)) stages.add(start to end)
            i = j
        }
        return stages
    }

    private fun stagesFor(samples: List<ActivitySample>, useFix: Boolean): List<Pair<Instant, Instant>> =
        if (useFix) SleepSyncer.buildSleepStages(samples, "test").map { it.startTime to it.endTime }
        else buildStagesLegacy(samples)

    // One sync run over [rangeStart, rangeEnd). persistedRows + hcStore persist across runs. Cursor
    // starts at rangeStart and only advances via planned record ends (as production does); prune is
    // relative to that cursor. Returns the advanced cursor -> next run's rangeStart.
    private fun runSync(
        rangeStart: Instant, rangeEnd: Instant,
        persistedRows: MutableList<SleepSessionRow>, hcStore: MutableMap<String, HcRecord>,
        useFix: Boolean, version: Long
    ): Instant {
        var rows: List<SleepSessionRow> = persistedRows.toList()
        val mintId: (Instant) -> String = { s -> "gb-sleep-${s.epochSecond / 3600 * 3600}" }
        var sliceStart = rangeStart
        var cursor = rangeStart
        while (sliceStart.isBefore(rangeEnd)) {
            val sliceEnd = minOf(sliceStart.plusSeconds(DAY), rangeEnd)
            val baseStart = sliceStart.minusSeconds(LOOKBACK)
            val queryEnd = sliceEnd.plusSeconds(LOOKFWD)
            val queryStart = if (useFix)
                SleepSyncer.sleepQueryStart(rows, baseStart, queryEnd, baseStart.minusSeconds(LOOKBACK))
            else baseStart

            val fetched = allSamples.filter { it.timestamp >= queryStart.epochSecond && it.timestamp <= queryEnd.epochSecond }
            val detected = mutableListOf<DetectedSleepSession>(); val stageCounts = mutableListOf<Int>()
            for (s in SleepAnalysis().calculateSleepSessions(fetched)) {
                val forSession = fetched.filter { it.timestamp >= s.sleepStart.time / 1000L && it.timestamp <= s.sleepEnd.time / 1000L }
                val stages = stagesFor(forSession, useFix)
                if (stages.isEmpty()) continue
                detected.add(DetectedSleepSession(stages.first().first, stages.last().second)); stageCounts.add(stages.size)
            }
            val plan = SleepSyncer.planSleepSessions(rows, detected, mintId)
            rows = plan.rows
            for (p in plan.planned) {
                val existing = hcStore[p.clientRecordId]
                if (existing == null || version >= existing.version)
                    hcStore[p.clientRecordId] = HcRecord(stageCounts[p.detectedIndex], version)
            }
            plan.planned.maxOfOrNull { it.end }?.let { e -> if (e.isAfter(cursor)) cursor = e }
            sliceStart = sliceEnd
        }
        val pruneBefore = cursor.minusSeconds(LOOKBACK)
        persistedRows.clear()
        persistedRows.addAll(SleepSyncer.pruneSleepRows(rows, pruneBefore)
            .map { SleepSessionRow(it.clientRecordId, Instant.ofEpochSecond(it.startTime.epochSecond), Instant.ofEpochSecond(it.endTime.epochSecond)) })
        return cursor
    }

    // Repro #6453: run 1 syncs n0 full; run 2 (cursor at n2 end) re-detects the clipped tail and,
    // with the +1ms end, overwrites n0 with fewer stages.
    @Test
    fun repro_laterRunTruncatesN0_withoutFix() {
        val rows = mutableListOf<SleepSessionRow>(); val hc = mutableMapOf<String, HcRecord>()
        runSync(Instant.ofEpochSecond(n0s - 3600), Instant.ofEpochSecond(n0e + 3 * 3600), rows, hc, useFix = false, version = 1)
        assertTrue("full night has multiple stages", hc[idOf(n0s)]!!.stageCount > 1)
        runSync(Instant.ofEpochSecond(n2e), Instant.ofEpochSecond(n2e + DAY), rows, hc, useFix = false, version = 2)
        assertTrue("bug reproduced: n0 truncated below full", hc[idOf(n0s)]!!.stageCount < expectedFullN0)
    }

    // Same two runs with the fix: n0 stays at full stage count.
    @Test
    fun control_laterRunWithFix_keepsN0Full() {
        val rows = mutableListOf<SleepSessionRow>(); val hc = mutableMapOf<String, HcRecord>()
        runSync(Instant.ofEpochSecond(n0s - 3600), Instant.ofEpochSecond(n0e + 3 * 3600), rows, hc, useFix = true, version = 1)
        assertEquals("initial sync writes full n0", expectedFullN0, hc[idOf(n0s)]!!.stageCount)
        runSync(Instant.ofEpochSecond(n2e), Instant.ofEpochSecond(n2e + DAY), rows, hc, useFix = true, version = 2)
        assertEquals("fix keeps n0 full", expectedFullN0, hc[idOf(n0s)]!!.stageCount)
    }

    // Reset without the fix is not durable: replay repairs n0, the next incremental run re-truncates it.
    @Test
    fun resetWithoutFix_repairsThenReCorrupts() {
        val rows = mutableListOf<SleepSessionRow>(); val hc = mutableMapOf<String, HcRecord>()
        val cursor = runSync(Instant.ofEpochSecond(n0s - 3600), Instant.ofEpochSecond(n2e + 3600), rows, hc, useFix = false, version = 10)
        assertEquals("reset replay repairs n0 to full", expectedFullN0, hc[idOf(n0s)]!!.stageCount)
        runSync(cursor, cursor.plusSeconds(DAY), rows, hc, useFix = false, version = 11)
        assertTrue("next incremental re-truncates without the fix", hc[idOf(n0s)]!!.stageCount < expectedFullN0)
    }

    // Reset + fix: repaired to full and stays full across the next incremental run.
    @Test
    fun resetWithFix_repairsAndStays() {
        val rows = mutableListOf<SleepSessionRow>(); val hc = mutableMapOf<String, HcRecord>()
        val cursor = runSync(Instant.ofEpochSecond(n0s - 3600), Instant.ofEpochSecond(n2e + 3600), rows, hc, useFix = true, version = 10)
        assertEquals("reset replay repairs n0 to full", expectedFullN0, hc[idOf(n0s)]!!.stageCount)
        runSync(cursor, cursor.plusSeconds(DAY), rows, hc, useFix = true, version = 11)
        assertEquals("fix keeps n0 full", expectedFullN0, hc[idOf(n0s)]!!.stageCount)
    }

    // Repair depends on the registry ROW, not the cursor. Row present (night ends after the prune
    // horizon): the fix's sleepQueryStart reaches back and a normal incremental sync repairs n0 to
    // full, no reset. Assert the row genuinely survives production pruning at this cursor.
    @Test
    fun fixRepairsCorruptedNight_whileRegistryRowPresent() {
        val hc = mutableMapOf(idOf(n0s) to HcRecord(1, 5))
        val row = SleepSessionRow(idOf(n0s), Instant.ofEpochSecond(n0s), Instant.ofEpochSecond(n0e))
        // Cursor at n2 end (07-19 07:38); prune horizon 07-18 07:38 < n0 end 07-18 07:46 -> row kept.
        val pruneBefore = Instant.ofEpochSecond(n2e).minusSeconds(LOOKBACK)
        assertEquals("row survives pruning at this cursor", 1, SleepSyncer.pruneSleepRows(listOf(row), pruneBefore).size)

        val rows = mutableListOf(row)
        runSync(Instant.ofEpochSecond(n2e), Instant.ofEpochSecond(n2e + DAY), rows, hc, useFix = true, version = 30)
        assertEquals("row retained -> incremental sync repairs n0 to full", expectedFullN0, hc[idOf(n0s)]!!.stageCount)
    }

    // Once the row IS pruned (night >24h behind cursor), the fix can't reach it on a normal sync ->
    // reset needed. Demonstrate the prune (not just seed empty), then run the incremental sync.
    @Test
    fun afterRowPruned_incrementalCannotRepair_resetRequired() {
        val hc = mutableMapOf(idOf(n0s) to HcRecord(1, 5))
        val row = SleepSessionRow(idOf(n0s), Instant.ofEpochSecond(n0s), Instant.ofEpochSecond(n0e))
        val cursor = n0e + 2 * DAY
        val pruneBefore = Instant.ofEpochSecond(cursor).minusSeconds(LOOKBACK)
        val rows = SleepSyncer.pruneSleepRows(listOf(row), pruneBefore).toMutableList()
        assertEquals("row is pruned at this cursor", 0, rows.size)

        runSync(Instant.ofEpochSecond(cursor), Instant.ofEpochSecond(cursor + DAY), rows, hc, useFix = true, version = 30)
        assertEquals("row pruned -> incremental sync cannot repair n0", 1, hc[idOf(n0s)]!!.stageCount)
    }

    // Reset (replay from before the night) repairs a pruned-row corrupted night to full.
    @Test
    fun fixWithReset_repairsPrunedNight() {
        val hc = mutableMapOf(idOf(n0s) to HcRecord(1, 5))
        val rows = mutableListOf<SleepSessionRow>()
        runSync(Instant.ofEpochSecond(n0s - 3600), Instant.ofEpochSecond(n2e + 3600), rows, hc, useFix = true, version = 20)
        assertEquals("reset + fix repairs n0 to full", expectedFullN0, hc[idOf(n0s)]!!.stageCount)
    }
}
