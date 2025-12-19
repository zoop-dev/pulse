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

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.ComputedHrvSummarySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;

/**
 * Receiver that listens for new data events and invalidates the HRV summary cache
 * for affected days. This ensures that computed HRV summaries are recalculated
 * with the latest data when new HRV samples arrive.
 *
 * Performance optimization: Only registers if at least one device supports HRV,
 * and filters events by device address to avoid unnecessary processing.
 */
public class HrvCacheInvalidationReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(HrvCacheInvalidationReceiver.class);
    private Context context;
    private boolean registered = false;
    private HrvCacheInvalidationBroadcastReceiver hrvCacheInvalidationBroadcastReceiver;
    private final Set<String> hrvCapableDeviceAddresses = new HashSet<>();

    public HrvCacheInvalidationReceiver() {
    }

    /**
     * Registers the receiver only if at least one device supports HRV measurements.
     * Builds a set of HRV-capable device addresses for fast filtering.
     */
    public void registerReceiver(Context context) {
        this.context = context;

        // Build list of HRV-capable devices
        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice device : devices) {
            if (device.getDeviceCoordinator().supportsHrvMeasurement(device)) {
                hrvCapableDeviceAddresses.add(device.getAddress());
            }
        }

        // Only register if we have at least one HRV-capable device
        if (hrvCapableDeviceAddresses.isEmpty()) {
            LOG.debug("No HRV-capable devices found, not registering HRV cache invalidation receiver");
            return;
        }

        this.hrvCacheInvalidationBroadcastReceiver = new HrvCacheInvalidationBroadcastReceiver(hrvCapableDeviceAddresses);
        IntentFilter intentFilter = new IntentFilter(ACTION_NEW_DATA);
        ContextCompat.registerReceiver(this.context, this.hrvCacheInvalidationBroadcastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        this.registered = true;
        LOG.debug("HRV cache invalidation receiver registered for {} HRV-capable devices", hrvCapableDeviceAddresses.size());
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
        private static final Logger LOG = LoggerFactory.getLogger(HrvCacheInvalidationBroadcastReceiver.class);
        private final Set<String> hrvCapableDeviceAddresses;

        HrvCacheInvalidationBroadcastReceiver(Set<String> hrvCapableDeviceAddresses) {
            this.hrvCapableDeviceAddresses = hrvCapableDeviceAddresses;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_NEW_DATA.equals(intent.getAction())) {
                final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (device != null && hrvCapableDeviceAddresses.contains(device.getAddress())) {
                    invalidateCacheForDevice(device);
                }
            }
        }

        private void invalidateCacheForDevice(GBDevice device) {
            try (DBHandler db = GBApplication.acquireDB()) {
                final DeviceCoordinator coordinator = device.getDeviceCoordinator();

                final TimeSampleProvider<? extends HrvValueSample> sampleProvider =
                    coordinator.getHrvValueSampleProvider(device, db.getDaoSession());

                // Get the latest HRV sample to determine which days to invalidate
                final HrvValueSample latestSample = sampleProvider.getLatestSample();
                if (latestSample == null) {
                    // No HRV data at all, nothing to invalidate
                    return;
                }

                // Invalidate cache for days that might be affected by the latest data
                final long latestTimestamp = latestSample.getTimestamp();
                invalidateCacheForTimestamp(device.getAddress(), latestTimestamp);

                LOG.debug("Invalidated HRV cache for device {} based on latest sample at {}",
                    device.getName(), new java.util.Date(latestTimestamp));

            } catch (Exception e) {
                LOG.error("Error invalidating HRV cache for device " + device.getName(), e);
            }
        }

        private void invalidateCacheForTimestamp(String deviceAddress, long timestamp) {
            final Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);

            // Set to end of day for the timestamp
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);

            final long dayEndTimestamp = cal.getTimeInMillis();

            // Invalidate the day containing the latest sample
            ComputedHrvSummarySampleProvider.invalidateCacheEntry(deviceAddress, dayEndTimestamp);

            // Also invalidate today if the latest sample is from a previous day
            // (the weekly average and baseline calculations use data from multiple days)
            final Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 23);
            now.set(Calendar.MINUTE, 59);
            now.set(Calendar.SECOND, 59);
            now.set(Calendar.MILLISECOND, 999);

            if (dayEndTimestamp < now.getTimeInMillis()) {
                ComputedHrvSummarySampleProvider.invalidateCacheEntry(deviceAddress, now.getTimeInMillis());
            }
        }
    }
}

