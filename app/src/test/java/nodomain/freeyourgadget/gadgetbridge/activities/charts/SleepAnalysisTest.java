package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SleepAnalysisTest {

    private static final int BASE = 1_000_000;
    private static final int MIN = 60;

    @Test
    public void testEmptySamples() {
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(Collections.emptyList());
        assertEquals(0, sessions.size());
    }

    @Test
    public void testSingleSleepType() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 5 * MIN, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.LIGHT_SLEEP)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 10 * MIN, 600, 0, 0, 0);
    }

    @Test
    public void testMixedSleepTypes() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 5 * MIN, ActivityKind.DEEP_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.REM_SLEEP),
                sleep(BASE + 15 * MIN, ActivityKind.LIGHT_SLEEP)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 15 * MIN, 300, 300, 300, 0);
    }

    @Test
    public void testSessionTooShort() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 4 * MIN, ActivityKind.LIGHT_SLEEP)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(0, sessions.size());
    }

    @Test
    public void testGapBridgedByResumingSleep() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.DEEP_SLEEP),
                idle(BASE + 20 * MIN),
                idle(BASE + 28 * MIN),
                sleep(BASE + 36 * MIN, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 46 * MIN, ActivityKind.DEEP_SLEEP)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 46 * MIN,
                480,
                1200,
                0,
                1080);
    }

    @Test
    public void testGapNotBridgedByActivity() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.DEEP_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.DEEP_SLEEP),
                idle(BASE + 20 * MIN),
                activity(BASE + 30 * MIN, 100)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 10 * MIN, 0, 600, 0, 0);
    }

    @Test
    public void testGapNotBridgedByTimeout() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.DEEP_SLEEP),
                sleep(BASE + 30 * MIN, ActivityKind.DEEP_SLEEP),
                idle(BASE + 61 * MIN),
                idle(BASE + 92 * MIN)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 30 * MIN, 0, 1800, 0, 0);
    }

    @Test
    public void testTrailingNonSleepExcludedFromAwake() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.DEEP_SLEEP),
                idle(BASE + 20 * MIN),
                idle(BASE + 30 * MIN)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 10 * MIN, 0, 600, 0, 0);
    }

    @Test
    public void testMultipleBridgedGaps() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.DEEP_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.DEEP_SLEEP),
                idle(BASE + 20 * MIN),
                sleep(BASE + 28 * MIN, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 38 * MIN, ActivityKind.LIGHT_SLEEP),
                idle(BASE + 48 * MIN),
                sleep(BASE + 56 * MIN, ActivityKind.REM_SLEEP),
                sleep(BASE + 66 * MIN, ActivityKind.REM_SLEEP)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 66 * MIN,
                1080,
                600,
                1080,
                1200);
    }

    @Test
    public void testDeviceReportedAwakeSleep() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.DEEP_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.DEEP_SLEEP),
                sleep(BASE + 20 * MIN, ActivityKind.AWAKE_SLEEP),
                sleep(BASE + 30 * MIN, ActivityKind.AWAKE_SLEEP),
                sleep(BASE + 40 * MIN, ActivityKind.LIGHT_SLEEP)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 40 * MIN, 600, 600, 0, 1200);
    }

    @Test
    public void testMixedDeviceAwakeAndBridgedGap() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.DEEP_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.DEEP_SLEEP),
                sleep(BASE + 20 * MIN, ActivityKind.AWAKE_SLEEP),
                idle(BASE + 30 * MIN),
                sleep(BASE + 38 * MIN, ActivityKind.LIGHT_SLEEP)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(1, sessions.size());
        assertSession(sessions.get(0), BASE, BASE + 38 * MIN,
                480,
                600,
                0,
                1200);
    }

    @Test
    public void testMultipleSessions() {
        List<ActivitySample> samples = Arrays.asList(
                sleep(BASE, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 10 * MIN, ActivityKind.DEEP_SLEEP),
                activity(BASE + 10 * MIN + 1, 200),
                sleep(BASE + 10 * MIN + 2, ActivityKind.LIGHT_SLEEP),
                sleep(BASE + 20 * MIN + 2, ActivityKind.REM_SLEEP)
        );
        List<SleepAnalysis.SleepSession> sessions =
                new SleepAnalysis().calculateSleepSessions(samples);
        assertEquals(2, sessions.size());
        assertEquals(new Date(BASE * 1000L), sessions.get(0).getSleepStart());
        assertEquals(new Date((BASE + 10 * MIN) * 1000L), sessions.get(0).getSleepEnd());
        assertEquals(600, sessions.get(0).getDeepSleepDuration());
        assertEquals(new Date((BASE + 10 * MIN + 2) * 1000L), sessions.get(1).getSleepStart());
        assertEquals(new Date((BASE + 20 * MIN + 2) * 1000L), sessions.get(1).getSleepEnd());
        assertEquals(600, sessions.get(1).getRemSleepDuration());
    }

    private static void assertSession(SleepAnalysis.SleepSession session,
                                       int expectedStart,
                                       int expectedEnd,
                                       long expectedLight,
                                       long expectedDeep,
                                       long expectedRem,
                                       long expectedAwake) {
        assertEquals(new Date(expectedStart * 1000L), session.getSleepStart());
        assertEquals(new Date(expectedEnd * 1000L), session.getSleepEnd());
        assertEquals("light", expectedLight, session.getLightSleepDuration());
        assertEquals("deep", expectedDeep, session.getDeepSleepDuration());
        assertEquals("rem", expectedRem, session.getRemSleepDuration());
        assertEquals("awake", expectedAwake, session.getAwakeSleepDuration());

        long total = session.getLightSleepDuration() + session.getDeepSleepDuration()
                + session.getRemSleepDuration() + session.getAwakeSleepDuration();
        long span = expectedEnd - expectedStart;
        assertTrue("Duration sum (" + total + ") exceeds session span (" + span + ")",
                total <= span);
    }

    private static MockSample sleep(int timestamp, ActivityKind kind) {
        return new MockSample(timestamp, kind, 0);
    }

    private static MockSample idle(int timestamp) {
        return new MockSample(timestamp, ActivityKind.ACTIVITY, 0);
    }

    private static MockSample activity(int timestamp, int steps) {
        return new MockSample(timestamp, ActivityKind.ACTIVITY, steps);
    }

    private static class MockSample implements ActivitySample {
        private final int timestamp;
        private final ActivityKind kind;
        private final int steps;

        MockSample(int timestamp, ActivityKind kind, int steps) {
            this.timestamp = timestamp;
            this.kind = kind;
            this.steps = steps;
        }

        @Override public int getTimestamp() { return timestamp; }
        @Override public ActivityKind getKind() { return kind; }
        @Override public int getSteps() { return steps; }
        @Override public SampleProvider<?> getProvider() { return null; }
        @Override public int getRawKind() { return kind.getCode(); }
        @Override public int getRawIntensity() { return NOT_MEASURED; }
        @Override public float getIntensity() { return 0; }
        @Override public int getDistanceCm() { return NOT_MEASURED; }
        @Override public int getActiveCalories() { return NOT_MEASURED; }
        @Override public int getHeartRate() { return NOT_MEASURED; }
        @Override public void setHeartRate(int value) {}
    }
}
