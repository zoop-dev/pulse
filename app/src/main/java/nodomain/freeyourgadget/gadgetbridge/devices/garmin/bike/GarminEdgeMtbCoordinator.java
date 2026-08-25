package nodomain.freeyourgadget.gadgetbridge.devices.garmin.bike;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class GarminEdgeMtbCoordinator extends GarminBikeComputerCoordinator {
    @Override
    public boolean isExperimental() {
        // Not tested, and the supported device name below is unconfirmed
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // TODO: unconfirmed device name
        return Pattern.compile("^Edge MTB$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_edge_mtb;
    }
}
