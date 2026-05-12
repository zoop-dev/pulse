package nodomain.freeyourgadget.gadgetbridge.devices.igpsport;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class IGPSportBSC300Coordinator extends IGPSportAbstractCoordinator{

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_igpsport_bsc300;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("BSC300");
    }

}
