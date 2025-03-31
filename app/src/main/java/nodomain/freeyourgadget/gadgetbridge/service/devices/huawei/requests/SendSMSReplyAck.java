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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendSMSReplyAck extends Request {
    private final byte resultCode;

    public SendSMSReplyAck(HuaweiSupportProvider support,
                               byte resultCode) {
        super(support);
        this.serviceId = Notifications.id;
        this.commandId = Notifications.NotificationReply.id;
        this.resultCode = resultCode;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Notifications.NotificationReply.ReplyAck(this.paramsProvider, this.resultCode).serialize();
        } catch(HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e.getMessage());
        }
    }
}
