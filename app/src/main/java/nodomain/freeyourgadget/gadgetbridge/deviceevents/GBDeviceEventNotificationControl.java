/*  Copyright (C) 2015-2024 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GBDeviceEventNotificationControl extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventNotificationControl.class);

    public long handle;
    public String phoneNumber;
    public String reply;
    public String title;
    public Event event = Event.UNKNOWN;

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        LOG.info("Got NOTIFICATION CONTROL device event");
        final String action;
        switch (event) {
            case DISMISS:
                action = NotificationListener.ACTION_DISMISS;
                break;
            case DISMISS_ALL:
                action = NotificationListener.ACTION_DISMISS_ALL;
                break;
            case OPEN:
                action = NotificationListener.ACTION_OPEN;
                break;
            case MUTE:
                action = NotificationListener.ACTION_MUTE;
                break;
            case REPLY:
                if (phoneNumber == null) {
                    phoneNumber = GBApplication.getIDSenderLookup().lookup((int) (handle >> 4));
                }
                if (phoneNumber != null) {
                    LOG.info("Got notification reply for SMS from {} : {}", phoneNumber, reply);
                    SmsManager.getDefault().sendTextMessage(phoneNumber, null, reply, null, null);
                    return;
                } else {
                    LOG.info("Got notification reply for notification id {} : {}", handle, reply);
                    action = NotificationListener.ACTION_REPLY;
                }
                break;
            case UNKNOWN:
            default:
                LOG.error("Unknown notification control action {}", event);
                return;
        }

        final Intent notificationListenerIntent = new Intent(action);
        notificationListenerIntent.putExtra("handle", handle);
        notificationListenerIntent.putExtra("title", title);
        if (reply != null) {
            final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            String suffix = prefs.getString("canned_reply_suffix", null);
            if (suffix != null && !Objects.equals(suffix, "")) {
                reply += suffix;
            }
            notificationListenerIntent.putExtra("reply", reply);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(notificationListenerIntent);
    }

    public enum Event {
        UNKNOWN,
        DISMISS,
        DISMISS_ALL,
        OPEN,
        MUTE,
        REPLY,
    }
}
