package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.airpro4;

import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunDeviceSupport;

public class EarFunAirPro4DeviceSupport extends EarFunDeviceSupport {

    @Override
    protected EarFunAirPro4Protocol createDeviceProtocol() {
        return new EarFunAirPro4Protocol(getDevice());
    }
}
