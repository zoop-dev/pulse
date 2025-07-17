package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.aeroFit;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class SoundcoreAeroFit2DeviceSupport extends AbstractSerialDeviceSupport {
    public static final UUID UUID_DEVICE_CTRL = UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3874");

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new SoundcoreAeroFitProtocol(getDevice());
    }

    @Override
    protected synchronized GBDeviceIoThread createDeviceIOThread() {
        return new SoundcoreAeroFitIOThread(
                getDevice(),
                getContext(),
                (SoundcoreAeroFitProtocol) getDeviceProtocol(),
                UUID_DEVICE_CTRL,
                this,
                getBluetoothAdapter()
        );
    }
}
