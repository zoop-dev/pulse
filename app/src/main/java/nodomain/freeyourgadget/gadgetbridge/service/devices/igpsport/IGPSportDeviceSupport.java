package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import com.google.protobuf.InvalidProtocolBufferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Ble;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Firmware;
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
    public final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    public final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    public final DeviceInfoProfile<IGPSportDeviceSupport> deviceInfoProfile;
    public final BatteryInfoProfile<IGPSportDeviceSupport> batteryInfoProfile;


    private int mtuSize=247; //FIXME use actual device mtu
    public IGPSportDeviceSupport() {
        super(LOG);

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
        byte[] bleBondData = craftData((byte) bleBuilder.getServiceType().getNumber(), (byte) 0xFF, (byte)bleBuilder.getBleOperateType().getNumber(), bleBuilder.build().toByteArray());
        builder.write(writeCharacteristic, bleBondData);


        Firmware.firmware_msg.Builder firmwareBuilder = Firmware.firmware_msg.newBuilder();
        firmwareBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_FIRMWARE);
        firmwareBuilder.setFirmwareOperateType(Firmware.FIRMWARE_OPERATE_TYPE.enum_FIRMWARE_OPERATE_TYPE_GET_VERSION);
        byte[] firmwareGetVersionData = craftData((byte)firmwareBuilder.getServiceType().getNumber(), (byte)0xff,(byte)firmwareBuilder.getFirmwareOperateType().getNumber(), firmwareBuilder.build().toByteArray());
        builder.write(writeCharacteristic, firmwareGetVersionData);

        Factory.factory_msg.Builder factoryBuilder = Factory.factory_msg.newBuilder();
        factoryBuilder.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_FACTORY);
        factoryBuilder.setFactoryOperateType(Factory.FACTORY_OPERATE_TYPE.enum_FACTORY_OPERATE_TYPE_BATTARY_GET);
        byte[] factoryGetBatterydata = craftData((byte)factoryBuilder.getServiceType().getNumber(), (byte)0xff, (byte)factoryBuilder.getFactoryOperateType().getNumber(), factoryBuilder.build().toByteArray());

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
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        byte[] value = characteristic.getValue();

        LOG.info("Characteristic changed UUID: " + characteristicUUID);
        LOG.info("Characteristic changed value: " + GB.hexdump(characteristic.getValue()));

        byte[] data = characteristic.getValue();
        if (data[0] != IGPSportConstants.DATA_HEADER) {
            LOG.info("FitPro, packet not starting with 0x01: " + data[0] + "other message types not implemented yet");
            return false;
        }
        if (data != null && data.length > 20) {
            byte mainService = data[1];
            byte mainOperation = data[4];
            int dataSize =  data[8] + (data[7] << 8);

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
                }
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
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


    public static byte[] craftData(byte mainService, byte secondService, byte command, byte[] data) {
        // 010C14FF02FFFF00064A 01FFFFFFFFFFFFFFFFF0 080C10141802
        byte[] result = new byte[IGPSportConstants.DATA_TEMPLATE.length + data.length];
        System.arraycopy(IGPSportConstants.DATA_TEMPLATE, 0, result, 0, IGPSportConstants.DATA_TEMPLATE.length);
        result[1] = (byte) mainService;
        result[2] = (byte) secondService;
        result[4] = command;

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


}
