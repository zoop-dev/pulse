package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import static org.junit.Assert.assertEquals;

import com.github.mikephil.charting.data.Entry;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class CaloriesDailyFragmentTest {
    private static final int START_TS = 1_000_000;

    @Test
    public void createsZeroEntryForEmptyDay() {
        final TimestampTranslation tsTranslation = new TimestampTranslation();

        final List<Entry> entries = CaloriesDailyFragment.createCumulativeActiveCaloriesEntries(
                Collections.emptyList(),
                tsTranslation,
                START_TS
        );

        assertEquals(1, entries.size());
        assertEntry(entries.get(0), 0f, 0f);
        assertEquals(START_TS, tsTranslation.toOriginalValue(0));
    }

    @Test
    public void accumulatesPositiveActiveCaloriesFromDayStart() {
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        final List<ActivitySample> samples = Arrays.asList(
                sample(START_TS + 60, 1200),
                sample(START_TS + 120, 0),
                sample(START_TS + 180, ActivitySample.NOT_MEASURED),
                sample(START_TS + 240, 1800)
        );

        final List<Entry> entries = CaloriesDailyFragment.createCumulativeActiveCaloriesEntries(
                samples,
                tsTranslation,
                START_TS
        );

        assertEquals(5, entries.size());
        assertEntry(entries.get(0), 0f, 0f);
        assertEntry(entries.get(1), 60f, 1f);
        assertEntry(entries.get(2), 120f, 1f);
        assertEntry(entries.get(3), 180f, 1f);
        assertEntry(entries.get(4), 240f, 3f);
    }

    private static void assertEntry(final Entry entry, final float x, final float y) {
        assertEquals(x, entry.getX(), 0.001f);
        assertEquals(y, entry.getY(), 0.001f);
    }

    private static ActivitySample sample(final int timestamp, final int activeCalories) {
        return new MockSample(timestamp, activeCalories);
    }

    private static class MockSample implements ActivitySample {
        private final int timestamp;
        private final int activeCalories;

        MockSample(final int timestamp, final int activeCalories) {
            this.timestamp = timestamp;
            this.activeCalories = activeCalories;
        }

        @Override public int getTimestamp() { return timestamp; }
        @Override public SampleProvider<?> getProvider() { return null; }
        @Override public int getRawKind() { return ActivityKind.ACTIVITY.getCode(); }
        @Override public ActivityKind getKind() { return ActivityKind.ACTIVITY; }
        @Override public int getRawIntensity() { return NOT_MEASURED; }
        @Override public float getIntensity() { return 0; }
        @Override public int getSteps() { return 0; }
        @Override public int getDistanceCm() { return NOT_MEASURED; }
        @Override public int getActiveCalories() { return activeCalories; }
        @Override public int getHeartRate() { return NOT_MEASURED; }
        @Override public void setHeartRate(int value) {}
    }
}
