package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LegendEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;

abstract class SleepFragment<D extends ChartsData> extends AbstractActivityChartFragment<D> {
    @Override
    public String getTitle() {
        return getString(R.string.sleepchart_your_sleep);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    public boolean supportsSleepScore() {
        final GBDevice device = getChartsHost().getDevice();
        return device.getDeviceCoordinator().supportsSleepScore(device);
    }

    protected List<SleepScoreSample> getSleepScoreSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        TimeSampleProvider<? extends SleepScoreSample> provider = device.getDeviceCoordinator().getSleepScoreProvider(device, db.getDaoSession());
        return (List<SleepScoreSample>) provider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    protected List<SleepScoreSample> getSleepScoreSamples(DBHandler db, GBDevice device, Calendar day) {
        int startTs;
        int endTs;

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, 0);
        startTs = (int) (day.getTimeInMillis() / 1000);
        endTs = startTs + 24 * 60 * 60 - 1;

        TimeSampleProvider<? extends SleepScoreSample> provider = device.getDeviceCoordinator().getSleepScoreProvider(device, db.getDaoSession());
        return (List<SleepScoreSample>) provider.getAllSamples(startTs * 1000L, endTs * 1000L);
    }

    protected List<LegendEntry> createLegendEntries(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(4);
        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = getActivity().getString(R.string.sleep_colored_stats_light);
        lightSleepEntry.formColor = akLightSleep.color;
        legendEntries.add(lightSleepEntry);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = getActivity().getString(R.string.sleep_colored_stats_deep);
        deepSleepEntry.formColor = akDeepSleep.color;
        legendEntries.add(deepSleepEntry);

        if (supportsRemSleep(getChartsHost().getDevice())) {
            LegendEntry remSleepEntry = new LegendEntry();
            remSleepEntry.label = getActivity().getString(R.string.sleep_colored_stats_rem);
            remSleepEntry.formColor = akRemSleep.color;
            legendEntries.add(remSleepEntry);
        }

        if (supportsAwakeSleep(getChartsHost().getDevice())) {
            LegendEntry awakeSleepEntry = new LegendEntry();
            awakeSleepEntry.label = getActivity().getString(R.string.abstract_chart_fragment_kind_awake_sleep);
            awakeSleepEntry.formColor = akAwakeSleep.color;
            legendEntries.add(awakeSleepEntry);
        }

        return legendEntries;
    }
}
