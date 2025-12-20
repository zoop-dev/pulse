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

import static nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiNotificationsManager.getCallSpecKey;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiNotificationsManager.getNotificationKey;

import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendNotificationRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendNotificationRequest.class);

    private HuaweiPacket packet;

    public SendNotificationRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Notifications.id;
        this.commandId = Notifications.NotificationActionRequest.id;
    }

    public static byte getNotificationType(NotificationType type) {
        return switch (type.getGenericType()) {
            case "generic", "generic_social", "generic_chat" ->
                    Notifications.NotificationType.generic;
            case "generic_email" -> Notifications.NotificationType.email;
            default -> Notifications.NotificationType.sms;
        };
    }
    
    public void buildNotificationTLVFromNotificationSpec(NotificationSpec notificationSpec) {
        String title;
        if (notificationSpec.title != null)
            title = notificationSpec.title;
        else
            title = notificationSpec.sourceName;

        String body = notificationSpec.body;
        if (body != null && body.length() > supportProvider.getDeviceState().getContentLength()) {
            body = notificationSpec.body.substring(0x0, supportProvider.getDeviceState().getContentLength() - 0xD);
            body += "...";
        }

        String replyKey = "";
        final boolean hasActions = (null != notificationSpec.attachedActions && !notificationSpec.attachedActions.isEmpty());
        if (hasActions) {
            for (int i = 0; i < notificationSpec.attachedActions.size(); i++) {
                final NotificationSpec.Action action = notificationSpec.attachedActions.get(i);
                if (action.isReply()) {
                    //NOTE: store notification key instead action key. The watch returns this key so it is more easier to find action by notification key
                    replyKey = getNotificationKey(notificationSpec);
                    break;
                }
            }
        }

        Notifications.NotificationActionRequest.AdditionalParams params = new Notifications.NotificationActionRequest.AdditionalParams();

        params.supportsReply = supportProvider.getDeviceState().supportsNotificationsReply();
        params.supportsRepeatedNotify = supportProvider.getDeviceState().supportsNotificationsRepeatedNotify();
        params.supportsRemoveSingle = supportProvider.getDeviceState().supportsNotificationsRemoveSingle();
        params.supportsReplyActions = supportProvider.getDeviceState().supportsNotificationsReplyActions();
        params.supportsTimestamp = supportProvider.getDeviceState().supportsNotificationsAddIconTimestamp();

        params.notificationId = notificationSpec.getId();
        params.notificationKey = getNotificationKey(notificationSpec);
        params.replyKey = replyKey;
        params.channelId = notificationSpec.channelId;
        params.category = notificationSpec.category;
        params.address = notificationSpec.phoneNumber;
        params.when = notificationSpec.when;

        boolean pictureEnabled = GBApplication
                .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
                .getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_PICTURES_ENABLE, true);
        if(supportProvider.getDeviceState().supportsNotificationPicture() && !TextUtils.isEmpty(notificationSpec.picturePath) && pictureEnabled) {
            params.pictureName = supportProvider.getHuaweiDataSyncNotificationPictures().getNameForPath(notificationSpec.picturePath);
        }

        ArrayList<Notifications.NotificationActionRequest.TextElement> content = new ArrayList<>();
        content.add(
                new Notifications.NotificationActionRequest.TextElement(
                        (byte)Notifications.TextType.title,
                        (byte)supportProvider.getDeviceState().getContentFormat(),
                        title)
        );
        content.add(
                new Notifications.NotificationActionRequest.TextElement(
                        (byte) Notifications.TextType.sender,
                        (byte)supportProvider.getDeviceState().getContentFormat(),
                        notificationSpec.sender)
        );
        content.add(
                new Notifications.NotificationActionRequest.TextElement(
                        (byte) Notifications.TextType.text,
                        (byte)supportProvider.getDeviceState().getContentFormat(),
                        body)
        );

        this.packet = new Notifications.NotificationActionRequest(
                paramsProvider,
                supportProvider.getNotificationId(),
                getNotificationType(notificationSpec.type),
                content,
                notificationSpec.sourceAppId,
                params
        );
    }

    public void buildNotificationTLVFromCallSpec(CallSpec callSpec) {
        byte notificationType = callSpec.command == CallSpec.CALL_OUTGOING?Notifications.NotificationType.outgoingCall:Notifications.NotificationType.call;

        Notifications.NotificationActionRequest.AdditionalParams params = null;
        String sourceAppId = null;

        ArrayList<Notifications.NotificationActionRequest.TextElement> content = new ArrayList<>();
        content.add(
                new Notifications.NotificationActionRequest.TextElement(
                        (byte) Notifications.TextType.text,
                        (byte)supportProvider.getDeviceState().getContentFormat(),
                        callSpec.name)
        );

        if(callSpec.isVoip && callSpec.command == CallSpec.CALL_INCOMING) {
            sourceAppId = callSpec.sourceAppId;
            params = new Notifications.NotificationActionRequest.AdditionalParams();

            params.supportsReply = supportProvider.getDeviceState().supportsNotificationsReply();
            params.supportsRepeatedNotify = supportProvider.getDeviceState().supportsNotificationsRepeatedNotify();
            params.supportsRemoveSingle = supportProvider.getDeviceState().supportsNotificationsRemoveSingle();
            params.supportsReplyActions = supportProvider.getDeviceState().supportsNotificationsReplyActions();
            params.supportsTimestamp = supportProvider.getDeviceState().supportsNotificationsAddIconTimestamp();

            params.notificationId = new Random().nextInt(Integer.MAX_VALUE - 1);
            params.notificationKey = getCallSpecKey(callSpec, params.notificationId);
            params.channelId = callSpec.channelId;
            if(supportProvider.getDeviceState().supportsVoipType3()) {
                params.category = "imcall";
            } else {
                params.category = callSpec.category;
            }
            params.address = null;
            if(supportProvider.getDeviceState().supportsVoipType2()) {
                params.voipType = 1;
            }
            if (supportProvider.getDeviceState().supportsVoipType1() || supportProvider.getDeviceState().supportsVoipType2()) {
                notificationType = Notifications.NotificationType.generic;

                content.add(
                        new Notifications.NotificationActionRequest.TextElement(
                                (byte) Notifications.TextType.title,
                                (byte) supportProvider.getDeviceState().getContentFormat(),
                                callSpec.name)
                );
                content.add(
                        new Notifications.NotificationActionRequest.TextElement(
                                (byte) Notifications.TextType.sender,
                                (byte) supportProvider.getDeviceState().getContentFormat(),
                                callSpec.name)
                );
// TODO: Reject action, need to be parsed from the notification and added here. Then the watch send it back in the service id: 0x2  Command id: 0x11
//                content.add(
//                        new Notifications.NotificationActionRequest.TextElement(
//                                (byte) 8,
//                                (byte) supportProvider.getDeviceState().getContentFormat(),
//                                "REJECT_CALL")
//                );
            }
        }

        if(supportProvider.getDeviceState().supportsIncomingNumber()) {
            content.add(
                    new Notifications.NotificationActionRequest.TextElement(
                            (byte) Notifications.TextType.flight,
                            (byte) supportProvider.getDeviceState().getIncomingNumberFormat(),
                            callSpec.number)
            );
        }

        this.packet = new Notifications.NotificationActionRequest(
                paramsProvider,
                supportProvider.getNotificationId(),
                notificationType,
                content,
                sourceAppId,
                params
        );
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return this.packet.serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Notification");
    }
}
