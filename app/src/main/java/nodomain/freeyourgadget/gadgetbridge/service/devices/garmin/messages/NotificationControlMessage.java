package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.NotificationControlStatusMessage;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler.NotificationCommand.GET_APP_ATTRIBUTES;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler.NotificationCommand.GET_NOTIFICATION_ATTRIBUTES;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler.NotificationCommand.PERFORM_LEGACY_NOTIFICATION_ACTION;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler.NotificationCommand.PERFORM_NOTIFICATION_ACTION;

public class NotificationControlMessage extends GFDIMessage {

    private final NotificationsHandler.NotificationCommand command;
    private final int notificationId;
    private Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap;
    private String appIdentifier;
    private List<NotificationsHandler.AppAttribute> appAttributes;
    private NotificationsHandler.LegacyNotificationAction legacyNotificationAction;
    private NotificationsHandler.NotificationAction notificationAction;
    private String actionString;
    private List<GBDeviceEvent> gbDeviceEventList;

    public NotificationControlMessage(GarminMessage garminMessage, NotificationsHandler.NotificationCommand command, int notificationId, NotificationsHandler.NotificationAction notificationAction, String actionString) {
        this.garminMessage = garminMessage;
        this.command = command;
        this.notificationId = notificationId;
        this.notificationAction = notificationAction;
        this.actionString = actionString;

        this.statusMessage = new NotificationControlStatusMessage(garminMessage, GFDIMessage.Status.ACK, NotificationControlStatusMessage.NotificationChunkStatus.OK, NotificationControlStatusMessage.NotificationStatusCode.NO_ERROR);

    }

    public NotificationControlMessage(GarminMessage garminMessage, NotificationsHandler.NotificationCommand command, int notificationId, NotificationsHandler.LegacyNotificationAction legacyNotificationAction) {
        this.garminMessage = garminMessage;
        this.command = command;
        this.notificationId = notificationId;
        this.legacyNotificationAction = legacyNotificationAction;

        this.statusMessage = new NotificationControlStatusMessage(garminMessage, GFDIMessage.Status.ACK, NotificationControlStatusMessage.NotificationChunkStatus.OK, NotificationControlStatusMessage.NotificationStatusCode.NO_ERROR);
    }

    public NotificationControlMessage(GarminMessage garminMessage, NotificationsHandler.NotificationCommand command, int notificationId, Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap) {
        this.garminMessage = garminMessage;
        this.command = command;
        this.notificationId = notificationId;
        this.notificationAttributesMap = notificationAttributesMap;

        this.statusMessage = new NotificationControlStatusMessage(garminMessage, GFDIMessage.Status.ACK, NotificationControlStatusMessage.NotificationChunkStatus.OK, NotificationControlStatusMessage.NotificationStatusCode.NO_ERROR);
    }

    public NotificationControlMessage(GarminMessage garminMessage,
                                      NotificationsHandler.NotificationCommand command,
                                      String appIdentifier,
                                      List<NotificationsHandler.AppAttribute> appAttributes) {
        this.garminMessage = garminMessage;
        this.command = command;
        this.notificationId = 0;
        this.appIdentifier = appIdentifier;
        this.appAttributes = appAttributes;

        this.statusMessage = new NotificationControlStatusMessage(garminMessage, GFDIMessage.Status.ACK, NotificationControlStatusMessage.NotificationChunkStatus.OK, NotificationControlStatusMessage.NotificationStatusCode.NO_ERROR);
    }

    //TODO: the fact that we return three versions of this object is really ugly
    public static NotificationControlMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {

        final NotificationsHandler.NotificationCommand command = NotificationsHandler.NotificationCommand.fromCode(reader.readByte());

        if (command == GET_NOTIFICATION_ATTRIBUTES) {
            final int notificationId = reader.readInt();
            final Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap = createGetNotificationAttributesCommand(reader);
            return new NotificationControlMessage(garminMessage, command, notificationId, notificationAttributesMap);
        } else if (command == PERFORM_LEGACY_NOTIFICATION_ACTION) {
            final int notificationId = reader.readInt();
            NotificationsHandler.LegacyNotificationAction[] values = NotificationsHandler.LegacyNotificationAction.values();
            final NotificationsHandler.LegacyNotificationAction legacyNotificationAction = values[reader.readByte()];
            return new NotificationControlMessage(garminMessage, command, notificationId, legacyNotificationAction);
        } else if (command == PERFORM_NOTIFICATION_ACTION) {
            final int notificationId = reader.readInt();
            final int actionId = reader.readByte();
            final NotificationsHandler.NotificationAction notificationAction = NotificationsHandler.NotificationAction.fromCode(actionId);
            // non-reply action might not have an action string at all in recent firmwares
            final String actionString = reader.remaining() > 0 ? reader.readNullTerminatedString() : null;
            return new NotificationControlMessage(garminMessage, command, notificationId, notificationAction, actionString);
        } else if (command == GET_APP_ATTRIBUTES) {
            final String appIdentifier = reader.readNullTerminatedString();
            final List<NotificationsHandler.AppAttribute> appAttributes = new ArrayList<>();
            while (reader.remaining() > 0) {
                final int attributeID = reader.readByte();
                final NotificationsHandler.AppAttribute attribute = NotificationsHandler.AppAttribute.getByCode(attributeID);
                if (attribute == null) {
                    LOG.error("Unknown app attribute requested {}", attributeID);
                    return null;
                }
                appAttributes.add(attribute);
            }
            return new NotificationControlMessage(garminMessage, command, appIdentifier, appAttributes);
        }
        LOG.warn("Unknown NotificationCommand in NotificationControlMessage");

        return null;
    }

    private static Map<NotificationsHandler.NotificationAttribute, Integer> createGetNotificationAttributesCommand(MessageReader reader) {
        final Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap = new LinkedHashMap<>();
        while (reader.remaining() > 0) {
            final int attributeID = reader.readByte();

            final NotificationsHandler.NotificationAttribute attribute = NotificationsHandler.NotificationAttribute.getByCode(attributeID);
            LOG.info("Requested attribute: {}", attribute);
            if (attribute == null) {
                LOG.error("Unknown notification attribute {}", attributeID);
                return null;
            }
            final int maxLength;
            if (attribute.hasLengthParam) {
                maxLength = reader.readShort();

            } else if (attribute.hasAdditionalParams) {
                maxLength = reader.readShort(); //TODO this is wrong
                // TODO: What is this??
                reader.readByte();

            } else {
                maxLength = 0;
            }
            notificationAttributesMap.put(attribute, maxLength);
        }
        return notificationAttributesMap;
    }

    public String getActionString() {
        return actionString;
    }

    public void addGbDeviceEvent(GBDeviceEvent gbDeviceEvent) {
        if (null == this.gbDeviceEventList)
            this.gbDeviceEventList = new ArrayList<>();
        this.gbDeviceEventList.add(gbDeviceEvent);
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        if (null == this.gbDeviceEventList)
            return Collections.emptyList();
        return gbDeviceEventList;
    }

    public NotificationsHandler.LegacyNotificationAction getLegacyNotificationAction() {
        return legacyNotificationAction;
    }

    public NotificationsHandler.NotificationAction getNotificationAction() {
        return notificationAction;
    }

    public NotificationsHandler.NotificationCommand getCommand() {
        return command;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public Map<NotificationsHandler.NotificationAttribute, Integer> getNotificationAttributesMap() {
        return notificationAttributesMap;
    }

    public String getAppIdentifier() {
        return appIdentifier;
    }

    public List<NotificationsHandler.AppAttribute> getAppAttributes() {
        return appAttributes;
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }

}
