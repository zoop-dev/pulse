/*  Copyright (C) 2025 Gideon Zenz

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */

package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.GenericHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;

/**
 * Provides HRV summary data for Huami/Zepp OS devices by computing statistics
 * from the per-minute HRV value samples.
 * This provider calculates:
 * - Weekly average (7-day rolling average)
 * - Last night average and 5-min high
 * - Baseline values for status determination
 */
public class HuamiHrvSummarySampleProvider implements TimeSampleProvider<HrvSummarySample> {
    private static final int DAYS_FOR_WEEKLY_AVG = 7;
    private static final int DAYS_FOR_BASELINE = 14; // Use 2 weeks for baseline calculation

    private final GenericHrvValueSampleProvider valueProvider;

    public HuamiHrvSummarySampleProvider(final GBDevice device, final DaoSession session) {
        this.valueProvider = new GenericHrvValueSampleProvider(device, session);
    }

    @NonNull
    @Override
    public List<HrvSummarySample> getAllSamples(long timestampFrom, long timestampTo) {
        // For each day in the range, generate a summary sample
        final List<HrvSummarySample> summaries = new ArrayList<>();

        // Start from the beginning of the first day
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampFrom);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Continue until we've processed all days up to timestampTo
        final Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(timestampTo);

        while (cal.getTimeInMillis() <= endCal.getTimeInMillis()) {
            // Generate summary for this day (at end of day timestamp)
            final Calendar dayEnd = (Calendar) cal.clone();
            dayEnd.set(Calendar.HOUR_OF_DAY, 23);
            dayEnd.set(Calendar.MINUTE, 59);
            dayEnd.set(Calendar.SECOND, 59);
            dayEnd.set(Calendar.MILLISECOND, 999);

            final HrvSummarySample summary = generateSummaryForDay(dayEnd.getTimeInMillis());
            if (summary != null) {
                summaries.add(summary);
            }

            // Move to next day
            cal.add(Calendar.DATE, 1);
        }

