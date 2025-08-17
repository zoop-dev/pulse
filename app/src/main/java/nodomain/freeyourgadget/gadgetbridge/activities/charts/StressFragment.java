package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LegendEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;

abstract class StressFragment<D extends ChartsData> extends AbstractChartFragment<D> {

    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int CHART_TEXT_COLOR;
    protected int TEXT_COLOR;
    protected int SUB_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;

    @Override
    public String getTitle() {
        return getString(R.string.menuitem_stress);
    }

    @Override
    protected void init() {
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(requireContext());
        LEGEND_TEXT_COLOR = DESCRIPTION_COLOR = TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = SUB_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
    }

    protected List<? extends StressSample> getStressSamples(DBHandler db, GBDevice device, int tsStart, int tsEnd) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends StressSample> sampleProvider = coordinator.getStressSampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(tsStart * 1000L, tsEnd * 1000L);
    }

    protected void ensureStartAndEndSamples(final List<StressSample> samples, int tsStart, int tsEnd) {
        if (samples == null || samples.isEmpty()) {
            return;
        }

        final long tsEndMillis = tsEnd * 1000L;
        final long tsStartMillis = tsStart * 1000L;

        final StressSample lastSample = samples.get(samples.size() - 1);
        if (lastSample.getTimestamp() < tsEndMillis) {
            samples.add(new EmptyStressSample(tsEndMillis));
        }

        final StressSample firstSample = samples.get(0);
        if (firstSample.getTimestamp() > tsStartMillis) {
            samples.add(0, new EmptyStressSample(tsStartMillis));
        }
    }

    protected List<LegendEntry> createLegendEntries(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(StressType.values().length);

        for (final StressType stressType : StressType.values()) {
            final LegendEntry entry = new LegendEntry();
            entry.label = stressType.getLabel(requireContext());
            entry.formColor = stressType.getColor(requireContext());
            legendEntries.add(entry);
        }

        return legendEntries;
    }

    protected Map<StressType, Integer> calculateStressTotals(Iterable<? extends StressSample> samples, int[] stressRanges, int sampleRate) {
        Map<StressType, Integer> result = new HashMap<>();
        for (StressType type : StressType.values()) {
            result.put(type, 0);
        }

        StressSample prevSample = null;
        for (StressSample sample : samples) {
            if (prevSample != null && sample.getStress() >= 0) {
                long durationSinceLastSample = sample.getTimestamp() - prevSample.getTimestamp();
                durationSinceLastSample = Math.min(durationSinceLastSample, sampleRate * 10 * 1000); // Cap at 10x sample rate

                StressType type = StressType.fromStress(prevSample.getStress(), stressRanges);
                if (type != StressType.UNKNOWN) {
                    int seconds = (int) (durationSinceLastSample / 1000);
                    result.put(type, result.get(type) + seconds);
                }
            }
            prevSample = sample;
        }

        return result;
    }

    protected int calculateAverageStress(Iterable<? extends StressSample> samples) {
        long sum = 0;
        int count = 0;

        for (StressSample sample : samples) {
            if (sample.getStress() > 0) {
                sum += sample.getStress();
                count++;
            }
        }

        return count > 0 ? Math.round((float) sum / count) : 0;
    }

    protected static final class EmptyStressSample implements StressSample {
        private final long ts;

        public EmptyStressSample(final long ts) {
            this.ts = ts;
        }

        @Override
        public Type getType() {
            return Type.AUTOMATIC;
        }

        @Override
        public int getStress() {
            return -1;
        }

        @Override
        public long getTimestamp() {
            return ts;
        }
    }

    public enum StressType {
        UNKNOWN(R.string.unknown, R.color.chart_stress_unknown),
        RELAXED(R.string.stress_relaxed, R.color.chart_stress_relaxed),
        MILD(R.string.stress_mild, R.color.chart_stress_mild),
        MODERATE(R.string.stress_moderate, R.color.chart_stress_moderate),
        HIGH(R.string.stress_high, R.color.chart_stress_high);

        private final int labelId;
        private final int colorId;

        StressType(final int labelId, final int colorId) {
            this.labelId = labelId;
            this.colorId = colorId;
        }

        public String getLabel(final Context context) {
            return context.getString(labelId);
        }

        public int getColor(final Context context) {
            return ContextCompat.getColor(context, colorId);
        }

        public static StressType fromStress(final int stress, final int[] stressRanges) {
            if (stress < stressRanges[0]) {
                return StressType.UNKNOWN;
            } else if (stress < stressRanges[1]) {
                return StressType.RELAXED;
            } else if (stress < stressRanges[2]) {
                return StressType.MILD;
            } else if (stress < stressRanges[3]) {
                return StressType.MODERATE;
            } else {
                return StressType.HIGH;
            }
        }
    }
}