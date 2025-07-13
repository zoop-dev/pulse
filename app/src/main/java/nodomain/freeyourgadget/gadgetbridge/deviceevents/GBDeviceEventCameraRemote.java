/*  Copyright (C) 2024 Martin.JM

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

import nodomain.freeyourgadget.gadgetbridge.activities.CameraActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GBDeviceEventCameraRemote extends GBDeviceEvent {
    public Event event;

    public GBDeviceEventCameraRemote() {
        this(Event.UNKNOWN);
    }

    public GBDeviceEventCameraRemote(Event event) {
        this.event = event;
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        final Intent cameraIntent = new Intent(context, CameraActivity.class);
        cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        cameraIntent.putExtra(CameraActivity.intentExtraEvent, GBDeviceEventCameraRemote.eventToInt(event));
        context.startActivity(cameraIntent);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + "event: " + event;
    }

    public enum Event {
        UNKNOWN,
        OPEN_CAMERA,
        TAKE_PICTURE,
        CLOSE_CAMERA,
        EXCEPTION
    }

    static public int eventToInt(Event event) {
        return switch (event) {
            case UNKNOWN -> 0;
            case OPEN_CAMERA -> 1;
            case TAKE_PICTURE -> 2;
            case CLOSE_CAMERA -> 3;
            default -> -1;
        };
    }

    static public Event intToEvent(int event) {
        return switch (event) {
            case 0 -> Event.UNKNOWN;
            case 1 -> Event.OPEN_CAMERA;
            case 2 -> Event.TAKE_PICTURE;
            case 3 -> Event.CLOSE_CAMERA;
            default -> Event.EXCEPTION;
        };
    }
}
