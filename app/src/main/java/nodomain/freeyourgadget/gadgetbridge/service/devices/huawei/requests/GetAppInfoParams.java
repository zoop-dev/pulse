/*  Copyright (C) 2024 Me7c7, Martin.JM

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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.App;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetAppInfoParams extends Request{

    public GetAppInfoParams(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = App.id;
        this.commandId = App.AppInfoParams.id;

    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsAppParams();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new App.AppInfoParams.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof App.AppInfoParams.Response))
            throw new ResponseTypeMismatchException(receivedPacket, App.AppInfoParams.Response.class);

        App.AppInfoParams.Response resp = (App.AppInfoParams.Response)(receivedPacket);
        supportProvider.getHuaweiCoordinator().setAppDeviceParams(resp.params);
    }
}
