package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEMultiDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

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
    private final HandlerThread backgroundThread = new HandlerThread("even_g1_background_thread", Process.THREAD_PRIORITY_DEFAULT);
    private Handler backgroundTasksHandler = null;
    private BroadcastReceiver intentReceiver = null;
    private final Object lensSkewLock = new Object();
    private G1SideManager leftSide = null;
    private G1SideManager rightSide = null;

    public G1DeviceSupport() {
        this(LOG);
    }

    public G1DeviceSupport(Logger logger) {
        super(logger, 2);
        addSupportedService(G1Constants.UUID_SERVICE_NORDIC_UART,
                            G1Constants.Side.LEFT.getDeviceIndex());

        addSupportedService(G1Constants.UUID_SERVICE_NORDIC_UART,
                            G1Constants.Side.RIGHT.getDeviceIndex());
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
        // IMPORTANT: use getDevice(deviceIdx), not getDevice(/* 0 */) here otherwise the device
        // will lock up in a half initialized state because GB thinks the left side is initialized,
        // after because the right ran first.
        if (side.getConnectingState() == GBDevice.State.CONNECTED) {
            builder.setDeviceState(GBDevice.State.INITIALIZING);
            side.initialize(builder);
        }

        synchronized (this) {
            if (leftSide != null && leftSide.getConnectingState() == GBDevice.State.INITIALIZED &&
                rightSide != null && rightSide.getConnectingState() == GBDevice.State.INITIALIZED) {
                // set device firmware to prevent the following error when data is saved to the
                // database and device firmware has not been set yet.
                // java.lang.IllegalArgumentException: the bind value at index 2 is null.
                // Must be called before the PostInitialize down below.
                getDevice().setFirmwareVersion("N/A");
                getDevice().setFirmwareVersion2("N/A");

                // Both sides are initialized. The whole device is initialized, don't use a device
                // index here. Device 0 is the device that the reset of GB sees.
                builder.setDeviceState(GBDevice.State.INITIALIZED);
                // This means that both sides have been connected to and basic info has been collected.
                // These next steps require that both sides are ready which is why they are done post
                // individual initialization. We don't know what thread we are handling the update state
                // event on, so to be safe, schedule these as a background task.
                backgroundTasksHandler.postDelayed(() -> {
                    leftSide.postInitializeLeft();
                    rightSide.postInitializeRight();
                    onSetDashboardMode();
                    onSetTime();
                }, 200);
            }
        }

        getDevice().sendDeviceUpdateIntent(getContext());
        return builder;
    }

    @Override
    public void dispose() {
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

    ////////////////////////////////////////////////////////////////////////
    // Below are all the onXXX() handlers overridden from the base class. //
    ////////////////////////////////////////////////////////////////////////

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
            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT:
                // Units or time format updated, update the time and weather on the glasses to match
                onSetTimeOrWeather();
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

        // In  FW v1.6.0, they flipped this boolean.
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
                G1Communications.CommandHandler leftCommandHandler =
                        new G1Communications.CommandSetTimeAndWeather(timeMilliseconds,
                                                                      use12HourFormat, weather,
                                                                      useFahrenheit);
                leftSide.send(leftCommandHandler);
                if (!leftCommandHandler.waitForResponsePayload()) {
                    LOG.error("Set time on left lens timed out");
                    getDevice().setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, getContext());
                }

                rightSide.send(new G1Communications.CommandSetTimeAndWeather(timeMilliseconds,
                                                                             use12HourFormat,
                                                                             weather,
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
            // This block is synchronized. We do not want two calls to overlap, otherwise the lenses
            // could get skewed with different values.
            synchronized (lensSkewLock) {
                // Send to the left synchronously, then once a response is received, send the right.
                // The glasses will ignore the command on the right lens if it arrives before the
                // left.
                // TODO: Pull these values from the settings and build a UI to configure it.
                G1Communications.CommandHandler leftCommandHandler =
                        new G1Communications.CommandSetDashboardModeSettings(
                                G1Constants.DashboardConfig.MODE_MINIMAl,
                                G1Constants.DashboardConfig.PANE_EMPTY);

                leftSide.send(leftCommandHandler);
                if (!leftCommandHandler.waitForResponsePayload()) {
                    LOG.error("Set dashboard on right lens timed out");
                    getDevice().setUpdateState(GBDevice.State.WAITING_FOR_RECONNECT, getContext());
                }

                rightSide.send(new G1Communications.CommandSetDashboardModeSettings(
                        G1Constants.DashboardConfig.MODE_MINIMAl,
                        G1Constants.DashboardConfig.PANE_EMPTY));
            }
        });
    }

    @Override
    public void onReset(int flags) {
        if (flags == GBDeviceProtocol.RESET_FLAGS_REBOOT) {
            leftSide.send(new G1Communications.CommandSendReset());
            rightSide.send(new G1Communications.CommandSendReset());
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
        leftSide.send(new G1Communications.CommandSendNotification(leftSide::send, notificationSpec));
    }

    @Override
    public void onDeleteNotification(int id) {
        leftSide.send(new G1Communications.CommandSendClearNotification(id));
    }
}
