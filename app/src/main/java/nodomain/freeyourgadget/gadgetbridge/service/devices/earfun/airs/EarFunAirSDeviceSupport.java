package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airs;

import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class EarFunAirSDeviceSupport extends EarFunDeviceSupport {

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new EarFunAirSProtocol(getDevice());
    }
}
