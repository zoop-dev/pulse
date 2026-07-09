/*  Copyright (C) 2026 Vladimir Tasic

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.fitcloud.ak102;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.GenericStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.fitcloud.ak102.Ak102Constants;
import nodomain.freeyourgadget.gadgetbridge.devices.fitcloud.ak102.Ak102SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Ak102ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class Ak102DeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(Ak102DeviceSupport.class);

    private static final String PREF_AUTH_TOKEN = "ak102_auth_token";
    private static final String PREF_DEVICE_INFO = Ak102Constants.PREF_DEVICE_INFO;

    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic notifyCharacteristic;

    private int sendSequence;
    private byte[] deviceInfo;

    // Reassembly state for multi-notification transport frames.
    private final ByteArrayOutputStream rxBuffer = new ByteArrayOutputStream();
    private boolean rxInProgress;
    private int rxExpectedLength;
    private int rxCrc;
    private int rxSequence;

    // History-sync driver state.
    private final List<Integer> syncQueue = new ArrayList<>();
    private final ByteArrayOutputStream syncData = new ByteArrayOutputStream();
    private int currentSyncType = -1;
    private boolean syncStarted;

    // Config TLV cache (item type -> raw bytes), populated from KEY_CONFIG_RESPONSE.
    private final Map<Byte, byte[]> configCache = new LinkedHashMap<>();

    // Last k27 battery poll, throttles the 0xAC battery-changed pushes.
    private long lastBatteryRequestMillis;

    public Ak102DeviceSupport() {
        super(LOG);
        addSupportedService(Ak102Constants.UUID_SERVICE);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);

        notifyCharacteristic = getCharacteristic(Ak102Constants.UUID_CHARACTERISTIC_NOTIFY);
        writeCharacteristic = getCharacteristic(Ak102Constants.UUID_CHARACTERISTIC_WRITE);

        if (notifyCharacteristic == null || writeCharacteristic == null) {
            LOG.error("AK102 characteristics missing (notify={}, write={})",
                    notifyCharacteristic, writeCharacteristic);
            builder.setDeviceState(GBDevice.State.NOT_CONNECTED);
            return builder;
        }

        rxReset();
        sendSequence = 0;
        deviceInfo = loadDeviceInfo();

        builder.requestMtu(Ak102Constants.MTU_REQUEST);
        builder.notify(notifyCharacteristic, true);
        builder.setCallback(this);

        // Start the application-layer login. The watch stays silent until this
        // succeeds, so the device is only marked INITIALIZED once the auth
        // response arrives (see handleAuthResponse).
        sendPacket(builder, Ak102Constants.CMD_AUTH, Ak102Constants.KEY_LOGIN_REQUEST, buildAuthPayload());

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic,
                                           final byte[] value) {
        if (!Ak102Constants.UUID_CHARACTERISTIC_NOTIFY.equals(characteristic.getUuid())) {
            return super.onCharacteristicChanged(gatt, characteristic, value);
        }
        if (value != null && value.length > 0) {
            onReceive(value);
        }
        return true;
    }

    // Transport layer -------------------------------------------------------

    private void onReceive(final byte[] value) {
        if (!rxInProgress) {
            if (value.length < Ak102Constants.HEADER_LENGTH || value[0] != Ak102Constants.FRAME_MAGIC) {
                LOG.warn("AK102 RX unexpected frame: {}", GB.hexdump(value));
                return;
            }
            final int flags = value[1] & 0xFF;
            if ((flags & Ak102Constants.FLAG_ACK) != 0) {
                // Acknowledgement of one of our writes; nothing to reassemble.
                return;
            }
            if ((flags & Ak102Constants.FLAG_ENCRYPTED) != 0) {
                LOG.warn("AK102 RX encrypted frame - not supported: {}", GB.hexdump(value));
                return;
            }
            rxExpectedLength = ((value[2] & 0xFF) << 8) | (value[3] & 0xFF);
            rxCrc = ((value[4] & 0xFF) << 8) | (value[5] & 0xFF);
            rxSequence = ((value[6] & 0xFF) << 8) | (value[7] & 0xFF);
            rxBuffer.reset();
            rxBuffer.write(value, Ak102Constants.HEADER_LENGTH, value.length - Ak102Constants.HEADER_LENGTH);
        } else {
            rxBuffer.write(value, 0, value.length);
        }

        if (rxBuffer.size() < rxExpectedLength) {
            rxInProgress = true;
            return;
        }

        final int sequence = rxSequence;
        final int expectedCrc = rxCrc;
        final int length = rxExpectedLength;
        final byte[] payload = trim(rxBuffer.toByteArray(), length);
        rxReset();

        if (CheckSums.getCRC16ansi(payload, 0) != expectedCrc) {
            LOG.warn("AK102 RX CRC mismatch (seq={}): {}", sequence, GB.hexdump(payload));
            sendAck(sequence, true);
            return;
        }

        sendAck(sequence, false);
        dispatch(payload);
    }

    private void rxReset() {
        rxInProgress = false;
        rxExpectedLength = 0;
        rxCrc = 0;
        rxSequence = 0;
        rxBuffer.reset();
    }

    private static byte[] trim(final byte[] data, final int length) {
        if (data.length == length) {
            return data;
        }
        final byte[] out = new byte[length];
        System.arraycopy(data, 0, out, 0, length);
        return out;
    }

    private void dispatch(final byte[] payload) {
        if (payload.length < 5) {
            LOG.warn("AK102 RX payload too short: {}", GB.hexdump(payload));
            return;
        }
        final byte cmdId = payload[0];
        // payload[1] holds the protocol version nibble.
        int offset = 2;
        while (offset + 3 <= payload.length) {
            final byte keyId = payload[offset];
            final int keyLen = ((payload[offset + 1] & 0xFF) << 8) | (payload[offset + 2] & 0xFF);
            offset += 3;
            if (offset + keyLen > payload.length) {
                LOG.warn("AK102 RX truncated key packet (cmd={}, key={}, len={})", cmdId, keyId, keyLen);
                return;
            }
            final byte[] keyData = new byte[keyLen];
            System.arraycopy(payload, offset, keyData, 0, keyLen);
            offset += keyLen;
            handlePacket(cmdId, keyId, keyData);
        }
    }

    private void handlePacket(final byte cmdId, final byte keyId, final byte[] keyData) {
        switch (cmdId) {
            case Ak102Constants.CMD_AUTH:
                if (keyId == Ak102Constants.KEY_LOGIN_RESPONSE || keyId == Ak102Constants.KEY_BIND_RESPONSE) {
                    handleAuthResponse(keyId, keyData);
                }
                return;
            case Ak102Constants.CMD_SETTINGS:
                handleSettingsPacket(keyId, keyData);
                return;
            case Ak102Constants.CMD_NOTIFICATION:
                handleTelephonyPacket(keyId);
                return;
            case Ak102Constants.CMD_DATA_SYNC:
                handleSyncPacket(keyId, keyData);
                return;
            case Ak102Constants.CMD_CAMERA_MEDIA:
                handleCameraMediaPacket(keyId, keyData);
                return;
            default:
                LOG.debug("AK102 RX unhandled cmd={} key={} data={}", cmdId, keyId, GB.hexdump(keyData));
        }
    }

    private void handleSettingsPacket(final byte keyId, final byte[] keyData) {
        switch (keyId) {
            case Ak102Constants.KEY_BATTERY_RESPONSE:
                // [0] charging flag, [1] bar count, [2] percentage 0-100
                // (verified against the watch's own battery display).
                LOG.debug("AK102 battery k28 raw: {}", GB.hexdump(keyData));
                if (keyData.length >= 3) {
                    handleBatteryStatus(keyData[0] == 1, keyData[2] & 0xFF);
                }
                return;
            case Ak102Constants.KEY_PUSH_BATTERY_CHANGED:
                // The payload is a bar count, not a percentage: use the push
                // only as a trigger to re-poll k27 for the real level.
                LOG.debug("AK102 battery push raw: {}", GB.hexdump(keyData));
                if (System.currentTimeMillis() - lastBatteryRequestMillis > 60_000L) {
                    lastBatteryRequestMillis = System.currentTimeMillis();
                    sendSimpleCommand("ak102-battery", Ak102Constants.CMD_SETTINGS,
                            Ak102Constants.KEY_BATTERY_REQUEST, null);
                }
                return;
            case Ak102Constants.KEY_CONFIG_RESPONSE:
                handleConfig(keyData);
                return;
            case Ak102Constants.KEY_PUSH_FIND_PHONE:
                handleFindPhone(true);
                return;
            case Ak102Constants.KEY_PUSH_STOP_FIND_PHONE:
                handleFindPhone(false);
                return;
            case Ak102Constants.KEY_PUSH_TYPED_EVENT:
                handleTypedEvent(keyData);
                return;
            case Ak102Constants.KEY_ALARMS_RESPONSE:
                handleAlarmsReadback(keyData);
                return;
            case Ak102Constants.KEY_RESTING_HR_RESPONSE:
                storeActivityPoints(Ak102SyncParser.parseRestingHeartRate(keyData));
                return;
            case Ak102Constants.KEY_EXERCISE_GOAL_RESPONSE:
                LOG.debug("AK102 exercise goal: {}", GB.hexdump(keyData));
                return;
            case Ak102Constants.KEY_PUSH_QUICK_REPLY_SMS:
            case Ak102Constants.KEY_PUSH_QUICK_REPLY_FREE:
                handleQuickReply(keyId, keyData);
                return;
            case Ak102Constants.KEY_PUSH_MUSIC_REFRESH:
                LOG.debug("AK102 requested music refresh");
                sendVolume();
                return;
            case Ak102Constants.KEY_PUSH_ALARM_CHANGED:
                LOG.info("AK102 alarms changed on watch - reading back");
                sendSimpleCommand("ak102-alarms-read", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_ALARMS_REQUEST, null);
                return;
            case Ak102Constants.KEY_PUSH_CONFIG_CHANGED:
                LOG.info("AK102 config changed on watch - re-reading");
                sendSimpleCommand("ak102-config", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_CONFIG_REQUEST, null);
                return;
            case Ak102Constants.KEY_PUSH_SILENT_MODE:
                LOG.info("AK102 requested phone silent-mode toggle (not implemented)");
                return;
            case Ak102Constants.KEY_PUSH_SOS:
                LOG.warn("AK102 SOS triggered on watch");
                return;
            case Ak102Constants.KEY_PUSH_AUDIO_DEVICE_MAC:
                LOG.debug("AK102 audio device MAC: {}", GB.hexdump(keyData));
                return;
            default:
                LOG.debug("AK102 RX unhandled settings key={} data={}", keyId, GB.hexdump(keyData));
        }
    }

    private void handleTelephonyPacket(final byte keyId) {
        final GBDeviceEventCallControl event = new GBDeviceEventCallControl();
        switch (keyId) {
            case Ak102Constants.KEY_PUSH_CALL_HANG_UP:
                event.event = GBDeviceEventCallControl.Event.REJECT;
                break;
            case Ak102Constants.KEY_PUSH_CALL_ACCEPT:
                event.event = GBDeviceEventCallControl.Event.ACCEPT;
                break;
            default:
                LOG.debug("AK102 RX unhandled telephony key={}", keyId);
                return;
        }
        evaluateGBDeviceEvent(event);
    }

    private void handleCameraMediaPacket(final byte keyId, final byte[] keyData) {
        switch (keyId) {
            case Ak102Constants.KEY_PUSH_CAMERA_SHOOT:
                sendCameraEvent(GBDeviceEventCameraRemote.Event.TAKE_PICTURE);
                return;
            case Ak102Constants.KEY_PUSH_CAMERA_OPEN:
                sendCameraEvent(GBDeviceEventCameraRemote.Event.OPEN_CAMERA);
                return;
            case Ak102Constants.KEY_PUSH_CAMERA_EXIT:
                sendCameraEvent(GBDeviceEventCameraRemote.Event.CLOSE_CAMERA);
                return;
            case Ak102Constants.KEY_PUSH_MEDIA_CONTROL:
                if (keyData.length >= 1) {
                    handleMediaControl(keyData[0] & 0xFF);
                }
                return;
            default:
                LOG.debug("AK102 RX unhandled camera/media key={} data={}", keyId, GB.hexdump(keyData));
        }
    }

    private void sendCameraEvent(final GBDeviceEventCameraRemote.Event cameraEvent) {
        final GBDeviceEventCameraRemote event = new GBDeviceEventCameraRemote();
        event.event = cameraEvent;
        evaluateGBDeviceEvent(event);
    }

    private void handleMediaControl(final int subtype) {
        final GBDeviceEventMusicControl event = new GBDeviceEventMusicControl();
        if (subtype <= Ak102Constants.MEDIA_PLAY_PAUSE_MAX) {
            event.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
        } else if (subtype == Ak102Constants.MEDIA_NEXT) {
            event.event = GBDeviceEventMusicControl.Event.NEXT;
        } else if (subtype == Ak102Constants.MEDIA_PREVIOUS) {
            event.event = GBDeviceEventMusicControl.Event.PREVIOUS;
        } else if (subtype == Ak102Constants.MEDIA_VOLUME_UP) {
            event.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
        } else if (subtype == Ak102Constants.MEDIA_VOLUME_DOWN) {
            event.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
        } else {
            LOG.debug("AK102 unhandled media subtype {}", subtype);
            return;
        }
        evaluateGBDeviceEvent(event);
    }

    private void handleFindPhone(final boolean start) {
        final GBDeviceEventFindPhone event = new GBDeviceEventFindPhone();
        event.event = start ? GBDeviceEventFindPhone.Event.START : GBDeviceEventFindPhone.Event.STOP;
        evaluateGBDeviceEvent(event);
    }

    private void handleTypedEvent(final byte[] keyData) {
        if (keyData.length < 1) {
            return;
        }
        final int type = keyData[0] & 0xFF;
        switch (type) {
            case Ak102Constants.EVENT_BATTERY:
            case Ak102Constants.EVENT_LOW_BATTERY:
                if (keyData.length >= 5) {
                    handleBatteryStatus(keyData[2] == 1, keyData[4] & 0xFF);
                }
                return;
            case Ak102Constants.EVENT_HEART_RATE_MEASURE:
                if (keyData.length >= 7) {
                    // keyData[2..5] packed timestamp (section 5.3), keyData[6] heart rate bpm.
                    final int bpm = keyData[6] & 0xFF;
                    LOG.info("AK102 heart rate measurement: {} bpm", bpm);
                    if (bpm > 0) {
                        final Ak102SyncParser.ActivityPoint p = new Ak102SyncParser.ActivityPoint(
                                (int) (Ak102SyncParser.decodeSecondTs(keyData, 2) / 1000L));
                        p.heartRate = bpm;
                        final List<Ak102SyncParser.ActivityPoint> one = new ArrayList<>();
                        one.add(p);
                        storeActivityPoints(one);
                    }
                }
                return;
            case Ak102Constants.EVENT_SPORT_FINISH:
                LOG.info("AK102 workout finished on watch - triggering sync");
                onFetchRecordedData(0);
                return;
            case Ak102Constants.EVENT_STOP_MEASURE:
                LOG.debug("AK102 measurement stopped on watch");
                return;
            default:
                LOG.debug("AK102 typed event {} data={}", type, GB.hexdump(keyData));
        }
    }

    private void sendAck(final int sequence, final boolean error) {
        final byte[] frame = new byte[Ak102Constants.HEADER_LENGTH];
        frame[0] = Ak102Constants.FRAME_MAGIC;
        frame[1] = (byte) (Ak102Constants.FLAG_ACK | (error ? Ak102Constants.FLAG_ACK_ERROR : 0));
        frame[6] = (byte) ((sequence >> 8) & 0xFF);
        frame[7] = (byte) (sequence & 0xFF);
        final TransactionBuilder builder = createTransactionBuilder("ak102-ack");
        builder.write(writeCharacteristic, frame);
        builder.queue();
    }

    private void sendPacket(final TransactionBuilder builder, final byte cmdId, final byte keyId,
                            final byte[] keyData) {
        final byte[] frame = encode(cmdId, keyId, keyData);
        builder.writeChunkedData(writeCharacteristic, frame, frame.length);
    }

    private void sendSimpleCommand(final String taskName, final byte cmdId, final byte keyId,
                                   final byte[] keyData) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        sendPacket(builder, cmdId, keyId, keyData);
        builder.queue();
    }

    private byte[] encode(final byte cmdId, final byte keyId, final byte[] keyData) {
        final int dataLength = keyData == null ? 0 : keyData.length;
        // payload = cmdId, version, keyId, len_hi, len_lo, data...
        final byte[] payload = new byte[5 + dataLength];
        payload[0] = cmdId;
        payload[1] = 0;
        payload[2] = keyId;
        payload[3] = (byte) ((dataLength >> 8) & 0xFF);
        payload[4] = (byte) (dataLength & 0xFF);
        if (dataLength > 0) {
            System.arraycopy(keyData, 0, payload, 5, dataLength);
        }

        final int crc = CheckSums.getCRC16ansi(payload, 0);
        final int sequence = nextSequence();
        final byte[] frame = new byte[Ak102Constants.HEADER_LENGTH + payload.length];
        frame[0] = Ak102Constants.FRAME_MAGIC;
        frame[1] = 0;
        frame[2] = (byte) ((payload.length >> 8) & 0xFF);
        frame[3] = (byte) (payload.length & 0xFF);
        frame[4] = (byte) ((crc >> 8) & 0xFF);
        frame[5] = (byte) (crc & 0xFF);
        frame[6] = (byte) ((sequence >> 8) & 0xFF);
        frame[7] = (byte) (sequence & 0xFF);
        System.arraycopy(payload, 0, frame, Ak102Constants.HEADER_LENGTH, payload.length);
        return frame;
    }

    private int nextSequence() {
        final int sequence = sendSequence;
        sendSequence = (sendSequence + 1) & 0xFFFF;
        return sequence;
    }

    // Authentication --------------------------------------------------------

    private byte[] buildAuthPayload() {
        final byte[] payload = new byte[Ak102Constants.AUTH_USER_ID_LENGTH];
        final byte[] token = getAuthToken().getBytes(StandardCharsets.UTF_8);
        System.arraycopy(token, 0, payload, 0, Math.min(token.length, Ak102Constants.AUTH_USER_ID_MAX));
        return payload;
    }

    private String getAuthToken() {
        final SharedPreferences prefs = getAk102Prefs();
        String token = prefs.getString(PREF_AUTH_TOKEN, null);
        if (token == null) {
            token = Long.toString(System.currentTimeMillis(), 36);
            prefs.edit().putString(PREF_AUTH_TOKEN, token).apply();
        }
        return token;
    }

    private SharedPreferences getAk102Prefs() {
        return GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
    }

    private void handleAuthResponse(final byte keyId, final byte[] keyData) {
        if (keyData.length < 1) {
            LOG.warn("AK102 auth response without result byte");
            return;
        }
        final byte result = keyData[0];
        if (keyId == Ak102Constants.KEY_LOGIN_RESPONSE) {
            if (result == Ak102Constants.AUTH_OK) {
                LOG.info("AK102 login OK");
                onAuthenticated();
            } else if (result == Ak102Constants.AUTH_NEED_BIND) {
                LOG.info("AK102 not bound - requesting bind (confirm on watch)");
                sendSimpleCommand("ak102-bind", Ak102Constants.CMD_AUTH, Ak102Constants.KEY_BIND_REQUEST, buildAuthPayload());
            } else {
                LOG.warn("AK102 login failed (result={})", result);
            }
            return;
        }

        // Bind response.
        if (result == Ak102Constants.AUTH_OK) {
            LOG.info("AK102 bind OK");
            onAuthenticated();
        } else if (result == Ak102Constants.AUTH_BIND_CANCELLED) {
            LOG.warn("AK102 bind cancelled on watch");
        } else if (result == Ak102Constants.AUTH_BIND_TIMEOUT) {
            LOG.warn("AK102 bind timed out");
        } else if (result == Ak102Constants.AUTH_SHOULD_RESET) {
            LOG.warn("AK102 bind requires watch reset");
        } else {
            LOG.warn("AK102 bind failed (result={})", result);
        }
    }

    private void onAuthenticated() {
        final TransactionBuilder builder = createTransactionBuilder("ak102-init");
        sendPacket(builder, Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SDK_FUNCTION, buildSdkFunctionPayload());
        sendPacket(builder, Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_USER_INFO, buildUserInfoPayload());
        sendPacket(builder, Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_TIME, buildTimePayload());
        sendPacket(builder, Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_EXERCISE_GOAL, buildExerciseGoalPayload());
        sendPacket(builder, Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_CONFIG_REQUEST, null);
        sendPacket(builder, Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_BATTERY_REQUEST, null);
        sendPacket(builder, Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_ALARMS_REQUEST, null);
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        builder.queue();
        sendVolume();
        // Push whatever weather GB already has, so the watch face fills in
        // immediately instead of waiting for the next provider broadcast.
        onSendWeather();
        // Re-attach call audio (HFP/A2DP) when the user opted in and the
        // classic side is already bonded.
        if (getAk102Prefs().getBoolean(Ak102Constants.PREF_AUDIO_AUTO_CONNECT, false)
                && getBondState() == BluetoothDevice.BOND_BONDED) {
            connectAudioProfiles();
        }
    }

    // Device info / feature bits --------------------------------------------

    private byte[] loadDeviceInfo() {
        final String hex = getAk102Prefs().getString(PREF_DEVICE_INFO, null);
        if (hex == null || hex.length() < Ak102Constants.DEVICE_INFO_MIN_LENGTH * 2) {
            return null;
        }
        return GB.hexStringToByteArray(hex);
    }

    private void handleConfig(final byte[] payload) {
        // The config blob is a list of TLV items: [type][len][data...].
        final SharedPreferences.Editor editor = getAk102Prefs().edit();
        int offset = 0;
        while (offset + 2 <= payload.length) {
            final int type = payload[offset] & 0xFF;
            final int length = payload[offset + 1] & 0xFF;
            offset += 2;
            if (offset + length > payload.length) {
                break;
            }
            final byte[] item = new byte[length];
            System.arraycopy(payload, offset, item, 0, length);
            configCache.put((byte) type, item);
            // Persist for the coordinator (settings gating) and later connects.
            editor.putString(Ak102Constants.PREF_CONFIG_PREFIX + type, GB.hexdump(item));
            if (type == Ak102Constants.CONFIG_ITEM_DEVICE_INFO
                    && length >= Ak102Constants.DEVICE_INFO_MIN_LENGTH) {
                handleDeviceInfo(item);
            }
            offset += length;
        }
        editor.apply();
        reflectConfigToPrefs();
        reconcileHealthMonitor();
    }

    // Mirror the watch's config into GB preferences so the settings UI matches.
    private void reflectConfigToPrefs() {
        final Map<String, Object> update = new LinkedHashMap<>();
        final byte[] fn = configCache.get(Ak102Constants.KEY_SET_CONFIG_FUNCTION);
        if (fn != null && fn.length >= 2) {
            update.put(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT,
                    getFunctionFlag(fn, Ak102Constants.FUNCTION_FLAG_TIME_FORMAT)
                            ? DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H
                            : DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H);
            update.put(DeviceSettingsPreferenceConst.PREF_WEARLOCATION,
                    getFunctionFlag(fn, Ak102Constants.FUNCTION_FLAG_WEAR_WAY) ? "right" : "left");
            update.put(DeviceSettingsPreferenceConst.PREF_DISCONNECTNOTIF_NOSHED,
                    getFunctionFlag(fn, Ak102Constants.FUNCTION_FLAG_DISCONNECT_REMINDER));
        }
        final byte[] tw = configCache.get(Ak102Constants.KEY_SET_CONFIG_TURN_WRIST);
        if (tw != null && tw.length >= 5) {
            final int start = u16(tw, 1);
            final int end = u16(tw, 3);
            update.put(DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT,
                    tw[0] != 1 ? "off" : (start == 0 && end >= 1439 ? "on" : "scheduled"));
            update.put(DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_START, hhmm(start));
            update.put(DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_END, hhmm(end));
        }
        final byte[] dnd = configCache.get(Ak102Constants.KEY_SET_CONFIG_DND);
        if (dnd != null && dnd.length >= 6) {
            update.put(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB,
                    dnd[0] == 1 ? DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_ALWAYS
                            : dnd[1] == 1 ? DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SCHEDULED
                            : DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_OFF);
            update.put(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_START, hhmm(u16(dnd, 2)));
            update.put(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_END, hhmm(u16(dnd, 4)));
        }
        final byte[] sed = configCache.get(Ak102Constants.KEY_SET_CONFIG_SEDENTARY);
        if (sed != null && sed.length >= 7) {
            update.put(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, (sed[0] & 1) != 0);
            update.put(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND, (sed[0] & 2) != 0);
            update.put(DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, hhmm(u16(sed, 1)));
            update.put(DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, hhmm(u16(sed, 3)));
            update.put(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, String.valueOf(u16(sed, 5)));
        }
        final byte[] dw = configCache.get(Ak102Constants.KEY_SET_CONFIG_DRINK_WATER);
        if (dw != null && dw.length >= 7) {
            update.put(DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH, dw[0] == 1);
            update.put(DeviceSettingsPreferenceConst.PREF_HYDRATION_PERIOD, String.valueOf(u16(dw, 1)));
            update.put(DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_START, hhmm(u16(dw, 3)));
            update.put(DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_END, hhmm(u16(dw, 5)));
        }
        final byte[] pages = configCache.get(Ak102Constants.KEY_SET_CONFIG_PAGE);
        if (pages != null && pages.length >= 2) {
            final Set<String> enabled = new HashSet<>();
            for (final int flag : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 13}) {
                if (getFunctionFlag(pages, flag)) {
                    enabled.add(String.valueOf(flag));
                }
            }
            update.put(Ak102Constants.PREF_DISPLAY_ITEMS, enabled);
        }
        if (!update.isEmpty()) {
            evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(update));
        }
    }

    // FlagUtil descending scheme reader (mirror of setFunctionFlag).
    private static boolean getFunctionFlag(final byte[] bytes, final int flag) {
        final int index = (bytes.length - 1) - flag / 8;
        if (index < 0 || index >= bytes.length) {
            return false;
        }
        return (bytes[index] & (1 << (flag % 8))) != 0;
    }

    private static int u16(final byte[] b, final int o) {
        return ((b[o] & 0xFF) << 8) | (b[o + 1] & 0xFF);
    }

    private static String hhmm(final int minuteOfDay) {
        return String.format(Locale.ROOT, "%02d:%02d", (minuteOfDay / 60) % 24, minuteOfDay % 60);
    }

    // Push the health-monitor state to the watch when it disagrees with the pref.
    private void reconcileHealthMonitor() {
        final SharedPreferences prefs = getAk102Prefs();
        final boolean wanted = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_AUTOMATIC_ENABLE, false);
        final byte[] cached = configCache.get(Ak102Constants.KEY_SET_CONFIG_HEALTH_MONITOR);
        final boolean actual = cached != null && cached.length >= 1 && cached[0] == 1;
        if (wanted != actual) {
            LOG.info("AK102 health monitor mismatch (want {}, watch {}), updating", wanted, actual);
            sendHealthMonitorConfig(prefs);
        }
    }

    private void handleDeviceInfo(final byte[] info) {
        deviceInfo = info;
        getAk102Prefs().edit().putString(PREF_DEVICE_INFO, GB.hexdump(info)).apply();

        // Firmware version, as displayed by the watch: app bytes [26..27] as
        // major.minor, project bytes [4..5] as build number.
        final String fwVersion = String.format(Locale.ROOT, "%d.%02d (%02X%02X)",
                info[26] & 0xFF, info[27] & 0xFF, info[4], info[5]);

        final GBDeviceEventVersionInfo event = new GBDeviceEventVersionInfo();
        event.fwVersion = fwVersion;
        event.hwVersion = GB.hexdump(info, 0, 6);
        handleGBDeviceEvent(event);
        LOG.info("AK102 device info ({}): {}", fwVersion, GB.hexdump(info));
    }

    private boolean supportsWatchFeature(final int feature) {
        return Ak102Constants.isFeatureSupported(deviceInfo, feature);
    }

    // Command payloads ------------------------------------------------------

    private static byte[] buildSdkFunctionPayload() {
        // Advertise the phone-side capabilities (bits 0..2 of the last byte).
        return new byte[]{0, 0, 0, 0x07};
    }

    private static byte[] buildTimePayload() {
        final Calendar now = Calendar.getInstance();
        return packDateTime(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
    }

    private static byte[] packDateTime(final int year, final int month, final int day,
                                       final int hour, final int minute, final int second) {
        final int shortYear = year - 2000;
        final byte[] packed = new byte[4];
        packed[0] = (byte) ((shortYear << 2) | (month >> 2));
        packed[1] = (byte) ((month << 6) | (day << 1) | (hour >> 4));
        packed[2] = (byte) ((hour << 4) | (minute >> 2));
        packed[3] = (byte) ((minute << 6) | second);
        return packed;
    }

    private static byte[] buildUserInfoPayload() {
        final ActivityUser user = new ActivityUser();
        final boolean male = user.getGender() == ActivityUser.GENDER_MALE;
        final int age = Math.max(0, Math.min(user.getAge(), 127));
        final int height = (user.getHeightCm() * 10) / 5;
        final int weight = (user.getWeightKg() * 10) / 5;
        final byte[] payload = new byte[4];
        payload[0] = (byte) ((male ? 128 : 0) | age);
        payload[1] = (byte) (height >> 1);
        payload[2] = (byte) ((height << 7) | (weight >> 3));
        payload[3] = (byte) (weight << 5);
        return payload;
    }

    // Battery ----------------------------------------------------------------

    private void handleBatteryStatus(final boolean charging, final int percentage) {
        final GBDeviceEventBatteryInfo event = new GBDeviceEventBatteryInfo();
        event.level = percentage;
        event.state = charging ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL;
        handleGBDeviceEvent(event);
    }

    // Notifications -----------------------------------------------------------

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        final String name = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        final StringBuilder content = new StringBuilder();
        if (!StringUtils.isEmpty(notificationSpec.title) && !notificationSpec.title.equals(name)) {
            content.append(notificationSpec.title).append(": ");
        }
        if (!StringUtils.isEmpty(notificationSpec.body)) {
            content.append(notificationSpec.body);
        } else if (!StringUtils.isEmpty(notificationSpec.subject)) {
            content.append(notificationSpec.subject);
        }
        sendNotification(mapNotificationType(notificationSpec), name, content.toString());
    }

    private byte mapNotificationType(final NotificationSpec notificationSpec) {
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                return Ak102Constants.NOTIFICATION_SMS;
            case QQ:
                return Ak102Constants.NOTIFICATION_QQ;
            case WECHAT:
                return Ak102Constants.NOTIFICATION_WECHAT;
            case FACEBOOK:
                return Ak102Constants.NOTIFICATION_FACEBOOK;
            case TWITTER:
                return Ak102Constants.NOTIFICATION_TWITTER;
            case LINKEDIN:
                return Ak102Constants.NOTIFICATION_LINKEDIN;
            case INSTAGRAM:
                return Ak102Constants.NOTIFICATION_INSTAGRAM;
            case PINTEREST:
                return Ak102Constants.NOTIFICATION_PINTEREST;
            case WHATSAPP:
                return Ak102Constants.NOTIFICATION_WHATSAPP;
            case LINE:
                return Ak102Constants.NOTIFICATION_LINE;
            case FACEBOOK_MESSENGER:
                return Ak102Constants.NOTIFICATION_FACEBOOK_MESSENGER;
            case KAKAO_TALK:
                return Ak102Constants.NOTIFICATION_KAKAO;
            case SKYPE:
                return Ak102Constants.NOTIFICATION_SKYPE;
            case GENERIC_EMAIL:
            case YAHOO_MAIL:
            case MAILBOX:
                return Ak102Constants.NOTIFICATION_EMAIL;
            case GMAIL:
            case GOOGLE_INBOX:
                return Ak102Constants.NOTIFICATION_GMAIL;
            case OUTLOOK:
                return Ak102Constants.NOTIFICATION_OUTLOOK;
            case TELEGRAM:
                return Ak102Constants.NOTIFICATION_TELEGRAM;
            case VIBER:
                return Ak102Constants.NOTIFICATION_VIBER;
            case GENERIC_CALENDAR:
            case BUSINESS_CALENDAR:
                return Ak102Constants.NOTIFICATION_CALENDAR;
            case SNAPCHAT:
                return Ak102Constants.NOTIFICATION_SNAPCHAT;
            case YOUTUBE:
                return Ak102Constants.NOTIFICATION_YOUTUBE;
            default:
                return Ak102Constants.NOTIFICATION_SMS;
        }
    }

    private void sendNotification(final byte type, final String name, final String content) {
        final byte[] nameBytes = name == null ? new byte[0] : name.getBytes(StandardCharsets.UTF_8);
        final byte[] contentBytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        final byte[] payload;

        if (supportsWatchFeature(Ak102Constants.FEATURE_LONG_NOTIFICATION)) {
            // [nameLen][name<=64][contentLen:2][content<=240-nameLen]
            final int nameLength = Math.min(nameBytes.length, 64);
            final int contentLength = Math.min(contentBytes.length, 240 - nameLength);
            payload = new byte[nameLength + 3 + contentLength];
            payload[0] = (byte) nameLength;
            System.arraycopy(nameBytes, 0, payload, 1, nameLength);
            payload[nameLength + 1] = (byte) ((contentLength >> 8) & 0xFF);
            payload[nameLength + 2] = (byte) (contentLength & 0xFF);
            System.arraycopy(contentBytes, 0, payload, nameLength + 3, contentLength);
        } else {
            // [name 20B space padded][contentLen:2][content<=221]
            final int nameLength = Math.min(nameBytes.length, 20);
            final int contentLength = Math.min(contentBytes.length, 221);
            payload = new byte[22 + contentLength];
            System.arraycopy(nameBytes, 0, payload, 0, nameLength);
            for (int i = nameLength; i < 20; i++) {
                payload[i] = ' ';
            }
            payload[20] = (byte) ((contentLength >> 8) & 0xFF);
            payload[21] = (byte) (contentLength & 0xFF);
            System.arraycopy(contentBytes, 0, payload, 22, contentLength);
        }

        sendSimpleCommand("ak102-notification", Ak102Constants.CMD_NOTIFICATION, type, payload);
    }

    // Calls -------------------------------------------------------------------

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        final String name = StringUtils.getFirstOf(callSpec.name, callSpec.number);
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                sendNotification(Ak102Constants.NOTIFICATION_TELEPHONY_INCOMING, name, callSpec.number);
                break;
            case CallSpec.CALL_REJECT:
                sendNotification(Ak102Constants.NOTIFICATION_TELEPHONY_REJECTED, name, callSpec.number);
                break;
            case CallSpec.CALL_ACCEPT:
            case CallSpec.CALL_START:
            case CallSpec.CALL_END:
                sendNotification(Ak102Constants.NOTIFICATION_TELEPHONY_ANSWERED, name, callSpec.number);
                break;
            default:
                break;
        }
    }

    // Find device ---------------------------------------------------------------

    @Override
    public void onFindDevice(final boolean start) {
        if (start) {
            sendSimpleCommand("ak102-find", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_FIND_DEVICE, null);
        } else if (supportsWatchFeature(Ak102Constants.FEATURE_STOP_FIND_DEVICE)) {
            sendSimpleCommand("ak102-find-stop", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_STOP_FIND_DEVICE, null);
        } else {
            LOG.info("AK102 does not support stopping find-device remotely");
        }
    }

    // Time ------------------------------------------------------------------------

    @Override
    public void onSetTime() {
        sendSimpleCommand("ak102-time", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_TIME, buildTimePayload());
    }

    // Alarms -------------------------------------------------------------------------

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        final ByteArrayOutputStream records = new ByteArrayOutputStream();
        for (final Alarm alarm : alarms) {
            if (alarm.getUnused()) {
                continue;
            }
            final byte[] record = encodeAlarm(alarm);
            records.write(record, 0, record.length);
        }
        final byte[] payload = records.size() > 0 ? records.toByteArray() : null;
        sendSimpleCommand("ak102-alarms", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_ALARMS, payload);
    }

    static byte[] encodeAlarm(final Alarm alarm) {
        int year = 0;
        int month = 0;
        int day = 0;
        if (alarm.getRepetition() == Alarm.ALARM_ONCE) {
            // One-shot alarms carry a concrete date: the next occurrence.
            final Calendar when = Calendar.getInstance();
            final Calendar now = Calendar.getInstance();
            when.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            when.set(Calendar.MINUTE, alarm.getMinute());
            when.set(Calendar.SECOND, 0);
            if (!when.after(now)) {
                when.add(Calendar.DAY_OF_MONTH, 1);
            }
            year = when.get(Calendar.YEAR) - 2000;
            month = when.get(Calendar.MONTH) + 1;
            day = when.get(Calendar.DAY_OF_MONTH);
        }

        byte[] label = new byte[0];
        if (alarm.getTitle() != null) {
            label = alarm.getTitle().getBytes(StandardCharsets.UTF_8);
            if (label.length > 32) {
                label = trim(label, 32);
            }
        }

        final byte[] record = new byte[label.length + 6];
        record[0] = (byte) (label.length + 5);
        record[1] = (byte) ((year << 2) | (month >> 2));
        record[2] = (byte) ((month << 6) | (day << 1) | (alarm.getHour() >> 4));
        record[3] = (byte) ((alarm.getHour() << 4) | (alarm.getMinute() >> 2));
        record[4] = (byte) ((alarm.getMinute() << 6) | (alarm.getPosition() << 3));
        record[5] = (byte) ((alarm.getEnabled() ? 0x80 : 0) | alarm.getRepetition());
        System.arraycopy(label, 0, record, 6, label.length);
        return record;
    }

    // Weather ----------------------------------------------------------------------------

    @Override
    public void onSendWeather() {
        final WeatherSpec weather = Weather.getWeatherSpec();
        if (weather == null) {
            LOG.warn("AK102 no weather data available");
            return;
        }
        if (!supportsWatchFeature(Ak102Constants.FEATURE_WEATHER)) {
            LOG.info("AK102 watch does not support weather");
            return;
        }
        // Enable the weather watchface complication (FitCloudPro sets this too).
        setFunctionFlag(Ak102Constants.FUNCTION_FLAG_WEATHER_DISPLAY, true);
        sendSimpleCommand("ak102-weather", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_WEATHER,
                buildWeatherPayload(weather, supportsWatchFeature(Ak102Constants.FEATURE_WEATHER_FORECAST)));
        LOG.info("AK102 weather sent: {} {}C", weather.getLocation(), weather.getCurrentTemp() - 273);
    }

    private static byte[] buildWeatherPayload(final WeatherSpec weather, final boolean withForecast) {
        final byte[] city = StringUtils.ensureNotNull(weather.getLocation()).getBytes(StandardCharsets.UTF_8);
        final int cityLength = Math.min(city.length, 64);
        final int currentTemp = weather.getCurrentTemp() - 273;
        final int lowTemp = weather.getTodayMinTemp() - 273;
        final int highTemp = weather.getTodayMaxTemp() - 273;
        final int condition = mapWeatherCondition(weather.getCurrentConditionCode());

        if (!withForecast) {
            // [curT][lowT][highT][code][cityLen][city][updTime:4][pad 6]
            final byte[] payload = new byte[cityLength + 15];
            payload[0] = (byte) currentTemp;
            payload[1] = (byte) lowTemp;
            payload[2] = (byte) highTemp;
            payload[3] = (byte) condition;
            payload[4] = (byte) cityLength;
            System.arraycopy(city, 0, payload, 5, cityLength);
            final byte[] time = packTimestamp(weather.getTimestamp() * 1000L);
            System.arraycopy(time, 0, payload, cityLength + 5, 4);
            return payload;
        }

        final List<WeatherSpec.Daily> forecasts = weather.getForecasts();
        final int forecastCount = Math.min(forecasts.size(), 7);
        final byte[] payload = new byte[cityLength + 10 + forecastCount * 3 + 12];
        payload[0] = (byte) currentTemp;
        payload[1] = (byte) lowTemp;
        payload[2] = (byte) highTemp;
        payload[3] = (byte) condition;
        payload[4] = (byte) cityLength;
        System.arraycopy(city, 0, payload, 5, cityLength);
        final byte[] time = packTimestamp(weather.getTimestamp() * 1000L);
        System.arraycopy(time, 0, payload, cityLength + 5, 4);

        int offset = cityLength + 9;
        payload[offset++] = (byte) forecastCount;
        for (int i = 0; i < forecastCount; i++) {
            final WeatherSpec.Daily daily = forecasts.get(i);
            payload[offset++] = (byte) (daily.getMinTemp() - 273);
            payload[offset++] = (byte) (daily.getMaxTemp() - 273);
            payload[offset++] = (byte) mapWeatherCondition(daily.getConditionCode());
        }

        final int pressure = (int) weather.getPressure();
        payload[offset++] = (byte) ((pressure >> 24) & 0xFF);
        payload[offset++] = (byte) ((pressure >> 16) & 0xFF);
        payload[offset++] = (byte) ((pressure >> 8) & 0xFF);
        payload[offset++] = (byte) (pressure & 0xFF);
        payload[offset++] = (byte) beaufortFromKmh(weather.getWindSpeed());
        final int visibilityMeters = (int) weather.getVisibility();
        payload[offset++] = (byte) (visibilityMeters / 1000);
        payload[offset++] = (byte) Math.round(weather.getUvIndex());
        payload[offset++] = (byte) ((visibilityMeters >> 8) & 0xFF);
        payload[offset++] = (byte) (visibilityMeters & 0xFF);
        final int aqi = weather.getAirQuality() != null ? weather.getAirQuality().getAqi() : 0;
        payload[offset++] = (byte) ((aqi >> 8) & 0xFF);
        payload[offset++] = (byte) (aqi & 0xFF);
        payload[offset] = (byte) (weather.getCurrentHumidity() & 0xFF);
        return payload;
    }

    private static byte[] packTimestamp(final long millis) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return packDateTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    // Maps OpenWeatherMap condition codes to the FitCloud weather table (0..13).
    private static int mapWeatherCondition(final int openWeatherMapCondition) {
        final int group = openWeatherMapCondition / 100;
        switch (group) {
            case 2: // thunderstorm
                return 5;
            case 3: // drizzle
                return 8;
            case 5: // rain
                if (openWeatherMapCondition == 500) return 8;
                if (openWeatherMapCondition == 511) return 7;
                if (openWeatherMapCondition >= 502 && openWeatherMapCondition <= 504) return 9;
                return 4;
            case 6: // snow
                if (openWeatherMapCondition >= 611 && openWeatherMapCondition <= 616) return 7;
                if (openWeatherMapCondition == 602 || openWeatherMapCondition >= 620) return 11;
                return 10;
            case 7: // atmosphere
                if (openWeatherMapCondition == 731 || openWeatherMapCondition == 751
                        || openWeatherMapCondition == 761 || openWeatherMapCondition == 762
                        || openWeatherMapCondition == 781) {
                    return 12;
                }
                return 13;
            case 8: // clear/clouds
                if (openWeatherMapCondition == 800) return 1;
                if (openWeatherMapCondition <= 802) return 2;
                return 3;
            default:
                return 0;
        }
    }

    private static int beaufortFromKmh(final float kmh) {
        final float[] limits = {1, 5, 11, 19, 28, 38, 49, 61, 74, 88, 102, 117};
        for (int i = 0; i < limits.length; i++) {
            if (kmh < limits[i]) {
                return i;
            }
        }
        return 12;
    }

    // Music -------------------------------------------------------------------------------

    @Override
    public void onSetMusicInfo(final MusicSpec musicSpec) {
        if (!supportsWatchFeature(Ak102Constants.FEATURE_MUSIC_INFO)) {
            return;
        }
        final byte[] title = StringUtils.ensureNotNull(musicSpec.track).getBytes(StandardCharsets.UTF_8);
        final byte[] artist = StringUtils.ensureNotNull(musicSpec.artist).getBytes(StandardCharsets.UTF_8);
        final int titleLength = Math.min(title.length, 127);
        final int artistLength = Math.min(artist.length, 96);
        final int duration = Math.max(musicSpec.duration, 0);

        final byte[] payload = new byte[6 + titleLength + artistLength];
        payload[0] = (byte) titleLength;
        payload[1] = (byte) artistLength;
        payload[2] = (byte) ((duration >> 24) & 0xFF);
        payload[3] = (byte) ((duration >> 16) & 0xFF);
        payload[4] = (byte) ((duration >> 8) & 0xFF);
        payload[5] = (byte) (duration & 0xFF);
        System.arraycopy(title, 0, payload, 6, titleLength);
        System.arraycopy(artist, 0, payload, 6 + titleLength, artistLength);
        sendSimpleCommand("ak102-music-info", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_MUSIC_INFO, payload);
    }

    @Override
    public void onSetMusicState(final MusicStateSpec stateSpec) {
        if (!supportsWatchFeature(Ak102Constants.FEATURE_MUSIC_INFO)) {
            return;
        }
        // FitCloud states: 0 stop, 1 playing, 2 pause.
        final int state = stateSpec.state == MusicStateSpec.STATE_PLAYING ? 1
                : stateSpec.state == MusicStateSpec.STATE_PAUSED ? 2 : 0;
        final int position = Math.max(stateSpec.position, 0);
        final int speed = 100;

        final byte[] payload = new byte[7];
        payload[0] = (byte) state;
        payload[1] = (byte) ((position >> 24) & 0xFF);
        payload[2] = (byte) ((position >> 16) & 0xFF);
        payload[3] = (byte) ((position >> 8) & 0xFF);
        payload[4] = (byte) (position & 0xFF);
        payload[5] = (byte) ((speed >> 8) & 0xFF);
        payload[6] = (byte) (speed & 0xFF);
        sendSimpleCommand("ak102-music-state", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_MUSIC_STATE, payload);
    }

    // Camera ------------------------------------------------------------------------------

    @Override
    public void onCameraStatusChange(final GBDeviceEventCameraRemote.Event event, final String filename) {
        if (event == GBDeviceEventCameraRemote.Event.OPEN_CAMERA) {
            sendSimpleCommand("ak102-camera", Ak102Constants.CMD_CAMERA_MEDIA, Ak102Constants.KEY_CAMERA_STATUS, new byte[]{0});
        } else if (event == GBDeviceEventCameraRemote.Event.CLOSE_CAMERA) {
            sendSimpleCommand("ak102-camera", Ak102Constants.CMD_CAMERA_MEDIA, Ak102Constants.KEY_CAMERA_STATUS, new byte[]{1});
        }
    }

    // History sync -----------------------------------------------------------

    // Merged activity buckets accumulated across one fetch (keyed by unix seconds).
    private final TreeMap<Integer, Ak102SyncParser.ActivityPoint> fetchActivity = new TreeMap<>();
    private final List<Ak102SyncParser.ValuePoint> fetchSpo2 = new ArrayList<>();
    private final List<Ak102SyncParser.ValuePoint> fetchStress = new ArrayList<>();
    private final List<Ak102SyncParser.Workout> fetchWorkouts = new ArrayList<>();
    private final List<Ak102SyncParser.GpsTrack> fetchGpsTracks = new ArrayList<>();

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        if (getDevice().isBusy()) {
            LOG.debug("AK102 ignoring fetch request: device busy");
            return;
        }
        if (currentSyncType != -1) {
            LOG.debug("AK102 sync already in progress");
            return;
        }
        syncQueue.clear();
        fetchActivity.clear();
        fetchSpo2.clear();
        fetchStress.clear();
        fetchWorkouts.clear();
        fetchGpsTracks.clear();
        // Order mirrors the SDK's per-device sync type list for AK102.
        addSyncType(Ak102Constants.SYNC_TYPE_STEP, true);
        addSyncType(Ak102Constants.SYNC_TYPE_SLEEP, true);
        addSyncType(Ak102Constants.SYNC_TYPE_HEART_RATE, Ak102Constants.FEATURE_HEART_RATE);
        if (supportsWatchFeature(Ak102Constants.FEATURE_MEASURE_DATA_SYNCABLE)) {
            addSyncType(Ak102Constants.SYNC_TYPE_HEART_RATE_MEASURE, Ak102Constants.FEATURE_HEART_RATE);
        }
        addSyncType(Ak102Constants.SYNC_TYPE_OXYGEN, Ak102Constants.FEATURE_OXYGEN);
        if (supportsWatchFeature(Ak102Constants.FEATURE_MEASURE_DATA_SYNCABLE)) {
            addSyncType(Ak102Constants.SYNC_TYPE_OXYGEN_MEASURE, Ak102Constants.FEATURE_OXYGEN);
        }
        addSyncType(Ak102Constants.SYNC_TYPE_SPORT, Ak102Constants.FEATURE_SPORT);
        if (supportsWatchFeature(Ak102Constants.FEATURE_GPS)) {
            addSyncType(Ak102Constants.SYNC_TYPE_GPS, true);
        }
        addSyncType(Ak102Constants.SYNC_TYPE_PRESSURE, Ak102Constants.FEATURE_PRESSURE);
        if (supportsWatchFeature(Ak102Constants.FEATURE_MEASURE_DATA_SYNCABLE)) {
            addSyncType(Ak102Constants.SYNC_TYPE_PRESSURE_MEASURE, Ak102Constants.FEATURE_PRESSURE);
        }
        if (syncQueue.isEmpty()) {
            LOG.info("AK102 nothing to sync");
            return;
        }
        getDevice().setBusyTask(R.string.busy_task_fetch_activity_data, getContext());
        getDevice().sendDeviceUpdateIntent(getContext());
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 0, getContext());
        // Resting HR uses a separate command; request it alongside the sync.
        if (supportsWatchFeature(Ak102Constants.FEATURE_HEART_RATE_RESTING)) {
            sendSimpleCommand("ak102-resting-hr", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_RESTING_HR_REQUEST, null);
        }
        // Today's running totals (single response, not part of the k1 flow).
        sendSimpleCommand("ak102-today", Ak102Constants.CMD_DATA_SYNC, Ak102Constants.KEY_TODAY_TOTAL_REQUEST, null);
        startNextSyncType();
    }

    private void addSyncType(final int type, final boolean enabled) {
        if (enabled) {
            syncQueue.add(type);
        }
    }

    private void addSyncType(final int type, final int gateFeature) {
        addSyncType(type, supportsWatchFeature(gateFeature));
    }

    private void startNextSyncType() {
        if (syncQueue.isEmpty()) {
            finishFetch();
            return;
        }
        currentSyncType = syncQueue.remove(0);
        syncStarted = false;
        syncData.reset();
        LOG.debug("AK102 syncing type {}", currentSyncType);
        sendSimpleCommand("ak102-sync", Ak102Constants.CMD_DATA_SYNC, Ak102Constants.KEY_SYNC_REQUEST,
                new byte[]{(byte) currentSyncType});
    }

    private void handleSyncPacket(final byte keyId, final byte[] keyData) {
        switch (keyId) {
            case Ak102Constants.KEY_SYNC_START_ACK:
                syncStarted = true;
                return;
            case Ak102Constants.KEY_SYNC_CHUNK:
                if (syncStarted) {
                    syncData.write(keyData, 0, keyData.length);
                }
                return;
            case Ak102Constants.KEY_SYNC_FINISH:
                handleSyncFinish(keyData);
                return;
            case Ak102Constants.KEY_TODAY_TOTAL_RESPONSE:
                handleTodayTotal(keyData);
                return;
            case Ak102Constants.KEY_REALTIME_RESPONSE:
                handleRealtime(keyData);
                return;
            case Ak102Constants.KEY_REALTIME_TEMP_ERROR:
                return;
            default:
                LOG.debug("AK102 RX unhandled sync key={} data={}", keyId, GB.hexdump(keyData));
        }
    }

    private void handleSyncFinish(final byte[] keyData) {
        if (currentSyncType == -1) {
            // Stray finish after the queue already drained.
            return;
        }
        final byte[] buffer = syncData.toByteArray();
        final int declared = keyData != null && keyData.length == 4
                ? ((keyData[0] & 0xFF) << 24) | ((keyData[1] & 0xFF) << 16)
                | ((keyData[2] & 0xFF) << 8) | (keyData[3] & 0xFF)
                : -1;
        final boolean ok = declared == buffer.length;
        LOG.info("AK102 sync type {} finished: {} bytes (declared {})",
                currentSyncType, buffer.length, declared);
        // Acknowledge: [0] on success, [1] on length mismatch.
        sendSimpleCommand("ak102-sync-ack", Ak102Constants.CMD_DATA_SYNC, Ak102Constants.KEY_SYNC_ACK,
                new byte[]{(byte) (ok ? 0 : 1)});
        if (ok) {
            try {
                decodeSyncBuffer(currentSyncType, buffer);
            } catch (final Exception e) {
                LOG.error("AK102 failed to decode sync type {}", currentSyncType, e);
            }
        } else {
            LOG.warn("AK102 sync type {} length mismatch (declared={}, got={})",
                    currentSyncType, declared, buffer.length);
        }
        startNextSyncType();
    }

    private void decodeSyncBuffer(final int type, final byte[] buffer) {
        if (buffer.length > 0 && buffer.length <= 200) {
            LOG.debug("AK102 sync type {} buffer: {}", type, GB.hexdump(buffer));
        }
        switch (type) {
            case Ak102Constants.SYNC_TYPE_STEP:
                mergeActivity(Ak102SyncParser.parseSteps(buffer,
                        supportsWatchFeature(Ak102Constants.FEATURE_STEP_EXTRA),
                        supportsWatchFeature(Ak102Constants.FEATURE_ACTIVITY_SPORT_DURATION)));
                return;
            case Ak102Constants.SYNC_TYPE_HEART_RATE:
                mergeActivity(Ak102SyncParser.parseHeartRate(buffer));
                return;
            case Ak102Constants.SYNC_TYPE_HEART_RATE_MEASURE:
                mergeActivity(Ak102SyncParser.parseHeartRateMeasure(buffer));
                return;
            case Ak102Constants.SYNC_TYPE_SLEEP:
                mergeActivity(Ak102SyncParser.parseSleep(buffer,
                        supportsWatchFeature(Ak102Constants.FEATURE_NEW_SLEEP_PROTOCOL),
                        supportsWatchFeature(Ak102Constants.FEATURE_SLEEP_REM)));
                return;
            case Ak102Constants.SYNC_TYPE_OXYGEN:
                fetchSpo2.addAll(Ak102SyncParser.parseOxygen(buffer));
                return;
            case Ak102Constants.SYNC_TYPE_OXYGEN_MEASURE:
                fetchSpo2.addAll(Ak102SyncParser.parseOxygenMeasure(buffer));
                return;
            case Ak102Constants.SYNC_TYPE_PRESSURE:
                fetchStress.addAll(Ak102SyncParser.parsePressure(buffer));
                return;
            case Ak102Constants.SYNC_TYPE_PRESSURE_MEASURE:
                fetchStress.addAll(Ak102SyncParser.parsePressureMeasure(buffer));
                return;
            case Ak102Constants.SYNC_TYPE_SPORT:
                // Buffer; workouts are correlated with GPS and persisted at fetch end.
                final List<Ak102SyncParser.Workout> workouts = Ak102SyncParser.parseSport(buffer,
                        supportsWatchFeature(Ak102Constants.FEATURE_DYNAMIC_HEART_RATE) ? 1 : 0,
                        supportsWatchFeature(Ak102Constants.FEATURE_GPS));
                for (final Ak102SyncParser.Workout w : workouts) {
                    // Store the workout HR series so the charts can draw it.
                    mergeActivity(w.hrPoints);
                }
                fetchWorkouts.addAll(workouts);
                return;
            case Ak102Constants.SYNC_TYPE_GPS:
                fetchGpsTracks.addAll(Ak102SyncParser.parseGps(buffer));
                return;
            default:
                LOG.debug("AK102 no decoder for sync type {}", type);
        }
    }

    private void finishFetch() {
        currentSyncType = -1;
        LOG.info("AK102 fetch results: {} activity buckets, {} spo2, {} stress, {} workouts, {} gps tracks",
                fetchActivity.size(), fetchSpo2.size(), fetchStress.size(),
                fetchWorkouts.size(), fetchGpsTracks.size());
        persistActivity(fetchActivity.values());
        persistSpo2(fetchSpo2);
        persistStress(fetchStress);
        storeWorkouts(fetchWorkouts, fetchGpsTracks);
        fetchActivity.clear();
        fetchSpo2.clear();
        fetchStress.clear();
        fetchWorkouts.clear();
        fetchGpsTracks.clear();
        if (getDevice().isBusy()) {
            getDevice().unsetBusyTask();
            getDevice().sendDeviceUpdateIntent(getContext());
        }
        GB.updateTransferNotification(null, "", false, 100, getContext());
        GB.signalActivityDataFinish(getDevice());
        LOG.info("AK102 sync complete");
    }

    private void mergeActivity(final List<Ak102SyncParser.ActivityPoint> points) {
        for (final Ak102SyncParser.ActivityPoint p : points) {
            final Ak102SyncParser.ActivityPoint existing = fetchActivity.get(p.timestamp);
            if (existing == null) {
                fetchActivity.put(p.timestamp, p);
            } else {
                existing.steps += p.steps;
                existing.distanceCm += p.distanceCm;
                existing.activeCalories += p.activeCalories;
                if (p.heartRate > 0) {
                    existing.heartRate = p.heartRate;
                }
                if (p.rawKind != ActivityKind.UNKNOWN.getCode()) {
                    existing.rawKind = p.rawKind;
                }
            }
        }
    }

    private void handleTodayTotal(final byte[] keyData) {
        // The watch answers with two k34 packets: the >=24B totals blob and an
        // 8B timestamp. dispatch() already split them, so parse directly.
        final int[] totals = Ak102SyncParser.parseTodayTotal(keyData);
        if (totals == null) {
            LOG.debug("AK102 today-total aux packet: {}", GB.hexdump(keyData));
            return;
        }
        LOG.info("AK102 today: steps={} distance={}m calories={} hr={}",
                totals[0], totals[1], totals[2], totals[3]);
        if (realtimeStepsEnabled) {
            broadcastRealtimeSteps(totals[0]);
        }
    }

    // Live activity expects step deltas; diff against the previous poll.
    private void broadcastRealtimeSteps(final int totalSteps) {
        final int previous = lastRealtimeTotalSteps;
        lastRealtimeTotalSteps = totalSteps;
        if (previous < 0 || totalSteps <= previous) {
            return;
        }
        final Ak102ActivitySample sample = new Ak102ActivitySample();
        sample.setTimestamp((int) (System.currentTimeMillis() / 1000L));
        sample.setSteps(totalSteps - previous);
        sample.setDistanceCm(ActivitySample.NOT_MEASURED);
        sample.setActiveCalories(ActivitySample.NOT_MEASURED);
        sample.setRawKind(ActivityKind.ACTIVITY.getCode());
        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
        sample.setHeartRate(ActivitySample.NOT_MEASURED);
        final Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(GBDevice.EXTRA_DEVICE, getDevice())
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    // Database storage -------------------------------------------------------

    private void storeActivityPoints(final List<Ak102SyncParser.ActivityPoint> points) {
        persistActivity(points);
    }

    private void persistActivity(final Collection<Ak102SyncParser.ActivityPoint> points) {
        if (points.isEmpty()) {
            return;
        }
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            final Ak102SampleProvider provider = new Ak102SampleProvider(getDevice(), session);
            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);
            final List<Ak102ActivitySample> samples = new ArrayList<>(points.size());
            for (final Ak102SyncParser.ActivityPoint p : points) {
                final Ak102ActivitySample sample = new Ak102ActivitySample();
                sample.setTimestamp(p.timestamp);
                sample.setDeviceId(device.getId());
                sample.setUserId(user.getId());
                sample.setProvider(provider);
                sample.setSteps(p.steps);
                sample.setDistanceCm(p.distanceCm);
                sample.setActiveCalories(p.activeCalories);
                sample.setRawKind(p.rawKind);
                sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                sample.setHeartRate(p.heartRate > 0
                        ? p.heartRate
                        : ActivitySample.NOT_MEASURED);
                samples.add(sample);
            }
            provider.addGBActivitySamples(samples);
            LOG.debug("AK102 stored {} activity samples", samples.size());
        } catch (final Exception e) {
            LOG.error("AK102 failed to store activity samples", e);
        }
    }

    private void persistSpo2(final List<Ak102SyncParser.ValuePoint> points) {
        if (points.isEmpty()) {
            return;
        }
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            final GenericSpo2SampleProvider provider = new GenericSpo2SampleProvider(getDevice(), session);
            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);
            final List<GenericSpo2Sample> samples = new ArrayList<>(points.size());
            for (final Ak102SyncParser.ValuePoint p : points) {
                samples.add(new GenericSpo2Sample(p.timestampMillis, device.getId(),
                        user.getId(), p.value));
            }
            provider.addSamples(samples);
            LOG.debug("AK102 stored {} SpO2 samples", samples.size());
        } catch (final Exception e) {
            LOG.error("AK102 failed to store SpO2 samples", e);
        }
    }

    private void persistStress(final List<Ak102SyncParser.ValuePoint> points) {
        if (points.isEmpty()) {
            return;
        }
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            final GenericStressSampleProvider provider = new GenericStressSampleProvider(getDevice(), session);
            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);
            final List<GenericStressSample> samples = new ArrayList<>(points.size());
            for (final Ak102SyncParser.ValuePoint p : points) {
                samples.add(new GenericStressSample(p.timestampMillis, device.getId(),
                        user.getId(), p.value));
            }
            provider.addSamples(samples);
            LOG.debug("AK102 stored {} stress samples", samples.size());
        } catch (final Exception e) {
            LOG.error("AK102 failed to store stress samples", e);
        }
    }

    private void storeWorkouts(final List<Ak102SyncParser.Workout> workouts,
                               final List<Ak102SyncParser.GpsTrack> gpsTracks) {
        if (workouts.isEmpty()) {
            return;
        }
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);
            for (final Ak102SyncParser.Workout w : workouts) {
                final BaseActivitySummary summary = new BaseActivitySummary();
                summary.setStartTime(new Date(w.startMillis));
                summary.setEndTime(new Date(w.startMillis + w.durationSeconds * 1000L));
                summary.setActivityKind(mapSportKind(w.sportId).getCode());
                summary.setName("Workout");
                summary.setDevice(device);
                summary.setUser(user);

                final Ak102SyncParser.GpsTrack track = matchGpsTrack(w, gpsTracks);
                if (track != null && !track.points.isEmpty()) {
                    try {
                        final String path = writeGpx(device, user, w, track);
                        summary.setGpxTrack(path);
                    } catch (final Exception e) {
                        LOG.warn("AK102 GPX export failed for workout at {}", w.startMillis, e);
                    }
                }

                final ActivitySummaryData data = new ActivitySummaryData();
                data.add(null, ActivitySummaryEntries.ACTIVE_SECONDS, w.durationSeconds, ActivitySummaryEntries.UNIT_SECONDS);
                data.add(null, ActivitySummaryEntries.DISTANCE_METERS, w.distanceMeters, ActivitySummaryEntries.UNIT_METERS);
                data.add(null, ActivitySummaryEntries.CALORIES_BURNT, w.caloriesKcal, ActivitySummaryEntries.UNIT_KCAL);
                data.add(null, ActivitySummaryEntries.STEPS, w.steps, ActivitySummaryEntries.UNIT_STEPS);
                data.add(null, ActivitySummaryEntries.HR_AVG, w.avgHeartRate, ActivitySummaryEntries.UNIT_BPM);
                addHeartRateStats(data, w);
                addDerivedStats(data, w);
                addElevationStats(data, track);
                // The workout list's GPS icon reads this flag from the JSON.
                data.setHasGps(summary.getGpxTrack() != null);
                summary.setSummaryData(data.toJson());

                session.getBaseActivitySummaryDao().insertOrReplace(summary);
                LOG.info("AK102 stored workout sportId={} kind={} dur={}s dist={}m gps={}",
                        w.sportId, mapSportKind(w.sportId), w.durationSeconds, w.distanceMeters,
                        track != null ? track.points.size() : 0);
            }
        } catch (final Exception e) {
            LOG.error("AK102 failed to store workouts", e);
        }
    }

    // Max/min heart rate from the per-record workout HR series.
    private static void addHeartRateStats(final ActivitySummaryData data,
                                          final Ak102SyncParser.Workout w) {
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (final Ak102SyncParser.ActivityPoint p : w.hrPoints) {
            if (p.heartRate > 0) {
                max = Math.max(max, p.heartRate);
                min = Math.min(min, p.heartRate);
            }
        }
        if (max > 0) {
            data.add(null, ActivitySummaryEntries.HR_MAX, max, ActivitySummaryEntries.UNIT_BPM);
            data.add(null, ActivitySummaryEntries.HR_MIN, min, ActivitySummaryEntries.UNIT_BPM);
        }
    }

    /*
     * Averages the watch itself derives for its workout screen: speed and
     * pace from distance+duration, step rate (cadence) from steps+duration,
     * stride length from distance+steps.
     */
    private static void addDerivedStats(final ActivitySummaryData data,
                                        final Ak102SyncParser.Workout w) {
        if (w.durationSeconds > 0 && w.distanceMeters > 0) {
            data.add(null, ActivitySummaryEntries.SPEED_AVG,
                    w.distanceMeters / (float) w.durationSeconds,
                    ActivitySummaryEntries.UNIT_METERS_PER_SECOND);
            data.add(null, ActivitySummaryEntries.PACE_AVG_SECONDS_KM,
                    (int) (w.durationSeconds * 1000L / w.distanceMeters),
                    ActivitySummaryEntries.UNIT_SECONDS_PER_KM);
        }
        if (w.durationSeconds > 0 && w.steps > 0) {
            data.add(null, ActivitySummaryEntries.STEP_RATE_AVG,
                    Math.round(w.steps * 60f / w.durationSeconds),
                    ActivitySummaryEntries.UNIT_SPM);
        }
        int maxCadence = 0;
        for (final Ak102SyncParser.ValuePoint p : w.cadencePoints) {
            maxCadence = Math.max(maxCadence, p.value);
        }
        if (maxCadence > 0) {
            data.add(null, ActivitySummaryEntries.STEP_RATE_MAX, maxCadence,
                    ActivitySummaryEntries.UNIT_SPM);
        }
        if (w.steps > 0 && w.distanceMeters > 0) {
            data.add(null, ActivitySummaryEntries.STRIDE_AVG,
                    Math.round(w.distanceMeters * 100f / w.steps),
                    ActivitySummaryEntries.UNIT_CM);
        }
    }

    /*
     * Elevation gain/loss/min/max from barometric altitudes in the GPS track.
     * A 3 m hysteresis filters sensor noise out of the gain/loss sums.
     */
    private static void addElevationStats(final ActivitySummaryData data,
                                          final Ak102SyncParser.GpsTrack track) {
        if (track == null || track.points.isEmpty()) {
            return;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double gain = 0;
        double loss = 0;
        double reference = track.points.get(0).altitude;
        for (final Ak102SyncParser.GpsPoint p : track.points) {
            min = Math.min(min, p.altitude);
            max = Math.max(max, p.altitude);
            final double delta = p.altitude - reference;
            if (delta >= 3) {
                gain += delta;
                reference = p.altitude;
            } else if (delta <= -3) {
                loss -= delta;
                reference = p.altitude;
            }
        }
        data.add(null, ActivitySummaryEntries.ELEVATION_GAIN, (int) gain, ActivitySummaryEntries.UNIT_METERS);
        data.add(null, ActivitySummaryEntries.ELEVATION_LOSS, (int) loss, ActivitySummaryEntries.UNIT_METERS);
        data.add(null, ActivitySummaryEntries.ALTITUDE_MIN, (int) min, ActivitySummaryEntries.UNIT_METERS);
        data.add(null, ActivitySummaryEntries.ALTITUDE_MAX, (int) max, ActivitySummaryEntries.UNIT_METERS);
    }

    // Nearest GPS track whose base time is within 3 minutes of the workout start.
    private static Ak102SyncParser.GpsTrack matchGpsTrack(final Ak102SyncParser.Workout workout,
                                                          final List<Ak102SyncParser.GpsTrack> tracks) {
        Ak102SyncParser.GpsTrack best = null;
        long bestDelta = Long.MAX_VALUE;
        for (final Ak102SyncParser.GpsTrack track : tracks) {
            final long delta = Math.abs(track.baseMillis - workout.startMillis);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = track;
            }
        }
        return bestDelta <= 180_000L ? best : null;
    }

    // Export a GPS track to a GPX file and return its absolute path.
    private String writeGpx(final Device device, final User user,
                            final Ak102SyncParser.Workout workout,
                            final Ak102SyncParser.GpsTrack track) throws Exception {
        final Date startDate = new Date(workout.startMillis);
        final File dir = getDevice().getDeviceCoordinator()
                .getWritableExportDirectory(getDevice(), true);
        final File gpxFile = new File(dir,
                "ak102_" + (workout.startMillis / 1000L) + ".gpx");
        final ActivityTrack activityTrack = new ActivityTrack();
        activityTrack.setBaseTime(startDate);
        activityTrack.setName("Workout");
        activityTrack.setDevice(device);
        activityTrack.setUser(user);
        for (final Ak102SyncParser.GpsPoint p : track.points) {
            if (p.restart) {
                // Pause/GPS restart: continue in a new trkseg (no-op while the
                // current segment is still empty).
                activityTrack.startNewSegment();
            }
            final ActivityPoint point = new ActivityPoint(new Date(p.timestampMillis));
            point.setLocation(new GPSCoordinate(p.longitude, p.latitude, p.altitude));
            // Attach the workout HR and cadence series: the workout-details
            // chart plots track points in preference to activity samples, and
            // the GPX gains gpxtpx:hr/cad extensions (read by e.g. Strava).
            final int hr = Ak102SyncParser.nearestHeartRate(workout.hrPoints,
                    (int) (p.timestampMillis / 1000L), 90);
            if (hr > 0) {
                point.setHeartRate(hr);
            }
            final int cadence = Ak102SyncParser.nearestValue(workout.cadencePoints,
                    p.timestampMillis, 90_000L);
            if (cadence >= 0) {
                point.setCadence(cadence);
            }
            activityTrack.addTrackPoint(point);
        }
        new GPXExporter().performExport(activityTrack, gpxFile, null);
        return gpxFile.getAbsolutePath();
    }

    // Maps a FitCloud workout sportId to a GB ActivityKind.
    private static ActivityKind mapSportKind(final int sportId) {
        switch (sportId) {
            case 1:
                return ActivityKind.OUTDOOR_CYCLING;
            case 5:
                return ActivityKind.OUTDOOR_RUNNING;
            case 9:
                return ActivityKind.INDOOR_RUNNING;
            case 13:
                return ActivityKind.OUTDOOR_WALKING;
            case 17:
                return ActivityKind.MOUNTAINEERING;
            case 21:
                return ActivityKind.BASKETBALL;
            case 25:
                return ActivityKind.SWIMMING;
            case 41:
                return ActivityKind.YOGA;
            case 61:
                return ActivityKind.INDOOR_CYCLING; // exercise bike
            case 89:
                return ActivityKind.STRENGTH_TRAINING;
            case 93:
                return ActivityKind.INDOOR_WALKING;
            case 97:
                return ActivityKind.INDOOR_CYCLING;
            case 133:
                return ActivityKind.HIKING;
            case 153:
                return ActivityKind.TRAIL_RUN;
            case 161:
                return ActivityKind.AIR_WALKER;
            case 305:
                return ActivityKind.HANDCYCLING;
            case 465:
                return ActivityKind.SPINNING;
            case 469:
                return ActivityKind.POOL_SWIM;
            case 473:
                return ActivityKind.SWIMMING_OPENWATER;
            default:
                return ActivityKind.EXERCISE;
        }
    }

    // Realtime ---------------------------------------------------------------

    private boolean realtimeHrEnabled;
    private boolean realtimeStepsEnabled;
    private boolean singleShotHr;
    private boolean realtimeStreamOpen;
    private int lastRealtimeTotalSteps = -1;
    private final Handler realtimeHandler = new Handler(Looper.getMainLooper());
    private final Runnable realtimeStepsPoll = new Runnable() {
        @Override
        public void run() {
            if (!realtimeStepsEnabled || !isConnected()) {
                return;
            }
            sendSimpleCommand("ak102-today", Ak102Constants.CMD_DATA_SYNC,
                    Ak102Constants.KEY_TODAY_TOTAL_REQUEST, null);
            realtimeHandler.postDelayed(this, 5000L);
        }
    };

    // Device-card heart icon: one-shot measure via the realtime stream.
    @Override
    public void onHeartRateTest() {
        singleShotHr = true;
        updateRealtimeStream();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        realtimeHrEnabled = enable;
        updateRealtimeStream();
    }

    // Live activity: no realtime step stream exists, poll today-total instead.
    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        realtimeStepsEnabled = enable;
        realtimeHandler.removeCallbacks(realtimeStepsPoll);
        if (enable) {
            lastRealtimeTotalSteps = -1;
            realtimeHandler.post(realtimeStepsPoll);
        }
        updateRealtimeStream();
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            realtimeHandler.removeCallbacks(realtimeStepsPoll);
            unregisterBondStateReceiver();
            super.dispose();
        }
    }

    // Open/close the combined measure stream to match the requested state.
    private void updateRealtimeStream() {
        final boolean wantOpen = realtimeHrEnabled || singleShotHr;
        if (wantOpen == realtimeStreamOpen) {
            return;
        }
        realtimeStreamOpen = wantOpen;
        // Combined measurement (FitCloudPro "stress test"): request every
        // supported metric at once; the stream reports them side by side.
        int mask = Ak102Constants.REALTIME_HEART_RATE;
        if (supportsWatchFeature(Ak102Constants.FEATURE_OXYGEN)) {
            mask |= Ak102Constants.REALTIME_OXYGEN;
        }
        if (supportsWatchFeature(Ak102Constants.FEATURE_PRESSURE)) {
            mask |= Ak102Constants.REALTIME_PRESSURE;
        }
        setRealtimeHealth(mask, wantOpen);
    }

    private void setRealtimeHealth(final int healthType, final boolean enable) {
        // [0..1] healthType u16 BE (0 to close), [2]=5 open marker, [3]=minutes.
        final int type = enable ? healthType : 0;
        final byte[] payload = new byte[]{
                (byte) ((type >> 8) & 0xFF), (byte) (type & 0xFF), 5, 3,
        };
        sendSimpleCommand("ak102-realtime", Ak102Constants.CMD_DATA_SYNC, Ak102Constants.KEY_REALTIME_REQUEST, payload);
    }

    // Last stored realtime stress/SpO2 minute, to throttle DB writes.
    private long lastRealtimeStressMinute = -1;
    private long lastRealtimeSpo2Minute = -1;

    private void handleRealtime(final byte[] keyData) {
        // k36 stream: [0..5] echo, [6] hr, [7] spo2, [8] dbp, [9] sbp,
        // [10] respiratory; feature 10 adds [11] tempFlag + [12..15] temps;
        // feature 13 appends stress at the following byte.
        if (keyData.length < 11) {
            return;
        }
        final long nowMillis = System.currentTimeMillis();
        final int heartRate = keyData[6] & 0xFF;
        final int spo2 = keyData[7] & 0xFF;
        int cursor = 11;
        if (supportsWatchFeature(Ak102Constants.FEATURE_TEMPERATURE)) {
            cursor = 16;
        }
        int stress = 0;
        if (supportsWatchFeature(Ak102Constants.FEATURE_PRESSURE) && keyData.length > cursor) {
            stress = keyData[cursor] & 0xFF;
        }
        if (spo2 > 0 && nowMillis / 60000L != lastRealtimeSpo2Minute) {
            lastRealtimeSpo2Minute = nowMillis / 60000L;
            persistSpo2(Collections.singletonList(
                    new Ak102SyncParser.ValuePoint(nowMillis, spo2)));
        }
        if (stress > 0 && nowMillis / 60000L != lastRealtimeStressMinute) {
            lastRealtimeStressMinute = nowMillis / 60000L;
            persistStress(Collections.singletonList(
                    new Ak102SyncParser.ValuePoint(nowMillis, stress)));
        }
        if (heartRate <= 0) {
            return;
        }
        final Ak102ActivitySample sample = new Ak102ActivitySample();
        sample.setTimestamp((int) (nowMillis / 1000L));
        sample.setSteps(ActivitySample.NOT_MEASURED);
        sample.setDistanceCm(ActivitySample.NOT_MEASURED);
        sample.setActiveCalories(ActivitySample.NOT_MEASURED);
        sample.setRawKind(ActivityKind.ACTIVITY.getCode());
        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
        sample.setHeartRate(heartRate);
        final Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(GBDevice.EXTRA_DEVICE, getDevice())
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        // One-shot measure (heart icon): close the stream after the first reading.
        if (singleShotHr) {
            singleShotHr = false;
            updateRealtimeStream();
        }
    }

    // Quick reply (reject-SMS / free-form) -----------------------------------

    private void handleQuickReply(final byte keyId, final byte[] keyData) {
        final String number;
        final String message;
        if (keyId == Ak102Constants.KEY_PUSH_QUICK_REPLY_SMS) {
            // [len][text]: canned reply, no explicit number (reply to active call).
            if (keyData.length < 1) {
                return;
            }
            final int len = Math.min(keyData[0] & 0xFF, keyData.length - 1);
            message = new String(keyData, 1, len, StandardCharsets.UTF_8);
            number = null;
            final GBDeviceEventCallControl reject = new GBDeviceEventCallControl();
            reject.event = GBDeviceEventCallControl.Event.REJECT;
            evaluateGBDeviceEvent(reject);
        } else {
            // [numLen][number][contentLen][content]
            if (keyData.length < 2) {
                return;
            }
            final int numLen = Math.min(keyData[0] & 0xFF, keyData.length - 1);
            number = new String(keyData, 1, numLen, StandardCharsets.UTF_8);
            final int contentPos = 1 + numLen;
            if (contentPos >= keyData.length) {
                return;
            }
            final int contentLen = Math.min(keyData[contentPos] & 0xFF, keyData.length - contentPos - 1);
            message = new String(keyData, contentPos + 1, contentLen, StandardCharsets.UTF_8);
        }
        final GBDeviceEventNotificationControl reply = new GBDeviceEventNotificationControl();
        reply.event = GBDeviceEventNotificationControl.Event.REPLY;
        reply.phoneNumber = number;
        reply.reply = message;
        evaluateGBDeviceEvent(reply);
    }

    // Alarm read-back --------------------------------------------------------

    // One alarm slot as reported by the watch.
    static final class WatchAlarm {
        int position;
        boolean enabled;
        int repetition;
        int hour;
        int minute;
        String label;
    }

    // Decode the KEY_ALARMS_RESPONSE record list (mirror of encodeAlarm).
    static List<WatchAlarm> decodeAlarms(final byte[] data) {
        final List<WatchAlarm> out = new ArrayList<>();
        int off = 0;
        while (off < data.length) {
            final int len = data[off] & 0xFF;
            if (len < 5 || off + 1 + len > data.length) {
                break;
            }
            final WatchAlarm alarm = new WatchAlarm();
            alarm.hour = ((data[off + 2] & 1) << 4) | ((data[off + 3] & 0xF0) >> 4);
            alarm.minute = ((data[off + 3] & 0x0F) << 2) | ((data[off + 4] & 0xC0) >> 6);
            alarm.position = (data[off + 4] >> 3) & 0x07;
            alarm.enabled = (data[off + 5] & 0x80) != 0;
            alarm.repetition = data[off + 5] & 0x7F;
            alarm.label = new String(data, off + 6, len - 5, StandardCharsets.UTF_8);
            out.add(alarm);
            off += 1 + len;
        }
        return out;
    }

    // Import alarms edited on the watch into GB's alarm database.
    private void handleAlarmsReadback(final byte[] keyData) {
        final List<WatchAlarm> watchAlarms = decodeAlarms(keyData);
        LOG.info("AK102 alarms read-back: {} alarm(s)", watchAlarms.size());
        final Map<Integer, WatchAlarm> byPosition = new HashMap<>();
        for (final WatchAlarm alarm : watchAlarms) {
            byPosition.put(alarm.position, alarm);
        }
        boolean changed = false;
        // getAlarmsWithDefaults covers every slot, so alarms created on the
        // watch land in (previously) unused positions.
        for (final nodomain.freeyourgadget.gadgetbridge.entities.Alarm dbAlarm
                : DBHelper.getAlarmsWithDefaults(getDevice())) {
            final WatchAlarm watchAlarm = byPosition.get(dbAlarm.getPosition());
            if (watchAlarm == null) {
                if (!dbAlarm.getUnused()) {
                    dbAlarm.setUnused(true);
                    DBHelper.store(dbAlarm);
                    changed = true;
                }
                continue;
            }
            final String dbTitle = dbAlarm.getTitle() == null ? "" : dbAlarm.getTitle();
            if (!dbAlarm.getUnused()
                    && dbAlarm.getEnabled() == watchAlarm.enabled
                    && dbAlarm.getHour() == watchAlarm.hour
                    && dbAlarm.getMinute() == watchAlarm.minute
                    && dbAlarm.getRepetition() == watchAlarm.repetition
                    && dbTitle.equals(watchAlarm.label)) {
                continue;
            }
            dbAlarm.setUnused(false);
            dbAlarm.setEnabled(watchAlarm.enabled);
            dbAlarm.setHour(watchAlarm.hour);
            dbAlarm.setMinute(watchAlarm.minute);
            dbAlarm.setRepetition(watchAlarm.repetition);
            dbAlarm.setTitle(watchAlarm.label);
            DBHelper.store(dbAlarm);
            changed = true;
        }
        if (changed) {
            // Refresh the alarms UI if it is open.
            LocalBroadcastManager.getInstance(getContext())
                    .sendBroadcast(new Intent(DeviceService.ACTION_SAVE_ALARMS));
        }
    }

    // Volume -----------------------------------------------------------------

    private void sendVolume() {
        if (!supportsWatchFeature(Ak102Constants.FEATURE_SYNC_VOLUME_INFO)) {
            return;
        }
        final AudioManager audio = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        final int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final int current = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        sendSimpleCommand("ak102-volume", Ak102Constants.CMD_SPECIAL, Ak102Constants.KEY_SET_VOLUME,
                new byte[]{(byte) max, (byte) current});
    }

    // Device-specific settings ----------------------------------------------

    @Override
    public void onSendConfiguration(final String config) {
        LOG.info("AK102 configuration change: {}", config);
        final SharedPreferences prefs = getAk102Prefs();
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_TIMEFORMAT: {
                // Function flag 2 set = 12-hour display.
                final String value = prefs.getString(config, DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO);
                final boolean h12;
                if (DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO.equals(value)) {
                    h12 = !DateFormat.is24HourFormat(getContext());
                } else {
                    h12 = DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_12H.equals(value);
                }
                setFunctionFlag(Ak102Constants.FUNCTION_FLAG_TIME_FORMAT, h12);
                return;
            }
            case DeviceSettingsPreferenceConst.PREF_WEARLOCATION: {
                final String value = prefs.getString(config, "left");
                setFunctionFlag(Ak102Constants.FUNCTION_FLAG_WEAR_WAY, "right".equals(value));
                return;
            }
            case SettingsActivity.PREF_UNIT_DISTANCE: {
                final String value = GBApplication.getPrefs().getString(SettingsActivity.PREF_UNIT_DISTANCE, "metric");
                setFunctionFlag(Ak102Constants.FUNCTION_FLAG_LENGTH_UNIT, "imperial".equals(value));
                return;
            }
            case SettingsActivity.PREF_UNIT_TEMPERATURE: {
                final String value = GBApplication.getPrefs().getString(SettingsActivity.PREF_UNIT_TEMPERATURE, "celsius");
                setFunctionFlag(Ak102Constants.FUNCTION_FLAG_TEMPERATURE_UNIT, "fahrenheit".equals(value));
                return;
            }
            case DeviceSettingsPreferenceConst.PREF_DISCONNECTNOTIF_NOSHED:
                setFunctionFlag(Ak102Constants.FUNCTION_FLAG_DISCONNECT_REMINDER,
                        prefs.getBoolean(config, false));
                return;
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_AUTOMATIC_ENABLE:
                sendHealthMonitorConfig(prefs);
                return;
            case DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
            case DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_START:
            case DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_END:
                sendTurnWristConfig(prefs);
                return;
            case DeviceSettingsPreferenceConst.PREF_SCREEN_TIMEOUT: {
                int seconds = 5;
                try {
                    seconds = Integer.parseInt(prefs.getString(config, "5"));
                } catch (final NumberFormatException ignored) {
                }
                sendScreenGearNearest(0, seconds);
                return;
            }
            case DeviceSettingsPreferenceConst.PREF_SCREEN_BRIGHTNESS:
                sendScreenGearPercent(24, prefs.getInt(config, 50));
                return;
            case DeviceSettingsPreferenceConst.PREF_VIBRATION_STRENGH_PERCENTAGE:
                sendScreenGearPercent(31, prefs.getInt(config, 50));
                return;
            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB:
            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_START:
            case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_END:
                sendDndConfig(prefs);
                return;
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_START:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_END:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND:
                sendSedentaryConfig(prefs);
                return;
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_PERIOD:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_START:
            case DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_END:
                sendDrinkWaterConfig(prefs);
                return;
            case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
                sendLanguage(prefs);
                return;
            case DeviceSettingsPreferenceConst.PREF_POWER_SAVING:
                sendPowerSave(prefs);
                return;
            case Ak102Constants.PREF_DISPLAY_ITEMS:
                sendPageConfig(prefs);
                return;
            case Ak102Constants.PREF_AUDIO_PAIR:
                pairAudio();
                return;
            case Ak102Constants.PREF_AUDIO_UNPAIR:
                unpairAudio();
                return;
            case Ak102Constants.PREF_AUDIO_AUTO_CONNECT:
                if (prefs.getBoolean(config, false) && getBondState() == BluetoothDevice.BOND_BONDED) {
                    connectAudioProfiles();
                }
                return;
            default:
                LOG.debug("AK102 unhandled configuration change: {}", config);
        }
    }

    private static int minuteOfDay(final SharedPreferences prefs, final String key, final String fallback) {
        final String value = prefs.getString(key, fallback);
        final String[] parts = value.split(":");
        try {
            return (Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1])) % 1440;
        } catch (final Exception e) {
            return 0;
        }
    }

    private static void putMinuteOfDay(final byte[] bytes, final int offset, final int minute) {
        bytes[offset] = (byte) ((minute >> 8) & 0xFF);
        bytes[offset + 1] = (byte) (minute & 0xFF);
    }

    private void sendConfigItem(final String taskName, final byte type, final byte[] bytes) {
        configCache.put(type, bytes);
        getAk102Prefs().edit()
                .putString(Ak102Constants.PREF_CONFIG_PREFIX + (type & 0xFF), GB.hexdump(bytes))
                .apply();
        // Config writes use a distinct key (type-2), not the TLV type itself.
        sendSimpleCommand(taskName, Ak102Constants.CMD_SETTINGS, Ak102Constants.configWriteKey(type), bytes);
    }

    // TurnWristLightingConfig (45, 9B): [en][startMoD u16][endMoD u16].
    private void sendTurnWristConfig(final SharedPreferences prefs) {
        final String mode = prefs.getString(DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, "off");
        final byte[] bytes = new byte[9];
        if ("scheduled".equals(mode)) {
            bytes[0] = 1;
            putMinuteOfDay(bytes, 1, minuteOfDay(prefs, DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_START, "00:00"));
            putMinuteOfDay(bytes, 3, minuteOfDay(prefs, DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_END, "00:00"));
        } else if ("on".equals(mode)) {
            bytes[0] = 1;
            putMinuteOfDay(bytes, 1, 0);
            putMinuteOfDay(bytes, 3, 1439);
        }
        sendConfigItem("ak102-turnwrist", Ak102Constants.KEY_SET_CONFIG_TURN_WRIST, bytes);
    }

    // DNDConfig (78, 6B): [allDay][scheduled][startMoD u16][endMoD u16].
    private void sendDndConfig(final SharedPreferences prefs) {
        final String mode = prefs.getString(DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB,
                DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_OFF);
        final byte[] bytes = new byte[6];
        bytes[0] = (byte) (DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_ALWAYS.equals(mode) ? 1 : 0);
        bytes[1] = (byte) (DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_SCHEDULED.equals(mode) ? 1 : 0);
        putMinuteOfDay(bytes, 2, minuteOfDay(prefs, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_START, "22:00"));
        putMinuteOfDay(bytes, 4, minuteOfDay(prefs, DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_END, "06:00"));
        sendConfigItem("ak102-dnd", Ak102Constants.KEY_SET_CONFIG_DND, bytes);
    }

    // SedentaryConfig (39, 7B): [b0:en|dnd][startMoD u16][endMoD u16][interval u16].
    private void sendSedentaryConfig(final SharedPreferences prefs) {
        final byte[] bytes = new byte[7];
        int flags = 0;
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, false)) {
            flags |= 1;
        }
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND, false)) {
            flags |= 2; // Midday break (watch-defined window).
        }
        bytes[0] = (byte) flags;
        putMinuteOfDay(bytes, 1, minuteOfDay(prefs, DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, "08:00"));
        putMinuteOfDay(bytes, 3, minuteOfDay(prefs, DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, "20:00"));
        int interval = 60;
        try {
            interval = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, "60"));
        } catch (final NumberFormatException ignored) {
        }
        bytes[5] = (byte) ((interval >> 8) & 0xFF);
        bytes[6] = (byte) (interval & 0xFF);
        sendConfigItem("ak102-sedentary", Ak102Constants.KEY_SET_CONFIG_SEDENTARY, bytes);
    }

    // DrinkWaterConfig (42, 9B): [en][interval u16][startMoD u16][endMoD u16][0][0].
    private void sendDrinkWaterConfig(final SharedPreferences prefs) {
        final byte[] bytes = new byte[9];
        bytes[0] = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYDRATION_SWITCH, false) ? 1 : 0);
        int interval = 60;
        try {
            interval = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_HYDRATION_PERIOD, "60"));
        } catch (final NumberFormatException ignored) {
        }
        bytes[1] = (byte) ((interval >> 8) & 0xFF);
        bytes[2] = (byte) (interval & 0xFF);
        putMinuteOfDay(bytes, 3, minuteOfDay(prefs, DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_START, "08:00"));
        putMinuteOfDay(bytes, 5, minuteOfDay(prefs, DeviceSettingsPreferenceConst.PREF_HYDRATION_REMINDER_END, "20:00"));
        sendConfigItem("ak102-drink", Ak102Constants.KEY_SET_CONFIG_DRINK_WATER, bytes);
    }

    /*
     * ScreenVibrateConfig (122, 38B) section update. Sections advertise their
     * supported values ("gears"): [start]=current, [start+1]=count,
     * [start+2..]=values. Writes the full config back.
     */
    private byte[] screenVibrateGears(final int sectionStart) {
        final byte[] cfg = configCache.get(Ak102Constants.KEY_SET_CONFIG_SCREEN_VIBRATE);
        if (cfg == null || cfg.length < sectionStart + 2) {
            LOG.warn("AK102 no ScreenVibrateConfig cached; ignoring setting");
            return null;
        }
        final int count = cfg[sectionStart + 1] & 0xFF;
        if (count <= 0 || cfg.length < sectionStart + 2 + count) {
            LOG.warn("AK102 ScreenVibrateConfig section {} unsupported", sectionStart);
            return null;
        }
        return cfg;
    }

    // Choose the gear closest to the requested raw value (e.g. seconds).
    private void sendScreenGearNearest(final int sectionStart, final int requested) {
        final byte[] cfg = screenVibrateGears(sectionStart);
        if (cfg == null) {
            return;
        }
        final int count = cfg[sectionStart + 1] & 0xFF;
        int best = cfg[sectionStart + 2] & 0xFF;
        for (int i = 1; i < count; i++) {
            final int gear = cfg[sectionStart + 2 + i] & 0xFF;
            if (Math.abs(gear - requested) < Math.abs(best - requested)) {
                best = gear;
            }
        }
        cfg[sectionStart] = (byte) best;
        sendConfigItem("ak102-screenvibe", Ak102Constants.KEY_SET_CONFIG_SCREEN_VIBRATE, cfg);
    }

    // Map a 0..100 percentage onto the gear index scale.
    private void sendScreenGearPercent(final int sectionStart, final int percent) {
        final byte[] cfg = screenVibrateGears(sectionStart);
        if (cfg == null) {
            return;
        }
        final int count = cfg[sectionStart + 1] & 0xFF;
        final int index = Math.min(count - 1, Math.max(0, Math.round(percent * (count - 1) / 100f)));
        cfg[sectionStart] = cfg[sectionStart + 2 + index];
        sendConfigItem("ak102-screenvibe", Ak102Constants.KEY_SET_CONFIG_SCREEN_VIBRATE, cfg);
    }

    // Language push: c2/k58 [type][0][0][0].
    private void sendLanguage(final SharedPreferences prefs) {
        final String language = prefs.getString(DeviceSettingsPreferenceConst.PREF_LANGUAGE, "auto");
        final byte type = Ak102Constants.languageByte(
                "auto".equals(language) ? Locale.getDefault().toString() : language);
        sendSimpleCommand("ak102-language", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_SET_LANGUAGE,
                new byte[]{type, 0, 0, 0});
    }

    // Power save: c2/k169 [en] (+ schedule window with feature 303).
    private void sendPowerSave(final SharedPreferences prefs) {
        if (!supportsWatchFeature(Ak102Constants.FEATURE_POWER_SAVE)) {
            return;
        }
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_POWER_SAVING, false);
        final byte[] bytes;
        if (supportsWatchFeature(Ak102Constants.FEATURE_POWER_SAVE_SCHEDULE)) {
            bytes = new byte[5];
            bytes[0] = (byte) (enabled ? 1 : 0);
            putMinuteOfDay(bytes, 1, 0);
            putMinuteOfDay(bytes, 3, 1439);
        } else {
            bytes = new byte[]{(byte) (enabled ? 1 : 0)};
        }
        sendSimpleCommand("ak102-powersave", Ak102Constants.CMD_SETTINGS, Ak102Constants.KEY_POWER_SAVE_WRITE, bytes);
    }

    // PageConfig (23, 2B flag set): rebuild from the multiselect and send.
    private void sendPageConfig(final SharedPreferences prefs) {
        final Set<String> selected = prefs.getStringSet(
                Ak102Constants.PREF_DISPLAY_ITEMS, Collections.emptySet());
        final byte[] cached = configCache.get(Ak102Constants.KEY_SET_CONFIG_PAGE);
        final byte[] bytes = new byte[cached != null && cached.length >= 2 ? cached.length : 2];
        setFlagDescending(bytes, 0, true); // TIME page is always on.
        for (final String flag : selected) {
            try {
                setFlagDescending(bytes, Integer.parseInt(flag), true);
            } catch (final NumberFormatException ignored) {
            }
        }
        sendConfigItem("ak102-pages", Ak102Constants.KEY_SET_CONFIG_PAGE, bytes);
    }

    // FlagUtil descending scheme writer (mirror of getFunctionFlag).
    private static void setFlagDescending(final byte[] bytes, final int flag, final boolean enabled) {
        final int index = (bytes.length - 1) - flag / 8;
        if (index < 0 || index >= bytes.length) {
            return;
        }
        final int mask = 1 << (flag % 8);
        if (enabled) {
            bytes[index] |= mask;
        } else {
            bytes[index] &= ~mask;
        }
    }

    // Set one FunctionConfig (type 26, size 2) flag and resend it.
    private void setFunctionFlag(final int flag, final boolean enabled) {
        byte[] bytes = configCache.get(Ak102Constants.KEY_SET_CONFIG_FUNCTION);
        if (bytes == null || bytes.length < 2) {
            bytes = new byte[2];
        }
        setFlagDescending(bytes, flag, enabled);
        sendConfigItem("ak102-fn-config", Ak102Constants.KEY_SET_CONFIG_FUNCTION, bytes);
    }

    /*
     * Rebuild HealthMonitorConfig (type 36) from prefs and resend it.
     * 7B `[en][startMoD u16][endMoD u16][interval u16]` when the watch has
     * feature 274, else legacy 5B without the interval (firmware default).
     */
    private void sendHealthMonitorConfig(final SharedPreferences prefs) {
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_AUTOMATIC_ENABLE, false);
        final boolean hasInterval = supportsWatchFeature(Ak102Constants.FEATURE_HEALTH_MONITOR_INTERVAL);
        final byte[] bytes = new byte[hasInterval ? 7 : 5];
        bytes[0] = (byte) (enabled ? 1 : 0);
        // Measure window: all day (00:00 .. 23:59) in minute-of-day, u16 BE.
        bytes[1] = 0;
        bytes[2] = 0;
        bytes[3] = (byte) ((1439 >> 8) & 0xFF);
        bytes[4] = (byte) (1439 & 0xFF);
        if (hasInterval) {
            // Preserve the watch's configured interval; default 60 min (5..720).
            final byte[] cached = configCache.get(Ak102Constants.KEY_SET_CONFIG_HEALTH_MONITOR);
            int intervalMinutes = 60;
            if (cached != null && cached.length >= 7) {
                final int existing = ((cached[5] & 0xFF) << 8) | (cached[6] & 0xFF);
                if (existing > 0) {
                    intervalMinutes = existing;
                }
            }
            bytes[5] = (byte) ((intervalMinutes >> 8) & 0xFF);
            bytes[6] = (byte) (intervalMinutes & 0xFF);
        }
        sendConfigItem("ak102-hr-monitor", Ak102Constants.KEY_SET_CONFIG_HEALTH_MONITOR, bytes);
    }

    // Bluetooth Classic call audio (phone-side bond, mirrors FitCloudPro) -----

    private BroadcastReceiver bondStateReceiver;
    private boolean audioPairRequested;

    // GB requests BLUETOOTH_CONNECT centrally; same suppression as BondingUtil.
    @SuppressLint("MissingPermission")
    private int getBondState() {
        return getBluetoothAdapter().getRemoteDevice(getDevice().getAddress()).getBondState();
    }

    // Bond the classic side; profiles connect once the bond completes.
    @SuppressLint("MissingPermission")
    private void pairAudio() {
        final BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(getDevice().getAddress());
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            connectAudioProfiles();
            return;
        }
        audioPairRequested = true;
        registerBondStateReceiver();
        boolean started;
        try {
            // Hidden createBond(int): 1 = TRANSPORT_BREDR (same trick as the vendor app).
            started = (Boolean) BluetoothDevice.class.getMethod("createBond", int.class)
                    .invoke(device, 1);
        } catch (final Exception e) {
            started = device.createBond();
        }
        LOG.info("AK102 audio bond started: {}", started);
        GB.toast(getContext().getString(R.string.ak102_audio_pairing_started), 3, GB.INFO);
    }

    // Remove the classic bond (reflective removeBond, like the vendor app).
    private void unpairAudio() {
        final BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(getDevice().getAddress());
        try {
            BluetoothDevice.class.getMethod("removeBond").invoke(device);
            GB.toast(getContext().getString(R.string.ak102_audio_unpaired), 3, GB.INFO);
        } catch (final Exception e) {
            LOG.warn("AK102 removeBond failed", e);
        }
    }

    // Ask the OS to connect HFP + A2DP to the (bonded) watch.
    private void connectAudioProfiles() {
        final BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(getDevice().getAddress());
        for (final int profile : new int[]{BluetoothProfile.HEADSET, BluetoothProfile.A2DP}) {
            getBluetoothAdapter().getProfileProxy(getContext(), new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(final int profileId, final BluetoothProfile proxy) {
                    try {
                        // BluetoothProfile#connect(BluetoothDevice) is hidden API.
                        proxy.getClass().getMethod("connect", BluetoothDevice.class).invoke(proxy, device);
                        LOG.info("AK102 audio profile {} connect requested", profileId);
                    } catch (final Exception e) {
                        LOG.warn("AK102 audio profile {} connect failed", profileId, e);
                    }
                    getBluetoothAdapter().closeProfileProxy(profileId, proxy);
                }

                @Override
                public void onServiceDisconnected(final int profileId) {
                }
            }, profile);
        }
    }

    // Connects the audio profiles as soon as a requested bond completes.
    private void registerBondStateReceiver() {
        if (bondStateReceiver != null) {
            return;
        }
        bondStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (device == null || !device.getAddress().equalsIgnoreCase(getDevice().getAddress())) {
                    return;
                }
                if (state == BluetoothDevice.BOND_BONDED && audioPairRequested) {
                    audioPairRequested = false;
                    LOG.info("AK102 audio bond complete, connecting profiles");
                    GB.toast(getContext().getString(R.string.ak102_audio_connected), 3, GB.INFO);
                    connectAudioProfiles();
                }
            }
        };
        getContext().registerReceiver(bondStateReceiver,
                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    private void unregisterBondStateReceiver() {
        if (bondStateReceiver != null) {
            try {
                getContext().unregisterReceiver(bondStateReceiver);
            } catch (final IllegalArgumentException ignored) {
            }
            bondStateReceiver = null;
        }
    }

    // Contacts ----------------------------------------------------------------

    /*
     * Replace-all contacts push (c2/k84). Payload: [0]=total count, then per
     * contact `[index][numLen][number <=20B][nameLen][name <=32B UTF-8]`,
     * chunked at 277 bytes; every chunk repeats the total in byte 0.
     * An empty list sends a single zero byte, clearing the watch list.
     */
    @Override
    public void onSetContacts(final ArrayList<? extends Contact> contacts) {
        final List<byte[]> chunks = new ArrayList<>();
        final byte[] buffer = new byte[277];
        int used = 1;
        int count = 0;
        if (contacts != null) {
            for (final Contact contact : contacts) {
                if (contact.getName() == null || contact.getName().isEmpty()
                        || contact.getNumber() == null || contact.getNumber().isEmpty()) {
                    continue;
                }
                final byte[] number = truncateUtf8(
                        contact.getNumber().replace("(", "").replace(")", ""), 20);
                final byte[] name = truncateUtf8(contact.getName(), 32);
                final int recordLen = 3 + number.length + name.length;
                if (used + recordLen >= buffer.length) {
                    chunks.add(Arrays.copyOf(buffer, used));
                    used = 1;
                }
                buffer[used++] = (byte) count;
                buffer[used++] = (byte) number.length;
                System.arraycopy(number, 0, buffer, used, number.length);
                used += number.length;
                buffer[used++] = (byte) name.length;
                System.arraycopy(name, 0, buffer, used, name.length);
                used += name.length;
                count++;
            }
        }
        chunks.add(Arrays.copyOf(buffer, used));
        LOG.info("AK102 sending {} contacts in {} packet(s)", count, chunks.size());
        for (final byte[] chunk : chunks) {
            chunk[0] = (byte) count;
            sendSimpleCommand("ak102-contacts", Ak102Constants.CMD_SETTINGS,
                    Ak102Constants.KEY_SET_CONTACTS, chunk);
        }
    }

    // UTF-8 encode, truncated to maxBytes without splitting a code point.
    private static byte[] truncateUtf8(final String value, final int maxBytes) {
        String s = value.trim();
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        while (bytes.length > maxBytes && !s.isEmpty()) {
            s = s.substring(0, s.length() - 1);
            bytes = s.getBytes(StandardCharsets.UTF_8);
        }
        return bytes;
    }

    // Exercise goal ----------------------------------------------------------

    private byte[] buildExerciseGoalPayload() {
        final ActivityUser user = new ActivityUser();
        final int steps = user.getStepsGoal();
        final int distance = user.getDistanceGoalMeters();
        final int calories = user.getCaloriesBurntGoal();
        final byte[] payload = new byte[18];
        putIntBe(payload, 0, steps);
        putIntBe(payload, 4, distance);
        putIntBe(payload, 8, calories);
        final byte[] time = buildTimePayload();
        System.arraycopy(time, 0, payload, 12, 4);
        // [16..17] sport-duration goal (minutes) left at 0.
        return payload;
    }

    private static void putIntBe(final byte[] out, final int offset, final int value) {
        out[offset] = (byte) ((value >> 24) & 0xFF);
        out[offset + 1] = (byte) ((value >> 16) & 0xFF);
        out[offset + 2] = (byte) ((value >> 8) & 0xFF);
        out[offset + 3] = (byte) (value & 0xFF);
    }
}
