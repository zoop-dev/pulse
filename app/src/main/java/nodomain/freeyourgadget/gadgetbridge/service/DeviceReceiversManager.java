/*  Copyright (C) 2015-2026 Andreas Böhler, Andreas Shimokawa, Arjan
    Schrijver, Avamander, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti,
    Daniel Hauck, Davis Mosenkovs, Dikay900, Dmitriy Bogdanov, Frank Slezak,
    Gabriele Monaco, Gordon Williams, ivanovlev, João Paulo Barraca, José
    Rebelo, Julien Pivotto, Kasha, keeshii, Martin, Martin Braun, Matthieu
    Baerts, mvn23, NekoBox, Nephiel, Petr Vaněk, Sebastian Kranz, Sergey
    Trofimov, Steffen Liebergeld, Taavi Eomäe, TylerWilliamson, Uwe Hermann,
    Yoran Vulker, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Pair;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.externalevents.AlarmClockReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothPairingRequestReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CMWeatherReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.DeviceAlarmReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.DeviceSettingsReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.GlobalSettingsReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.HrvCacheInvalidationReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.IntentApiReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.KeyMissingReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.LineageOsWeatherReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.MusicPlaybackReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NewDataReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.OmniJawsObserver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.OsmandEventReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PebbleReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PhoneCallReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.SMSReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.SilentModeReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TimeChangeReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TinyWeatherForecastGermanyReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.VolumeChangeReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.comaps.CoMapsNavigationReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.comaps.CoMapsNavigationReceiverFactory;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid.SleepAsAndroidReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.GBAutoFetchReceiver;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

/**
 * Owns registration, feature-gated enabling/disabling, and teardown of the broadcast
 * receivers / observers used by {@link DeviceCommunicationService} to feed data into
 * connected devices (notifications, calendar, weather, navigation, music, etc).
 */
class DeviceReceiversManager {
    enum Feature {
        WEATHER,
        DATA_FETCHING,
        CALENDAR,
        MUSIC_INFO,
        NAVIGATION,
        SLEEP_AS_ANDROID,
    }

    static class FeatureSet {
        private final Set<Feature> features = EnumSet.noneOf(Feature.class);

        public boolean supports(final Feature feature) {
            return features.contains(feature);
        }

