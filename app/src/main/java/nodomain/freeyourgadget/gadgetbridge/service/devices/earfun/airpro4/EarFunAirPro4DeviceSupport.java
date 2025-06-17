package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airpro4;

import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class EarFunAirPro4DeviceSupport extends EarFunDeviceSupport {

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new EarFunAirPro4Protocol(getDevice());
    }
}
