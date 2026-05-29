package nodomain.freeyourgadget.gadgetbridge.service.devices.keephealth;

public class NotificationItem {
    String title;
    String body;
    KeepHealthNotificationType type;

    public NotificationItem(String title, String body, KeepHealthNotificationType type) {
        this.title = title;
        this.body = body;
        this.type = type;
    }

}
