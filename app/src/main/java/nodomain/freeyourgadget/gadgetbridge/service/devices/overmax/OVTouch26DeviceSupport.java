/*  Copyright (C) 2019-2025 Andreas Shimokawa, Arjan Schrijver, Cre3per,
    Damien Gaignon, Taavi Eomäe, vappster

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
//TODO: Implement SpO2, sleep and sports data sync, as well as various misc commands (for ex.
//smart camera, automatic date & time sync, etc.)
//TODO: Various settings (device wake gesture, alarms, etc.)
//TODO: Firmware updates

package nodomain.freeyourgadget.gadgetbridge.service.devices.overmax;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.overmax.OVTouch26Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.overmax.OVTouch26SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.OVTouch26ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class OVTouch26DeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(OVTouch26DeviceSupport.class);
    private final DeviceInfoProfile<OVTouch26DeviceSupport> deviceInfoProfile;
    int packet_seq = 0x0000;
    private BluetoothGattCharacteristic mControlCharacteristic = null;
    private BluetoothGattCharacteristic mReportCharacteristic = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable batteryRunner = () -> {
        LOG.info("Running retrieving battery through runner.");
        sendCmd(OVTouch26Constants.CMD_BATTERY_INFO, OVTouch26Constants.ARGS_NONE, "getBatteryInfo");
    };
    private OVTouch26ActivitySample CurrentSample = null;

    public OVTouch26DeviceSupport() {
        super(LOG);

        addSupportedService(OVTouch26Constants.UUID_SERVICE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);

        IntentListener mListener = new IntentListener() {
            @Override
            public void notify(Intent intent) {
                String action = intent.getAction();
                if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                    handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
                }
            }
        };

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private byte[] craftData(byte[] command, byte[] data) {
        byte[] payload = new byte[data.length + 4];
        byte[] result = new byte[OVTouch26Constants.PACKET_CMD_TEMPLATE.length + data.length];
        int crc;

        System.arraycopy(OVTouch26Constants.PACKET_CMD_TEMPLATE, 0, result, 0, OVTouch26Constants.PACKET_CMD_TEMPLATE.length);

        //The unknown and command bytes are part of the checksum-protected data
        result[8] = command[0];
        result[10] = command[1];

        System.arraycopy(data, 0, result, OVTouch26Constants.PACKET_CMD_TEMPLATE.length, data.length);
        System.arraycopy(result, 8, payload, 0, payload.length);
        crc = CheckSums.getCRC16ansi(payload, 0x0000);

        //All of these are not checksum-protected
        result[2] = (byte) ((data.length + 4 >> 8) & 0xFF);
        result[3] = (byte) (data.length + 4 & 0xFF);
        result[4] = (byte) ((crc >> 8) & 0xFF);
        result[5] = (byte) (crc & 0xFF);
        result[6] = (byte) ((packet_seq >> 8) & 0xFF);
        result[7] = (byte) (packet_seq & 0xFF);

        if (packet_seq == 0xFFFF) {
            packet_seq = 0;
        } else {
            packet_seq++;
        }

        return result;
    }

    private OVTouch26ActivitySample createActivitySample() {
        OVTouch26ActivitySample sample = new OVTouch26ActivitySample();

        sample.setTimestamp(-1);
        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
        sample.setRawKind(OVTouch26SampleProvider.TYPE_ACTIVITY);
        sample.setSteps(ActivitySample.NOT_MEASURED);
        sample.setHeartRate(ActivitySample.NOT_MEASURED);

        return sample;
    }

    private void sendTransaction(byte[] data, String transactionName) {
        TransactionBuilder transactionBuilder = this.createTransactionBuilder(transactionName);

        transactionBuilder.write(this.mControlCharacteristic, data);
        try {
            transactionBuilder.queueConnected();
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
        }
    }

    private void sendCmd(byte[] cmd, byte[] args, String transactionName) {
        byte[] data = this.craftData(cmd, args);

        sendTransaction(data, transactionName);
    }

    private void sendAck(byte[] packet) {
        byte[] ackPacket = OVTouch26Constants.PACKET_ACK_TEMPLATE;
        ackPacket[6] = packet[packet.length - 2];
        ackPacket[7] = packet[packet.length - 1];

        sendTransaction(ackPacket, "sendAckPacket");
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        CurrentSample = createActivitySample();
        sendCmd(OVTouch26Constants.CMD_GET_STEPS, OVTouch26Constants.ARGS_NONE, "sendGetStepsCmd");
    }

    @Override
    public void onFindDevice(boolean start) {
        if (!start) {
            return;
        }

        sendCmd(OVTouch26Constants.CMD_FIND_DEVICE, OVTouch26Constants.ARGS_FIND_DEVICE, "findDevice");
    }

    public boolean startBatteryRunnerDelayed() {
        int interval_minutes = GBApplication.getDevicePrefs(gbDevice).getBatteryPollingIntervalMinutes();
        int interval = interval_minutes * 60 * 1000;
        LOG.debug("Starting battery runner delayed by {} ({} minutes)", interval, interval_minutes);
        handler.removeCallbacks(batteryRunner);
        return handler.postDelayed(batteryRunner, interval);
    }

    public void stopBatteryRunnerDelayed() {
        LOG.debug("Stopping battery runner delayed");
        handler.removeCallbacks(batteryRunner);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        packet_seq = 0x0000;
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 0, getContext());

        builder.setDeviceState(GBDevice.State.INITIALIZING);
        deviceInfoProfile.requestDeviceInfo(builder);

        this.mControlCharacteristic = getCharacteristic(OVTouch26Constants.UUID_CHARACTERISTIC_CONTROL);
        this.mReportCharacteristic = getCharacteristic(OVTouch26Constants.UUID_CHARACTERISTIC_REPORT);

        builder.notify(this.mReportCharacteristic, true);
        builder.setCallback(this);

        builder.setDeviceState(GBDevice.State.INITIALIZED);

        //Send battery info command if polling is enabled to show the charge % in the devices screen as soon as we're connected
        //(It's too early to use the sendCmd func so we use the following instead)
        if (GBApplication.getDevicePrefs(getDevice()).getBatteryPollingEnabled()) {
            builder.write(this.mControlCharacteristic, this.craftData(OVTouch26Constants.CMD_BATTERY_INFO, OVTouch26Constants.ARGS_NONE));
        }

        GB.updateTransferNotification(null, "", false, 100, getContext());

        return builder;
    }

    private void handleDeviceInfo(DeviceInfo info) {
        LOG.debug("Device info: {}", info);

        GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    private void parseAndSetSampleTimestamp(byte[] packet, int offset) {
        //The sample timestamps reported by this watch report how many seconds have passed since 01/01/2000 at 00:00.
        //Therefore an easy way to convert them in a timestamp understandable by Gadgetbridge is to convert them to
        //Unix time by adding 946681200 seconds (= the difference to Unix epoch)
        if (CurrentSample.getTimestamp() == -1) {   //If the timestamp has already been set by a previous cmd reply, don't overwrite it
            int timestamp = BLETypeConversions.toUint32(packet[packet.length - offset], packet[packet.length - (offset + 1)], packet[packet.length - (offset + 2)], packet[packet.length - (offset + 3)]) + 946681200;
            CurrentSample.setTimestamp(timestamp);
        }
    }

    private void addCurrentSample() {
        if (CurrentSample.getTimestamp() != -1) { //If timestamp is -1 it means no data has been recorded and the current sample can be discarded
            try (DBHandler handler = GBApplication.acquireDB()) {
                DaoSession session = handler.getDaoSession();

                OVTouch26SampleProvider provider = new OVTouch26SampleProvider(gbDevice, session);
                Device device = DBHelper.getDevice(getDevice(), handler.getDaoSession());
                User user = DBHelper.getUser(handler.getDaoSession());
                CurrentSample.setDevice(device);
                CurrentSample.setUser(user);
                CurrentSample.setProvider(provider);
                provider.addGBActivitySample(CurrentSample);
                Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                        .putExtra(GBDevice.EXTRA_DEVICE, getDevice())
                        .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, CurrentSample)
                        .putExtra(DeviceService.EXTRA_TIMESTAMP, CurrentSample.getTimestamp());
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            } catch (Exception e) {
                LOG.error("Error acquiring database", e);
            }
        }
        CurrentSample = null;
        GB.updateTransferNotification(null, "", false, 100, getContext());  //Clear notification
        GB.signalActivityDataFinish(getDevice());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        //The sample requests in the proprietary app always happens in this sequence:
        //Steps -> Heart Rate -> (SpO2) -> (Sleep) -> (Sports data)
        //We try to stick to that order here
        switch (BLETypeConversions.toUint32(value[3], value[2], value[1], value[0])) {  //Reply packet header, little endian
            case OVTouch26Constants.REPLY_HEADER_BATTERY:
                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();

                batteryInfo.level = value[5];
                batteryInfo.state = BatteryState.UNKNOWN;

                this.handleGBDeviceEvent(batteryInfo);
                //Restart the battery runner once the packet has been handled
                //(no need to check  if battery polling is enabled as we only ever request battery info if it's enabled to begin with)
                startBatteryRunnerDelayed();
                break;
            case OVTouch26Constants.REPLY_HEADER_STEPS:
                //Sometimes when the device errors out it can return a much longer packet with what looks to be garbage data. In that case, we can just ignore it
                //TODO: On the other hand, in *very* rare cases (for ex. multiple dropped packets in the span of 2+ days) the packet can include two samples with
                //multiple timestamps (so far it seems like two at a time is the limit), add logic to parse those as well
                if (CurrentSample != null && value.length == 13) {
                    int steps = BLETypeConversions.toUint32(value[value.length - 1], value[value.length - 2], value[value.length - 3], value[value.length - 4]);
                    parseAndSetSampleTimestamp(value, 5);
                    CurrentSample.setSteps(steps);
                    sendCmd(OVTouch26Constants.CMD_GET_HEART_RATE, OVTouch26Constants.ARGS_NONE, "getHeartRate");
                }
                break;
            case OVTouch26Constants.REPLY_HEADER_HEART_RATE:
                if (CurrentSample != null) {
                    if (value.length >= 10){
                        parseAndSetSampleTimestamp(value, 2);
                        CurrentSample.setHeartRate(value[value.length - 1]);
                    }
                    addCurrentSample(); //Always try to add the sample, even if heart rate was not reported
                }
                break;
            case OVTouch26Constants.REPLY_HEADER_FIND_MY_PHONE:
                GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
                if (value[5] == 0x01) {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                } else {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                }
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            case OVTouch26Constants.REPLY_HEADER_ERROR: //If the watch returns an error the sent data is likely garbage/incorrect
                LOG.debug("device reported error, invalidating sample!");
                CurrentSample = null;
                break;
            default:
                if (BLETypeConversions.toUint24(value[2], value[1], value[0]) == OVTouch26Constants.REPLY_HEADER_ACK) {
                    //Technically speaking, most packets seem to have their own distinct ACK requests identifiable by an unique
                    //ack id in the header (uint8, offset 0x3). Practically, they all follow the same request/response structure,
                    //and failing to reply to enough of them *will* eventually make the watch return an error
                    //Therefore, it's better to always reply to ACKs requests, regardless if the associated command is currently
                    //implemented or otherwise
                    sendAck(value);
                }
                break;
        }

        return false;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String body = notificationSpec.body;
        String title = notificationSpec.title;
        if (title.isEmpty()) {
            title = "Notification";
        }
        if (body.isEmpty()) {
            body = title;
        }
        int titleLength = title.length();
        int bodyLength = body.length();
        if (titleLength > 32) {
            titleLength = 32;
        }
        if (bodyLength > 223) {
            bodyLength = 223;
        }  //Length is uint8 and its initial value is 32 (see below) so the body cannot exceed 255-32=223 bytes
        byte[] args = new byte[33 + bodyLength]; //1 byte for len + 32 bytes are *always* allocated for the title, even if it's shorter than that

        Arrays.fill(args, (byte) 0x00);
        args[0] = (byte) (32 + bodyLength);
        System.arraycopy(title.getBytes(), 0, args, 1, titleLength);
        System.arraycopy(body.getBytes(), 0, args, 33, bodyLength);

        sendCmd(OVTouch26Constants.CMD_NOTIFICATION, args, "onNotification");
    }

    @Override
    public void onHeartRateTest() {
        //NOTE: The proprietary app for this watch does not support on-demand heart rate reporting, meaning it will
        //simply display the value of the last heart rate sample taken by the watch either through periodic tests
        //or manually by pressing the associated icon on the watch's UI. As such, there seems to be no command for
        //an on-demand heart rate test, and because the watch clears that value from memory once it's sent, there
        //is a chance for this function to not return any value if, for example, it's used twice in a row
        CurrentSample = createActivitySample();
        sendCmd(OVTouch26Constants.CMD_GET_HEART_RATE, OVTouch26Constants.ARGS_NONE, "getHeartRate");
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }

    public void onSendConfiguration(String config) {
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_ENABLE:
                    if (!GBApplication.getDevicePrefs(gbDevice).getBatteryPollingEnabled()) {
                        stopBatteryRunnerDelayed();
                        break;
                    }
                    // Fall through if enabled
                case DeviceSettingsPreferenceConst.PREF_BATTERY_POLLING_INTERVAL:
                    if (!startBatteryRunnerDelayed()) {
                        GB.toast(getContext(), R.string.battery_polling_failed_start, Toast.LENGTH_SHORT, GB.ERROR);
                        LOG.error("Failed to start the battery polling");
                    }
                    break;
            }
    }

    @Override
    public void dispose() {
        stopBatteryRunnerDelayed();
        super.dispose();
    }
}
