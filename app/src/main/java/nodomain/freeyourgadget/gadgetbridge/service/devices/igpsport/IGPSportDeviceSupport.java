package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;

public class IGPSportDeviceSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(IGPSportDeviceSupport.class);
    public BluetoothGattCharacteristic readCharacteristic;
    public BluetoothGattCharacteristic writeCharacteristic;
    public final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    public final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    public final DeviceInfoProfile<IGPSportDeviceSupport> deviceInfoProfile;
    public final BatteryInfoProfile<IGPSportDeviceSupport> batteryInfoProfile;



    public IGPSportDeviceSupport() {
        super(LOG);

        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_SERVICE);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_REPORT);

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

    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // mark the device as initializing
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        readCharacteristic = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_REPORT);
        writeCharacteristic = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_CONTROL);

        builder.notify(getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_REPORT), true);
        builder.notify(getCharacteristic(GattService.UUID_SERVICE_BATTERY_SERVICE), true);
        builder.setCallback(this);

        deviceInfoProfile.requestDeviceInfo(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);
        deviceInfoProfile.enableNotify(builder, true);

        // ... custom initialization logic ...

        // set device firmware to prevent the following error when you (later) try to save data to database and
        // device firmware has not been set yet
        // Error executing 'the bind value at index 2 is null'java.lang.IllegalArgumentException: the bind value at index 2 is null
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        // mark the device as initialized
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        return builder;
    }

    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        byte[] value = characteristic.getValue();

        LOG.info("Characteristic changed UUID: " + characteristicUUID);
        LOG.info("Characteristic changed value: " + characteristic.getValue());
        return false;
    }


}
