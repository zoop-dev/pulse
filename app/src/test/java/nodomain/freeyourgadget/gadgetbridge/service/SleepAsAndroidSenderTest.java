package nodomain.freeyourgadget.gadgetbridge.service;

import org.junit.Assert;
import org.junit.Test;

public class SleepAsAndroidSenderTest {

    private static final float EPSILON = 1e-4f;

    @Test
    public void computeAccelerationMagnitude_unitX() {
        Assert.assertEquals(1f, SleepAsAndroidSender.computeAccelerationMagnitude(1f, 0f, 0f), EPSILON);
    }

    @Test
    public void computeAccelerationMagnitude_unitY() {
        Assert.assertEquals(1f, SleepAsAndroidSender.computeAccelerationMagnitude(0f, 1f, 0f), EPSILON);
    }

    @Test
    public void computeAccelerationMagnitude_gravity() {
        // 1g vector on z, magnitude == 9.81
        Assert.assertEquals(9.81f, SleepAsAndroidSender.computeAccelerationMagnitude(0f, 0f, 9.81f), EPSILON);
    }

    @Test
    public void computeAccelerationMagnitude_threeAxis() {
        // sqrt(1+4+4) = 3
        Assert.assertEquals(3f, SleepAsAndroidSender.computeAccelerationMagnitude(1f, 2f, 2f), EPSILON);
    }

    @Test
    public void computeAccelerationMagnitude_zero() {
        Assert.assertEquals(0f, SleepAsAndroidSender.computeAccelerationMagnitude(0f, 0f, 0f), EPSILON);
    }

    @Test
    public void aggregateWindow_simple() {
        float[] out = new float[3];
        SleepAsAndroidSender.aggregateWindow(new float[]{1f, 2f, 3f, 4f, 5f}, out);
        Assert.assertEquals(5f, out[0], EPSILON);  // max
        Assert.assertEquals(1f, out[1], EPSILON);  // min
        Assert.assertEquals(15f, out[2], EPSILON); // sum
    }

    @Test
    public void aggregateWindow_singleSample() {
        float[] out = new float[3];
        SleepAsAndroidSender.aggregateWindow(new float[]{9.81f}, out);
        Assert.assertEquals(9.81f, out[0], EPSILON);
        Assert.assertEquals(9.81f, out[1], EPSILON);
        Assert.assertEquals(9.81f, out[2], EPSILON);
    }

    @Test
    public void aggregateWindow_negativeAndPositive() {
        float[] out = new float[3];
        SleepAsAndroidSender.aggregateWindow(new float[]{-2f, 0f, 3f}, out);
        Assert.assertEquals(3f, out[0], EPSILON);
        Assert.assertEquals(-2f, out[1], EPSILON);
        Assert.assertEquals(1f, out[2], EPSILON);
    }

    @Test
    public void constants_areCorrect() {
        Assert.assertEquals(10_000L, SleepAsAndroidSender.ACCEL_AGGREGATE_INTERVAL_MS);
        Assert.assertEquals(60, SleepAsAndroidSender.HR_BUFFER_MAX);
        Assert.assertEquals(10f, SleepAsAndroidSender.HR_MIN_VALID, EPSILON);
        Assert.assertEquals(240f, SleepAsAndroidSender.HR_MAX_VALID, EPSILON);
    }
}
