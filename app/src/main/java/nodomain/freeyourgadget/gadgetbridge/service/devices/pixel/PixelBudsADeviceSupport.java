package nodomain.freeyourgadget.gadgetbridge.service.devices.pixel;

import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class PixelBudsADeviceSupport extends AbstractSerialDeviceSupport {
    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new PixelBudsAProtocol(getDevice());
    }

    @Override
    protected synchronized GBDeviceIoThread createDeviceIOThread() {
        return new PixelBudsAIOThread(
                getDevice(),
                getContext(),
                (PixelBudsAProtocol) getDeviceProtocol(),
                this,
                getBluetoothAdapter()
        );
    }
}
