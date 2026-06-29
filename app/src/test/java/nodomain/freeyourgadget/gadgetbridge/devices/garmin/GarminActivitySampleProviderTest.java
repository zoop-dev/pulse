package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

import static org.junit.Assert.assertEquals;

public class GarminActivitySampleProviderTest extends TestBase {
    private GBDevice dummyGBDevice;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dummyGBDevice = createDummyGDevice("00:00:00:00:10");
    }

    private GarminActivitySample createGarminSample(GarminActivitySampleProvider sampleProvider, int timestamp, int steps, int distanceCm, int activeCalories, User user, Device device) {
        GarminActivitySample sample = sampleProvider.createActivitySample();
        sample.setProvider(sampleProvider);
        sample.setRawKind(sampleProvider.toRawActivityKind(ActivityKind.ACTIVITY));
        sample.setTimestamp(timestamp);
        sample.setRawIntensity(0);
        sample.setHeartRate(ActivitySample.NOT_MEASURED);
        sample.setSteps(steps);
        sample.setDistanceCm(distanceCm);
        sample.setActiveCalories(activeCalories);
        sample.setUserId(user.getId());
        sample.setDeviceId(device.getId());

        return sample;
    }

    private GarminActivitySample getSampleAt(List<GarminActivitySample> samples, int timestamp) {
        for (GarminActivitySample sample : samples) {
            if (sample.getTimestamp() == timestamp) {
                return sample;
            }
        }

        throw new AssertionError("Missing sample at " + timestamp);
    }

    private void assertGarminCounters(List<GarminActivitySample> samples, int timestamp, int steps, int distanceCm, int activeCalories) {
        GarminActivitySample sample = getSampleAt(samples, timestamp);
        assertEquals(steps, sample.getSteps());
        assertEquals(distanceCm, sample.getDistanceCm());
        assertEquals(activeCalories, sample.getActiveCalories());
    }

    private void assertGarminNotMeasuredCounters(List<GarminActivitySample> samples, int timestamp) {
        assertGarminCounters(
                samples,
                timestamp,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED * 1000
        );
    }

    private int timestamp(String isoDateTime) {
        return (int) Instant.parse(isoDateTime).getEpochSecond();
    }

    @Test
    public void testGarminActivitySamplesKeepBaselineAcrossShiftedMidnightGap() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        try {
            GarminActivitySampleProvider sampleProvider = new GarminActivitySampleProvider(dummyGBDevice, daoSession);
            User user = DBHelper.getUser(daoSession);
            Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
            int dayStart = timestamp("2026-03-06T05:00:00Z");

            sampleProvider.addGBActivitySamples(Arrays.asList(
                    createGarminSample(sampleProvider, dayStart, 6064, 479466, 179, user, device),
                    createGarminSample(sampleProvider, dayStart + 60, 6064, 479466, 179, user, device),
                    createGarminSample(sampleProvider, dayStart + 120, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, user, device),
                    createGarminSample(sampleProvider, dayStart + 180, 6064, 479466, 179, user, device),
                    createGarminSample(sampleProvider, dayStart + 840, 6170, 479466, 182, user, device)
            ));

            List<GarminActivitySample> samples = sampleProvider.getAllActivitySamples(dayStart, dayStart + 13 * 60);

            assertGarminCounters(samples, dayStart, 0, 0, 0);
            assertGarminNotMeasuredCounters(samples, dayStart + 60);
            assertGarminCounters(samples, dayStart + 120, 0, 0, 0);
            assertGarminCounters(samples, dayStart + 13 * 60, 106, 0, 3000);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void testGarminActivitySamplesPreserveResetAfterShiftedMidnightGap() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        try {
            GarminActivitySampleProvider sampleProvider = new GarminActivitySampleProvider(dummyGBDevice, daoSession);
            User user = DBHelper.getUser(daoSession);
            Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
            int dayStart = timestamp("2026-03-06T05:00:00Z");

            sampleProvider.addGBActivitySamples(Arrays.asList(
                    createGarminSample(sampleProvider, dayStart, 6064, 479466, 179, user, device),
                    createGarminSample(sampleProvider, dayStart + 60, 6064, 479466, 179, user, device),
                    createGarminSample(sampleProvider, dayStart + 120, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, user, device),
                    createGarminSample(sampleProvider, dayStart + 180, 38, 400, 1, user, device),
                    createGarminSample(sampleProvider, dayStart + 240, 50, 520, 2, user, device)
            ));

            List<GarminActivitySample> samples = sampleProvider.getAllActivitySamples(dayStart, dayStart + 3 * 60);

            assertGarminCounters(samples, dayStart, 0, 0, 0);
            assertGarminNotMeasuredCounters(samples, dayStart + 60);
            assertGarminCounters(samples, dayStart + 120, 38, 400, 1000);
            assertGarminCounters(samples, dayStart + 180, 12, 120, 1000);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void testGarminActivitySamplesPreserveResetWhenRangeStartsInsideShiftedMidnightGap() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        try {
            GarminActivitySampleProvider sampleProvider = new GarminActivitySampleProvider(dummyGBDevice, daoSession);
            User user = DBHelper.getUser(daoSession);
            Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
            int dayStart = timestamp("2026-03-06T05:00:00Z");

            sampleProvider.addGBActivitySamples(Arrays.asList(
                    createGarminSample(sampleProvider, dayStart, 6064, 479466, 179, user, device),
                    createGarminSample(sampleProvider, dayStart + 60, 6064, 479466, 179, user, device),
                    createGarminSample(sampleProvider, dayStart + 120, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, user, device),
                    createGarminSample(sampleProvider, dayStart + 180, 38, 400, 1, user, device),
                    createGarminSample(sampleProvider, dayStart + 240, 50, 520, 2, user, device)
            ));

            List<GarminActivitySample> samples = sampleProvider.getAllActivitySamples(dayStart + 60, dayStart + 3 * 60);

            assertGarminNotMeasuredCounters(samples, dayStart + 60);
            assertGarminCounters(samples, dayStart + 120, 38, 400, 1000);
            assertGarminCounters(samples, dayStart + 180, 12, 120, 1000);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void testCumulativeCountersRejectSameDayDecreaseWhenRangeStartsWithUnmeasuredSample() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        try {
            GarminActivitySampleProvider sampleProvider = new GarminActivitySampleProvider(dummyGBDevice, daoSession);
            User user = DBHelper.getUser(daoSession);
            Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
            int dayStart = timestamp("2026-03-06T05:00:00Z");
            int previousTimestamp = dayStart + 60;
            int firstTimestamp = dayStart + 2 * 60;
            int nextTimestamp = dayStart + 3 * 60;

            sampleProvider.addGBActivitySample(createGarminSample(
                    sampleProvider,
                    previousTimestamp,
                    100,
                    1000,
                    10,
                    user,
                    device
            ));

            List<GarminActivitySample> samples = Arrays.asList(
                    createGarminSample(sampleProvider, firstTimestamp, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, user, device),
                    createGarminSample(sampleProvider, nextTimestamp, 50, 500, 5, user, device)
            );

            sampleProvider.convertCumulativeSteps(samples, GarminActivitySampleDao.Properties.Steps);

            assertGarminCounters(
                    samples,
                    firstTimestamp,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED
            );
            assertGarminCounters(
                    samples,
                    nextTimestamp,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED
            );
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void testGarminActivitySamplesRejectShiftedSameDayDecreaseWhenRangeStartsWithUnmeasuredSample() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        try {
            GarminActivitySampleProvider sampleProvider = new GarminActivitySampleProvider(dummyGBDevice, daoSession);
            User user = DBHelper.getUser(daoSession);
            Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
            int dayStart = timestamp("2026-03-06T05:00:00Z");
            int previousTimestamp = dayStart + 20 * 60;
            int firstTimestamp = dayStart + 21 * 60;
            int nextTimestamp = dayStart + 22 * 60;

            sampleProvider.addGBActivitySamples(Arrays.asList(
                    createGarminSample(sampleProvider, previousTimestamp, 100, 1000, 10, user, device),
                    createGarminSample(sampleProvider, firstTimestamp, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, user, device),
                    createGarminSample(sampleProvider, nextTimestamp, 50, 500, 5, user, device)
            ));

            List<GarminActivitySample> samples = sampleProvider.getAllActivitySamples(
                    previousTimestamp,
                    nextTimestamp
            );

            assertGarminNotMeasuredCounters(samples, previousTimestamp);
            assertGarminNotMeasuredCounters(samples, firstTimestamp);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void testCumulativeCountersUseRoundedTimestampForMidnightBoundary() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        try {
            GarminActivitySampleProvider sampleProvider = new GarminActivitySampleProvider(dummyGBDevice, daoSession);
            User user = DBHelper.getUser(daoSession);
            Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
            int dayStart = timestamp("2026-03-06T05:00:00Z");

            List<GarminActivitySample> samples = Arrays.asList(
                    createGarminSample(sampleProvider, dayStart - 60, 100, 1000, 10, user, device),
                    createGarminSample(sampleProvider, dayStart + 30, 138, 1500, 12, user, device)
            );

            sampleProvider.convertCumulativeSteps(samples, GarminActivitySampleDao.Properties.Steps);

            assertGarminCounters(samples, dayStart, 38, 500, 2);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
    }
}
