/*  Copyright (C) 2025 Vitaliy Tomin, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;


import static nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportConstants.DATA_HEADER_SIZE;
import static nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_THIRD_RX;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Back;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Ble;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Config;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.CyclingData;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Firmware;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Ins;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Media;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.PeripheralCommon;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.RoutePlan;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Common;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Factory;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;


public class IGPSportDeviceSupport extends AbstractBTLESingleDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(IGPSportDeviceSupport.class);
    public BluetoothGattCharacteristic readCharacteristic;
    public BluetoothGattCharacteristic writeCharacteristic;
    public BluetoothGattCharacteristic writeCharacteristicThird;
    public BluetoothGattCharacteristic writeCharacteristicFourth;
    public final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    public final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    public final DeviceInfoProfile<IGPSportDeviceSupport> deviceInfoProfile;
    public final BatteryInfoProfile<IGPSportDeviceSupport> batteryInfoProfile;
    private IGPSportRoutesManager routeManager;
    private IGPSportDownloadManager downloadManager;
    private IGPSportWeather weatherManager;
    private MediaManager mediaManager;


    private int mtuSize=247; //FIXME use actual device mtu
    boolean partialPacketInProgress = false;
    ByteBuffer partialBuffer = ByteBuffer.allocate(mtuSize);
    public IGPSportDeviceSupport() {
        super(LOG);

        routeManager = new IGPSportRoutesManager(this);
        downloadManager = new IGPSportDownloadManager(this);
        weatherManager = new IGPSportWeather(this);

        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);


        IntentListener mListener = new IntentListener() {
            @Override
            public void notify(Intent intent) {
                String action = intent.getAction();
                if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                    handleDeviceInfo((DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
                } else if (BatteryInfoProfile.ACTION_BATTERY_INFO.equals(action)) {
                    handleBatteryInfo((BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
                }
            }
        };

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);

        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_RX);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_SERVICE);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_SECOND_RX);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_SECOND_SERVICE);
        addSupportedService(UUID_IGPSPORT_CHARACTERISTIC_THIRD_RX);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_THIRD_SERVICE);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FOURTH_RX);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FORTH_SERVICE);


    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    public void handleDeviceInfo(DeviceInfo info) {
        LOG.debug("iGPSport device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getSoftwareRevision();
        handleGBDeviceEvent(versionCmd);
    }


    public void handleBatteryInfo(BatteryInfo info) {
        LOG.debug("iGPSport battery info: " + info);
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // mark the device as initializing
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        builder.requestMtu(mtuSize);
        readCharacteristic = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_RX);
        writeCharacteristic = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_TX);
        writeCharacteristicThird = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_THIRD_TX);
        writeCharacteristicFourth = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FOURTH_TX);

        builder.notify(getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_RX), true);
        builder.notify(getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_SECOND_RX), true);
        builder.notify(getCharacteristic(UUID_IGPSPORT_CHARACTERISTIC_THIRD_RX), true);
        builder.notify(getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FOURTH_RX), true);

        builder.setCallback(this);

        deviceInfoProfile.requestDeviceInfo(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);
        deviceInfoProfile.enableNotify(builder, true);

        // ... custom initialization logic ...


        Ble.ble_msg.Builder bleBuilder = Ble.ble_msg.newBuilder();
        bleBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_BLE);
        bleBuilder.setBleOperateType(Ble.BLE_OPERATE_TYPE.enum_BLE_OPERATE_TYPE_BOND_INFO);
        byte[] bleBondData = craftData(bleBuilder.getServiceType().getNumber(), 0xFF, bleBuilder.getBleOperateType().getNumber(), bleBuilder.build().toByteArray());
        builder.write(writeCharacteristic, bleBondData);


        Firmware.firmware_msg.Builder firmwareBuilder = Firmware.firmware_msg.newBuilder();
        firmwareBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_FIRMWARE);
        firmwareBuilder.setFirmwareOperateType(Firmware.FIRMWARE_OPERATE_TYPE.enum_FIRMWARE_OPERATE_TYPE_GET_VERSION);
        byte[] firmwareGetVersionData = craftData(firmwareBuilder.getServiceType().getNumber(), 0xff,firmwareBuilder.getFirmwareOperateType().getNumber(), firmwareBuilder.build().toByteArray());
        builder.write(writeCharacteristic, firmwareGetVersionData);

        Factory.factory_msg.Builder factoryBuilder = Factory.factory_msg.newBuilder();
        factoryBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_FACTORY);
        factoryBuilder.setFactoryOperateType(Factory.FACTORY_OPERATE_TYPE.enum_FACTORY_OPERATE_TYPE_BATTARY_GET);
        byte[] factoryGetBatterydata = craftData(factoryBuilder.getServiceType().getNumber(), 0xff, factoryBuilder.getFactoryOperateType().getNumber(), factoryBuilder.build().toByteArray());

        builder.write(writeCharacteristic, factoryGetBatterydata);

        Factory.factory_msg.Builder snFfactoryBuilder = Factory.factory_msg.newBuilder();
        snFfactoryBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_FACTORY);
        snFfactoryBuilder.setFactoryOperateType(Factory.FACTORY_OPERATE_TYPE.enum_FACTORY_OPERATE_TYPE_SN_GET);
        byte[] factoryGetSNdata = craftData(snFfactoryBuilder.getServiceType().getNumber(), 0xff, snFfactoryBuilder.getFactoryOperateType().getNumber(), snFfactoryBuilder.build().toByteArray());
        builder.write(writeCharacteristic, factoryGetSNdata);

        // set device firmware to prevent the following error when you (later) try to save data to database and
        // device firmware has not been set yet
        // Error executing 'the bind value at index 2 is null'java.lang.IllegalArgumentException: the bind value at index 2 is null
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        // mark the device as initialized
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    private void parseProtobufPacket(byte[] value) {
        LOG.info("Parsing complete packet: " + GB.hexdump(value));
        byte mainService = value[1];
        byte mainOperation = value[4];
        int dataSize = ByteBuffer.wrap(value, 7, 2).getShort();
        byte[] pbData = new byte[dataSize];
        System.arraycopy(value, DATA_HEADER_SIZE, pbData, 0, dataSize);

        try {
            switch (mainService) {
                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_FACTORY_VALUE:
                    handleFactoryData(pbData);
                    break;
                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_FIRMWARE_VALUE:
                    handleFirmwareData(pbData);
                    break;

                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_CYCLING_DATA_VALUE:
                    if (mainOperation == CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_LIST_SEND_VALUE) {
                        downloadManager.setFilesAvaliable(pbData);
                    }
                    break;
                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_ROUTE_PLAN_VALUE:
                    if (mainOperation == RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_LIST_NUM_GET_VALUE) {
                        routeManager.handleRouteNumber(pbData);
                    } else if (mainOperation == RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_LIST_SEND_VALUE) {
                        routeManager.handleRouteList(pbData);
                    }
                    break;
                default:
                    LOG.error("Unknown general operation received");
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,

                                           BluetoothGattCharacteristic characteristic, byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();

        LOG.info("Characteristic changed UUID: " + characteristicUUID);
        LOG.info("Characteristic changed value: " + GB.hexdump(characteristic.getValue()));


        if (downloadManager.needMoreData()) {
            if (characteristicUUID.compareTo(UUID_IGPSPORT_CHARACTERISTIC_THIRD_RX) == 0 ) {
                downloadManager.addData(value);
            }
            return true;
        }

        if(partialPacketInProgress) {
            LOG.info("Waiting for partial packet, adding current data");
            partialBuffer.put(value);
            if (partialBuffer.remaining() > 0) {
                LOG.info("Need even more data for partial packet");
                return true;
            } else {
                LOG.info("Partial packet completed going to parse it.");
                partialBuffer.flip();
                byte[] finalData = new byte[partialBuffer.remaining()];
                partialBuffer.get(finalData);
                parseProtobufPacket(finalData);
                partialPacketInProgress=false;
                partialBuffer.clear();
                return true;
            }
        }

        byte mainService = value[1];
        byte mainOperation = value[4];


        // 01 - header protobuf packets
        if ((value[0] == (byte) 0x01) && ( value[3] == (byte)0xff)) {
            int dataSize = ByteBuffer.wrap(value, 7, 2).getShort();
            if (dataSize+DATA_HEADER_SIZE > value.length) {
                LOG.info("Partial packet detected, creating buffer for it, buffer size: " + dataSize+DATA_HEADER_SIZE);
                partialPacketInProgress = true;
                partialBuffer = ByteBuffer.allocate(dataSize+DATA_HEADER_SIZE);
                partialBuffer.put(value);
                return true;
            }
            parseProtobufPacket(value);
            return true;

        }

        // 01 - header files packets
        if (value[3] == (byte)0x55) {
            switch (mainService) {
                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_CYCLING_DATA_VALUE:
                    if (mainOperation == CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_FILE_SEND_VALUE) {
                        downloadManager.startDownload();
                        downloadManager.addData(value);
                    }
                    break;
                default:
                    LOG.error("Unknown file operation received");
            }
        }


        // 02-header -- results of operations from device
        //0215FFFF03FFFF00FFFFFFFFFFFFFFFFFFFFFF55
        if (value[0] == (byte) 0x02) {
            byte result = value[7];
            switch (mainService) {
                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_FILE_OPERATION_VALUE:
                    if (mainOperation == Common.SERVICE_OPERATE_TYPE.enum_SERVICE_OPERATE_TYPE_ADD_VALUE) {
                        gbDevice.unsetBusyTask();
                        gbDevice.sendDeviceUpdateIntent(getContext());
                        TransactionBuilder builder = createTransactionBuilder("Route  upload finished");
                        if (result == 0) {
                            builder.setProgress(R.string.route_upload_completed, false, 100);
                            handleGBDeviceEvent(new GBDeviceEventDisplayMessage("Route upload completed", Toast.LENGTH_LONG, GB.INFO));
                        } else {
                            builder.setProgress(R.string.route_upload_failed, false, 0);
                            handleGBDeviceEvent(new GBDeviceEventDisplayMessage("Failed to upload route", Toast.LENGTH_LONG, GB.ERROR));
                        }
                        builder.queue();
                    }
                    break;

                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_CYCLING_DATA_VALUE:
                    if (mainOperation == CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_FILE_SEND_VALUE) {
                        LOG.info("Got reply from file service"); // Do we have to do something here?
                    }
                    break;
                //0207FFFF03FFFF00FFFFFFFFFFFFFFFFFFFFFFE7  - route deletion success
                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_ROUTE_PLAN_VALUE:
                    if (mainOperation == RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_FILE_DEL_VALUE) {
                        LOG.info("File removed, refresh list"); // Do we have to do something here?
                        routeManager.requestRouteList();
                    }
                    break;

            }
        }



        // 03-header -- requests from device
        if (value[0] == (byte) 0x03) {
            byte secondService = value[2];
            if (mainService == Common.service_type_index.enum_SERVICE_TYPE_INDEX_BACK_VALUE) {
                switch (secondService) {
                    case Back.BACK_SERVICE_TYPE.enum_BACK_SERVICE_TYPE_WEATHER_VALUE:
                        LOG.info("Device asking for weather");
                        handleWeather();
                        break;
                    case Back.BACK_SERVICE_TYPE.enum_BACK_SERVICE_TYPE_EPHEMERIS_VALUE:
                        LOG.error("Device asking for ephemeris, not implemented");
                        break;
                    default:
                        LOG.info("Unknown request");

                }
            }

            if (mainService == Common.service_type_index.enum_SERVICE_TYPE_INDEX_DEV_STATUS_VALUE) {
                LOG.info("Device sending its status " + value[7]);
            }

            if (mainService == Common.service_type_index.enum_SERVICE_TYPE_INDEX_CONFIG_VALUE) {
                LOG.info("Device transmitting its config");
            }

            if (mainService == PeripheralCommon.PERIPHERAL_SERVICE_TYPE.PST_HR_VALUE) {
                LOG.info("Device transmitting its config");
            }
            if (mainService == Common.service_type_index.enum_SERVICE_TYPE_INDEX_MUSIC_CTL_VALUE) {
                byte secondCommand = value[5];
                GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();

                switch (secondCommand) {
                    case Media.REMOTE_CMD.PLAY_VALUE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case Media.REMOTE_CMD.PAUSE_VALUE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case Media.REMOTE_CMD.TOGGLE_PLAY_PAUSE_VALUE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                        break;
                    case Media.REMOTE_CMD.NEXT_TRACK_VALUE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case Media.REMOTE_CMD.PREVIOUS_TRACK_VALUE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case Media.REMOTE_CMD.VOLUME_UP_VALUE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case Media.REMOTE_CMD.VOLUME_DOWN_VALUE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                }
                evaluateGBDeviceEvent(deviceEventMusicControl);
            }
        }

        return true;
    }


    public void handleFactoryData(byte[] data) throws InvalidProtocolBufferException {
        Factory.factory_msg factoryMsg = Factory.factory_msg.parseFrom(data);
        if (factoryMsg.hasBattaryMsg()) {
            gbDevice.setBatteryLevel(factoryMsg.getBattaryMsg().getPowerPercent());
        }
        if (factoryMsg.getFactorySnMsgList().size() > 0) {
            // save serial to volatile address now, need to find way to use it as identifier in database
            // instead of MAc, because MAC changes on rebind for this devices
            gbDevice.setVolatileAddress(factoryMsg.getFactorySnMsg(0).getSn());
        }
    }

    public void handleFirmwareData(byte[] data) throws InvalidProtocolBufferException {
        Firmware.firmware_msg firmwareMsg = Firmware.firmware_msg.parseFrom(data);
        if (firmwareMsg.hasFirmwareDataMsg()) {
            Firmware.firmware_data_message fwDataMsg = firmwareMsg.getFirmwareDataMsg();
            if (fwDataMsg.hasBleBootFirmwareVer()) {
                gbDevice.setFirmwareVersion2(String.valueOf(fwDataMsg.getBleBootFirmwareVer()));
            }
        }
    }

    public static byte[] craftData(int mainService, int secondService, int command, byte[] data) {
        return craftData(mainService, secondService, command, data, false);
    }

    public static byte[] craftData(int mainService, int secondService, int command, byte[] data, boolean fileoperation) {
        // 010C14FF02FFFF00064A 01FFFFFFFFFFFFFFFFF0 080C10141802
        byte[] result = new byte[IGPSportConstants.DATA_TEMPLATE.length + data.length];
        System.arraycopy(IGPSportConstants.DATA_TEMPLATE, 0, result, 0, IGPSportConstants.DATA_TEMPLATE.length);
        result[1] = (byte) mainService;
        result[2] = (byte) secondService;
        if (fileoperation) {
            result[3] = (byte) 0x55;
        } else {
            result[3] = (byte) 0xff;
        }
        result[4] = (byte) command;

        result[7] = (byte) ((data.length >> 8) & 0xff);
        result[8] = (byte) (data.length & 0xff);
        result[9] = (byte) CheckSums.getCRC8(data);
        byte[] header = Arrays.copyOfRange(result, 0, 19);
        result[19] = (byte)CheckSums.getCRC8(header);
        System.arraycopy(data, 0, result, DATA_HEADER_SIZE, data.length);
        //debug
        LOG.info(GB.hexdump(result), "crafted packet");
        return result;
    }

    public static byte[] craftFileData(int mainService, int secondService, int command, byte[] data, byte[] fileData) {
        // 0115ffaa03ffff00000001ffffffffffffffff57 0000001f 08151003180220c71628e6ea44320b7465737420616b6164656d3a03636e78 efbbbf3c3f786d6c2076657273696f6e3d22312e302220656e...
        byte[] result = new byte[IGPSportConstants.DATA_TEMPLATE.length + data.length + 4 + fileData.length];
        System.arraycopy(IGPSportConstants.DATA_TEMPLATE, 0, result, 0, IGPSportConstants.DATA_TEMPLATE.length);
        result[1] = (byte) mainService;
        result[2] = (byte) secondService;
        result[3] = (byte) 0xaa;
        result[4] = (byte) command;

        result[7] = (byte) 0x00;
        result[8] = (byte) 0x00;
        result[9] = (byte) 0x00;
        byte[] header = Arrays.copyOfRange(result, 0, 19);
        result[19] = (byte)CheckSums.getCRC8(header);

        ByteBuffer buf = ByteBuffer.wrap(result, DATA_HEADER_SIZE,4);
        buf.putInt(data.length);

        System.arraycopy(data, 0, result, 24, data.length);
        System.arraycopy(fileData, 0, result, 24+data.length, fileData.length);
        //debug
        LOG.info(GB.hexdump(result), "crafted fileData packet");
        return result;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        LOG.debug("iGPSport notification: " + notificationSpec.type);
        TransactionBuilder builder = createTransactionBuilder("notification");

        Ins.ins_msg.Builder insMsgBuilder = Ins.ins_msg.newBuilder();
        insMsgBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_INS);
        insMsgBuilder.setInsServiceType(Ins.INS_SERVICE_TYPE.enum_INS_SERVICE_TYPE_NOTE);
        insMsgBuilder.setInsOperateType(Ins.INS_OPERATE_TYPE.enum_INS_OPERATE_TYPE_INCOMING_NOTE);
        Ins.ins_data_message.Builder insDataMsgBuilder = Ins.ins_data_message.newBuilder();
        if (notificationSpec.type == NotificationType.GENERIC_SMS) {
            insDataMsgBuilder.setIsApp(0);
        } else {
            insDataMsgBuilder.setIsApp(1);
            insDataMsgBuilder.setAppName(notificationSpec.sourceName);
        }

        if (notificationSpec.phoneNumber != null)
            insDataMsgBuilder.setTelNum(ByteString.copyFromUtf8(notificationSpec.phoneNumber));
        if (notificationSpec.title != null)
            insDataMsgBuilder.setName(notificationSpec.title);
        if (notificationSpec.body != null)
            insDataMsgBuilder.setContent(notificationSpec.body);
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        insDataMsgBuilder.setTime(timeStamp);
        insMsgBuilder.setInsDataMsg(insDataMsgBuilder);
        byte[] callData = craftData(insMsgBuilder.getServiceType().getNumber(), insMsgBuilder.getInsServiceType().getNumber(), insMsgBuilder.getInsOperateType().getNumber(), insMsgBuilder.build().toByteArray());
        builder.write(writeCharacteristic, callData);
        builder.queue();
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        LOG.debug("iGPSport send call notification");
        TransactionBuilder builder = createTransactionBuilder("CALL");
        Ins.ins_msg.Builder insMsgBuilder = Ins.ins_msg.newBuilder();
        insMsgBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_INS);
        insMsgBuilder.setInsServiceType(Ins.INS_SERVICE_TYPE.enum_INS_SERVICE_TYPE_CALL);
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            insMsgBuilder.setInsOperateType(Ins.INS_OPERATE_TYPE.enum_INS_OPERATE_TYPE_INCOMING_CALL);
        } else if (callSpec.command == CallSpec.CALL_ACCEPT ||
                callSpec.command == CallSpec.CALL_START ||
                callSpec.command == CallSpec.CALL_END) {
            insMsgBuilder.setInsOperateType(Ins.INS_OPERATE_TYPE.enum_INS_OPERATE_TYPE_ANSWER_CALL);
        } else if (callSpec.command == CallSpec.CALL_REJECT) {
            insMsgBuilder.setInsOperateType(Ins.INS_OPERATE_TYPE.enum_INS_OPERATE_TYPE_REJECT_CALL);
        }

        Ins.ins_data_message.Builder insDataMsgBuilder = Ins.ins_data_message.newBuilder();
        insDataMsgBuilder.setTelNum(ByteString.copyFromUtf8(callSpec.number));
        insDataMsgBuilder.setName(callSpec.name);
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        insDataMsgBuilder.setTime(timeStamp);
        insMsgBuilder.setInsDataMsg(insDataMsgBuilder);
        byte[] callData = craftData(insMsgBuilder.getServiceType().getNumber(), insMsgBuilder.getInsServiceType().getNumber(), insMsgBuilder.getInsOperateType().getNumber(), insMsgBuilder.build().toByteArray());
        builder.write(writeCharacteristic, callData);
        builder.queue();
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        TransactionBuilder builder = this.createTransactionBuilder("onfetchfitness");
        CyclingData.cycling_data_msg.Builder cycleDataMsg = CyclingData.cycling_data_msg.newBuilder();
        cycleDataMsg.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_CYCLING_DATA);
        cycleDataMsg.setCyclingDataOperateType(CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_LIST_GET);
//        cycleDataMsg.setListMsg(Common.file_list_get_message.newBuilder().setFileIndexStart(0).setFileIndexEnd(11) );
//        cycleDataMsg.setCyclingDataOperateType(CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_FILE_GET);
//        cycleDataMsg.addCyclingDataFileFlagMsg( CyclingData.cycling_data_file_flag_message.newBuilder().setTimestamp(1092299406) ); //FIXME: hardcoded value from commented request above

        byte[] cycleDataMsgBytes = craftData(cycleDataMsg.getServiceType().getNumber(), 0xff, cycleDataMsg.getCyclingDataOperateType().getNumber(), cycleDataMsg.build().toByteArray(), true);

        builder.write(writeCharacteristicThird, cycleDataMsgBytes);
        builder.queue();

    }

    @Override
    public void onSendConfiguration(String config) {

        LOG.debug("iGPSport on send config: " + config);
        try {
            TransactionBuilder builder = performInitialized("sendConfiguration");
            switch (config) {
                case ActivityUser.PREF_USER_WEIGHT_KG:
                case ActivityUser.PREF_USER_GENDER:
                case ActivityUser.PREF_USER_HEIGHT_CM:
                case ActivityUser.PREF_USER_DATE_OF_BIRTH:
                    setUserData(builder);
                    break;
                case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                    setMeasurementSystem(builder);
                    break;
            }
            builder.queue();
        } catch (IOException e) {
            GB.toast(getContext(), "Error sending configuration: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void setMeasurementSystem(TransactionBuilder builder) {
        Config.config_msg.Builder configMsgBuilder = Config.config_msg.newBuilder();
        configMsgBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_CONFIG);
        configMsgBuilder.setConfigSeviceType(Config.CONFIG_SERVICE_TYPE.enum_CONFIG_SERVICE_TYPE_UNIT);
        configMsgBuilder.setConfigOperateType(Config.CONFIG_OPERATE_TYPE.enum_CONFIG_OPERATE_TYPE_SET);

        Config.unit_msg.Builder unitMsgBuilder = Config.unit_msg.newBuilder();

        //unitMsgBuilder.setUnitItem()  ??? need all unit items?
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            configMsgBuilder.addUnitMessage(Config.unit_msg.newBuilder().setUnitItem(Config.UNIT_ITEM.enum_UNIT_ITEM_DISTANCE).setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_INCH));
            configMsgBuilder.addUnitMessage(Config.unit_msg.newBuilder().setUnitItem(Config.UNIT_ITEM.enum_UNIT_ITEM_ELEVATION).setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_INCH));
            configMsgBuilder.addUnitMessage(Config.unit_msg.newBuilder().setUnitItem(Config.UNIT_ITEM.enum_UNIT_ITEM_WEIGHT).setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_INCH));
            configMsgBuilder.addUnitMessage(Config.unit_msg.newBuilder().setUnitItem(Config.UNIT_ITEM.enum_UNIT_ITEM_TEMPERATURE).setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_INCH));
        } else {
            configMsgBuilder.addUnitMessage(Config.unit_msg.newBuilder().setUnitItem(Config.UNIT_ITEM.enum_UNIT_ITEM_DISTANCE).setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_METRIC));
            configMsgBuilder.addUnitMessage(Config.unit_msg.newBuilder().setUnitItem(Config.UNIT_ITEM.enum_UNIT_ITEM_ELEVATION).setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_METRIC));
            configMsgBuilder.addUnitMessage(Config.unit_msg.newBuilder().setUnitItem(Config.UNIT_ITEM.enum_UNIT_ITEM_WEIGHT).setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_METRIC));
            configMsgBuilder.addUnitMessage(Config.unit_msg.newBuilder().setUnitItem(Config.UNIT_ITEM.enum_UNIT_ITEM_TEMPERATURE).setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_METRIC));
        }
        configMsgBuilder.addUnitMessage(unitMsgBuilder);

        byte[] confMsgBytes = craftData(configMsgBuilder.getServiceType().getNumber(),
                configMsgBuilder.getConfigSeviceType().getNumber(),
                configMsgBuilder.getConfigOperateType().getNumber(),
                configMsgBuilder.build().toByteArray());
        builder.write(writeCharacteristic, confMsgBytes);

    }

    private void setUserData(TransactionBuilder builder) {
        Config.config_msg.Builder configMsgBuilder = Config.config_msg.newBuilder();
        configMsgBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_CONFIG);
        configMsgBuilder.setConfigSeviceType(Config.CONFIG_SERVICE_TYPE.enum_CONFIG_SERVICE_TYPE_USER);
        configMsgBuilder.setConfigOperateType(Config.CONFIG_OPERATE_TYPE.enum_CONFIG_OPERATE_TYPE_SET);

        Config.user_data_msg.Builder userDataMsgBuilder = Config.user_data_msg.newBuilder();

        ActivityUser user = new ActivityUser();
        userDataMsgBuilder.setAge(user.getAge());
        userDataMsgBuilder.setHeight(user.getHeightCm());
        userDataMsgBuilder.setWeight(user.getWeightKg()*10);
        userDataMsgBuilder.setSex(user.getGender()); // matches GB 0 - female, 1 - male
        configMsgBuilder.setUserDataMessage(userDataMsgBuilder);
        byte[] confMsgBytes = craftData(configMsgBuilder.getServiceType().getNumber(),
                configMsgBuilder.getConfigSeviceType().getNumber(),
                configMsgBuilder.getConfigOperateType().getNumber(),
                configMsgBuilder.build().toByteArray());
        builder.write(writeCharacteristic, confMsgBytes);


    }

    @Override
    public void onInstallApp(Uri uri, @NonNull final Bundle options) {
        //final IGPSportGpxRouteInstallHandler gpxRouteHandler = new IGPSportGpxRouteInstallHandler(uri, getContext());
        final IGPSportRouteInstallHandler routeHandler = new IGPSportRouteInstallHandler(uri, getContext());
        if (routeHandler.isValid()) {

            routeManager.uploadRoute(routeHandler);

        }
        return;
    }

    @Override
    public void onAppInfoReq() {
            routeManager.requestRouteList();
    }

    @Override
    public void onAppStart(final UUID uuid, boolean start) {
        if (start) {
           routeManager.activateRoute(uuid);
        }
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        routeManager.deleteRoute(uuid);
    }

    @Override
    public void onSendWeather() {
        WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec != null) {
            weatherManager.handleWeather(weatherSpec);
        }
    }

    private void handleWeather() {
        // Send weather
        final ArrayList<WeatherSpec> specs = new ArrayList<>(nodomain.freeyourgadget.gadgetbridge.model.weather.Weather.getWeatherSpecs());
        if (!specs.isEmpty()) {
            weatherManager.handleWeather(specs.get(0));
        }
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        this.mediaManager = new MediaManager(context);
        this.downloadManager.setContext(context);
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        if (!mediaManager.onSetMusicInfo(musicSpec)) {
            return;
        }

        TransactionBuilder builder = createTransactionBuilder("Music");

        LOG.debug("onSetMusicInfo: {}", musicSpec.toString());

        Media.track_message.Builder track_message = Media.track_message.newBuilder()
                .setAlbum(musicSpec.album)
                .setArtist(musicSpec.artist)
                .setTitle(musicSpec.track)
                .setTotalTime(musicSpec.duration);

        Media.queue_message.Builder queue_message = Media.queue_message.newBuilder()
                .setQueueCount(musicSpec.trackCount)
                .setQueueIndex(musicSpec.trackNr);


        Media.player_message.Builder player_message = Media.player_message.newBuilder();


        final MusicStateSpec bufferMusicStateSpec = mediaManager.getBufferMusicStateSpec();
        if (bufferMusicStateSpec != null) {
            player_message.setPlayerState(getPlayerState(bufferMusicStateSpec.state))
                    .setPlayerRate(bufferMusicStateSpec.playRate)
                    .setElapsedTime(bufferMusicStateSpec.position)
                    .setVolumeCur(mediaManager.getPhoneVolume())
                    .setVolumeMax(100);
        }

        Media.media_message.Builder media_message = Media.media_message.newBuilder()
                .setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_MUSIC_CTL)
                .setOperateType(Common.SERVICE_OPERATE_TYPE.enum_SERVICE_OPERATE_TYPE_SET)
                .setTrackMsg(track_message)
                .setQueueMsg(queue_message)
                .setPlayerMsg(player_message);

        byte[] mediaData = craftData(media_message.getServiceType().getNumber(), 0xff, media_message.getOperateType().getNumber(), media_message.build().toByteArray());
        builder.write(writeCharacteristic, mediaData);
        builder.queue();

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        if (!mediaManager.onSetMusicState(stateSpec)) {
            return;
        }

        LOG.debug("onSetMusicState: {}", stateSpec.toString());
        TransactionBuilder builder = createTransactionBuilder("Music");

        Media.player_message.Builder player_message = Media.player_message.newBuilder();
        final MusicStateSpec bufferMusicStateSpec = mediaManager.getBufferMusicStateSpec();
        if (bufferMusicStateSpec != null) {
            player_message.setPlayerState(getPlayerState(bufferMusicStateSpec.state))
                    .setPlayerRate(bufferMusicStateSpec.playRate)
                    .setElapsedTime(bufferMusicStateSpec.position);
        }

        Media.media_message.Builder media_message = Media.media_message.newBuilder()
                .setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_MUSIC_CTL)
                .setOperateType(Common.SERVICE_OPERATE_TYPE.enum_SERVICE_OPERATE_TYPE_SET)
                .setPlayerMsg(player_message);

        byte[] mediaData = craftData(media_message.getServiceType().getNumber(), 0xff, media_message.getOperateType().getNumber(), media_message.build().toByteArray());
        builder.write(writeCharacteristic, mediaData);
        builder.queue();

    }

    @Override
    public void onSetPhoneVolume(float volume) {
        TransactionBuilder builder = createTransactionBuilder("Music");

        Media.player_message.Builder player_message = Media.player_message.newBuilder();
        player_message.setVolumeCur(Math.round(volume))
                .setVolumeMax(100);

        Media.media_message.Builder media_message = Media.media_message.newBuilder()
                .setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_MUSIC_CTL)
                .setOperateType(Common.SERVICE_OPERATE_TYPE.enum_SERVICE_OPERATE_TYPE_SET)
                .setPlayerMsg(player_message);

        byte[] mediaData = craftData(media_message.getServiceType().getNumber(), 0xff, media_message.getOperateType().getNumber(), media_message.build().toByteArray());
        builder.write(writeCharacteristic, mediaData);
        builder.queue();

    }

    Media.PLAYER_STATE getPlayerState(byte gbState) {
        switch (gbState) {
            case MusicStateSpec.STATE_PLAYING:
                return Media.PLAYER_STATE.PLAYING;
            case  MusicStateSpec.STATE_UNKNOWN:
            case MusicStateSpec.STATE_PAUSED:
            case MusicStateSpec.STATE_STOPPED:
                return Media.PLAYER_STATE.PAUSED;
        }
        return Media.PLAYER_STATE.PAUSED;
    }


    public File getWritableExportDirectory() throws IOException {
        return getDevice().getDeviceCoordinator().getWritableExportDirectory(getDevice(), true);
    }

}
