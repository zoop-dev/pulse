/*  Copyright (C) 2015-2024 Andreas Shimokawa, Petr Vaněk

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

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.GBMusicControlReceiver;

public class GBDeviceEventMusicControl extends GBDeviceEvent {
    public Event event;

    public GBDeviceEventMusicControl() {
        this(Event.UNKNOWN);
    }

    public GBDeviceEventMusicControl(Event event) {
        this.event = event;
    }

    @Override
    public void evaluate(final Context context, @NonNull final GBDevice device) {
        final Intent musicIntent = new Intent(GBMusicControlReceiver.ACTION_MUSICCONTROL);
        musicIntent.putExtra("event", event.ordinal());
        musicIntent.setPackage(context.getPackageName());
        context.sendBroadcast(musicIntent);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + "event: " + event;
    }

    public enum Event {
        UNKNOWN,
        PLAY,
        PAUSE,
        PLAYPAUSE,
        NEXT,
        PREVIOUS,
        VOLUMEUP,
        VOLUMEDOWN,
        FORWARD,
        REWIND
    }
}
