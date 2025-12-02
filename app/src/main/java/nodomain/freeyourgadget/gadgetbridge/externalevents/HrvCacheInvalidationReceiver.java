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

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.ComputedHrvSummarySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * Receiver that listens for new data events and invalidates the HRV summary cache
 * for the current day. This ensures that computed HRV summaries are recalculated
 * with the latest data when new HRV samples arrive during the day.
 */
public class HrvCacheInvalidationReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(HrvCacheInvalidationReceiver.class);
    private Context context;
    private boolean registered = false;
    private HrvCacheInvalidationBroadcastReceiver hrvCacheInvalidationBroadcastReceiver;

    public HrvCacheInvalidationReceiver() {
    }

    public void registerReceiver(Context context) {
        this.context = context;
        this.hrvCacheInvalidationBroadcastReceiver = new HrvCacheInvalidationBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(ACTION_NEW_DATA);
        ContextCompat.registerReceiver(this.context, this.hrvCacheInvalidationBroadcastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        this.registered = true;
        LOG.debug("HRV cache invalidation receiver registered");
    }

    public void unregisterReceiver() {
        if (this.registered) {
            try {
                this.context.unregisterReceiver(this.hrvCacheInvalidationBroadcastReceiver);
                this.registered = false;
                LOG.debug("HRV cache invalidation receiver unregistered");
            } catch (Exception e) {
                LOG.error("Error unregistering HRV cache invalidation receiver", e);
            }
        }
    }

    private static class HrvCacheInvalidationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_NEW_DATA.equals(intent.getAction())) {
                final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (device != null) {
                    // Invalidate HRV summary cache for current day when new data arrives
                    ComputedHrvSummarySampleProvider.invalidateCurrentDay(device.getAddress());
                }
            }
        }
    }
}

