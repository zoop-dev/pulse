/*  Copyright (C) 2022-2024 Daniel Dakhno, José Rebelo, Oleg Vasilev

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import static java.lang.Thread.sleep;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions.fromUint8;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.HEART_RATE_ALL_DAY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.SLEEP_HIGH_ACCURACY_MONITORING;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsMapsInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsMusicInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.SleepAsAndroidSender;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Service;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsAgpsInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsGpxRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid.SleepAsAndroidAction;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021ChunkedDecoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021ChunkedEncoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Handler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiDevicePrefs;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFetcher;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsAgpsUpdateOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsFirmwareUpdateOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsGpxRouteUploadOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsMusicUploadOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsActivityFetchService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAgpsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAlarmsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAssistantService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAppsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAuthenticationService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsBatteryService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsCalendarService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsCannedMessagesService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConnectionService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsDeviceInfoService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsDisplayItemsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFindDeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsHeartRateService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsHttpService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsLogsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsLoyaltyCardService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsMapsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsMusicService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsNotificationService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsRemindersService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsServicesService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsShortcutCardsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsContactsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFtpServerService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsMorningUpdatesService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsPhoneService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsSilentModeService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsStepsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsTimeService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsUserInfoService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsVibrationPatternsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsVoiceMemosService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsWatchfaceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsWeatherService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsWifiService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsWorkoutService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsWorldClocksService;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.RealtimeSamplesAggregator;

public class ZeppOsSupport extends AbstractDeviceSupport
        implements Huami2021Handler, HuamiFetcher.HuamiFetchSupport, ZeppOsFileTransferService.DownloadCallback {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsSupport.class);

    private final ZeppOsCommunicator communicator;

    // Keep track of whether the rawSensor is enabled
    private boolean rawSensor = false;
    private ScheduledExecutorService rawSensorScheduler;

    private int mMTU = 23;

    private final Huami2021ChunkedEncoder huami2021ChunkedEncoder = new Huami2021ChunkedEncoder(getMTU());
    private final Huami2021ChunkedDecoder huami2021ChunkedDecoder = new Huami2021ChunkedDecoder(this, true);

    private final HuamiFetcher fetcher = new HuamiFetcher(this);

    private SleepAsAndroidSender sleepAsAndroidSender;

    private ZeppOsFirmwareUpdateOperation firmwareUpdateOperation;

    // Services
    private final ZeppOsServicesService servicesService = new ZeppOsServicesService(this);
    private final ZeppOsAuthenticationService authenticationService = new ZeppOsAuthenticationService(this);
    private final ZeppOsFileTransferService fileTransferService = new ZeppOsFileTransferService(this);
    private final ZeppOsConfigService configService = new ZeppOsConfigService(this);
    private final ZeppOsAgpsService agpsService = new ZeppOsAgpsService(this);
    private final ZeppOsWifiService wifiService = new ZeppOsWifiService(this);
    private final ZeppOsFtpServerService ftpServerService = new ZeppOsFtpServerService(this);
    private final ZeppOsContactsService contactsService = new ZeppOsContactsService(this);
    private final ZeppOsMorningUpdatesService morningUpdatesService = new ZeppOsMorningUpdatesService(this);
    private final ZeppOsPhoneService phoneService = new ZeppOsPhoneService(this);
    private final ZeppOsShortcutCardsService shortcutCardsService = new ZeppOsShortcutCardsService(this);
    private final ZeppOsWatchfaceService watchfaceService = new ZeppOsWatchfaceService(this);
    private final ZeppOsAlarmsService alarmsService = new ZeppOsAlarmsService(this);
    private final ZeppOsCalendarService calendarService = new ZeppOsCalendarService(this);
    private final ZeppOsCannedMessagesService cannedMessagesService = new ZeppOsCannedMessagesService(this);
    private final ZeppOsNotificationService notificationService = new ZeppOsNotificationService(this, fileTransferService);
    private final ZeppOsAssistantService alexaService = new ZeppOsAssistantService(this, ZeppOsAssistantService.ENDPOINT_ALEXA);
    private final ZeppOsAssistantService zeppFlowService = new ZeppOsAssistantService(this, ZeppOsAssistantService.ENDPOINT_ZEPP_FLOW);
    private final ZeppOsAppsService appsService = new ZeppOsAppsService(this);
    private final ZeppOsLogsService logsService = new ZeppOsLogsService(this);
    private final ZeppOsDisplayItemsService displayItemsService = new ZeppOsDisplayItemsService(this);
    private final ZeppOsHttpService httpService = new ZeppOsHttpService(this, fileTransferService);
    private final ZeppOsRemindersService remindersService = new ZeppOsRemindersService(this);
    private final ZeppOsLoyaltyCardService loyaltyCardService = new ZeppOsLoyaltyCardService(this);
    private final ZeppOsVoiceMemosService voiceMemosService = new ZeppOsVoiceMemosService(this);
    private final ZeppOsMapsService mapsService = new ZeppOsMapsService(this, httpService);
    private final ZeppOsMusicService musicService = new ZeppOsMusicService(this);
    private final ZeppOsFindDeviceService findDeviceService = new ZeppOsFindDeviceService(this);
    private final ZeppOsSilentModeService silentModeService = new ZeppOsSilentModeService(this);
    private final ZeppOsUserInfoService userInfoService = new ZeppOsUserInfoService(this);
    private final ZeppOsVibrationPatternsService vibrationPatternsService = new ZeppOsVibrationPatternsService(this);
    private final ZeppOsBatteryService batteryService = new ZeppOsBatteryService(this);
    private final ZeppOsWeatherService weatherService = new ZeppOsWeatherService(this);
    private final ZeppOsConnectionService connectionService = new ZeppOsConnectionService(this);
    private final ZeppOsWorldClocksService worldClocksService = new ZeppOsWorldClocksService(this);
    private final ZeppOsWorkoutService workoutService = new ZeppOsWorkoutService(this);
    private final ZeppOsHeartRateService heartRateService = new ZeppOsHeartRateService(this);
    private final ZeppOsStepsService stepsService = new ZeppOsStepsService(this);
    private final ZeppOsActivityFetchService activityFetchService = new ZeppOsActivityFetchService(this, fetcher);
    private final ZeppOsTimeService timeService = new ZeppOsTimeService(this);
    private final ZeppOsDeviceInfoService deviceInfoService = new ZeppOsDeviceInfoService(this);

    private final Set<Short> mSupportedServices = new HashSet<>();
    private final Map<Short, AbstractZeppOsService> mServiceMap = new LinkedHashMap<Short, AbstractZeppOsService>() {{
        put(servicesService.getEndpoint(), servicesService);
        put(authenticationService.getEndpoint(), authenticationService);
        put(batteryService.getEndpoint(), batteryService);
        put(connectionService.getEndpoint(), connectionService);
        put(fileTransferService.getEndpoint(), fileTransferService);
        put(configService.getEndpoint(), configService);
        put(agpsService.getEndpoint(), agpsService);
        put(wifiService.getEndpoint(), wifiService);
        put(ftpServerService.getEndpoint(), ftpServerService);
        put(contactsService.getEndpoint(), contactsService);
        put(morningUpdatesService.getEndpoint(), morningUpdatesService);
        put(phoneService.getEndpoint(), phoneService);
        put(shortcutCardsService.getEndpoint(), shortcutCardsService);
        put(watchfaceService.getEndpoint(), watchfaceService);
        put(alarmsService.getEndpoint(), alarmsService);
        put(calendarService.getEndpoint(), calendarService);
        put(cannedMessagesService.getEndpoint(), cannedMessagesService);
        put(notificationService.getEndpoint(), notificationService);
        put(alexaService.getEndpoint(), alexaService);
        put(zeppFlowService.getEndpoint(), zeppFlowService);
        put(appsService.getEndpoint(), appsService);
        put(logsService.getEndpoint(), logsService);
        put(displayItemsService.getEndpoint(), displayItemsService);
        put(httpService.getEndpoint(), httpService);
        put(remindersService.getEndpoint(), remindersService);
        put(loyaltyCardService.getEndpoint(), loyaltyCardService);
        put(musicService.getEndpoint(), musicService);
        put(voiceMemosService.getEndpoint(), voiceMemosService);
        put(mapsService.getEndpoint(), mapsService);
        put(findDeviceService.getEndpoint(), findDeviceService);
        put(silentModeService.getEndpoint(), silentModeService);
        put(userInfoService.getEndpoint(), userInfoService);
        put(vibrationPatternsService.getEndpoint(), vibrationPatternsService);
        put(weatherService.getEndpoint(), weatherService);
        put(worldClocksService.getEndpoint(), worldClocksService);
        put(workoutService.getEndpoint(), workoutService);
        put(heartRateService.getEndpoint(), heartRateService);
        put(stepsService.getEndpoint(), stepsService);
        put(activityFetchService.getEndpoint(), activityFetchService);
        put(timeService.getEndpoint(), timeService);
        put(deviceInfoService.getEndpoint(), deviceInfoService);
    }};

    public ZeppOsSupport(final ZeppOsCommunicator communicator) {
        this.communicator = communicator;
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        sleepAsAndroidSender = new SleepAsAndroidSender(gbDevice);
        heartRateService.setSleepAsAndroidSender(sleepAsAndroidSender);
        final RealtimeSamplesAggregator realtimeSamplesAggregator = new RealtimeSamplesAggregator(getContext(), getDevice());
        heartRateService.setRealtimeSamplesAggregator(realtimeSamplesAggregator);
        stepsService.setRealtimeSamplesAggregator(realtimeSamplesAggregator);
    }

    @Override
    public boolean connect() {
        // Nothing to do - connection is done in the communicator
        return true;
    }

    @Override
    public HuamiDevicePrefs getDevicePrefs() {
        return new HuamiDevicePrefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()), gbDevice);
    }

    @Override
    public void dispose() {
        for (final Short endpoint : mSupportedServices) {
            if (mServiceMap.containsKey(endpoint)) {
                Objects.requireNonNull(mServiceMap.get(endpoint)).dispose();
            }
        }
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    protected void initializeDevice(final ZeppOsTransactionBuilder builder) {
        huami2021ChunkedEncoder.reset();
        huami2021ChunkedDecoder.reset();
        fetcher.reset();

        builder.setDeviceState(GBDevice.State.AUTHENTICATING);

        authenticationService.startAuthentication(builder);
    }

    @Override
    public void onSendConfiguration(final String config) {
        final Prefs prefs = getDevicePrefs();

        // Check if any of the services handles this config
        for (AbstractZeppOsService service : mServiceMap.values()) {
            if (service.onSendConfiguration(config, prefs)) {
                return;
            }
        }

        LOG.warn("Unhandled config {}", config);

        super.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onFindDevice(final boolean start) {
        findDeviceService.onFindDevice(start);
    }

    private void sendFindDeviceCommand(boolean start) {
        findDeviceService.sendFindDeviceCommand(start);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        if ((dataTypes & RecordedDataTypes.TYPE_AUDIO_REC) != 0 && getCoordinator().supportsAudioRecordings(getDevice())) {
            voiceMemosService.requestList();
        }

        fetcher.onFetchRecordedData(dataTypes);
    }

    @Override
    public void onFindPhone(final boolean start) {
        findDeviceService.onFindPhone(start);
    }

    @Override
    public void onScreenshotReq() {
        appsService.requestScreenshot();
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        int minuteInterval;
        if (seconds == -1) {
            // Smart
            minuteInterval = -1;
        } else {
            minuteInterval = seconds / 60;
            minuteInterval = Math.min(minuteInterval, 120);
            minuteInterval = Math.max(0, minuteInterval);
        }

        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder(String.format(Locale.ROOT, "set heart rate interval to: %d min", minuteInterval));
        configService.newSetter()
                .setByte(HEART_RATE_ALL_DAY_MONITORING, (byte) minuteInterval)
                .write(builder);
        builder.queue();
    }

    @Override
    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        calendarService.addEvent(calendarEventSpec);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, final long id) {
        calendarService.deleteEvent(type, id);
    }

    @Override
    public void onHeartRateTest() {
        heartRateService.onHeartRateTest();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        heartRateService.onEnableRealtimeHeartRateMeasurement(enable);
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        alarmsService.onSetAlarms(alarms);
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        notificationService.setCallState(callSpec);
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        notificationService.sendNotification(notificationSpec);
    }

    @Override
    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("onSetReminders");
        remindersService.sendReminders(builder, reminders);
        builder.queue();
    }

    @Override
    public void onSetWorldClocks(ArrayList<? extends WorldClock> clocks) {
        worldClocksService.onSetWorldClocks(clocks);
    }

    @Override
    public void onSetLoyaltyCards(final ArrayList<LoyaltyCard> cards) {
        loyaltyCardService.setCards(cards);
    }

    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {
        //noinspection unchecked
        contactsService.setContacts((List<Contact>) contacts);
    }

    @Override
    public void onDeleteNotification(final int id) {
        notificationService.deleteNotification(id);
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        workoutService.onSetGpsLocation(location);
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        cannedMessagesService.setCannedMessages(cannedMessagesSpec);
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        musicService.sendVolume(volume);
    }

    @Override
    public void onSetMusicState(final MusicStateSpec stateSpec) {
        musicService.onSetMusicState(stateSpec);
    }

    @Override
    public void onSetMusicInfo(final MusicSpec musicSpec) {
        musicService.onSetMusicInfo(musicSpec);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        stepsService.setRealtimeSteps(enable);
    }

    @Override
    public void onInstallApp(final Uri uri, @NonNull final Bundle options) {
        final ZeppOsAgpsInstallHandler agpsHandler = new ZeppOsAgpsInstallHandler(uri, getContext());
        if (agpsHandler.isValid()) {
            try {
                if (getCoordinator().sendAgpsAsFileTransfer()) {
                    LOG.info("Sending AGPS as file transfer");

                    if (getMTU() == 23) {
                        // AGPS updates without high MTU are too slow and eventually get stuck,
                        // so let's fail right away and inform the user
                        LOG.warn("MTU of {} is too low for AGPS file transfer", getMTU());
                        handleGBDeviceEvent(new GBDeviceEventDisplayMessage(
                                getContext().getString(R.string.updatefirmwareoperation_failed_low_mtu, getMTU()),
                                Toast.LENGTH_LONG,
                                GB.WARN
                        ));
                        return;
                    }

                    new ZeppOsAgpsUpdateOperation(
                            this,
                            agpsHandler.getFile(),
                            agpsService,
                            fileTransferService,
                            configService
                    ).perform();
                } else {
                    LOG.info("Sending AGPS as firmware update");

                    // Write the agps epo update to a temporary file in cache, so we can reuse the firmware update operation
                    final File cacheDir = getContext().getCacheDir();
                    final File agpsCacheDir = new File(cacheDir, "zepp-os-agps");
                    //noinspection ResultOfMethodCallIgnored
                    agpsCacheDir.mkdir();
                    final File uihhFile = new File(agpsCacheDir, "epo-agps.uihh");

                    try (FileOutputStream outputStream = new FileOutputStream(uihhFile)) {
                        outputStream.write(agpsHandler.getFile().getUihhBytes());
                    } catch (final IOException e) {
                        LOG.error("Failed to write agps bytes to temporary uihhFile", e);
                        return;
                    }

                    firmwareUpdateOperation = new ZeppOsFirmwareUpdateOperation(
                            Uri.parse(uihhFile.toURI().toString()),
                            this
                    );
                    firmwareUpdateOperation.perform();
                }
            } catch (final Exception e) {
                GB.toast(getContext(), "AGPS install error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            }

            return;
        }

        final ZeppOsGpxRouteInstallHandler gpxRouteHandler = new ZeppOsGpxRouteInstallHandler(uri, getContext());
        if (gpxRouteHandler.isValid()) {
            final String trackName = options.getString(ZeppOsGpxRouteInstallHandler.EXTRA_TRACK_NAME);
            try {
                new ZeppOsGpxRouteUploadOperation(
                        this,
                        gpxRouteHandler.getGpxFile(),
                        trackName,
                        fileTransferService
                ).perform();
            } catch (final Exception e) {
                GB.toast(getContext(), "Gpx install error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            }

            return;
        }

        final ZeppOsMusicInstallHandler musicHandler = new ZeppOsMusicInstallHandler(uri, getContext());
        if (musicHandler.isValid()) {
            try {
                final byte[] musicBytes = musicHandler.readFileBytes();
                if (musicBytes == null) {
                    return;
                }
                new ZeppOsMusicUploadOperation(
                        this,
                        musicHandler.getAudioInfo(),
                        musicBytes,
                        fileTransferService
                ).perform();
            } catch (final Exception e) {
                GB.toast(getContext(), "Music install error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            }

            return;
        }

        final ZeppOsMapsInstallHandler mapsHandler = new ZeppOsMapsInstallHandler(uri, getContext());
        if (mapsHandler.isValid()) {
            mapsService.upload(mapsHandler.getFile());
            return;
        }

        try {
            firmwareUpdateOperation = new ZeppOsFirmwareUpdateOperation(uri, this);
            firmwareUpdateOperation.perform();
        } catch (final IOException ex) {
            GB.toast(getContext(), "Firmware install error: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    @Override
    public void onAppInfoReq() {
        // Merge the data from apps and watchfaces
        // This is required because the apps service only knows the versions, not the app type,
        // and the watchface service only knows the app IDs, and not the versions

        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        final List<GBDeviceApp> appsFull = new ArrayList<>();

        final Map<UUID, GBDeviceApp> watchfacesById = new HashMap<>();
        final List<GBDeviceApp> watchfaces = watchfaceService.getWatchfaces();
        for (final GBDeviceApp watchface : watchfaces) {
            watchfacesById.put(watchface.getUUID(), watchface);
        }

        final List<GBDeviceApp> apps = appsService.getApps();
        for (final GBDeviceApp app : apps) {
            final GBDeviceApp watchface = watchfacesById.get(app.getUUID());
            if (watchface != null) {
                appsFull.add(new GBDeviceApp(
                        watchface.getUUID(),
                        watchface.getName(),
                        watchface.getCreator(),
                        app.getVersion(),
                        GBDeviceApp.Type.WATCHFACE
                ));
            } else {
                appsFull.add(new GBDeviceApp(
                        app.getUUID(),
                        app.getName(),
                        app.getCreator(),
                        app.getVersion(),
                        GBDeviceApp.Type.APP_GENERIC
                ));
            }
        }

        appInfoCmd.apps = appsFull.toArray(new GBDeviceApp[0]);
        evaluateGBDeviceEvent(appInfoCmd);
    }

    @Override
    public void onAppStart(final UUID uuid, final boolean start) {
        if (start) {
            // This actually also starts apps...
            watchfaceService.setWatchface(uuid);
        }
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        appsService.deleteApp(uuid);
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("enable heart rate sleep support: " + enable);
        final boolean enableHrSleepSupport = MiBandCoordinator.getHeartrateSleepSupport(gbDevice.getAddress());

        configService.newSetter()
                .setBoolean(SLEEP_HIGH_ACCURACY_MONITORING, enableHrSleepSupport)
                .write(builder);

        builder.queue();
    }

    @Override
    public byte[] getTimeBytes(final Calendar calendar, final TimeUnit precision) {
        final byte[] bytes = BLETypeConversions.shortCalendarToRawBytes(calendar);

        if (precision != TimeUnit.MINUTES && precision != TimeUnit.SECONDS) {
            throw new IllegalArgumentException("Unsupported precision, only MINUTES and SECONDS are supported");
        }
        final byte seconds = precision == TimeUnit.SECONDS ? fromUint8(calendar.get(Calendar.SECOND)) : 0;
        final byte tz = BLETypeConversions.mapTimeZone(calendar, BLETypeConversions.TZ_FLAG_INCLUDE_DST_IN_TZ);
        return BLETypeConversions.join(bytes, new byte[]{seconds, tz});
    }

    @Override
    public void onSetTime() {
        if (GBApplication.getPrefs().syncTime()) {
            final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("set date and time");
            if (mSupportedServices.contains(timeService.getEndpoint())) {
                timeService.setTime(builder);
            } else {
                communicator.setCurrentTime(builder);
            }
            builder.queue();
        }

        CalendarReceiver.forceSync(getDevice());
    }

    @Override
    public void onSendWeather() {
        weatherService.onSendWeather();
    }

    @Override
    public void onSleepAsAndroidAction(String action, Bundle extras) {
        // Validate if our device can work with an action
        try {
            sleepAsAndroidSender.validateAction(action);
        } catch (UnsupportedOperationException e) {
            return;
        }

        // Consult the SleepAsAndroid documentation for a set of actions and their extra
        // https://docs.sleep.urbandroid.org/devs/wearable_api.html
        switch (action) {
            case SleepAsAndroidAction.CHECK_CONNECTED:
                sleepAsAndroidSender.confirmConnected();
                break;
            // Received when the app starts sleep tracking
            case SleepAsAndroidAction.START_TRACKING:
                heartRateService.onEnableRealtimeHeartRateMeasurement(true);
                enableRawSensor(true);
                sleepAsAndroidSender.startTracking();
                break;
            // Received when the app stops sleep tracking
            case SleepAsAndroidAction.STOP_TRACKING:
                heartRateService.onEnableRealtimeHeartRateMeasurement(false);
                enableRawSensor(false);
                sleepAsAndroidSender.stopTracking();
                break;
            // Received when the app pauses sleep tracking
//            case SleepAsAndroidAction.SET_PAUSE:
//                long pauseTimestamp = extras.getLong("TIMESTAMP");
//                long delay = pauseTimestamp > 0 ? pauseTimestamp - System.currentTimeMillis() : 0;
//                setRawSensor(delay > 0);
//                enableRealtimeSamplesTimer(delay > 0);
//                sleepAsAndroidSender.pauseTracking(delay);
//                break;
            // Same as above but controlled by a boolean value
            case SleepAsAndroidAction.SET_SUSPENDED:
                boolean suspended = extras.getBoolean("SUSPENDED", false);
                setRawSensor(!suspended);
                //FIXME is this really needed? enableRealtimeSamplesTimer(!suspended);
                sleepAsAndroidSender.pauseTracking(suspended);
                break;
            // Received when the app changes the batch size for the movement data
            case SleepAsAndroidAction.SET_BATCH_SIZE:
                long batchSize = extras.getLong("SIZE", 12L);
                sleepAsAndroidSender.setBatchSize(batchSize);
                break;
            // Received when the app requests the wearable to vibrate
            case SleepAsAndroidAction.HINT:
                int repeat = extras.getInt("REPEAT");
                for (int i = 0; i < repeat; i++) {
                    sendFindDeviceCommand(true);
                    try {
                        sleep(500);
                        sendFindDeviceCommand(false);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            // Received when the app sends a notificaation
            case SleepAsAndroidAction.SHOW_NOTIFICATION:
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.title = extras.getString("TITLE");
                notificationSpec.body = extras.getString("BODY");
                notificationService.sendNotification(notificationSpec);
                break;
            // Received when the app updates an alarm (Snoozing included too)
            // It's better to use SleepAsAndroidAction.START_ALARM and .STOP_ALARM where possible to have more control over the alarm.
            // Using .UPDATE_ALARM will let Gadgetbridge know when an alarm was set but not when it was dismissed.
            case SleepAsAndroidAction.UPDATE_ALARM:
                long alarmTimestamp = extras.getLong("TIMESTAMP");

                // Sets the alarm at a giver hour and minute
                // Snoozing from the app will create a new alarm in the future
                setSleepAsAndroidAlarm(alarmTimestamp);
                break;
            // Received when an app alarm is stopped
            case SleepAsAndroidAction.STOP_ALARM:
                // Manually stop an alarm
                break;
            // Received when an app alarm starts
            case SleepAsAndroidAction.START_ALARM:
                // Manually start an alarm
                break;
            default:
                LOG.warn("Received unsupported " + action);
                break;
        }
    }

    private void setSleepAsAndroidAlarm(long alarmTimestamp) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(new Timestamp(alarmTimestamp).getTime());
        Alarm alarm = AlarmUtils.createSingleShot(SleepAsAndroidSender.getAlarmSlot(), false, false, calendar);
        ArrayList<Alarm> alarms = new ArrayList<>(1);
        alarms.add(alarm);

        GBApplication.deviceService(gbDevice).onSetAlarms(alarms);
    }

    private void stopRawSensors() {
        if (rawSensorScheduler != null) {
            rawSensorScheduler.shutdown();
            rawSensorScheduler = null;
        }
    }

    private ScheduledExecutorService startRawSensors() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (rawSensor) {
                    setRawSensor(true);
                }
            }
        }, 0, 10000, TimeUnit.MILLISECONDS);
        return service;
    }

    private void enableRawSensor(boolean enable) {
        setRawSensor(enable);
        if (enable) {
            rawSensorScheduler = startRawSensors();
        } else {
            stopRawSensors();
        }

    }

    public void requestDisplayItems(final ZeppOsTransactionBuilder builder) {
        displayItemsService.requestItems(builder, ZeppOsDisplayItemsService.DISPLAY_ITEMS_MENU);
    }

    public void requestApps(final ZeppOsTransactionBuilder builder) {
        appsService.requestApps(builder);
    }

    public void requestWatchfaces(final ZeppOsTransactionBuilder builder) {
        watchfaceService.requestWatchfaces(builder);
        watchfaceService.requestCurrentWatchface(builder);
    }

    public void onFirmwareUpdateFinished() {
        firmwareUpdateOperation = null;
    }

    public void onAuthenticationSuccess() {
        LOG.info("ZeppOS phase 2 initialize...");

        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("phase 2 initialize");
        builder.setDeviceState(GBDevice.State.INITIALIZING);

        communicator.onAuthenticationSuccess(builder);

        // Make sure that performInitialized is not called accidentally in here
        // (eg. by creating a new TransactionBuilder).
        // In those cases, the device will be initialized twice, which will change the shared
        // session key during these requests and decrypting messages will fail

        // In here, we only request the list of supported services - they will all be initialized in
        // initializeServices below
        mSupportedServices.clear();
        servicesService.requestServices(builder);

        builder.queue();
    }

    public void addSupportedService(final short endpoint, final boolean encrypted) {
        mSupportedServices.add(endpoint);
    }

    public void initializeServices() {
        LOG.info("2021 initializeServices...");

        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("initialize services");

        // At this point we got the service list from phase 3, so we know which
        // services are supported, and whether they are encrypted or not

        if (GBApplication.getPrefs().syncTime()) {
            if (mSupportedServices.contains(timeService.getEndpoint())) {
                timeService.setTime(builder);
            } else {
                communicator.setCurrentTime(builder);
            }
        }
        if (mSupportedServices.contains(deviceInfoService.getEndpoint())) {
            deviceInfoService.requestDeviceInfo(builder);
        } else {
            communicator.requestDeviceInfo(builder);
        }

        for (AbstractZeppOsService service : mServiceMap.values()) {
            if (mSupportedServices.contains(service.getEndpoint())) {
                // Only initialize supported services
                service.initialize(builder);
            }
        }

        builder.setDeviceState(GBDevice.State.INITIALIZED);

        builder.queue();
    }

    @Override
    public void setActivityNotifications(final boolean control, final boolean data) {
        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("set activity notifications: " + control + " " + data);
        builder.notify(HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_CONTROL, control);
        builder.notify(HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_DATA, data);
        builder.queue();
    }

    @Override
    public void writeActivityControl(final String name, final byte[] value) {
        if (mSupportedServices.contains(activityFetchService.getEndpoint())) {
            activityFetchService.writeActivityControl(name, value);
        } else {
            final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder(name);
            builder.write(HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_CONTROL, value);
            builder.queue();
        }
    }

    @Nullable
    public AbstractZeppOsService getService(final short endpoint) {
        return mServiceMap.get(endpoint);
    }

    @Override
    public int getActivitySampleSize() {
        return 8;
    }

    @Override
    public ZeppOsCoordinator getCoordinator() {
        return (ZeppOsCoordinator) gbDevice.getDeviceCoordinator();
    }

    private void setRawSensor(final boolean enable) {
        LOG.info("Set raw sensor to {}", enable);
        rawSensor = enable;

        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("set raw sensor");
        if (enable) {
            builder.write(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL, Huami2021Service.CMD_RAW_SENSOR_START_1);
            builder.write(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL, Huami2021Service.CMD_RAW_SENSOR_START_2);
            builder.write(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL, Huami2021Service.CMD_RAW_SENSOR_START_3);
        } else {
            builder.write(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL, Huami2021Service.CMD_RAW_SENSOR_STOP);
        }
        builder.notify(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_DATA, enable);
        builder.queue();
    }

    private void handleRawSensorData(final byte[] value) {
        // The g values seem to vary between -4100 and 4100, so we scale them
        final float scaleFactor = 4100f;
        final float gravity = -9.81f;

        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        final byte type = buf.get();
        final int index = buf.get() & 0xff; // always incrementing, for each type

        if (type == 0x00) {
            // g-sensor x y z values, per second
            if ((value.length - 2) % 6 != 0) {
                LOG.warn("Raw sensor value for type 0 not divisible by 6");
                return;
            }

            for (int i = 2; i < value.length; i += 6) {
                final int x = (BLETypeConversions.toUint16(value, i) << 16) >> 16;
                final int y = (BLETypeConversions.toUint16(value, i + 2) << 16) >> 16;
                final int z = (BLETypeConversions.toUint16(value, i + 4) << 16) >> 16;

                final float gx = (x * gravity) / scaleFactor;
                final float gy = (y * gravity) / scaleFactor;
                final float gz = (z * gravity) / scaleFactor;
                sleepAsAndroidSender.onAccelChanged(gx, gy, gz);

                LOG.info("Raw sensor g: x={} y={} z={}", gx, gy, gz);
            }
        } else if (type == 0x01) {
            // TODO not sure what this is?
            if ((value.length - 2) % 4 != 0) {
                LOG.warn("Raw sensor value for type 1 not divisible by 4");
                return;
            }

            for (int i = 2; i < value.length; i += 4) {
                int val = BLETypeConversions.toUint32(value, i);
                LOG.info("Raw sensor 1: {}", val);
            }
        } else if (type == 0x07) {
            // Timestamp for the targetType, sent in intervals of ~10 seconds
            final int targetType = buf.get() & 0xff;
            final long tsMillis = buf.getLong();
            LOG.debug("Raw sensor timestamp for type={} index={}: {}", targetType, index, new Date(tsMillis));
        } else {
            LOG.warn("Unknown raw sensor type: {}", GB.hexdump(value));
        }
    }

    public boolean onCharacteristicChanged(final UUID characteristicUUID,
                                           final byte[] value) {
        if (HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ.equals(characteristicUUID)) {
            handleChunkedRead(value);
            return true;
        } if (HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_WRITE.equals(characteristicUUID)) {
            handleChunkedWrite(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_DATA.equals(characteristicUUID)) {
            fetcher.onActivityData(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_CONTROL.equals(characteristicUUID)) {
            fetcher.onActivityControl(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_SEND.equals(characteristicUUID)) {
            fileTransferService.onCharacteristicChanged(characteristicUUID, value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_RECEIVE.equals(characteristicUUID)) {
            fileTransferService.onCharacteristicChanged(characteristicUUID, value);
            return true;
        } else if (GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            heartRateService.handleHeartRate(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_DATA.equals(characteristicUUID)) {
            handleRawSensorData(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL.equals(characteristicUUID)) {
            if (firmwareUpdateOperation != null) {
                firmwareUpdateOperation.handleControlNotification(value);
            } else {
                LOG.error("Got firmware control, but there is no ongoing operation: {}", GB.hexdump(value));
            }
            return true;
        }  else {
            LOG.warn("Unhandled characteristic changed: {}, data: {}", characteristicUUID, GB.hexdump(value));
        }

        return false;
    }

    public int getMTU() {
        return mMTU;
    }

    public void setMtu(final int mtu) {
        if (mtu > 23 && !getDevicePrefs().allowHighMtu()) {
            LOG.warn("High MTU is not allowed, ignoring");
            return;
        }

        if (mtu < 23) {
            LOG.error("Device announced unreasonable low MTU of {}, ignoring", mtu);
            return;
        }

        LOG.debug("Setting mtu to {}", mtu);

        this.mMTU = mtu;
        this.huami2021ChunkedEncoder.setMTU(mtu);
    }

    /** @noinspection SwitchStatementWithTooFewBranches*/
    private void handleChunkedRead(final byte[] value) {
        switch (value[0]) {
            case 0x03:
                final boolean needsAck = huami2021ChunkedDecoder.decode(value);
                if (needsAck) {
                    sendChunkedAck();
                }
                return;
            default:
                LOG.warn("Unhandled chunked read payload of type {}", value[0]);
        }
    }

    /** @noinspection SwitchStatementWithTooFewBranches*/
    private void handleChunkedWrite(final byte[] value) {
        switch (value[0]) {
            case 0x04:
                final byte handle = value[2];
                final byte count = value[4];
                LOG.info("Got chunked ack, handle={}, count={}", handle, count);
                // TODO: We should probably update the handle and count on the encoder
                return;
            default:
                LOG.warn("Unhandled chunked write payload of type {}", value[0]);
        }
    }

    public void sendChunkedAck() {
        final byte handle = huami2021ChunkedDecoder.getLastHandle();
        final byte count = huami2021ChunkedDecoder.getLastCount();

        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder("send chunked ack");
        builder.write(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ, new byte[] {0x04, 0x00, handle, 0x01, count});
        builder.queue();
    }

    @Override
    public void handle2021Payload(final short type, final byte[] payload) {
        if (payload == null || payload.length == 0) {
            LOG.warn("Empty or null payload for {}", String.format("0x%04x", type));
            return;
        }

        final AbstractZeppOsService service = mServiceMap.get(type);
        if (service != null) {
            service.handlePayload(payload);
            return;
        }

        LOG.warn("Unhandled 2021 payload {}", String.format("0x%04x", type));
    }

    public void setEncryptionParameters(final int encryptedSequenceNr, final byte[] sharedSessionKey) {
        huami2021ChunkedEncoder.setEncryptionParameters(encryptedSequenceNr, sharedSessionKey);
        huami2021ChunkedDecoder.setEncryptionParameters(sharedSessionKey);
    }

    @Override
    public void onFileDownloadFinish(final String url, final String filename, final byte[] data) {
        LOG.info("File received: url={} filename={} length={}", url, filename, data.length);

        if (filename.startsWith("screenshot-")) {
            GBDeviceEventScreenshot gbDeviceEventScreenshot = new GBDeviceEventScreenshot(data);
            evaluateGBDeviceEvent(gbDeviceEventScreenshot);
            return;
        }

        if (url.startsWith("voicememo://")) {
            voiceMemosService.onFileDownloadFinish(url, filename, data);
            return;
        }

        final String fileDownloadsDir = "zepp-os-received-files";
        final File targetFile;
        try {
            final String validFilename = FileUtils.makeValidFileName(filename);
            final File targetFolder = new File(FileUtils.getExternalFilesDir(), fileDownloadsDir);
            //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
            targetFile = new File(targetFolder, validFilename);
        } catch (final IOException e) {
            LOG.error("Failed create folder to save file", e);
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            final File targetFolder = new File(FileUtils.getExternalFilesDir(), fileDownloadsDir);
            //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
            outputStream.write(data);
        } catch (final IOException e) {
            LOG.error("Failed to save file bytes", e);
        }
    }

    private byte bool(final boolean b) {
        return (byte) (b ? 1 : 0);
    }

    public void writeToChunked2021(final ZeppOsTransactionBuilder builder, final short type, final byte data, final boolean encrypt) {
        writeToChunked2021(builder, type, new byte[]{data}, encrypt);
    }

    public void writeToChunked2021(final ZeppOsTransactionBuilder builder, final short type, final byte[] data, final boolean encrypt) {
        huami2021ChunkedEncoder.write(chunk -> builder.write(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_WRITE, chunk), type, data, true, encrypt);
    }

    public void writeToChunked2021(final String taskName, final short type, final byte data, final boolean encrypt) {
        writeToChunked2021(taskName, type, new byte[]{data}, encrypt);
    }

    public void writeToChunked2021(final String taskName, final short type, final byte[] data, final boolean encrypt) {
        final ZeppOsTransactionBuilder builder = createZeppOsTransactionBuilder(taskName);
        writeToChunked2021(builder, type, data, encrypt);
        builder.queue();
    }

    public ZeppOsTransactionBuilder createZeppOsTransactionBuilder(final String taskName) {
        return communicator.createZeppOsTransactionBuilder(taskName);
    }
}
