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
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_DISABLE_SPO2;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_ENABLE_SPO2;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_EXERCISE_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_EXERCISE_STOP;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_GET_FIRST_RECORDING_NR;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_GET_LAST_RECORDING_NR;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_GET_RECORDINGS;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_RESET;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.OPERATION_SETTIME;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.UUID_COMMAND;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.UUID_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.UUID_RESPONSE;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.UUID_STATE;
import static nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.UltrahumanConstants.UUID_TODO;
import static nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State.INITIALIZED;
import static nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State.INITIALIZING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.COMMAND;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.DATA;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.RESPONSE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.STATE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.ultrahuman.UltrahumanCharacteristic.TODO;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BleNamesResolver;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.notifications.GBProgressNotification;

public class UltrahumanDeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final String LOG_READ_FAILED = "UH>>GB failed read {} {}";
    private static final String LOG_UNDECODED = "UH>>GB undecoded {} {}";
    private static final String LOG_UNHANDLED = "UH>>GB unhandled {} {}";

    private static final Logger LOG = LoggerFactory.getLogger(UltrahumanDeviceSupport.class);

    private UltrahumanReceiver CommandReceiver;
    private GBProgressNotification ProgressNotification;

    private int FetchCurrent;
    private int FetchFrom;
    private int FetchTo;

    private int LatestBatteryLevel;
    private BatteryState LatestBatteryState;
    private int LatestExercise;

    private String FirmwareVersion = "N/A";
    private String FirmwareVersion2 = "N/A";

    public UltrahumanDeviceSupport() {
        super(LOG);
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            if (CommandReceiver.Registered) {
                CommandReceiver.Registered = false;
                getContext().unregisterReceiver(CommandReceiver);
            }

            super.dispose();
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        final UUID uuid = characteristic.getUuid();

        if (STATE.uuid.equals(uuid)) {
            return decodeSTATE(value);
        } else if (RESPONSE.uuid.equals(uuid)) {
            return decodeRESPONSE(value);
        } else if (DATA.uuid.equals(uuid)) {
            return decodeDATA(value);
        } else if (TODO.uuid.equals(uuid)) {
            return decodeTODO(value);
        }

        LOG.info("UH>>GB unhandled onCharacteristicChanged {} {}", uuid, GB.hexdump(value));
        return false;
    }

    @Override
    public boolean onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, byte[] value, final int status) {
        if (super.onCharacteristicRead(gatt, characteristic, value, status)) {
            return true;
        }

        final UUID uuid = characteristic.getUuid();

        if (STATE.uuid.equals(uuid)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                return decodeSTATE(value);
            }
            LOG.warn(LOG_READ_FAILED, STATE, status);
            return true;
        } else if (DATA.uuid.equals(characteristic.getUuid())) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                return decodeDATA(value);
            }
            LOG.warn(LOG_READ_FAILED, DATA, status);
            return true;
        } else if (TODO.uuid.equals(characteristic.getUuid())) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                return decodeTODO(value);
            }
            LOG.warn(LOG_READ_FAILED, TODO, status);
            return true;
        }

        LOG.warn("UH>>GB unhandled onCharacteristicRead {} {}",
                BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()),
                GB.hexdump(value));
        return false;
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        if (!enable || LatestExercise != OPERATION_EXERCISE_START) {
            sendCommand("onEnableRealtimeHeartRateMeasurement", enable ? OPERATION_EXERCISE_START : OPERATION_EXERCISE_STOP);
        }
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        if (getDevice().isBusy()) {
            return;
        }

        TransactionBuilder builder = createTransactionBuilder("onFetchRecordedData");
        builder.setBusyTask(R.string.busy_task_fetch_activity_data);

        // fetch deltas while the device is connected
        FetchFrom = (FetchCurrent > 0) ? FetchCurrent : -1;
        FetchTo = -1;
        FetchCurrent = -1;

        if (FetchFrom <= 0) {
            builder.write(UUID_COMMAND, OPERATION_GET_FIRST_RECORDING_NR);
        }
        builder.write(UUID_COMMAND, OPERATION_GET_LAST_RECORDING_NR);

        enqueue(builder);
    }

    protected BluetoothGattCharacteristic getCharacteristic(UltrahumanCharacteristic characteristic) {
        return getCharacteristic(characteristic.uuid);
    }

    @Override
    public void onReset(int flags) {
        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) == GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) {
            TransactionBuilder builder = createTransactionBuilder("onReset");
            builder.write(UUID_COMMAND, OPERATION_RESET);
            builder.run(this::disconnect);
            try {
                builder.queueConnected();
            } catch (IOException e) {
                LOG.error("onReset failed {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void onSendConfiguration(@NonNls String config) {
        super.onSendConfiguration(config);
        TransactionBuilder builder = createTransactionBuilder("onSendConfiguration");
        if (DeviceSettingsPreferenceConst.PREF_TIME_SYNC.equals(config)) {
            onSetTime(builder);
        } else if (DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING.equals(config)) {
            onSetSpo2(builder);
        } else {
            return;
        }
        enqueue(builder);
    }

    @Override
    public void onSetTime() {
        TransactionBuilder builder = createTransactionBuilder("onSetTime");
        onSetTime(builder);
        enqueue(builder);
    }

    private void onSetTime(TransactionBuilder builder) {
        boolean timeSync = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_TIME_SYNC, true);
        if (timeSync) {
            builder.add(new UltrahumanWriteTime(getCharacteristic(COMMAND)));
        }
    }

    private void onSetSpo2(TransactionBuilder builder) {
        boolean spo2 = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING, false);
        builder.write(UUID_COMMAND, spo2 ? OPERATION_ENABLE_SPO2 : OPERATION_DISABLE_SPO2);
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        // a completed "this" is required so do the initialization here and not in the constructor
        if (CommandReceiver == null) {
            addSupportedService(UltrahumanConstants.UUID_SERVICE_REQUEST);
            addSupportedService(UltrahumanConstants.UUID_SERVICE_STATE);
            addSupportedService(UltrahumanConstants.UUID_SERVICE_DATA);
            addSupportedService(UltrahumanConstants.UUID_SERVICE_TODO);
            addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);

            CommandReceiver = new UltrahumanReceiver();

            DeviceInfoProfile<UltrahumanDeviceSupport> deviceProfile = new DeviceInfoProfile<>(this);
            deviceProfile.addListener(CommandReceiver);
            addSupportedProfile(deviceProfile);
        }

        super.setContext(gbDevice, btAdapter, context);
        ProgressNotification = new GBProgressNotification(getContext(), GB.NOTIFICATION_CHANNEL_ID_TRANSFER);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // reset here to avoid funny states for re-connect
        FetchTo = -1;
        FetchFrom = -1;
        FetchCurrent = -1;
        LatestBatteryLevel = -1;
        LatestBatteryState = BatteryState.NO_BATTERY;
        LatestExercise = -1;

        // required for DB
        GBDevice device = getDevice();
        if (device.getFirmwareVersion() == null) {
            device.setFirmwareVersion(FirmwareVersion);
            device.setFirmwareVersion2(FirmwareVersion2);
        }

        if (!CommandReceiver.Registered) {
            IntentFilter exported = new IntentFilter();
            exported.addAction(UltrahumanConstants.ACTION_AIRPLANE_MODE);
            exported.addAction(UltrahumanConstants.ACTION_CHANGE_EXERCISE);
            ContextCompat.registerReceiver(getContext(), CommandReceiver, exported, ContextCompat.RECEIVER_EXPORTED);

            CommandReceiver.Registered = true;
        }

        builder.setDeviceState(INITIALIZING);

        // trying to read non-existing characteristics sometimes causes odd BLE failures
        // so avoid DeviceInfoProfile.requestDeviceInfo
        builder.read(DeviceInfoProfile.UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING);
        builder.read(DeviceInfoProfile.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING);
        builder.read(DeviceInfoProfile.UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING);

        builder.read(UUID_STATE);

        builder.notify(UUID_RESPONSE, true);
        builder.notify(UUID_STATE, true);
        builder.notify(UUID_DATA, true);
        builder.notify(UUID_TODO, true);

        builder.write(UUID_COMMAND, OPERATION_CHECK_DATA);

        onSetTime(builder);
        onSetSpo2(builder);

        builder.setDeviceState(INITIALIZED);

        return builder;
    }

    private boolean decodeDATA(byte[] raw) {
        if (raw.length == 27 && raw[0] == OPERATION_BREATHING_START) {
            decodeDATA_Breathing(raw);
            return true;
        } else if (raw.length == 11 && raw[0] == OPERATION_EXERCISE_START) {
            decodeDATA_Exercise(raw);
            return true;
        }

        LOG.warn(LOG_UNHANDLED, DATA, GB.hexdump(raw));
        return false;
    }

    private void decodeDATA_Breathing(byte[] raw) {
        UltrahumanExerciseData data = new UltrahumanExerciseData(LatestBatteryLevel, LatestExercise, raw[0]);

        data.Timestamp = BLETypeConversions.toUint32(raw, 3);
        data.HR = BLETypeConversions.toUnsigned(raw, 7);
        data.HRV = BLETypeConversions.toUnsigned(raw, 9);
        data.Temperature = Float.intBitsToFloat(BLETypeConversions.toUint32(raw, 11));
        data.Mystery = Integer.toString(BLETypeConversions.toUint16(raw, 23));

        if (BuildConfig.DEBUG) {
            final char[] undecoded = GB.hexdump(raw).toCharArray();

            // keep type ([0], [1]) for log message

            // payload length
            undecoded[1 * 2] = undecoded[1 * 2 + 1] = '.';
            undecoded[2 * 2] = undecoded[2 * 2 + 2] = '.';

            // time stamp
            undecoded[3 * 2] = undecoded[3 * 2 + 1] = '.';
            undecoded[4 * 2] = undecoded[4 * 2 + 1] = '.';
            undecoded[5 * 2] = undecoded[5 * 2 + 1] = '.';
            undecoded[6 * 2] = undecoded[6 * 2 + 1] = '.';

            // HR
            undecoded[7 * 2] = undecoded[7 * 2 + 1] = '.';

            // HRV
            undecoded[9 * 2] = undecoded[9 * 2 + 1] = '.';

            // temperature
            undecoded[11 * 2] = undecoded[11 * 2 + 1] = '.';
            undecoded[12 * 2] = undecoded[12 * 2 + 1] = '.';
            undecoded[13 * 2] = undecoded[13 * 2 + 1] = '.';
            undecoded[14 * 2] = undecoded[14 * 2 + 1] = '.';

            // check sum
            undecoded[undecoded.length - 4] = '.';
            undecoded[undecoded.length - 3] = '.';
            undecoded[undecoded.length - 2] = '.';
            undecoded[undecoded.length - 1] = '.';

            LOG.debug(LOG_UNDECODED, DATA, new String(undecoded));
        }

        publishExerciseData(data);
    }

    private void decodeDATA_Exercise(byte[] raw) {
        UltrahumanExerciseData data = new UltrahumanExerciseData(LatestBatteryLevel, LatestExercise, raw[0]);

        data.Timestamp = BLETypeConversions.toUint32(raw, 3);
        data.HR = BLETypeConversions.toUnsigned(raw, 7);
        data.Mystery = Integer.toString(BLETypeConversions.toUnsigned(raw, 8));
        // no other payload fields

        if (BuildConfig.DEBUG) {
            final char[] undecoded = GB.hexdump(raw).toCharArray();

            // keep type ([0], [1]) for log message

            // payload length
            undecoded[2] = undecoded[3] = '.';
            undecoded[4] = undecoded[5] = '.';

            // time stamp
            undecoded[3 * 2] = undecoded[3 * 2 + 1] = '.';
            undecoded[4 * 2] = undecoded[4 * 2 + 1] = '.';
            undecoded[5 * 2] = undecoded[5 * 2 + 1] = '.';
            undecoded[6 * 2] = undecoded[6 * 2 + 1] = '.';

            // HR
            undecoded[7 * 2] = undecoded[7 * 2 + 1] = '.';

            // check sum
            undecoded[undecoded.length - 4] = '.';
            undecoded[undecoded.length - 3] = '.';
            undecoded[undecoded.length - 2] = '.';
            undecoded[undecoded.length - 1] = '.';

            LOG.debug(LOG_UNDECODED, DATA, new String(undecoded));
        }

        publishExerciseData(data);
    }

    private boolean decodeRESPONSE(final byte[] raw) {
        if (raw.length < 3) {
            LOG.error("UH>>GB {} too short: {}", RESPONSE, GB.hexdump(raw));
            return false;
        }

        final byte op = raw[0];
        final byte success = raw[1];
        final byte result = raw[2];
        // ignore checksums for now - algorithm is unknown
        //byte chk1 = raw[raw.length-1];
        //byte chk2 = raw[raw.length-2];

        switch (op) {
            case OPERATION_GET_RECORDINGS:
                return decodeRESPONSE_RecordedData(raw);

            case OPERATION_BREATHING_START:
                if (success == (byte) 0xEE && result == 0x01) {
                    LOG.info("UH>>GB {} breathing is already started", RESPONSE);
                    return true;
                }
                // fall through
            case OPERATION_EXERCISE_START:
            case OPERATION_CHECK_DATA:
            case OPERATION_BREATHING_STOP:
                if (success == 0x00 && result == 0x01 && raw.length == 6) {
                    LatestExercise = raw[3];
                    publishExerciseData();
                    return true;
                }
                break;
            case OPERATION_EXERCISE_STOP:
                if (success == (byte) 0xFF && result == 0x01) {
                    LOG.info("UH>>GB {} exercise is already stopped", RESPONSE);
                    return true;
                }
                if (success == 0x00 && result == 0x01 && raw.length == 6) {
                    LatestExercise = raw[3];
                    publishExerciseData();
                    return true;
                }
                break;
            case OPERATION_ACTIVATE_AIRPLANE_MODE:
                switch (result) {
                    case 0x01:
                        uiInfo(R.string.ultrahuman_airplane_mode_activated);
                        return true;
                    case 0x02:
                        uiError(getContext().getString(R.string.ultrahuman_airplane_mode_on_charger));
                        return true;
                    case 0x03:
                        uiError(getContext().getString(R.string.ultrahuman_airplane_mode_too_full));
                        return true;
                }

                uiError(getContext().getString(R.string.ultrahuman_airplane_mode_unknown, result));
                return false;

            case OPERATION_GET_FIRST_RECORDING_NR:
                if (success == 0x00 && result == 0x01) {
                    FetchFrom = BLETypeConversions.toUint16(raw, 3);
                    if (FetchTo != -1) {
                        fetchRecordedDataActually();
                    }
                    return true;
                }
                fetchRecordedDataFinished();
                break;

            case OPERATION_GET_LAST_RECORDING_NR:
                if (success == 0x00 && result == 0x01) {
                    FetchTo = BLETypeConversions.toUint16(raw, 3);
                    if (FetchFrom != -1) {
                        fetchRecordedDataActually();
                    }
                    return true;
                }
                fetchRecordedDataFinished();
                break;

            case OPERATION_DISABLE_SPO2:
            case OPERATION_ENABLE_SPO2:
            case OPERATION_SETTIME:
                if (success == 0x00 && result == 0x01) {
                    return true;
                }
                break;

            default:
                LOG.warn(LOG_UNHANDLED, RESPONSE, GB.hexdump(raw));
                uiError(getContext().getString(R.string.ultrahuman_unhandled_operation_response, GB.hexdump(raw)));
                return false;
        }

        LOG.warn(LOG_UNHANDLED, RESPONSE, GB.hexdump(raw));
        uiError(getContext().getString(R.string.ultrahuman_unhandled_error_response, op, success, result));
        return false;
    }

    private boolean decodeRESPONSE_RecordedData(final byte[] raw) {
        if (raw[1] != 0) {
            if ((raw[1] & 0xFF) == 0xEE) {
                LOG.info("UH>>GB no historic data recorded");
            } else {
                uiError(getContext().getString(R.string.ultrahuman_unhandled_error_response, raw[0], raw[1], raw[2]));
            }
            fetchRecordedDataFinished();
            return raw.length == 5;
        }

        final int entries = raw[2];

        boolean success = true;
        try (DBHandler db = GBApplication.acquireDB()) {
            GBDevice device = getDevice();
            DaoSession session = db.getDaoSession();

            Long userId = DBHelper.getUser(session).getId();
            Long deviceId = DBHelper.getDevice(device, session).getId();

            for (int entry = 0; entry < entries; entry++) {
                //noinspection NonShortCircuitBooleanExpression
                success &= decodeRESPONSE_RecordedDataEntry(raw, 3 + entry * 32, device, session, deviceId, userId, entry == 0);
            }
        } catch (Exception e) {
            LOG.error("UH>>GB error acquiring database for decodeRESPONSE_RecordedData {}", e.getMessage(), e);
        }

        if (FetchCurrent >= FetchTo || entries < 7) {
            fetchRecordedDataFinished();
        }

        return success;
    }

    private boolean decodeRESPONSE_RecordedDataEntry(byte[] raw, int start, GBDevice device, DaoSession session, long deviceId, long userId, boolean updateProgress) {
        if (raw.length < start + 32) {
            LOG.error("UH>>GB length of history record is only from {} to {} instead of expected {}: {}", start, raw.length, start + 32, GB.hexdump(raw));
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
            ProgressNotification.setTotalProgress(index - FetchFrom);
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
            float temperature = (temperatureMax + temperatureMin) / 2.0f;
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

    private boolean decodeSTATE(byte[] raw) {
        boolean success = false;

        BatteryState batteryState = BatteryState.UNKNOWN;
        Integer batteryLevel = null;
        Integer deviceState = null;
        Integer deviceTemperature = null;

        try (DBHandler db = GBApplication.acquireDB()) {
            if (raw.length != 7) {
                LOG.warn("UH>>GB received {} with unexpected length {}: {}", STATE, raw.length, GB.hexdump(raw));
            } else {
                batteryLevel = 0xFF & raw[0];
                deviceState = 0xFF & raw[5];
                deviceTemperature = 0xFF & raw[6];

                if (BuildConfig.DEBUG) {
                    char[] undecoded = GB.hexdump(raw).toCharArray();

                    // battery
                    undecoded[0] = undecoded[1] = '.';

                    // device state
                    undecoded[5 * 2] = undecoded[5 * 2 + 1] = '.';

                    // device temperature
                    undecoded[6 * 2] = undecoded[6 * 2 + 1] = '.';

                    LOG.debug(LOG_UNDECODED, STATE, new String(undecoded));
                }

                switch (deviceState) {
                    case 0x00:
                        batteryState = BatteryState.BATTERY_NORMAL;
                        break;
                    case 0x03:
                        batteryState = (batteryLevel > 99) ? BatteryState.BATTERY_CHARGING_FULL : BatteryState.BATTERY_CHARGING;
                        break;
                    default:
                        LOG.warn("UH>>GB {} contains unhandled device state {}: {}", STATE, raw[5], GB.hexdump(raw));
                }

                LOG.debug("device: state={} charge={} temperature={}", batteryState, batteryLevel, deviceTemperature);
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
            LOG.error("Error acquiring database for recording device state sample {}", e.getMessage(), e);
            LOG.warn("device state sample: {}", GB.hexdump(raw));
        }

        GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
        batteryEvent.level = (batteryLevel == null) ? -1 : batteryLevel;
        batteryEvent.state = batteryState;

        boolean batteryChanged = false;

        if (batteryEvent.level != LatestBatteryLevel) {
            LatestBatteryLevel = batteryEvent.level;
            batteryChanged = true;
        }

        if (batteryEvent.state != LatestBatteryState) {
            LatestBatteryState = batteryState;
            batteryChanged = true;
        }

        if (batteryChanged) {
            // avoid spamming if no relevant details were changed
            evaluateGBDeviceEvent(batteryEvent);
            publishExerciseData();
        }

        return success;
    }

    private boolean decodeTODO(byte[] raw) {
        LOG.warn(LOG_UNHANDLED, TODO, GB.hexdump(raw));
        return false;
    }

    private void fetchRecordedDataActually() {
        // ID overflow
        if (FetchFrom > FetchTo) {
            FetchFrom = 0;
        }
        ProgressNotification.start(R.string.busy_task_fetch_activity_data, 0, FetchTo - FetchFrom);
        sendCommand("fetchRecordedDataActually", OPERATION_GET_RECORDINGS, (byte) (FetchFrom & 0xFF), (byte) ((FetchFrom >> 8) & 0xFF));
    }

    void enqueue(@NonNull final TransactionBuilder builder) {
        if (isConnected()) {
            builder.queue();
        } else {
            uiError(getContext().getString(R.string.devicestatus_disconnected));
        }
    }

    private void fetchRecordedDataFinished() {
        ProgressNotification.finish();
        GBDevice device = getDevice();
        if (device.isBusy()) {
            device.unsetBusyTask();
            device.sendDeviceUpdateIntent(getContext());
        }
        GB.signalActivityDataFinish(device);
    }

    void handleDeviceInfo(@NonNull DeviceInfo deviceInfo) {
        GBDeviceEventVersionInfo info = new GBDeviceEventVersionInfo();
        info.fwVersion = deviceInfo.getFirmwareRevision();
        info.fwVersion2 = deviceInfo.getSerialNumber();
        info.hwVersion = deviceInfo.getHardwareRevision();
        if (info.fwVersion != null && info.fwVersion2 != null && info.hwVersion != null) {
            FirmwareVersion = info.fwVersion;
            FirmwareVersion2 = info.fwVersion2;
            handleGBDeviceEvent(info);
        }
    }

    private void publishExerciseData() {
        if (LatestExercise > -1 && LatestBatteryLevel > -1) {
            UltrahumanExerciseData data = new UltrahumanExerciseData(LatestBatteryLevel, LatestExercise);
            publishExerciseData(data);
        }
    }

    private void publishExerciseData(@NonNull final UltrahumanExerciseData data) {
        String action;
        if (data.HR > -1 && data.Timestamp != -1) {
            action = DeviceService.ACTION_REALTIME_SAMPLES;
        } else {
            action = UltrahumanConstants.ACTION_EXERCISE_UPDATE;
        }

        LOG.debug("publishExerciseData BatteryLevel:{} Exercise:{} Timestamp:{} HR:{} HRV:{} Temperature:{}",
                data.BatteryLevel, data.Exercise,
                data.Timestamp, data.HR, data.HRV, data.Temperature);

        final Intent intent = new Intent(action);
        intent.setPackage(BuildConfig.APPLICATION_ID);

        intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
        intent.putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, data);

        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }


    void sendCommand(String taskName, byte... contents) {
        TransactionBuilder builder = createTransactionBuilder(taskName);
        builder.write(UUID_COMMAND, contents);
        enqueue(builder);
    }

    private void uiError(String message) {
        GB.toast(message, Toast.LENGTH_LONG, GB.ERROR);
    }

    private void uiInfo(@StringRes int stringRes) {
        GB.toast(getContext(), stringRes, Toast.LENGTH_LONG, GB.INFO);
    }

    final class UltrahumanReceiver extends BroadcastReceiver implements IntentListener {
        boolean Registered;

        @Override
        public void onReceive(Context context, Intent intent) {
            notify(intent);
        }

        @Override
        public void notify(Intent intent) {
            final @NonNls String address = intent.getStringExtra(UltrahumanConstants.EXTRA_ADDRESS);

            if (address != null && !address.isEmpty() && !address.equalsIgnoreCase(getDevice().getAddress())) {
                // this intent is for another device
                return;
            }

            final @NonNls String action = intent.getAction();
            if (action == null) {
                // invalid intent
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
                        DeviceInfo info = intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO);
                        if (info != null) {
                            handleDeviceInfo(info);
                        }
                    }
            }
        }

        private void changeExercise(byte exercise) {
            TransactionBuilder builder = createTransactionBuilder("changeExercise");
            if (exercise != UltrahumanExercise.CHECK.Code) {
                builder.write(UUID_COMMAND, exercise);
            }
            builder.write(UUID_COMMAND, OPERATION_CHECK_DATA);

            enqueue(builder);
        }
    }
}
