/*  Copyright (C) 2025 jrthomas270, José Rebelo, Thomas Kuehne, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEMultiDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

/**
 * Support class for the Even Realities G1. This sends and receives commands to and from the device.
 * The Protocol is defined in G1SideManager.
 * One interesting point about this device is that it requires a constant BLE connection which is
 * contrary to the way BLE is supposed to work. Unfortunately the device will show the disconnected
 * icon and stop displaying any information when it is in the disconnected state. Because of this,
 * we need to send a heartbeat ever 30 seconds, otherwise the device will disconnect and reconnect
 * every 32 seconds per the BLE spec.
 */
public class G1DeviceSupport extends AbstractBTLEMultiDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(G1DeviceSupport.class);
    private final HandlerThread backgroundThread = new HandlerThread("even_g1_background_thread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
    private final Runnable heartBeatRunner;
    private final Runnable displaySettingsPreviewCloserRunner;
    private final Runnable calendarSyncRunner;
    private Handler backgroundTasksHandler = null;
    private BroadcastReceiver intentReceiver = null;
    private final Object lensSkewLock = new Object();
    private final Object initializationLock = new Object();
    private final Object calendarLock = new Object();
    private G1SideManager leftSide = null;
    private G1SideManager rightSide = null;
    private long lastHeartBeatTime;
    private long lastHeartBeatDelayTarget;
    private long heartBeatTargetModifier;
    private byte globalSequence;

    private List<CalendarEvent> lastSyncedEvents;

    public G1DeviceSupport() {
        this(LOG);
    }

    public G1DeviceSupport(Logger logger) {
        super(logger, 2);
        addSupportedService(G1Constants.UUID_SERVICE_NORDIC_UART,
                            G1Constants.Side.LEFT.getDeviceIndex());

        addSupportedService(G1Constants.UUID_SERVICE_NORDIC_UART,
                            G1Constants.Side.RIGHT.getDeviceIndex());

        this.heartBeatRunner = () -> {
            if (isConnected()) {
                // We can send any command as a heart beat. The official app uses this one.
                G1Communications.CommandSilentModeGet leftCommand =
                        new G1Communications.CommandSilentModeGet(b -> { return true;});
                G1Communications.CommandSilentModeGet rightCommand =
                        new G1Communications.CommandSilentModeGet(b -> { return true;});
                leftSide.send(leftCommand);
                rightSide.send(rightCommand);

                // Wait for both sides to respond. Resend if there is no response.
                while(!leftCommand.waitForResponsePayload() || !rightCommand.waitForResponsePayload()) {
                    if (!leftCommand.waitForResponsePayload()) {
                        leftSide.send(leftCommand);
                    }

                    if (!rightCommand.waitForResponsePayload()) {
                        rightSide.send(rightCommand);
                    }
                }

                scheduleHeatBeat();
            } else {
                // Don't reschedule if the device is disconnected.
                LOG.debug("Stopping heartbeat runner since side is in state: {} {}",
                          getDevice(G1Constants.Side.LEFT.getDeviceIndex()).getState(),
                          getDevice(G1Constants.Side.RIGHT.getDeviceIndex()).getState());
            }
        };

        this.displaySettingsPreviewCloserRunner = () -> {
            DevicePrefs prefs = getDevicePrefs();
            G1Communications.CommandHardwareDisplaySet command =
                    new G1Communications.CommandHardwareDisplaySet(getNextSequence(),
                                                                   false /* preview */,
                                                                   // Height ranges from 0-8 instead of 1-9, so offset by one to convert from
                                                                   // the slider space.
                                                                   (byte) (prefs.getInt(
                                DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_HEIGHT, 1) - 1),
                                                                   (byte) prefs.getInt(
                                DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_DEPTH, 1));
            leftSide.send(command);
            rightSide.send(command);
        };

        this.calendarSyncRunner = this::syncCalendar;

        // Non Finals
        this.lastHeartBeatTime = 0;
        this.lastHeartBeatDelayTarget = G1Constants.HEART_BEAT_TARGET_DELAY_MS;
        this.globalSequence = 0;
        this.lastSyncedEvents = null;
    }

    @Override
    public void setContext(GBDevice device, BluetoothAdapter btAdapter, Context context) {
        // Determine the left and right names based on if this is the parent device or not.
        // Ignore any context sets from non-left devices.
        G1Constants.Side side = G1Constants.getSideFromFullName(device.getName());
        if (side == G1Constants.Side.LEFT) {
            ItemWithDetails right_name = device.getDeviceInfo(G1Constants.Side.RIGHT.getNameKey());
            ItemWithDetails right_address =
                    device.getDeviceInfo(G1Constants.Side.RIGHT.getAddressKey());
            if (right_name != null && !right_name.getDetails().isEmpty() && right_address != null &&
                !right_address.getDetails().isEmpty()) {
                GBDevice rightDevice =
                        new GBDevice(right_address.getDetails(), right_name.getDetails(), null,
                                     device.getParentFolder(), device.getType());
                super.setDevice(rightDevice, 1);
            } else {
                super.setDevice(null, 1);
            }

            // The left device acts as the parent device
            super.setContext(device, btAdapter, context);
        } else {
            // This should only happen during pairing. Once the devices are linked by the
            // entries for right and left devices in the device specific preferences, this will
            // never be called on the right device again. BUT we need this to connect to the right
            // device before the devices are linked.
            super.setContext(device, btAdapter, context);
        }

        if (backgroundTasksHandler == null) {
            backgroundThread.start();
            backgroundTasksHandler = new Handler(backgroundThread.getLooper());
        }

        // Register to receive silent mode intent calls from the UI.
        if (intentReceiver == null) {
            intentReceiver = new IntentReceiver();
            ContextCompat.registerReceiver(context, intentReceiver,
                                           new IntentFilter(G1Constants.INTENT_TOGGLE_SILENT_MODE),
                                           ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder, int deviceIdx) {
        // Verify that the characteristics are present for the current device.
        BluetoothGattCharacteristic rx =
                getCharacteristic(G1Constants.UUID_CHARACTERISTIC_NORDIC_UART_RX, deviceIdx);
        BluetoothGattCharacteristic tx =
                getCharacteristic(G1Constants.UUID_CHARACTERISTIC_NORDIC_UART_TX, deviceIdx);

        if (rx == null || tx == null) {
            // If the characteristics are not received from the device reconnect and try again.
            LOG.warn("RX/TX characteristics are null, will attempt to reconnect");
            builder.setDeviceState(GBDevice.State.WAITING_FOR_RECONNECT);
            GB.toast(getContext(), "Failed to connect to Glasses, waiting for reconnect.",
                     Toast.LENGTH_LONG, GB.ERROR);
            return builder;
        }

        // Create either the left or right side depending on which device is initialized.
        G1SideManager side;
        synchronized (this) {
            side = getSideFromIndex(deviceIdx);
            if (side == null) {
                side = createSideFromIndex(deviceIdx, rx, tx);
            }
        }

        // Paranoid protection from a bad index being passed in.
        if (side == null) {
            LOG.error("Device index is not left or right: {}", deviceIdx);
            builder.setDeviceState(GBDevice.State.WAITING_FOR_RECONNECT);
            GB.toast(getContext(), "Unable to manage connection to device.", Toast.LENGTH_LONG,
                     GB.ERROR);
            return builder;
        }

        // The glasses expect a specific MTU, set that now.
        builder.requestMtu(G1Constants.MTU);

        // Register callbacks for this device.
        builder.setCallback(this);
        builder.notify(rx, true);

        // If the side is in the connected state, it is ready to be initialized.
        if (side.getConnectingState() == GBDevice.State.NOT_CONNECTED) {
            // Since the left side is device 0, ony it can mark the global device as initializing.
            // If the right side were to do so, the Device support will skip ever initializing
            // device 0. See: super::onServicesDiscovered()
            if (side == leftSide) {
                builder.setDeviceState(GBDevice.State.INITIALIZING);
            }
            side.initialize(builder);
        }

        // The final step of each transaction will be to decide if that particular side is the
        // second side to complete, and if it, that side will be the one responsible for marking
        // the composite device as initialized.
        builder.run(() -> {
            // There is a race condition of each device marking INITIALIZED. If one device
            // initialize transaction runs after the other has completely finished, the device
            // will transition from INITIALIZED back to INITIALIZING. Run this final step in a
            // synchronized block so that only one of the devices can be the final one. Since
            // this action always runs after "side.initialize(builder)" in the transaction order
            // it is not possible for both devices to pass this point before both are marked
            // initialized.
            // NOTE: side.getConnectingState() != getDevice().getState().
            synchronized (initializationLock) {
                // This means that both sides have been connected to and basic info has been collected.
                if (leftSide != null &&
                    leftSide.getConnectingState() == GBDevice.State.INITIALIZED &&
                    rightSide != null &&
                    rightSide.getConnectingState() == GBDevice.State.INITIALIZED) {
                    // Set device firmware to prevent the following error when data is saved to
                    // the database and device firmware has not been set yet.
                    // java.lang.IllegalArgumentException: the bind value at index 2 is null.
                    // Must be called before the PostInitialize down below.
                    getDevice().setFirmwareVersion("N/A");
                    getDevice().setFirmwareVersion2("N/A");

                    // These next steps require that both sides are ready and they can run very
                    // slowly which is why they are done post individual initialization and in
                    // the background. We don't know what thread we are handling the update
                    // state event on, so to be safe, schedule these as a background task.
                    backgroundTasksHandler.postDelayed(() -> {
                        onSetDashboardMode();
                        onLanguageChange();
                        onSetTime();
                        // The glasses will auto disconnect after 30 seconds of no data on the wire.
                        // Schedule a heartbeat task. If this is not enabled, the glasses will disconnect
                        // and be useless to the user.
                        scheduleHeatBeat();

                        // Sent to the left only and it's own transaction, this is a large piece
                        // of data and can cause GB to time out the initialization and get stuck
                        // in a loop.
                        leftSide.send(new G1Communications.CommandNotificationAppListSet(
                                leftSide::send, List.of(G1Constants.FIXED_NOTIFICATION_APP_ID),
                                false, false, false));

                        // Tell the calendar events to synchronize.
                        forceNextCalendarSync();
                        syncCalendar();
                    }, 200);

                    // Mark both sub devices as INITIALIZED so that the composite device is
                    // considered INITIALIZED.
                    getDevice(G1Constants.Side.LEFT.getDeviceIndex())
                            .setUpdateState(GBDevice.State.INITIALIZED, getContext());
                    getDevice(G1Constants.Side.RIGHT.getDeviceIndex())
                            .setUpdateState(GBDevice.State.INITIALIZED, getContext());
                }
            }
        });

        getDevice().sendDeviceUpdateIntent(getContext());
        return builder;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        forceNextCalendarSync();

        if (backgroundTasksHandler != null) {
            // Remove all background tasks.
            backgroundTasksHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void dispose() {
        forceNextCalendarSync();

        synchronized (ConnectionMonitor) {
            if (backgroundTasksHandler != null) {
                // Remove all background tasks.
                backgroundTasksHandler.removeCallbacksAndMessages(null);

                // Shutdown the background handler.
                backgroundThread.quitSafely();
                backgroundTasksHandler = null;
            }

            // Kill both sides.
            leftSide = null;
            rightSide = null;

            // Stop listening for intent actions
            if (intentReceiver != null) {
                getContext().unregisterReceiver(intentReceiver);
            }

            super.dispose();
        }
    }

    @Override
    public boolean useAutoConnect() {
        // Only allow reconnection if both devices are present. When devices are being bonded, if
        // the auto connect kicks in at the wrong 1ime, it can fragment the devices and break
        // things.
        return getDevice(G1Constants.Side.LEFT.getDeviceIndex()) != null &&
               getDevice(G1Constants.Side.RIGHT.getDeviceIndex()) != null;
    }

    private G1SideManager createSideFromIndex(int deviceIdx, BluetoothGattCharacteristic rx,
                                              BluetoothGattCharacteristic tx) {
        // Package some of the DeviceSupport functions as callbacks here. We deliberately skip
        // passing in "this" because we don't want to forward ALL functionality of the device
        // support and we don't want a hard dependency on G1DeviceSupport in G1SideManager.
        Callable<BtLEQueue> getQueue = () -> this.getQueue(deviceIdx);
        Callable<GBDevice> getDevice = () -> this.getDevice(deviceIdx);

        // Create the desired side.
        if (deviceIdx == G1Constants.Side.LEFT.getDeviceIndex()) {
            leftSide = new G1SideManager(G1Constants.Side.LEFT, backgroundTasksHandler, getQueue,
                                         getDevice, this::evaluateGBDeviceEvent,
                                         this::getDevicePrefs, rx, tx,
                                         this::createTransactionBuilder);
            return leftSide;
        } else if (deviceIdx == G1Constants.Side.RIGHT.getDeviceIndex()) {
            rightSide = new G1SideManager(G1Constants.Side.RIGHT, backgroundTasksHandler, getQueue,
                                          getDevice, this::evaluateGBDeviceEvent,
                                          this::getDevicePrefs, rx, tx,
                                          this::createTransactionBuilder);
            return rightSide;
        }

        // Return null under an unexpected index.
        return null;
    }

    private G1SideManager getSideFromIndex(int deviceIdx) {
        if (deviceIdx == G1Constants.Side.LEFT.getDeviceIndex()) {
            return leftSide;
        } else if (deviceIdx == G1Constants.Side.RIGHT.getDeviceIndex()) {
            return rightSide;
        }
        return null;
    }

    private class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(G1Constants.INTENT_TOGGLE_SILENT_MODE)) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (device != null && device.equals(getDevice())) {
                    // We don't know what thread is handling this event, schedule the BLE to run in
                    // the background.
                    backgroundTasksHandler.post(G1DeviceSupport.this::onToggleSilentMode);
                }
            }
        }
    }

    private synchronized byte getNextSequence() {
        // Synchronized so the sequence increments atomically.
        // This number will eventually overflow, and that is fine. The sequence number is just to
        // match the request and response together.
        return globalSequence++;
    }

    /**
     * Lets a caller reserve multiple sequence all in one go.
     */
    private synchronized byte[] getNextSequence(byte reservedCount) {
        byte[] out = new byte[reservedCount];
        for (byte i = 0; i < reservedCount; i++) {
            out[i] = (byte)(globalSequence + i);
        }
        globalSequence += reservedCount;
        return out;
    }

    private void scheduleHeatBeat() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentMilliseconds = c.getTimeInMillis();
        long lastDelay = currentMilliseconds - lastHeartBeatTime;
        LOG.debug("{}ms since the last heartbeat", lastDelay);

        // The actual delay can change based on the sleep state of the phone CPU, the base delay
        // should always be enough to keep the glasses connected, however it uses the most amount
        // of battery. Since the amount of time the system adds is unknown, look at the last amount
        // of sleep time between the last heart beat and now to estimate how much of a delay should
        // be used to get as close to the target as possible. When transitioning to a low power
        // state, the timeout may be crossed because the heartbeat was too late, in that case, we
        // rely on the reconnection logic to connect back. After reconnection, the base delay will
        // be used and the correct system added delay will be determined.
        long systemAddedTime = lastDelay - lastHeartBeatDelayTarget;

        // Anytime we disconnect due to the heartbeat not being fast enough, make the heartbeat more
        // and more aggressive, so subtract the modifier here.
        long delay = G1Constants.HEART_BEAT_TARGET_DELAY_MS - heartBeatTargetModifier;
        LOG.info("{}ms since the last heartbeat, system delay {}ms, modified target {}ms", lastDelay, systemAddedTime, delay);
        if (systemAddedTime > 0) {
            delay -= systemAddedTime;
        }

        // Bound the delay between the base and the target.
        delay = Math.max(delay, G1Constants.HEART_BEAT_BASE_DELAY_MS);
        delay = Math.min(delay, G1Constants.HEART_BEAT_TARGET_DELAY_MS);

        backgroundTasksHandler.removeCallbacksAndMessages(heartBeatRunner);
        LOG.debug("Starting heartbeat runner delayed by {}ms", delay);
        backgroundTasksHandler.postDelayed(heartBeatRunner, delay);

        lastHeartBeatTime = currentMilliseconds;
        lastHeartBeatDelayTarget = delay;
    }

    private synchronized void sendDisplaySettings() {
        DevicePrefs prefs = getDevicePrefs();
        // Synchronized so that there can only ever be one background task.
        // Clear any existing runner in case the user has changed the value multiple times
        // before th delay expired.
        backgroundTasksHandler.removeCallbacksAndMessages(displaySettingsPreviewCloserRunner);

        // The glasses expect the setting to be sent with the preview mode set to true.
        G1Communications.CommandHardwareDisplaySet
                command = new G1Communications.CommandHardwareDisplaySet(
                getNextSequence(),
                true /* preview */,
                // Height ranges from 0-8 instead of 1-9, so offset by one to convert from
                // the slider space.
                (byte)(prefs.getInt(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_HEIGHT, 1) - 1),
                (byte)prefs.getInt(DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_DEPTH, 1));

        // Send to both sides.
        leftSide.send(command);
        rightSide.send(command);

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

    private void forceNextCalendarSync() {
        synchronized(calendarLock) {
            lastSyncedEvents = null;
        }
    }

    private void syncCalendar() {
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR, false)) {
            // Clear the last sync list so that when syncing is re-enabled a sync is forced.
            lastSyncedEvents = null;
            return;
        }

        // Run this on an async thread instead of directly in case was run from the UI thread.
        backgroundTasksHandler.post(() -> {
            LOG.info("Even G1 Calendar Sync");
            List<CalendarEvent> events;
            synchronized (calendarLock) {
                // Filter out any events in the past.
                Calendar now = Calendar.getInstance();
                CalendarManager
                        calendarManager  = new CalendarManager(getContext(), getDevice().getAddress());
                events = calendarManager.getCalendarEventList()
                                .stream()
                                .filter(e -> e.isAllDay() ||
                                             e.getBegin() > (now.getTimeInMillis() - G1Constants.CALENDAR_EVENT_CLEAR_DELAY))
                                .toList();

                // Schedule a sync at the start of the next event so it can be removed from the screen.
                // If there are no events, just sync again in the future.
                long nextSyncTime = (events.isEmpty() ? System.currentTimeMillis() : events.get(0).getBegin()) + G1Constants.CALENDAR_EVENT_CLEAR_DELAY;
                LOG.info("Next Calendar Sync Time: {}", new Date(nextSyncTime));

                // The delay needs to be relative to the current time.
                long syncDelay = nextSyncTime - System.currentTimeMillis();
                if (syncDelay < 0) {
                    // If the sync time is in the past, something went wrong, in that case, schedule
                    // the resync for 5 seconds in the future and try again then.
                    syncDelay = System.currentTimeMillis() + 5000;
                }
                backgroundTasksHandler.removeCallbacksAndMessages(calendarSyncRunner);
                backgroundTasksHandler.postDelayed(calendarSyncRunner, syncDelay);

                // The list of events is the same as the last sync, so nothing to do.
                // Using .equals() checks the contents of the lists, not the actual instance, so a newly
                // generated list will will be equal.
                if (events.equals(lastSyncedEvents)) {
                    LOG.info("Skipping Calendar Sync, no new events");
                    return;
                }

                // Ready sync the current list.
                lastSyncedEvents = events;
            }

            boolean use12HourFormat =
                    getDevicePrefs().getTimeFormat()
                                    .equals(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H);

            // The same payload is sent to both sides, so only generated it once.
            byte[] calendarPayload = G1Communications.CommandDashboardCalendarSet.generatePayload(use12HourFormat, events);

            // The sequence ids should be same between the left and right, so reserve them now.
            byte sequenceCount = G1Communications.CommandDashboardCalendarSet.getRequiredSequenceCount(calendarPayload);
            byte[] sequenceIds = getNextSequence(sequenceCount);

            // This block is synchronized. We do not want two calls to overlap, otherwise the lenses
            // could get skewed with different values.
            synchronized (lensSkewLock) {
                G1CommandHandler leftCommandHandler =
                        new G1Communications.CommandDashboardCalendarSet(sequenceIds, calendarPayload, leftSide::send);

                G1CommandHandler rightCommandHandler =
                        new G1Communications.CommandDashboardCalendarSet(sequenceIds, calendarPayload, rightSide::send);

                // The commands can be sent in parallel.
                leftSide.send(leftCommandHandler);
                rightSide.send(rightCommandHandler);

                if (!leftCommandHandler.waitForResponsePayload() || !rightCommandHandler.waitForResponsePayload()) {
                    LOG.error("Set calendar events on timed out");
                    getDevice().setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, getContext());
                }
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////
    // Below are all the onXXX() handlers overridden from the base class. //
    ////////////////////////////////////////////////////////////////////////

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        BluetoothDevice device = gatt.getDevice();
        if (device == null) {
            return;
        }

        GBDevice leftDevice = getDevice(G1Constants.Side.LEFT.getDeviceIndex());
        GBDevice rightDevice = getDevice(G1Constants.Side.RIGHT.getDeviceIndex());
        boolean isLeft = leftDevice != null && leftDevice.getAddress().equals(device.getAddress());
        boolean isRight = rightDevice != null && rightDevice.getAddress().equals(device.getAddress());

        if (!isLeft && !isRight) {
            // Not one of the managed devices, nothing to do.
            return;
        }

        if (newState == BluetoothGattServer.STATE_DISCONNECTED) {
            // If either side disconnects, initiate a reconnection on both sides.
            if (status != BluetoothGatt.GATT_SUCCESS) {
                synchronized (ConnectionMonitor) {
                    LOG.info("One side unexpectedly diconnected, attempting to reconnect both.");
                    // The sides must also have their state reset so that the fully initialization
                    // processes is followed.
                    // TODO: Add an intermediate state that the initialization process can respect where
                    // only minimal communications are sent. For example the notification whitelist
                    // messages don't need to be resent.
                    if (leftSide != null) leftSide.resetConnectingState();
                    if (rightSide != null) rightSide.resetConnectingState();

                    // HACK: disconnect() will notify the whole system of the disconnection if the
                    // device is not already in the disconnected state, so manually set it to
                    // disconnected temporarily just so the global broadcast isn't sent out.
                    if (leftDevice != null) leftDevice.setState(GBDevice.State.NOT_CONNECTED);
                    if (rightDevice != null) rightDevice.setState(GBDevice.State.NOT_CONNECTED);

                    disconnect();

                    if (leftDevice != null) leftDevice.setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, getContext());
                    if (rightDevice != null) rightDevice.setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, getContext());

                    // Anytime we disconnect due to the heartbeat not being fast enough, make the
                    // target modifier to the heartbeat more and more aggressive.
                    Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    long currentMilliseconds = c.getTimeInMillis();
                    long timeSinceLastHeartBeat = currentMilliseconds - lastHeartBeatTime;
                    if (status == /* GATT_CONN_TERMINATE_PEER_USER */ 0x13 && lastHeartBeatDelayTarget < timeSinceLastHeartBeat) {
                        long missedBy = timeSinceLastHeartBeat - lastHeartBeatDelayTarget;
                        heartBeatTargetModifier = Math.min(heartBeatTargetModifier + missedBy,
                                                           G1Constants.HEART_BEAT_MAX_DELAY_MODIFIER_MS);
                        LOG.info("Heartbeat not fast enough by {}ms, new modifier {}ms", missedBy,
                                 heartBeatTargetModifier);
                    }
                }
            }
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);

        // If the status was not successful, don't forward to the glasses.
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        // The glasses expect to be forwarded the MTU, so when it is changed, also notify the side
        // that it changed on. Cap the MTU value that is sent to the glasses to the officially supported
        // value. The FW on the glasses seems to internal allocate a buffer at this size, but it has
        // a maximum value that is not tied to the real underlying BLE MTU.
        String address = gatt.getDevice().getAddress();
        if (getDevice(G1Constants.Side.LEFT.getDeviceIndex()) != null) {
            String leftAddress = getDevice(G1Constants.Side.LEFT.getDeviceIndex()).getAddress();
            if (address.equals(leftAddress) && leftSide != null) {
                leftSide.send(new G1Communications.CommandMtuSet((byte)Math.min(G1Constants.MTU, mtu)));
            }
        }

        if (getDevice(G1Constants.Side.RIGHT.getDeviceIndex()) != null) {
            String rightAddress =
                    getDevice(G1Constants.Side.RIGHT.getDeviceIndex()).getAddress();
            if (address.equals(rightAddress) && rightSide != null) {
                rightSide.send(new G1Communications.CommandMtuSet((byte)Math.min(G1Constants.MTU, mtu)));
            }
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] payload) {
        // Super already handled this.
        if (super.onCharacteristicChanged(gatt, characteristic, payload)) {
            return true;
        }

        // If this is the correct UART RX message, forward to the correct side based on the BLE
        // address.
        if (characteristic.getUuid().equals(G1Constants.UUID_CHARACTERISTIC_NORDIC_UART_RX)) {
            String address = gatt.getDevice().getAddress();
            if (getDevice(G1Constants.Side.LEFT.getDeviceIndex()) != null) {
                String leftAddress = getDevice(G1Constants.Side.LEFT.getDeviceIndex()).getAddress();
                if (address.equals(leftAddress) && leftSide != null) {
                    return leftSide.handlePayload(payload);
                }
            }

            if (getDevice(G1Constants.Side.RIGHT.getDeviceIndex()) != null) {
                String rightAddress =
                        getDevice(G1Constants.Side.RIGHT.getDeviceIndex()).getAddress();
                if (address.equals(rightAddress) && rightSide != null) {
                    return rightSide.handlePayload(payload);
                }
            }
        }

        // Not handled by either side.
        LOG.debug("Unhandled payload: {}", Logging.formatBytes(payload));
        return false;
    }

    /**
     * If configuration options can be set on the device, this method
     * can be overridden and implemented by the device support class.
     *
     * @param config the device specific option to set on the device
     */
    @Override
    public void onSendConfiguration(String config) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_ACTIVATION_ANGLE:
                // This setting is only sent to the right arm.
                if (rightSide != null)
                    rightSide.onSendConfiguration(config);
                break;
            case DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_HEIGHT:
            case DeviceSettingsPreferenceConst.PREF_EVEN_REALITIES_SCREEN_DEPTH:
                sendDisplaySettings();
                break;
            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                // Units or time format updated, update the time and weather on the glasses to match
                onSetTimeOrWeather();
                // Fall through to update the calendar with the new format.
            case GBPrefs.CALENDAR_BLACKLIST:
            case DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_EVENTS_AMOUNT:
            case DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_CANCELED:
            case DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_DECLINED:
            case DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_FOCUS_TIME:
            case DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_ALL_DAY:
            case DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_WORKING_LOCATION:
            case DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_COLOR_BLACKLIST:
            case DeviceSettingsPreferenceConst.PREF_CALENDAR_LOOKAHEAD_DAYS:
            case DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR:
            case DeviceSettingsPreferenceConst.PREF_SYNC_BIRTHDAYS:
                onSetDashboardMode();
                forceNextCalendarSync();
                syncCalendar();
                break;
            case SettingsActivity.PREF_LANGUAGE:
                onLanguageChange();
                // The calendar entries have locale specific parts (eg. day of the week) so sync the
                // calendar when the language changes.
                forceNextCalendarSync();
                syncCalendar();
                break;
            default:
                // Forward to both sides.
                if (leftSide != null)
                    leftSide.onSendConfiguration(config);
                if (rightSide != null)
                    rightSide.onSendConfiguration(config);
                break;
        }
    }

    private void onSetTimeOrWeather() {
        if (leftSide == null || rightSide == null)
            return;

        // In FW v1.6.0, they flipped this boolean.
        boolean use12HourFormat =
                getDevicePrefs().getTimeFormat()
                          .equals(getDevice().getFirmwareVersion().compareTo("1.6.0") >= 0
                                    ? DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H
                                    : DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H);

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentMilliseconds = c.getTimeInMillis();
        long tzOffset = TimeZone.getDefault().getOffset(currentMilliseconds);
        long timeMilliseconds = currentMilliseconds + tzOffset;

        // Check if the GB settings are set to metric, if not, set the temp to use Fahrenheit.
        String metricString = GBApplication.getContext().getString(R.string.p_unit_metric);
        boolean useFahrenheit = !GBApplication.getPrefs()
                                              .getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM,
                                                         metricString).equals(metricString);

        // Pull the weather into a local variable so that if it changes between the two lenses being
        // updated, we won't end up with a skewed value.
        @Nullable WeatherSpec weather = Weather.getWeatherSpec();

        // Run in the background in case the command hangs and this was run from the UI thread.
        backgroundTasksHandler.post(() -> {
            // This block is synchronized. We do not want two calls to overlap, otherwise the lenses
            // could get skewed with different values.
            synchronized (lensSkewLock) {
                // Send the left the time synchronously, then once a response is received, send the right.
                // The glasses will ignore the command on the right lens if it arrives before the left.
                byte sequence = getNextSequence();
                G1CommandHandler leftCommandHandler =
                        new G1Communications.CommandDashboardWeatherAndTimeSet(sequence, timeMilliseconds,
                                                                               use12HourFormat, weather,
                                                                               useFahrenheit);
                leftSide.send(leftCommandHandler);
                if (!leftCommandHandler.waitForResponsePayload()) {
                    LOG.error("Set time on left lens timed out");
                    getDevice().setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, getContext());
                }

                rightSide.send(
                        new G1Communications.CommandDashboardWeatherAndTimeSet(sequence, timeMilliseconds,
                                                                               use12HourFormat, weather,
                                                                               useFahrenheit));
            }
        });
    }

    private void onToggleSilentMode() {
        if (leftSide == null || rightSide == null)
            return;

        // If both lenses are in sync on what the status is, set them both. Otherwise, only set the
        // right one so they can be resynchronized.
        if (leftSide.getSilentModeStatus() == rightSide.getSilentModeStatus()) {
            leftSide.onToggleSilentMode();
            rightSide.onToggleSilentMode();
        } else {
            rightSide.onToggleSilentMode();
        }
    }

    private void onSetDashboardMode() {
        // Run in the background in case the command hangs and this was run from the UI thread.
        backgroundTasksHandler.post(() -> {
            // TODO: Support more than just calendar.
            boolean showCalendar = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR, false);
            byte mode = showCalendar ? G1Constants.DashboardMode.DUAL : G1Constants.DashboardMode.MINIMAl;
            byte pane = showCalendar ? G1Constants.DashboardPaneMode.CALENDAR : G1Constants.DashboardPaneMode.EMPTY;

            // This block is synchronized. We do not want two calls to overlap, otherwise the lenses
            // could get skewed with different values.
            synchronized (lensSkewLock) {
                // Send to the left synchronously, then once a response is received, send the right.
                // The glasses will ignore the command on the right lens if it arrives before the
                // left.
                byte sequence = getNextSequence();
                G1CommandHandler leftCommandHandler =
                        new G1Communications.CommandDashboardModeSet(sequence, mode, pane);

                leftSide.send(leftCommandHandler);
                if (!leftCommandHandler.waitForResponsePayload()) {
                    LOG.error("Set dashboard on left lens timed out");
                    getDevice().setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, getContext());
                    return;
                }

                rightSide.send(new G1Communications.CommandDashboardModeSet(sequence, mode, pane));
            }
        });
    }

    @Override
    public void onReset(int flags) {
        if (flags == GBDeviceProtocol.RESET_FLAGS_REBOOT) {
            leftSide.send(new G1Communications.CommandSystemRebootControl());
            rightSide.send(new G1Communications.CommandSystemRebootControl());
        }
    }

    @Override
    public void onSendWeather() {
        onSetTimeOrWeather();
    }

    @Override
    public void onSetTime() {
        onSetTimeOrWeather();
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        // Rewrite the App Id to the fixed one used for all notifications. See the comment in
        // G1Constants.java for more information.
        notificationSpec.sourceAppId = G1Constants.FIXED_NOTIFICATION_APP_ID.first;
        // Notifications are only sent to the left side.
        leftSide.send(new G1Communications.CommandNotificationSendControl(leftSide::send, notificationSpec));
    }

    @Override
    public void onDeleteNotification(int id) {
        leftSide.send(new G1Communications.CommandNotificationClearControl(id));
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        // Always sync all events.
        syncCalendar();
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        // Always sync all events.
        syncCalendar();
    }

    public void onLanguageChange() {
        String localeString = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(SettingsActivity.PREF_LANGUAGE, "auto");
        if (localeString.equals("auto")) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            localeString = language + "_" + country.toUpperCase();
        }

        // If the language is set to one of the supported ones update the glasses to that language.
        // If the language is not supported, english is used as the default.
        byte language = switch (localeString.substring(0, 2)) {
            case "zh" -> G1Constants.LanguageId.CHINESE;
            case "ja" -> G1Constants.LanguageId.JAPANESE;
            case "ko" -> G1Constants.LanguageId.KOREAN;
            case "fr" -> G1Constants.LanguageId.FRENCH;
            case "de" -> G1Constants.LanguageId.GERMAN;
            case "es" -> G1Constants.LanguageId.SPANISH;
            case "it" -> G1Constants.LanguageId.ITALIAN;
            default -> G1Constants.LanguageId.ENGLISH;
        };

        byte sequence = getNextSequence();
        G1CommandHandler leftCommandHandler =
                new G1Communications.CommandLanguageSet(sequence, language);

        leftSide.send(leftCommandHandler);
        if (!leftCommandHandler.waitForResponsePayload()) {
            LOG.error("Set language on left lens timed out");
            getDevice().setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, getContext());
            return;
        }

        rightSide.send(new G1Communications.CommandLanguageSet(sequence, language));
    }
}
