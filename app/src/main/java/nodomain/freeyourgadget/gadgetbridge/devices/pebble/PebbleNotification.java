package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.palette.graphics.Palette;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;

public class PebbleNotification {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleNotification.class);
    private final int icon;
    private final byte color;
    private final NotificationType notificationType;

    public PebbleNotification(NotificationSpec notificationSpec) {
        this.notificationType = notificationSpec.type;
        this.icon = setIcon(notificationSpec.type);
        this.color = setColor(notificationSpec);
    }

    public int getIcon() {
        return icon;
    }

    public byte getColor() {
        return color;
    }

    private byte setColor(NotificationType notificationType) {
        return switch (notificationType) {
            case AMAZON -> PebbleColor.ChromeYellow;
            case BBM -> PebbleColor.DarkGray;
            case CONVERSATIONS -> PebbleColor.Inchworm;
            case FACEBOOK, HIPCHAT, INSTAGRAM, LINKEDIN -> PebbleColor.CobaltBlue;
            case FACEBOOK_MESSENGER, GENERIC_CALENDAR, GOOGLE_INBOX, GOOGLE_MAPS,
                 GOOGLE_PHOTOS, OUTLOOK, BUSINESS_CALENDAR, SIGNAL, WIRE, TWITTER,
                 DELTACHAT -> PebbleColor.BlueMoon;
            case GENERIC_ALARM_CLOCK, GMAIL -> PebbleColor.Red;
            case GENERIC_EMAIL, GENERIC_NAVIGATION -> PebbleColor.Orange;
            case GENERIC_PHONE, GOOGLE_HANGOUTS, THREEMA, KONTALK, ANTOX, TRANSIT ->
                    PebbleColor.JaegerGreen;
            case GENERIC_SMS, VIBER -> PebbleColor.VividViolet;
            case GOOGLE_MESSENGER, MAILBOX, SKYPE, TELEGRAM -> PebbleColor.VividCerulean;
            case KAKAO_TALK -> PebbleColor.Yellow;
            case KIK, LINE, WHATSAPP, COL_REMINDER -> PebbleColor.IslamicGreen;
            case LIGHTHOUSE -> PebbleColor.PictonBlue;
            case RIOT, MOLLY -> PebbleColor.LavenderIndigo;
            case SLACK -> PebbleColor.Folly;
            case SNAPCHAT -> PebbleColor.Icterine;
            case DISCORD -> PebbleColor.Purpureus;
            case WECHAT -> PebbleColor.KellyGreen;
            case YAHOO_MAIL -> PebbleColor.Indigo;
            case ELEMENT, ELEMENTX -> PebbleColor.Malachite;
            default -> PebbleColor.DarkCandyAppleRed;
        };
    }

    private int setIcon(NotificationType notificationType) {
        return switch (notificationType) {
            case AMAZON -> PebbleIconID.NOTIFICATION_AMAZON;
            case BBM -> PebbleIconID.NOTIFICATION_BLACKBERRY_MESSENGER;
            case CONVERSATIONS, HIPCHAT, RIOT, SIGNAL, WIRE, THREEMA, KONTALK,
                 ANTOX, DISCORD, DELTACHAT, ELEMENT, ELEMENTX, MOLLY ->
                    PebbleIconID.NOTIFICATION_HIPCHAT;
            case FACEBOOK -> PebbleIconID.NOTIFICATION_FACEBOOK;
            case FACEBOOK_MESSENGER -> PebbleIconID.NOTIFICATION_FACEBOOK_MESSENGER;
            case GENERIC_ALARM_CLOCK -> PebbleIconID.ALARM_CLOCK;
            case GENERIC_CALENDAR, BUSINESS_CALENDAR -> PebbleIconID.TIMELINE_CALENDAR;
            case GENERIC_EMAIL -> PebbleIconID.GENERIC_EMAIL;
            case GENERIC_NAVIGATION, TRANSIT -> PebbleIconID.LOCATION;
            case GENERIC_PHONE -> PebbleIconID.DURING_PHONE_CALL;
            case GENERIC_SMS -> PebbleIconID.GENERIC_SMS;
            case GMAIL -> PebbleIconID.NOTIFICATION_GMAIL;
            case GOOGLE_HANGOUTS -> PebbleIconID.NOTIFICATION_GOOGLE_HANGOUTS;
            case GOOGLE_INBOX -> PebbleIconID.NOTIFICATION_GOOGLE_INBOX;
            case GOOGLE_MAPS -> PebbleIconID.NOTIFICATION_GOOGLE_MAPS;
            case GOOGLE_MESSENGER -> PebbleIconID.NOTIFICATION_GOOGLE_MESSENGER;
            case GOOGLE_PHOTOS -> PebbleIconID.NOTIFICATION_GOOGLE_PHOTOS;
            case INSTAGRAM -> PebbleIconID.NOTIFICATION_INSTAGRAM;
            case KAKAO_TALK -> PebbleIconID.NOTIFICATION_KAKAOTALK;
            case KIK -> PebbleIconID.NOTIFICATION_KIK;
            case LIGHTHOUSE -> PebbleIconID.NOTIFICATION_LIGHTHOUSE;
            case LINE -> PebbleIconID.NOTIFICATION_LINE;
            case LINKEDIN -> PebbleIconID.NOTIFICATION_LINKEDIN;
            case MAILBOX -> PebbleIconID.NOTIFICATION_MAILBOX;
            case OUTLOOK -> PebbleIconID.NOTIFICATION_OUTLOOK;
            case SKYPE -> PebbleIconID.NOTIFICATION_SKYPE;
            case SLACK -> PebbleIconID.NOTIFICATION_SLACK;
            case SNAPCHAT -> PebbleIconID.NOTIFICATION_SNAPCHAT;
            case TELEGRAM -> PebbleIconID.NOTIFICATION_TELEGRAM;
            case TWITTER -> PebbleIconID.NOTIFICATION_TWITTER;
            case VIBER -> PebbleIconID.NOTIFICATION_VIBER;
            case WECHAT -> PebbleIconID.NOTIFICATION_WECHAT;
            case WHATSAPP -> PebbleIconID.NOTIFICATION_WHATSAPP;
            case YAHOO_MAIL -> PebbleIconID.NOTIFICATION_YAHOO_MAIL;
            case COL_REMINDER -> PebbleIconID.NOTIFICATION_REMINDER;
            default -> PebbleIconID.NOTIFICATION_GENERIC;
        };
    }

    /**
     * @param notificationSpec The NotificationSpec to read from.
     * @return Returns a PebbleColor that best represents this notification.
     */
    private byte setColor(NotificationSpec notificationSpec) {
        String appId = notificationSpec.sourceAppId;
        NotificationType existingType = notificationSpec.type;

        // If the notification type is known, return the associated color.
        if (existingType != NotificationType.UNKNOWN) {
            return setColor(existingType);
        }

        // Otherwise, we go and attempt to find the color from the app icon.
        Drawable icon;
        try {
            icon = NotificationUtils.getAppIcon(GBApplication.getContext(), appId);
            Objects.requireNonNull(icon);
        } catch (Exception ex) {
            // If we can't get the icon, we go with the default defined above.
            LOG.warn("Could not get icon for AppID " + appId, ex);
            return PebbleColor.IslamicGreen;
        }

        Bitmap bitmapIcon = BitmapUtil.convertDrawableToBitmap(icon);
        int iconPrimaryColor = new Palette.Builder(bitmapIcon)
                .generate()
                .getVibrantColor(Color.parseColor("#aa0000"));

        return PebbleUtils.getPebbleColor(iconPrimaryColor);
    }

}
