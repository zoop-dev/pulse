/*  Copyright (C) 2016-2024 Andreas Shimokawa, Andrzej Surowiec, Arjan
    Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Ganblejs,
    gfwilliams, Gordon Williams, Johannes Tysiak, José Rebelo, marco.altomonte,
    Petr Vaněk, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.activities.views.PulseRefreshLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.debug.DebugActivityV2;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.activities.welcome.WelcomeActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBChangeLog;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils;

//TODO: extend AbstractGBActivity, but it requires actionbar that is not available
public class ControlCenterv2 extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ControlCenterv2.class);
    public static final int MENU_REFRESH_CODE = 1;
    private boolean isLanguageInvalid = false;
    private boolean isThemeInvalid = false;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;
    private PulseRefreshLayout swipeLayout;
    private ProgressBar syncBar;
    private AlertDialog clDialog;
    private boolean weatherSentForConnection = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBApplication.ACTION_LANGUAGE_CHANGE:
                    setLanguage(GBApplication.getLanguage(), true);
                    break;
                case GBApplication.ACTION_THEME_CHANGE:
                    isThemeInvalid = true;
                    break;
                case GBApplication.ACTION_QUIT:
                    finish();
                    break;
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    handleRealtimeSample(device, intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null && !dev.isBusy()) {
                        stopSyncUi();
                    }
                    // Pulse: push weather as soon as the watch finishes connecting (once per connection)
                    if (dev != null && dev.isInitialized()) {
                        if (!weatherSentForConnection) {
                            weatherSentForConnection = true;
                            GBApplication.deviceService().onSendWeather();
                            nodomain.freeyourgadget.gadgetbridge.util.PulseWeather.maybeFetch(ControlCenterv2.this);
                        }
                    } else {
                        weatherSentForConnection = false;
                    }
                    updateToolbarDevice();
                    break;
            }
        }
    };
    private boolean pesterWithPermissions = true;
    private final Map<GBDevice, ActivitySample> currentHRSample = new HashMap<>();

    public ActivitySample getCurrentHRSample(final GBDevice device) {
        return currentHRSample.get(device);
    }

    private void setCurrentHRSample(final GBDevice device, ActivitySample sample) {
        if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate())) {
            currentHRSample.put(device, sample);
        }
    }

    private void handleRealtimeSample(final GBDevice device, Serializable extra) {
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            setCurrentHRSample(device, sample);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pulse: (re)arm the Sunday-evening weekly recap notification.
        nodomain.freeyourgadget.gadgetbridge.util.PulseWeeklyRecapReceiver.schedule(this);

        GBPrefs prefs = GBApplication.getPrefs();

        // Pulse: first-run welcome.
        if (!prefs.getBoolean(PulseOnboardingActivity.PREF_ONBOARDED, false)) {
            startActivity(new Intent(this, PulseOnboardingActivity.class));
        }

        // Determine availability of device with activity tracking functionality
        boolean activityTrackerAvailable = false;
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice dev : devices) {
            if (dev.getDeviceCoordinator().supportsActivityTracking(dev)) {
                activityTrackerAvailable = true;
                break;
            }
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("cl")) {
                GBChangeLog cl = GBChangeLog.createChangeLog(this);
                try {
                    if (cl.hasChanges(false)) {
                        clDialog = cl.getMaterialLogDialog();
                    } else {
                        clDialog = cl.getMaterialFullLogDialog();
                    }
                    clDialog.show();
                } catch (Exception ex) {
                    GB.toast(getBaseContext(), getString(R.string.error_showing_changelog), Toast.LENGTH_LONG, GB.ERROR, ex);
                }
            }
        }

        // Initialize drawer
        NavigationView drawerNavigationView = findViewById(R.id.nav_view);
        drawerNavigationView.setNavigationItemSelectedListener(this);

        View navigationHeaderView = drawerNavigationView.getHeaderView(0);
        ViewCompat.setOnApplyWindowInsetsListener(navigationHeaderView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(view.getPaddingLeft(), insets.top, view.getPaddingRight(), view.getPaddingBottom());
            return windowInsets;
        });

        // Initialize bottom navigation
        BottomNavigationView navigationView = findViewById(R.id.bottom_nav_bar);
        // Pulse: the 4-tab Google-Health style nav is always available
        if (prefs.getBoolean("display_bottom_navigation_bar", true)) {
            navigationView.setVisibility(View.VISIBLE);
        } else {
            navigationView.setVisibility(View.GONE);
        }
        navigationView.setOnItemSelectedListener(menuItem -> {
            final int itemId = menuItem.getItemId();
            if (itemId == R.id.bottom_nav_today) {
                viewPager.setCurrentItem(0, true);
            } else if (itemId == R.id.bottom_nav_fitness) {
                viewPager.setCurrentItem(1, true);
            } else if (itemId == R.id.bottom_nav_sleep) {
                viewPager.setCurrentItem(2, true);
            } else if (itemId == R.id.bottom_nav_health) {
                viewPager.setCurrentItem(3, true);
            }
            return true;
        });

        // Initialize actionbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Pulse: styled device chip in the toolbar instead of a plain title
        toolbar.setTitle("");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        final View deviceChip = getLayoutInflater().inflate(R.layout.toolbar_device_chip, toolbar, false);
        deviceChip.setOnClickListener(v -> startActivity(new Intent(this, DevicesActivity.class)));
        // Pulse: long-press the chip → watch battery history.
        deviceChip.setOnLongClickListener(v -> {
            final GBDevice dev = pulseFirstDevice();
            if (dev != null) {
                final Intent i = new Intent(this, BatteryInfoActivity.class);
                i.putExtra(GBDevice.EXTRA_DEVICE, dev);
                startActivity(i);
            }
            return true;
        });
        toolbar.addView(deviceChip);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.controlcenter_navigation_drawer_open, R.string.controlcenter_navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        if (GBApplication.areDynamicColorsEnabled()) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
            @ColorInt int toolbarBackground = typedValue.data;
            toolbar.setBackgroundColor(toolbarBackground);
        } else {
            // Pulse: flat near-black app bar to match the dark theme
            toolbar.setBackgroundColor(getResources().getColor(R.color.pulse_bg));
            toolbar.setTitleTextColor(getResources().getColor(R.color.pulse_text));
        }
        // Pulse: blend the status bar into the near-black background
        getWindow().setStatusBarColor(getResources().getColor(R.color.pulse_bg));
        updateToolbarDevice();

        // Configure ViewPager2 with fragment adapter and default fragment
        viewPager = findViewById(R.id.dashboard_viewpager);
        pagerAdapter = new MainFragmentsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        // Pulse: preload all 4 tabs so switching is instant (stats warm off the UI thread now)
        viewPager.setOffscreenPageLimit(3);
        // Always land on the Today tab

        // Sync ViewPager changes with BottomNavigationView
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private MenuProvider existingMenuProvider = null;

            @Override
            public void onPageSelected(int position) {
                navigationView.getMenu().getItem(position).setChecked(true);

                // Ensure the menu provider is set to the current fragment
                if (existingMenuProvider != null) {
                    ControlCenterv2.this.removeMenuProvider(existingMenuProvider);
                }
                final Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
                if (fragment instanceof MenuProvider) {
                    existingMenuProvider = (MenuProvider) fragment;
                    ControlCenterv2.this.addMenuProvider(existingMenuProvider);
                }
            }
        });

        // Make sure the SwipeRefreshLayout doesn't interfere with the ViewPager2
        viewPager.getChildAt(0).setOnTouchListener((v, event) -> {
            if (prefs.refreshOnSwipe()) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    swipeLayout.setEnabled(false);
                } else {
                    swipeLayout.setEnabled(true);
                }
            }
            return false;
        });

        // Set pull-down-to-refresh action
        swipeLayout = findViewById(R.id.dashboard_swipe_layout);
        syncBar = findViewById(R.id.pulse_sync_bar);
        swipeLayout.setSyncBar(syncBar);
        // Pulse: hide the default circular spinner — content follows the finger + a thick top bar
        swipeLayout.setColorSchemeColors(Color.TRANSPARENT);
        swipeLayout.setProgressBackgroundColorSchemeColor(Color.TRANSPARENT);
        swipeLayout.setEnabled(prefs.refreshOnSwipe());
        swipeLayout.setOnRefreshListener(() -> {
            if (prefs.refreshOnSwipe()) {
                List<GBDevice> devices1 = GBApplication.app().getDeviceManager().getDevices();
                final boolean anyConnected = devices1.stream().anyMatch(GBDevice::isInitialized);
                if (!anyConnected) {
                    // No devices are connected at all
                    GB.toast(getString(R.string.info_no_devices_connected), Toast.LENGTH_LONG, GB.WARN);
                    stopSyncUi();
                    return;
                }
                startSyncUi();
                // Fetch activity for all connected devices
                GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);

                // Hide 'refreshing' animation immediately if no devices are connected that support sync
                final boolean anySupported = devices1.stream().filter(GBDevice::isInitialized)
                        .anyMatch(dev -> dev.getDeviceCoordinator().supportsDataFetching(dev));
                if (!anySupported) {
                    stopSyncUi();
                    GB.toast(getString(R.string.info_no_devices_to_sync), Toast.LENGTH_LONG, GB.WARN);
                }
            } else {
                stopSyncUi();
            }
        });

        // Set up local intent listener
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_LANGUAGE_CHANGE);
        filterLocal.addAction(GBApplication.ACTION_THEME_CHANGE);
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        // Open the Welcome flow on first run, only check permissions on next runs
        boolean firstRun = prefs.getBoolean("first_run", true);
        if (firstRun) {
            launchWelcomeActivity();
        } else {
            pesterWithPermissions = prefs.getBoolean("permission_pestering", true);
            if (pesterWithPermissions && !PermissionsUtils.checkAllPermissions(this)) {
                Intent permissionsIntent = new Intent(this, PermissionsActivity.class);
                permissionsIntent.putExtra(PermissionsActivity.ARG_SHOW_DO_NOT_ASK_BUTTON, true);
                startActivity(permissionsIntent);
            }
        }

        GBChangeLog cl = GBChangeLog.createChangeLog(this);
        boolean showChangelog = prefs.getBoolean("show_changelog", true);
        if (showChangelog && cl.isFirstRun() && cl.hasChanges(cl.isFirstRunEver())) {
            try {
                cl.getMaterialLogDialog().show();
            } catch (Exception ex) {
                GB.toast(this, getString(R.string.error_showing_changelog), Toast.LENGTH_LONG, GB.ERROR, ex);
            }
        }

        GBApplication.deviceService().requestDeviceInfo();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (clDialog != null) {
            outState.putBoolean("cl", clDialog.isShowing());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleShortcut(getIntent());
        // Pulse: keep the watch's weather fresh (Open-Meteo, throttled)
        nodomain.freeyourgadget.gadgetbridge.util.PulseWeather.maybeFetch(this);
        if (isLanguageInvalid || isThemeInvalid) {
            isLanguageInvalid = false;
            isThemeInvalid = false;
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /** Pulse: the device to scope device-specific screens to (first connected, else first known). */
    private GBDevice pulseFirstDevice() {
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (final GBDevice d : devices) {
            if (d.isInitialized()) {
                return d;
            }
        }
        return devices.isEmpty() ? null : devices.get(0);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        final int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            final Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, MENU_REFRESH_CODE);
            return false;
        } else if (itemId == R.id.pulse_weather) {
            startActivity(new Intent(this, PulseWeatherActivity.class));
            return false;
        } else if (itemId == R.id.pulse_workouts) {
            startActivity(new Intent(this, PulseWorkoutsActivity.class));
            return false;
        } else if (itemId == R.id.pulse_week) {
            startActivity(new Intent(this, PulseWeekActivity.class));
            return false;
        } else if (itemId == R.id.pulse_vo2max) {
            final GBDevice dev = pulseFirstDevice();
            if (dev != null) {
                final Intent i = new Intent(this, nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity.class);
                i.putExtra(GBDevice.EXTRA_DEVICE, dev);
                i.putExtra(nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity.EXTRA_SINGLE_FRAGMENT_NAME, "vo2max");
                startActivity(i);
            }
            return false;
        } else if (itemId == R.id.pulse_alarms) {
            final GBDevice dev = pulseFirstDevice();
            if (dev != null) {
                final Intent i = new Intent(this, ConfigureAlarms.class);
                i.putExtra(GBDevice.EXTRA_DEVICE, dev);
                startActivity(i);
            }
            return false;
        } else if (itemId == R.id.pulse_calendar) {
            final GBDevice dev = pulseFirstDevice();
            if (dev != null) {
                final Intent i = new Intent(this, CalendarSelectionActivity.class);
                i.putExtra(GBDevice.EXTRA_DEVICE, dev);
                startActivity(i);
            }
            return false;
        } else if (itemId == R.id.action_debug) {
            final Intent debugIntent = new Intent(this, DebugActivityV2.class);
            startActivity(debugIntent);
            return false;
        } else if (itemId == R.id.action_data_management) {
            final Intent dbIntent = new Intent(this, DataManagementActivity.class);
            startActivity(dbIntent);
            return false;
        } else if (itemId == R.id.device_action_discover) {
            launchDiscoveryActivity();
            return false;
        } else if (itemId == R.id.action_quit) {
            GBApplication.quit();
            return false;
        } else if (itemId == R.id.donation_link) {
            final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://liberapay.com/Gadgetbridge")); //TODO: centralize if ever used somewhere else
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return false;
        } else if (itemId == R.id.external_changelog) {
            final GBChangeLog cl = GBChangeLog.createChangeLog(this);
            try {
                if (cl.hasChanges(false)) {
                    clDialog = cl.getMaterialLogDialog();
                } else {
                    clDialog = cl.getMaterialFullLogDialog();
                }
                clDialog.show();
            } catch (Exception ex) {
                GB.toast(getBaseContext(), getString(R.string.error_showing_changelog), Toast.LENGTH_LONG, GB.ERROR, ex);
            }
            return false;
        } else if (itemId == R.id.about) {
            final Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return false;
        }

        return false;  // we do not want the drawer menu item to get selected
    }


    /** Pulse: content drops and the thick top bar fills while syncing (no spinner). */
    private void startSyncUi() {
        if (swipeLayout != null) {
            swipeLayout.onSyncStarted();
        }
    }

    private void stopSyncUi() {
        if (swipeLayout != null) {
            swipeLayout.setRefreshing(false);
            swipeLayout.onSyncFinished();
        }
    }

    /** Pulse: update the styled device + battery chip in the toolbar. */
    private void updateToolbarDevice() {
        final TextView name = findViewById(R.id.toolbar_device_name);
        final TextView battery = findViewById(R.id.toolbar_device_battery);
        final View dot = findViewById(R.id.toolbar_device_dot);
        if (name == null) {
            return;
        }
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (devices.isEmpty()) {
            name.setText(R.string.app_name);
            battery.setText("");
            if (dot != null) dot.setAlpha(0.3f);
            return;
        }
        final GBDevice dev = devices.get(0);
        name.setText(dev.getAliasOrName());
        if (dot != null) dot.setAlpha(dev.isInitialized() ? 1f : 0.3f);
        final int batt = dev.getBatteryLevel();
        battery.setText((batt >= 0 && batt <= 100) ? batt + "%" : "");
    }

    private void launchWelcomeActivity() {
        startActivity(new Intent(this, WelcomeActivity.class));
    }

    private void launchDiscoveryActivity() {
        startActivity(new Intent(this, DiscoveryActivityV2.class));
    }

    private void handleShortcut(Intent intent) {
        if (ACTION_CONNECT.equals(intent.getAction())) {
            String btDeviceAddress = intent.getStringExtra("device");
            if (btDeviceAddress != null) {
                GBDevice candidate = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress);
                if (candidate != null && !candidate.isConnected()) {
                    GBApplication.deviceService(candidate).connect();
                }
            }
        }
    }

    @Override
    public void setLanguage(Locale language, boolean invalidateLanguage) {
        if (invalidateLanguage) {
            isLanguageInvalid = true;
        }
        AndroidUtils.setLanguage(this, language);
    }

    private class MainFragmentsPagerAdapter extends FragmentStateAdapter {
        public MainFragmentsPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:  return DashboardFragment.newInstance("fitness");
                case 2:  return DashboardFragment.newInstance("sleep");
                case 3:  return DashboardFragment.newInstance("health");
                default: return DashboardFragment.newInstance("today");
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
