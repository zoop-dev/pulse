package nodomain.freeyourgadget.gadgetbridge.devices.polar;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class PolarH9DeviceCoordinator extends AbstractPolarDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_polarh9;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Polar H9( \\w+)?$");
    }
}
