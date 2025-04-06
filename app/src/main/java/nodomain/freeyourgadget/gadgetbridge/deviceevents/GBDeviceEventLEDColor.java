/*  Copyright (C) 2018-2024 José Rebelo

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

import androidx.annotation.NonNull;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GBDeviceEventLEDColor extends GBDeviceEvent {
    public final int color;

    public GBDeviceEventLEDColor(final int color) {
        this.color = color;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + "color: " + Integer.toHexString(this.color).toUpperCase(Locale.ROOT);
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        device.setExtraInfo("led_color", this.color);
        device.sendDeviceUpdateIntent(context);
    }
}
