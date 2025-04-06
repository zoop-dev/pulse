/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer

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

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AbstractAppManagerFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

public class GBDeviceEventAppInfo extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventAppInfo.class);

    public GBDeviceApp[] apps;
    public byte freeSlot = -1;

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        LOG.info("Got event for APP_INFO");

        Intent appInfoIntent = new Intent(AbstractAppManagerFragment.ACTION_REFRESH_APPLIST);
        int appCount = apps.length;
        appInfoIntent.putExtra("app_count", appCount);
        for (int i = 0; i < appCount; i++) {
            appInfoIntent.putExtra("app_name" + i, apps[i].getName());
            appInfoIntent.putExtra("app_creator" + i, apps[i].getCreator());
            appInfoIntent.putExtra("app_version" + i, apps[i].getVersion());
            appInfoIntent.putExtra("app_uuid" + i, apps[i].getUUID().toString());
            appInfoIntent.putExtra("app_type" + i, apps[i].getType().ordinal());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(appInfoIntent);
    }
}
