package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuamiUtils {
    private HuamiUtils() {
        // Utility class
    }

    public static int getFindDeviceInterval(final GBDevice device,
                                            final boolean supportsDeviceDefaultVibrationProfiles) {
        final VibrationProfile findBand = HuamiCoordinator.getVibrationProfile(
                device.getAddress(),
                HuamiVibrationPatternNotificationType.FIND_BAND,
                supportsDeviceDefaultVibrationProfiles
        );
        int findDeviceInterval = 0;

        if (findBand != null) {
            // It can be null if the device supports continuous find mode
            // If that's the case, this function shouldn't even have been called
            for (int len : findBand.getOnOffSequence())
                findDeviceInterval += len;

            if (findBand.getRepeat() > 0)
                findDeviceInterval *= findBand.getRepeat();

            if (findDeviceInterval > 10000) // 10 seconds, about as long as Mi Fit allows
                findDeviceInterval = 10000;
        } else {
            findDeviceInterval = 10000;
        }

        return findDeviceInterval;
    }
}
