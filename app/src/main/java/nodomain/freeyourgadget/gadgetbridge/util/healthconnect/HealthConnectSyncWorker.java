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
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.health.connect.client.HealthConnectClient;
import androidx.work.Data;
import androidx.work.CoroutineWorker;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;
import kotlin.coroutines.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class HealthConnectSyncWorker extends CoroutineWorker {
    private static final Logger LOG = LoggerFactory.getLogger(HealthConnectSyncWorker.class);
    private static final int NOTIFICATION_ID = 123;
    public static final String INPUT_DEVICE_ADDRESS = "device_address";

    public HealthConnectSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Object doWork(@NonNull Continuation<? super Result> continuation) {
        LOG.info("Health Connect sync worker started");

        try {
            return performSync();
        } catch (Exception e) {
            LOG.error("Health Connect sync worker failed", e);
            return Result.failure();
        }
    }

    private Result performSync() throws Exception {
        GBPrefs prefs = GBApplication.getPrefs();

        if (!prefs.getBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false)) {
            LOG.info("Health Connect is disabled, aborting sync.");
            return Result.failure();
        }

        setForegroundAsync(createForegroundInfo());
        setProgressAsync(new Data.Builder().putString("progress", getApplicationContext().getString(R.string.health_connect_syncing)).build());

        HealthConnectClient healthConnectClient = HealthConnectClientProvider.healthConnectInit(getApplicationContext());
        if (healthConnectClient == null) {
            LOG.error("SyncWorker: HealthConnectClient is null, cannot perform HC sync");
            return Result.success();
        }

        performHealthConnectSync(healthConnectClient);
        LOG.info("Health Connect sync worker finished successfully.");
        return Result.success();
    }


    private void performHealthConnectSync(HealthConnectClient healthConnectClient) throws InterruptedException {
        LOG.info("SyncWorker: Starting HC data sync");

        // Extract device address from input data, if provided
        String deviceAddress = getInputData().getString(INPUT_DEVICE_ADDRESS);
        if (deviceAddress != null && !deviceAddress.isEmpty()) {
            LOG.info("SyncWorker: Syncing specific device: {}", deviceAddress);
        } else {
            LOG.info("SyncWorker: Syncing all selected devices");
        }

        CountDownLatch latch = new CountDownLatch(1);
        BiConsumer<String, Boolean> summaryCallback = (summary, inProgress) -> {
            if (!inProgress) {
                boolean saved = GBApplication.getPrefs().getPreferences().edit()
                    .putString(GBPrefs.HEALTH_CONNECT_SYNC_STATUS, summary)
                    .commit();
                if (!saved) {
                    LOG.warn("Failed to save final sync status to SharedPreferences");
                }
            }
            setProgressAsync(new Data.Builder().putString("progress", summary).build());
        };

        new HealthConnectUtils().healthConnectDataSync(
            getApplicationContext(),
            healthConnectClient,
            summaryCallback,
            latch::countDown,
            this,
            deviceAddress  // Pass the device address (null if not provided)
        );
        latch.await();
        LOG.info("SyncWorker: HC data sync completed");
    }


    @NonNull
    private ForegroundInfo createForegroundInfo() {
        Context context = getApplicationContext();
        String channelId = GB.NOTIFICATION_CHANNEL_ID_HEALTH_CONNECT_SYNC;
        String title = context.getString(R.string.health_connect_sync_notification_title);
        String message = context.getString(R.string.health_connect_sync_notification_message);

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            return new ForegroundInfo(NOTIFICATION_ID, notification);
        }
    }
}

