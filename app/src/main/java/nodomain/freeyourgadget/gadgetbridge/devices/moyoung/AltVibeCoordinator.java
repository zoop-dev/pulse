package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AltVibeCoordinator extends AbstractMoyoungDeviceCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Alt Vibe$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_alt_vibe;
    }

    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miwatch;
    }

    @Override
    public String getManufacturer() {
        return "Mo Young / Da Fit";
    }

    @Override
    public int getMtu() {
        return 508;
    }

    @Override
    public boolean supportsBloodPressureMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsDeviceInfoProfile() {
        return false;
    }
}
