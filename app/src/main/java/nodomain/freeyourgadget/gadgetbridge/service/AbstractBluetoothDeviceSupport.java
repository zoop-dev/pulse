package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.hardware.usb.UsbAccessory;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class AbstractBluetoothDeviceSupport extends AbstractDeviceSupport {
    private BluetoothAdapter btAdapter;

    @Override
    public void setContext(@NonNull final GBDevice gbDevice,
                           @NonNull final BluetoothAdapter btAdapter,
                           @NonNull final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        this.btAdapter = btAdapter;
    }

    @Override
    public void setContext(@NonNull final GBDevice gbDevice,
                           @NonNull final UsbAccessory usbAccessory,
                           @NonNull final Context context) {
        throw new IllegalStateException("This is a Bluetooth support class");
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return btAdapter;
    }
}
