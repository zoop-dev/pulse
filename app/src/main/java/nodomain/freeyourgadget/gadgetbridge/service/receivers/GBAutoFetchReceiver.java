/*  Copyright (C) 2018-2025 Daniele Gobbetti, José Rebelo, Martin

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;


public class GBAutoFetchReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBAutoFetchReceiver.class);

    private static final String PREF_AUTO_FETCH_LAST_TIME = "auto_fetch_last_time";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        LOG.info("Trigger auto fetch by {}", intent.getAction());

        final GBPrefs prefs = GBApplication.getPrefs();
        if (!prefs.getBoolean(GBPrefs.PREF_AUTO_FETCH_ENABLED, false)) {
            return;
        }

        synchronized (this) {
            final long now = new Date().getTime();
            final int fetchIntervalMinutes = prefs.getInt(GBPrefs.PREF_AUTO_FETCH_INTERVAL_LIMIT, 0);
            final long fetchIntervalMillis = fetchIntervalMinutes * 60 * 1000L;

            final List<GBDevice> devices = GBApplication.app().getDeviceManager().getSelectedDevices();

            for (GBDevice device : devices) {
                if (!device.isInitialized()) {
                    LOG.trace("Not auto-fetching from {}, not initialized", device);
                    continue;
                }

                final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);
                final long lastSync = devicePrefs.getLong(PREF_AUTO_FETCH_LAST_TIME, 0);

                final long timeSinceLast = now - lastSync;
                if (timeSinceLast < fetchIntervalMillis) {
                    // #4165 - prevent multiple syncs in very quick succession
                    LOG.warn("Not auto-fetching from {}, last fetch was {}ms ago", device, timeSinceLast);
                    continue;
                }

                LOG.debug("Auto-fetching from {}", device);
                GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);

                devicePrefs.getPreferences().edit()
                        .putLong(PREF_AUTO_FETCH_LAST_TIME, now)
                        .apply();
            }
        }
    }
}
