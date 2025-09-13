package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.TimeZone;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryIncrementalInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

/**
 * This is a supporting class for G1DeviceSupport and allows each side of the glasses to be have
 * some level of independence but also allow the G1DeviceSupport class to control them and offload
 * functionality. It might be tempting to pass the entire G1DeviceSupport into the constructor
 * instead of the callbacks, but this is not done deliberately to make this class not directly tied
 * to the G1DeviceSupport class.
 */
public class G1SideManager {
    private static final Logger LOG = LoggerFactory.getLogger(G1SideManager.class);

    private final G1Constants.Side mySide;
    private final Handler backgroundTasksHandler;
    private final Callable<BtLEQueue> getQueueHandler;
    private final Callable<GBDevice> getDeviceHandler;
    private final Consumer<GBDeviceEvent> sendEventHandler;
    private final Callable<DevicePrefs> getPrefsHandler;
    private final BiFunction<String, Integer, TransactionBuilder> createTransactionBuilder;
    private final BluetoothGattCharacteristic rx;
    private final BluetoothGattCharacteristic tx;
    private final Runnable batteryRunner;
    private final Runnable heartBeatRunner;
    private final Runnable displaySettingsPreviewCloserRunner;
    private final Set<G1Communications.CommandHandler> commandHandlers;
    private byte globalSequence;
    private boolean isSilentModeEnabled;
    private GBDevice.State connectingState;
    private boolean debugEnabled;
    private long lastHeartBeatTime;

    public G1SideManager(G1Constants.Side mySide, Handler backgroundTasksHandler,
                         Callable<BtLEQueue> getQueue, Callable<GBDevice> getDevice,
                         Consumer<GBDeviceEvent> sendEvent, Callable<DevicePrefs> getPrefs,
                         BluetoothGattCharacteristic rx, BluetoothGattCharacteristic tx,
                         BiFunction<String, Integer, TransactionBuilder> createTransactionBuilder) {
        this.mySide = mySide;
        this.backgroundTasksHandler = backgroundTasksHandler;
        this.getQueueHandler = getQueue;
        this.getDeviceHandler = getDevice;
        this.sendEventHandler = sendEvent;
        this.getPrefsHandler = getPrefs;
        this.createTransactionBuilder = createTransactionBuilder;
        this.rx = rx;
        this.tx = tx;
        this.batteryRunner = () -> {
            send(new G1Communications.CommandGetBatteryInfo(this::handleBatteryPayload));
            scheduleBatteryPolling();
        };

        this.heartBeatRunner = () -> {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            long currentMilliseconds = c.getTimeInMillis();
            LOG.info("Side {}: {}ms since the last heartbeat", mySide, currentMilliseconds - lastHeartBeatTime);
            lastHeartBeatTime = currentMilliseconds;
            if (getDevice().isConnected()) {
                // We can send any command as a heart beat. The official app uses this one.
                send(new G1Communications.CommandSendHeartBeat(getNextSequence()));
                scheduleHeatBeat();
            } else {
                // Don't reschedule if the device is disconnected.
                LOG.debug("Stopping heartbeat runner since side is in state: {}", getDevice().getState());
            }
        };
        this.displaySettingsPreviewCloserRunner = () -> {
            DevicePrefs prefs = getDevicePrefs();
            send(new G1Communications.CommandSetDisplaySettings(
                    false /* preview */,
                    (byte)prefs.getInt(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_HEIGHT, 0),
                    // Depth ranges from 1-9 instead of 0-8, so offset by one to convert from
                    // the slider space.
                    (byte)(prefs.getInt(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_DEPTH, 0) + 1)));
        };
        this.commandHandlers = new HashSet<>();

        // Non Finals
        this.globalSequence = 0;
        this.isSilentModeEnabled = false;
        this.connectingState = GBDevice.State.CONNECTED;
        this.debugEnabled = false;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.lastHeartBeatTime = c.getTimeInMillis();

    }

