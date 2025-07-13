/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.gloryfit

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType

enum class GloryFitNotificationType(val code: Byte) {
    CALL(0),
    QQ(1),
    WECHAT(2),
    SMS(3),
    UNKNOWN_APP(4),
    FACEBOOK(5),
    TWITTER(6),
    WHATSAPP(7),
    SKYPE(8),
    FACEBOOK_MESSENGER(9),
    HANGOUTS(10),
    LINE(11),
    LINKEDIN(12),
    INSTAGRAM(13),
    VIBER(14),
    KAKAO_TALK(15),
    VK(16),
    SNAPCHAT(17),
    GOOGLE_PLUS(18),
    EMAIL(19),
    UNK_BLUE_RED_DOT(20),
    TUMBLR(21),
    PINTEREST(22),
    YOUTUBE(23),
    TELEGRAM(24),
    NO_ICON(25),
    ;

    companion object {
        fun fromNotificationType(type: NotificationType): GloryFitNotificationType {
            when (type) {
                NotificationType.CONVERSATIONS, NotificationType.RIOT, NotificationType.HIPCHAT, NotificationType.KONTALK,
                NotificationType.ANTOX, NotificationType.GENERIC_SMS, NotificationType.WECHAT,
                NotificationType.SIGNAL-> return WECHAT

                NotificationType.GENERIC_EMAIL, NotificationType.GMAIL, NotificationType.YAHOO_MAIL,
                NotificationType.OUTLOOK -> return EMAIL

                NotificationType.FACEBOOK -> return FACEBOOK
                NotificationType.FACEBOOK_MESSENGER -> return FACEBOOK_MESSENGER
                NotificationType.GOOGLE_HANGOUTS, NotificationType.GOOGLE_MESSENGER -> return HANGOUTS
                NotificationType.INSTAGRAM, NotificationType.GOOGLE_PHOTOS -> return INSTAGRAM
                NotificationType.KAKAO_TALK -> return KAKAO_TALK
                NotificationType.LINE -> return LINE
                NotificationType.TWITTER -> return TWITTER
                NotificationType.SKYPE -> return SKYPE
                NotificationType.SNAPCHAT -> return SNAPCHAT
                NotificationType.TELEGRAM -> return TELEGRAM
                NotificationType.VIBER, NotificationType.DISCORD -> return VIBER
                NotificationType.WHATSAPP -> return WHATSAPP
                NotificationType.VK -> return VK
                NotificationType.QQ -> return QQ
                NotificationType.TUMBLR -> return TUMBLR
                NotificationType.PINTEREST -> return PINTEREST
                NotificationType.YOUTUBE -> return YOUTUBE

                else -> {
                    when (type.genericType) {
                        "generic_email" -> return EMAIL
                        "generic_chat" -> return WECHAT
                    }
                    return UNKNOWN_APP
                }
            }
        }
    }
}
