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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendNotificationRemoveRequest extends Request {

    private static final Logger LOG = LoggerFactory.getLogger(SendNotificationRemoveRequest.class);

    private final byte notificationType;
    private final String sourceAppId;
    private final String notificationKey;
    private final int notificationId;
    private final String notificationChannelId;
    private final String notificationCategory;

    public SendNotificationRemoveRequest(HuaweiSupportProvider support, byte notificationType, String sourceAppId, String notificationKey, int notificationId, String notificationChannelId, String notificationCategory) {
        super(support);
        this.serviceId = Notifications.id;
        this.commandId = Notifications.NotificationRemoveAction.id;
        this.notificationType = notificationType;
        this.sourceAppId = sourceAppId;
        this.notificationKey = notificationKey;
        this.notificationId = notificationId;
        this.notificationChannelId = notificationChannelId;
        this.notificationCategory = notificationCategory;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Notifications.NotificationRemoveAction(
                    paramsProvider,
                    this.notificationType,
                    this.sourceAppId,
                    this.notificationKey,
                    this.notificationId,
                    this.notificationChannelId,
                    this.notificationCategory).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle NotificationRemove");
    }
}
