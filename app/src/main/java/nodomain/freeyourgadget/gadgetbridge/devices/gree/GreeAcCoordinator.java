package nodomain.freeyourgadget.gadgetbridge.devices.gree;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gree.GreeAcSupport;

public class GreeAcCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice, @NonNull final Device device, @NonNull final DaoSession session) throws GBException {
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // GR-AC_10001_09_xxxx_SC
        return Pattern.compile("^GR-AC_\\d{5}_\\d{2}_[0-9a-f]{4}_SC$");
    }

    @Override
    public String getManufacturer() {
        return "Gree";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return GreeAcSupport.class;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_gree_ac;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_air_conditioning;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_air_conditioning_disabled;
    }

    @Override
    public boolean suggestUnbindBeforePair() {
        // shouldn't matter
        return false;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return GreeAcPairingActivity.class;
    }
}
