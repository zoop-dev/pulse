package nodomain.freeyourgadget.gadgetbridge.util.healthconnect

import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers.SyncerStatistics
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SyncTimestampAdvancementTest {

    private val baseEpoch = 1_700_000_000L
    private val baseInstant: Instant = Instant.ofEpochSecond(baseEpoch)

    @Test
    fun latestRecordTimestamp_advancesCursor_whenPresent() {
        val stats = listOf(
            SyncerStatistics(recordsSynced = 3, recordType = "Steps", latestRecordTimestamp = baseInstant.plusSeconds(3600))
        )
        val latestRecordTs = stats.mapNotNull { it.latestRecordTimestamp }.maxOrNull()

        assertNotNull(latestRecordTs)
        assertTrue(latestRecordTs!!.isAfter(baseInstant))
    }

    @Test
    fun cursorDoesNotAdvance_whenNoRecordsSynced() {
        val stats = listOf(
            SyncerStatistics(recordsSynced = 0, recordType = "Sleep")
        )
        val latestRecordTs = stats.mapNotNull { it.latestRecordTimestamp }.maxOrNull()

        assertNull(latestRecordTs)
    }

    @Test
    fun multipleStats_usesMaxTimestamp() {
        val earlier = baseInstant.plusSeconds(1000)
        val later = baseInstant.plusSeconds(5000)
        val stats = listOf(
            SyncerStatistics(recordsSynced = 2, recordType = "Steps", latestRecordTimestamp = earlier),
            SyncerStatistics(recordsSynced = 5, recordType = "HeartRate", latestRecordTimestamp = later),
            SyncerStatistics(recordsSynced = 1, recordType = "Calories", latestRecordTimestamp = earlier)
        )
        val latestRecordTs = stats.mapNotNull { it.latestRecordTimestamp }.maxOrNull()

        assertEquals(later, latestRecordTs)
    }

    @Test
    fun mixedNullAndNonNull_usesNonNull() {
        val ts = baseInstant.plusSeconds(7200)
        val stats = listOf(
            SyncerStatistics(recordsSynced = 0, recordType = "Steps"),
            SyncerStatistics(recordsSynced = 1, recordType = "HeartRate", latestRecordTimestamp = ts),
            SyncerStatistics(recordsSynced = 0, recordType = "Calories")
        )
        val latestRecordTs = stats.mapNotNull { it.latestRecordTimestamp }.maxOrNull()

        assertEquals(ts, latestRecordTs)
    }

    @Test
    fun allNullTimestamps_cursorStaysAtCurrentValue() {
        val currentPersisted = baseInstant
        val stats = listOf(
            SyncerStatistics(recordsSynced = 0, recordType = "Sleep"),
            SyncerStatistics(recordsSynced = 0, recordType = "Workout")
        )
        val latestRecordTs = stats.mapNotNull { it.latestRecordTimestamp }.maxOrNull()
        val newPersisted = if (latestRecordTs != null && latestRecordTs.isAfter(currentPersisted)) {
            latestRecordTs
        } else {
            currentPersisted
        }

        assertEquals(currentPersisted, newPersisted)
    }

    @Test
    fun cursorDoesNotGoBackward_whenLatestIsBeforeCurrent() {
        val currentPersisted = baseInstant.plusSeconds(10000)
        val olderTs = baseInstant.plusSeconds(5000)
        val stats = listOf(
            SyncerStatistics(recordsSynced = 1, recordType = "Steps", latestRecordTimestamp = olderTs)
        )
        val latestRecordTs = stats.mapNotNull { it.latestRecordTimestamp }.maxOrNull()
        val newPersisted = if (latestRecordTs != null && latestRecordTs.isAfter(currentPersisted)) {
            latestRecordTs
        } else {
            currentPersisted
        }

        assertEquals(currentPersisted, newPersisted)
    }

    @Test
    fun emptyStatsList_cursorStays() {
        val currentPersisted = baseInstant
        val stats = emptyList<SyncerStatistics>()
        val latestRecordTs = stats.mapNotNull { it.latestRecordTimestamp }.maxOrNull()
        val newPersisted = if (latestRecordTs != null && latestRecordTs.isAfter(currentPersisted)) {
            latestRecordTs
        } else {
            currentPersisted
        }

        assertEquals(currentPersisted, newPersisted)
    }

    @Test
    fun defaultSyncerStatistics_hasNullTimestamp() {
        val stats = SyncerStatistics()
        assertNull(stats.latestRecordTimestamp)
    }

    @Test
    fun syncerStatistics_preservesTimestampInCopy() {
        val ts = Instant.ofEpochSecond(baseEpoch + 999)
        val original = SyncerStatistics(recordsSynced = 1, recordType = "HRV", latestRecordTimestamp = ts)
        val copy = original.copy(recordsSynced = 2)

        assertEquals(ts, copy.latestRecordTimestamp)
        assertEquals(2, copy.recordsSynced)
    }
}

