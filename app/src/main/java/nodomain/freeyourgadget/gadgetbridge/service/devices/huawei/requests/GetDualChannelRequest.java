/*  Copyright (C) 2026 Vitaliy Tomin

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

/**
 * Service 0x01 / command 0x3C — negotiates the second ("dual") RFCOMM channel and the routing
 * tables that decide which packets go over it. Only sent when the device advertises dual-socket
 * support (expand-capability bit 56). The parsed routing tables are stored in
 * {@link HuaweiSupportProvider#getDualChannelHelper()}; opening the actual socket is handled by
 * the transport layer.
 */
public class GetDualChannelRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetDualChannelRequest.class);

    public GetDualChannelRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.DualChannel.id;
    }

    @Override
    protected boolean requestSupported() {
        // Also requires a BR (RFCOMM) transport; on BLE there is no second socket to open.
        return !supportProvider.isBLE() && supportProvider.getDeviceState().supportsDualSocket();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        boolean supportsExtend = supportProvider.getDeviceState().supportsExtendSocket();
        DeviceConfig.DualChannel.Request request =
                new DeviceConfig.DualChannel.Request(paramsProvider, supportsExtend);
        try {
            return request.serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle DualChannel");

        if (!(receivedPacket instanceof DeviceConfig.DualChannel.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.DualChannel.Response.class);

        DeviceConfig.DualChannel.Response response = (DeviceConfig.DualChannel.Response) receivedPacket;
        supportProvider.getDualChannelHelper().update(response);
        LOG.info("Dual channel negotiated on RFCOMM channel {}", response.channel);

        // Open the aux RFCOMM socket. It shares the same onSocketRead sink and does not affect the
        // primary connection state. Request.performConnected() then routes flagged packets to it
        // (via HuaweiDualChannelHelper), transparently falling back to the primary until it is up.
        supportProvider.openDualChannel(response.channel);
    }
}
