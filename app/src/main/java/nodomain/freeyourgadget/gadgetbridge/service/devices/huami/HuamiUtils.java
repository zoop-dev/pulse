/*  Copyright (C) 2025 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuamiUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HuamiUtils.class);

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

    public static List<Short> truncateVibrationsOnOff(final VibrationProfile profile,
                                                      final int limitMillis) {
        if (profile == null) {
            return Collections.emptyList();
        }

        int totalLengthMs = 0;

        // The on-off sequence, until the max total length is reached
        final List<Short> onOff = new ArrayList<>(profile.getOnOffSequence().length);

        for (int c = 0; c < profile.getRepeat(); c++) {
            for (int i = 0; i < profile.getOnOffSequence().length; i += 2) {
                final short on = (short) profile.getOnOffSequence()[i];
                final short off = (short) profile.getOnOffSequence()[i + 1];

                if (totalLengthMs + on + off > limitMillis) {
                    LOG.warn("VibrationProfile {} too long, truncating to {} ms", profile.getId(), limitMillis);
                    break;
                }

                onOff.add(on);
                onOff.add(off);
                totalLengthMs += on + off;
            }
        }

        return onOff;
    }
}
