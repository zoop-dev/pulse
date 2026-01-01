package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airs;

import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunDeviceSupport;

public class EarFunAirSDeviceSupport extends EarFunDeviceSupport {

    @Override
    protected EarFunAirSProtocol createDeviceProtocol() {
        return new EarFunAirSProtocol(getDevice());
    }
}
