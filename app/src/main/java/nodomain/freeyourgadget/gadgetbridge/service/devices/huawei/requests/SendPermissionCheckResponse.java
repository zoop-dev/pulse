/*  Copyright (C) 2024-2025 Me7c7

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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendPermissionCheckResponse extends Request {
    short permission;
    short status;


    public SendPermissionCheckResponse(HuaweiSupportProvider support, short permission, short status) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.PermissionCheck.id;
        this.permission = permission;
        this.status = status;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new DeviceConfig.PermissionCheck.PermissionCheckResponse(this.paramsProvider,  this.permission, this.status).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }
}
