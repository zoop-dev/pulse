/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;

/**
 * A simple sample aggregator that takes values from multiple sources for steps and HR, and broadcasts
 * the resulting activity sample. Invalidates previously seen values after a certain timeout.
 */
public class RealtimeSamplesAggregator {
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeSamplesAggregator.class);

    private final Context context;
    private final GBDevice gbDevice;

    private long lastHeartRateTime = -1;
    private int heartRate = -1;

    private long lastStepsTime = -1;
    private int previousSteps = -1;

    public RealtimeSamplesAggregator(final Context context, final GBDevice gbDevice) {
        this.context = context;
        this.gbDevice = gbDevice;
    }

    public void broadcastSteps(final int steps) {
        if (System.currentTimeMillis() - lastStepsTime > 60_000L) {
            previousSteps = -1;
        }

        lastStepsTime = System.currentTimeMillis();

        if (previousSteps == -1) {
            previousSteps = steps;
        }

        broadcast(heartRate, steps - previousSteps);
    }

    public void broadcastHeartRate(final int newHeartRate) {
        lastHeartRateTime = System.currentTimeMillis();
        heartRate = newHeartRate;

        broadcast(heartRate, 0);
    }

    private void broadcast(final int hr, final int steps) {
        final AbstractActivitySample sample;
        try (final DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final SampleProvider<? extends ActivitySample> sampleProvider = gbDevice.getDeviceCoordinator().getSampleProvider(gbDevice, session);
            sample = sampleProvider.createActivitySample();
            if (!(sample instanceof Serializable)) {
                LOG.error("Activity sample {} is not Serializable!", sample.getClass());
                return;
            }

            sample.setProvider(sampleProvider);
            sample.setDeviceId(device.getId());
            sample.setUserId(user.getId());
            sample.setTimestamp((int) (System.currentTimeMillis() / 1000));

            if (System.currentTimeMillis() - lastHeartRateTime < 10_000L) {
                sample.setHeartRate(hr);
            } else {
                sample.setSteps(-1);
            }
            if (System.currentTimeMillis() - lastStepsTime < 60_000L) {
                sample.setSteps(steps);
            } else {
                sample.setSteps(0);
            }
            sample.setRawKind(sampleProvider.toRawActivityKind(ActivityKind.UNKNOWN));
            sample.setRawIntensity(ActivitySample.NOT_MEASURED);
        } catch (final Exception e) {
            LOG.error("Error creating activity sample", e);
            return;
        }

        final Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(GBDevice.EXTRA_DEVICE, gbDevice)
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, (Serializable) sample);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
