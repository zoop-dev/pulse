package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadRecordResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.runBlocking
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.reflect.KClass

class HeartRateSyncerTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")
    private val gbDevice = GBDevice("00:11:22:33:44:55", "Testie", "Testie Alias", "Test Folder", DeviceType.TEST)
    private val metadata: Metadata = Metadata.unknownRecordingMethod(
        Device(type = Device.TYPE_WATCH, manufacturer = "test", model = "test")
    )
    private val grantedPermissions = setOf(HealthPermission.getWritePermission(HeartRateRecord::class))

    private fun hrSample(ts: Int, bpm: Int): ActivitySample =
        object : ActivitySample {
            override fun getTimestamp(): Int = ts
            override fun getProvider(): nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider<*>? = null
            override fun getRawKind(): Int = ActivityKind.UNKNOWN.code
            override fun getKind(): ActivityKind = ActivityKind.UNKNOWN
            override fun getRawIntensity(): Int = 0
            override fun getIntensity(): Float = 0f
            override fun getSteps(): Int = 0
            override fun getDistanceCm(): Int = 0
            override fun getActiveCalories(): Int = 0
            override fun getHeartRate(): Int = bpm
            override fun setHeartRate(value: Int) {}
        }

    private fun syncBpm(values: List<Int>): List<Long> {
        val baseTs = 1_700_000_000
        val samples = values.mapIndexed { i, bpm -> hrSample(baseTs + i * 60, bpm) }
        val start = Instant.ofEpochSecond(baseTs.toLong())
        val end = Instant.ofEpochSecond(baseTs.toLong() + values.size * 60L)
        val client = CapturingClient()
        runBlocking {
            HeartRateSyncer.sync(client, gbDevice, metadata, zoneId, start, end, grantedPermissions, samples)
        }
        return client.inserted
            .filterIsInstance<HeartRateRecord>()
            .flatMap { it.samples }
            .map { it.beatsPerMinute }
    }

    @Test
    fun sentinel255_isNotSynced() {
        val bpm = syncBpm(listOf(60, 255, 62))
        assertTrue("255 sentinel must not reach Health Connect", 255L !in bpm)
        assertEquals(listOf(60L, 62L), bpm)
    }

    @Test
    fun zero_isNotSynced() {
        val bpm = syncBpm(listOf(60, 0, 62))
        assertEquals(listOf(60L, 62L), bpm)
    }

    @Test
    fun validRange_isSynced() {
        val bpm = syncBpm(listOf(1, 300))
        assertEquals(listOf(1L, 300L), bpm)
    }

    @Test
    fun aboveHcLimit_isNotSynced() {
        val bpm = syncBpm(listOf(60, 301, 62))
        assertEquals(listOf(60L, 62L), bpm)
    }

    private class CapturingClient : HealthConnectClient {
        val inserted = mutableListOf<Record>()

        override suspend fun insertRecords(records: List<Record>): InsertRecordsResponse {
            inserted.addAll(records)
            return InsertRecordsResponse(records.map { "id" })
        }

        override val permissionController: PermissionController get() = throw NotImplementedError()
        override suspend fun updateRecords(records: List<Record>) = throw NotImplementedError()
        override suspend fun deleteRecords(recordType: KClass<out Record>, recordIdsList: List<String>, clientRecordIdsList: List<String>) = throw NotImplementedError()
        override suspend fun deleteRecords(recordType: KClass<out Record>, timeRangeFilter: TimeRangeFilter) = throw NotImplementedError()
        override suspend fun <T : Record> readRecord(recordType: KClass<T>, recordId: String): ReadRecordResponse<T> = throw NotImplementedError()
        override suspend fun <T : Record> readRecords(request: ReadRecordsRequest<T>): ReadRecordsResponse<T> = throw NotImplementedError()
        override suspend fun aggregate(request: AggregateRequest): AggregationResult = throw NotImplementedError()
        override suspend fun aggregateGroupByDuration(request: AggregateGroupByDurationRequest): List<AggregationResultGroupedByDuration> = throw NotImplementedError()
        override suspend fun aggregateGroupByPeriod(request: AggregateGroupByPeriodRequest): List<AggregationResultGroupedByPeriod> = throw NotImplementedError()
        override suspend fun getChangesToken(request: ChangesTokenRequest): String = throw NotImplementedError()
        override suspend fun getChanges(changesToken: String): ChangesResponse = throw NotImplementedError()
    }
}
