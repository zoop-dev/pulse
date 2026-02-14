/*  Copyright (C) 2021-2024 Arjan Schrijver, Damien Gaignon, Petr Vaněk

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.laxasfit;

import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_ALARM;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_DND;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_FIND_BAND;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GET_HW_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GROUP_BAND_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GROUP_BIND;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GROUP_GENERAL;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GROUP_HEARTRATE_SETTINGS;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GROUP_RECEIVE_BUTTON_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GROUP_RECEIVE_SPORTS_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GROUP_REQUEST_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_GROUP_RESET;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_HEART_RATE_MEASUREMENT;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_NOTIFICATIONS_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_NOTIFICATION_CALL;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_NOTIFICATION_MESSAGE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_REQUEST_HEALTH_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_RESET;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_RX_BAND_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_ARM;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_DATE_TIME;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_DEVICE_VIBRATIONS;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_DISPLAY_ON_LIFT;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_LONG_SIT_REMINDER;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_SLEEP_TIMES;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_STEP_GOAL;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_SET_USER_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_TYPE_ACTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_TYPE_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_UNBIND;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.CMD_WEATHER;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.DATA_HEADER;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.DATA_HEADERS;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.GENDER_FEMALE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.GENDER_MALE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.NOTIFICATION_ICON_FACEBOOK;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.NOTIFICATION_ICON_INSTAGRAM;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.NOTIFICATION_ICON_LINE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.NOTIFICATION_ICON_QQ;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.NOTIFICATION_ICON_SMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.NOTIFICATION_ICON_TWITTER;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.NOTIFICATION_ICON_WECHAT;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.NOTIFICATION_ICON_WHATSAPP;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_BP_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_CAMERA1;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_CAMERA2;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_CAMERA3;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_FIND_PHONE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_HEART_RATE_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_MEDIA_BACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_MEDIA_FORW;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_MEDIA_PLAY_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_SLEEP_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_SPO2_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_SPORTS_DAY_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_SPORT_EXT;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.RX_STEP_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.UNIT_IMPERIAL;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.UNIT_METRIC;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.UUID_CHARACTERISTIC_RX;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.UUID_CHARACTERISTIC_TX;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_ON;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_SET_ARM_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_SET_ARM_RIGHT;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_SET_DEVICE_VIBRATIONS_DISABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_SET_DEVICE_VIBRATIONS_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_SET_LONG_SIT_REMINDER_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_SET_LONG_SIT_REMINDER_ON;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_SET_NOTIFICATIONS_ENABLE_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants.VALUE_SET_NOTIFICATIONS_ENABLE_ON;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.laxasfit.LaxasFitSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.LaxasFitActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.weather.WeatherMapper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class LaxasFitDeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LaxasFitDeviceSupport.class);
    public final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    public final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    public final DeviceInfoProfile<LaxasFitDeviceSupport> deviceInfoProfile;
    public final BatteryInfoProfile<LaxasFitDeviceSupport> batteryInfoProfile;

    public BluetoothGattCharacteristic readCharacteristic;
    public BluetoothGattCharacteristic writeCharacteristic;
    private static final boolean debugEnabled = false;
    private final int mtuSize=20;

    public LaxasFitDeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);

        IntentListener mListener = new IntentListener() {
            @Override
            public void notify(Intent intent) {
                String action = intent.getAction();
                if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                    handleDeviceInfo((DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
                } else if (BatteryInfoProfile.ACTION_BATTERY_INFO.equals(action)) {
                    handleBatteryInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
                }
            }
        };

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);
        addSupportedService(LaxasFitConstants.UUID_CHARACTERISTIC_RX);
        addSupportedService(LaxasFitConstants.UUID_CHARACTERISTIC_UART);
    }

    @Override
    public TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        readCharacteristic = getCharacteristic(UUID_CHARACTERISTIC_RX);
        writeCharacteristic = getCharacteristic(UUID_CHARACTERISTIC_TX);

        builder.notify(UUID_CHARACTERISTIC_RX, true);
        builder.notify(GattService.UUID_SERVICE_BATTERY_SERVICE, true);
        builder.setCallback(this);

        deviceInfoProfile.requestDeviceInfo(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);
        deviceInfoProfile.enableNotify(builder, true);

        // this sequence seems to be important as without it:
        // - fetch steps doesn't work
        // - band seems to drain battery really fast
        // - the wait time is needed as the band must process each command
        // - (implementation based on individual requests did not work, the wait is still needed)

        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, LaxasFitConstants.CMD_INIT1, (byte) 0x2));
        setTime(builder);
        //builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, LaxasFitConstants.CMD_INIT1));
        //builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, LaxasFitConstants.CMD_INIT2));
        //builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, LaxasFitConstants.CMD_INIT3, VALUE_ON));
        //builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, VALUE_ON));
        //builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, (byte) 0xf));
        //builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, CMD_GET_HW_INFO));
        //builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_BAND_INFO, CMD_RX_BAND_INFO));
        //builder.wait(200);

        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    public void handleDeviceInfo(DeviceInfo info) {
        LOG.debug("laxasfit device info: " + info);
        versionCmd.hwVersion = "LaxasFit";
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    public void handleDeviceInfo(byte[] value) {
        LOG.debug("LaxasFit device info2");
        if (value.length < 20) {
            return;
        }
        int start = 14;
        int data_len = (int) value[start];

        byte[] name = new byte[data_len];
        System.arraycopy(value, start + 1, name, 0, data_len);
        String sName = new String(name, StandardCharsets.UTF_8); //unused for now

        start = start + data_len + 1;
        data_len = (int) value[start];
        byte[] hwname = new byte[data_len];
        System.arraycopy(value, start + 1, hwname, 0, data_len);
        String sHWName = new String(hwname, StandardCharsets.UTF_8);
        LOG.debug("Device info: " + versionCmd);
        versionCmd.hwVersion = sHWName;
        handleGBDeviceEvent(versionCmd);
    }

    protected void unsetBusy() {
        if (getDevice().isBusy()) {
            GB.signalActivityDataFinish(getDevice());
            getDevice().unsetBusyTask();
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    byte[] rxPkg = new byte[]{};
    int expectedBytes = 0;

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] data) {
        super.onCharacteristicChanged(gatt, characteristic, data);
        // we received an answer, unset busy ...
        unsetBusy();

        if(ArrayUtils.indexOf(DATA_HEADERS, data[0])<0) {
            // NOT a protocol header, check id we wait for additional bytes
            if(rxPkg.length < expectedBytes)
            {
                rxPkg = ArrayUtils.addAll(rxPkg, data);
            }
        }
        else {
            // Known protocol header, set expected length and package data
            expectedBytes = ByteBuffer.wrap(data).getShort(1);
            rxPkg = data;
        }

        // ensure this is a data message
        if (rxPkg[0] != DATA_HEADER) {
            return false;
        }
        // package data is complete for processing
        if (rxPkg.length >= expectedBytes) {
            byte command = rxPkg[4];
            byte commandGroup = rxPkg[5];
            byte param = rxPkg[6];
            byte[] payload = Arrays.copyOfRange(rxPkg,9, rxPkg.length);
            switch (command) {
                case CMD_GROUP_RECEIVE_BUTTON_DATA:
                    switch (param) {
                        case RX_FIND_PHONE:
                            handleFindPhone();
                            break;
                        case RX_MEDIA_BACK:
                        case RX_MEDIA_FORW:
                        case RX_MEDIA_PLAY_PAUSE:
                            handleMediaButton(param);
                            break;
                        case RX_CAMERA1:
                        case RX_CAMERA2:
                        case RX_CAMERA3:
                            handleCamera(param);
                            break;
                        default:
                    }
                    break;
                case CMD_GROUP_RECEIVE_SPORTS_DATA:
                    switch (param) {
                        case RX_HEART_RATE_DATA:
                            handleHR(payload);
                            break;
                        case RX_BP_DATA:
                            handleBP(payload);
                            break;
                        case RX_SPO2_DATA:
                            handleSPO2(payload);
                            break;
                        case RX_SPORT_EXT:
                            sendAck((byte)0x07,(byte)0x6,(short)0x301, new byte[]{});
                            break;
                        case RX_SPORTS_DAY_DATA:
                            handleDayTotalsData(payload);
                            break;
                        case RX_SLEEP_DATA:
                            handleSleepData(payload);
                            break;
                        case RX_STEP_DATA:
                            handleStepData(payload);
                            break;
                    }
                    break;
                case CMD_GROUP_BAND_INFO:
                    switch (param) {
                        case CMD_RX_BAND_INFO:
                            handleDeviceInfo(data);
                            break;
                    }
                    break;
                case CMD_GROUP_REQUEST_DATA:
                    switch (param) {
                        case CMD_GET_HW_INFO:
                            handleHardwareDetails(data);
                            break;
                    }
                    break;
            }
            sendAck(command, param, (short)payload.length, new byte[]{1});
        }
        return false;
    }

    public void handleHardwareDetails(byte[] value) {
        LOG.debug("LaxasFit hardware details");
        if (value.length < 20) {
            return;
        }
        int start = 8;
        int data_len = (int) value[start];

        byte[] led = new byte[data_len];
        System.arraycopy(value, start + 1, led, 0, data_len);
        String sLED = new String(led, StandardCharsets.UTF_8);

        start = start + data_len + 1;
        data_len = (int) value[start];
        byte[] gsensor = new byte[data_len];
        System.arraycopy(value, start + 1, gsensor, 0, data_len);
        String sGsensor = new String(gsensor, StandardCharsets.UTF_8);

        gbDevice.setFirmwareVersion2(sGsensor + " " + sLED);

        //the band does not like to answer when asked together for both hw info, so ask now,
        // after data is already received

        TransactionBuilder builder = createTransactionBuilder("notification");
        builder.write(writeCharacteristic, craftData(CMD_GROUP_BAND_INFO, CMD_RX_BAND_INFO));
        builder.queue();

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        LOG.debug("LaxasFit send call notification");
        TransactionBuilder builder = createTransactionBuilder("CALL");

        if (callSpec.command == CallSpec.CALL_INCOMING) {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                outputStream.write(0x1);
                outputStream.write(0x0);
                outputStream.write(0x0);

                if (callSpec.name != null) {
                    outputStream.write(callSpec.name.getBytes(StandardCharsets.UTF_8));
                    outputStream.write(0x20);
                }
                if (callSpec.number != null) {
                    outputStream.write(callSpec.number.getBytes(StandardCharsets.UTF_8));
                    outputStream.write(0x20);
                }

            } catch (IOException e) {
                LOG.error("error sending call notification: " + e);
            }

            writeChunkedData(builder, craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATION_CALL, outputStream.toByteArray()));

        } else {
            builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATION_CALL, VALUE_OFF));
        }
        builder.queue();
    }

    @Override
    public void onSendConfiguration(String config) {

        LOG.debug("LaxasFit on send config: " + config);
        try {
            TransactionBuilder builder = performInitialized("sendConfiguration");
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
                    setLanguage(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD_EXTENDED:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_START:
                case DeviceSettingsPreferenceConst.PREF_INACTIVITY_END:
                    setLongSitReminder(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
                case DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_START:
                case DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_END:
                    setDisplayOnLift(builder);
                    break;
                case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                case ActivityUser.PREF_USER_WEIGHT_KG:
                case ActivityUser.PREF_USER_GENDER:
                case ActivityUser.PREF_USER_HEIGHT_CM:
                case ActivityUser.PREF_USER_DATE_OF_BIRTH:
                    setUserData(builder);
                    break;
                case ActivityUser.PREF_USER_STEPS_GOAL:
                    setStepsGoal(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END:
                    setDoNotDisturb(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME:
                case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME_START:
                case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME_END:
                    setSleepTime(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_WEARLOCATION:
                    setWearLocation(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_VIBRATION_ENABLE:
                    setVibrations(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE:
                    setNotifications(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_SWITCH:
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_SLEEP:
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_INTERVAL:
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_START:
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_END:
                    setAutoHeartRate(builder);
                    break;
            }
            builder.queue();
        } catch (IOException e) {
            GB.toast(getContext(), "Error sending configuration: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onTestNewFunction() {
        LOG.debug("Hello LaxasFit Test function");
    }

    @Override
    public void onSendWeather() {
        final WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            LOG.warn("No weather found in singleton");
            return;
        }
        LOG.debug("LaxasFit send weather");
        short todayMax = (short) (weatherSpec.getTodayMaxTemp() - 273);
        short todayMin = (short) (weatherSpec.getTodayMinTemp() - 273);
        byte weatherUnit = 0;
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            todayMax = (short) (todayMax * 1.8f + 32);
            todayMin = (short) (todayMin * 1.8f + 32);
            weatherUnit = 1;
        }

        byte currentConditionCode = WeatherMapper.mapToFitProCondition(weatherSpec.getCurrentConditionCode());
        TransactionBuilder builder = createTransactionBuilder("weather");
        writeChunkedData(builder, craftData(CMD_GROUP_GENERAL, CMD_WEATHER, new byte[]{(byte) todayMin, (byte) todayMax, (byte) currentConditionCode, (byte) weatherUnit}));
        builder.queue();
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        LOG.debug("LaxasFit notification: " + notificationSpec.type);
        TransactionBuilder builder = createTransactionBuilder("notification");
        byte icon = NOTIFICATION_ICON_SMS;
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                icon = NOTIFICATION_ICON_SMS;
                break;
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                icon = NOTIFICATION_ICON_FACEBOOK;
                break;
            case LINE:
                icon = NOTIFICATION_ICON_LINE;
                break;
            case WHATSAPP:
                icon = NOTIFICATION_ICON_WHATSAPP;
                break;
            case TWITTER:
                icon = NOTIFICATION_ICON_TWITTER;
                break;
            case SIGNAL:
            case VIBER:
            case CONVERSATIONS:
                icon = NOTIFICATION_ICON_QQ;
                break;
            case WECHAT:
            case GMAIL:
                icon = NOTIFICATION_ICON_WECHAT;
                break;
            case INSTAGRAM:
                icon = NOTIFICATION_ICON_INSTAGRAM;
                break;
            default:
                icon = NOTIFICATION_ICON_SMS;
                break;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(icon);
            outputStream.write(0x0);
            outputStream.write(0x0);

            if (notificationSpec.sender != null) {
                outputStream.write(notificationSpec.sender.getBytes(StandardCharsets.UTF_8));
                outputStream.write(0x20);
            } else {
                if (notificationSpec.phoneNumber != null) { //use number only if there is no sender
                    outputStream.write(notificationSpec.phoneNumber.getBytes(StandardCharsets.UTF_8));
                    outputStream.write(0x20);
                }
            }

            if (notificationSpec.subject != null) {
                outputStream.write(notificationSpec.subject.getBytes(StandardCharsets.UTF_8));
                outputStream.write(0x20);
            }
            if (notificationSpec.body != null) {
                outputStream.write(notificationSpec.body.getBytes(StandardCharsets.UTF_8));
                outputStream.write(0x20);
            }

        } catch (IOException e) {
            LOG.error("LaxasFit error sending notification: " + e);
        }
        String output = outputStream.toString();
        if (outputStream.toString().length() > 250) {
            output = outputStream.toString().substring(0, 250);
        }

        writeChunkedData(builder, craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATION_MESSAGE, output.getBytes(StandardCharsets.UTF_8)));
        builder.queue();
    }

    public LaxasFitDeviceSupport setLanguage(TransactionBuilder builder) {
        LOG.debug("LaxasFit set language");
        String localeString = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("language", "auto");
        if (localeString == null || localeString.equals("auto")) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();

            if (country == null) {
                country = language;
            }
            localeString = language + "_" + country.toUpperCase();
        }
        LOG.info("Setting device to locale: " + localeString);

        LaxasFitConstants.LANG languageCode = LaxasFitConstants.LANG.ENGLISH;

        switch (localeString.substring(0, 2)) {
            case "zh":
                languageCode = LaxasFitConstants.LANG.CHINESE;
                break;
            case "it":
                languageCode = LaxasFitConstants.LANG.ITALIAN;
                break;
            case "cs":
                languageCode = LaxasFitConstants.LANG.CZECH;
                break;
            case "en":
                languageCode = LaxasFitConstants.LANG.ENGLISH;
                break;
            case "tr":
                languageCode = LaxasFitConstants.LANG.TURKISH;
                break;
            case "ru":
                languageCode = LaxasFitConstants.LANG.RUSSIAN;
                break;
            case "pl":
                languageCode = LaxasFitConstants.LANG.POLISH;
                break;
            case "nl":
                languageCode = LaxasFitConstants.LANG.DUTCH;
                break;
            case "fr":
                languageCode = LaxasFitConstants.LANG.FRENCH;
                break;
            case "es":
                languageCode = LaxasFitConstants.LANG.SPANISH;
                break;
            case "de":
                languageCode = LaxasFitConstants.LANG.GERMAN;
                break;
            case "pt":
                languageCode = LaxasFitConstants.LANG.PORTUGESE;
                break;

            default:
                languageCode = LaxasFitConstants.LANG.ENGLISH;
        }

        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_LANGUAGE, (byte)languageCode.ordinal()));

        return this;
    }

    public LaxasFitDeviceSupport setUserData(TransactionBuilder builder) {
        //0xcd 0x00 0x09 0x12 0x01 0x04 0x00 0x04 0xaf 0x59 0x09 0xe1 LaxasFit
        LOG.debug("LaxasFit set user data");

        ActivityUser activityUser = new ActivityUser();

        int age = activityUser.getAge();

        int gender = activityUser.getGender();
        byte genderUnit = GENDER_FEMALE;
        if (gender == ActivityUser.GENDER_MALE) {
            genderUnit = GENDER_MALE;
        }

        int heightCm = activityUser.getHeightCm();
        int weightKg = activityUser.getWeightKg();

        byte distanceUnit = UNIT_METRIC;
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            distanceUnit = UNIT_IMPERIAL;
        }

        int userData = genderUnit << 31 | age << 24 | heightCm << 15 | weightKg << 5 | distanceUnit;
        byte[] data = craftData(CMD_GROUP_GENERAL, CMD_SET_USER_DATA, ByteBuffer.allocate(4).putInt(userData).array());
        builder.write(writeCharacteristic, data);
        return this;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        TransactionBuilder builder = createTransactionBuilder("fetch sports data");
        builder.setBusyTask(R.string.busy_task_fetch_activity_data);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_RECEIVE_SPORTS_DATA, CMD_TYPE_ACTION, CMD_REQUEST_HEALTH_DATA, new byte[]{}));
        builder.queue();
    }

    public void handleBatteryInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo info) {
        LOG.debug("LaxasFit battery info: " + info);
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    /*   public void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
           LOG.debug("LaxasFit device info: " + info);
           versionCmd.hwVersion = "+LaxasFit";
           versionCmd.fwVersion = info.getFirmwareRevision();
           handleGBDeviceEvent(versionCmd);
       }

     */
    public void handleCamera(byte command) {
        GB.toast(getContext(), "Camera buttons are detected but not further handled.", Toast.LENGTH_SHORT, GB.INFO);
    }

    public void handleFindPhone() {
        LOG.info("LaxasFit find phone");
        GBDeviceEventFindPhone deviceEventFindPhone = new GBDeviceEventFindPhone();
        deviceEventFindPhone.event = GBDeviceEventFindPhone.Event.START;
        evaluateGBDeviceEvent(deviceEventFindPhone);
    }

    public void handleMediaButton(byte command) {
        GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
        if (command == RX_MEDIA_PLAY_PAUSE) {
            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
            evaluateGBDeviceEvent(deviceEventMusicControl);
        } else if (command == RX_MEDIA_FORW) {
            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
            evaluateGBDeviceEvent(deviceEventMusicControl);
        } else if (command == RX_MEDIA_BACK) {
            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
            evaluateGBDeviceEvent(deviceEventMusicControl);
        }
    }

    public LaxasFitDeviceSupport setVibrations(TransactionBuilder builder) {
        LOG.debug("LaxasFit set enable vibrations");
        boolean vibrations = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_VIBRATION_ENABLE, false);
        byte[] enable = VALUE_SET_DEVICE_VIBRATIONS_ENABLE;
        if (!vibrations) {
            enable = VALUE_SET_DEVICE_VIBRATIONS_DISABLE;
        }
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_DEVICE_VIBRATIONS, enable));
        return this;
    }

    public LaxasFitDeviceSupport setNotifications(TransactionBuilder builder) {
        LOG.debug("LaxasFit set enable notifications");
        boolean notifications = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false);
        byte[] enable = VALUE_SET_NOTIFICATIONS_ENABLE_ON;
        if (!notifications) {
            enable = VALUE_SET_NOTIFICATIONS_ENABLE_OFF;
        }
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATIONS_ENABLE, enable));
        return this;
    }

    public static byte[] craftData(byte command_group, byte command) {
        return craftData(command_group, command, new byte[]{});
    }

    public static byte[] craftData(byte command_group, byte command, byte value) {
        return craftData(command_group, command, new byte[]{value});
    }

    public static byte[] craftData(byte command_group, byte command, byte[] data) {
        return craftData(command_group, CMD_TYPE_DATA, command, data);
    }

    public static byte[] craftData(byte command_group, byte command_type, byte command, byte[] data) {
        //0xDF 0x00 0x09 0x?? 0x12 0x01 0x01 0x00 0x04 0xA5 0x83 0x73 0xDB
        byte[] result = new byte[LaxasFitConstants.DATA_TEMPLATE.length + data.length];
        System.arraycopy(LaxasFitConstants.DATA_TEMPLATE, 0, result, 0, LaxasFitConstants.DATA_TEMPLATE.length);
        result[1] = (byte) (((LaxasFitConstants.DATA_TEMPLATE.length + data.length - 4) >> 8) & 0xff);
        result[2] = (byte) ((LaxasFitConstants.DATA_TEMPLATE.length + data.length - 4) & 0xff);
        result[4] = command_group;
        result[5] = command_type;
        result[6] = command;
        result[7] = (byte) ((data.length >> 8) & 0xff);
        result[8] = (byte) (data.length & 0xff);
        System.arraycopy(data, 0, result, 9, data.length);
        // fix message CRC with current content
        return fixCrc(result);
    }

    /**
     * Fix CRC field of message
     * @param buffer Message bugger to be fixed
     * @return Message buffer with fixed CRC field
     */
    static byte[] fixCrc(byte[] buffer) {
        byte crc=0;
        for (byte b : buffer) {
            crc += b;
        }
        buffer[3] = crc;
        return buffer;
    }

    public void sendAck(byte command, byte param, short status, byte[] payload) {
        byte[] msg = ByteBuffer.allocate(8+payload.length)
                .put(LaxasFitConstants.DATA_HEADER_ACK)
                .putShort((short)(4+payload.length))
                .put((byte)0)
                .put(command)
                .put(param)
                .putShort(status)
                .put(payload)
                .array();
        createTransactionBuilder("ACK")
                .write(writeCharacteristic, fixCrc(msg))
                .queue();
    }

    // send chucked up data
    public void writeChunkedData(TransactionBuilder builder, byte[] data) {
        for (int start = 0; start < data.length; start += mtuSize) {
            int end = start + mtuSize;
            if (end > data.length) end = data.length;
            builder.write(writeCharacteristic, Arrays.copyOfRange(data, start, end));
        }
    }

    @Override
    public void onSetTime() {
        LOG.debug("LaxasFit set date and time");
        TransactionBuilder builder = createTransactionBuilder("Set date and time");
        setTime(builder);
        builder.queue();
    }


    public LaxasFitDeviceSupport setTime(TransactionBuilder builder) {
        LOG.debug("LaxasFit set time");
        Calendar calendar = Calendar.getInstance();

        int datetime = calendar.get(Calendar.SECOND) | (
                (calendar.get(Calendar.YEAR) - 2000) << 26 | calendar.get(Calendar.MONTH) + 1 << 22 |
                        calendar.get(Calendar.DAY_OF_MONTH) << 17 |
                        calendar.get(Calendar.HOUR_OF_DAY) << 12 | calendar.get(Calendar.MINUTE) << 6);

        //this is how the values can be re-stored
        // result is this
        //byte[] array = new byte[]{(byte) (datetime >> 24), (byte) (datetime >> 16), (byte) (datetime >> 8), (byte) (datetime >> 0)};
        // int datetime2 = ByteBuffer.wrap(array).getInt();

        //byte[] time = craftData(LT716Constants.CMD_SET_DATE_TIME, new byte[]{(byte) (datetime >> 24), (byte) (datetime >> 16), (byte) (datetime >> 8), (byte) (datetime >> 0)});
        byte[] time = craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_SET_DATE_TIME, (ByteBuffer.allocate(4).putInt(datetime).array()));
        builder.write(writeCharacteristic, time);
        return this;
    }


    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        LOG.debug("LaxasFit set alarms");

        // handle one-shot alarm from the widget:
        // this device doesn't have concept of on-off alarm, so use the last slot for this and store
        // this alarm in the database so the user knows what is going on and can disable it

        if (alarms.size() == 1 && alarms.get(0).getRepetition() == 0) { //single shot?
            Alarm oneshot = alarms.get(0);
            alarms = (ArrayList<? extends Alarm>) AlarmUtils.mergeOneshotToDeviceAlarms(gbDevice, (nodomain.freeyourgadget.gadgetbridge.entities.Alarm) oneshot, 7);
        }

        try {
            TransactionBuilder builder = performInitialized("Set alarm");
            boolean anyAlarmEnabled = false;
            byte[] all_alarms = new byte[]{};

            for (Alarm alarm : alarms) {
                Calendar calendar = AlarmUtils.toCalendar(alarm);
                anyAlarmEnabled |= alarm.getEnabled();
                LOG.debug("alarms: " + alarm.getPosition());
                int maxAlarms = 8;
                if (alarm.getPosition() >= maxAlarms) { //we should never encounter this, but just in case
                    if (alarm.getEnabled()) {
                        GB.toast(getContext(), "Only 8 alarms are supported.", Toast.LENGTH_LONG, GB.WARN);
                    }
                    return;
                }
                if (alarm.getEnabled()) {
                    long datetime = (long) alarm.getRepetition() | (
                            (long) (calendar.get(Calendar.YEAR) - 2000) << 34 |
                                    (long) (calendar.get(Calendar.MONTH) + 1) << 30 |
                                    (long) (calendar.get(Calendar.DAY_OF_MONTH)) << 25 |
                                    (long) (calendar.get(Calendar.HOUR_OF_DAY)) << 20 |
                                    (long) (calendar.get(Calendar.MINUTE)) << 14 |
                                    1L << 11);
                    byte[] single_alarm = new byte[]{(byte) (datetime >> 32), (byte) (datetime >> 24), (byte) (datetime >> 16), (byte) (datetime >> 8), (byte) (datetime)};
                    all_alarms = ArrayUtils.addAll(all_alarms, single_alarm);
                }
            }

            writeChunkedData(builder, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_ALARM, all_alarms));
            //builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_ALARM, all_alarms));
            builder.queue();
            if (anyAlarmEnabled) {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_ok), Toast.LENGTH_SHORT, GB.INFO);
            } else {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_all_alarms_disabled), Toast.LENGTH_SHORT, GB.INFO);
            }
        } catch (IOException ex) {
            GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_failed), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    @Override
    public void onReset(int flags) {
        LOG.debug("LaxasFit reset flags: " + flags);
        byte[] command = craftData(CMD_GROUP_RESET, CMD_RESET);
        switch (flags) {
            case 1:
                command = craftData(CMD_GROUP_RESET, CMD_RESET);
                break;
            case 2:
                command = craftData(CMD_GROUP_BIND, CMD_UNBIND);
                break;
        }

        getQueue().clear();
        TransactionBuilder builder = createTransactionBuilder("resetting");
        builder.write(writeCharacteristic, command);
        builder.queue();
    }

    @Override
    public void onHeartRateTest() {
        TransactionBuilder builder = createTransactionBuilder("heartrate");
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_HEART_RATE_MEASUREMENT, new byte[]{VALUE_ON}));
        builder.queue();
    }

    @Override
    public void onFindDevice(boolean start) {
        getQueue().clear();
        LOG.debug("LaxasFit find device");
        TransactionBuilder builder = createTransactionBuilder("searching");
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_FIND_BAND, new byte[]{start ? VALUE_ON : VALUE_OFF}));
        builder.queue();
    }

    public LaxasFitDeviceSupport setAutoHeartRate(TransactionBuilder builder) {
        LOG.debug("LaxasFit set automatic heartrate measurements");
        boolean prefAutoheartrateSwitch = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean("pref_autoheartrate_switch", false);
        LOG.info("Setting autoheartrate to " + prefAutoheartrateSwitch);

        boolean sleep = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean("pref_autoheartrate_sleep", false);
        String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_autoheartrate_start", "06:00");
        String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_autoheartrate_end", "23:00");
        String interval = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_autoheartrate_interval", "2");

        int intervalInt = Integer.parseInt(interval);
        int sleepInt = sleep ? 1 : 0;
        int autoheartrateInt = prefAutoheartrateSwitch ? 1 : 0;

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        int startTime = (startCalendar.get(Calendar.HOUR_OF_DAY) * 60) + startCalendar.get(Calendar.MINUTE);
        int endTime = (endCalendar.get(Calendar.HOUR_OF_DAY) * 60) + endCalendar.get(Calendar.MINUTE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(autoheartrateInt);
        outputStream.write(sleepInt);
        outputStream.write(intervalInt >> 8);
        outputStream.write(intervalInt);
        outputStream.write(startTime >> 8);
        outputStream.write(startTime);
        outputStream.write(endTime >> 8);
        outputStream.write(endTime);
        //outputStream.write(0x7F);

        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_GROUP_HEARTRATE_SETTINGS, outputStream.toByteArray()));

        return this;
    }

    public LaxasFitDeviceSupport setLongSitReminder(TransactionBuilder builder) {
        LOG.debug("LaxasFit set inactivity warning");
        boolean prefLongsitSwitch = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, false);
        LOG.info("Setting long sit warning to " + prefLongsitSwitch);

        if (prefLongsitSwitch) {

            String inactivity = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD_EXTENDED, "4");
            String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, "08:00");
            String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, "16:00");
            Calendar startCalendar = GregorianCalendar.getInstance();
            Calendar endCalendar = GregorianCalendar.getInstance();
            DateFormat df = new SimpleDateFormat("HH:mm");

            try {
                startCalendar.setTime(df.parse(start));
                endCalendar.setTime(df.parse(end));
            } catch (ParseException e) {
                LOG.debug("settings error: " + e);
            }

            int inactivityInt = Integer.parseInt(inactivity);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                outputStream.write(VALUE_SET_LONG_SIT_REMINDER_ON);
                outputStream.write(inactivityInt);
                outputStream.write(startCalendar.get(Calendar.HOUR_OF_DAY));
                outputStream.write(endCalendar.get(Calendar.HOUR_OF_DAY));
                outputStream.write(0x7F);
            } catch (IOException e) {
                LOG.error("settings error: " + e);
            }

            builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_SET_LONG_SIT_REMINDER, outputStream.toByteArray()));
            LOG.info("Setting long sit warning to scheduled");

        } else {
            builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_SET_LONG_SIT_REMINDER, VALUE_SET_LONG_SIT_REMINDER_OFF));
            LOG.info("Setting long sit warning to OFF");
        }
        return this;
    }

    public LaxasFitDeviceSupport setDoNotDisturb(TransactionBuilder builder) {
        LOG.debug("LaxasFit set DND");
        String dnd = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("do_not_disturb_no_auto", "off");
        LOG.info("Setting DND to " + dnd);
        int dndInt = dnd.equals("scheduled") ? 1 : 0;

        String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("do_not_disturb_no_auto_start", "22:00");
        String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("do_not_disturb_no_auto_end", "06:00");

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        int startTime = (startCalendar.get(Calendar.HOUR_OF_DAY) * 60) + startCalendar.get(Calendar.MINUTE);
        int endTime = (endCalendar.get(Calendar.HOUR_OF_DAY) * 60) + endCalendar.get(Calendar.MINUTE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        outputStream.write(dndInt);
        outputStream.write(startTime >> 8);
        outputStream.write(startTime);
        outputStream.write(endTime >> 8);
        outputStream.write(endTime);

        LOG.info("Setting DND to scheduled: " + start + " " + end);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_DND, outputStream.toByteArray()));
        LOG.info("Setting DND scheduled");

        return this;
    }

    public LaxasFitDeviceSupport setSleepTime(TransactionBuilder builder) {
        LOG.debug("LaxasFit set sleep times");
        String sleepTime = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("prefs_enable_sleep_time", "off");
        LOG.info("Setting sleep times to " + sleepTime);
        int sleepTimeInt = sleepTime.equals("scheduled") ? 1 : 0;

        String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("prefs_sleep_time_start", "22:00");
        String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("prefs_sleep_time_end", "06:00");

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        int startTime = (startCalendar.get(Calendar.HOUR_OF_DAY) * 60) + startCalendar.get(Calendar.MINUTE);
        int endTime = (endCalendar.get(Calendar.HOUR_OF_DAY) * 60) + endCalendar.get(Calendar.MINUTE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(sleepTimeInt);
        outputStream.write(startTime >> 8);
        outputStream.write(startTime);
        outputStream.write(endTime >> 8);
        outputStream.write(endTime);
        LOG.info("Setting sleep times scheduled: " + start + " " + end);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_SET_SLEEP_TIMES, outputStream.toByteArray()));
        LOG.info("Setting sleep times scheduled");
        return this;
    }

    public LaxasFitDeviceSupport setWearLocation(TransactionBuilder builder) {
        LOG.debug("LaxasFit set wearing location");
        byte location = VALUE_SET_ARM_LEFT;
        String setLocation = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_WEARLOCATION, "left");
        if ("right".equals(setLocation)) {
            location = VALUE_SET_ARM_RIGHT;
        }
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_SET_ARM, new byte[]{location}));
        return this;
    }

    public LaxasFitDeviceSupport setDisplayOnLift(TransactionBuilder builder) {
        LOG.debug("LaxasFit set display on lift");
        String displayLift = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("activate_display_on_lift_wrist", "off");

        int displayLiftInt = displayLift.equals("scheduled") ? 1 : 0;

        LOG.info("Setting activate display on lift wrist to:" + displayLift + ": " + displayLiftInt);

        String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("display_on_lift_start", "08:00");
        String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("display_on_lift_end", "16:00");

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        int startTime = (startCalendar.get(Calendar.HOUR_OF_DAY) * 60) + startCalendar.get(Calendar.MINUTE);
        int endTime = (endCalendar.get(Calendar.HOUR_OF_DAY) * 60) + endCalendar.get(Calendar.MINUTE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(displayLiftInt);
        outputStream.write(startTime >> 8);
        outputStream.write(startTime);
        outputStream.write(endTime >> 8);
        outputStream.write(endTime);
        LOG.info("Setting activate display on lift wrist scheduled: " + start + " " + end);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_SET_DISPLAY_ON_LIFT, outputStream.toByteArray()));
        LOG.info("Setting activate display on lift wrist scheduled");

        return this;
    }

    public LaxasFitDeviceSupport setStepsGoal(TransactionBuilder builder) {
        LOG.debug("LaxasFit set step goal");
        //cd 00 09 12 01 03 00 04 00 00 05 dc

        ActivityUser activityUser = new ActivityUser();
        int stepGoal = activityUser.getStepsGoal();
        byte[] data = craftData(CMD_GROUP_GENERAL, CMD_TYPE_ACTION, CMD_SET_STEP_GOAL, ByteBuffer.allocate(4).putInt(stepGoal).array());

        builder.write(writeCharacteristic, data);
        return this;
    }

    public void handleSleepData(byte[] value) {
        LOG.debug("SLEEP data: {}", GB.hexdump(value));
        // sleep packet consists of: date + list of 4bytes of 15minutes intervals
        // these intervals contain seconds offset from the date and type of sleep
        final ActivityKind[] SleepKind={ActivityKind.LIGHT_SLEEP, ActivityKind.DEEP_SLEEP, ActivityKind.AWAKE_SLEEP, ActivityKind.REM_SLEEP};
        ByteBuffer buf = ByteBuffer.wrap(value);
        Calendar date = decodeDateTime(buf.getShort());
        int timestamp = (int)(date.getTimeInMillis()/1000L);
        List<LaxasFitActivitySample> samples = new ArrayList<>();
        // add initial entry with action type unknown
        LaxasFitActivitySample sample = new LaxasFitActivitySample();
        sample.setTimestamp(timestamp);
        sample.setRawKind(ActivityKind.NOT_MEASURED.getCode());
        samples.add(sample);
        for (int iterationOfs = 4; iterationOfs < value.length - 4; iterationOfs += 8) {
            // loop through sleep type fields
            for(int sleepDepthIndex=0; sleepDepthIndex<4; sleepDepthIndex++) {
                // get duration of sleep depth
                short actionTime = buf.getShort(iterationOfs + sleepDepthIndex * 2);
                // create a entry for every 5 minutes
                for( int minute =0; minute < actionTime; minute += 5)
                {
                    timestamp += 5 * 60;
                    sample = new LaxasFitActivitySample();
                    sample.setTimestamp(timestamp);
                    sample.setRawKind(SleepKind[sleepDepthIndex].getCode());
                    samples.add(sample);
                }
            }
        }
        addGBActivitySamples(samples);
    }

    public void handleDayTotalsData(byte[] value) {
        LOG.debug("STEP data: {}", GB.hexdump(value));
        ByteBuffer buf = ByteBuffer.wrap(value);
        // generate sample
        LaxasFitActivitySample sample = new LaxasFitActivitySample();
        sample.setTimestamp((int)(System.currentTimeMillis()/1000L));
        sample.setRawKind(0); // unknown
        sample.setTotalSteps(buf.getInt(0));
        sample.setTotalDistance_m(buf.getInt(4));
        sample.setTotalCalories(buf.getInt(8));
        // add sample
        addGBActivitySample(sample);
    }
    public void handleStepData(byte[] value) {
        LOG.debug("STEPS data: {}", GB.hexdump(value));
        // sleep packet consists of: date + list of 4bytes of 15minutes intervals
        // these intervals contain seconds offset from the date and type of sleep
        ByteBuffer buf = ByteBuffer.wrap(value);
        Calendar date = decodeDateTime(buf.getShort());
        int timestamp = (int)(date.getTimeInMillis()/1000L);
        List<LaxasFitActivitySample> samples = new ArrayList<>();
        int seconds = 0;
        for (int i = 2; i < value.length - 2; i+=8) {
            LaxasFitActivitySample sample = new LaxasFitActivitySample();
            sample.setRawKind(0);
            sample.setTimestamp(timestamp+seconds);
            sample.setSteps(buf.getShort(i));
            sample.setDistanceCm((int)buf.getShort(i+2)*100);
            sample.setActiveCalories((int)buf.getShort(i+6));
            samples.add(sample);
            seconds += 3600;
        }
        addGBActivitySamples(samples);
    }

    public void handleHR(byte[] value) {
        LOG.debug("HR data: {}", GB.hexdump(value));
        ByteBuffer buf = ByteBuffer.wrap(value);
        Calendar date = decodeDateTime(buf.getShort(0));
        int timestamp = (int)(date.getTimeInMillis()/1000L);
        int numEntries = (int)buf.getShort(2);
        List<LaxasFitActivitySample> samples = new ArrayList<>();
        for(int i=0; i<numEntries; i++)
        {
            int seconds = buf.getInt(4+i*8);
            int heartRate = buf.getInt(8+i*8);

            LaxasFitActivitySample sample = new LaxasFitActivitySample();
            sample.setTimestamp(timestamp+seconds);
            sample.setHeartRate(heartRate);
            samples.add(sample);
        }
        addGBActivitySamples(samples);
    }

    public void handleBP(byte[] value) {
        LOG.debug("BP data: {}", GB.hexdump(value));
        ByteBuffer buf = ByteBuffer.wrap(value);
        Calendar date = decodeDateTime(buf.getShort(0));
        int timestamp = (int)(date.getTimeInMillis()/1000L);
        int numEntries = (int)buf.getShort(2);
        List<LaxasFitActivitySample> samples = new ArrayList<>();
        for(int i=0; i<numEntries; i++)
        {
            int seconds = buf.getInt(4+i*8);
            int systolic = buf.get(6+i*8);
            int asystolic = buf.get(7+i*8);

            LaxasFitActivitySample sample = new LaxasFitActivitySample();
            sample.setTimestamp(timestamp+seconds);
            sample.setPressureHighMmHg(systolic);
            sample.setPressureLowMmHg(asystolic);
            samples.add(sample);
        }
        addGBActivitySamples(samples);
    }

    public void handleSPO2(byte[] value) {
        LOG.debug("SPO2 data: {}", GB.hexdump(value));
        ByteBuffer buf = ByteBuffer.wrap(value);
        Calendar date = decodeDateTime(buf.getShort(0));
        int timestamp = (int)(date.getTimeInMillis()/1000L);
        int numEntries = (int)buf.getShort(2);
        List<LaxasFitActivitySample> samples = new ArrayList<>();
        for(int i=0; i<numEntries; i++)
        {
            int seconds = buf.getInt(4+i*8);
            int spo2 = buf.getShort(6+i*8);

            LaxasFitActivitySample sample = new LaxasFitActivitySample();
            sample.setTimestamp(timestamp+seconds);
            sample.setSpo2Percent(spo2);
            samples.add(sample);
        }
        addGBActivitySamples(samples);
    }

    public void addGBActivitySample(LaxasFitActivitySample sample) {
        List<LaxasFitActivitySample> samples = new ArrayList<>();
        samples.add(sample);
        addGBActivitySamples(samples);
    }

    private boolean addGBActivitySamples(List<LaxasFitActivitySample> samples) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), dbHandler.getDaoSession());
            LaxasFitSampleProvider provider = new LaxasFitSampleProvider(this.getDevice(), dbHandler.getDaoSession());

            for (LaxasFitActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);
                LOG.trace("SAMPLE: {}", sample);
                provider.addGBActivitySample(sample);
            }

        } catch (Exception ex) {
            LOG.error("Error saving samples: ", ex);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
            return false;
        }
        return true;
    }

    public Calendar decodeDateTime(short dateShort) {
        int day = (dateShort & 0x1f);
        int month = ((dateShort >> 5) & 0xf);
        int year = ((dateShort >> 9) + 2000);

        Calendar date = GregorianCalendar.getInstance();
        date.set(year, month - 1, day, 0, 0, 0);
        return date;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}