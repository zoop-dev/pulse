/*  Copyright (C) 2024 Me7c7, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendNotificationRemoveRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendSMSReplyAck;

public class HuaweiNotificationsManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiNotificationsManager.class);

    private final HuaweiSupportProvider support;
    private final Queue<NotificationSpec> notificationSpecCache = new LinkedList<>();

    public HuaweiNotificationsManager(HuaweiSupportProvider support) {
        this.support = support;
    }

    private void addNotificationToCache(NotificationSpec notificationSpec) {
        // TODO: rewrite this
        if (notificationSpecCache.size() > 10)
            notificationSpecCache.poll();

        Iterator<NotificationSpec> iterator = notificationSpecCache.iterator();
        while (iterator.hasNext()) {
            NotificationSpec e = iterator.next();
            if (e.getId() == notificationSpec.getId()) {
                iterator.remove();
            }
        }
        notificationSpecCache.offer(notificationSpec);
    }

    public static String getNotificationKey(NotificationSpec notificationSpec) {
        if(!TextUtils.isEmpty(notificationSpec.key)) {
            return notificationSpec.key;
        }
        return "0|" + notificationSpec.sourceAppId + "|" + notificationSpec.getId() + "||0";
    }

    public void onNotification(NotificationSpec notificationSpec) {

        addNotificationToCache(notificationSpec);

        SendNotificationRequest sendNotificationReq = new SendNotificationRequest(this.support);
        try {
            sendNotificationReq.buildNotificationTLVFromNotificationSpec(notificationSpec);
            sendNotificationReq.doPerform();
        } catch (IOException e) {
            LOG.error("Sending notification failed", e);
        }
    }

    public void onDeleteNotification(int id) {
        if (!support.getHuaweiCoordinator().supportsNotificationsRepeatedNotify() && !support.getHuaweiCoordinator().supportsNotificationsRemoveSingle()) {
            LOG.info("Delete notification is not supported");
            return;
        }
        NotificationSpec notificationSpec = null;
        Iterator<NotificationSpec> iterator = notificationSpecCache.iterator();
        while (iterator.hasNext()) {
            notificationSpec = iterator.next();
            if (notificationSpec.getId() == id) {
                iterator.remove();
                break;
            }
        }
        if (notificationSpec == null) {
            LOG.info("Notification is not found");
            return;
        }

        try {
            SendNotificationRemoveRequest sendNotificationReq = new SendNotificationRemoveRequest(this.support,
                    SendNotificationRequest.getNotificationType(notificationSpec.type), // notificationType
                    notificationSpec.sourceAppId,
                    getNotificationKey(notificationSpec),
                    id,
                    notificationSpec.channelId,
                    notificationSpec.category);
            sendNotificationReq.doPerform();
        } catch (IOException e) {
            LOG.error("Sending notification remove failed", e);
        }
    }

    void onReplyResponse(Notifications.NotificationReply.ReplyResponse response) {
        LOG.info(" KEY: {}, Text: {}", response.key, response.text);
        if(!this.support.getHuaweiCoordinator().supportsNotificationsReplyActions()) {
            LOG.info("Reply is not supported");
            return;
        }
        if (TextUtils.isEmpty(response.key) || TextUtils.isEmpty(response.text)) {
            LOG.info("Reply is empty");
            return;
        }

        NotificationSpec notificationSpec = null;
        if(response.type == 1) { // generic SMS notification reply. Find by phone number
            for (NotificationSpec spec : notificationSpecCache) {
                if (spec.phoneNumber.equals(response.key)) {
                    notificationSpec = spec;
                    break;
                }
            }
        } else if(response.type == 2) {
            for (NotificationSpec spec : notificationSpecCache) {
                if (getNotificationKey(spec).equals(response.key)) {
                    notificationSpec = spec;
                    break;
                }
            }
        } else {
            LOG.info("Reply type {} is not supported", response.type);
            return;
        }
        if (notificationSpec == null) {
            LOG.info("Notification for reply is not found");
            return;
        }
        final GBDeviceEventNotificationControl deviceEvtNotificationControl = new GBDeviceEventNotificationControl();
        deviceEvtNotificationControl.handle = notificationSpec.getId();
        deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
        deviceEvtNotificationControl.reply = response.text;
        if (notificationSpec.type.equals(NotificationType.GENERIC_PHONE) || notificationSpec.type.equals(NotificationType.GENERIC_SMS)) {
            deviceEvtNotificationControl.phoneNumber = notificationSpec.phoneNumber;
        } else {
            final boolean hasActions = (null != notificationSpec.attachedActions && !notificationSpec.attachedActions.isEmpty());
            if (hasActions) {
                for (int i = 0; i < notificationSpec.attachedActions.size(); i++) {
                    final NotificationSpec.Action action = notificationSpec.attachedActions.get(i);
                    if (action.isReply()) {
                        deviceEvtNotificationControl.handle = action.handle; //handle of wearable action is needed
                        break;
                    }
                }
            }
        }
        this.support.evaluateGBDeviceEvent(deviceEvtNotificationControl);
        if(response.type == 1) {
            // NOTE: send response only for SMS reply
            try {
                // 0xff - OK
                // 0x7f - error
                // TODO: get response from SMSManager. Send pending intent result.
                //    result can be one of the RESULT_ERROR_* from SmsManager. Not sure, need to check.
                //    currently always send OK.
                byte resultCode = (byte)0xff;
                SendSMSReplyAck sendNotificationReq = new SendSMSReplyAck(this.support, resultCode);
                sendNotificationReq.doPerform();
            } catch (IOException e) {
                LOG.error("Sending sns reply ACK", e);
            }
        }
    }

}
