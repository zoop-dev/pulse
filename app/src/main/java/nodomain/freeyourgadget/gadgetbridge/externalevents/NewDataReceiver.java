/*  Copyright (C) 2025 Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.ACTION_NEW_DATA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectSyncWorker;

public class NewDataReceiver extends BroadcastReceiver {
    private static final String HEALTH_CONNECT_SYNC_WORKER_TAG = "HealthConnectSyncWorker";
    // Debounce window: a fetch can emit several ACTION_NEW_DATA broadcasts in quick succession
    // (multi-phase or chatty drivers fire mid-fetch), and a sync that runs against a half-fetched
    // database can miss or duplicate records. Delay the sync and restart the timer on every new
    // broadcast (REPLACE), so a single sync runs once the fetch has settled.
    private static final long DEBOUNCE_SECONDS = 10;
    private final Logger LOG = LoggerFactory.getLogger(NewDataReceiver.class);
    private Context context;
    private boolean registered = false;

    public NewDataReceiver() {
    }

    public void registerReceiver(Context context) {
        this.context = context;
        IntentFilter intentFilter = new IntentFilter(ACTION_NEW_DATA);
        LocalBroadcastManager.getInstance(this.context).registerReceiver(this, intentFilter);
        this.registered = true;
        LOG.info("NewDataReceiver registered for ACTION_NEW_DATA");
    }

    public void unregisterReceiver() {
        if (this.registered) {
            try {
                LocalBroadcastManager.getInstance(this.context).unregisterReceiver(this);
                this.registered = false;
            } catch (Exception e) {
                LOG.error("Error unregister new data receiver", e);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        GBPrefs prefs = GBApplication.getPrefs();
        if (ACTION_NEW_DATA.equals(intent.getAction()) &&
                prefs.getBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false) &&
                prefs.getBoolean(GBPrefs.HEALTH_CONNECT_SYNC_ON_EVENT, false)) {

            // Extract device from the intent
            final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
            final String deviceAddress = device != null ? device.getAddress() : null;
            if (deviceAddress == null || deviceAddress.isBlank()) {
                // This shouldn't happen for ACTION_NEW_DATA, but handle gracefully
                LOG.warn("ACTION_NEW_DATA received without device information, skipping HC sync");
                return;
            }

            final Set<String> hcDevices = prefs.getStringSet(GBPrefs.HEALTH_CONNECT_DEVICE_SELECTION, Collections.emptySet());
            if (!hcDevices.contains(deviceAddress.toUpperCase(Locale.ROOT))) {
                LOG.debug("Ignoring new data for {} - not configured for HC sync", deviceAddress);
                return;
            }

            // Per-device unique work name so each device debounces independently.
            String workName = HEALTH_CONNECT_SYNC_WORKER_TAG + "_" + deviceAddress;

            OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(HealthConnectSyncWorker.class)
                .addTag(HEALTH_CONNECT_SYNC_WORKER_TAG)
                .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
                .setInputData(
                    new Data.Builder()
                        .putString(HealthConnectSyncWorker.INPUT_DEVICE_ADDRESS, deviceAddress)
                        .build()
                )
                .build();

            LOG.debug("Scheduling HC sync for device: {} with work name: {}", deviceAddress, workName);

            // REPLACE so a burst of broadcasts during one fetch cancels the pending (delayed) sync
            // and re-arms it; the sync runs once, DEBOUNCE_SECONDS after the last broadcast.
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            );
        }
    }
}
