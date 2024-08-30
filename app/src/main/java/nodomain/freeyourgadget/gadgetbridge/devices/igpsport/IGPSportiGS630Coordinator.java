package nodomain.freeyourgadget.gadgetbridge.devices.igpsport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class IGPSportiGS630Coordinator extends IGPSportAbstractCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(IGPSportiGS630Coordinator.class);

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_igpsport_igs630;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("iGS630");
    }
}
