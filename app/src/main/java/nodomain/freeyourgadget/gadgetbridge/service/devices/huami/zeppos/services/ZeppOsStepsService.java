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

import static org.apache.commons.lang3.ArrayUtils.subarray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.RealtimeSamplesAggregator;

public class ZeppOsStepsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsStepsService.class);

    private static final short ENDPOINT = 0x0016;

    public static final byte CMD_GET = 0x03;
    public static final byte CMD_REPLY = 0x04;
    public static final byte CMD_ENABLE_REALTIME = 0x05;
    public static final byte CMD_ENABLE_REALTIME_ACK = 0x06;
    public static final byte CMD_REALTIME_NOTIFICATION = 0x07;

    private RealtimeSamplesAggregator realtimeSamplesAggregator;

    public ZeppOsStepsService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        // It can stay enable across connections
        setRealtimeSteps(builder, false);
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_REPLY:
                LOG.info("Got steps reply, status = {}", payload[1]);
                if (payload.length != 15) {
                    LOG.error("Unexpected steps reply payload length {}", payload.length);
                    return;
                }
                handleRealtimeSteps(subarray(payload, 2, 15));
                return;
            case CMD_ENABLE_REALTIME_ACK:
                LOG.info("Band acknowledged realtime steps, status = {}, enabled = {}", payload[1], payload[2]);
                return;
            case CMD_REALTIME_NOTIFICATION:
                LOG.info("Got steps notification");
                if (payload.length != 14) {
                    LOG.error("Unexpected realtime notification payload length {}", payload.length);
                    return;
                }
                handleRealtimeSteps(subarray(payload, 1, 14));
                return;
        }

        LOG.warn("Unexpected steps payload byte {}", String.format("0x%02x", payload[0]));
    }

    public void setRealtimeSamplesAggregator(final RealtimeSamplesAggregator realtimeSamplesAggregator) {
        this.realtimeSamplesAggregator = realtimeSamplesAggregator;
    }

    public void setRealtimeSteps(final boolean enable) {
        withTransactionBuilder("toggle realtime steps", builder -> setRealtimeSteps(builder, enable));
    }

    public void setRealtimeSteps(final ZeppOsTransactionBuilder builder, final boolean enable) {
        write("toggle realtime steps", new byte[]{CMD_ENABLE_REALTIME, bool(enable)});
    }

    protected void handleRealtimeSteps(final byte[] value) {
        if (value.length != 13) {
            LOG.warn("Unrecognized realtime steps value: {}", Logging.formatBytes(value));
            return;
        }

        final int steps = BLETypeConversions.toUint16(value, 1);
        LOG.debug("realtime steps: {}", steps);

        if (realtimeSamplesAggregator != null) {
            realtimeSamplesAggregator.broadcastSteps(steps);
        }
    }
}
