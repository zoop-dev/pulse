/*  Copyright (C) 2025  Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericTemperatureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.samples.UltrahumanActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.samples.UltrahumanDeviceStateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.UltrahumanActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.UltrahumanDeviceStateSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class UltrahumanDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(UltrahumanDeviceSupport.class);
    private BroadcastReceiver CommandReceiver;
    private int FetchTo;
    private int FetchFrom;
    private int FetchCurrent;

    public UltrahumanDeviceSupport(DeviceType type) {
        super(LOG);

        addSupportedService(UltrahumanConstants.UUID_SERVICE_COMMAND);
        addSupportedService(UltrahumanConstants.UUID_SERVICE_STATE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);

        DeviceInfoProfile<UltrahumanDeviceSupport> deviceProfile = new DeviceInfoProfile<>(this);
        deviceProfile.addListener(new UltrahumanIntentListener());
        addSupportedProfile(deviceProfile);
    }

    @Override
    public void dispose() {
        BroadcastReceiver receiver = CommandReceiver;
        CommandReceiver = null;

        if (receiver != null) {
            getContext().unregisterReceiver(receiver);
        }

        super.dispose();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // reset to avoid funny states for re-connect
        FetchTo = -1;
        FetchFrom = -1;
        FetchCurrent = -1;

        // required for DB
        if (getDevice().getFirmwareVersion() == null) {
            getDevice().setFirmwareVersion("N/A");
            getDevice().setFirmwareVersion2("N/A");
        }

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        if (CommandReceiver == null) {
            CommandReceiver = new UltrahumanBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(UltrahumanConstants.ACTION_AIRPLANE_MODE);
            ContextCompat.registerReceiver(getContext(), CommandReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        }

        // trying to read non-existing characteristics sometimes causes odd BLE failures
        // so avoid DeviceInfoProfile.requestDeviceInfo
        builder.read(getCharacteristic(DeviceInfoProfile.UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING));
        builder.read(getCharacteristic(DeviceInfoProfile.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING));
        builder.read(getCharacteristic(DeviceInfoProfile.UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING));

        // TODO - implement a "OPERATION_PING until answer" logic instead of waits
        // sometimes the device is quite quick and other times it takes a while after
        // BLE connectivity has been established before the services work reliably

        builder.wait(48 * 3); //BluetoothGatt.onConnectionUpdated typically reports interval=48

        builder.read(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_STATE));

        builder.wait(48 * 2);

        builder.notify(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_RESPONSE), true);
        builder.notify(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_STATE), true);

        builder.write(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_COMMAND), new byte[]{UltrahumanConstants.OPERATION_PING});

        boolean timeSync = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_TIME_SYNC, true);
        if (timeSync) {
            builder.add(new UltrahumanSetTimeAction(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_COMMAND)));
        }

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        String title = getContext().getString(R.string.busy_task_fetch_activity_data);
        GB.updateTransferNotification(title, "", true, 0, getContext());

        String task = getContext().getString(R.string.busy_task_fetch_activity_data);
        getDevice().setBusyTask(task);
        getDevice().sendDeviceUpdateIntent(getContext());

        FetchFrom = -1;
        FetchTo = -1;
        FetchCurrent = -1;

        TransactionBuilder builder = new TransactionBuilder("onFetchRecordedData");
        builder.write(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_COMMAND), new byte[]{UltrahumanConstants.OPERATION_GET_FIRST_RECORDING_NR});
        builder.write(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_COMMAND), new byte[]{UltrahumanConstants.OPERATION_GET_LAST_RECORDING_NR});

        if (isConnected()) {
            builder.queue(getQueue());
        } else {
            GB.toast(getContext(), R.string.devicestatus_disconnected, Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public boolean onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        if (super.onCharacteristicRead(gatt, characteristic, status)) {
            return true;
        }

        if (UltrahumanConstants.UUID_CHARACTERISTIC_STATE.equals(characteristic.getUuid())) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                return decodeDeviceState(characteristic.getValue());
            }
        }

        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] raw = characteristic.getValue();

        if (UltrahumanConstants.UUID_CHARACTERISTIC_STATE.equals(characteristicUUID)) {
            return decodeDeviceState(raw);
        } else if (UltrahumanConstants.UUID_CHARACTERISTIC_RESPONSE.equals(characteristicUUID)) {
            if (raw.length < 5) {
                LOG.error("received unexpectedly short BLE command response: {}", StringUtils.bytesToHex(raw));
                return false;
            }

            byte op = raw[0];
            byte success = raw[1];
            byte result = raw[2];
            // ignore checksums for now - algorithm is unknown
            //byte chk1 = raw[raw.length-1];
            //byte chk2 = raw[raw.length-2];

            if (success == 0x00) {
                LOG.debug("received BLE command response op={}, success={}, result={} : {}", op, success, result, StringUtils.bytesToHex(raw));
            } else if ((0xFF & success) == 0xFF) {
                LOG.warn("received BLE command response op={}, success={}, result={} : {}", op, success, result, StringUtils.bytesToHex(raw));
            } else {
                LOG.info("received BLE command response op={}, success={}, result={} : {}", op, success, result, StringUtils.bytesToHex(raw));
            }

            Context context = getContext();

            switch (op) {
                case UltrahumanConstants.OPERATION_GET_RECORDINGS:
                    return decodeRecordings(raw);
                case UltrahumanConstants.OPERATION_PING:
                    if (raw[1] != 0x00) {
                        String message = getContext().getString(R.string.ultrahuman_unhandled_error_response, raw[1], raw[0]);
                        GB.toast(getContext(), message, Toast.LENGTH_LONG, GB.ERROR);
                    }
                    return true;
                case UltrahumanConstants.OPERATION_ACTIVATE_AIRPLANE_MODE:
                    switch (raw[2]) {
                        case 0x01:
                            GB.toast(context, context.getString(R.string.ultrahuman_airplane_mode_activated), Toast.LENGTH_LONG, GB.INFO);
                            return true;
                        case 0x02:
                            GB.toast(context, context.getString(R.string.ultrahuman_airplane_mode_on_charger), Toast.LENGTH_LONG, GB.ERROR);
                            return true;
                        case 0x03:
                            GB.toast(context, context.getString(R.string.ultrahuman_airplane_mode_too_full), Toast.LENGTH_LONG, GB.ERROR);
                            return true;
                    }
                    LOG.warn("set airplane mode - unknown error: {} ", StringUtils.bytesToHex(raw));
                    GB.toast(context, context.getString(R.string.ultrahuman_airplane_mode_unknown), Toast.LENGTH_LONG, GB.ERROR);
                    return false;

                case UltrahumanConstants.OPERATION_GET_FIRST_RECORDING_NR:
                    if (raw[1] == 0x00 || raw[2] == 0x01) {
                        FetchFrom = BLETypeConversions.toUint16(raw, 3);
                        if (FetchTo != -1) {
                            fetchRecordingActually();
                        }
                        return true;
                    } else {
                        String message = getContext().getString(R.string.ultrahuman_unhandled_error_response, raw[1], raw[0]);
                        GB.toast(getContext(), message, Toast.LENGTH_LONG, GB.ERROR);
                        fetchRecordedDataFinished();
                    }
                    return false;

                case UltrahumanConstants.OPERATION_GET_LAST_RECORDING_NR:
                    if (raw[1] == 0x00 && raw[2] == 0x01) {
                        FetchTo = BLETypeConversions.toUint16(raw, 3);
                        if (FetchFrom != -1) {
                            fetchRecordingActually();
                        }
                        return true;
                    } else {
                        String message = getContext().getString(R.string.ultrahuman_unhandled_error_response, raw[1], raw[0]);
                        GB.toast(getContext(), message, Toast.LENGTH_LONG, GB.ERROR);
                        fetchRecordedDataFinished();
                    }
                    return false;

                case UltrahumanConstants.OPERATION_SETTIME:
                    if (raw[1] != 0x00 || raw[2] != 0x01) {
                        String message = getContext().getString(R.string.ultrahuman_unhandled_error_response, raw[1], raw[0]);
                        GB.toast(getContext(), message, Toast.LENGTH_LONG, GB.ERROR);
                    }
                    return true;
                default:
                    LOG.error("unhandled BLE command response: {} ", StringUtils.bytesToHex(raw));
                    return false;
            }
        }

        LOG.error("unhandled characteristics {} - {} ", characteristicUUID, StringUtils.bytesToHex(raw));
        return false;
    }


    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private void fetchRecordingActually() {
        sendCommand("GetRecordingsCommand", new byte[]{UltrahumanConstants.OPERATION_GET_RECORDINGS, (byte) (FetchFrom & 0xFF), (byte) ((FetchFrom >> 8) & 0xFF)});
    }

    private boolean decodeRecordings(byte[] raw) {
        if (raw[1] != 0) {
            if ((raw[1] & 0xFF) == 0xEE) {
                LOG.warn("no historic data recorded");
            } else {
                String message = getContext().getString(R.string.ultrahuman_unhandled_error_response, raw[1], raw[0]);
                GB.toast(getContext(), message, Toast.LENGTH_LONG, GB.ERROR);
            }
            fetchRecordedDataFinished();
            return raw.length == 5;
        }

        boolean success = true;
        try (DBHandler db = GBApplication.acquireDB()) {
            GBDevice device = getDevice();
            DaoSession session = db.getDaoSession();

            Long userId = DBHelper.getUser(session).getId();
            Long deviceId = DBHelper.getDevice(device, session).getId();

            for (int record = 0; record < raw[2]; record++) {
                success &= decodeRecording(raw, 3 + record * 32, device, session, deviceId, userId, record == 0);
            }
        } catch (Exception e) {
            LOG.error("Error acquiring database for recording historic sample", e);
        }

        if (FetchCurrent >= FetchTo || raw[2] < 7) {
            fetchRecordedDataFinished();
        }
        return success;
    }

    private boolean decodeRecording(byte[] raw, int start, GBDevice device, DaoSession session, long deviceId, long userId, boolean updateProgress) {
        if (raw.length < start + 32) {
            LOG.error("length of history record is only from {} to {} instead of expected {}: {}", start, raw.length, start + 32, StringUtils.bytesToHex(raw));
            return false;
        }

        int timestampPPG = BLETypeConversions.toUint32(raw, start);
        int heartRate = BLETypeConversions.toUnsigned(raw[start + 4]);
        int HRV = BLETypeConversions.toUnsigned(raw[start + 5]);
        int spo2 = BLETypeConversions.toUnsigned(raw[start + 6]);
        int recordType = BLETypeConversions.toUnsigned(raw[start + 7]);

        int timestampTemp = BLETypeConversions.toUint32(raw, start + 8);
        float temperatureMax = Float.intBitsToFloat(BLETypeConversions.toUint32(raw, start + 12));
        float temperatureMin = Float.intBitsToFloat(BLETypeConversions.toUint32(raw, start + 16));

        int timestampActivity = BLETypeConversions.toUint32(raw, start + 20);
        int rawIntensity = BLETypeConversions.toUint16(raw[start + 24]);
        int steps = BLETypeConversions.toUint16(raw[start + 26]);
        int stress = (BLETypeConversions.toUint16(raw[start + 28]) * 100) / 255;

        int index = BLETypeConversions.toUint16(raw, start + 30);

        if (updateProgress) {
            int target = (FetchTo - FetchFrom);
            if (target != 0) {
                int progress = ((index - FetchFrom) * 100) / target;
                if (progress > 99) {
                    progress = 99;
                }
                GB.updateTransferNotification(null, Integer.toString(index), true, progress, getContext());
            }
        }

        FetchCurrent = Integer.max(FetchCurrent, index);

        LOG.debug("record[{}]: timeA={}, heartRate={}, HRV={}, spo2={}, recordType={}, timestampTemp={}, tempMax={}, tempMin={}," + "timeC={}, rawIntensity={}, steps={}, stress={}", index, timestampPPG, heartRate, HRV, spo2, recordType, timestampTemp, temperatureMax, temperatureMin, timestampActivity, rawIntensity, steps, stress);

        if (heartRate != 0) {
            GenericHeartRateSampleProvider provider = new GenericHeartRateSampleProvider(device, session);
            GenericHeartRateSample sample = new GenericHeartRateSample(timestampPPG * 1000L, deviceId, userId, heartRate);
            provider.addSample(sample);
        }

        if (HRV != 0) {
            GenericHrvValueSampleProvider provider = new GenericHrvValueSampleProvider(device, session);
            GenericHrvValueSample sample = new GenericHrvValueSample(timestampPPG * 1000L, deviceId, userId, HRV);
            provider.addSample(sample);
        }

        if (spo2 != 0) {
            GenericSpo2SampleProvider provider = new GenericSpo2SampleProvider(device, session);
            GenericSpo2Sample sample = new GenericSpo2Sample(timestampPPG * 1000L, deviceId, userId, spo2);
            provider.addSample(sample);
        }

        if (temperatureMax != 0.0f || temperatureMin != 0.0f) {
            float temperature = (temperatureMax + temperatureMin) / 2f;
            GenericTemperatureSampleProvider provider = new GenericTemperatureSampleProvider(device, session);
            GenericTemperatureSample sample = new GenericTemperatureSample(timestampTemp * 1000L, deviceId, userId, temperature, 0);
            provider.addSample(sample);
        }

        if (stress != 0) {
            GenericStressSampleProvider provider = new GenericStressSampleProvider(device, session);
            GenericStressSample sample = new GenericStressSample(timestampActivity * 1000L, deviceId, userId, stress);
            provider.addSample(sample);
        }

        if (rawIntensity != 0 || steps != 0 || heartRate != 0) {
            int hr = (heartRate == 0) ? -1 : heartRate;
            UltrahumanActivitySampleProvider provider = new UltrahumanActivitySampleProvider(device, session);
            UltrahumanActivitySample sample = new UltrahumanActivitySample(timestampActivity, deviceId, userId, recordType, hr, rawIntensity, steps);
            provider.addGBActivitySample(sample);
        }

        return true;
    }

    private boolean decodeDeviceState(byte[] raw) {
        boolean success = false;

        BatteryState batteryState = BatteryState.UNKNOWN;
        Integer batteryLevel = null;
        Integer deviceState = null;
        Integer deviceTemperature = null;

        try (DBHandler db = GBApplication.acquireDB()) {
            if (raw.length != 7) {
                LOG.warn("received Device State with unexpected length {}: {}", raw.length, StringUtils.bytesToHex(raw));
            } else {
                batteryLevel = 0xFF & raw[0];
                // decoding of 1..4 is unknown
                deviceState = 0xFF & raw[5];
                deviceTemperature = 0xFF & raw[6];

                switch (deviceState) {
                    case 0x00:
                        batteryState = BatteryState.BATTERY_NORMAL;
                        break;
                    case 0x03:
                        batteryState = BatteryState.BATTERY_CHARGING;
                        break;
                    default:
                        LOG.warn("DeviceState contains unhandled device state {}: {}", raw[5], StringUtils.bytesToHex(raw));
                }
            }

            GBDevice device = getDevice();
            DaoSession session = db.getDaoSession();

            long now = System.currentTimeMillis();
            Long userId = DBHelper.getUser(session).getId();
            Long deviceId = DBHelper.getDevice(device, session).getId();

            UltrahumanDeviceStateSample sample = new UltrahumanDeviceStateSample(now, deviceId, userId, raw, batteryLevel, deviceState, deviceTemperature);

            UltrahumanDeviceStateSampleProvider sampleProvider = new UltrahumanDeviceStateSampleProvider(device, session);
            sampleProvider.addSample(sample);
            success = true;
        } catch (Exception e) {
            LOG.error("Error acquiring database for recording device state sample", e);
            LOG.warn("device state sample: {}", StringUtils.bytesToHex(raw));
        }

        GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
        batteryEvent.level = (batteryLevel == null) ? -1 : batteryLevel;
        batteryEvent.state = batteryState;
        evaluateGBDeviceEvent(batteryEvent);

        return success;
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        LOG.debug("handleDeviceInfo: {}", info);
        GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
        versionCmd.fwVersion = info.getFirmwareRevision();
        versionCmd.fwVersion2 = info.getSerialNumber();
        versionCmd.hwVersion = info.getHardwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    private void fetchRecordedDataFinished() {
        GB.updateTransferNotification(null, "", false, 100, getContext());
        LOG.info("Sync finished!");
        getDevice().unsetBusyTask();
        getDevice().sendDeviceUpdateIntent(getContext());
        GB.signalActivityDataFinish(getDevice());
    }

    public void activateAirplaneMode() {
        sendCommand("ActivateAirplaneMode", new byte[]{UltrahumanConstants.OPERATION_ACTIVATE_AIRPLANE_MODE});
    }

    @Override
    public void onReset(int flags) {
        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) == GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) {
            sendCommand("onReset", new byte[]{UltrahumanConstants.OPERATION_RESET});
        }
    }

    @Override
    public void onSetTime() {
        TransactionBuilder builder = new TransactionBuilder("onSetTime");
        UltrahumanSetTimeAction action = new UltrahumanSetTimeAction(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_COMMAND));
        builder.add(action);

        if (isConnected()) {
            builder.queue(getQueue());
        } else {
            GB.toast(getContext(), R.string.devicestatus_disconnected, Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void sendCommand(String taskName, byte[] contents) {
        LOG.debug("sendCommand {} : {}", taskName, StringUtils.bytesToHex(contents));
        TransactionBuilder builder = new TransactionBuilder(taskName);
        builder.write(getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_COMMAND), contents);

        if (isConnected()) {
            builder.queue(getQueue());
        } else {
            GB.toast(getContext(), R.string.devicestatus_disconnected, Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private class UltrahumanBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
            if (device != null && !device.equals(getDevice())) {
                // this intent is for another device
                return;
            }

            String action = intent.getAction();
            if (UltrahumanConstants.ACTION_AIRPLANE_MODE.equals(action)) {
                activateAirplaneMode();
            }
        }
    }

    private class UltrahumanIntentListener implements IntentListener {
        @Override
        public void notify(Intent intent) {
            String action = intent.getAction();
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
        }
    }
}
