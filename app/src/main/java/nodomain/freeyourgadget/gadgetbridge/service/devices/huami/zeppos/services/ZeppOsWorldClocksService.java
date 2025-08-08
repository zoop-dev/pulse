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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsWorldClocksService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsWorldClocksService.class);

    public static final short ENDPOINT = 0x0008;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_LIST_SET = 0x03;
    public static final byte CMD_LIST_SET_ACK = 0x04;
    public static final byte CMD_LIST_GET = 0x05;
    public static final byte CMD_LIST_RET = 0x06;

    public ZeppOsWorldClocksService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (payload[0]) {
            case CMD_LIST_SET_ACK:
                LOG.info("World clocks list set ack, status = {}", payload[1]);
                return;
            default:
                LOG.warn("Unexpected world clocks byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public void onSetWorldClocks(final ArrayList<? extends WorldClock> clocks) {
        final DeviceCoordinator coordinator = getCoordinator();
        if (coordinator.getWorldClocksSlotCount() == 0) {
            LOG.warn("Got world clocks, but device does not support them");
            return;
        }

        write("send world clocks", encodeWorldClocks(clocks, coordinator, getSupport().getDevice()));
    }

    public static byte[] encodeWorldClocks(final List<? extends WorldClock> clocks, final DeviceCoordinator coordinator, final GBDevice device) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(0x03);
            baos.write(clocks.size());
            int i = 0;
            for (final WorldClock clock : clocks) {
                baos.write(i++);
                baos.write(encodeWorldClock(clock, coordinator, device));
            }
        } catch (final IOException e) {
            throw new RuntimeException("This should never happen", e);
        }

        return baos.toByteArray();
    }

    public static byte[] encodeWorldClock(final WorldClock clock, final DeviceCoordinator coordinator, final GBDevice device) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            final TimeZone timezone = TimeZone.getTimeZone(clock.getTimeZoneId());
            final ZoneId zoneId = ZoneId.of(clock.getTimeZoneId());

            // Usually the 3-letter city code (eg. LIS for Lisbon)
            if (clock.getCode() != null) {
                baos.write(StringUtils.truncate(clock.getCode(), 3).toUpperCase().getBytes(StandardCharsets.UTF_8));
            } else {
                baos.write(StringUtils.truncate(clock.getLabel(), 3).toUpperCase().getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0x00);

            // Some other string? Seems to be empty
            baos.write(0x00);

            // The city name / label that shows up on the band
            baos.write(StringUtils.truncate(clock.getLabel(), coordinator.getWorldClocksLabelLength()).getBytes(StandardCharsets.UTF_8));
            baos.write(0x00);

            // The raw offset from UTC, in number of 15-minute blocks
            baos.write((int) (timezone.getRawOffset() / (1000L * 60L * 15L)));

            // Daylight savings
            final boolean useDaylightTime = timezone.useDaylightTime();
            final boolean inDaylightTime = timezone.inDaylightTime(new Date());
            byte daylightByte = 0;
            // The daylight savings offset, either currently (the previous transition) or future (the next transition), in minutes
            byte daylightOffsetMinutes = 0;

            final ZoneRules zoneRules = zoneId.getRules();
            if (useDaylightTime) {
                final ZoneOffsetTransition transition;
                if (inDaylightTime) {
                    daylightByte = 0x01;
                    transition = zoneRules.previousTransition(Instant.now());
                } else {
                    daylightByte = 0x02;
                    transition = zoneRules.nextTransition(Instant.now());
                }
                daylightOffsetMinutes = (byte) transition.getDuration().toMinutes();
            }

            baos.write(daylightByte);
            baos.write(daylightOffsetMinutes);

            // The timestamp of the next daylight savings transition, if any
            final ZoneOffsetTransition nextTransition = zoneRules.nextTransition(Instant.now());
            long nextTransitionTs = 0;
            if (nextTransition != null) {
                nextTransitionTs = nextTransition
                        .getDateTimeBefore()
                        .atZone(zoneId)
                        .toEpochSecond();
            }

            for (int i = 0; i < 4; i++) {
                baos.write((byte) ((nextTransitionTs >> (i * 8)) & 0xff));
            }

            if (coordinator.supportsDisabledWorldClocks(device)) {
                baos.write((byte) (clock.getEnabled() ? 0x01 : 0x00));
            }

            return baos.toByteArray();
        } catch (final IOException e) {
            throw new RuntimeException("This should never happen", e);
        }
    }
}
