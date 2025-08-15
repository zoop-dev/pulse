package nodomain.freeyourgadget.gadgetbridge.devices.igpsport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class IGPSportBSC200SCoordinator extends IGPSportAbstractCoordinator{
    private static final Logger LOG = LoggerFactory.getLogger(IGPSportBSC200SCoordinator.class);

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_igpsport_bsc200s;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("BSC200S");
    }

}
