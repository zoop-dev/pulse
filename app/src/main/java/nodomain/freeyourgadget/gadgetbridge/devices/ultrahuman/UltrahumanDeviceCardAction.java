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

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class UltrahumanDeviceCardAction implements DeviceCardAction {

    private final int Icon;
    private final int Description;
    private final int Question;

    @Nullable
    private final String IntentAction;
    @Nullable
    private final Class<?> IntentClass;

    UltrahumanDeviceCardAction(int icon, int description, int question, String action) {
        Icon = icon;
        Description = description;
        Question = question;
        IntentAction = action;
        IntentClass = null;
    }

    UltrahumanDeviceCardAction(int icon, int description, Class<?> cls) {
        Icon = icon;
        Description = description;
        Question = 0;
        IntentAction = null;
        IntentClass = cls;
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
        if (Question == 0) {
            sendIntent(device, context);
        } else {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(Description)
                    .setMessage(Question)
                    .setIcon(Icon)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton)
                            -> sendIntent(device, context))
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    private void sendIntent(GBDevice device, Context context) {
        final Intent intent = new Intent(IntentAction);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        intent.putExtra(UltrahumanConstants.EXTRA_ADDRESS, device.getAddress());

        if (IntentClass != null) {
            intent.setClass(context, IntentClass);
            if (AbstractGBActivity.class.isAssignableFrom(IntentClass)) {
                context.startActivity(intent);
                return;
            }
        }

        context.sendBroadcast(intent);
    }

    @Override
    public boolean isVisible(final GBDevice device) {
        // device.isInitialized() also treats State.SCANNED as initialized but that isn't
        // appropriate for this device type
        final GBDevice.State state = device.getState();
        return state.equalsOrHigherThan(GBDevice.State.INITIALIZED);
    }
}