/*  Copyright (C) 2015-2025 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Damien Gaignon, Daniel Dakhno, Daniele Gobbetti, Davis Mosenkovs,
    Dmitriy Bogdanov, Joel Beckmeyer, José Rebelo, Kornél Schmidt, Ludovic
    Jozeau, Martin, Martin.JM, mvn23, Normano64, odavo32nof, Pauli Salmenrinne,
    Pavel Elagin, Petr Vaněk, Saul Nunez, Taavi Eomäe, x29a, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_ID_ERROR;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import androidx.lifecycle.ProcessLifecycleOwner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicDbExporter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothStateChangeReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksContentObserver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.weather.WeatherCacheManager;
import nodomain.freeyourgadget.gadgetbridge.prefs.GBPrefsMigrator;
import nodomain.freeyourgadget.gadgetbridge.service.NotificationCollectorMonitorService;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.InternetHelperSingleton;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.backup.PeriodicZipExporter;
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectPermissionManager;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

/**
 * Main Application class that initializes and provides access to certain things like
 * logging and DB access.
 */
public class GBApplication extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(GBApplication.class);

    private static GBApplication context;
    private static DeviceService deviceService;
    private static SharedPreferences sharedPrefs;

    private static final LimitedQueue<Integer, String> mIDSenderLookup = new LimitedQueue<>(16);
    private static GBPrefs prefs;
    /**
     * Note: is null on Lollipop
     */
    private static NotificationManager notificationManager;

    public static final String ACTION_QUIT
            = "nodomain.freeyourgadget.gadgetbridge.gbapplication.action.quit";
    public static final String ACTION_LANGUAGE_CHANGE = "nodomain.freeyourgadget.gadgetbridge.gbapplication.action.language_change";
    public static final String ACTION_THEME_CHANGE = "nodomain.freeyourgadget.gadgetbridge.gbapplication.action.theme_change";
    public static final String ACTION_NEW_DATA = "nodomain.freeyourgadget.gadgetbridge.action.new_data";
    public static final String ACTION_APP_IS_IN_FOREGROUND = "nodomain.freeyourgadget.gadgetbridge.action.app_foreground";
    public static final String ACTION_APP_IS_IN_BACKGROUND = "nodomain.freeyourgadget.gadgetbridge.action.app_background";

    private static GBApplication app;

    private static Locale language;

    private DeviceManager deviceManager;
    private BluetoothStateChangeReceiver bluetoothStateChangeReceiver;

    private OpenTracksContentObserver openTracksObserver;

    public static void quit() {
        LOG.info("Quitting Gadgetbridge...");
        BondingUtil.StopObservingAll(getContext());
        Intent quitIntent = new Intent(GBApplication.ACTION_QUIT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(quitIntent);
        GBApplication.deviceService().quit();
        GBDatabaseManager.closeDatabase();
        System.exit(0);
    }

    public static void restart() {
        LOG.info("Restarting Gadgetbridge...");
        BondingUtil.StopObservingAll(getContext());
        final Intent quitIntent = new Intent(GBApplication.ACTION_QUIT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(quitIntent);
        GBApplication.deviceService().quit();

        GBDatabaseManager.closeDatabase();

        final Intent startActivity = new Intent(context, ControlCenterv2.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1337,
                startActivity,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 1500, pendingIntent);

        Runtime.getRuntime().exit(0);
    }

    public GBApplication() {
        context = this;
        // don't do anything here, add it to onCreate instead

        if (BuildConfig.DEBUG) {
            StrictMode.ThreadPolicy.Builder thread = new StrictMode.ThreadPolicy.Builder();
            thread.detectCustomSlowCalls();
            thread.permitDiskReads(); // log requires disk access
            thread.permitDiskWrites(); // log requires disk access
            thread.detectNetwork();
            thread.detectResourceMismatches();
            if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                thread.detectUnbufferedIo();
            }
            if (VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                thread.detectExplicitGc();
            }
            StrictMode.setThreadPolicy(thread.penaltyLog().build());

            StrictMode.VmPolicy.Builder vm = new StrictMode.VmPolicy.Builder();
            vm.detectLeakedSqlLiteObjects();
            vm.detectLeakedClosableObjects();
            vm.detectLeakedRegistrationObjects();
            vm.detectFileUriExposure();
            vm.detectCleartextNetwork();
            if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vm.detectContentUriWithoutPermission();
                vm.detectUntaggedSockets();
            }
            if (VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // androidx.appcompat causes:
                // NonSdkApiUsedViolation: Landroid/view/ViewGroup;->makeOptionalFitsSystemWindows()V
                // vm.detectNonSdkApiUsage();
            }
            if (VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vm.detectImplicitDirectBoot();
                vm.detectCredentialProtectedWhileLocked();
            }
            if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                vm.detectIncorrectContextUse();
                vm.detectUnsafeIntentLaunch();
            }
            if (VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                vm.detectBlockedBackgroundActivityLaunch();
            }
            StrictMode.setVmPolicy(vm.penaltyLog().build());
        }
    }

    protected DeviceService createDeviceService() {
        return new GBDeviceService(this);
    }

    @Override
    public void onCreate() {
        if (app != null) {
            super.onCreate();
            // guard against multiple invocations (robolectric)
            return;
        }

        app = this;
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs = new GBPrefs(sharedPrefs);

        if (!GBEnvironment.isEnvironmentSetup()) {
            GBEnvironment.setupEnvironment(GBEnvironment.createDeviceEnvironment());
            // setup db after the environment is set up, but don't do it in test mode
            // in test mode, it's done individually, see TestBase
            GBDatabaseManager.setupDatabase(this);
        }

        Logging.getInstance().initialize(prefs.getBoolean("log_to_file", false));

        migratePrefsIfNeeded();

        setupExceptionHandler(prefs.getBoolean("crash_notification", isDebug()));

        registerActivityLifecycleCallbacks(new GBActivityLifecycleCallbacks());

        Weather.initializeCache(new WeatherCacheManager(getCacheDir(), prefs.getBoolean("cache_weather", true)));

        deviceManager = new DeviceManager(this);
        String language = prefs.getString("language", "default");
        setLanguage(language);

        deviceService = createDeviceService();
        loadAppsNotifBlackList();
        loadAppsPebbleBlackList();

        if (!GBEnvironment.env().isTest()) {
            PeriodicDbExporter.INSTANCE.scheduleNextExecution(context);
            PeriodicZipExporter.INSTANCE.scheduleNextExecution(context);
        }

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (isRunningOreoOrLater()) {
            bluetoothStateChangeReceiver = new BluetoothStateChangeReceiver();
            final IntentFilter bif = new IntentFilter();
            bif.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            if (isRunningPieOrLater())
                bif.addAction(BluetoothStateChangeReceiver.ANDROID_BLUETOOTH_DEVICE_ACTION_BATTERY_LEVEL_CHANGED);
            registerReceiver(bluetoothStateChangeReceiver, bif);
        }
        startNotificationCollectorMonitorService();

        BondingUtil.StartObservingAll(getBaseContext());

        if (prefs.getBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false)) {
            HealthConnectPermissionManager.checkAndRectifyPermissions(this);
        }

        // Ensure the InternetHelper is bound, so that it works on first usage
        InternetHelperSingleton.INSTANCE.ensureInternetHelperBound();
    }

    private void startNotificationCollectorMonitorService() {
        if (!prefs.getBoolean("prefs_key_enable_deprecated_notificationcollectormonitor", false)) {
            return;
        }
        LOG.info("Starting the deprecated NotificationCollectorMonitorService foreground service.");
        try {
            //the following will ensure the notification manager is kept alive
            Intent serviceIntent = new Intent(context, NotificationCollectorMonitorService.class);

            //ContextCompat starts a background service on android < O automatically despite the method name
            ContextCompat.startForegroundService(context, serviceIntent);

        } catch (IllegalStateException e) {
            String message = e.toString();
            final Intent instructionsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gadgetbridge.org/basics/topics/background-service/"));
            final PendingIntent pi = PendingIntent.getActivity(
                    context,
                    0,
                    instructionsIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );
            GB.notify(NOTIFICATION_ID_ERROR,
                    new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(getString(R.string.error_background_service))
                            .setContentText(getString(R.string.error_background_service_reason_truncated))
                            .setContentIntent(pi)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(getString(R.string.error_background_service_reason) + " \"" + message + "\""))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .build(), context);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_BACKGROUND) {
            if (!hasBusyDevice()) {
                DBHelper.clearSession();
            }
        }
    }

    /**
     * Returns true if at least a single device is busy, e.g synchronizing activity data
     * or something similar.
     * Note: busy is not the same as connected or initialized!
     */
    private boolean hasBusyDevice() {
        List<GBDevice> devices = getDeviceManager().getDevices();
        for (GBDevice device : devices) {
            if (device.isBusy()) {
                return true;
            }
        }
        return false;
    }

    private void setupExceptionHandler(final boolean notifyOnCrash) {
        final GBExceptionHandler handler = new GBExceptionHandler(Thread.getDefaultUncaughtExceptionHandler(), notifyOnCrash);
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    public static boolean minimizeNotification() {
        return prefs.getBoolean("minimize_priority", false);
    }

    public static Context getContext() {
        return context;
    }

    /**
     * Returns the facade for talking to devices. Devices are managed by
     * an Android Service and this facade provides access to its functionality.
     *
     * @return the facade for talking to the service/devices.
     */
    public static DeviceService deviceService() {
        return deviceService;
    }

    /**
     * Returns the facade for talking to a specific device. Devices are managed by
     * an Android Service and this facade provides access to its functionality.
     *
     * @return the facade for talking to the service/device.
     */
    public static DeviceService deviceService(GBDevice device) {
        return deviceService.forDevice(device);
    }

    /**
     * @see GBDatabaseManager#acquireWrite()
     */
    public static DBHandler acquireDB() throws GBException {
        return GBDatabaseManager.acquireWrite();
    }

    /**
     * @see GBDatabaseManager#acquireReadOnly()
     */
    public static DBHandler acquireDbReadOnly() throws GBException {
        return GBDatabaseManager.acquireReadOnly();
    }

    public static boolean isRunningNougatOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static boolean isRunningOreoOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static boolean isRunningTenOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    public static boolean isRedVelvetCakeOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    public static boolean isRunningTwelveOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static boolean isRunningTiramisuOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    public static boolean isRunningPieOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    private static boolean isPrioritySender(int prioritySenders, String number) {
        if (prioritySenders == Policy.PRIORITY_SENDERS_ANY) {
            return true;
        } else {
            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            String[] projection = new String[]{PhoneLookup._ID, PhoneLookup.STARRED};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            boolean exists = false;
            int starred = 0;
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    exists = true;
                    starred = cursor.getInt(cursor.getColumnIndexOrThrow(PhoneLookup.STARRED));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (prioritySenders == Policy.PRIORITY_SENDERS_CONTACTS && exists) {
                return true;
            } else if (prioritySenders == Policy.PRIORITY_SENDERS_STARRED && starred == 1) {
                return true;
            }
            return false;
        }
    }

    public static boolean isPriorityNumber(int priorityType, String number) {
        NotificationManager.Policy notificationPolicy = notificationManager.getNotificationPolicy();
        if (priorityType == Policy.PRIORITY_CATEGORY_MESSAGES) {
            if ((notificationPolicy.priorityCategories & Policy.PRIORITY_CATEGORY_MESSAGES) == Policy.PRIORITY_CATEGORY_MESSAGES) {
                return isPrioritySender(notificationPolicy.priorityMessageSenders, number);
            }
        } else if (priorityType == Policy.PRIORITY_CATEGORY_CALLS) {
            if ((notificationPolicy.priorityCategories & Policy.PRIORITY_CATEGORY_CALLS) == Policy.PRIORITY_CATEGORY_CALLS) {
                return isPrioritySender(notificationPolicy.priorityCallSenders, number);
            }
        }
        return false;
    }

    public static int getGrantedInterruptionFilter() {
        if (notificationManager.isNotificationPolicyAccessGranted()) {
            return notificationManager.getCurrentInterruptionFilter();
        }
        return NotificationManager.INTERRUPTION_FILTER_ALL;
    }

    private static HashSet<String> apps_notification_blacklist = null;

    public static boolean appIsNotifBlacklisted(String packageName) {
        if (apps_notification_blacklist == null) {
            LOG.info("appIsNotifBlacklisted: apps_notification_blacklist is null!");
        }
        return apps_notification_blacklist != null && apps_notification_blacklist.contains(packageName);
    }

    public static void setAppsNotifBlackList(Set<String> packageNames) {
        setAppsNotifBlackList(packageNames, sharedPrefs.edit());
    }

    public static void setAppsNotifBlackList(Set<String> packageNames, SharedPreferences.Editor editor) {
        if (packageNames == null) {
            LOG.info("Set null apps_notification_blacklist");
            apps_notification_blacklist = new HashSet<>();
        } else {
            apps_notification_blacklist = new HashSet<>(packageNames);
        }
        LOG.info("New apps_notification_blacklist has {} entries", apps_notification_blacklist.size());
        saveAppsNotifBlackList(editor);
    }

    private static void loadAppsNotifBlackList() {
        LOG.info("Loading apps_notification_blacklist");
        apps_notification_blacklist = (HashSet<String>) sharedPrefs.getStringSet(GBPrefs.PACKAGE_BLACKLIST, null); // lgtm [java/abstract-to-concrete-cast]
        if (apps_notification_blacklist == null) {
            apps_notification_blacklist = new HashSet<>();
        }
        LOG.info("Loaded apps_notification_blacklist has {} entries", apps_notification_blacklist.size());
    }

    private static void saveAppsNotifBlackList() {
        saveAppsNotifBlackList(sharedPrefs.edit());
    }

    private static void saveAppsNotifBlackList(SharedPreferences.Editor editor) {
        LOG.info("Saving apps_notification_blacklist with {} entries", apps_notification_blacklist.size());
        if (apps_notification_blacklist.isEmpty()) {
            editor.putStringSet(GBPrefs.PACKAGE_BLACKLIST, null);
        } else {
            Prefs.putStringSet(editor, GBPrefs.PACKAGE_BLACKLIST, apps_notification_blacklist);
        }
        editor.apply();
    }

    public static void addAppToNotifBlacklist(String packageName) {
        if (apps_notification_blacklist.add(packageName)) {
            saveAppsNotifBlackList();
        }
    }

    public static synchronized void removeFromAppsNotifBlacklist(String packageName) {
        LOG.info("Removing from apps_notification_blacklist: {}", packageName);
        apps_notification_blacklist.remove(packageName);
        saveAppsNotifBlackList();
    }

    private static HashSet<String> apps_pebblemsg_blacklist = null;

    public static boolean appIsPebbleBlacklisted(String sender) {
        if (apps_pebblemsg_blacklist == null) {
            LOG.info("appIsPebbleBlacklisted: apps_pebblemsg_blacklist is null!");
        }
        return apps_pebblemsg_blacklist != null && apps_pebblemsg_blacklist.contains(sender);
    }

    public static void setAppsPebbleBlackList(Set<String> packageNames) {
        setAppsPebbleBlackList(packageNames, sharedPrefs.edit());
    }

    public static void setAppsPebbleBlackList(Set<String> packageNames, SharedPreferences.Editor editor) {
        if (packageNames == null) {
            LOG.info("Set null apps_pebblemsg_blacklist");
            apps_pebblemsg_blacklist = new HashSet<>();
        } else {
            apps_pebblemsg_blacklist = new HashSet<>(packageNames);
        }
        LOG.info("New apps_pebblemsg_blacklist has {} entries", apps_pebblemsg_blacklist.size());
        saveAppsPebbleBlackList(editor);
    }

    private static void loadAppsPebbleBlackList() {
        LOG.info("Loading apps_pebblemsg_blacklist");
        apps_pebblemsg_blacklist = (HashSet<String>) sharedPrefs.getStringSet(GBPrefs.PACKAGE_PEBBLEMSG_BLACKLIST, null); // lgtm [java/abstract-to-concrete-cast]
        if (apps_pebblemsg_blacklist == null) {
            apps_pebblemsg_blacklist = new HashSet<>();
        }
        LOG.info("Loaded apps_pebblemsg_blacklist has {} entries", apps_pebblemsg_blacklist.size());
    }

    private static void saveAppsPebbleBlackList() {
        saveAppsPebbleBlackList(sharedPrefs.edit());
    }

    private static void saveAppsPebbleBlackList(SharedPreferences.Editor editor) {
        LOG.info("Saving apps_pebblemsg_blacklist with {} entries", apps_pebblemsg_blacklist.size());
        if (apps_pebblemsg_blacklist.isEmpty()) {
            editor.putStringSet(GBPrefs.PACKAGE_PEBBLEMSG_BLACKLIST, null);
        } else {
            Prefs.putStringSet(editor, GBPrefs.PACKAGE_PEBBLEMSG_BLACKLIST, apps_pebblemsg_blacklist);
        }
        editor.apply();
    }

    public static void addAppToPebbleBlacklist(String packageName) {
        if (apps_pebblemsg_blacklist.add(packageNameToPebbleMsgSender(packageName))) {
            saveAppsPebbleBlackList();
        }
    }

    public static synchronized void removeFromAppsPebbleBlacklist(String packageName) {
        LOG.info("Removing from apps_pebblemsg_blacklist: {}", packageName);
        apps_pebblemsg_blacklist.remove(packageNameToPebbleMsgSender(packageName));
        saveAppsPebbleBlackList();
    }

    public static String packageNameToPebbleMsgSender(String packageName) {
        if ("eu.siacs.conversations".equals(packageName)) {
            return ("Conversations");
        } else if ("net.osmand.plus".equals(packageName)) {
            return ("OsmAnd");
        }
        return packageName;
    }

    @VisibleForTesting
    protected void migratePrefsIfNeeded() {
        GBPrefsMigrator.migratePrefsIfNeeded(sharedPrefs);
    }

    public static SharedPreferences getDeviceSpecificSharedPrefs(CharSequence deviceIdentifier) {
        if (deviceIdentifier == null || deviceIdentifier.length() < 1) {
            return null;
        }
        return context.getSharedPreferences("devicesettings_" + deviceIdentifier.toString().toUpperCase(Locale.ROOT), Context.MODE_PRIVATE);
    }

    public static DevicePrefs getDevicePrefs(GBDevice gbDevice) {
        return new DevicePrefs(getDeviceSpecificSharedPrefs(gbDevice.getAddress()), gbDevice);
    }

    public static void deleteDeviceSpecificSharedPrefs(CharSequence deviceIdentifier) {
        if (deviceIdentifier == null || deviceIdentifier.length() < 1) {
            return;
        }
        context.getSharedPreferences("devicesettings_" + deviceIdentifier.toString().toUpperCase(Locale.ROOT), Context.MODE_PRIVATE).edit().clear().apply();
    }


    public static void setLanguage(String lang) {
        if (lang.equals("default")) {
            language = Resources.getSystem().getConfiguration().locale;
        } else if (lang.length() == 2) {
            language = new Locale(lang);
        } else {
            final String[] split = lang.split("_");
            if (split.length == 2) {
                language = new Locale(split[0], split[1]);
            } else {
                // Unexpected format, fallback to system default
                language = Resources.getSystem().getConfiguration().locale;
            }
        }
        updateLanguage(language);
    }

    public static void updateLanguage(Locale locale) {
        AndroidUtils.setLanguage(context, locale);

        Intent intent = new Intent();
        intent.setAction(ACTION_LANGUAGE_CHANGE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static LimitedQueue<Integer, String> getIDSenderLookup() {
        return mIDSenderLookup;
    }

    public static boolean isDarkThemeEnabled() {
        String selectedTheme = prefs.getString("pref_key_theme", context.getString(R.string.pref_theme_value_system));
        Resources resources = context.getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                (selectedTheme.equals(context.getString(R.string.pref_theme_value_system)) || selectedTheme.equals(context.getString(R.string.pref_theme_value_dynamic)))) {
            return (resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        } else {
            return selectedTheme.equals(context.getString(R.string.pref_theme_value_dark));
        }
    }

    public static boolean isAmoledBlackEnabled() {
        return prefs.getBoolean("pref_key_theme_amoled_black", false);
    }

    public static boolean areDynamicColorsEnabled() {
        String selectedTheme = prefs.getString("pref_key_theme", context.getString(R.string.pref_theme_value_system));
        return selectedTheme.equals(context.getString(R.string.pref_theme_value_dynamic));
    }

    public static int getTextColor(Context context) {
        if (GBApplication.isDarkThemeEnabled()) {
            return context.getResources().getColor(R.color.primarytext_dark);
        } else {
            return context.getResources().getColor(R.color.primarytext_light);
        }
    }

    public static int getSecondaryTextColor(Context context) {
        return context.getResources().getColor(R.color.secondarytext);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLanguage(getLanguage());
    }

    public static int getBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.background, typedValue, true);
        return typedValue.data;
    }

    public static int getWindowBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        return typedValue.data;
    }

    public static boolean hasDirectInternetAccess() {
        return PermissionsUtils.checkPermission(getContext(), Manifest.permission.INTERNET);
    }

    public static boolean hasInternetAccess() {
        return hasDirectInternetAccess() || InternetHelperSingleton.INSTANCE.ensureInternetHelperBound();
    }

    public static GBPrefs getPrefs() {
        return prefs;
    }

    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    public static GBApplication app() {
        return app;
    }

    public static Locale getLanguage() {
        return language;
    }

    public static boolean isNightly() {
        //noinspection ConstantValue - false positive
        return BuildConfig.APPLICATION_ID.contains("nightly");
    }

    public static boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    public String getVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LOG.warn("Unable to determine Gadgetbridge's version", e);
            return "0.0.0";
        }
    }

    public String getNameAndVersion() {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            return String.format("%s %s", appInfo.name, packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            LOG.warn("Unable to determine Gadgetbridge's name/version", e);
            return "Gadgetbridge";
        }
    }

    public void setOpenTracksObserver(OpenTracksContentObserver openTracksObserver) {
        this.openTracksObserver = openTracksObserver;
    }

    public OpenTracksContentObserver getOpenTracksObserver() {
        return openTracksObserver;
    }

    private static class GBActivityLifecycleCallbacks implements ActivityLifecycleCallbacks{
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            boolean preventScreenshots = getPrefs().getBoolean(GBPrefs.BLOCK_SCREENSHOTS, false);
            if (preventScreenshots) {
                LOG.debug("set FLAG_SECURE for {}", activity.getLocalClassName());
                Window window = activity.getWindow();
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            }
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }
    }
}
