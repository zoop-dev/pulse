package nodomain.freeyourgadget.gadgetbridge.service.devices.keephealth;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

public enum KeepHealthNotificationType {
    CALL((byte) 0x00),
    SMS((byte) 0x01),
    WECHAT((byte) 0x02),
    QQ((byte) 0x03),
    FACEBOOK((byte) 0x04),
    SKYPE((byte) 0x05),
    TWITTER((byte) 0x06),
    WHATSAPP((byte) 0x07),
    LINE((byte) 0x08),
    EMAIL((byte) 0x09),
    INSTAGRAM((byte) 0x0A),
    LINKEDIN((byte) 0x0B),
    FACEBOOK_MESSENGER((byte) 0x0C),
    VK((byte) 0x0D),
    VIBER((byte) 0x0E),
    TELEGRAM((byte) 0x10),
    KAKAO_TALK((byte) 0x12),
    ;

    private final byte code;

    KeepHealthNotificationType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static KeepHealthNotificationType fromCode(byte code) {
        for (KeepHealthNotificationType t : values()) {
            if (t.code == code) return t;
        }
        return SMS;
    }

    public static KeepHealthNotificationType fromNotificationType(NotificationType type) {
        if (type == null) return KeepHealthNotificationType.SMS;

        switch (type) {
            case GENERIC_SMS:
                return KeepHealthNotificationType.SMS;
            case CONVERSATIONS:
            case HIPCHAT:
            case KONTALK:
            case ANTOX:
            case WECHAT:
            case SIGNAL:
            case GOOGLE_MESSENGER:
            case GOOGLE_HANGOUTS:
                return KeepHealthNotificationType.WECHAT;
            case GENERIC_EMAIL:
            case GMAIL:
            case YAHOO_MAIL:
            case OUTLOOK:
                return KeepHealthNotificationType.EMAIL;
            case FACEBOOK:
                return KeepHealthNotificationType.FACEBOOK;
            case FACEBOOK_MESSENGER:
                return KeepHealthNotificationType.FACEBOOK_MESSENGER;
            case INSTAGRAM:
            case GOOGLE_PHOTOS:
                return KeepHealthNotificationType.INSTAGRAM;
            case KAKAO_TALK:
                return KeepHealthNotificationType.KAKAO_TALK;
            case LINE:
                return KeepHealthNotificationType.LINE;
            case TWITTER:
                return KeepHealthNotificationType.TWITTER;
            case SKYPE:
                return KeepHealthNotificationType.SKYPE;
            case TELEGRAM:
                return KeepHealthNotificationType.TELEGRAM;
            case VIBER:
            case DISCORD:
                return KeepHealthNotificationType.VIBER;
            case WHATSAPP:
                return KeepHealthNotificationType.WHATSAPP;
            case VK:
                return KeepHealthNotificationType.VK;
            case QQ:
                return KeepHealthNotificationType.QQ;

            default:
                String g = type.getGenericType();
                if ("generic_email".equals(g)) return KeepHealthNotificationType.EMAIL;
                if ("generic_chat".equals(g)) return KeepHealthNotificationType.WECHAT;
                return KeepHealthNotificationType.SMS;
        }
    }
}
