/*  Copyright (C) 2025 Me7c7

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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetOTAChangeLog extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetOTAChangeLog.class);

    public GetOTAChangeLog(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.GetChangeLog.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsOTAChangelog() &&
                supportProvider.getHuaweiCoordinator().getOtaSoftwareVersion() != null;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            // TODO: get proper language.
            return new OTA.GetChangeLog.Request(paramsProvider, supportProvider.getHuaweiCoordinator().getOtaSoftwareVersion(), "en").serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseTypeMismatchException {
        LOG.debug("handle GetOTAChangeLog");
        if (!(receivedPacket instanceof OTA.GetChangeLog.Response))
            throw new ResponseTypeMismatchException(receivedPacket, OTA.GetChangeLog.Response.class);

        OTA.GetChangeLog.Response resp = (OTA.GetChangeLog.Response) receivedPacket;

        SetOTAChangeLog setOTAChangeLog = new SetOTAChangeLog(supportProvider);
        setOTAChangeLog.nextRequest(this.nextRequest);
        nextRequest(setOTAChangeLog);
    }
}
