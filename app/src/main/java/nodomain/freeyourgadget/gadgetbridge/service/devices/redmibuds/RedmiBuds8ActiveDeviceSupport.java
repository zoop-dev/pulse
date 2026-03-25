package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds;

import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;

public class RedmiBuds8ActiveDeviceSupport extends RedmiBudsDeviceSupport {
    @Override
    protected RedmiBuds8ActiveProtocol createDeviceProtocol() {
        return new RedmiBuds8ActiveProtocol(getDevice());
    }
}
