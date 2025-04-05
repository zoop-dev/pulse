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

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;

public class ZeppOsConnectionService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsConnectionService.class);

    private static final short ENDPOINT = 0x0015;

    private static final byte CMD_MTU_REQUEST = 0x01;
    private static final byte CMD_MTU_RESPONSE = 0x02;
    private static final byte CMD_UNKNOWN_3 = 0x03;
    private static final byte CMD_UNKNOWN_4 = 0x04;

    public ZeppOsConnectionService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_MTU_RESPONSE:
                final int mtu = BLETypeConversions.toUint16(payload, 1) + 3;
                LOG.info("Device announced MTU change: {}", mtu);
                getSupport().setMtu(mtu);
                return;
            case CMD_UNKNOWN_3:
                // Some ping? Band sometimes sends 0x03, phone replies with 0x04
                LOG.info("Got unknown 3, replying with unknown 4");
                write("respond connection unknown 4", CMD_UNKNOWN_4);
                return;
        }

        LOG.warn("Unexpected connection payload byte {}", String.format("0x%02x", payload[0]));
    }

    public void requestMTU(final TransactionBuilder builder) {
        write(builder, CMD_MTU_REQUEST);
    }
}
