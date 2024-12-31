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
                    notificationSpec.key,
                    id,
                    "", // TODO:
                    null);
            sendNotificationReq.doPerform();
        } catch (IOException e) {
            LOG.error("Sending notification remove failed", e);
        }
    }

    void onReplyResponse(Notifications.NotificationReply.ReplyResponse response) {
        LOG.info(" KEY: {}, Text: {}", response.key, response.text);
        if(!this.support.getHuaweiCoordinator().supportsNotificationsReply()) {
            LOG.info("Reply is not supported");
            return;
        }
        if (TextUtils.isEmpty(response.key) || TextUtils.isEmpty(response.text)) {
            LOG.info("Reply is empty");
            return;
        }
        if(response.type != 1 && response.type != 2) {
            LOG.info("Reply: only type 1 and 2 supported");
            return;
        }
        NotificationSpec notificationSpec = null;
        for (NotificationSpec spec : notificationSpecCache) {
            notificationSpec = spec;
            if (notificationSpec.key.equals(response.key)) {
                break;
            }
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
                    if (action.type == NotificationSpec.Action.TYPE_WEARABLE_REPLY || action.type == NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR) {
                        deviceEvtNotificationControl.handle = action.handle; //handle of wearable action is needed
                        break;
                    }
                }
            }

        }
        this.support.evaluateGBDeviceEvent(deviceEvtNotificationControl);
        //TODO: maybe should be send reply. Service: 0x2, command: 0x10, tlv 7 and/or 1, type byte, 7f on error
    }

}
