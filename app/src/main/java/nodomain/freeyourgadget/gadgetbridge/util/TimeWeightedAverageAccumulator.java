package nodomain.freeyourgadget.gadgetbridge.util;

public class TimeWeightedAverageAccumulator {
    private long lastTimestamp;
    private int count;

    private double min = Double.MAX_VALUE;
    private double max = -Double.MAX_VALUE;

    private double weightedSum = 0.0;
    private long totalWeight = 0;
    private boolean first = true;

    private final long defaultTimeDiff;
    private final long maxTimeDiff;

    public TimeWeightedAverageAccumulator(long maxTimeDiff, long defaultTimeDiff) {
        this.maxTimeDiff = maxTimeDiff;
        this.defaultTimeDiff = defaultTimeDiff;
        this.count = 0;
    }

    public void add(long timestamp, double value) {
        if (value > max) {
            max = value;
        }
        if (value < min) {
            min = value;
        }
        if (first) {
            if (defaultTimeDiff > 0) {
                weightedSum = value * defaultTimeDiff;
                totalWeight = defaultTimeDiff;
            } else {
                weightedSum = 0;
                totalWeight = 0;
            }
            lastTimestamp = timestamp;
            first = false;
            return;
        }
        long timeDiff = timestamp - lastTimestamp;
        if (maxTimeDiff > 0 && defaultTimeDiff > 0 && timeDiff > maxTimeDiff) {
            timeDiff = defaultTimeDiff;
        }
        weightedSum += value * timeDiff;
        totalWeight += timeDiff;
        lastTimestamp = timestamp;
        count++;
    }

    public double getAverage() {
        return totalWeight == 0 ? 0.0 : weightedSum / totalWeight;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public int getCount() {
        return count;
    }
}