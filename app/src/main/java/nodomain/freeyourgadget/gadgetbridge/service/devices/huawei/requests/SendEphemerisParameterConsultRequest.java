/*  Copyright (C) 2024 Me7c7

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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Ephemeris;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendEphemerisParameterConsultRequest extends Request  {
    private Logger LOG = LoggerFactory.getLogger(SendEphemerisParameterConsultRequest.class);

    public SendEphemerisParameterConsultRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Ephemeris.id;
        this.commandId = Ephemeris.ParameterConsult.id;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new Ephemeris.ParameterConsult.Request(this.paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Ephemeris.ParameterConsult.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Ephemeris.ParameterConsult.Response.class);

        Ephemeris.ParameterConsult.Response resp = (Ephemeris.ParameterConsult.Response)receivedPacket;
        supportProvider.getHuaweiEphemerisManager().handleParameterConsultResponse(resp.consultDeviceTime, resp.downloadVersion, resp.downloadTag);
    }
}
