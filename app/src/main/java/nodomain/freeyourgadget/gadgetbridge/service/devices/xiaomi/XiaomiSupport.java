/*  Copyright (C) 2023-2026 Andreas Shimokawa, José Rebelo, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid.SleepAsAndroidAction;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractBluetoothDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.SleepAsAndroidSender;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.AbstractXiaomiService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiCalendarService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiDataUploadService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiHealthService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiMusicService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiNotificationService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiPhonebookService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiRpkService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiScheduleService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiSystemService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiWatchfaceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiWeatherService;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiSupport extends AbstractBluetoothDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSupport.class);

    private final XiaomiAuthService authService = new XiaomiAuthService(this);
    private final XiaomiMusicService musicService = new XiaomiMusicService(this);
    private final XiaomiHealthService healthService = new XiaomiHealthService(this);
    private final XiaomiNotificationService notificationService = new XiaomiNotificationService(this);
    private final XiaomiScheduleService scheduleService = new XiaomiScheduleService(this);
    private final XiaomiWeatherService weatherService = new XiaomiWeatherService(this);
    private final XiaomiSystemService systemService = new XiaomiSystemService(this);
    private final XiaomiCalendarService calendarService = new XiaomiCalendarService(this);
    private final XiaomiWatchfaceService watchfaceService = new XiaomiWatchfaceService(this);
    private final XiaomiDataUploadService dataUploadService = new XiaomiDataUploadService(this);
    private final XiaomiPhonebookService phonebookService = new XiaomiPhonebookService(this);
    private final XiaomiRpkService rpkService = new XiaomiRpkService(this);


    private String cachedFirmwareVersion = null;
    private XiaomiConnectionSupport connectionSupport = null;
    private SleepAsAndroidSender sleepAsAndroidSender;
    private ScheduledExecutorService saaHintScheduler;
    private ScheduledExecutorService saaAlarmScheduler;

    private final Map<Integer, AbstractXiaomiService> mServiceMap = new LinkedHashMap<>() {{
        put(XiaomiAuthService.COMMAND_TYPE, authService);
        put(XiaomiMusicService.COMMAND_TYPE, musicService);
        put(XiaomiHealthService.COMMAND_TYPE, healthService);
        put(XiaomiNotificationService.COMMAND_TYPE, notificationService);
        put(XiaomiScheduleService.COMMAND_TYPE, scheduleService);
        put(XiaomiWeatherService.COMMAND_TYPE, weatherService);
        put(XiaomiSystemService.COMMAND_TYPE, systemService);
        put(XiaomiCalendarService.COMMAND_TYPE, calendarService);
        put(XiaomiWatchfaceService.COMMAND_TYPE, watchfaceService);
        put(XiaomiDataUploadService.COMMAND_TYPE, dataUploadService);
        put(XiaomiPhonebookService.COMMAND_TYPE, phonebookService);
        put(XiaomiRpkService.COMMAND_TYPE, rpkService);
    }};

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void setAutoReconnect(boolean enabled) {
        super.setAutoReconnect(enabled);
        if (this.connectionSupport != null) {
            this.connectionSupport.setAutoReconnect(enabled);
        }
    }

    private XiaomiConnectionSupport createConnectionSpecificSupport() {
        DeviceCoordinator.ConnectionType connType = getCoordinator().getConnectionType();

        if (connType == DeviceCoordinator.ConnectionType.BOTH) {
            connType = getDevicePrefs().getForcedConnectionTypeFromPrefs();
        }

        return switch (connType) {
            case BLE, BOTH -> new XiaomiBleSupport(this);
            case BT_CLASSIC -> new XiaomiSppSupport(this);
            default -> throw new IllegalArgumentException("Unknown connection type " + connType);
        };

    }

    public XiaomiConnectionSupport getConnectionSpecificSupport() {
        if (connectionSupport == null) {
            connectionSupport = createConnectionSpecificSupport();
        }

        return connectionSupport;
    }

    @Override
    public boolean connect() {
        if (getConnectionSpecificSupport() != null)
            return getConnectionSpecificSupport().connect();

        LOG.error("getConnectionSpecificSupport returned null, could not connect");
        return false;
    }

    @Override
    public void dispose() {
        for (final AbstractXiaomiService service : mServiceMap.values()) {
            service.dispose();
        }

        if (this.connectionSupport != null) {
            XiaomiConnectionSupport connectionSupport = this.connectionSupport;
            this.connectionSupport = null;
            connectionSupport.dispose();
        }
    }

    @Override
    public void setContext(final GBDevice device, final BluetoothAdapter adapter, final Context context) {
        // FIXME unsetDynamicState unsets the fw version, which causes problems..
        if (device.getFirmwareVersion() != null) {
            setCachedFirmwareVersion(device.getFirmwareVersion());
        }

        super.setContext(device, adapter, context);

        for (AbstractXiaomiService service : mServiceMap.values()) {
            service.setContext(context);
        }

        if (getConnectionSpecificSupport() != null) {
            getConnectionSpecificSupport().setContext(device, adapter, context);
        }

        if (device.getDeviceCoordinator().supportsSleepAsAndroid(device)) {
            sleepAsAndroidSender = new SleepAsAndroidSender(device);
            healthService.setSleepAsAndroidSender(sleepAsAndroidSender);
        }
    }

    public SleepAsAndroidSender getSleepAsAndroidSender() {
        return sleepAsAndroidSender;
    }

    public String getCachedFirmwareVersion() {
        return this.cachedFirmwareVersion;
    }

    public void setCachedFirmwareVersion(String version) {
        this.cachedFirmwareVersion = version;
    }

    public void onDisconnect() {
        // propagate disconnection to services
        for (AbstractXiaomiService service : mServiceMap.values()) {
            service.onDisconnect();
        }
    }

    public void handleCommandBytes(final byte[] plainValue) {
        LOG.debug("Got command: {}", GB.hexdump(plainValue));

        final XiaomiProto.Command cmd;
        try {
            cmd = XiaomiProto.Command.parseFrom(plainValue);
        } catch (final Exception e) {
            LOG.error("Failed to parse bytes as protobuf command payload", e);
            return;
        }

        final AbstractXiaomiService service = mServiceMap.get(cmd.getType());
        if (service != null) {
            service.handleCommand(cmd);
            return;
        }

        LOG.warn("Unexpected watch command type {}", cmd.getType());
    }

    @Override
    public void onSendConfiguration(final String config) {
        final Prefs prefs = getDevicePrefs();

        // Check if any of the services handles this config
        for (final AbstractXiaomiService service : mServiceMap.values()) {
            if (service.onSendConfiguration(config, prefs)) {
                return;
            }
        }

        LOG.warn("Unhandled config changed: {}", config);
    }

    @Override
    public void onSetTime() {
        systemService.setCurrentTime();

        if (getCoordinator().supportsCalendarEvents(getDevice())) {
            // TODO this should not be done here
            calendarService.syncCalendar();
        }
    }

    @Override
    public void onTestNewFunction(@Nullable Bundle options) {
        //sendCommand("test new function", 2, 29);
    }

    @Override
    public void onFindPhone(final boolean start) {
        systemService.onFindPhone(start);
    }

    @Override
    public void onFindDevice(final boolean start) {
        systemService.onFindWatch(start);
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        musicService.onSetPhoneVolume(volume);
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        healthService.onSetGpsLocation(location);
    }

    @Override
    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        scheduleService.onSetReminders(reminders);
    }

    @Override
    public void onSetWorldClocks(final ArrayList<? extends WorldClock> clocks) {
        scheduleService.onSetWorldClocks(clocks);
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        notificationService.onNotification(notificationSpec);
    }

    @Override
    public void onDeleteNotification(final int id) {
        notificationService.onDeleteNotification(id);
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        scheduleService.onSetAlarms(alarms);
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        notificationService.onSetCallState(callSpec);
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        notificationService.onSetCannedMessages(cannedMessagesSpec);
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
    public void onInstallApp(final Uri uri, @NonNull final Bundle options) {
        final XiaomiFWHelper fwHelper = new XiaomiFWHelper(uri, getContext());

        if (!fwHelper.isValid()) {
            LOG.warn("Uri {} is not valid", uri);
            return;
        }

        if (fwHelper.isFirmware()) {
            systemService.installFirmware(fwHelper);
        } else if (fwHelper.isWatchface()) {
            watchfaceService.installWatchface(fwHelper);
        }else if (fwHelper.isRpk()) {
            rpkService.installRpk(fwHelper);
        } else {
            LOG.warn("Unknown fwhelper for {}", uri);
        }
    }

    @Override
    public void onAppInfoReq() {
        watchfaceService.requestWatchfaceList();
        rpkService.requestRpkList();
    }

    @Override
    public void onAppStart(final UUID uuid, boolean start) {
        if (start) {
            watchfaceService.setWatchface(uuid);
        }
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        watchfaceService.deleteWatchface(uuid);
        rpkService.deleteRpk(uuid);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        healthService.onFetchRecordedData(dataTypes);
    }

    @Override
    public void onHeartRateTest() {
        healthService.onHeartRateTest();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        healthService.enableRealtimeStats(enable);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        healthService.enableRealtimeStats(enable);
    }

    @Override
    public void onEnableHeartRateSleepSupport(final boolean enable) {
        healthService.setHeartRateConfig();
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        healthService.setHeartRateConfig();
    }

    @Override
    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        calendarService.onAddCalendarEvent(calendarEventSpec);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, long id) {
        calendarService.onDeleteCalendarEvent(type, id);
    }

    @Override
    public void onSendWeather() {
        weatherService.onSendWeather();
    }

    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {
        //noinspection unchecked
        phonebookService.setContacts((List<Contact>) contacts);
    }

    public XiaomiCoordinator getCoordinator() {
        return (XiaomiCoordinator) gbDevice.getDeviceCoordinator();
    }

    protected void onAuthSuccess() {
        LOG.info("onAuthSuccess");

        getConnectionSpecificSupport().onAuthSuccess();

        if (GBApplication.getPrefs().syncTime()) {
            systemService.setCurrentTime();
        }

        for (final AbstractXiaomiService service : mServiceMap.values()) {
            service.initialize();
        }
    }

    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        getConnectionSpecificSupport().sendCommand(taskName, command);
    }

    public void sendCommand(final String taskName, final int type, final int subtype) {
        sendCommand(
                taskName,
                XiaomiProto.Command.newBuilder()
                        .setType(type)
                        .setSubtype(subtype)
                        .build()
        );
    }

    public XiaomiAuthService getAuthService() {
        return this.authService;
    }

    public XiaomiDataUploadService getDataUploadService() {
        return this.dataUploadService;
    }

    public XiaomiHealthService getHealthService() {
        return this.healthService;
    }

    public XiaomiRpkService getRpkService() {
        return this.rpkService;
    }

    public XiaomiWatchfaceService getWatchfaceService() {
        return this.watchfaceService;
    }

    @Override
    public void onSleepAsAndroidAction(final String action, final Bundle extras) {
        if (sleepAsAndroidSender == null) {
            LOG.warn("SaA sender not initialized, dropping {}", action);
            return;
        }
        if (!gbDevice.isInitialized()) {
            LOG.warn("Device not initialized, dropping SaA action {}", action);
            return;
        }
        try {
            sleepAsAndroidSender.validateAction(action);
        } catch (UnsupportedOperationException e) {
            return;
        }
        switch (action) {
            case SleepAsAndroidAction.CHECK_CONNECTED:
                sleepAsAndroidSender.confirmConnected();
                break;
            case SleepAsAndroidAction.START_TRACKING:
                healthService.startRawSensor();
                sleepAsAndroidSender.startTracking();
                break;
            case SleepAsAndroidAction.STOP_TRACKING:
                healthService.stopRawSensor();
                sleepAsAndroidSender.stopTracking();
                break;
            case SleepAsAndroidAction.SET_PAUSE: {
                long pauseTimestamp = extras.getLong("TIMESTAMP");
                long delay = pauseTimestamp > 0 ? pauseTimestamp - System.currentTimeMillis() : 0;
                sleepAsAndroidSender.pauseTracking(delay);
                break;
            }
            case SleepAsAndroidAction.SET_SUSPENDED: {
                boolean suspended = extras.getBoolean("SUSPENDED", false);
                sleepAsAndroidSender.pauseTracking(suspended);
                break;
            }
            case SleepAsAndroidAction.SET_BATCH_SIZE:
                sleepAsAndroidSender.setBatchSize(extras.getLong("SIZE", 12L));
                break;
            case SleepAsAndroidAction.HINT:
                triggerSleepAsAndroidHint(extras.getInt("REPEAT", 1));
                break;
            case SleepAsAndroidAction.SHOW_NOTIFICATION: {
                NotificationSpec spec = new NotificationSpec();
                spec.title = extras.getString("TITLE");
                spec.body = extras.getString("TEXT");
                notificationService.onNotification(spec);
                break;
            }
            case SleepAsAndroidAction.UPDATE_ALARM:
                setSleepAsAndroidAlarm(extras.getLong("TIMESTAMP"));
                break;
            case SleepAsAndroidAction.START_ALARM:
                scheduleSleepAsAndroidAlarmVibration(extras.getInt("DELAY", 60000));
                break;
            case SleepAsAndroidAction.STOP_ALARM:
                cancelSleepAsAndroidAlarmVibration();
                break;
            default:
                LOG.warn("Received unsupported SaA action: {}", action);
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

    private void triggerSleepAsAndroidHint(int repeat) {
        if (repeat <= 0) return;
        if (saaHintScheduler != null) {
            saaHintScheduler.shutdownNow();
        }
        saaHintScheduler = Executors.newSingleThreadScheduledExecutor();
        final int repeats = repeat;
        saaHintScheduler.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < repeats; i++) {
                        systemService.onFindWatch(true);
                        Thread.sleep(500);
                        systemService.onFindWatch(false);
                        if (i + 1 < repeats) Thread.sleep(300);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void scheduleSleepAsAndroidAlarmVibration(int delayMs) {
        cancelSleepAsAndroidAlarmVibration();
        if (delayMs == -1) return;
        saaAlarmScheduler = Executors.newSingleThreadScheduledExecutor();
        saaAlarmScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                triggerSleepAsAndroidHint(3);
            }
        }, Math.max(0, delayMs), TimeUnit.MILLISECONDS);
    }

    private void cancelSleepAsAndroidAlarmVibration() {
        if (saaAlarmScheduler != null) {
            saaAlarmScheduler.shutdownNow();
            saaAlarmScheduler = null;
        }
        if (saaHintScheduler != null) {
            saaHintScheduler.shutdownNow();
            saaHintScheduler = null;
        }
        systemService.onFindWatch(false);
    }

    @Override
    public String customStringFilter(final String inputString) {
        return StringUtils.replaceEach(inputString, EMOJI_SOURCE, EMOJI_TARGET);
    }

    public void setFeatureSupported(final String featureKey, final boolean supported) {
        LOG.debug("Setting feature {} -> {}", featureKey, supported ? "supported" : "not supported");
        evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(featureKey, supported));
    }

    private static final String[] EMOJI_SOURCE = new String[]{
            "\uD83D\uDE0D", // 😍
            "\uD83D\uDE18", // 😘
            "\uD83D\uDE02", // 😂
            "\uD83D\uDE0A", // 😊
            "\uD83D\uDE0E", // 😎
            "\uD83D\uDE09", // 😉
            "\uD83D\uDC8B", // 💋
            "\uD83D\uDC4D", // 👍
            "\uD83E\uDD23", // 🤣
            "\uD83D\uDC95", // 💕
            "\uD83D\uDE00", // 😀
            "\uD83D\uDE04", // 😄
            "\uD83D\uDE2D", // 😭
            "\uD83E\uDD7A", // 🥺
            "\uD83D\uDE4F", // 🙏
            "\uD83E\uDD70", // 🥰
            "\uD83E\uDD14", // 🤔
            "\uD83D\uDD25", // 🔥
            "\uD83D\uDE29", // 😩
            "\uD83D\uDE14", // 😔
            "\uD83D\uDE01", // 😁
            "\uD83D\uDC4C", // 👌
            "\uD83D\uDE0F", // 😏
            "\uD83D\uDE05", // 😅
            "\uD83E\uDD0D", // 🤍
            "\uD83D\uDC94", // 💔
            "\uD83D\uDE0C", // 😌
            "\uD83D\uDE22", // 😢
            "\uD83D\uDC99", // 💙
            "\uD83D\uDC9C", // 💜
            "\uD83C\uDFB6", // 🎶
            "\uD83D\uDE33", // 😳
            "\uD83D\uDC96", // 💖
            "\uD83D\uDE4C", // 🙌
            "\uD83D\uDCAF", // 💯
            "\uD83D\uDE48", // 🙈
            "\uD83D\uDE0B", // 😋
            "\uD83D\uDE11", // 😑
            "\uD83D\uDE34", // 😴
            "\uD83D\uDE2A", // 😪
            "\uD83D\uDE1C", // 😜
            "\uD83D\uDE1B", // 😛
            "\uD83D\uDE1D", // 😝
            "\uD83D\uDE1E", // 😞
            "\uD83D\uDE15", // 😕
            "\uD83D\uDC97", // 💗
            "\uD83D\uDC4F", // 👏
            "\uD83D\uDE10", // 😐
            "\uD83D\uDC49", // 👉
            "\uD83D\uDC9B", // 💛
            "\uD83D\uDC9E", // 💞
            "\uD83D\uDCAA", // 💪
            "\uD83C\uDF39", // 🌹
            "\uD83D\uDC80", // 💀
            "\uD83D\uDE31", // 😱
            "\uD83D\uDC98", // 💘
            "\uD83E\uDD1F", // 🤟
            "\uD83D\uDE21", // 😡
            "\uD83D\uDCF7", // 📷
            "\uD83C\uDF38", // 🌸
            "\uD83D\uDE08", // 😈
            "\uD83D\uDC48", // 👈
            "\uD83C\uDF89", // 🎉
            "\uD83D\uDC81", // 💁
            "\uD83D\uDE4A", // 🙊
            "\uD83D\uDC9A", // 💚
            "\uD83D\uDE2B", // 😫
            "\uD83D\uDE24", // 😤
            "\uD83D\uDC93", // 💓
            "\uD83C\uDF1A", // 🌚
            "\uD83D\uDC47", // 👇
            "\uD83D\uDE07", // 😇
            "\uD83D\uDC4A", // 👊
            "\uD83D\uDC51", // 👑
            "\uD83D\uDE13", // 😓
            "\uD83D\uDE3B", // 😻
            "\uD83D\uDD34", // 🔴
            "\uD83D\uDE25", // 😥
            "\uD83E\uDD29", // 🤩
            "\uD83D\uDE1A", // 😚
            "\uD83D\uDE37", // 😷
            "\uD83D\uDC4B", // 👋
            "\uD83D\uDCA5", // 💥
            "\uD83E\uDD2D", // 🤭
            "\uD83C\uDF1F", // 🌟
            "\uD83E\uDD71", // 🥱
            "\uD83D\uDCA9", // 💩
            "\uD83D\uDE80", // 🚀
    };

    private static final String[] EMOJI_TARGET = new String[]{
            "ꀂ", // 😍
            "ꀃ", // 😘
            "ꀄ", // 😂
            "ꀅ", // 😊
            "ꀆ", // 😎
            "ꀇ", // 😉
            "ꀈ", // 💋
            "ꀉ", // 👍
            "ꀊ", // 🤣
            "ꀋ", // 💕
            "ꀌ", // 😀
            "ꀍ", // 😄
            "ꀎ", // 😭
            "ꀏ", // 🥺
            "ꀑ", // 🙏
            "ꀒ", // 🥰
            "ꀓ", // 🤔
            "ꀔ", // 🔥
            "ꀗ", // 😩
            "ꀘ", // 😔
            "ꀙ", // 😁
            "ꀚ", // 👌
            "ꀛ", // 😏
            "ꀜ", // 😅
            "ꀝ", // 🤍
            "ꀞ", // 💔
            "ꀟ", // 😌
            "ꀠ", // 😢
            "ꀡ", // 💙
            "ꀢ", // 💜
            "ꀤ", // 🎶
            "ꀥ", // 😳
            "ꀦ", // 💖
            "ꀧ", // 🙌
            "ꀨ", // 💯
            "ꀩ", // 🙈
            "ꀫ", // 😋
            "ꀬ", // 😑
            "ꀭ", // 😴
            "ꀮ", // 😪
            "ꀯ", // 😜
            "ꀰ", // 😛
            "ꀱ", // 😝
            "ꀲ", // 😞
            "ꀳ", // 😕
            "ꀴ", // 💗
            "ꀵ", // 👏
            "ꀶ", // 😐
            "ꀷ", // 👉
            "ꀸ", // 💛
            "ꀹ", // 💞
            "ꀺ", // 💪
            "ꀻ", // 🌹
            "ꀼ", // 💀
            "ꀽ", // 😱
            "ꀾ", // 💘
            "ꀿ", // 🤟
            "ꁀ", // 😡
            "ꁁ", // 📷
            "ꁂ", // 🌸
            "ꁃ", // 😈
            "ꁄ", // 👈
            "ꁅ", // 🎉
            "ꁆ", // 💁
            "ꁇ", // 🙊
            "ꁈ", // 💚
            "ꁉ", // 😫
            "ꁊ", // 😤
            "ꁍ", // 💓
            "ꁎ", // 🌚
            "ꁏ", // 👇
            "ꁒ", // 😇
            "ꁓ", // 👊
            "ꁔ", // 👑
            "ꁕ", // 😓
            "ꁖ", // 😻
            "ꁗ", // 🔴
            "ꁘ", // 😥
            "ꁙ", // 🤩
            "ꁚ", // 😚
            "ꁜ", // 😷
            "ꁝ", // 👋
            "ꁞ", // 💥
            "ꁠ", // 🤭
            "ꁡ", // 🌟
            "ꁢ", // 🥱
            "ꁣ", // 💩
            "ꁤ", // 🚀
    };
}
