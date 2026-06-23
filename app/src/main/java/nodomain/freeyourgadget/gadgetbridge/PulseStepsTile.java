/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;

/** Pulse: a Quick Settings tile showing today's step count; tap to open Pulse. */
@RequiresApi(Build.VERSION_CODES.N)
public class PulseStepsTile extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        update();
    }

    private void update() {
        final Tile tile = getQsTile();
        if (tile == null) return;

        int steps = 0;
        final GBDevice device = firstDevice();
        if (device != null) {
            try {
                steps = (int) DailyTotals.getDailyTotalsForDevice(device, Calendar.getInstance()).getSteps();
            } catch (final Exception ignored) {
            }
        }

        tile.setLabel(getString(R.string.pulse_qs_steps_label, NumberFormat.getIntegerInstance().format(steps)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(getString(R.string.steps));
        }
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_steps));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        final Intent intent = new Intent(this, ControlCenterv2.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            startActivityAndCollapse(intent);
        }
    }

    private GBDevice firstDevice() {
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (final GBDevice d : devices) {
            if (d.isInitialized()) {
                return d;
            }
        }
        return devices.isEmpty() ? null : devices.get(0);
    }
}
