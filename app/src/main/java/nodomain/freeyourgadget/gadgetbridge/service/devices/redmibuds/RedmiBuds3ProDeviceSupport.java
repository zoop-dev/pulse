package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds;

import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class RedmiBuds3ProDeviceSupport extends RedmiBudsDeviceSupport {
    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new RedmiBuds3ProProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new RedmiBudsIOThread(getDevice(), getContext(),
                (RedmiBuds3ProProtocol) getDeviceProtocol(),
                RedmiBuds3ProDeviceSupport.this, getBluetoothAdapter());
    }
}
