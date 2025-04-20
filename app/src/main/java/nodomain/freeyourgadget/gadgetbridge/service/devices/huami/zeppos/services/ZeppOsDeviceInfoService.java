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

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsDeviceInfoService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsDeviceInfoService.class);

    private static final short ENDPOINT = 0x0043;

    private static final byte CMD_REQUEST = 0x01;
    private static final byte CMD_REPLY = 0x02;

    public ZeppOsDeviceInfoService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final byte cmd = buf.get();
        if (cmd != CMD_REPLY) {
            LOG.warn("Unexpected device info payload byte {}", String.format("0x%02x", cmd));
            return;
        }

        final byte one = buf.get();
        if (one != 1) {
            LOG.warn("Unexpected device info payload 2nd byte {}", String.format("0x%02x", one));
            return;
        }

        final int unk1 = buf.getInt(); // 0x000000ff
        final int unk2 = buf.getInt(); // 0x00000000

        if (unk1 != 0x000000ff || unk2 != 0x00000000) {
            LOG.error("Unexpected unk values {}/{}", String.format("0x%08x", unk1), String.format("0x%08x", unk2));
            return;
        }

        final int unk3count = buf.get() & 0xff;
        buf.get(new byte[unk3count]);

        final String serialNumber = StringUtils.untilNullTerminator(buf);
        final String hwVersion = StringUtils.untilNullTerminator(buf);
        final String fwVersion = StringUtils.untilNullTerminator(buf);

        final GBDeviceEventVersionInfo versionInfo = new GBDeviceEventVersionInfo();
        versionInfo.fwVersion = fwVersion;
        versionInfo.hwVersion = hwVersion;
        evaluateGBDeviceEvent(versionInfo);
    }

    public void requestDeviceInfo(final ZeppOsTransactionBuilder builder) {
        write(builder, CMD_REQUEST);
    }
}
