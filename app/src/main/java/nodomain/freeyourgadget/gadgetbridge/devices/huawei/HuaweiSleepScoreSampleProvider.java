package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;

import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStatsSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;

public class HuaweiSleepScoreSampleProvider implements TimeSampleProvider<HuaweiSleepScoreSampleProvider.HuaweiSleepScoreSample> {

    private final GBDevice device;
    private final DaoSession session;

    public HuaweiSleepScoreSampleProvider(GBDevice device, DaoSession session) {
        this.device = device;
        this.session = session;
    }

    public static class HuaweiSleepScoreSample implements SleepScoreSample {
        long timestamp;
        int score;

        public HuaweiSleepScoreSample(long timestamp, int score) {
            this.timestamp = timestamp;
            this.score = score;
        }

        @Override
        public int getSleepScore() {
            return score;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }


    }

    @NonNull
    @Override
    public List<HuaweiSleepScoreSample> getAllSamples(long timestamp_from, long timestamp_to) {

        List<HuaweiSleepScoreSample> ret = new ArrayList<>();

        final HuaweiSleepStatsSampleProvider sleepStatsSampleProvider = new HuaweiSleepStatsSampleProvider(this.device, this.session);
        List<HuaweiSleepStatsSample> sleepStatsSamples = sleepStatsSampleProvider.getSleepSamples(timestamp_from, timestamp_to);
        for (HuaweiSleepStatsSample sample : sleepStatsSamples) {
            if (sample.getSleepScore() > 0)
                ret.add(new HuaweiSleepScoreSample(sample.getWakeupTime(), sample.getSleepScore()));

        }
        return ret;
    }

    @Override
    public void addSample(HuaweiSleepScoreSample timeSample) {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Override
    public void addSamples(List<HuaweiSleepScoreSample> timeSamples) {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Override
    public HuaweiSleepScoreSample createSample() {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Nullable
    @Override
    public HuaweiSleepScoreSample getLatestSample() {
        final HuaweiSleepStatsSampleProvider sleepStatsSampleProvider = new HuaweiSleepStatsSampleProvider(this.device, this.session);
        HuaweiSleepStatsSample sample = sleepStatsSampleProvider.getLatestSample();
        if (sample == null)
            return null;
        return new HuaweiSleepScoreSample(sample.getWakeupTime(), sample.getSleepScore());
    }

    @Nullable
    @Override
    public HuaweiSleepScoreSample getLatestSample(final long until) {
        final HuaweiSleepStatsSampleProvider sleepStatsSampleProvider = new HuaweiSleepStatsSampleProvider(this.device, this.session);
        HuaweiSleepStatsSample sample = sleepStatsSampleProvider.getLatestSample(until);
        if (sample == null)
            return null;
        return new HuaweiSleepScoreSample(sample.getWakeupTime(), sample.getSleepScore());
    }

    @Nullable
    @Override
    public HuaweiSleepScoreSample getFirstSample() {
        final HuaweiSleepStatsSampleProvider sleepStatsSampleProvider = new HuaweiSleepStatsSampleProvider(this.device, this.session);
        HuaweiSleepStatsSample sample = sleepStatsSampleProvider.getFirstSample();
        if (sample == null)
            return null;
        return new HuaweiSleepScoreSample(sample.getWakeupTime(), sample.getSleepScore());
    }

}
