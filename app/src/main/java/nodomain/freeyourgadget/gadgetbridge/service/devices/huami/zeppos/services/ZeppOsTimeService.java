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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;

public class ZeppOsTimeService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsTimeService.class);

    private static final short ENDPOINT = 0x0047;

    private static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    private static final byte CMD_SET_TIME = 0x05;
    private static final byte CMD_SET_TIME_ACK = 0x06;
    private static final byte CMD_SET_DST = 0x07;
    private static final byte CMD_SET_DST_ACK = 0x08;

    public ZeppOsTimeService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                final int version = payload[1] & 0xff;
                LOG.info("Got time service version {}", version);
                return;
            case CMD_SET_TIME_ACK:
                LOG.debug("Got set time ack, status={}", payload[1]); // 1
                return;
            case CMD_SET_DST_ACK:
                LOG.debug("Got set DST ack, status={}", payload[1]); // 1
                return;
        }

        LOG.warn("Unexpected time payload byte {}", String.format("0x%02x", payload[0]));
    }

    public void setTime(final ZeppOsTransactionBuilder builder) {
        setCurrentTime(builder);
        setNextDst(builder);
    }

    public void setCurrentTime(final ZeppOsTransactionBuilder builder) {
        final ByteBuffer buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);

        final Calendar now = GregorianCalendar.getInstance();

        buf.put(CMD_SET_TIME);
        buf.put(HuamiUtils.getCurrentTimeBytes());

        write(builder, buf.array());
    }

    public void setNextDst(final ZeppOsTransactionBuilder builder) {
        final ZoneId zoneId = ZoneId.systemDefault();
        final ZoneRules rules = zoneId.getRules();
        final ZoneOffsetTransition nextTransition = rules.nextTransition(Instant.now());
        if (nextTransition == null) {
            return;
        }

        final long nextTransitionEpochSeconds = nextTransition.getInstant().getEpochSecond();

        final ByteBuffer buf = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_SET_DST);
        buf.putInt((int) nextTransitionEpochSeconds);
        buf.putShort((short) nextTransition.getDuration().getSeconds());

        write(builder, buf.array());
    }
}
