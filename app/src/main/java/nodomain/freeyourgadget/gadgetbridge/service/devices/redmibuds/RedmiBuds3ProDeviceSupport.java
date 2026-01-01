package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds;

import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;

public class RedmiBuds3ProDeviceSupport extends RedmiBudsDeviceSupport {
    @Override
    protected RedmiBuds3ProProtocol createDeviceProtocol() {
        return new RedmiBuds3ProProtocol(getDevice());
    }
}
