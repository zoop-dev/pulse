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

import android.location.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;

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

    private static final int GPS_FLAG_STATUS = 0x1;
    private static final int GPS_FLAG_POSITION = 0x40000;

    public static byte[] encodePhoneGpsPayload(final HuamiPhoneGpsStatus status, final Location location) {

        int flags = 0;
        int length = 4; // Start with just the flag bytes

        if (status != null) {
            flags |= GPS_FLAG_STATUS;
            length += 1;
        }

        if (location != null) {
            flags |= GPS_FLAG_POSITION;
            length += 31;
        }

        final ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.putInt(flags);

        if (status != null) {
            buf.put(status.getCode());
        }

        if (location != null) {
            buf.putInt((int) (location.getLongitude() * 3000000.0));
            buf.putInt((int) (location.getLatitude() * 3000000.0));
            buf.putInt((int) location.getSpeed() * 10);

            buf.putInt((int) (location.getAltitude() * 100));
            buf.putLong(location.getTime());

            // Seems to always be ff ?
            buf.putInt(0xffffffff);

            // Not sure what this is, maybe bearing? It changes while moving, but
            // doesn't seem to be needed on the Mi Band 5
            buf.putShort((short) 0x00);

            // Seems to always be 0 ?
            buf.put((byte) 0x00);
        }

        return buf.array();
    }

    public static byte[] getCurrentTimeBytes() {
        // It seems that the format sent to the Current Time characteristic changed in newer devices
        // to kind-of match the GATT spec, but it doesn't quite respect it?
        // - 11 bytes get sent instead of 10 (extra byte at the end for the offset in quarter-hours?)
        // - Day of week starts at 0
        // Otherwise, the command gets rejected with an "Out of Range" error and init fails.

        final Calendar timestamp = BLETypeConversions.createCalendar();

        final ByteBuffer buf = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) timestamp.get(Calendar.YEAR));
        buf.put((byte) (timestamp.get(Calendar.MONTH) + 1));
        buf.put((byte) timestamp.get(Calendar.DATE));
        buf.put((byte) timestamp.get(Calendar.HOUR_OF_DAY));
        buf.put((byte) timestamp.get(Calendar.MINUTE));
        buf.put((byte) timestamp.get(Calendar.SECOND));
        buf.put((byte) (timestamp.get(Calendar.DAY_OF_WEEK) - 1));
        buf.put(BLETypeConversions.fromUint8((int) (timestamp.get(Calendar.MILLISECOND) / 1000. * 256))); // Fractions256

        final ZoneId zoneId = ZoneId.systemDefault();
        final ZoneRules rules = zoneId.getRules();
        // reason
        if (rules.isDaylightSavings(Instant.now())) {
            buf.put((byte) 0x08);
        } else {
            buf.put((byte) 0x00);
        }
        buf.put((byte) (rules.getOffset(Instant.now()).getTotalSeconds() / (60 * 15)));

        return buf.array();
    }
}
