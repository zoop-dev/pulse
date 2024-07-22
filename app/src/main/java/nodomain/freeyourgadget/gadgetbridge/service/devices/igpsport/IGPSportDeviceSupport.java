package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;

import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.UUID_CHARACTERISTIC_RX;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.UUID_CHARACTERISTIC_TX;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.igpsport.IGPSportConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.makibeshr3.MakibesHR3DeviceSupport;

public class IGPSportDeviceSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(IGPSportDeviceSupport.class);
    public BluetoothGattCharacteristic readCharacteristic;
    public BluetoothGattCharacteristic writeCharacteristic;

    public IGPSportDeviceSupport() {
        super(LOG);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_SERVICE);
        addSupportedService(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_REPORT);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // mark the device as initializing
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        readCharacteristic = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_REPORT);
        writeCharacteristic = getCharacteristic(IGPSportConstants.UUID_IGPSPORT_CHARACTERISTIC_CONTROL);

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
