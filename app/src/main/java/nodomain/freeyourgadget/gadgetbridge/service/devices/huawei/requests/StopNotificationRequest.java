/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class StopNotificationRequest extends Request {
    final byte type;
    public StopNotificationRequest(HuaweiSupportProvider support, byte type) {
        super(support);
        this.serviceId = Notifications.id;
        this.commandId = Notifications.NotificationActionRequest.id;
        this.addToResponse = false;
        this.type = type;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            ArrayList<Notifications.NotificationActionRequest.TextElement> content = new ArrayList<>();
            content.add(
                    new Notifications.NotificationActionRequest.TextElement(
                            (byte) Notifications.TextType.text,
                            (byte)supportProvider.getDeviceState().getContentFormat(),
                            "")
            );
            return new Notifications.NotificationActionRequest(
                    paramsProvider,
                    supportProvider.getNotificationId(),
                    type,
                    content,
                    null,
                    null
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
