package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Back;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Ble;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Config;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.CyclingData;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Firmware;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.GeneralFileOperation;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Ins;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.RoutePlan;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Common;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Factory;


public class IGPSportDeviceSupport extends AbstractBTLEDeviceSupport {

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


    private int mtuSize=247; //FIXME use actual device mtu
    public IGPSportDeviceSupport() {
        super(LOG);

        routeManager = new IGPSportRoutesManager(this);

        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);


        IntentListener mListener = new IntentListener() {
            @Override
            public void notify(Intent intent) {
                String action = intent.getAction();
                if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                    handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
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

        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_RX);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_SERVICE);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_SECOND_RX);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_SECOND_SERVICE);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_THIRD_RX);
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


    public void handleBatteryInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo info) {
        LOG.debug("iGPSport battery info: " + info);
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // mark the device as initializing
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        readCharacteristic = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_RX);
        writeCharacteristic = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_TX);
        writeCharacteristicThird = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_THIRD_TX);
        writeCharacteristicFourth = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FOURTH_TX);

        builder.notify(getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_FIRST_RX), true);
        builder.notify(getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_SECOND_RX), true);
        builder.notify(getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_THIRD_RX), true);
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

        // set device firmware to prevent the following error when you (later) try to save data to database and
        // device firmware has not been set yet
        // Error executing 'the bind value at index 2 is null'java.lang.IllegalArgumentException: the bind value at index 2 is null
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        // mark the device as initialized
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        return builder;
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

        byte[] data = characteristic.getValue();
        if (data[0] == IGPSportConstants.DATA_HEADER) {

            if (data != null && data.length > 20) {
                byte mainService = data[1];
                byte mainOperation = data[4];
                int dataSize = ByteBuffer.wrap(data, 7, 2).getShort();

                byte[] pbData = new byte[dataSize];
                System.arraycopy(data, 20, pbData, 0, dataSize);

                try {
                    switch (mainService) {
                        case Common.service_type_index.enum_SERVICE_TYPE_INDEX_FACTORY_VALUE:
                            handleFactoryData(pbData);
                            break;
                        case Common.service_type_index.enum_SERVICE_TYPE_INDEX_FIRMWARE_VALUE:
                            handleFirmwareData(pbData);
                            break;
                        case Common.service_type_index.enum_SERVICE_TYPE_INDEX_ROUTE_PLAN_VALUE:
                            if(mainOperation == RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_LIST_NUM_GET_VALUE) {
                                routeManager.handleRouteNumber(pbData);
                            } else if (mainOperation == RoutePlan.ROUTE_PLAN_OPERATE_TYPE.enum_ROUTE_PLAN_OPERATE_TYPE_LIST_SEND_VALUE) {
                                routeManager.handleRouteList(pbData);
                            }
                            break;
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }

            }
        }

        if (data[0] == 0x02) {
            //0215FFFF03FFFF00FFFFFFFFFFFFFFFFFFFFFF55
            byte mainService = data[1];
            byte mainOperation = data[4];
            byte result = data[7];
            switch (mainService) {
                case Common.service_type_index.enum_SERVICE_TYPE_INDEX_FILE_OPERATION_VALUE:
                    if(mainOperation == Common.SERVICE_OPERATE_TYPE.enum_SERVICE_OPERATE_TYPE_ADD_VALUE) {
                        gbDevice.unsetBusyTask();
                        gbDevice.sendDeviceUpdateIntent(getContext());
                        TransactionBuilder builder = new TransactionBuilder("Route  upload finished");
                        if (result == 0) {
                            builder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction(
                                    "Route upload completed",
                                    false,
                                    100,
                                    getContext()
                            ));
                            handleGBDeviceEvent(new GBDeviceEventDisplayMessage("Route upload completed", Toast.LENGTH_LONG, GB.INFO));
                        } else {
                            builder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction(
                                    "Route upload failed",
                                    false,
                                    0,
                                    getContext()
                            ));
                            handleGBDeviceEvent(new GBDeviceEventDisplayMessage("Failed to upload route", Toast.LENGTH_LONG, GB.ERROR));
                        }
                        builder.queue(getQueue());
                    }
                    break;
            }
        }

        return false;
    }


    public void handleFactoryData(byte[] data) throws InvalidProtocolBufferException {
        Factory.factory_msg factoryMsg = Factory.factory_msg.parseFrom(data);
        if (factoryMsg.hasBattaryMsg()) {
            gbDevice.setBatteryLevel(factoryMsg.getBattaryMsg().getPowerPercent());
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
        System.arraycopy(data, 0, result, 20, data.length);
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

        ByteBuffer buf = ByteBuffer.wrap(result, 20,4);
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
        TransactionBuilder builder = new TransactionBuilder("notification");

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
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());
        insDataMsgBuilder.setTime(timeStamp);
        insMsgBuilder.setInsDataMsg(insDataMsgBuilder);
        byte[] callData = craftData(insMsgBuilder.getServiceType().getNumber(), insMsgBuilder.getInsServiceType().getNumber(), insMsgBuilder.getInsOperateType().getNumber(), insMsgBuilder.build().toByteArray());
        builder.write(writeCharacteristic, callData);
        builder.queue(getQueue());
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        LOG.debug("iGPSport send call notification");
        TransactionBuilder builder = new TransactionBuilder("CALL");
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
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());
        insDataMsgBuilder.setTime(timeStamp);
        insMsgBuilder.setInsDataMsg(insDataMsgBuilder);
        byte[] callData = craftData(insMsgBuilder.getServiceType().getNumber(), insMsgBuilder.getInsServiceType().getNumber(), insMsgBuilder.getInsOperateType().getNumber(), insMsgBuilder.build().toByteArray());
        builder.write(writeCharacteristic, callData);
        builder.queue(getQueue());
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        TransactionBuilder builder = this.createTransactionBuilder("onfetchfitness");
        CyclingData.cycling_data_msg.Builder cycleDataMsg = CyclingData.cycling_data_msg.newBuilder();
        cycleDataMsg.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_CYCLING_DATA);
