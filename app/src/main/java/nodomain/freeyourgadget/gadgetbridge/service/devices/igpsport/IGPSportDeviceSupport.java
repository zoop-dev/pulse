package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;

import org.slf4j.Logger;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;

public class IGPSportDeviceSupport extends AbstractBTLEDeviceSupport {
    public IGPSportDeviceSupport(Logger logger) {
        super(logger);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
