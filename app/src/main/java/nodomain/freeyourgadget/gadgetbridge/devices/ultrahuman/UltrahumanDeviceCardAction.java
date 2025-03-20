/*  Copyright (C) 2025  Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman;

import android.content.Context;
import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class UltrahumanDeviceCardAction implements DeviceCardAction {

    private final int Icon;
    private final int Description;
    private final int Question;

    private final String Action;

    public UltrahumanDeviceCardAction(int icon, int description, int question, String action) {
        Icon = icon;
        Description = description;
        Question = question;
        Action = action;
    }

    @Override
    public int getIcon(GBDevice device) {
        return Icon;
    }

    @Override
    public String getDescription(GBDevice device, Context context) {
        return context.getString(Description);
    }

    @Override
    public void onClick(GBDevice device, Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(Description)
                .setMessage(Question)
                .setIcon(Icon)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    final Intent intent = new Intent(Action);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    context.sendBroadcast(intent);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}