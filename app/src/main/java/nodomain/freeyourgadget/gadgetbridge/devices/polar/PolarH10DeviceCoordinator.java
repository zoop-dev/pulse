package nodomain.freeyourgadget.gadgetbridge.devices.polar;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class PolarH10DeviceCoordinator extends AbstractPolarDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_polarh10;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Polar H10.*");
    }
}
