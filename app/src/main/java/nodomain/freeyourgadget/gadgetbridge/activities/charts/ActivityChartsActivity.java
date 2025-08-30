/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ActivityChartsActivity extends AbstractGBActivity implements ChartsHost {
    private static final Logger LOG = LoggerFactory.getLogger(ActivityChartsActivity.class);

    public static final String STATE_START_DATE = "stateStartDate";
    public static final String STATE_END_DATE = "stateEndDate";

    public static final String EXTRA_FRAGMENT_ID = "fragmentId";
    public static final String EXTRA_SINGLE_FRAGMENT_NAME = "singleFragmentName";
    public static final String EXTRA_ACTIONBAR_TITLE = "actionbarTitle";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_MODE = "mode";

    private TextView mDateControl;

    private Date mStartDate;
    private Date mEndDate;
    private SwipeRefreshLayout swipeLayout;
    LimitedQueue<Integer, ActivityAmounts> mActivityAmountCache = new LimitedQueue<>(60);

    List<String> enabledTabsList;

    private GBDevice mGBDevice;
    private ViewGroup dateBar;

    private ActivityResultLauncher<Intent> chartsPreferencesLauncher;
    private final ActivityResultCallback<ActivityResult> chartsPreferencesCallback = result -> {
        recreate();
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            //noinspection SwitchStatementWithTooFewBranches
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                    final GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null) {
                        refreshBusyState(dev);
                    }
                    break;
            }
        }
    };

    private void refreshBusyState(final GBDevice dev) {
        if (dev.isBusy()) {
            swipeLayout.setRefreshing(true);
        } else {
            final boolean wasBusy = swipeLayout.isRefreshing();
            swipeLayout.setRefreshing(false);
            if (wasBusy) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(REFRESH));
            }
        }
        enableSwipeRefresh(true);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);

        chartsPreferencesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                chartsPreferencesCallback
        );

        // Set start and end date
        if (savedInstanceState != null) {
            setEndDate(new Date(savedInstanceState.getLong(STATE_END_DATE, System.currentTimeMillis())));
        } else if (extras.containsKey(EXTRA_TIMESTAMP)) {
            final int endTimestamp = extras.getInt(EXTRA_TIMESTAMP, 0);
            setEndDate(new Date(endTimestamp * 1000L));
        } else {
            setEndDate(new Date());
        }
        setStartDate(DateTimeUtils.shiftByDays(getEndDate(), -1));

        final IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        // Open the specified fragment, if any, and setup single page view if specified
        final int tabFragmentIdToOpen = extras.getInt(EXTRA_FRAGMENT_ID, -1);
        final String singleFragmentName = extras.getString(EXTRA_SINGLE_FRAGMENT_NAME, null);
        final int actionbarTitle = extras.getInt(EXTRA_ACTIONBAR_TITLE, 0);

        if (tabFragmentIdToOpen >= 0 && singleFragmentName != null) {
            throw new IllegalArgumentException("Must specify either fragment ID or single fragment name, not both");
        }

        if (singleFragmentName != null) {
            enabledTabsList = Collections.singletonList(singleFragmentName);
        } else {
            enabledTabsList = fillChartsTabsList();
        }

        swipeLayout = findViewById(R.id.activity_swipe_layout);
        swipeLayout.setOnRefreshListener(this::fetchRecordedData);
        enableSwipeRefresh(true);

        // Set up the ViewPager with the sections adapter.
        final ViewPager2 viewPager = findViewById(R.id.charts_pager);
        TabLayout tabLayout = findViewById(R.id.charts_pagerTabStrip);

        viewPager.setAdapter(getPagerAdapter());

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
            tab.setText(getPageTitle(position));
                }).attach();

        if (tabFragmentIdToOpen > -1) {
            viewPager.setCurrentItem(tabFragmentIdToOpen);  // open the tab as specified in the intent
        }

        viewPager.setUserInputEnabled(singleFragmentName == null && GBApplication.getPrefs().getBoolean("charts_allow_swipe", true));

        if (singleFragmentName != null) {
            tabLayout.setVisibility(TextView.GONE);
        }

        if (actionbarTitle != 0) {
            final ActionBar actionBar = getSupportActionBar();

            if (actionBar != null) {
                actionBar.setTitle(actionbarTitle);
            }
        }

        dateBar = findViewById(R.id.charts_date_bar);
        mDateControl = findViewById(R.id.charts_text_date);
        mDateControl.setOnClickListener(v -> {
            String detailedDuration = formatDetailedDuration();
            new ShowDurationDialog(detailedDuration, ActivityChartsActivity.this).show();
        });

        final Button mPrevButton = findViewById(R.id.charts_previous_day);
        mPrevButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_DAY));
        final Button mNextButton = findViewById(R.id.charts_next_day);
        mNextButton.setOnClickListener(v -> handleButtonClicked(DATE_NEXT_DAY));

        final Button mPrevWeekButton = findViewById(R.id.charts_previous_week);
        mPrevWeekButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_WEEK));
        final Button mNextWeekButton = findViewById(R.id.charts_next_week);
        mNextWeekButton.setOnClickListener(v -> handleButtonClicked(DATE_NEXT_WEEK));

        final Button mPrevMonthButton = findViewById(R.id.charts_previous_month);
        mPrevMonthButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_MONTH));
        final Button mNextMonthButton = findViewById(R.id.charts_next_month);
        mNextMonthButton.setOnClickListener(v -> handleButtonClicked(DATE_NEXT_MONTH));
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(STATE_END_DATE, getEndDate().getTime());
        outState.putLong(STATE_START_DATE, getStartDate().getTime());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        setEndDate(new Date(savedInstanceState.getLong(STATE_END_DATE, System.currentTimeMillis())));
        setStartDate(new Date(savedInstanceState.getLong(STATE_START_DATE, DateTimeUtils.shiftByDays(getEndDate(), -1).getTime())));
    }

    protected FragmentStateAdapter getPagerAdapter() {
        return new SectionsStateAdapter(this);
    }
    protected List<String> fillChartsTabsList() {
        return fillChartsTabsList(getDevice(), this);
    }

    private static List<String> fillChartsTabsList(final GBDevice device, final Context context) {
        final List<String> tabList;
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
        final String myTabs = prefs.getString(DeviceSettingsPreferenceConst.PREFS_DEVICE_CHARTS_TABS, null);

        if (myTabs == null) {
            //make list mutable to be able to remove items later
            tabList = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.pref_charts_tabs_items_default)));
        } else {
            tabList = new ArrayList<>(Arrays.asList(myTabs.split(",")));
        }
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!coordinator.supportsActivityTabs(device)) {
            tabList.remove("activity");
            tabList.remove("activitylist");
        }
        if (!coordinator.supportsSleepMeasurement(device)) {
            tabList.remove("sleep");
        }
        if (!coordinator.supportsStressMeasurement(device)) {
            tabList.remove("stress");
        }
        if (!coordinator.supportsPai(device)) {
            tabList.remove("pai");
        }
        if (!coordinator.supportsSpo2(device)) {
            tabList.remove("spo2");
        }
        if (!coordinator.supportsStepCounter(device)) {
            tabList.remove("stepsweek");
        }
        if (!coordinator.supportsSpeedzones(device)) {
            tabList.remove("speedzones");
        }
        if (!coordinator.supportsRealtimeData(device)) {
            tabList.remove("livestats");
        }
        if (!coordinator.supportsTemperatureMeasurement(device)) {
            tabList.remove("temperature");
        }
        if (!coordinator.supportsCyclingData(device)) {
            tabList.remove("cycling");
        }
        if (!coordinator.supportsWeightMeasurement(device)) {
            tabList.remove("weight");
        }
        if (!coordinator.supportsHrvMeasurement(device)) {
            tabList.remove("hrvstatus");
        }
        if (!coordinator.supportsHeartRateMeasurement(device)) {
            tabList.remove("heartrate");
        }
        if (!coordinator.supportsBodyEnergy(device)) {
            tabList.remove("bodyenergy");
        }
        if (!coordinator.supportsVO2Max(device)) {
            tabList.remove("vo2max");
        }
        if (!coordinator.supportsTrainingLoad(device)) {
            tabList.remove("load");
        }
        if (!coordinator.supportsActiveCalories(device)) {
            tabList.remove("calories");
        }
        if (!coordinator.supportsRespiratoryRate(device)) {
            tabList.remove("respiratoryrate");
        }
        return tabList;
    }

    public static int getChartsTabIndex(final String tab, final GBDevice device, final Context context) {
        final List<String> enabledTabsList = fillChartsTabsList(device, context);
        return enabledTabsList.indexOf(tab);
    }

    private String formatDetailedDuration() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        final String dateStringFrom = dateFormat.format(getStartDate());
        final String dateStringTo = dateFormat.format(getEndDate());

        return getString(R.string.sleep_activity_date_range, dateStringFrom, dateStringTo);
    }

    protected void initDates() {
        setEndDate(new Date());
        setStartDate(DateTimeUtils.shiftByDays(getEndDate(), -1));
    }

    @Override
    public GBDevice getDevice() {
        return mGBDevice;
    }

    @Override
    public void setStartDate(Date startDate) {
        mStartDate = startDate;
    }

    @Override
    public void setEndDate(Date endDate) {
        mEndDate = endDate;
    }

    @Override
    public Date getStartDate() {
        return mStartDate;
    }

    @Override
    public Date getEndDate() {
        return mEndDate;
    }

    @Override
    public void setDateInfo(final String dateInfo) {
        mDateControl.setText(dateInfo);
    }

    @Override
    public ViewGroup getDateBar() {
        return dateBar;
    }

    private void handleButtonClicked(final String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_charts, menu);

        if (!mGBDevice.isConnected() || !supportsRefresh()) {
            menu.removeItem(R.id.charts_fetch_activity_data);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.charts_fetch_activity_data) {
            fetchRecordedData();
            return true;
        } else if (itemId == R.id.charts_set_date) {
            final Calendar currentDate = Calendar.getInstance();
            currentDate.setTime(getEndDate());
            new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
                currentDate.set(year, monthOfYear, dayOfMonth);
                setEndDate(currentDate.getTime());
                setStartDate(DateTimeUtils.shiftByDays(getEndDate(), -1));
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(REFRESH));
            }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
        } else if (itemId == R.id.prefs_charts_menu) {
            final Intent settingsIntent = new Intent(this, ChartsPreferencesActivity.class);
            chartsPreferencesLauncher.launch(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void enableSwipeRefresh(final boolean enable) {
        final boolean refreshOnSwipe = GBApplication.getPrefs().refreshOnSwipe();
        swipeLayout.setEnabled(enable && refreshOnSwipe && allowRefresh());
    }

    protected boolean supportsRefresh() {
        final DeviceCoordinator coordinator = getDevice().getDeviceCoordinator();
        return coordinator.supportsActivityDataFetching(getDevice());
    }

    protected boolean allowRefresh() {
        final DeviceCoordinator coordinator = getDevice().getDeviceCoordinator();
        return coordinator.allowFetchActivityData(getDevice()) && supportsRefresh();
    }

    protected int getRecordedDataType() {
        return RecordedDataTypes.TYPE_ACTIVITY | RecordedDataTypes.TYPE_STRESS;
    }
    private void fetchRecordedData() {
        if (getDevice().isInitialized()) {
            GBApplication.deviceService(getDevice()).onFetchRecordedData(getRecordedDataType());
        } else {
            swipeLayout.setRefreshing(false);
            GB.toast(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    public CharSequence getPageTitle(int position) {
        switch (enabledTabsList.get(position)) {
            case "activity":
                return getString(R.string.activity_sleepchart_activity_and_sleep);
            case "activitylist":
                return getString(R.string.charts_activity_list);
            case "sleep":
                return getString(R.string.sleepchart_your_sleep);
            case "heartrate":
                return getString(R.string.menuitem_hr);
            case "hrvstatus":
                return getString(R.string.pref_header_hrv_status);
            case "bodyenergy":
                return getString(R.string.body_energy);
            case "vo2max":
                return getString(R.string.menuitem_vo2_max);
            case "stress":
                return getString(R.string.menuitem_stress);
            case "pai":
                return getString(getDevice().getDeviceCoordinator().getPaiName());
            case "stepsweek":
                return getString(R.string.steps);
            case "speedzones":
                return getString(R.string.stats_title);
            case "livestats":
                return getString(R.string.liveactivity_live_activity);
            case "spo2":
                return getString(R.string.pref_header_spo2);
            case "temperature":
                return getString(R.string.menuitem_temperature);
            case "cycling":
                return getString(R.string.title_cycling);
            case "weight":
                return getString(R.string.menuitem_weight);
            case "calories":
                return getString(R.string.calories);
            case "respiratoryrate":
                return getString(R.string.respiratoryrate);
            case "load":
                return getString(R.string.pref_header_training_load);
        }

        return String.format(Locale.getDefault(), "Unknown %d", position);
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsStateAdapter extends FragmentStateAdapter {
        SectionsStateAdapter(FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            final DeviceCoordinator coordinator = getDevice().getDeviceCoordinator();
            // getItem is called to instantiate the fragment for the given page.
            switch (enabledTabsList.get(position)) {
                case "activity":
                    return new ActivitySleepChartFragment();
                case "activitylist":
                    return new ActivityListingChartFragment();
                case "sleep":
                    return SleepCollectionFragment.newInstance(enabledTabsList.size() == 1);
                case "heartrate":
                    return HeartRateCollectionFragment.newInstance(enabledTabsList.size() == 1);
                case "hrvstatus":
                    return new HRVStatusFragment();
                case "bodyenergy":
                    return new BodyEnergyFragment();
                case "vo2max":
                    return new VO2MaxFragment();
                case "load":
                    return new LoadFragment();
                case "stress":
                    return StressCollectionFragment.newInstance(enabledTabsList.size() == 1);
                case "pai":
                    return new PaiChartFragment();
                case "stepsweek":
                    return StepsCollectionFragment.newInstance(enabledTabsList.size() == 1);
                case "speedzones":
                    return new SpeedZonesFragment();
                case "livestats":
                    return new LiveActivityFragment();
                case "spo2":
                    return new Spo2ChartFragment();
                case "temperature":
                    return coordinator.supportsContinuousTemperature(getDevice())? new TemperatureDailyFragment(): new TemperatureChartFragment();
                case "cycling":
                    return new CyclingChartFragment();
                case "weight":
                    return new WeightChartFragment();
                case "calories":
                    return CaloriesCollectionFragment.newInstance(enabledTabsList.size() == 1);
                case "respiratoryrate":
                    return RespiratoryRateCollectionFragment.newInstance(enabledTabsList.size() == 1);
            }

            return new ActivityChartsActivity.UnknownFragment();
        }

        @Override
        public int getItemCount() {
            return enabledTabsList.size();
        }

    }

    /**
     * A dummy fragment to avoid a crash when we get a unknown tab position (eg. broken
     * preference migration).
     */
    public static class UnknownFragment extends AbstractGBFragment {
        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater,
                                 final ViewGroup container,
                                 final Bundle savedInstanceState) {
            return null;
        }

        @Nullable
        @Override
        protected CharSequence getTitle() {
            return "Unknown";
        }
    }
}
