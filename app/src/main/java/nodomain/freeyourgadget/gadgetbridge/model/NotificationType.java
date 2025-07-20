package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.Arrays;
import java.util.Comparator;

public enum NotificationType {
    UNKNOWN,

    AMAZON,
    BBM,
    CONVERSATIONS,
    FACEBOOK,
    FACEBOOK_MESSENGER,
    GENERIC_ALARM_CLOCK,
    GENERIC_CALENDAR,
    GENERIC_EMAIL,
    GENERIC_NAVIGATION,
    GENERIC_PHONE,
    GENERIC_SMS,
    GMAIL,
    GOOGLE_HANGOUTS,
    GOOGLE_INBOX,
    GOOGLE_MAPS,
    GOOGLE_MESSENGER,
    GOOGLE_PHOTOS,
    HIPCHAT,
    INSTAGRAM,
    KAKAO_TALK,
    KIK,
    LIGHTHOUSE, // ??? - No idea what this is, but it works.
    LINE,
    LINKEDIN,
    MAILBOX,
    OUTLOOK,
    BUSINESS_CALENDAR,
    RIOT,
    SIGNAL,
    WIRE,
    SKYPE,
    SLACK,
    SNAPCHAT,
    TELEGRAM,
    THREEMA,
    KONTALK,
    ANTOX,
    DISCORD,
    TRANSIT,
    TWITTER,
    VIBER,
    WECHAT,
    WHATSAPP,
    YAHOO_MAIL,
    COL_REMINDER,
    DELTACHAT,
    ELEMENT,
    ELEMENTX,
    MOLLY,
    VK,
    QQ,
    TUMBLR,
    PINTEREST,
    YOUTUBE,
    GADGETBRIDGE_TEXT_RECEIVER,
    ;


    /**
     * Returns the enum constant as a fixed String value, e.g. to be used
     * as preference key. In case the keys are ever changed, this method
     * may be used to bring backward compatibility.
     */
    public String getFixedValue() {
        return name().toLowerCase();
    }

    public String getGenericType() {
        switch (this) {
            case GENERIC_EMAIL:
            case GENERIC_NAVIGATION:
            case GENERIC_SMS:
            case GENERIC_PHONE:
            case GENERIC_CALENDAR:
            case GENERIC_ALARM_CLOCK:
                return getFixedValue();
            case FACEBOOK:
            case TWITTER:
            case SNAPCHAT:
            case INSTAGRAM:
            case LINKEDIN:
                return "generic_social";
            case CONVERSATIONS:
            case FACEBOOK_MESSENGER:
            case RIOT:
            case SIGNAL:
            case WIRE:
            case TELEGRAM:
            case THREEMA:
            case KONTALK:
            case ANTOX:
            case WHATSAPP:
            case GOOGLE_MESSENGER:
            case GOOGLE_HANGOUTS:
            case HIPCHAT:
            case SKYPE:
            case WECHAT:
            case KIK:
            case KAKAO_TALK:
            case SLACK:
            case LINE:
            case VIBER:
            case DISCORD:
            case DELTACHAT:
            case ELEMENT:
            case ELEMENTX:
            case MOLLY:
                return "generic_chat";
            case GMAIL:
            case GOOGLE_INBOX:
            case MAILBOX:
            case OUTLOOK:
            case YAHOO_MAIL:
                return "generic_email";
            case COL_REMINDER:
            case GADGETBRIDGE_TEXT_RECEIVER:
            case UNKNOWN:
            default:
                return "generic";
        }
    }

    public static NotificationType[] sortedValues() {
        final NotificationType[] sorted = NotificationType.values();
        Arrays.sort(sorted, new Comparator<NotificationType>() {
            @Override public int compare(final NotificationType n1, final NotificationType n2) {
                // Keep unknown first
                if (n1.equals(NotificationType.UNKNOWN)) {
                    return -1;
                } else if (n2.equals(NotificationType.UNKNOWN)) {
                    return 1;
                }

                return n1.name().compareToIgnoreCase(n2.name());
            }
        });

        return sorted;
    }
}
