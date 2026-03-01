package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds;

import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;

public class RedmiBuds6ActiveDeviceSupport extends RedmiBudsDeviceSupport {
    @Override
    protected RedmiBuds6ActiveProtocol createDeviceProtocol() {
        return new RedmiBuds6ActiveProtocol(getDevice());
    }
}
