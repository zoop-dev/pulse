package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
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
    private final Function<GBDeviceEvent, Void> sendEventHandler;
    private final Callable<DevicePrefs> getPrefsHandler;
    private final BluetoothGattCharacteristic rx;
    private final BluetoothGattCharacteristic tx;
    private final Runnable batteryRunner;
    private final Runnable heartBeatRunner;
    private final Set<G1Communications.CommandHandler> commandHandlers;
    private short heartBeatSequence;
    private short globalSequence;
    private GBDevice.State state;

    public G1SideManager(G1Constants.Side mySide, Handler backgroundTasksHandler,
                         Callable<BtLEQueue> getQueue, Function<GBDeviceEvent, Void> sendEvent,
                         Callable<DevicePrefs> getPrefs,
                         BluetoothGattCharacteristic rx, BluetoothGattCharacteristic tx) {
        this.mySide = mySide;
        this.backgroundTasksHandler = backgroundTasksHandler;
        this.getQueueHandler = getQueue;
        this.sendEventHandler = sendEvent;
        this.getPrefsHandler = getPrefs;
        this.rx = rx;
        this.tx = tx;
        this.batteryRunner = () -> {
            send(new G1Communications.CommandBatteryLevel(this::handleBatteryPayload));
            scheduleBatteryPolling();
        };

        this.heartBeatRunner = () -> {
            send(new G1Communications.CommandHeartBeat(heartBeatSequence++));
            scheduleHeatBeat();
        };
        this.commandHandlers = new HashSet<>();

        // Non Finals
        this.heartBeatSequence = 0;
        this.globalSequence = 0;
        this.state = GBDevice.State.CONNECTED;
    }

    private BtLEQueue getQueue() {
        try {
            return getQueueHandler.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void evaluateGBDeviceEvent(GBDeviceEvent event) {
        sendEventHandler.apply(event);
    }

    private DevicePrefs getDevicePrefs() {
        try {
            return getPrefsHandler.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GBDevice.State getState() {
        return state;
    }

    public void initialize() {
        // The glasses will auto disconnect after 30 seconds of no data on the wire.
        // Schedule a heartbeat task. If this is not enabled, the glasses will disconnect and be
        // useless to the user.
        scheduleHeatBeat();

        // Schedule the battery polling.
        scheduleBatteryPolling();

        state = GBDevice.State.INITIALIZED;
    }

    public void postInitialize() {
        send(new G1Communications.CommandBatteryLevel(this::handleBatteryPayload));
        send(new G1Communications.CommandFirmwareInfo(this::handleFirmwareInfoPayload));
    }

    public void onSendConfiguration(String config) {
        switch (config) {
            // Reschedule battery polling. The new schedule may be disabled.
            case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE:
            case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_INTERVAL:
                scheduleBatteryPolling();
                break;
        }
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

    private synchronized short getNextSequence() {
        // This number will eventually overflow, and that is fine. The sequence number is just to
        // match the request and response together.
        return globalSequence++;
    }

    public void send(G1Communications.CommandHandler command) {
        TransactionBuilder builder =
                new TransactionBuilder(command.getName() + "_" + mySide.getDeviceIndex());
        sendInTransaction(builder, command);
        builder.queue(getQueue());
    }

    private void sendInTransaction(TransactionBuilder builder, G1Communications.CommandHandler command) {
        // Calling getNextSequence() will advance the global sequence, if the command doesn't need
        // a sequence number, don't call it so we don't waste a sequence number.
        if (command.needsGlobalSequence()) {
            command.setGlobalSequence(getNextSequence());
        }

        LOG.debug("Send command {} on side {}", command.getName(), mySide.getDeviceIndex());

        // Write the packet to the BLE txn.
        builder.write(tx, command.serialize());

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

    public boolean handlePayload(byte[] payload) {
        for (G1Communications.CommandHandler commandHandler : commandHandlers) {
            LOG.debug("Got response payload for command {} on side {}: {}", commandHandler.getName(), mySide.getDeviceIndex(), Logging.formatBytes(payload));
            if (commandHandler.responseMatches(payload)) {
                synchronized (commandHandlers) {
                    commandHandlers.remove(commandHandler);
                    commandHandler.setResponsePayload(payload);
                }

                Function<byte[], Boolean> callback = commandHandler.getCallback();
                return callback != null && callback.apply(payload);
            }
        }
        LOG.debug("Unhandled payload on side {}: {}", mySide.getDeviceIndex(), Logging.formatBytes(payload));

        // Not handled by any handlers.
        return false;
    }

    private boolean handleBatteryPayload(byte[] payload) {
        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
        batteryInfo.state = BatteryState.BATTERY_NORMAL;
        batteryInfo.level = payload[2];
        batteryInfo.batteryIndex = mySide.getDeviceIndex();
        evaluateGBDeviceEvent(batteryInfo);
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
            LOG.debug("Parsed fw version: {}", version);
            GBDeviceEventVersionInfo fwInfo = new GBDeviceEventVersionInfo();
            if (mySide == G1Constants.Side.LEFT) {
                fwInfo.fwVersion = version;
            } else if (mySide == G1Constants.Side.RIGHT) {
                fwInfo.fwVersion2 = version;
            }
            // Actually get this some how?
            fwInfo.hwVersion = "G1A";
            evaluateGBDeviceEvent(fwInfo);
            return true;
        }
        return false;
    }
}
