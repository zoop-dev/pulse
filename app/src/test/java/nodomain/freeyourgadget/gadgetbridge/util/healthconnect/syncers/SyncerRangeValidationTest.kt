package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import nodomain.freeyourgadget.gadgetbridge.entities.GlucoseSample
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample
import nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample
import nodomain.freeyourgadget.gadgetbridge.model.WeightSample
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class SyncerRangeValidationTest {

    private val offset: ZoneOffset = ZoneOffset.UTC
    private val device = "test-device"
    private val metadata: Metadata = Metadata.unknownRecordingMethod(
        Device(type = Device.TYPE_WATCH, manufacturer = "test", model = "test")
    )

    // --- HRV ---

    private fun hrvSample(value: Int, ts: Long = 1_700_000_000_000L): HrvValueSample =
        object : HrvValueSample {
            override fun getTimestamp(): Long = ts
            override fun getValue(): Int = value
        }

    @Test
    fun hrv_lowerBoundary_accepted() {
        assertNotNull(HrvSyncer.convertSample(hrvSample(1), offset, metadata, device))
    }

    @Test
    fun hrv_upperBoundary_accepted() {
        assertNotNull(HrvSyncer.convertSample(hrvSample(200), offset, metadata, device))
    }

    @Test
    fun hrv_belowLower_dropped() {
        assertNull(HrvSyncer.convertSample(hrvSample(0), offset, metadata, device))
    }

    @Test
    fun hrv_aboveUpper_dropped() {
        // The actual #6190 value
        assertNull(HrvSyncer.convertSample(hrvSample(211), offset, metadata, device))
    }

    @Test
    fun hrv_negative_dropped() {
        assertNull(HrvSyncer.convertSample(hrvSample(-1), offset, metadata, device))
    }

    // --- VO2 max ---

    private fun vo2Sample(value: Float, ts: Long = 1_700_000_000_000L): Vo2MaxSample =
        object : Vo2MaxSample {
            override fun getTimestamp(): Long = ts
            override fun getValue(): Float = value
            override fun getType(): Vo2MaxSample.Type = Vo2MaxSample.Type.ANY
        }

    @Test
    fun vo2_lowerBoundary_accepted() {
        assertNotNull(Vo2MaxSyncer.convertSample(vo2Sample(0.0f), offset, metadata, device))
    }

    @Test
    fun vo2_upperBoundary_accepted() {
        assertNotNull(Vo2MaxSyncer.convertSample(vo2Sample(100.0f), offset, metadata, device))
    }

    @Test
    fun vo2_aboveUpper_dropped() {
        assertNull(Vo2MaxSyncer.convertSample(vo2Sample(101.0f), offset, metadata, device))
    }

    @Test
    fun vo2_negative_dropped() {
        assertNull(Vo2MaxSyncer.convertSample(vo2Sample(-0.5f), offset, metadata, device))
    }

    @Test
    fun vo2_nan_dropped() {
        assertNull(Vo2MaxSyncer.convertSample(vo2Sample(Float.NaN), offset, metadata, device))
    }

    // --- Weight ---

    private fun weightSample(kg: Float, ts: Long = 1_700_000_000_000L): WeightSample =
        object : WeightSample {
            override fun getTimestamp(): Long = ts
            override fun getWeightKg(): Float = kg
        }

    @Test
    fun weight_lowerBoundary_accepted() {
        // HC's WeightRecord.weight uses requireNotLess(weight, 0 kg), so 0.0 is at the bound and accepted.
        assertNotNull(WeightSyncer.convertSample(weightSample(0.0f), offset, metadata, device))
    }

    @Test
    fun weight_upperBoundary_accepted() {
        assertNotNull(WeightSyncer.convertSample(weightSample(1000.0f), offset, metadata, device))
    }

    @Test
    fun weight_aboveUpper_dropped() {
        assertNull(WeightSyncer.convertSample(weightSample(1001.0f), offset, metadata, device))
    }

    @Test
    fun weight_negative_dropped() {
        assertNull(WeightSyncer.convertSample(weightSample(-0.1f), offset, metadata, device))
    }

    // --- Respiratory rate ---

    private fun respSample(rate: Float, ts: Long = 1_700_000_000_000L): RespiratoryRateSample =
        object : RespiratoryRateSample {
            override fun getTimestamp(): Long = ts
            override fun getRespiratoryRate(): Float = rate
        }

    @Test
    fun resp_lowerBoundary_accepted() {
        assertNotNull(RespiratoryRateSyncer.convertSample(respSample(0.0f), offset, metadata, device))
    }

    @Test
    fun resp_upperBoundary_accepted() {
        assertNotNull(RespiratoryRateSyncer.convertSample(respSample(1000.0f), offset, metadata, device))
    }

    @Test
    fun resp_aboveUpper_dropped() {
        assertNull(RespiratoryRateSyncer.convertSample(respSample(1001.0f), offset, metadata, device))
    }

    @Test
    fun resp_negative_dropped() {
        assertNull(RespiratoryRateSyncer.convertSample(respSample(-1.0f), offset, metadata, device))
    }

    // --- SpO2 ---

    private fun spo2Sample(spo2: Int, ts: Long = 1_700_000_000_000L): Spo2Sample =
        object : Spo2Sample {
            override fun getTimestamp(): Long = ts
            override fun getSpo2(): Int = spo2
            override fun getType(): Spo2Sample.Type = Spo2Sample.Type.UNKNOWN
        }

    @Test
    fun spo2_lowerBoundary_accepted() {
        assertNotNull(Spo2Syncer.convertSample(spo2Sample(1), offset, metadata, device))
    }

    @Test
    fun spo2_zero_dropped() {
        assertNull(Spo2Syncer.convertSample(spo2Sample(0), offset, metadata, device))
    }

    @Test
    fun spo2_upperBoundary_accepted() {
        assertNotNull(Spo2Syncer.convertSample(spo2Sample(100), offset, metadata, device))
    }

    @Test
    fun spo2_aboveUpper_dropped() {
        assertNull(Spo2Syncer.convertSample(spo2Sample(101), offset, metadata, device))
    }

    @Test
    fun spo2_negative_dropped() {
        assertNull(Spo2Syncer.convertSample(spo2Sample(-1), offset, metadata, device))
    }

    // --- Resting HR ---

    private fun hrSample(bpm: Int, ts: Long = 1_700_000_000_000L): HeartRateSample =
        object : HeartRateSample {
            override fun getTimestamp(): Long = ts
            override fun getHeartRate(): Int = bpm
        }

    @Test
    fun restingHr_lowerBoundary_accepted() {
        assertNotNull(RestingHeartRateSyncer.convertSample(hrSample(0), offset, metadata, device))
    }

    @Test
    fun restingHr_upperBoundary_accepted() {
        assertNotNull(RestingHeartRateSyncer.convertSample(hrSample(300), offset, metadata, device))
    }

    @Test
    fun restingHr_aboveUpper_dropped() {
        assertNull(RestingHeartRateSyncer.convertSample(hrSample(301), offset, metadata, device))
    }

    @Test
    fun restingHr_sentinel255_dropped() {
        assertNull(RestingHeartRateSyncer.convertSample(hrSample(255), offset, metadata, device))
    }

    @Test
    fun restingHr_negative_dropped() {
        assertNull(RestingHeartRateSyncer.convertSample(hrSample(-1), offset, metadata, device))
    }

    // --- Blood glucose ---

    private fun glucoseSample(mgDl: Double, ts: Long = 1_700_000_000_000L): GlucoseSample {
        val s = GlucoseSample()
        s.timestamp = ts
        s.valueMgDl = mgDl
        return s
    }

    @Test
    fun bloodGlucose_lowerBoundary_accepted() {
        assertNotNull(BloodGlucoseSyncer.convertSample(glucoseSample(0.0), offset, metadata, device))
    }

    @Test
    fun bloodGlucose_upperBoundary_accepted() {
        // 50 mmol/L ~= 900.91 mg/dL
        assertNotNull(BloodGlucoseSyncer.convertSample(glucoseSample(900.0), offset, metadata, device))
    }

    @Test
    fun bloodGlucose_aboveUpper_dropped() {
        // > 50 mmol/L
        assertNull(BloodGlucoseSyncer.convertSample(glucoseSample(950.0), offset, metadata, device))
    }

    @Test
    fun bloodGlucose_negative_dropped() {
        assertNull(BloodGlucoseSyncer.convertSample(glucoseSample(-1.0), offset, metadata, device))
    }
}
