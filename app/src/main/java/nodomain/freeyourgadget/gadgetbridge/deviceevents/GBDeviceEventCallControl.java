/*  Copyright (C) 2015-2024 Andreas Böhler, Andreas Shimokawa, José Rebelo

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

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.GBCallControlReceiver;

public class GBDeviceEventCallControl extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventCallControl.class);

    public Event event = Event.UNKNOWN;

    public GBDeviceEventCallControl() {
    }

    public GBDeviceEventCallControl(final Event event) {
        this.event = event;
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        LOG.info("Got event for CALL_CONTROL");
        if (event == GBDeviceEventCallControl.Event.IGNORE) {
            LOG.info("Sending intent for mute");
            final Intent broadcastIntent = new Intent(context.getPackageName() + ".MUTE_CALL");
            broadcastIntent.setPackage(context.getPackageName());
            context.sendBroadcast(broadcastIntent);
            return;
        }
        final Intent callIntent = new Intent(GBCallControlReceiver.ACTION_CALLCONTROL);
        callIntent.putExtra("event", event.ordinal());
        callIntent.setPackage(context.getPackageName());
        context.sendBroadcast(callIntent);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + "event: " + event;
    }

    public enum Event {
        UNKNOWN,
        ACCEPT,
        END,
        INCOMING,
        OUTGOING,
        REJECT,
        START,
        IGNORE,
    }
}
