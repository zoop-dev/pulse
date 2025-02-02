package nodomain.freeyourgadget.gadgetbridge.devices.evenrealities;

import android.app.Activity;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities.G1DeviceSupport;

/**
 * Coordinator for the Even Realities G1 smart glasses. Describes the supported capabilities of the
 * device.
 * <p>
 * This class partners with G1DeviceSupport.java and G1PairingActivity.java
 */
public class G1DeviceCoordinator extends AbstractBLEDeviceCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger(G1DeviceCoordinator.class);

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return G1DeviceSupport.class;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return G1PairingActivity.class;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        // eg. G1_45_L_F2333, G1_63_R_04935.
        // Note that the G1_XX_L_YYYYY will have a corresponding G1_XX_R_ZZZZZ. The XX will match,
        // but the trailing 5 characters will not.
        return Pattern.compile("Even G1_\\d\\d_[L|R]_\\w+");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_even_realities_g1;
    }

    @Override
    public String getManufacturer() {
        return "Even Realities";
    }

    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_even_realities_g1;
    }

    @Override
    @DrawableRes
    public int getDisabledIconResource() {
        return R.drawable.ic_device_even_realities_g1_disabled;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_LAZY;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device,
                                @NonNull DaoSession session) throws GBException {
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }
}