        public void logicalOr(final DeviceCoordinator operand, final GBDevice device) {
            if (operand.supportsCalendarEvents(device)) {
                features.add(Feature.CALENDAR);
            }
            if (operand.supportsWeather(device)) {
                features.add(Feature.WEATHER);
            }
            if (operand.supportsDataFetching(device)) {
                features.add(Feature.DATA_FETCHING);
            }
            if (operand.supportsMusicInfo(device)) {
                features.add(Feature.MUSIC_INFO);
            }
            if (operand.supportsNavigation(device)) {
                features.add(Feature.NAVIGATION);
            }
            if (operand.supportsSleepAsAndroid(device)) {
                features.add(Feature.SLEEP_AS_ANDROID);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DeviceReceiversManager.class);

    private final DeviceCommunicationService service;

    private PhoneCallReceiver mPhoneCallReceiver = null;
    private SMSReceiver mSMSReceiver = null;
    private PebbleReceiver mPebbleReceiver = null;
    private MusicPlaybackReceiver mMusicPlaybackReceiver = null;
    private TimeChangeReceiver mTimeChangeReceiver = null;
    private KeyMissingReceiver mKeyMissingReceiver = null;
    private BluetoothPairingRequestReceiver mBlueToothPairingRequestReceiver = null;
    private AlarmClockReceiver mAlarmClockReceiver = null;
    private SilentModeReceiver mSilentModeReceiver = null;
    private GBAutoFetchReceiver mGBAutoFetchReceiver = null;

    private VolumeChangeReceiver mVolumeChangeReceiver = null;
    private HrvCacheInvalidationReceiver mHrvCacheInvalidationReceiver = null;
    private NewDataReceiver mNewDataReceiver = null;

    private final List<CalendarReceiver> mCalendarReceiver = new ArrayList<>();
    private CMWeatherReceiver mCMWeatherReceiver = null;
    private LineageOsWeatherReceiver mLineageOsWeatherReceiver = null;
    private TinyWeatherForecastGermanyReceiver mTinyWeatherForecastGermanyReceiver = null;
    private OmniJawsObserver mOmniJawsObserver = null;

    private final Stack<BroadcastReceiver> globalReceivers = new Stack<>();
    private GBLocationService locationService = null;

    private OsmandEventReceiver mOsmandAidlHelper = null;
    private final List<CoMapsNavigationReceiver> mCoMapsNavigationReceivers = new ArrayList<>();

    private SleepAsAndroidReceiver mSleepAsAndroidReceiver = null;

    private final String[] mMusicActions = {
            "com.android.music.metachanged",
            "com.android.music.playstatechanged",
            "com.android.music.queuechanged",
            "com.android.music.playbackcomplete",
            "net.sourceforge.subsonic.androidapp.EVENT_META_CHANGED",
            "com.maxmpz.audioplayer.TPOS_SYNC",
            "com.maxmpz.audioplayer.STATUS_CHANGED",
            "com.maxmpz.audioplayer.PLAYING_MODE_CHANGED",
            "com.spotify.music.metadatachanged",
            "com.spotify.music.playbackstatechanged"
    };

    private boolean mReceiversEnabled = false;
    private FeatureSet mCurrentFeatureSet = null;

    DeviceReceiversManager(final DeviceCommunicationService service) {
        this.service = service;
    }

    void registerReceivers() {
        if (GBApplication.getPrefs().getBoolean("intent_api_allow_global_settings", false)) {
            final GlobalSettingsReceiver globalSettingsReceiver = new GlobalSettingsReceiver();
            final IntentFilter globalSettingsIntentFilter = new IntentFilter();
            globalSettingsIntentFilter.addAction(GlobalSettingsReceiver.COMMAND);
            ContextCompat.registerReceiver(service, globalSettingsReceiver, globalSettingsIntentFilter, ContextCompat.RECEIVER_EXPORTED);
            globalReceivers.add(globalSettingsReceiver);
        }

        final DeviceSettingsReceiver deviceSettingsReceiver = new DeviceSettingsReceiver();
        final IntentFilter deviceSettingsIntentFilter = new IntentFilter();
        deviceSettingsIntentFilter.addAction(DeviceSettingsReceiver.COMMAND);
        ContextCompat.registerReceiver(service, deviceSettingsReceiver, deviceSettingsIntentFilter, ContextCompat.RECEIVER_EXPORTED);
        globalReceivers.add(deviceSettingsReceiver);

        final DeviceAlarmReceiver deviceAlarmReceiver = new DeviceAlarmReceiver();
        ContextCompat.registerReceiver(service, deviceAlarmReceiver, deviceAlarmReceiver.buildFilter(), ContextCompat.RECEIVER_EXPORTED);
        globalReceivers.add(deviceAlarmReceiver);

        final IntentApiReceiver intentApiReceiver = new IntentApiReceiver();
        ContextCompat.registerReceiver(service, intentApiReceiver, intentApiReceiver.buildFilter(), ContextCompat.RECEIVER_EXPORTED);
        globalReceivers.add(intentApiReceiver);

        mKeyMissingReceiver = new KeyMissingReceiver();
        ContextCompat.registerReceiver(service, mKeyMissingReceiver, new IntentFilter(KeyMissingReceiver.ACTION_KEY_MISSING), ContextCompat.RECEIVER_EXPORTED);

        mHrvCacheInvalidationReceiver = new HrvCacheInvalidationReceiver();
        mHrvCacheInvalidationReceiver.registerReceiver(service);
    }

    private boolean deviceHasCalendarReceiverRegistered(final GBDevice device) {
        for (final CalendarReceiver receiver : mCalendarReceiver) {
            if (receiver.getGBDevice().equals(device)) {
                return true;
            }
        }
        return false;
    }

    void setReceiversEnableState(final boolean enable, final boolean initialized, final FeatureSet features, final List<GBDevice> devicesWithCalendar) {
        LOG.info("Setting broadcast receivers to: {}", enable);

        if (enable && features == null) {
            throw new RuntimeException("features cannot be null when enabling receivers");
        }

        mReceiversEnabled = enable;
        mCurrentFeatureSet = features;

        if (enable && initialized && features.supports(Feature.CALENDAR)) {
            for (final GBDevice deviceWithCalendar : devicesWithCalendar) {
                if (!deviceHasCalendarReceiverRegistered(deviceWithCalendar)) {
                    if (ContextCompat.checkSelfPermission(service, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                        CalendarReceiver receiver = new CalendarReceiver(service, deviceWithCalendar);
                        receiver.registerBroadcastReceivers();
                        mCalendarReceiver.add(receiver);
                    }
                }
            }
        } else {
            for (CalendarReceiver registeredReceiver : mCalendarReceiver) {
                registeredReceiver.dispose();
            }
            mCalendarReceiver.clear();
        }

        if (enable) {
            if (mPhoneCallReceiver == null) {
                mPhoneCallReceiver = new PhoneCallReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.PHONE_STATE");
                filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
                filter.addAction("nodomain.freeyourgadget.gadgetbridge.MUTE_CALL");
                ContextCompat.registerReceiver(service, mPhoneCallReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }
            if (mSMSReceiver == null) {
                mSMSReceiver = new SMSReceiver();
                ContextCompat.registerReceiver(service, mSMSReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"), ContextCompat.RECEIVER_EXPORTED);
            }
            if (mPebbleReceiver == null) {
                mPebbleReceiver = new PebbleReceiver();
                ContextCompat.registerReceiver(service, mPebbleReceiver, new IntentFilter("com.getpebble.action.SEND_NOTIFICATION"), ContextCompat.RECEIVER_EXPORTED);
            }
            if (mMusicPlaybackReceiver == null && features.supports(Feature.MUSIC_INFO)) {
                mMusicPlaybackReceiver = new MusicPlaybackReceiver();
                IntentFilter filter = new IntentFilter();
                for (String action : mMusicActions) {
                    filter.addAction(action);
                }
                ContextCompat.registerReceiver(service, mMusicPlaybackReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }
            if (mVolumeChangeReceiver == null && features.supports(Feature.MUSIC_INFO)) {
                mVolumeChangeReceiver = new VolumeChangeReceiver();
                mVolumeChangeReceiver.registerReceiver(service);
            }
            if (mNewDataReceiver == null) {
                mNewDataReceiver = new NewDataReceiver();
                mNewDataReceiver.registerReceiver(service);
            }
            if (mTimeChangeReceiver == null) {
                mTimeChangeReceiver = new TimeChangeReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.TIME_SET");
                filter.addAction("android.intent.action.TIMEZONE_CHANGED");
                filter.addAction(TimeChangeReceiver.ACTION_DST_CHANGED_OR_PERIODIC_SYNC);
                ContextCompat.registerReceiver(service, mTimeChangeReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
                // Ensure alarm is scheduled after registering broadcast receiver
                // (this is important in case receiver was unregistered when the previous alarm arrived).
                TimeChangeReceiver.ifEnabledScheduleNextDstChangeOrPeriodicSync(service);
            }
            if (mBlueToothPairingRequestReceiver == null) {
                mBlueToothPairingRequestReceiver = new BluetoothPairingRequestReceiver(service);
                ContextCompat.registerReceiver(service, mBlueToothPairingRequestReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST), ContextCompat.RECEIVER_EXPORTED);
            }
            if (mAlarmClockReceiver == null) {
                mAlarmClockReceiver = new AlarmClockReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(AlarmClockReceiver.ALARM_ALERT_ACTION);
                filter.addAction(AlarmClockReceiver.ALARM_DONE_ACTION);
                filter.addAction(AlarmClockReceiver.GOOGLE_CLOCK_ALARM_ALERT_ACTION);
                filter.addAction(AlarmClockReceiver.GOOGLE_CLOCK_ALARM_DONE_ACTION);
                ContextCompat.registerReceiver(service, mAlarmClockReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }

            if (mSilentModeReceiver == null) {
                mSilentModeReceiver = new SilentModeReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
                ContextCompat.registerReceiver(service, mSilentModeReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }

            if (locationService == null) {
                locationService = new GBLocationService(service);
                //noinspection deprecation
                LocalBroadcastManager.getInstance(service).registerReceiver(locationService, locationService.buildFilter());
            }

            if (mOsmandAidlHelper == null && features.supports(Feature.NAVIGATION)) {
                mOsmandAidlHelper = new OsmandEventReceiver(service.getApplication());
            }

            if (features.supports(Feature.NAVIGATION) && GBApplication.getPrefs().getBoolean(GBPrefs.NAVIGATION_APP_COMAPS, false)) {
                for (Pair<Uri, CoMapsNavigationReceiver> pair : CoMapsNavigationReceiverFactory.createCoMapsNavigationReceiversForApplication(service.getApplication())) {
                    service.getContentResolver().registerContentObserver(pair.first, false, pair.second);
                    mCoMapsNavigationReceivers.add(pair.second);
                }
            }

            // Weather receivers
            if (features.supports(Feature.WEATHER)) {
                if (GBApplication.isRunningOreoOrLater()) {
                    if (mLineageOsWeatherReceiver == null) {
                        mLineageOsWeatherReceiver = new LineageOsWeatherReceiver();
                        ContextCompat.registerReceiver(service, mLineageOsWeatherReceiver, new IntentFilter("GB_UPDATE_WEATHER"), ContextCompat.RECEIVER_EXPORTED);
                    }
                } else {
                    if (mCMWeatherReceiver == null) {
                        mCMWeatherReceiver = new CMWeatherReceiver();
                        ContextCompat.registerReceiver(service, mCMWeatherReceiver, new IntentFilter("GB_UPDATE_WEATHER"), ContextCompat.RECEIVER_EXPORTED);
                    }
                }
                if (mTinyWeatherForecastGermanyReceiver == null) {
                    mTinyWeatherForecastGermanyReceiver = new TinyWeatherForecastGermanyReceiver();
                    ContextCompat.registerReceiver(service, mTinyWeatherForecastGermanyReceiver, new IntentFilter("de.kaffeemitkoffein.broadcast.WEATHERDATA"), ContextCompat.RECEIVER_EXPORTED);
                }
                if (mOmniJawsObserver == null) {
                    try {
                        //noinspection deprecation
                        mOmniJawsObserver = new OmniJawsObserver(new Handler());
                        service.getContentResolver().registerContentObserver(OmniJawsObserver.WEATHER_URI, true, mOmniJawsObserver);
                    } catch (PackageManager.NameNotFoundException e) {
                        //Nothing wrong, it just means we're not running on omnirom.
                    }
                }
            }

            if (features.supports(Feature.SLEEP_AS_ANDROID)) {
                if (mSleepAsAndroidReceiver == null) {
                    mSleepAsAndroidReceiver = new SleepAsAndroidReceiver();
                    ContextCompat.registerReceiver(service, mSleepAsAndroidReceiver, mSleepAsAndroidReceiver.getIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
                }
            }

            if (features.supports(Feature.DATA_FETCHING) && mGBAutoFetchReceiver == null) {
                mGBAutoFetchReceiver = new GBAutoFetchReceiver();
                ContextCompat.registerReceiver(service, mGBAutoFetchReceiver, new IntentFilter("android.intent.action.USER_PRESENT"), ContextCompat.RECEIVER_EXPORTED);
            }
        } else {
            if (mPhoneCallReceiver != null) {
                service.unregisterReceiver(mPhoneCallReceiver);
                mPhoneCallReceiver = null;
            }
            if (mSMSReceiver != null) {
                service.unregisterReceiver(mSMSReceiver);
                mSMSReceiver = null;
            }
            if (mPebbleReceiver != null) {
                service.unregisterReceiver(mPebbleReceiver);
                mPebbleReceiver = null;
            }
            if (mMusicPlaybackReceiver != null) {
                service.unregisterReceiver(mMusicPlaybackReceiver);
                mMusicPlaybackReceiver = null;
            }
            if (mVolumeChangeReceiver != null) {
                mVolumeChangeReceiver.unregisterReceiver();
                mVolumeChangeReceiver = null;
            }
            if (mTimeChangeReceiver != null) {
                service.unregisterReceiver(mTimeChangeReceiver);
                mTimeChangeReceiver = null;
            }

            if (mBlueToothPairingRequestReceiver != null) {
                service.unregisterReceiver(mBlueToothPairingRequestReceiver);
                mBlueToothPairingRequestReceiver = null;
            }
            if (mAlarmClockReceiver != null) {
                service.unregisterReceiver(mAlarmClockReceiver);
                mAlarmClockReceiver = null;
            }
            if (mSilentModeReceiver != null) {
                service.unregisterReceiver(mSilentModeReceiver);
                mSilentModeReceiver = null;
            }
            if (locationService != null) {
                //noinspection deprecation
                LocalBroadcastManager.getInstance(service).unregisterReceiver(locationService);
                locationService.stopAll();
                locationService = null;
            }
            if (mCMWeatherReceiver != null) {
                service.unregisterReceiver(mCMWeatherReceiver);
                mCMWeatherReceiver = null;
            }
            if (mLineageOsWeatherReceiver != null) {
                service.unregisterReceiver(mLineageOsWeatherReceiver);
                mLineageOsWeatherReceiver = null;
            }
            if (mOmniJawsObserver != null) {
                service.getContentResolver().unregisterContentObserver(mOmniJawsObserver);
                mOmniJawsObserver = null;
            }
            if (mTinyWeatherForecastGermanyReceiver != null) {
                service.unregisterReceiver(mTinyWeatherForecastGermanyReceiver);
                mTinyWeatherForecastGermanyReceiver = null;
            }
            if (mOsmandAidlHelper != null) {
                mOsmandAidlHelper.cleanupResources();
                mOsmandAidlHelper = null;
            }
            mCoMapsNavigationReceivers.forEach(service.getContentResolver()::unregisterContentObserver);
            mCoMapsNavigationReceivers.clear();
            if (mGBAutoFetchReceiver != null) {
                service.unregisterReceiver(mGBAutoFetchReceiver);
                mGBAutoFetchReceiver = null;
            }
            if (mSleepAsAndroidReceiver != null) {
                service.unregisterReceiver(mSleepAsAndroidReceiver);
                mSleepAsAndroidReceiver = null;
            }
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case GBPrefs.NAVIGATION_APP_COMAPS -> {
                if (mReceiversEnabled && mCurrentFeatureSet != null && mCurrentFeatureSet.supports(Feature.NAVIGATION)) {
                    boolean enable = sharedPreferences.getBoolean(GBPrefs.NAVIGATION_APP_COMAPS, false);
                    LOG.debug("Actioning CoMaps preference change to {}", enable);
                    if (enable) {
                        for (Pair<Uri, CoMapsNavigationReceiver> pair : CoMapsNavigationReceiverFactory.createCoMapsNavigationReceiversForApplication(service.getApplication())) {
                            service.getContentResolver().registerContentObserver(pair.first, false, pair.second);
                            mCoMapsNavigationReceivers.add(pair.second);
                        }
                    } else {
                        mCoMapsNavigationReceivers.forEach(service.getContentResolver()::unregisterContentObserver);
                        mCoMapsNavigationReceivers.clear();
                    }
                }
            }
        }
    }

    void onDestroy() {
        setReceiversEnableState(false, false, null, null); // disable BroadcastReceivers

        while (!globalReceivers.isEmpty()) {
            final BroadcastReceiver receiver = globalReceivers.pop();
            try {
                LOG.debug("Unregistering global receiver {}", receiver.getClass().getSimpleName());
                service.unregisterReceiver(receiver);
            } catch (final Exception e) {
                LOG.error("Failed to unregister broadcast receiver", e);
            }
        }

        if (mKeyMissingReceiver != null) {
            try {
                LOG.debug("Unregistering missing receiver");
                service.unregisterReceiver(mKeyMissingReceiver);
            } catch (final Exception e) {
                LOG.error("Failed to unregister broadcast receiver", e);
            }
        }

        if (mHrvCacheInvalidationReceiver != null) {
            mHrvCacheInvalidationReceiver.unregisterReceiver();
            mHrvCacheInvalidationReceiver = null;
        }
    }
}
