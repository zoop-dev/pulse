package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.q30;

import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class SoundcoreQ30DeviceSupport extends AbstractSerialDeviceSupport {

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new SoundcoreQ30Protocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new SoundcoreQ30IOThread(getDevice(), getContext(), (SoundcoreQ30Protocol) getDeviceProtocol(),this, getBluetoothAdapter());
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