//        cycleDataMsg.setCyclingDataOperateType(CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_LIST_GET);
//        cycleDataMsg.setListMsg(Common.file_list_get_message.newBuilder().setFileIndexStart(0).setFileIndexEnd(11) );
        cycleDataMsg.setCyclingDataOperateType(CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_FILE_GET);
        cycleDataMsg.addCyclingDataFileFlagMsg( CyclingData.cycling_data_file_flag_message.newBuilder().setTimestamp(1092299406) ); //FIXME: hardcoded value from commented request above


        byte[] cycleDataMsgBytes = craftData(cycleDataMsg.getServiceType().getNumber(), 0xff, cycleDataMsg.getCyclingDataOperateType().getNumber(), cycleDataMsg.build().toByteArray(), true);

        builder.write(writeCharacteristicThird, cycleDataMsgBytes);
        builder.queue(getQueue());

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
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error sending configuration: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void setMeasurementSystem(TransactionBuilder builder) {
        Config.config_msg.Builder configMsgBuilder = Config.config_msg.newBuilder();
        configMsgBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_CONFIG);
        configMsgBuilder.setConfigSeviceType(Config.CONFIG_SERVICE_TYPE.enum_CONFIG_SERVICE_TYPE_UNIT);

        Config.unit_msg.Builder unitMsgBuilder = Config.unit_msg.newBuilder();

        //unitMsgBuilder.setUnitItem()  ??? need all unit items?
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            unitMsgBuilder.setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_INCH);
        } else {
            unitMsgBuilder.setUnitType(Config.UNIT_TYPE.enum_UNIT_TYPE_METRIC);
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
        configMsgBuilder.setConfigOperateType(Config.CONFIG_OPERATE_TYPE.enum_CONFIG_OPERATE_TYPE_SEND);

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
    public void onInstallApp(Uri uri) {
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
    public void onSendWeather(ArrayList<WeatherSpec> weatherSpecs) {
        WeatherSpec weatherSpec = weatherSpecs.get(0);

        try {
            TransactionBuilder builder = performInitialized("set weather");
            Back.back_msg.Builder weatherMsg = Back.back_msg.newBuilder();
            Back.weather_current_data_message.Builder currentWeatherMsg =Back.weather_current_data_message.newBuilder();
            currentWeatherMsg.setCurDayMinTemp(weatherSpec.todayMinTemp-273);
            currentWeatherMsg.setCurDayMaxTemp(weatherSpec.todayMaxTemp-273);
            currentWeatherMsg.setCurTemperature(weatherSpec.currentTemp-273);
            currentWeatherMsg.setCurWeather(weatherSpec.currentConditionCode);
            currentWeatherMsg.setWindDeg(String.valueOf(weatherSpec.windDirection));
            currentWeatherMsg.setWindSpd(String.valueOf(weatherSpec.windSpeed));
            weatherMsg.setCurMsg(currentWeatherMsg);
            weatherMsg.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_BACK);
            weatherMsg.setBackOperateType(Back.BACK_OPERATE_TYPE.enum_BACK_OPERATE_TYPE_SEND);
            weatherMsg.setBackServiceType(Back.BACK_SERVICE_TYPE.enum_BACK_SERVICE_TYPE_WEATHER);


            byte[] weatherBytes = craftData(weatherMsg.getServiceType().getNumber(),
                    weatherMsg.getBackServiceType().getNumber(),
                    weatherMsg.getBackOperateType().getNumber(),
                    weatherMsg.build().toByteArray());
            builder.writeChunkedData(writeCharacteristicFourth, weatherBytes, getMTU());
            builder.queue(getQueue());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
