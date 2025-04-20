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

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;

public class ZeppOsBatteryService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsBatteryService.class);

    private static final short ENDPOINT = 0x0029;

    public static final byte CMD_BATTERY_REQUEST = 0x03;
    public static final byte CMD_BATTERY_REPLY = 0x04;

    public ZeppOsBatteryService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        if (payload[0] != CMD_BATTERY_REPLY) {
            LOG.warn("Unexpected battery payload byte {}", String.format("0x%02x", payload[0]));
            return;
        }

        if (payload.length != 21) {
            LOG.warn("Unexpected battery payload length: {}", payload.length);
        }

        final HuamiBatteryInfo batteryInfo = new HuamiBatteryInfo(subarray(payload, 1, payload.length));
        getSupport().evaluateGBDeviceEvent(batteryInfo.toDeviceEvent());
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        requestBatteryInfo(builder);
    }

    public void requestBatteryInfo(final ZeppOsTransactionBuilder builder) {
        LOG.debug("Requesting Battery Info");

        write(builder, CMD_BATTERY_REQUEST);
    }
}