    private BtLEQueue getQueue() {
        try {
            return getQueueHandler.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GBDevice getDevice() {
        try {
            return getDeviceHandler.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void evaluateGBDeviceEvent(GBDeviceEvent event) {
        sendEventHandler.accept(event);
    }

    private DevicePrefs getDevicePrefs() {
        try {
            return getPrefsHandler.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GBDevice.State getConnectingState() {
        return connectingState;
    }

    public void initialize(TransactionBuilder transaction) {
        // Disable device logging in the prefs. There is no way to query this state from the device
        // so instead, it is always disabled on connection, and then if a debug message arrives, the
        // setting will be flipped to true.
        this.debugEnabled = false;
        getDevicePrefs().getPreferences().edit()
            .putBoolean(DeviceSettingsPreferenceConst.PREF_DEVICE_LOGS_TOGGLE, this.debugEnabled)
            .apply();

        // The glasses will auto disconnect after 30 seconds of no data on the wire.
        // Schedule a heartbeat task. If this is not enabled, the glasses will disconnect and be
        // useless to the user.
        scheduleHeatBeat();

        // Schedule the battery polling.
        scheduleBatteryPolling();

        connectingState = GBDevice.State.INITIALIZED;
    }

    public byte getSilentModeStatus() {
        return isSilentModeEnabled ? G1Constants.SilentStatus.ENABLE : G1Constants.SilentStatus.DISABLE;
    }

    private void postInitializeCommon(TransactionBuilder transaction) {
        sendInTransaction(transaction, new G1Communications.CommandGetBatteryInfo(this::handleBatteryPayload));
        sendInTransaction(transaction, new G1Communications.CommandGetFirmwareInfo(this::handleFirmwareInfoPayload));
        sendInTransaction(transaction, new G1Communications.CommandGetSilentModeSettings(this::handleSilentStatusPayload));
    }

    public void postInitializeLeft() {
        TransactionBuilder transaction =
                createTransactionBuilder.apply("post_initialize_left", mySide.getDeviceIndex());
        postInitializeCommon(transaction);

        // These can be sent to both, but the left lens is used as the master for these settings.
        sendInTransaction(transaction, new G1Communications.CommandGetBrightnessSettings(this::handleBrightnessSettingsPayload));
        sendInTransaction(transaction, new G1Communications.CommandGetSerialNumber(this::handleSerialNumberPayload));
        transaction.queue();

        // Sent to the left only and it's own transaction, this is a large piece of data and can
        // cause GB to time out the initialization and get stuck in a loop.
        send(new G1Communications.CommandSetAppNotificationSettings(
                this::send, List.of(G1Constants.FIXED_NOTIFICATION_APP_ID),
                false, false, false));
    }

    public void postInitializeRight() {
        TransactionBuilder transaction =
                createTransactionBuilder.apply( "post_initialize_right", mySide.getDeviceIndex());
        postInitializeCommon(transaction);

        // This settings are only sent to the right lens in the official app, so we copy that.
        sendInTransaction(transaction, new G1Communications.CommandGetHeadGestureSettings(this::handleHeadGestureSettingsPayload));
        // This setting uses the right lens as the master for the setting simply to balance the amount
        // of commands being sent to the left vs right.
        sendInTransaction(transaction, new G1Communications.CommandGetDisplaySettings(this::handleDisplaySettingsPayload));
        sendInTransaction(transaction, new G1Communications.CommandGetWearDetectionSettings(this::handleWearDetectionSettingsPayload));
        sendInTransaction(transaction, new G1Communications.CommandGetNotificationDisplaySettings(this::handleNotificationDisplaySettingsPayload));
        transaction.queue();
    }

    public void onSendConfiguration(String config) {
        DevicePrefs prefs = getDevicePrefs();
        switch (config) {
            // Reschedule battery polling. The new schedule may be disabled.
            case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE:
            case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_INTERVAL:
                scheduleBatteryPolling();
                break;
            case DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_HEIGHT:
            case DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_DEPTH:
                sendDisplaySettings(prefs);
                break;
            case DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_ACTIVATION_ANGLE:
                send(new G1Communications.CommandSetHeadGestureSettings(
                        (byte)prefs.getInt(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_ACTIVATION_ANGLE, 40)));
                break;
            case DeviceSettingsPreferenceConst.PREF_SCREEN_AUTO_BRIGHTNESS:
            case DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS:
                send(new G1Communications.CommandSetBrightnessSettings(
                        prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SCREEN_AUTO_BRIGHTNESS, true),
                        (byte)prefs.getInt(DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS, 0x2A)));// TODO: Add a constant for the max value?
                break;
            case DeviceSettingsPreferenceConst.PREF_WEAR_SENSOR_TOGGLE:
                send(new G1Communications.CommandSetWearDetectionSettings(
                        prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_WEAR_SENSOR_TOGGLE, true)));
                break;
            case DeviceSettingsPreferenceConst.PREF_DEVICE_LOGS_TOGGLE:
                this.debugEnabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_DEVICE_LOGS_TOGGLE, false);
                send(new G1Communications.CommandSetDebugLogSettings(this.debugEnabled));
                break;
            case DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS:
            case DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS_TIMEOUT:
                send(new G1Communications.CommandSetNotificationDisplaySettings(
                        prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS, true),
                        (byte)prefs.getInt(DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS_TIMEOUT, 5)));
                break;
        }
    }

    public void onToggleSilentMode() {
        isSilentModeEnabled = !isSilentModeEnabled;
        send(new G1Communications.CommandSetSilentModeSettings(isSilentModeEnabled));
    }

    private void scheduleHeatBeat() {
        backgroundTasksHandler.removeCallbacksAndMessages(heartBeatRunner);
        LOG.debug("Starting heartbeat runner delayed by {}ms", G1Constants.HEART_BEAT_DELAY_MS);
        backgroundTasksHandler.postDelayed(heartBeatRunner, G1Constants.HEART_BEAT_DELAY_MS);
    }

    private void scheduleBatteryPolling() {
        backgroundTasksHandler.removeCallbacksAndMessages(batteryRunner);
            DevicePrefs prefs = getDevicePrefs();
            if (prefs.getBatteryPollingEnabled()) {
                int interval_minutes = prefs.getBatteryPollingIntervalMinutes();
                int interval = interval_minutes * 60 * 1000;
                LOG.debug("Starting battery runner delayed by {} ({} minutes)", interval,
                          interval_minutes);
                backgroundTasksHandler.postDelayed(batteryRunner, interval);
        }
    }

    private synchronized byte getNextSequence() {
        // Synchronized so the sequence increments atomically.
        // This number will eventually overflow, and that is fine. The sequence number is just to
        // match the request and response together.
        return globalSequence++;
    }

    private synchronized void sendDisplaySettings(DevicePrefs prefs) {
        // Synchronized so that there can only ever be one background task.
        // Clear any existing runner in case the user has changed the value multiple times
        // before th delay expired.
        backgroundTasksHandler.removeCallbacksAndMessages(displaySettingsPreviewCloserRunner);

        // The glasses expect the setting to
        send(new G1Communications.CommandSetDisplaySettings(
                true /* preview */,
                (byte)prefs.getInt(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_HEIGHT, 0),
                // Depth ranges from 1-9 instead of 0-8, so offset by one to convert from
                // the slider space.
                (byte)(prefs.getInt(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_DEPTH, 0) + 1)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // On newer APIs, use the runner as the token.
            backgroundTasksHandler.postDelayed(displaySettingsPreviewCloserRunner,
                                               displaySettingsPreviewCloserRunner,
                                               G1Constants.DISPLAY_SETTINGS_PREVIEW_DELAY);
        } else {
            backgroundTasksHandler.postDelayed(displaySettingsPreviewCloserRunner,
                                               G1Constants.DISPLAY_SETTINGS_PREVIEW_DELAY);
        }
    }

    public void send(G1Communications.CommandHandler command) {
        TransactionBuilder transaction =
                createTransactionBuilder.apply(command.getName(), mySide.getDeviceIndex());
        sendInTransaction(transaction, command);
        transaction.queue();
    }

    private void sendInTransaction(TransactionBuilder transaction, G1Communications.CommandHandler command) {
        // Calling getNextSequence() will advance the global sequence, if the command doesn't need
        // a sequence number, don't call it so we don't waste a sequence number.
        if (command.needsGlobalSequence()) {
            command.setGlobalSequence(getNextSequence());
        }

        LOG.debug("Send command {} on side {}", command.getName(), mySide.getDeviceIndex());

        // Write the packet to the BLE txn.
        transaction.write(tx, command.serialize());

        // If this command expects a response, register the handler.
        if (command.expectResponse()) {
            registerResponseHandler(command);
        }

        // Schedule a task that will sleep for the timeout time, and then wake up and check if the
        // command is completed. If the command has not completed, a retry is sent if there are
        // available retries.
        if (command.expectResponse()) {
            backgroundTasksHandler.postDelayed(() -> {
                boolean retry;
                synchronized (command) {
                    command.notifyAttempt();
                    retry = !command.hasResponsePayload() && command.hasRetryRemaining();
                }

                // Do this outside the synchronized block, better to avoid comm work while holding
                // the lock.
                if (retry) {
                    LOG.debug("Retry {} command {} on side {}", command.getRetryCount(),
                             command.getName(), mySide.getDeviceIndex());
                    // TODO: This will change the global sequence number of the command, is this
                    //  what the stock app does on retry? Or does it resend with the same one.
                    send(command);
                }
            }, command.getTimeout());
        }
    }

    private void registerResponseHandler(G1Communications.CommandHandler commandHandler) {
        synchronized (commandHandlers) {
            commandHandlers.add(commandHandler);
        }
    }

    private void updateBatteryLevel(int level, int index) {
        evaluateGBDeviceEvent(new GBDeviceEventBatteryIncrementalInfo(index, level));
    }

    private void updateBatteryLevel(int level) {
        updateBatteryLevel(level, mySide.getDeviceIndex());
    }

    private void updateBatteryState(BatteryState state, int index) {
        evaluateGBDeviceEvent(new GBDeviceEventBatteryIncrementalInfo(index, state));
    }

    private void updateBatteryState(BatteryState state) {
        updateBatteryState(state, mySide.getDeviceIndex());
    }

    public boolean handlePayload(byte[] payload) {
        for (G1Communications.CommandHandler commandHandler : commandHandlers) {
            if (commandHandler.responseMatches(payload)) {
                LOG.debug("Got response payload for command {} on side {}: {}",
                          commandHandler.getName(), mySide.getDeviceIndex(),
                          Logging.formatBytes(payload));
                synchronized (commandHandlers) {
                    commandHandlers.remove(commandHandler);
                    commandHandler.setResponsePayload(payload);
                }

                Function<byte[], Boolean> callback = commandHandler.getCallback();
                return callback != null && callback.apply(payload);
            }
        }

        // The glasses will send unprompted messages indicating certain events happening.
        // ex. glasses are taken off, glasses are charging, or touch pad was pressed.
        if (G1Communications.DeviceEvent.messageMatches(payload)) {
            return handleDeviceEventPayload(payload);
        }

        if (G1Communications.DebugLog.messageMatches(payload)) {
            return handleDebugLogPayload(payload);
        }

        LOG.debug("Unhandled payload on side {}: {}",
                  mySide.getDeviceIndex(), Logging.formatBytes(payload));

        // Not handled by any handlers.
        return false;
    }

    private boolean handleBatteryPayload(byte[] payload) {
        updateBatteryLevel(G1Communications.CommandGetBatteryInfo.getBatteryPercent(payload));
        return true;
    }

    private boolean handleFirmwareInfoPayload(byte[] payload) {
        // FW info string
        String fwString = new String(payload, StandardCharsets.US_ASCII).trim();
        LOG.debug("Got FW: {}", fwString);
        int versionStart = fwString.lastIndexOf(" ver ") + " ver ".length();
        int versionEnd = fwString.indexOf(',', versionStart);
        if (versionStart > -1 && versionEnd > versionStart) {
            String version = fwString.substring(versionStart, versionEnd);
            GBDeviceEventVersionInfo fwInfo = new GBDeviceEventVersionInfo();
            fwInfo.hwVersion = null;
            fwInfo.fwVersion = mySide == G1Constants.Side.LEFT ? version : null;
            fwInfo.fwVersion2 = mySide == G1Constants.Side.RIGHT ? version : null;
            evaluateGBDeviceEvent(fwInfo);
            return true;
        }
        return false;
    }

    private boolean handleSerialNumberPayload(byte[] payload) {
        String serialNumber = G1Communications.CommandGetSerialNumber.getSerialNumber(payload);

        // Parse the hardware information out of the serial number.
        int shape = G1Communications.CommandGetSerialNumber.getFrameType(payload);
        int color = G1Communications.CommandGetSerialNumber.getFrameColor(payload);
        if (shape != -1 && color != -1) {
            GBDeviceEventVersionInfo fwInfo = new GBDeviceEventVersionInfo();
            fwInfo.hwVersion = GBApplication.getContext().getString(
                    R.string.even_realities_frame_description,
                    GBApplication.getContext().getString(color),
                    GBApplication.getContext().getString(shape),
                    GBApplication.getContext().getString(R.string.serial_number),
                    serialNumber);
            fwInfo.fwVersion = null;
            fwInfo.fwVersion2 = null;
            evaluateGBDeviceEvent(fwInfo);
            return true;
        }
        return false;
    }

    private boolean handleSilentStatusPayload(byte[] payload) {
        isSilentModeEnabled = G1Communications.CommandGetSilentModeSettings.isEnabled(payload);
        return true;
    }
    private boolean handleDisplaySettingsPayload(byte[] payload) {
        GBDeviceEventUpdatePreferences prefsEvent = new GBDeviceEventUpdatePreferences();
        prefsEvent.preferences.put(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_HEIGHT,
                                   G1Communications.CommandGetDisplaySettings.getHeight(payload));
        // Depth is indexed is 1-9, so subtract 1 to map it to the 0-8 of the slider.
        prefsEvent.preferences.put(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_DEPTH,
                                   G1Communications.CommandGetDisplaySettings.getDepth(payload) - 1);
        evaluateGBDeviceEvent(prefsEvent);
        return true;
    }

    private boolean handleHeadGestureSettingsPayload(byte[] payload) {
        GBDeviceEventUpdatePreferences prefsEvent = new GBDeviceEventUpdatePreferences();
        prefsEvent.preferences.put(
                DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_ACTIVATION_ANGLE,
                G1Communications.CommandGetHeadGestureSettings.getActivationAngle(payload));
        evaluateGBDeviceEvent(prefsEvent);
        return true;
    }

    private boolean handleBrightnessSettingsPayload(byte[] payload) {
        GBDeviceEventUpdatePreferences prefsEvent = new GBDeviceEventUpdatePreferences();
        prefsEvent.preferences.put(DeviceSettingsPreferenceConst.PREF_SCREEN_AUTO_BRIGHTNESS,
                                   G1Communications.CommandGetBrightnessSettings.isAutoBrightnessEnabled(payload));
        prefsEvent.preferences.put(DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS,
                                   G1Communications.CommandGetBrightnessSettings.getBrightnessLevel(payload));
        evaluateGBDeviceEvent(prefsEvent);
        return true;
    }

    private boolean handleWearDetectionSettingsPayload(byte[] payload) {
        GBDeviceEventUpdatePreferences prefsEvent = new GBDeviceEventUpdatePreferences();
        prefsEvent.preferences.put(DeviceSettingsPreferenceConst.PREF_WEAR_SENSOR_TOGGLE,
                                   G1Communications.CommandGetWearDetectionSettings.isEnabled(payload));
        evaluateGBDeviceEvent(prefsEvent);
        return true;
    }

    private boolean handleNotificationDisplaySettingsPayload(byte[] payload) {
        GBDeviceEventUpdatePreferences prefsEvent = new GBDeviceEventUpdatePreferences();
        prefsEvent.preferences.put(DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS,
                                   G1Communications.CommandGetNotificationDisplaySettings.isEnabled(payload));
        prefsEvent.preferences.put(
                DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS_TIMEOUT,
                Integer.toString(G1Communications.CommandGetNotificationDisplaySettings.getTimeout(payload)));
        evaluateGBDeviceEvent(prefsEvent);
        return true;
    }
    private boolean handleDeviceEventPayload(byte[] payload) {
        switch (G1Communications.DeviceEvent.getEventId(payload)) {
            case G1Constants.DeviceEventId.GLASSES_CHARGING:
                updateBatteryState(
                        G1Communications.DeviceEvent.getValue(payload) == 0x01
                            ? BatteryState.BATTERY_CHARGING
                            : BatteryState.BATTERY_NORMAL);
                break;
            case G1Constants.DeviceEventId.GLASSES_SIDE_BATTERY_LEVEL:
                updateBatteryLevel(G1Communications.DeviceEvent.getValue(payload));
                break;
            case G1Constants.DeviceEventId.CASE_CHARGING:
                updateBatteryState(
                        G1Communications.DeviceEvent.getValue(payload) == 0x01
                            ? BatteryState.BATTERY_CHARGING
                            : BatteryState.BATTERY_NORMAL,
                        G1Constants.CASE_BATTERY_INDEX);
                break;
            case G1Constants.DeviceEventId.CASE_BATTERY_LEVEL:
                updateBatteryLevel(G1Communications.DeviceEvent.getValue(payload),
                                   G1Constants.CASE_BATTERY_INDEX);
                break;
            case G1Constants.DeviceEventId.GLASSES_NOT_WORN_NO_CASE:
                updateBatteryState(BatteryState.NO_BATTERY, G1Constants.CASE_BATTERY_INDEX);
                break;
            default:
                LOG.debug("Device Event on side {}: {}", mySide.getDeviceIndex(),
                          Logging.formatBytes(payload));
                return false;
        }
        return true;
    }

    private boolean handleDebugLogPayload(byte[] payload) {
        // Use the local boolean so that we aren't constantly committing the same value to the prefs
        if (!this.debugEnabled) {
            this.debugEnabled = true;
            // Mark the pref as enabled so that the Setting UI reflects the true state.
            getDevicePrefs().getPreferences().edit().putBoolean(
                    DeviceSettingsPreferenceConst.PREF_DEVICE_LOGS_TOGGLE, this.debugEnabled).apply();
        }
        LOG.info("{}: {}", mySide, G1Communications.DebugLog.getMessage(payload));
        return true;
    }
}
