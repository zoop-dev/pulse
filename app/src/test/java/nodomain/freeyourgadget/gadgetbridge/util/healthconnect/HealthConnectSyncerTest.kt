package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import org.junit.Assert.*
import org.junit.Test

class HealthConnectSyncerTest {

    @Test
    fun testSyncerStatistics_defaultInitialization() {
        val stats = SyncerStatistics()

        assertEquals(0, stats.recordsSynced)
        assertEquals(0, stats.recordsSkipped)
        assertEquals("", stats.recordType)
    }

    @Test
    fun testSyncerStatistics_withRecordsSynced() {
        val stats = SyncerStatistics(recordsSynced = 5, recordType = "Steps")

        assertEquals(5, stats.recordsSynced)
        assertEquals(0, stats.recordsSkipped)
        assertEquals("Steps", stats.recordType)
    }

    @Test
    fun testSyncerStatistics_withRecordsSkipped() {
        val stats = SyncerStatistics(recordsSkipped = 3, recordType = "HeartRate")

        assertEquals(0, stats.recordsSynced)
        assertEquals(3, stats.recordsSkipped)
        assertEquals("HeartRate", stats.recordType)
    }

    @Test
    fun testSyncerStatistics_withBothSyncedAndSkipped() {
        val stats = SyncerStatistics(
            recordsSynced = 10,
            recordsSkipped = 5,
            recordType = "Sleep"
        )

        assertEquals(10, stats.recordsSynced)
        assertEquals(5, stats.recordsSkipped)
        assertEquals("Sleep", stats.recordType)
    }

    @Test
    fun testSyncerStatistics_withOnlyRecordType() {
        val stats = SyncerStatistics(recordType = "Weight")

        assertEquals(0, stats.recordsSynced)
        assertEquals(0, stats.recordsSkipped)
        assertEquals("Weight", stats.recordType)
    }

    @Test
    fun testSyncerStatistics_dataClassEquality() {
        val stats1 = SyncerStatistics(recordsSynced = 5, recordsSkipped = 2, recordType = "HRV")
        val stats2 = SyncerStatistics(recordsSynced = 5, recordsSkipped = 2, recordType = "HRV")
        val stats3 = SyncerStatistics(recordsSynced = 3, recordsSkipped = 2, recordType = "HRV")

        assertEquals(stats1, stats2)
        assertNotEquals(stats1, stats3)
    }

    @Test
    fun testSyncerStatistics_copy() {
        val original = SyncerStatistics(recordsSynced = 10, recordsSkipped = 5, recordType = "SpO2")
        val copy = original.copy(recordsSynced = 15)

        assertEquals(15, copy.recordsSynced)
        assertEquals(5, copy.recordsSkipped)
        assertEquals("SpO2", copy.recordType)

        // Original should be unchanged
        assertEquals(10, original.recordsSynced)
    }

    @Test
    fun testSyncerStatistics_toString() {
        val stats = SyncerStatistics(recordsSynced = 7, recordsSkipped = 3, recordType = "Temperature")
        val stringRepresentation = stats.toString()

        assertTrue(stringRepresentation.contains("recordsSynced=7"))
        assertTrue(stringRepresentation.contains("recordsSkipped=3"))
        assertTrue(stringRepresentation.contains("recordType=Temperature"))
    }

    @Test
    fun testSyncerStatistics_totalRecords() {
        val stats = SyncerStatistics(recordsSynced = 100, recordsSkipped = 25, recordType = "Workout")
        val total = stats.recordsSynced + stats.recordsSkipped

        assertEquals(125, total)
    }

    @Test
    fun testSyncerStatistics_hasWork() {
        val noWork = SyncerStatistics(recordType = "Test")
        val hasWork = SyncerStatistics(recordsSynced = 1, recordType = "Test")
        val hasSkipped = SyncerStatistics(recordsSkipped = 1, recordType = "Test")

        assertEquals(0, noWork.recordsSynced + noWork.recordsSkipped)
        assertTrue((hasWork.recordsSynced + hasWork.recordsSkipped) > 0)
        assertTrue((hasSkipped.recordsSynced + hasSkipped.recordsSkipped) > 0)
    }
}

