package nodomain.freeyourgadget.gadgetbridge.service.devices.keephealth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.keephealth.C60Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.keephealth.KeephealthBloodPressureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.keephealth.KeephealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.keephealth.KeephealthTemperatureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthBloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.KeephealthTemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class C60DeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(C60DeviceSupport.class);
    private ByteBuffer cmdBuff = null;
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    private final int CHUNK_SIZE = 20;

    private final byte CMD_DEVICE_DATA = 0x01;

    private final byte CMD_DEVICE_STATE = 0x02;

    private final byte CMD_USER_INFO = 0x03;

    private final byte CMD_DATETIME = 0x04;

    // TODO find more about
    // two fragment response
    private final byte[] CMD_GET_ALARM = {0x05, 0x00, 0x08, (byte) 0x80};

    private final byte CMD_INACTIVITY = 0x06;

    private final byte CMD_TARGET_DATA = 0x07;

    private final byte CMD_DO_NOT_DISTURB = 0x08;

    // TODO find more about
    private final byte[] CMD_GET_NOTICE = {0x09, 0x00, 0x00, (byte) 0x60};

    private final byte CMD_NOTIFICATION = 0x0A;
    private final byte CMD_NOTIFICATION_ARG_TYPE = 0x00;
    private final byte CMD_NOTIFICATION_ARG_TITLE = 0x01;
    private final byte CMD_NOTIFICATION_ARG_BODY = 0x02;
    private final byte CMD_NOTIFICATION_ARG_END = 0x03;


    private final byte CMD_PHONE_CONTROL = 0x10;
    private final byte[] CMD_PHONE_CONTROL_ARG_CAMERA_OPEN = {0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00};
    private final byte[] CMD_PHONE_CONTROL_ARG_CAMERA_TAKE_PHOTO = {0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00};
    private final byte[] CMD_PHONE_CONTROL_ARG_CAMERA_CLOSE = {0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00};


    private final byte CMD_STEPS = 0x20;
    private final byte CMD_STEPS_ARG_CURRENT = 0x00;
    private final byte CMD_STEPS_ARG_HISTORY = 0x01;

    private final byte[] CMD_GET_CURRENT_HEARTRATE = {0x21, 0x01, 0x00, 0x00, (byte) 0xc6};

    // TODO find more about
    // Obtain blood pressure and blood oxygen data
    private final byte[] CMD_GET_OXYGEN = {0x21, 0x01, 0x00, 0x07, (byte) 0x20};
    // TODO find more about
    // Obtaining automatic heart rate sampling data
    private final byte[] CMD_GET_HEARTRATE_SAMPLING = {0x21, 0x01, 0x00, 0x08, (byte) 0x76};

    private final byte CMD_HEARTRATE = 0x21;
    private final byte CMD_HEARTRATE_ARG_CURRENT = 0x00;
    private final byte CMD_HEARTRATE_ARG_HISTORY = 0x01;

    private final byte CMD_BATTERY = 0x27;

    private final byte CMD_TEMPERATURE = 0x2c;
    private final byte CMD_TEMPERATURE_ARG_HISTORY = 0x01;

    private final byte CMD_HYDRATION = 0x2e;

    private final byte CMD_HYDRATION_ARG_GET_REMINDER = 0x01;
    private final byte CMD_HYDRATION_ARG_SET_REMINDER = 0x02;

    // FIXME this variables should be removed and build all data from settings when all values are known
    private byte[] currentDeviceSettings = null;
    private byte[] currentDndSettings = null;

    private int daysAgo;
    private Calendar syncingDay;

    private final ConcurrentMap<Byte, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Byte, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private final ConcurrentMap<Byte, Integer> retryCounts = new ConcurrentHashMap<>();

    private final NotificationQueue notificationQueue = new NotificationQueue();

    private ScheduledFuture<?> startResponseTimeout(Byte cmdByte, Runnable sendAction, @Nullable Runnable afterFail, long timeoutMs) {
        return startResponseTimeout(cmdByte, sendAction, afterFail, timeoutMs, false);
    }
    private ScheduledFuture<?> startResponseTimeout(Byte cmdByte, Runnable sendAction, long timeoutMs) {
        return startResponseTimeout(cmdByte, sendAction, null, timeoutMs, false);
    }
    private ScheduledFuture<?> startResponseTimeout(Byte cmdByte, Runnable sendAction, @Nullable Runnable afterFail, long timeoutMs, boolean resetCount) {
        LOG.debug("pending {}", pending);
//        if (pending.getOrDefault(cmdByte, null) != null) {
//            return null;
//        }
        // create or reuse scheduler
        ScheduledExecutorService scheduler = schedulers.computeIfAbsent(cmdByte, k ->
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName("resp-timeout-" + k);
                    return t;
                })
        );

        if (resetCount) {
            LOG.debug("{} startResponseTimeout retryCounts.remove", GB.hexdump(new byte[]{cmdByte}));
            retryCounts.remove(cmdByte);
        }

        // compute and check retry count
        int attempt = retryCounts.compute(cmdByte, (k, v) -> (v == null) ? 1 : v + 1);

        if (attempt > 3) {
            LOG.debug("{} response timeout already fired 3 times, will not schedule further", GB.hexdump(new byte[]{cmdByte}));
            // shut down scheduler for this cmdByte to free resources
            ScheduledExecutorService s = schedulers.remove(cmdByte);
            if (s != null) {
                s.shutdownNow();
            }
            // remove retry count for this cmdByte
            retryCounts.remove(cmdByte);
            if (afterFail != null) {
                LOG.debug("{} firing afterFail", GB.hexdump(new byte[]{cmdByte}));
                afterFail.run();
            }
            return null;
        }
        LOG.debug("{} current attempt: {}", GB.hexdump(new byte[]{cmdByte}), attempt);

        sendAction.run();
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                startResponseTimeout(cmdByte, sendAction, afterFail, timeoutMs);
            }
            finally {
                pending.remove(cmdByte);
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);

        pending.put(cmdByte, future);
        return future;
    }

    private void cancelResponseTimeout(Byte cmdByte) {
        ScheduledFuture<?> future = pending.remove(cmdByte);
        if (future != null) {
            future.cancel(true);
            LOG.debug("{} cancelResponseTimeout retryCounts.remove", GB.hexdump(new byte[]{cmdByte}));
            retryCounts.remove(cmdByte);
        }
    }

    private void cleanupSchedulers() {
        for (Byte b : new ArrayList<>(schedulers.keySet())) {
            cancelResponseTimeout(b);
            ScheduledExecutorService s = schedulers.remove(b);
            if (s != null) s.shutdownNow();
        }
        schedulers.clear();
        pending.clear();
        retryCounts.clear();
    }

    public C60DeviceSupport() {
        super(LOG);
        addSupportedService(C60Constants.SERVICE);
//        addSupportedService(C60Constants.SERVICE_ACTIVE_UPLOAD);
//        addSupportedService(C60Constants.SERVICE_ECG);
//        addSupportedService(C60Constants.SERVICE_FFD2);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        notificationQueue.empty();
        cleanupSchedulers();
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        builder.notify(C60Constants.CHARACTERISTIC_READ, true);
//        builder.notify(C60Constants.READ_ECG, true);
//        builder.notify(C60Constants.READ_FFD2, true);
//        builder.notify(C60Constants.SERVICE_ACTIVE_UPLOAD_READ, true);
        // builder.requestMtu(23);
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        int wait = 100;
        builder.wait(wait);
        getDeviceData(builder);
        builder.wait(wait);
        getBatteryData(builder);
        builder.wait(wait);
        setTime(builder);
        builder.wait(wait);
        getDeviceState(builder);
        builder.wait(wait);
        getDndState(builder);
        builder.wait(wait);
        getInactivityState(builder);
        builder.wait(wait);
        getTargetData(builder);
        builder.wait(wait);
        getHydration(builder);
        builder.wait(wait);
//        getSteps(builder);
//        builder.wait(wait);
//        getHeartrate(builder);
//        builder.wait(wait);
//        getBodytemp(builder);
//        builder.wait(wait);
//        setDeviceState(builder);
//        builder.wait(wait);
//        setUserInfo(builder);
//        builder.wait(wait);
//        getTargetData(builder);
//        builder.wait(wait);
//        getNotice(builder);
//        builder.wait(wait);
//        getOxygen(builder);
//        builder.wait(wait);
//        getHrSampling(builder);
//        builder.wait(wait);
//        getAlarm(builder);
//        builder.wait(wait);
//        getStepsHistory(builder);
//        builder.wait(500);
//        getHeartrateHistory(builder);
//        builder.wait(500);

        builder.setDeviceState(GBDevice.State.INITIALIZED);

        return builder;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            LOG.info("Dispose");
            cleanupSchedulers();
            super.dispose();
        }
    }

    @Override
    public void onSetTime() {
        LOG.debug("set date and time");
        TransactionBuilder builder = createTransactionBuilder("Set date and time");
        setTime(builder);
        builder.queue();
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        GB.updateTransferNotification(getContext().getString(R.string.busy_task_fetch_activity_data), "", true, 0, getContext());
        daysAgo = 0;
        fetchHistoryActivity();
    }

    private void fetchRecordedDataFinished() {
        GB.updateTransferNotification(null, "", false, 100, getContext());
        LOG.info("Sync finished!");
        getDevice().unsetBusyTask();
        getDevice().sendDeviceUpdateIntent(getContext());
        GB.signalActivityDataFinish(getDevice());
    }

    private void fetchHistoryActivity() {
        getDevice().setBusyTask(R.string.busy_task_fetch_activity_data, getContext());
        getDevice().sendDeviceUpdateIntent(getContext());
        syncingDay = Calendar.getInstance();
        syncingDay.add(Calendar.DAY_OF_MONTH, 0 - daysAgo);
        syncingDay.set(Calendar.HOUR_OF_DAY, 0);
        syncingDay.set(Calendar.MINUTE, 0);
        syncingDay.set(Calendar.SECOND, 0);
        syncingDay.set(Calendar.MILLISECOND, 0);
        byte[] activityHistoryRequest = getStepsHistoryCommand(syncingDay);
        LOG.info("Fetch historical activity data request sent: {}", StringUtils.bytesToHex(activityHistoryRequest));
        startResponseTimeout(
                activityHistoryRequest[0],
                () -> sendWrite("activityHistoryRequest", activityHistoryRequest),
                null,
                10_000
        );
    }

    private void fetchHistoryHR() {
        getDevice().setBusyTask(R.string.busy_task_fetch_hr_data, getContext());
        getDevice().sendDeviceUpdateIntent(getContext());
        syncingDay = Calendar.getInstance();
        syncingDay.add(Calendar.DAY_OF_MONTH, 0 - daysAgo);
        syncingDay.set(Calendar.HOUR_OF_DAY, 0);
        syncingDay.set(Calendar.MINUTE, 0);
        syncingDay.set(Calendar.SECOND, 0);
        syncingDay.set(Calendar.MILLISECOND, 0);
        byte[] hrHistoryRequest = getHeartrateHistoryCommand(syncingDay);
        LOG.info("Fetch historical HR data request sent ({}): {}", DateTimeUtils.formatIso8601(syncingDay.getTime()), StringUtils.bytesToHex(hrHistoryRequest));
        startResponseTimeout(
                hrHistoryRequest[0],
                () -> sendWrite("hrHistoryRequest", hrHistoryRequest),
                null,
                10_000
        );
    }

    private void fetchHistoryTemperature() {
        getDevice().setBusyTask(R.string.busy_task_fetch_temperature, getContext());
        getDevice().sendDeviceUpdateIntent(getContext());
        syncingDay = Calendar.getInstance();
        syncingDay.add(Calendar.DAY_OF_MONTH, 0 - daysAgo);
        syncingDay.set(Calendar.HOUR_OF_DAY, 0);
        syncingDay.set(Calendar.MINUTE, 0);
        syncingDay.set(Calendar.SECOND, 0);
        syncingDay.set(Calendar.MILLISECOND, 0);
        byte[] tempHistoryRequest = getTemperatureHistoryCommand(syncingDay);
        LOG.info("Fetch historical temperature data request sent ({}): {}", DateTimeUtils.formatIso8601(syncingDay.getTime()), StringUtils.bytesToHex(tempHistoryRequest));
        startResponseTimeout(tempHistoryRequest[0],
                () -> sendWrite("temperatureHistoryRequest", tempHistoryRequest),
                null,
                10_000
        );
    }

    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] responseValue) {
        super.onCharacteristicChanged(gatt, characteristic, responseValue);

        UUID characteristicUUID = characteristic.getUuid();

        LOG.info("Characteristic changed UUID: {}", characteristicUUID);
        LOG.info("Characteristic changed value: {}", GB.hexdump(responseValue));

        ByteBuffer bb = ByteBuffer.wrap(responseValue).order(ByteOrder.LITTLE_ENDIAN);

        if (cmdBuff == null) {
            int expectedLength = bb.getShort(1) + 4; // 1 cmd, 2 length, 1 checksum
            LOG.info("Incoming data expected length: {}", expectedLength);
            cmdBuff = ByteBuffer.allocate(expectedLength);
        }

        cmdBuff.put(bb);

        LOG.info("cmdBuff.remaining() = {}", cmdBuff.remaining());

        if (cmdBuff.remaining() == 0) {
            byte[] value = cmdBuff.array();
            // when received all data delete buffer
            cmdBuff = null;
            if (responseChecksumValid(value)) {
                // get cmd based on response first byte - 0x80
                byte cmdPrefix = (byte) (value[0] - (byte) 0x80);
                LOG.info("Expected CMD prefix: {}", GB.hexdump(new byte[]{cmdPrefix}));
                cancelResponseTimeout(cmdPrefix);
                if (cmdPrefix == CMD_DEVICE_DATA) {
                    handleDeviceData(value);
                } else if (cmdPrefix == CMD_BATTERY) {
                    handleBatteryInfo(value);
                } else if (cmdPrefix == CMD_DEVICE_STATE) {
                    handleDeviceState(value);
                } else if (cmdPrefix == CMD_STEPS) {
                    handleSteps(value);
                    getDevice().unsetBusyTask();
                    getDevice().sendDeviceUpdateIntent(getContext());
                    if (!getDevice().isBusy()) {
                        if (daysAgo < 7) {
                            daysAgo++;
                            fetchHistoryActivity();
                        } else {
                            daysAgo = 0;
                            fetchHistoryHR();
                        }
                    }
                } else if (cmdPrefix == CMD_HEARTRATE) {
                    handleHeartrate(value);
                    getDevice().unsetBusyTask();
                    getDevice().sendDeviceUpdateIntent(getContext());
                    if (!getDevice().isBusy()) {
                        if (daysAgo < 7) {
                            daysAgo++;
                            fetchHistoryHR();
                        } else {
                            daysAgo = 0;
                            fetchHistoryTemperature();
                        }
                    }
                } else if (cmdPrefix == CMD_TEMPERATURE) {
                    handleTemperature(value);
                    getDevice().unsetBusyTask();
                    getDevice().sendDeviceUpdateIntent(getContext());
                    if (!getDevice().isBusy()) {
                        if (daysAgo < 7) {
                            daysAgo++;
                            fetchHistoryTemperature();
                        } else {
                            daysAgo = 0;
                            fetchRecordedDataFinished();
                        }
                    }
                } else if (cmdPrefix == CMD_DO_NOT_DISTURB) {
                    handleDoNotDisturb(value);
                } else if (cmdPrefix == CMD_INACTIVITY) {
                    handleInactivity(value);
                } else if (cmdPrefix == CMD_TARGET_DATA) {
                    handleTargetData(value);
                } else if (cmdPrefix == CMD_HYDRATION) {
                    handleHydration(value);
                } else if (cmdPrefix == CMD_NOTIFICATION) {
                    handleNotificationResponse(value);
                } else if (cmdPrefix == CMD_PHONE_CONTROL) {
                    handlePhoneControl(value);
                } else {
                    LOG.info("Unhandled data: {}", GB.hexdump(value));
                }
            } else {
                LOG.info("Received data have invalid checksum: {}", GB.hexdump(value));
            }
        }

        return false;
    }

    @Override
    public void onSendConfiguration(String config) {
        final Prefs prefs = getDevicePrefs();
        byte[] configPacket = null;
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
            case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
            case DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED:
            case DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE:
                configPacket = setDeviceStateCommand(prefs);
                break;
            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO:
            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START:
            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END:
                configPacket = setDoNotDisturbCommand(prefs);
                break;
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_START:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_END:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_MO:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_TU:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_WE:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_TH:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_FR:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_SA:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_SU:
                configPacket = setInactivityCommand(prefs);
                break;
            case DeviceSettingsPreferenceConst.PREF_USER_FITNESS_GOAL_NOTIFICATION:
            case DeviceSettingsPreferenceConst.PREF_USER_FITNESS_GOAL:
            case ActivityUser.PREF_USER_CALORIES_BURNT:
            case ActivityUser.PREF_USER_DISTANCE_METERS:
                configPacket = setGoalCommand(prefs);
                break;
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_START:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_END:
                configPacket = setHydrationCommand(prefs);
                break;
            case ActivityUser.PREF_USER_GENDER:
            case ActivityUser.PREF_USER_DATE_OF_BIRTH:
            case ActivityUser.PREF_USER_HEIGHT_CM:
            case ActivityUser.PREF_USER_WEIGHT_KG:
            case ActivityUser.PREF_USER_STEP_LENGTH_CM:
                configPacket = setUserInfoCommand(prefs);
                break;
            default:
                LOG.debug("Unknown pref: {}", config);
        }

        if (configPacket == null) { return; }
        LOG.debug("send config: {} - {}", config, StringUtils.bytesToHex(configPacket));
        byte[] finalConfigPacket = configPacket;
        startResponseTimeout(
                configPacket[0],
                () -> sendWrite("onSendConfigurationRequest", finalConfigPacket),
                () -> GB.toast(getContext().getString(R.string.save_configuration)  + " " + getContext().getString(R.string.work_info_status_failed), Toast.LENGTH_SHORT, GB.ERROR),
                5000,
                true
        );
    }

    @Override
    public void onFindDevice(boolean start) {
        if (!start) return;
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) 0x10);
        buf.put((byte) 8);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 1);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0xc0);

        sendWrite("onFindDevice", buf.array());
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            String callerStr;
            if (!StringUtils.isNullOrEmpty(callSpec.name) && !StringUtils.isNullOrEmpty(callSpec.number)) {
                callerStr = callSpec.name + ": " + callSpec.number;
            } else if (!StringUtils.isNullOrEmpty(callSpec.name)) {
                callerStr = callSpec.name;
            } else if (!StringUtils.isNullOrEmpty(callSpec.number)) {
                callerStr = callSpec.number;
            } else {
                callerStr = "?";
            }

            notificationQueue.addToQueue(new NotificationItem(callerStr, null, KeepHealthNotificationType.CALL));
            sendNotification();
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String titleStr = notificationSpec.title;
        String bodyStr = notificationSpec.body;
        KeepHealthNotificationType type = KeepHealthNotificationType.fromNotificationType(notificationSpec.type);
        notificationQueue.addToQueue(new NotificationItem(titleStr, bodyStr, type));
        LOG.debug("notificationQueue: {}", notificationQueue);
        sendNotification();
    }

    public void onCameraStatusChange(
            GBDeviceEventCameraRemote.Event event,
            String filename
    ) {
        int timeout = 1000;
        byte[] payload;
        switch (event) {
            case OPEN_CAMERA:
                payload = CMD_PHONE_CONTROL_ARG_CAMERA_OPEN;
                break;
            case CLOSE_CAMERA:
                payload = CMD_PHONE_CONTROL_ARG_CAMERA_CLOSE;
                break;
            default:
                LOG.warn("Unknown camera status change {}", event);
                return;
        }
        byte[] setCommand = buildCommand(
                CMD_PHONE_CONTROL,
                payload
        );
        sendWrite("open camera", setCommand);
    }

    private void sendNotification() {
        sendNotification(CMD_NOTIFICATION_ARG_TYPE);
    }

    private void sendNotification(byte stage) {
        if (notificationQueue.isRunning() && stage == CMD_NOTIFICATION_ARG_TYPE) return;
        notificationQueue.setRunning(true);
        long timeout = 2000;
        String builder = "send notification";
        if (stage == CMD_NOTIFICATION_ARG_TYPE) {
            byte[] setTypeCommand = buildCommand(
                    CMD_NOTIFICATION,
                    new byte[]{CMD_NOTIFICATION_ARG_TYPE, notificationQueue.get().type.getCode()}
            );
            LOG.debug("write type: {}", GB.hexdump(setTypeCommand));
            startResponseTimeout(
                    CMD_NOTIFICATION,
                    () -> sendWrite(builder, setTypeCommand),
                    notificationQueue::finish,
                    timeout
            );
        } else if (stage == CMD_NOTIFICATION_ARG_TITLE) {
            byte[] titleBytes = notificationQueue.get().title.getBytes();
            byte[] titleData = getByteBuffer(1 + titleBytes.length)
                    .put(CMD_NOTIFICATION_ARG_TITLE)
                    .put(titleBytes, 0, Math.min(32, titleBytes.length))
                    .array();
            byte[] setTitleCommand = buildCommand(
                    CMD_NOTIFICATION,
                    titleData
            );
            LOG.debug("write title: {}", GB.hexdump(setTitleCommand));
            startResponseTimeout(
                    CMD_NOTIFICATION,
                    () -> sendWrite(builder, setTitleCommand),
                    notificationQueue::finish,
                    timeout
            );
        } else if (stage == CMD_NOTIFICATION_ARG_BODY) {
            String notificationBodyString = notificationQueue.get().body;
            if (notificationBodyString == null) { notificationBodyString = ""; }
            byte[] bodyBytes = notificationBodyString.getBytes();
            byte[] bodyData = getByteBuffer(1 + bodyBytes.length)
                    .put(CMD_NOTIFICATION_ARG_BODY)
                    .put(bodyBytes, 0, Math.min(128, bodyBytes.length))
                    .array();
            byte[] setBodyCommand = buildCommand(
                    CMD_NOTIFICATION,
                    bodyData
            );
            LOG.debug("write body: {}", GB.hexdump(setBodyCommand));
            startResponseTimeout(
                    CMD_NOTIFICATION,
                    () -> sendWrite(builder, setBodyCommand),
                    notificationQueue::finish,
                    timeout
            );
        } else if (stage == CMD_NOTIFICATION_ARG_END) {
            byte[] setEndCommand = buildCommand(
                    CMD_NOTIFICATION,
                    CMD_NOTIFICATION_ARG_END
            );
            LOG.debug("write end: {}", GB.hexdump(setEndCommand));
            startResponseTimeout(
                    CMD_NOTIFICATION,
                    () -> sendWrite(builder, setEndCommand),
                    notificationQueue::finish,
                    timeout
            );
        }
    }

    private byte[] buildCommand(byte cmd) {
        return buildCommand(cmd, new byte[]{});
    }

    private byte[] buildCommand(byte cmd, byte data) {
        return buildCommand(cmd, new byte[]{data});
    }

    private byte[] buildCommand(byte cmd, byte[] data) {
        // +4, 1 cmd, 2 length, 1 checksum
        ByteBuffer buf = ByteBuffer.allocate(data.length + 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(cmd);
        buf.putShort((short) (data.length));
        buf.put(data);
        buf.put(getChecksum(buf.array()));
        return buf.array();
    }

    private void writeInBuilder(TransactionBuilder builder, byte[] data, int wait) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(C60Constants.CHARACTERISTIC_WRITE);
        builder.writeChunkedData(characteristic, data, CHUNK_SIZE);
        if (wait > 0) {
            builder.wait(wait);
        }
    }

    private void writeInBuilder(TransactionBuilder builder, byte[] data) {
        writeInBuilder(builder, data, 0);
    }

    private void sendWrite(String taskName, byte[] contents) {
        TransactionBuilder builder = createTransactionBuilder(taskName);
        sendWriteBuilder(builder, contents);
    }

    private void sendWriteBuilder(TransactionBuilder builder, byte[] contents) {
        writeInBuilder(builder, contents);
        builder.queue();
    }

    private void handleBatteryInfo(byte[] info) {
        LOG.debug("Battery info: " + GB.hexdump(info));
        var level = info[3];
        if (level == (byte) 0xff) {
            batteryCmd.state = BatteryState.BATTERY_CHARGING;
        } else {
            batteryCmd.state = BatteryState.BATTERY_NORMAL;
            batteryCmd.level = Math.min(info[3], 100);
        }
        handleGBDeviceEvent(batteryCmd);
    }

    private void handleDeviceData(byte[] info) {
        LOG.debug("Device Data: " + GB.hexdump(info));
        String model = new String(info, 3, 8);
        int major = Byte.toUnsignedInt(info[11]);
        int minor = Byte.toUnsignedInt(info[12]);
        String version = major + "." + (minor < 10 ? "0" + minor : Integer.toString(minor));
        getDevice().setModel(model + " v" + version);
        getDevice().setFirmwareVersion(version);
    }

    private void handleDeviceState(byte[] info) {
        LOG.debug("Device State: " + GB.hexdump(info));
        if (info.length == 5 && info[3] == 0x00) {
            GB.toast(getContext().getString(R.string.save_configuration)  + " " + getContext().getString(R.string.ok), Toast.LENGTH_SHORT, GB.INFO);
        }
        if (info.length == 20) {
            this.currentDeviceSettings = trimData(info);
            Prefs prefs = getDevicePrefs();
            SharedPreferences sharedPrefs = prefs.getPreferences();
            String langCode = "en_US";
            for (Map.Entry<String,Integer> e : C60Constants.LANGUAGES.entrySet()) {
                if (e.getValue() == info[6]) langCode = e.getKey();
            }
            sharedPrefs.edit()
                    .putString(DeviceSettingsPreferenceConst.PREF_LANGUAGE, langCode)
                    .putString(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, info[8] == 0x01 ? DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H : DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H)
                    .putBoolean(DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED, info[9] == 0x01)
                    .apply();
            LOG.debug("Saved Device State: " + GB.hexdump(this.currentDeviceSettings));
        }
    }

    private void handleSteps(byte[] data) {
        if (data[3] == 0) {
            LOG.debug("Current steps data: " + GB.hexdump(data));
            ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int totalSteps    = bb.getInt(4);
            int totalCalories = bb.getInt(8);
            int totalDistance = bb.getInt(12);
            GB.toast("totalSteps: " +  totalSteps + " | totalCalories: " + totalCalories + " | totalDistance: " + totalDistance, Toast.LENGTH_LONG, GB.INFO);
        } else if (data[3] == 1) {
            if (data[4] == 5) {
                LOG.debug("No history steps data for this date");
            } else {
                LOG.debug("History steps data: " + GB.hexdump(data));
                ActivityUser activityUser = new ActivityUser();
                int weightKg = activityUser.getWeightKg();
                int stepCm = activityUser.getStepLengthCm();

                ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

                // Header (9 bytes)
                byte header0 = buf.get();                    // [0]
                short length = buf.getShort();            // [1..2]
                byte subtype = buf.get();                    // [3]
                int year = buf.getShort() & 0xFFFF;          // [4..5]
                int month = buf.get() & 0xFF;                // [6]
                int day = buf.get() & 0xFF;                  // [7]
                int interval = buf.get() & 0xFF;             // [8] minutes per sample

                int samplesPerDay = 1440 / interval;

                KeephealthActivitySample[] activitySample = new KeephealthActivitySample[samplesPerDay];

                try (DBHandler db = GBApplication.acquireDB()) {
                    Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
                    KeephealthSampleProvider sampleProvider = new KeephealthSampleProvider(getDevice(), db.getDaoSession());

                    for (int sampleIndex = 0; sampleIndex < samplesPerDay && buf.remaining() >= 2; sampleIndex++) {
                        int raw = buf.getShort() & 0xFFFF;         // little-endian 16-bit
                        int flag = (raw >> 12) & 0xF;             // top 4 bits
                        int value = raw & 0x0FFF;                 // lower 12 bits

                        int hour = sampleIndex / (60 / interval);
                        int minute = (sampleIndex % (60 / interval)) * interval;
                        int timestamp = (int) (buildTimestamp(year, month, day, hour, minute) / 1000); // implement to produce epoch seconds or ms as needed

                        activitySample[sampleIndex] = new KeephealthActivitySample(timestamp, deviceId);

                        if (flag == 0xF) {
                            int sleepStatus = (value >> 8) & 0xF;
                            activitySample[sampleIndex].setRawKind(sleepStatus);
                            LOG.debug("sample {} time {}:{} sleepStatus {}", sampleIndex, hour, minute, sleepStatus);
                        } else {
                            int activeCalories = (int) (((stepCm * weightKg * value) * 0.78) / 100);
                            int distanceCm = value * stepCm;
                            activitySample[sampleIndex].setSteps(value);
                            activitySample[sampleIndex].setActiveCalories(activeCalories);
                            activitySample[sampleIndex].setDistanceCm(distanceCm);
                            LOG.debug("sample {} time {}:{} steps {} activeCalories {} distanceCm {}", sampleIndex, hour, minute, value, activeCalories, distanceCm);
                        }
                    }

                    sampleProvider.addGBActivitySamples(Arrays.asList(activitySample));
                } catch (Exception e) {
                    LOG.error("Error acquiring database", e);
                }
            }
        } else {
            LOG.debug("Cmd arg: " + GB.hexdump(new byte[]{data[3]}));
            LOG.debug("other steps data: " + GB.hexdump(data));
        }
    }

    private void handleHeartrate(byte[] data) {
        if (data[3] == 0) {
            LOG.debug("Current heartrate data: " + GB.hexdump(data));
        } else if (data[3] == 1) {
            if (data[4] == 5) {
                LOG.debug("No history heartrate data for this date");
            } else {
                LOG.debug("History heart/bp data: " + GB.hexdump(data));
                ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

                // Header (9 bytes)
                byte header0 = buf.get();                  // [0]
                short length = buf.getShort();             // [1..2]
                byte subtype = buf.get();                  // [3]
                int year = buf.getShort() & 0xFFFF;        // [4..5]
                int month = buf.get() & 0xFF;              // [6]
                int day = buf.get() & 0xFF;                // [7]
                int interval = buf.get() & 0xFF;           // [8] minutes per sample

                int samplesPerDay = 1440 / interval;

                // allocate arrays
                List<GenericHeartRateSample> samples = new ArrayList<>();
                List<GenericSpo2Sample> spo2Samples = new ArrayList<>();
                List<KeephealthBloodPressureSample> bpSamples = new ArrayList<>();

                try (DBHandler db = GBApplication.acquireDB()) {
                    Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                    Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
                    GenericHeartRateSampleProvider sampleProvider = new GenericHeartRateSampleProvider(getDevice(), db.getDaoSession());
                    GenericSpo2SampleProvider spo2SampleProvider = new GenericSpo2SampleProvider(getDevice(), db.getDaoSession());
                    KeephealthBloodPressureSampleProvider bpSampleProvider = new KeephealthBloodPressureSampleProvider(getDevice(), db.getDaoSession());

                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.set(year, month - 1, day, 0, 0, 0);

                    for (int sampleIndex = 0; sampleIndex < samplesPerDay && buf.remaining() >= 4; sampleIndex++) {
                        int hr = buf.get() & 0xFF;           // heart rate
                        int fz = buf.get() & 0xFF;           // blood pressure fz (diastolic/systolic order in original)
                        int ss = buf.get() & 0xFF;           // blood pressure ss
                        int oxy = buf.get() & 0xFF;          // oxygen

                        int hour = sampleIndex / (60 / interval);
                        int minute = (sampleIndex % (60 / interval)) * interval;
                        cal.set(Calendar.HOUR_OF_DAY, hour);
                        cal.set(Calendar.MINUTE, minute);
                        cal.set(Calendar.SECOND, 0);
                        long timestamp = buildTimestamp(year, month, day, hour, minute);

                        // create sample
                        GenericHeartRateSample sample = new GenericHeartRateSample(timestamp, deviceId);
                        sample.setHeartRate(hr);
                        samples.add(sample);

                        GenericSpo2Sample spo2Sample = new GenericSpo2Sample(timestamp, deviceId);
                        spo2Sample.setSpo2(oxy);
                        spo2Samples.add(spo2Sample);

                        KeephealthBloodPressureSample bpSample = new KeephealthBloodPressureSample(timestamp, deviceId);
                        bpSample.setBpDiastolic(Math.min(fz, ss));   // original code orders values to ss/fz but store both
                        bpSample.setBpSystolic(Math.max(fz, ss));
                        bpSamples.add(bpSample);

                        LOG.debug("sample {} time {}:{} timestamp: {} hr {}", sampleIndex, hour, minute, timestamp, hr);
                        LOG.debug("sample {} time {}:{} bp {}/{} oxy {}", sampleIndex, hour, minute, Math.min(fz, ss), Math.max(fz, ss), oxy);
                    }

                    sampleProvider.addSamples(samples);
                    spo2SampleProvider.addSamples(spo2Samples);
                    bpSampleProvider.addSamples(bpSamples);
                } catch (Exception e) {
                    LOG.error("Error acquiring database", e);
                }
            }
        } else {
            LOG.debug("Cmd arg: " + GB.hexdump(new byte[]{data[3]}));
            LOG.debug("other heartrate data: " + GB.hexdump(data));
        }
    }

    private void handleTemperature(byte[] data) {
        LOG.debug("Some temperature data: " + GB.hexdump(data));
        if (data[3] == 0) {
            LOG.debug("Current temperature data: " + GB.hexdump(data));
        } else if (data[3] == 1) {
            if (data[4] == 5) {
                LOG.debug("No history temperature data for this date");
            } else {
                LOG.debug("History temp data: " + GB.hexdump(data));
                ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

                // Header (9 bytes)
                byte header0 = buf.get();                  // [0]
                short length = buf.getShort();             // [1..2]
                byte subtype = buf.get();                  // [3]
                int year = buf.getShort() & 0xFFFF;        // [4..5]
                int month = buf.get() & 0xFF;              // [6]
                int day = buf.get() & 0xFF;                // [7]
                int interval = buf.get() & 0xFF;           // [8] minutes per sample

                int samplesPerDay = 1440 / interval;

                // allocate arrays
                List<KeephealthTemperatureSample> samples = new ArrayList<>();

                try (DBHandler db = GBApplication.acquireDB()) {
                    Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
                    KeephealthTemperatureSampleProvider sampleProvider = new KeephealthTemperatureSampleProvider(getDevice(), db.getDaoSession());

                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.set(year, month - 1, day, 0, 0, 0);

                    for (int sampleIndex = 0; sampleIndex < samplesPerDay && buf.remaining() >= 4; sampleIndex++) {
                        float temp = (buf.getShort() & 0xFFFF) / 100f;

                        int hour = sampleIndex / (60 / interval);
                        int minute = (sampleIndex % (60 / interval)) * interval;
                        cal.set(Calendar.HOUR_OF_DAY, hour);
                        cal.set(Calendar.MINUTE, minute);
                        cal.set(Calendar.SECOND, 0);
                        long timestamp = buildTimestamp(year, month, day, hour, minute);

                        // create sample
                        KeephealthTemperatureSample sample = new KeephealthTemperatureSample(timestamp, deviceId);
                        sample.setTemperature(temp);
                        samples.add(sample);

                        LOG.debug("sample {} time {}:{} timestamp: {} temp {}", sampleIndex, hour, minute, timestamp, temp);
                    }

                    sampleProvider.addSamples(samples);
                } catch (Exception e) {
                    LOG.error("Error acquiring database", e);
                }
            }
        } else {
            LOG.debug("Cmd arg: " + GB.hexdump(new byte[]{data[3]}));
            LOG.debug("other heartrate data: " + GB.hexdump(data));
        }
    }

    private void handleDoNotDisturb(byte[] data) {
        byte[] dndPrefix = {(byte)0x88, 0x10, 0x00};

        if (data.length >= dndPrefix.length && Arrays.equals(Arrays.copyOfRange(data, 0, dndPrefix.length), dndPrefix)) {
            LOG.debug("Current DND settings: " + GB.hexdump(data));
            if (data.length == 20) {
                this.currentDndSettings = trimData(data);
                Prefs prefs = getDevicePrefs();
                SharedPreferences sharedPrefs = prefs.getPreferences();
                sharedPrefs.edit()
                        .putString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO, data[3] == (byte) 0xff ? DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SCHEDULED : DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_OFF)
                        .putString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START, (int)data[4] + ":" + (int)data[5])
                        .putString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END, (int)data[6] + ":" + (int)data[7])
                        .apply();
                LOG.debug("Saved DND settings: " + GB.hexdump(this.currentDndSettings));
            }
        } else {
            LOG.debug("other sleep/dnd data: " + GB.hexdump(data));
        }
    }

    private void handleInactivity(byte[] data) {
        if (data.length == 5 && data[3] == 0x00) {
            GB.toast(getContext().getString(R.string.save_configuration)  + " " + getContext().getString(R.string.ok), Toast.LENGTH_SHORT, GB.INFO);
        }
        if (data.length == 9) {
            Prefs prefs = getDevicePrefs();
            SharedPreferences sharedPrefs = prefs.getPreferences();
            int rawMask = data[6] & 0xFF;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime start = LocalTime.of(data[4],0);
            LocalTime end = LocalTime.of(data[5], 0);
            sharedPrefs.edit()
                .putBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, data[3] == (byte) 0x01)
                .putString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, String.valueOf(((int)data[7] * 5)))
                .putString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, start.format(fmt))
                .putString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, end.format(fmt))
                .putBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_MO, (rawMask & WeekdayMask.MON_BIT) != 0)
                .putBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_TU, (rawMask & WeekdayMask.TUE_BIT) != 0)
                .putBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_WE, (rawMask & WeekdayMask.WED_BIT) != 0)
                .putBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_TH, (rawMask & WeekdayMask.THU_BIT) != 0)
                .putBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_FR, (rawMask & WeekdayMask.FRI_BIT) != 0)
                .putBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_SA, (rawMask & WeekdayMask.SAT_BIT) != 0)
                .putBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_SU, (rawMask & WeekdayMask.SUN_BIT) != 0)
                .apply();
        }
    }

    private void handleTargetData(byte[] data) {
        if (data.length == 5 && data[3] == 0x00) {
            GB.toast(getContext().getString(R.string.save_configuration)  + " " + getContext().getString(R.string.ok), Toast.LENGTH_SHORT, GB.INFO);
        }
        if (data.length == 9) {
            // TODO should i do something with it?
            LOG.debug("Received target data");
        }
    }

    private void handleHydration(byte[] data) {
        if (data.length == 6 && data[3] == 0x02 && data[4] == 0x00) {
            GB.toast(getContext().getString(R.string.save_configuration)  + " " + getContext().getString(R.string.ok), Toast.LENGTH_SHORT, GB.INFO);
        }
        if (data.length == 27) {
            Prefs prefs = getDevicePrefs();
            SharedPreferences sharedPrefs = prefs.getPreferences();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime start = LocalTime.of(data[10], data[11]);
            LocalTime end = LocalTime.of(data[24], data[25]);
            sharedPrefs.edit()
                    .putBoolean(DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH, data[4] == (byte) 0xff)
                    .putString(DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_START, start.format(fmt))
                    .putString(DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_END, end.format(fmt))
                    .apply();
            LOG.debug("saved hydration ({}) from: {} to: {}", data[4] == (byte) 0xff, start.format(fmt), end.format(fmt));
        }
    }

    private void handleNotificationResponse(byte[] data) {
        LOG.debug("handleNotificationResponse: arg - {}, code - {}", data[3], data[4]);
        if (data[4] == 0) {
            if (data[3] == CMD_NOTIFICATION_ARG_TYPE) {
                sendNotification(CMD_NOTIFICATION_ARG_TITLE);
            } else if (data[3] == CMD_NOTIFICATION_ARG_TITLE) {
                sendNotification(CMD_NOTIFICATION_ARG_BODY);
            } else if (data[3] == CMD_NOTIFICATION_ARG_BODY) {
                sendNotification(CMD_NOTIFICATION_ARG_END);
            } else if (data[3] == CMD_NOTIFICATION_ARG_END) {
                notificationQueue.finish();
                if (!notificationQueue.isEmpty()) {
                    sendNotification();
                }
            }
        }
    }

    private void handlePhoneControl(byte[] data) {
        byte[] trimmedData = trimData(data);
        LOG.debug("handlePhoneControl: payload - {}", GB.hexdump(trimmedData));
        if (trimmedData[0] == 0x00) {
            return;
        }
        if (trimmedData[2] == 0x02) {
            GBDeviceEventCameraRemote cameraEvent = new GBDeviceEventCameraRemote();
            cameraEvent.event = GBDeviceEventCameraRemote.Event.TAKE_PICTURE;
            evaluateGBDeviceEvent(cameraEvent);
        } else if (trimmedData[2] == 0x03) {
            GBDeviceEventCameraRemote cameraEvent = new GBDeviceEventCameraRemote();
            cameraEvent.event = GBDeviceEventCameraRemote.Event.CLOSE_CAMERA;
            evaluateGBDeviceEvent(cameraEvent);
        }
    }

    private long buildTimestamp(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, 0);
        return cal.getTimeInMillis();
    }

    public void getDeviceData(TransactionBuilder builder) {
        writeInBuilder(
                builder,
                buildCommand(CMD_DEVICE_DATA)
        );
    }

    public void getBatteryData(TransactionBuilder builder) {
        writeInBuilder(
                builder,
                buildCommand(CMD_BATTERY)
        );
    }

    public void setTime(TransactionBuilder builder) {
        final Calendar calendar = Calendar.getInstance();
        ByteBuffer buf = getByteBuffer(8)
                .putShort((short) calendar.get(Calendar.YEAR))
                .put((byte) (calendar.get(Calendar.MONTH) + 1))
                .put((byte) calendar.get(Calendar.DAY_OF_MONTH))
                .put((byte) calendar.get(Calendar.HOUR_OF_DAY))
                .put((byte) calendar.get(Calendar.MINUTE))
                .put((byte) calendar.get(Calendar.SECOND))
                .put((byte) 0);
        writeInBuilder(
                builder,
                buildCommand(
                        CMD_DATETIME,
                        buf.array()
                )
        );
    }

    public void getDeviceState(TransactionBuilder builder) {
        writeInBuilder(
                builder,
                buildCommand(CMD_DEVICE_STATE)
        );
    }

    public void getDndState(TransactionBuilder builder) {
        writeInBuilder(
                builder,
                buildCommand(CMD_DO_NOT_DISTURB)
        );
    }
    public void getInactivityState(TransactionBuilder builder) {
        writeInBuilder(
                builder,
                buildCommand(CMD_INACTIVITY)
        );
    }

    public void getSteps(TransactionBuilder builder) {
        writeInBuilder(
                builder,
                buildCommand(CMD_STEPS, CMD_STEPS_ARG_CURRENT)
        );
    }

    public byte[] getStepsHistoryCommand(Calendar calendar) {
        ByteBuffer buf = getByteBuffer(5)
            .put(CMD_STEPS_ARG_HISTORY)
            .putShort((short) calendar.get(Calendar.YEAR))
            .put((byte) (calendar.get(Calendar.MONTH) + 1))
            .put((byte) calendar.get(Calendar.DAY_OF_MONTH));
        return buildCommand(
            CMD_STEPS,
            buf.array()
        );
    }

    public byte[] getHeartrateHistoryCommand(Calendar calendar) {
        ByteBuffer buf = getByteBuffer(5)
                .put(CMD_HEARTRATE_ARG_HISTORY)
                .putShort((short) calendar.get(Calendar.YEAR))
                .put((byte) (calendar.get(Calendar.MONTH) + 1))
                .put((byte) calendar.get(Calendar.DAY_OF_MONTH));
        return buildCommand(
                CMD_HEARTRATE,
                buf.array()
        );
    }

    public byte[] getTemperatureHistoryCommand(Calendar calendar) {
        ByteBuffer buf = getByteBuffer(5)
                .put(CMD_TEMPERATURE_ARG_HISTORY)
                .putShort((short) calendar.get(Calendar.YEAR))
                .put((byte) (calendar.get(Calendar.MONTH) + 1))
                .put((byte) calendar.get(Calendar.DAY_OF_MONTH));
        return buildCommand(
                CMD_TEMPERATURE,
                buf.array()
        );
    }

    public C60DeviceSupport getHeartrate(TransactionBuilder builder) {
        builder.write(C60Constants.CHARACTERISTIC_WRITE, CMD_GET_CURRENT_HEARTRATE);
        return this;
    }

    public byte[] setUserInfoCommand(Prefs prefs) {
        ActivityUser activityUser = new ActivityUser();
        ByteBuffer buf = getByteBuffer(7);
        byte gender = (byte) (activityUser.getGender() == ActivityUser.GENDER_FEMALE ? 0x01 : 0x00);
        buf.put(gender);
        byte years = (byte) activityUser.getAge();
        buf.put(years);
        int heightCm = Math.max(40, Math.min(230, activityUser.getHeightCm())); // limits from vendor app
        buf.putShort((short) heightCm);
        int weightKg = Math.max(5, Math.min(300, activityUser.getWeightKg())) * 10; // limits from vendor app
        buf.putShort((short) weightKg);
        int stepCm = activityUser.getStepLengthCm();
        buf.put((byte) stepCm);
        return buildCommand(
                CMD_USER_INFO,
                buf.array()
        );
    }

    public byte[] setDeviceStateCommand(Prefs prefs) {
        ByteBuffer buf = getByteBuffer(this.currentDeviceSettings);

        // set language byte
        Integer langIdObj = C60Constants.LANGUAGES.getOrDefault(prefs.getString(DeviceSettingsPreferenceConst.PREF_LANGUAGE, "en_US"), 0);
        int langId = (langIdObj != null) ? langIdObj : 0;
        buf.put(3, (byte) langId);

        // TODO 4 - Units, 0 - metric, 1 - imperial

        // set timeformat byte
        byte timeformatByte;
        String timeFormat = prefs.getString(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO);
        if (timeFormat.equals(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H)) {
            timeformatByte = 0x01;
        } else if (timeFormat.equals(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H)) {
            timeformatByte = 0x00;
        } else {
            timeformatByte = (byte) (DateFormat.is24HourFormat(GBApplication.getContext()) ? 0x00 : 0x01);
        }
        buf.put(5, timeformatByte);

        // set liftwrist byte
        byte stateByte = (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED, true)) ? (byte) 0x01 : 0x00;
        buf.put(6, stateByte);

        // set notifications byte
        byte notificationsByte = (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false)) ? (byte) 0x01 : 0x00;
        buf.put(8, notificationsByte);

        // TODO 10 - Units, 0 - C, 1 - F
        // TODO 11 - Water unit, 0 - ml, 1 - oz uk, 2 - oz us

        this.currentDeviceSettings = buf.array();
        return buildCommand(
                CMD_DEVICE_STATE,
                buf.array()
        );
    }

    public byte[] setDoNotDisturbCommand(Prefs prefs) {
        ByteBuffer buf = getByteBuffer(this.currentDndSettings);

        byte enabled = (prefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_OFF).equals(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SCHEDULED))
                ? (byte) 0xff : (byte) 0x00;
        buf.put(0, enabled);
        LocalTime time;
        time = prefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START, "00:00");
        buf.put(1, (byte) time.getHour());
        buf.put(2, (byte) time.getMinute());
        time = prefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END, "00:00");
        buf.put(3, (byte) time.getHour());
        buf.put(4, (byte) time.getMinute());
        this.currentDndSettings = buf.array();
        return buildCommand(
                CMD_DO_NOT_DISTURB,
                buf.array()
        );
    }

    public byte[] setInactivityCommand(Prefs prefs) {
        ByteBuffer buf = getByteBuffer(5);

        // enable flag
        byte enable = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, false) ? (byte)0x01 : (byte)0x00;
        buf.put(enable);

        // start hour
        LocalTime time = prefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, "00:00");
        buf.put((byte) time.getHour());

        // end hour
        time = prefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, "00:00");
        buf.put((byte) time.getHour());

        // weekday mask (byte 6)
        int mask = 0;
        boolean anyDay = false;
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_MO, false)) { mask |= WeekdayMask.MON; anyDay = true; }
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_TU, false)) { mask |= WeekdayMask.TUE; anyDay = true; }
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_WE, false)) { mask |= WeekdayMask.WED; anyDay = true; }
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_TH, false)) { mask |= WeekdayMask.THU; anyDay = true; }
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_FR, false)) { mask |= WeekdayMask.FRI; anyDay = true; }
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_SA, false)) { mask |= WeekdayMask.SAT; anyDay = true; }
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_SU, false)) { mask |= WeekdayMask.SUN; anyDay = true; }

        // if no weekdays selected, set ONCE bit
        if (!anyDay) mask |= WeekdayMask.ONCE;

        buf.put((byte)(mask & 0xFF));

        // threshold (byte 7) stored as minutes/5
        int thresholdMinutes = prefs.getInt(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, 0);
        int thresholdUnits = Math.max(0, Math.min(255, thresholdMinutes / 5));
        buf.put((byte) thresholdUnits);

        return buildCommand(
                CMD_INACTIVITY,
                buf.array()
        );
    }

    public byte[] setGoalCommand(Prefs prefs) {
        ActivityUser activityUser = new ActivityUser();
        int steps = activityUser.getStepsGoal();
        int calories = activityUser.getCaloriesBurntGoal();
        int distance = activityUser.getDistanceGoalMeters() / 1000;  // ZeTime only accepts km goals

        ByteBuffer buf = getByteBuffer(14);

        buf.put((byte) 0x00); // sleep on/off
        buf.put((byte) 0x00); // sleep goal?

        // steps goal
        buf.put(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_USER_FITNESS_GOAL_NOTIFICATION, false) ? (byte) 0x01 : 0x00);
        buf.put((byte) (steps & 0xFF));
        buf.put((byte) ((steps >>> 8) & 0xFF));
        buf.put((byte) ((steps >>> 16) & 0xFF));

        // calories is always disabled ?
        buf.put((byte) 0x00);
        buf.put((byte) (calories & 0xFF));
        buf.put((byte) ((calories >>> 8) & 0xFF));
        buf.put((byte) ((calories >>> 16) & 0xFF));

        // distance is always disabled ?
        buf.put((byte) 0x00);
        buf.put((byte) (distance & 0xFF));
        buf.put((byte) ((distance >>> 8) & 0xFF));
        buf.put((byte) ((distance >>> 16) & 0xFF));

        return buildCommand(
                CMD_TARGET_DATA,
                buf.array()
        );
    }

    public byte[] setHydrationCommand(Prefs prefs) {
        ByteBuffer buf = getByteBuffer(23);
        buf.put(CMD_HYDRATION_ARG_SET_REMINDER);

        // enabled ?
        buf.put(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH, false) ? (byte) 0xff : 0x00);
        buf.put((byte) 0x01);
        buf.put((byte) 0x08);
        buf.put((byte) 0x07);
        buf.put((byte) 0x00);
        buf.put((byte) 0x00);
        // start end hour
        LocalTime start = prefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_START, "00:00");
        LocalTime end = prefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_END, "00:00");

        int slots = 8;
        int startMinutes = start.getHour() * 60 + start.getMinute();
        int endMinutes = end.getHour() * 60 + end.getMinute();

        // handle crossing midnight
        if (endMinutes <= startMinutes) endMinutes += 24 * 60;

        double interval = (double) (endMinutes - startMinutes) / (slots - 1);

        List<LocalTime> result = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            int mins = (int) Math.round(startMinutes + i * interval); // round to nearest minute
            mins = ((mins % (24 * 60)) + (24 * 60)) % (24 * 60); // normalize to 0..1439
            int h = mins / 60;
            int m = mins % 60;
            result.add(LocalTime.of(h, m));
        }

        int counter = 1;
        for (LocalTime t : result) {
            LOG.debug("{} cup: {}", counter, t);
            buf.put((byte) t.getHour());
            buf.put((byte) t.getMinute());
            counter++;
        }

        return buildCommand(
                CMD_HYDRATION,
                buf.array()
        );
    }

    public void getTargetData(TransactionBuilder builder) {
        writeInBuilder(
                builder,
                buildCommand(CMD_TARGET_DATA)
        );
    }

    public void getHydration(TransactionBuilder builder) {
        writeInBuilder(
                builder,
                buildCommand(CMD_HYDRATION, CMD_HYDRATION_ARG_GET_REMINDER)
        );
    }

    public C60DeviceSupport getNotice(TransactionBuilder builder) {
        builder.write(C60Constants.CHARACTERISTIC_WRITE, CMD_GET_NOTICE);
        return this;
    }

    public C60DeviceSupport getOxygen(TransactionBuilder builder) {
        builder.write(C60Constants.CHARACTERISTIC_WRITE, CMD_GET_OXYGEN);
        return this;
    }

    public C60DeviceSupport getHrSampling(TransactionBuilder builder) {
        builder.write(C60Constants.CHARACTERISTIC_WRITE, CMD_GET_HEARTRATE_SAMPLING);
        return this;
    }

    public C60DeviceSupport getAlarm(TransactionBuilder builder) {
        builder.write(C60Constants.CHARACTERISTIC_WRITE, CMD_GET_ALARM);
        return this;
    }

    private byte getChecksum(byte[] command) {
        byte sum = 0;
        for (byte b : command) {
            sum = (byte) (sum + b);
        }
        return (byte) ((sum * C60Constants.CHECKSUM_CODE) + 90);
    }

    private byte getResponseChecksum(byte[] data) {
        int len = data.length - 1;
        byte sum = 0;
        for (int i = 0; i < len; i++) {
            sum += data[i];
        }
        return (byte) (sum * C60Constants.CHECKSUM_CODE + 90);
    }

    private boolean responseChecksumValid(byte[] data) {
        return (getResponseChecksum(data) & (byte) 0xff) == (data[data.length - 1] & (byte) 0xff);
    }

    private byte[] trimData(byte[] data) {
        int newLen = data.length - 4;
        byte[] trimmed = new byte[newLen];
        System.arraycopy(data, 3, trimmed, 0, newLen);
        return trimmed;
    }

    private ByteBuffer getByteBuffer(int length) {
        return ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
    }

    private ByteBuffer getByteBuffer(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    // UNUSED might be used later
    // setting of sport modes
    // full hashmap in SPORT_MODES constant
//    Map<Integer, Boolean> statusMap = new HashMap<>();
//    statusMap.put(0, true);   // walk
//    statusMap.put(1, true);   // run
//    statusMap.put(8, true);   // badminton
//    statusMap.put(24, false); // indoor_walk
//    public static byte[] buildSportModePacket(Map<Integer, Boolean> statusMap) {
//        byte[] packet = new byte[20];
//        // Header / command fields (match original)
//        packet[0] = Opcodes.OPC_laload; // must match device expected start byte
//        packet[1] = 16;
//        packet[2] = 0;
//        packet[3] = 2;
//
//        // Helper to build a byte from mode indices range [base..base+7]
//        java.util.function.IntFunction<Byte> buildByte = base -> {
//            byte[] bits = new byte[8];
//            for (int i = 0; i < 8; i++) {
//                int modeIdx = base + i;
//                // mapping in original: mode base..base+7 -> bits[7..0]
//                int bitArrayIndex = 7 - i;
//                boolean val = false;
//                if (statusMap != null && statusMap.containsKey(modeIdx)) {
//                    Boolean b = statusMap.get(modeIdx);
//                    val = b != null && b;
//                }
//                bits[bitArrayIndex] = (byte) (val ? 1 : 0);
//            }
//            int intVal = ByteDataConvertUtil.Bit8Array2Int(bits);
//            return (byte) intVal;
//        };
//
//        // bArr[4] covers modes 0..7, bArr[5] 8..15, bArr[6] 16..23, bArr[7] 24..31 (we use up to 25)
//        packet[4] = buildByte.apply(0);
//        packet[5] = buildByte.apply(8);
//        packet[6] = buildByte.apply(16);
//        // For base 24 only modes 24..25 exist; remaining bits remain zero
//        packet[7] = buildByte.apply(24);
//
//        // bytes 8..18 are zero (explicitly set to 0 for clarity)
//        for (int i = 8; i <= 18; i++) packet[i] = 0;
//
//        // checksum (uses existing helper)
//        packet[19] = CmdHelper.completeCheckCode(packet);
//
//        return packet;
//    }
}
