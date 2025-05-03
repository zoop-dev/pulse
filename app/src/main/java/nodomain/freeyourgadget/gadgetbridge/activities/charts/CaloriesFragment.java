package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.Activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DefaultRestingMetabolicRateProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.RestingMetabolicRateSample;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;

abstract class CaloriesFragment<T extends ChartsData> extends AbstractChartFragment<T> {
    protected static final Logger LOG = LoggerFactory.getLogger(CaloriesFragment.class);

    protected int CHART_TEXT_COLOR;
    protected int TEXT_COLOR;

    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int TOTAL_DAYS = 1;

    @Override
    public String getTitle() {
        return getString(R.string.steps);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(getContext());
        DESCRIPTION_COLOR = GBApplication.getTextColor(getContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(getContext());
    }

    protected List<CaloriesFragment.CaloriesDay> getMyCaloriesDaysData(DBHandler db, Calendar day, GBDevice device) {
        RestingMetabolicRateSample metabolicRate = getRestingMetabolicRate(db, device);
        int restingCalories = metabolicRate.getRestingMetabolicRate();

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);

        List<CaloriesDay> daysData = new ArrayList<>();
        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            long activeCalories = 0;
            ActivityAmounts amounts = getActivityAmountsForDay(db, day, device);
            for (ActivityAmount amount : amounts.getAmounts()) {
                if (amount.getTotalActiveCalories() > 0) {
                    activeCalories += amount.getTotalActiveCalories() / 1000;
                }
            }
            Calendar d = (Calendar) day.clone();
            daysData.add(new CaloriesDay(d, activeCalories, restingCalories));
            day.add(Calendar.DATE, 1);
        }
        return daysData;
    }

    protected RestingMetabolicRateSample getRestingMetabolicRate(DBHandler db, GBDevice device) {
        TimeSampleProvider<? extends RestingMetabolicRateSample> provider = device.getDeviceCoordinator().getRestingMetabolicRateProvider(device, db.getDaoSession());
        RestingMetabolicRateSample latestSample = provider.getLatestSample();
        if (latestSample != null) {
            return latestSample;
        }
        DefaultRestingMetabolicRateProvider defaultProvider = new DefaultRestingMetabolicRateProvider(device, db.getDaoSession());
        return defaultProvider.getLatestSample();
    }

    protected ActivityAmounts getActivityAmountsForDay(DBHandler db, Calendar day, GBDevice device) {
        LimitedQueue<Integer, ActivityAmounts> activityAmountCache = null;
        ActivityAmounts amounts = null;

        Activity activity = getActivity();
        int key = (int) (day.getTimeInMillis() / 1000);
        if (activity != null) {
            activityAmountCache = ((ActivityChartsActivity) activity).mActivityAmountCache;
            amounts = activityAmountCache.lookup(key);
        }

        if (amounts == null) {
            ActivityAnalysis analysis = new ActivityAnalysis();
            amounts = analysis.calculateActivityAmounts(getSamplesOfDay(db, day, 0, device));
            if (activityAmountCache != null) {
                activityAmountCache.add(key, amounts);
            }
        }

        return amounts;
    }

    protected List<? extends ActivitySample> getSamplesOfDay(DBHandler db, Calendar day, int offsetHours, GBDevice device) {
        int startTs;
        int endTs;

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, offsetHours);

        startTs = (int) (day.getTimeInMillis() / 1000);
        endTs = startTs + 24 * 60 * 60 - 1;

        return getSamples(db, device, startTs, endTs);
    }

    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = device.getDeviceCoordinator().getSampleProvider(device, db.getDaoSession());
        return provider.getAllActivitySamples(tsFrom, tsTo);
    }

    protected static class CaloriesDay {
        public long activeCalories;
        public long restingCalories;
        public Calendar day;

        protected CaloriesDay(Calendar day, long activeCalories, long restingCalories) {
            this.activeCalories = activeCalories;
            this.restingCalories = restingCalories;
            this.day = day;
        }
    }


}