        return summaries;
    }

    /**
     * Generate a summary sample for a specific day.
     * The timestamp should be at the end of the day (23:59:59).
     */
    private HrvSummarySample generateSummaryForDay(long dayEndTimestamp) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dayEndTimestamp);

        // Get start of current day
        final Calendar dayStart = (Calendar) cal.clone();
        dayStart.set(Calendar.HOUR_OF_DAY, 0);
        dayStart.set(Calendar.MINUTE, 0);
        dayStart.set(Calendar.SECOND, 0);
        dayStart.set(Calendar.MILLISECOND, 0);

        // Get value samples for the current day
        final List<? extends HrvValueSample> todaySamples = valueProvider.getAllSamples(
                dayStart.getTimeInMillis(),
                dayEndTimestamp
        );

        if (todaySamples.isEmpty()) {
            return null;
        }


        // Calculate weekly average (past 7 days including today)
        final Calendar weekStart = (Calendar) dayStart.clone();
        weekStart.add(Calendar.DATE, -DAYS_FOR_WEEKLY_AVG + 1);
        final int weeklyAvg = calculateAverageHrv(weekStart.getTimeInMillis(), dayEndTimestamp);

        // Calculate last night average (previous night, typically 10 PM to 6 AM)
        final Calendar lastNightEnd = (Calendar) dayStart.clone();
        final Calendar lastNightStart = (Calendar) lastNightEnd.clone();
        lastNightStart.add(Calendar.HOUR_OF_DAY, -8); // 8 hours back from midnight
        final int lastNightAvg = calculateAverageHrv(lastNightStart.getTimeInMillis(), lastNightEnd.getTimeInMillis());
        final int lastNight5MinHigh = calculate5MinHighHrv(lastNightStart.getTimeInMillis(), lastNightEnd.getTimeInMillis());

        // Calculate baseline values (using past 2 weeks)
        final Calendar baselineStart = (Calendar) dayStart.clone();
        baselineStart.add(Calendar.DATE, -DAYS_FOR_BASELINE);
        final BaselineValues baseline = calculateBaseline(baselineStart.getTimeInMillis(), dayEndTimestamp);

        // Determine status based on weekly average and baseline
        final HrvSummarySample.Status status = determineStatus(weeklyAvg, baseline);

        return new HuamiHrvSummarySample(
                dayEndTimestamp,
                weeklyAvg > 0 ? weeklyAvg : null,
                lastNightAvg > 0 ? lastNightAvg : null,
                lastNight5MinHigh > 0 ? lastNight5MinHigh : null,
                baseline.lowUpper > 0 ? baseline.lowUpper : null,
                baseline.balancedLower > 0 ? baseline.balancedLower : null,
                baseline.balancedUpper > 0 ? baseline.balancedUpper : null,
                status
        );
    }

    private int calculateAverageHrv(long timestampFrom, long timestampTo) {
        final List<? extends HrvValueSample> samples = valueProvider.getAllSamples(timestampFrom, timestampTo);
        if (samples.isEmpty()) {
            return 0;
        }

        return (int) samples.stream()
                .mapToInt(HrvValueSample::getValue)
                .filter(v -> v > 0) // Filter out invalid values
                .average()
                .orElse(0);
    }

    private int calculate5MinHighHrv(long timestampFrom, long timestampTo) {
        final List<? extends HrvValueSample> samples = valueProvider.getAllSamples(timestampFrom, timestampTo);
        if (samples.size() < 5) {
            return 0;
        }

        // Calculate rolling 5-minute averages and find the highest
        int maxAvg = 0;
        for (int i = 0; i <= samples.size() - 5; i++) {
            int sum = 0;
            for (int j = i; j < i + 5; j++) {
                sum += samples.get(j).getValue();
            }
            int avg = sum / 5;
            if (avg > maxAvg) {
                maxAvg = avg;
            }
        }

        return maxAvg;
    }

    private BaselineValues calculateBaseline(long timestampFrom, long timestampTo) {
        final List<? extends HrvValueSample> samples = valueProvider.getAllSamples(timestampFrom, timestampTo);
        if (samples.isEmpty()) {
            return new BaselineValues(0, 0, 0);
        }

        // Get all HRV values and sort them
        final List<Integer> values = new ArrayList<>();
        for (HrvValueSample sample : samples) {
            if (sample.getValue() > 0) {
                values.add(sample.getValue());
            }
        }

        if (values.isEmpty()) {
            return new BaselineValues(0, 0, 0);
        }

        Collections.sort(values);

        // Calculate percentiles for baseline determination
        // Low: below 25th percentile
        // Balanced: 25th to 75th percentile
        // Unbalanced: above 75th percentile
        final int p25Index = values.size() / 4;
        final int p75Index = (values.size() * 3) / 4;

        final int baselineLowUpper = values.get(Math.max(0, p25Index - 1));
        final int baselineBalancedLower = values.get(p25Index);
        final int baselineBalancedUpper = values.get(Math.min(values.size() - 1, p75Index));

        return new BaselineValues(baselineLowUpper, baselineBalancedLower, baselineBalancedUpper);
    }

    private HrvSummarySample.Status determineStatus(int weeklyAvg, BaselineValues baseline) {
        if (weeklyAvg == 0 || baseline.balancedLower == 0 || baseline.balancedUpper == 0) {
            return HrvSummarySample.Status.NONE;
        }

        if (weeklyAvg < baseline.lowUpper) {
            // Very low HRV - could indicate poor recovery or health issues
            return HrvSummarySample.Status.LOW;
        } else if (weeklyAvg < baseline.balancedLower) {
            // Below balanced range
            return HrvSummarySample.Status.UNBALANCED;
        } else if (weeklyAvg <= baseline.balancedUpper) {
            // Within balanced range
            return HrvSummarySample.Status.BALANCED;
        } else {
            // Above balanced range - could indicate overtraining or stress
            return HrvSummarySample.Status.UNBALANCED;
        }
    }

    @Override
    public void addSample(HrvSummarySample timeSample) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public void addSamples(List<HrvSummarySample> timeSamples) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @NonNull
    @Override
    public HrvSummarySample createSample() {
        // This provider is read-only and computes samples on-demand, so we return an empty sample
        return new HuamiHrvSummarySample(
                System.currentTimeMillis(),
                null,
                null,
                null,
                null,
                null,
                null,
                HrvSummarySample.Status.NONE
        );
    }

    @Nullable
    @Override
    public HrvSummarySample getLatestSample() {
        // Get the latest HRV value sample timestamp
        final HrvValueSample latestValue = valueProvider.getLatestSample();
        if (latestValue == null) {
            return null;
        }

        // Generate summary for that day
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(latestValue.getTimestamp());
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return generateSummaryForDay(cal.getTimeInMillis());
    }

    @Nullable
    @Override
    public HrvSummarySample getLatestSample(long until) {
        // Get the latest HRV value sample until the specified timestamp
        final HrvValueSample latestValue = valueProvider.getLatestSample(until);
        if (latestValue == null) {
            return null;
        }

        // Generate summary for that day
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(latestValue.getTimestamp());
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return generateSummaryForDay(cal.getTimeInMillis());
    }

    @Nullable
    @Override
    public HrvSummarySample getFirstSample() {
        // Get the first HRV value sample timestamp
        final HrvValueSample firstValue = valueProvider.getFirstSample();
        if (firstValue == null) {
            return null;
        }

        // Generate summary for that day
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(firstValue.getTimestamp());
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return generateSummaryForDay(cal.getTimeInMillis());
    }

    private static class BaselineValues {
        final int lowUpper;
        final int balancedLower;
        final int balancedUpper;

        BaselineValues(int lowUpper, int balancedLower, int balancedUpper) {
            this.lowUpper = lowUpper;
            this.balancedLower = balancedLower;
            this.balancedUpper = balancedUpper;
        }
    }

    /**
     * Computed HRV summary sample for Huami/Zepp OS devices.
     * This is not stored in the database but computed on-demand from HRV value samples.
     */
    public static class HuamiHrvSummarySample implements HrvSummarySample {
        private final long timestamp;
        private final Integer weeklyAverage;
        private final Integer lastNightAverage;
        private final Integer lastNight5MinHigh;
        private final Integer baselineLowUpper;
        private final Integer baselineBalancedLower;
        private final Integer baselineBalancedUpper;
        private final Status status;

        public HuamiHrvSummarySample(
                long timestamp,
                Integer weeklyAverage,
                Integer lastNightAverage,
                Integer lastNight5MinHigh,
                Integer baselineLowUpper,
                Integer baselineBalancedLower,
                Integer baselineBalancedUpper,
                Status status) {
            this.timestamp = timestamp;
            this.weeklyAverage = weeklyAverage;
            this.lastNightAverage = lastNightAverage;
            this.lastNight5MinHigh = lastNight5MinHigh;
            this.baselineLowUpper = baselineLowUpper;
            this.baselineBalancedLower = baselineBalancedLower;
            this.baselineBalancedUpper = baselineBalancedUpper;
            this.status = status;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public Integer getWeeklyAverage() {
            return weeklyAverage;
        }

        @Override
        public Integer getLastNightAverage() {
            return lastNightAverage;
        }

        @Override
        public Integer getLastNight5MinHigh() {
            return lastNight5MinHigh;
        }

        @Override
        public Integer getBaselineLowUpper() {
            return baselineLowUpper;
        }

        @Override
        public Integer getBaselineBalancedLower() {
            return baselineBalancedLower;
        }

        @Override
        public Integer getBaselineBalancedUpper() {
            return baselineBalancedUpper;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }
}

