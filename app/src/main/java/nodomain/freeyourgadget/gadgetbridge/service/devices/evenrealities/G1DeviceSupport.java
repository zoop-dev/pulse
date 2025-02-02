package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Support class for the Even Realities G1. This sends and receives commands to and from the device.
 * The Protocol is mostly defined in G1Constants.java right now. In the future the protocol will be
 * broken out to a different class.
 * One interesting point about this device is that it requires a constant BLE connection which is
 * contrary to the way BLE is supposed to work. Unfortunately the device will show the disconnected
 * icon and stop displaying any information when it is in the disconnected state. Because of this,
 * we need to send a heartbeat ever 30 seconds, otherwise the device will disconnect and reconnect
 * every 32 seconds per the BLE spec.
 */
public class G1DeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(G1DeviceSupport.class);
    private final Handler backgroundTasksHandler = new Handler(Looper.getMainLooper());
    private BluetoothGattCharacteristic rxCharacteristic;
    private BluetoothGattCharacteristic txCharacteristic;
    private int heartBeatSequence;

    public G1DeviceSupport() {
        this(LOG);
    }

    public G1DeviceSupport(Logger logger) {
        super(logger);
        addSupportedService(G1DeviceConstants.UUID_SERVICE_NORDIC_UART);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {//}, int deviceIdx) {
        this.heartBeatSequence = 0;
        builder.add(
                new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        rxCharacteristic = getCharacteristic(G1DeviceConstants.UUID_CHARACTERISTIC_NORDIC_UART_RX);
        txCharacteristic = getCharacteristic(G1DeviceConstants.UUID_CHARACTERISTIC_NORDIC_UART_TX);
        builder.requestMtu(G1DeviceConstants.MTU);

        if (rxCharacteristic == null || txCharacteristic == null) {
            // If the characteristics are not received from the device reconnect and try again.
            LOG.warn("RX/TX characteristics are null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT,
                                                 getContext()));
            GB.toast(getContext(), "Failed to connect to Glasses, waiting for reconnect.",
                     Toast.LENGTH_LONG, GB.ERROR);
            return builder;
        }

        // Register callbacks for this device.
        builder.setCallback(this);
        builder.notify(rxCharacteristic, true);

        // Send the command to fetch FW version.
        byte[] packet = new byte[2];
        packet[0] = G1DeviceConstants.CommandId.FW_INFO_REQUEST.id;
        packet[1] = 0x74;
        builder.write(txCharacteristic, packet);

        // Send the command to fetch battery info.
        packet = new byte[2];
        packet[0] = G1DeviceConstants.CommandId.BATTERY_LEVEL.id;
        packet[1] = 0x01;
        builder.write(txCharacteristic, packet);

        if (getDevice().getFirmwareVersion() == null) {
            getDevice().setFirmwareVersion("N/A");
            getDevice().setFirmwareVersion2("N/A");
        }

        // The glasses will auto disconnect after 30 seconds of no data on the wire.
        // Schedule a heartbeat task. If this is not enabled, button presses on the glasses will not
        // be sent to the phone, so realtime interactions won't work.
        scheduleHeatBeat();

        // Schedule the battery polling.
        scheduleBatteryPolling();

        // Device is ready for use.
        builder.add(
                new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        gbDevice.sendDeviceUpdateIntent(getContext());
        return builder;
    }

    @Override
    public void dispose() {
        // Remove all callbacks
        backgroundTasksHandler.removeCallbacksAndMessages(null);
        super.dispose();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        // Super already handled this.
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        // If this is the correct UART RX message, parse it.
        if (G1DeviceConstants.UUID_CHARACTERISTIC_NORDIC_UART_RX.equals(characteristic.getUuid())) {
            byte[] payload = characteristic.getValue();
            if (payload[0] == G1DeviceConstants.CommandId.BATTERY_LEVEL.id) {
                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                batteryInfo.state = BatteryState.BATTERY_NORMAL;
                batteryInfo.level = payload[2];
                handleGBDeviceEvent(batteryInfo);
            } else if (payload[0] == G1DeviceConstants.CommandId.FW_INFO_RESPONSE.id) {
                // FW info string
                String fwInfo = new String(payload, StandardCharsets.US_ASCII).trim();
                LOG.debug("Got FW: " + fwInfo);
                int versionStart = fwInfo.lastIndexOf(" ver ") + " ver ".length();
                int versionEnd = fwInfo.indexOf(',', versionStart);
                if (versionStart > -1 && versionEnd > versionStart) {
                    String version = fwInfo.substring(versionStart, versionEnd);
                    LOG.debug("Parsed fw version: " + version);
                    getDevice().setFirmwareVersion(version);
                }
            }
        }
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
            // Reschedule battery polling. The new schedule may be disabled.
            case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE:
            case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_INTERVAL:
                scheduleBatteryPolling();
                break;
        }
    }

    private void scheduleHeatBeat() {
        backgroundTasksHandler.removeCallbacksAndMessages(heartBeatRunner);
        backgroundTasksHandler.postDelayed(heartBeatRunner, 30 * 1000);
    }

    private void scheduleBatteryPolling() {
        backgroundTasksHandler.removeCallbacksAndMessages(batteryRunner);
        if (GBApplication.getDevicePrefs(gbDevice).getBatteryPollingEnabled()) {
            int interval_minutes =
                    GBApplication.getDevicePrefs(gbDevice).getBatteryPollingIntervalMinutes();
            int interval = interval_minutes * 60 * 1000;
            LOG.debug("Starting battery runner delayed by {} ({} minutes)", interval,
                      interval_minutes);
            backgroundTasksHandler.postDelayed(batteryRunner, interval);
        }
    }

    private final Runnable batteryRunner = () -> {
        TransactionBuilder builder = new TransactionBuilder("battery_request");
        byte[] packet = new byte[2];
        packet[0] = G1DeviceConstants.CommandId.BATTERY_LEVEL.id;
        packet[1] = 0x01;
        builder.write(txCharacteristic, packet);
        builder.queue(getQueue());

        // Schedule the next check.
        scheduleBatteryPolling();
    };


    private final Runnable heartBeatRunner = () -> {
        TransactionBuilder builder = new TransactionBuilder("heart_beat");
        int length = 6;
        byte[] packet = new byte[length];
        packet[0] = G1DeviceConstants.CommandId.HEARTBEAT.id;
        packet[1] = (byte) (length & 0xFF);
        packet[2] = 0x00; //(byte)((length >> 8) & 0xFF);
        packet[3] = (byte) (heartBeatSequence % 0xFF);
        packet[4] = 0x04;
        packet[5] = (byte) (heartBeatSequence % 0xFF);
        builder.write(txCharacteristic, packet);
        builder.queue(getQueue());

        // Schedule the next heartbeat.
        scheduleHeatBeat();
    };


}
