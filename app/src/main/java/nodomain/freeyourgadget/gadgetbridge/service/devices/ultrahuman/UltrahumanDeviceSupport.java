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

import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_ACTIVATE_AIRPLANE_MODE;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_BREATHING_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_BREATHING_STOP;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_CHECK_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_DISABLE_POWERSAVE;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_ENABLE_POWERSAVE;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_GET_FIRST_RECORDING_NR;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_GET_LAST_RECORDING_NR;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_GET_RECORDINGS;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_RESET;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_SETTIME;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.COMMAND;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.DATA;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.RESPONSE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.STATE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.TODO;

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

import java.util.Calendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
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
import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanExercise;
import nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanExerciseData;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ReadAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class UltrahumanDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(UltrahumanDeviceSupport.class);
    private final UltrahumanBroadcastReceiver CommandReceiver;
    private int FetchTo;
    private int FetchFrom;
    private int FetchCurrent;

    private int LatestBatteryLevel = -1;
    private int LatestExercise = -1;

    public UltrahumanDeviceSupport() {
        super(LOG);

        addSupportedService(UltrahumanConstants.UUID_SERVICE_REQUEST);
        addSupportedService(UltrahumanConstants.UUID_SERVICE_STATE);
        addSupportedService(UltrahumanConstants.UUID_SERVICE_DATA);
        addSupportedService(UltrahumanConstants.UUID_SERVICE_TODO);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);

        CommandReceiver = new UltrahumanBroadcastReceiver();

        DeviceInfoProfile<UltrahumanDeviceSupport> deviceProfile = new DeviceInfoProfile<>(this);
        deviceProfile.addListener(CommandReceiver);
        addSupportedProfile(deviceProfile);
    }

    @Override
    public void dispose() {
        if (CommandReceiver.Registered) {
            CommandReceiver.Registered = false;
            getContext().unregisterReceiver(CommandReceiver);
        }

        super.dispose();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // reset to avoid funny states for re-connect
        FetchTo = -1;
        FetchFrom = -1;
        FetchCurrent = -1;
        LatestBatteryLevel = -1;
        LatestExercise = -1;

        // required for DB
        if (getDevice().getFirmwareVersion() == null) {
            getDevice().setFirmwareVersion("N/A");
            getDevice().setFirmwareVersion2("N/A");
        }

        builder.setUpdateState(getDevice(), GBDevice.State.INITIALIZING, getContext());

        if (!CommandReceiver.Registered) {
            IntentFilter exported = new IntentFilter();
            exported.addAction(UltrahumanConstants.ACTION_AIRPLANE_MODE);
            exported.addAction(UltrahumanConstants.ACTION_CHANGE_EXERCISE);
            ContextCompat.registerReceiver(getContext(), CommandReceiver, exported, ContextCompat.RECEIVER_EXPORTED);

            CommandReceiver.Registered = true;
        }

        // trying to read non-existing characteristics sometimes causes odd BLE failures
        // so avoid DeviceInfoProfile.requestDeviceInfo
        builder.read(getCharacteristic(DeviceInfoProfile.UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING));
        builder.read(getCharacteristic(DeviceInfoProfile.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING));
        builder.read(getCharacteristic(DeviceInfoProfile.UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING));

        // sometimes the device is quite quick and other times it takes a while after
        // BLE connectivity has been established before the services work reliably
        builder.wait(48 * 2); //BluetoothGatt.onConnectionUpdated typically reports interval=48

        builder.add(new UHRead(STATE));

        builder.notify(getCharacteristic(RESPONSE.uuid), true);
        builder.notify(getCharacteristic(STATE.uuid), true);
        builder.notify(getCharacteristic(DATA.uuid), true);
        builder.notify(getCharacteristic(TODO.uuid), true);

        builder.add(new UHWrite(COMMAND, OPERATION_CHECK_DATA));

        boolean timeSync = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_TIME_SYNC, true);
        if (timeSync) {
            builder.add(new UHSetTime(COMMAND));
        }

        boolean powerSave = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_POWER_SAVING, true);
        builder.add(new UHWrite(COMMAND, powerSave ? OPERATION_ENABLE_POWERSAVE : OPERATION_DISABLE_POWERSAVE));

        builder.setUpdateState(getDevice(), GBDevice.State.INITIALIZED, getContext());

        return builder;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        String title = getContext().getString(R.string.busy_task_fetch_activity_data);
        GB.updateTransferNotification(title, "", true, 0, getContext());

        String task = getContext().getString(R.string.busy_task_fetch_activity_data);
        getDevice().setBusyTask(task);
        getDevice().sendDeviceUpdateIntent(getContext());

        TransactionBuilder builder = createTransactionBuilder("onFetchRecordedData");
        buildFetchRecordedData(builder);
        enqueue(builder);
    }

    private void buildFetchRecordedData(TransactionBuilder builder) {
        // fetch deltas while the device is connected
        FetchFrom = (FetchCurrent > 0) ? FetchCurrent : -1;
        FetchTo = -1;
        FetchCurrent = -1;

        if (FetchFrom <= 0) {
            builder.add(new UHWrite(COMMAND, OPERATION_GET_FIRST_RECORDING_NR));
        }
        builder.add(new UHWrite(COMMAND, OPERATION_GET_LAST_RECORDING_NR));
    }

    @Override
    public boolean onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic,
                                        final byte[] raw, final int status) {
        if (super.onCharacteristicRead(gatt, characteristic, raw, status)) {
            return true;
        }


        if (STATE.uuid.equals(characteristic.getUuid())) {
            LOG.debug("UH>>GB read {} {} {}", STATE, status, StringUtils.bytesToHex(raw));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                return decodeDeviceState(raw);
            }
        }

        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] raw) {
        if (super.onCharacteristicChanged(gatt, characteristic, raw)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();

        if (STATE.uuid.equals(characteristicUUID)) {
            LOG.debug("UH>>GB changed STATE {}", StringUtils.bytesToHex(raw));
            return decodeDeviceState(raw);
        } else if (RESPONSE.uuid.equals(characteristicUUID)) {
            LOG.debug("UH>>GB changed RESPONSE {}", StringUtils.bytesToHex(raw));
            if (raw.length < 3) {
                LOG.error("UH>>GB !! RESPONSE too short: {}", StringUtils.bytesToHex(raw));
                return false;
            }

            final byte op = raw[0];
            final byte success = raw[1];
            final byte result = raw[2];
            // ignore checksums for now - algorithm is unknown
            //byte chk1 = raw[raw.length-1];
            //byte chk2 = raw[raw.length-2];

            Context context = getContext();

            switch (op) {
                case OPERATION_GET_RECORDINGS:
                    return decodeRecordings(raw);
                case OPERATION_CHECK_DATA:
                case OPERATION_BREATHING_START:
                case OPERATION_BREATHING_STOP:
                    if (success == 0x00 && result == 0x01 && raw.length >= 5) {
                        LatestExercise = raw[3];
                        publishExerciseData();
                        return true;
                    }
                    break;
                case OPERATION_ACTIVATE_AIRPLANE_MODE:
                    switch (result) {
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

                    String airplaneMessage = getContext().getString(R.string.ultrahuman_airplane_mode_unknown, result);
                    GB.toast(context, airplaneMessage, Toast.LENGTH_LONG, GB.ERROR);
                    return false;

                case OPERATION_GET_FIRST_RECORDING_NR:
                    if (success == 0x00 && result == 0x01) {
                        FetchFrom = BLETypeConversions.toUint16(raw, 3);
                        if (FetchTo != -1) {
                            fetchRecordingActually();
                        }
                        return true;
                    }
                    fetchRecordedDataFinished();
                    break;

                case OPERATION_GET_LAST_RECORDING_NR:
                    if (success == 0x00 && result == 0x01) {
                        FetchTo = BLETypeConversions.toUint16(raw, 3);
                        if (FetchFrom != -1) {
                            fetchRecordingActually();
                        }
                        return true;
                    }
                    fetchRecordedDataFinished();
                    break;

                case OPERATION_DISABLE_POWERSAVE:
                case OPERATION_ENABLE_POWERSAVE:
                case OPERATION_SETTIME:
                    if (success == 0x00 && result == 0x01) {
                        return true;
                    }
                    break;

                default:
                    LOG.warn("UH>>GB  !! changed {} A {}", RESPONSE, StringUtils.bytesToHex(raw));
                    String message = getContext().getString(R.string.ultrahuman_unhandled_operation_response, StringUtils.bytesToHex(raw));
                    GB.toast(getContext(), message, Toast.LENGTH_LONG, GB.ERROR);
                    return false;
            }

            LOG.warn("UH>>GB !! changed {} B {}", RESPONSE, StringUtils.bytesToHex(raw));
            String message = getContext().getString(R.string.ultrahuman_unhandled_error_response, op, success, result);
            GB.toast(getContext(), message, Toast.LENGTH_LONG, GB.ERROR);
            return false;
        } else if (DATA.uuid.equals(characteristicUUID)) {
            if (raw[0] == 0x72 && raw.length >= 27) {
                LOG.debug("UH>>GB changed {} {}", DATA, StringUtils.bytesToHex(raw));
                decodeBreathing(raw);
                return true;
            }
            LOG.warn("UH>>GB !! changed {} 1 {}", DATA, StringUtils.bytesToHex(raw));
            return true;
        } else if (TODO.uuid.equals(characteristicUUID)) {
            LOG.warn("UH>>GB !! changed {} {}", TODO, StringUtils.bytesToHex(raw));
            return true;
        }

        LOG.warn("UH>>GB !! unhandled characteristic {} - {} ", characteristicUUID, StringUtils.bytesToHex(raw));
        return false;
    }

    private void decodeBreathing(byte[] raw) {
        char[] undecoded = StringUtils.bytesToHex(raw).toCharArray();

        UltrahumanExerciseData data = new UltrahumanExerciseData(LatestBatteryLevel, LatestExercise);

        data.Timestamp = BLETypeConversions.toUint32(raw, 3);
        undecoded[3 * 2] = undecoded[3 * 2 + 1] = '.';
        undecoded[4 * 2] = undecoded[4 * 2 + 1] = '.';
        undecoded[5 * 2] = undecoded[5 * 2 + 1] = '.';
        undecoded[6 * 2] = undecoded[6 * 2 + 1] = '.';

        data.HR = BLETypeConversions.toUnsigned(raw, 7);
        undecoded[7 * 2] = undecoded[7 * 2 + 1] = '.';

        data.HRV = BLETypeConversions.toUnsigned(raw, 9);
        undecoded[9 * 2] = undecoded[9 * 2 + 1] = '.';

        data.Temperature = Float.intBitsToFloat(BLETypeConversions.toUint32(raw, 11));
        undecoded[11 * 2] = undecoded[11 * 2 + 1] = '.';
        undecoded[12 * 2] = undecoded[12 * 2 + 1] = '.';
        undecoded[13 * 2] = undecoded[13 * 2 + 1] = '.';
        undecoded[14 * 2] = undecoded[14 * 2 + 1] = '.';

        // type
        undecoded[0] = undecoded[1] = '.';

        // payload length
        undecoded[2] = undecoded[3] = '.';
        undecoded[4] = undecoded[5] = '.';

        // check sum
        undecoded[undecoded.length - 4] = '.';
        undecoded[undecoded.length - 3] = '.';
        undecoded[undecoded.length - 2] = '.';
        undecoded[undecoded.length - 1] = '.';

        LOG.debug("UH>>GB decode DAT72 {}", new String(undecoded));
        publishExerciseData(data);
    }

    private void publishExerciseData() {
        if (LatestExercise > -1 && LatestBatteryLevel > -1) {
            UltrahumanExerciseData data = new UltrahumanExerciseData(LatestBatteryLevel, LatestExercise);
            publishExerciseData(data);
        }
    }

    private void publishExerciseData(UltrahumanExerciseData data) {
        final Intent intent = new Intent(UltrahumanConstants.ACTION_EXERCISE_UPDATE);
        intent.setPackage(BuildConfig.APPLICATION_ID);

        String address = getDevice().getAddress();
        intent.putExtra(UltrahumanConstants.EXTRA_ADDRESS, address);
        intent.putExtra(UltrahumanConstants.EXTRA_EXERCISE, data);

        final Context context = getContext();
        context.sendBroadcast(intent);
    }

    @Override
    public void onSendConfiguration(String config) {
        if (DeviceSettingsPreferenceConst.PREF_TIME_SYNC.equals(config)) {
            onSetTime();
        } else if (DeviceSettingsPreferenceConst.PREF_POWER_SAVING.equals(config)) {
            boolean powerSave = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_POWER_SAVING, true);
            sendCommand("onSetPowerSaveMode", powerSave ? OPERATION_ENABLE_POWERSAVE : OPERATION_DISABLE_POWERSAVE);
        } else {
            super.onSendConfiguration(config);
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private void fetchRecordingActually() {
        // ID overflow
        if (FetchFrom > FetchTo) {
            FetchFrom = 0;
        }
        sendCommand("fetchRecordingActually", OPERATION_GET_RECORDINGS, (byte) (FetchFrom & 0xFF), (byte) ((FetchFrom >> 8) & 0xFF));
    }

    private boolean decodeRecordings(byte[] raw) {
        if (raw[1] != 0) {
            if ((raw[1] & 0xFF) == 0xEE) {
                LOG.warn("!! no historic data recorded");
            } else {
                String message = getContext().getString(R.string.ultrahuman_unhandled_error_response, raw[0], raw[1], raw[2]);
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
        int rawIntensity = BLETypeConversions.toUnsigned(raw[start + 24]);
        int steps = BLETypeConversions.toUint16(raw, start + 26);
        int stress = (BLETypeConversions.toUnsigned(raw[start + 28]) * 100) / 255;

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
                LOG.warn("!! received Device State with unexpected length {}: {}", raw.length, StringUtils.bytesToHex(raw));
            } else {
                char[] undecoded = StringUtils.bytesToHex(raw).toCharArray();

                batteryLevel = 0xFF & raw[0];
                undecoded[0] = undecoded[1] = '.';

                // decoding of 1..4 is unknown

                deviceState = 0xFF & raw[5];
                undecoded[5 * 2] = undecoded[5 * 2 + 1] = '.';

                deviceTemperature = 0xFF & raw[6];
                undecoded[6 * 2] = undecoded[6 * 2 + 1] = '.';

                LOG.debug("UH>>GB decode STATE {}", new String(undecoded));

                switch (deviceState) {
                    case 0x00:
                        batteryState = BatteryState.BATTERY_NORMAL;
                        break;
                    case 0x03:
                        batteryState = (batteryLevel > 99) ? BatteryState.BATTERY_CHARGING_FULL : BatteryState.BATTERY_CHARGING;
                        break;
                    default:
                        LOG.warn("!! DeviceState contains unhandled device state {}: {}", raw[5], StringUtils.bytesToHex(raw));
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

        if (batteryEvent.level != LatestBatteryLevel) {
            LatestBatteryLevel = batteryEvent.level;
            publishExerciseData();
        }

        return success;
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
        versionCmd.fwVersion = info.getFirmwareRevision();
        versionCmd.fwVersion2 = info.getSerialNumber();
        versionCmd.hwVersion = info.getHardwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    private void fetchRecordedDataFinished() {
        GB.updateTransferNotification(null, "", false, 100, getContext());
        getDevice().unsetBusyTask();
        getDevice().sendDeviceUpdateIntent(getContext());
        GB.signalActivityDataFinish(getDevice());
    }

    @Override
    public void onReset(int flags) {
        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) == GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) {
            sendCommand("onReset", OPERATION_RESET);
        }
    }

    @Override
    public void onSetTime() {
        boolean timeSync = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_TIME_SYNC, true);
        if (timeSync) {
            TransactionBuilder builder = createTransactionBuilder("onSetTime");
            builder.add(new UHSetTime(COMMAND));

            enqueue(builder);
        }
    }

    private void sendCommand(String taskName, byte... contents) {
        TransactionBuilder builder = createTransactionBuilder(taskName);
        builder.add(new UHWrite(COMMAND, contents));

        enqueue(builder);
    }

    private void changeExercise(byte exercise) {
        TransactionBuilder builder = createTransactionBuilder("changeExercise");
        if (exercise != UltrahumanExercise.CHECK.Code) {
            builder.add(new UHWrite(COMMAND, exercise));
        }
        builder.add(new UHWrite(COMMAND, OPERATION_CHECK_DATA));

        enqueue(builder);
    }

    private void enqueue(final TransactionBuilder builder) {
        if (isConnected()) {
            builder.queue(getQueue());
        } else {
            GB.toast(getContext(), R.string.devicestatus_disconnected, Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private BluetoothGattCharacteristic resolve(UltrahumanCharacteristic chara) {
        switch (chara) {
            case DATA:
                return getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_DATA);
            case STATE:
                return getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_STATE);
            case COMMAND:
                return getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_COMMAND);
            case RESPONSE:
                return getCharacteristic(UltrahumanConstants.UUID_CHARACTERISTIC_RESPONSE);
        }
        LOG.error("resolve {} is unknown", chara);
        return null;
    }

    private class UltrahumanBroadcastReceiver extends BroadcastReceiver implements IntentListener {
        boolean Registered;

        @Override
        public void onReceive(Context context, Intent intent) {
            notify(intent);
        }

        @Override
        public void notify(Intent intent) {
            final String address = intent.getStringExtra(UltrahumanConstants.EXTRA_ADDRESS);

            if (address != null && !address.isEmpty() && !address.equalsIgnoreCase(getDevice().getAddress())) {
                // this intent is for another device
                return;
            }

            final String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case UltrahumanConstants.ACTION_AIRPLANE_MODE:
                    sendCommand("activateAirplaneMode", OPERATION_ACTIVATE_AIRPLANE_MODE);
                    return;
                case UltrahumanConstants.ACTION_CHANGE_EXERCISE:
                    final byte exercise = intent.getByteExtra(UltrahumanConstants.EXTRA_EXERCISE, UltrahumanExercise.CHECK.Code);
                    changeExercise(exercise);
                    return;
                default:
                    if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                        handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
                        return;
                    }
            }
        }
    }

    ///  simplify debugging and tracing
    private class UHWrite extends WriteAction {
        private final UltrahumanCharacteristic Characteristic;

        public UHWrite(UltrahumanCharacteristic chara, byte... value) {
            super(resolve(chara), value);
            Characteristic = chara;
        }

        @Override
        protected boolean writeValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            LOG.debug("GB>>UH write {} {}", Characteristic, StringUtils.bytesToHex(value));
            return super.writeValue(gatt, characteristic, value);
        }
    }

    ///  simplify debugging and tracing
    private class UHRead extends ReadAction {
        private final UltrahumanCharacteristic Characteristic;

        public UHRead(UltrahumanCharacteristic chara) {
            super(resolve(chara));
            Characteristic = chara;
        }

        @Override
        public boolean run(BluetoothGatt gatt) {
            LOG.debug("GB>>UH read {}", Characteristic);
            return super.run(gatt);
        }
    }

    /// calculate date/time on the fly to avoid setting an outdated value
    private class UHSetTime extends UHWrite {
        UHSetTime(UltrahumanCharacteristic chara) {
            super(chara);
        }

        @Override
        protected boolean writeValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            final Calendar calendar = DateTimeUtils.getCalendarUTC();
            final long millis = calendar.getTimeInMillis();
            final long epoc = Math.round(millis / 1000.0d);

            byte[] command = new byte[]{
                    OPERATION_SETTIME,
                    (byte) (epoc & 0xff),
                    (byte) ((epoc >> 8) & 0xff),
                    (byte) ((epoc >> 16) & 0xff),
                    (byte) ((epoc >> 24) & 0xff)
            };
            return super.writeValue(gatt, characteristic, command);
        }
    }
}
